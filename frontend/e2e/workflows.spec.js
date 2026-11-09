import { expect, test } from '@playwright/test'
import { ACCOUNTS, unique } from './support/accounts'
import { Api } from './support/api'
import {
  createFarmerAccount,
  createOffering,
  createSeason,
  huye,
  nationalId,
  payConfirmed,
  registerFarmer,
} from './support/data'
import { latestEmailTo, oneTimePassword, verifyEmail } from './support/mailpit'
import { logIn } from './support/ui'

// T1 (Phase 6): the demo-plan workflows, through the screens, against the real backend. Each test drives the
// step it is about through the UI and then asks the backend what was really stored.

let registrar
let place
test.beforeAll(async () => {
  registrar = await Api.as(ACCOUNTS.registrar)
  place = await huye(registrar)
})
test.afterAll(async () => {
  await registrar.dispose()
})

test.describe('1. Accounts', () => {
  test('the Administrator creates a farmer account; the farmer logs in with the emailed password and replaces it', async ({
    page,
  }) => {
    const farmer = await registerFarmer(registrar, place.cooperative.id)
    const username = unique('e2e.ui').toLowerCase()

    await logIn(page, ACCOUNTS.admin)
    await page.goto('/admin/users/new')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Email').fill(`${username}@example.test`)
    await page.getByLabel('Role').selectOption({ label: 'Farmer' })
    await page.getByLabel('Farmer record').fill(farmer.fullName)
    await page.getByRole('button', { name: new RegExp(farmer.fullName) }).click()
    await page.getByRole('button', { name: 'Create account' }).click()
    await expect(page.getByText(`Account ${username} created`)).toBeVisible()

    const email = await latestEmailTo(`${username}@example.test`)
    const temporary = oneTimePassword(email)
    await page.getByRole('button', { name: 'Log out' }).click()
    const logInWithEmailedPassword = async () => {
      await page.getByLabel('Username').fill(username)
      await page.getByLabel('Password', { exact: true }).fill(temporary)
      await page.getByRole('button', { name: 'Log in' }).click()
    }

    // Before the farmer clicks the link in the email, the login page says so.
    await logInWithEmailedPassword()
    await expect(page.getByRole('alert').filter({ hasText: 'Please confirm your email address first' })).toBeVisible()
    await verifyEmail(email)
    await logInWithEmailedPassword()

    await expect(page.getByRole('heading', { name: 'Choose your own password' })).toBeVisible()
    const password = `E2e-${unique('pw')}`
    await page.getByLabel('One-time password', { exact: true }).fill(temporary)
    await page.getByLabel('New password', { exact: true }).fill(password)
    await page.getByLabel('Confirm new password', { exact: true }).fill(password)
    await page.getByRole('button', { name: 'Save new password' }).click()
    await expect(page.getByRole('heading', { name: 'Dashboard', level: 1 })).toBeVisible()
    await expect(page.getByRole('region', { name: 'My profile' }).getByText(farmer.membershipNumber)).toBeVisible()

    // In the backend: the account is linked to the farmer, and only the new password works.
    const account = await Api.as(username, password)
    expect((await account.get('/auth/me')).farmerId).toBe(farmer.id)
    await account.dispose()
    const anonymous = await Api.anonymous()
    expect((await anonymous.call('POST', '/auth/login', { data: { username, password: temporary } })).status).toBe(401)
    await anonymous.dispose()
  })

  test('the Administrator links a self-registered account to its farmer record', async ({ page }) => {
    const farmer = await registerFarmer(registrar, place.cooperative.id)
    const username = unique('e2e.self').toLowerCase()
    const anonymous = await Api.anonymous()
    const registered = await anonymous.call('POST', '/auth/register', {
      data: { username, email: `${username}@example.test` },
    })
    expect(registered.status).toBe(201)
    await anonymous.dispose()

    const admin = await Api.as(ACCOUNTS.admin)
    const newest = await admin.get('/users', { sortBy: 'createdAt', direction: 'desc', size: 50 })
    const user = newest.content.find((u) => u.username === username)
    expect(user.farmerId).toBeNull()

    await logIn(page, ACCOUNTS.admin)
    await page.goto(`/admin/users/${user.id}`)
    await page.getByRole('button', { name: 'Link to a farmer record' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Farmer record').fill(farmer.fullName)
    await dialog.getByRole('button', { name: new RegExp(farmer.fullName) }).click()
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(page.getByText(`${username} is now linked to Farmer #${farmer.id}`)).toBeVisible()

    expect((await admin.get(`/users/${user.id}`)).farmerId).toBe(farmer.id)
    await admin.dispose()
  })
})

test.describe('2. Farmers', () => {
  test('the Registrar registers a farmer and gets the membership number; a second registration with the same national ID is refused', async ({
    page,
  }) => {
    const name = unique('E2E Registered')
    const id = nationalId()
    await logIn(page, ACCOUNTS.registrar)

    const fill = async () => {
      await page.goto('/farmers/new')
      await page.getByLabel('Full name').fill(name)
      await page.getByLabel('National ID').fill(id)
      await page.getByLabel('District').selectOption({ label: 'Huye' })
      await page.getByLabel('Cooperative').selectOption({ label: place.cooperative.name })
      await page.getByRole('button', { name: 'Register farmer' }).click()
    }

    await fill()
    await expect(page.getByText(new RegExp(`${name} registered with membership number RCU-\\d+`))).toBeVisible()
    await expect(page).toHaveURL(/\/farmers\/\d+\/details$/)
    const farmerId = Number(page.url().match(/\/farmers\/(\d+)\//)[1])
    const stored = await registrar.get(`/farmers/${farmerId}`)
    expect(stored).toMatchObject({ fullName: name, nationalId: id, status: 'ACTIVE' })
    expect(stored.membershipNumber).toMatch(/^RCU-\d+$/)

    await fill()
    await expect(page.getByLabel('National ID')).toHaveAttribute('aria-invalid', 'true')
    await expect(page).toHaveURL(/\/farmers\/new$/)
    expect((await registrar.get('/farmers', { name })).totalElements).toBe(1)
  })

  test('a retired farmer cannot become active again (422), and the status tab offers no change', async ({ page }) => {
    const farmer = await registerFarmer(registrar, place.cooperative.id)
    await registrar.patch(`/farmers/${farmer.id}/status`, { status: 'RETIRED' })
    const refused = await registrar.call('PATCH', `/farmers/${farmer.id}/status`, { data: { status: 'ACTIVE' } })
    expect(refused.status).toBe(422)

    await logIn(page, ACCOUNTS.registrar)
    await page.goto(`/farmers/${farmer.id}/status`)
    await expect(page.getByRole('heading', { name: /Status/ })).toContainText('Retired')
    await expect(page.getByRole('button', { name: 'Change status' })).toHaveCount(0)
  })
})

test.describe('3-6. One farmer through a season: enrolment, payments, results, permit', () => {
  test.describe.configure({ mode: 'serial' })

  const s = {}
  test.beforeAll(async () => {
    s.season = await createSeason(registrar)
    Object.assign(s, await createOffering(registrar, s.season, place))
    s.farmer = await registerFarmer(registrar, place.cooperative.id)
    s.account = await createFarmerAccount(s.farmer)
    s.finance = await Api.as(ACCOUNTS.finance)
    s.contribution = await s.finance.post('/contributions', {
      farmerId: s.farmer.id,
      seasonId: s.season.id,
      amountDue: 50000,
    })
    s.card = `${s.programme.name}, ${s.season.name}`
  })
  test.afterAll(async () => {
    await s.finance?.dispose()
  })

  test('3a. the Registrar opens the enrolment window', async ({ page }) => {
    await logIn(page, ACCOUNTS.registrar)
    await page.goto(`/seasons/${s.season.id}`)
    await page.getByRole('button', { name: 'Open enrolment window' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Open window' }).click()
    await expect(page.getByText('Enrolment window opened')).toBeVisible()
    expect((await registrar.get(`/seasons/${s.season.id}`)).enrolmentOpen).toBe(true)
  })

  test('3b. the farmer cannot enrol with less than 30% paid', async ({ page }) => {
    await logIn(page, s.account.username, s.account.password)
    await page.goto('/my/offerings')
    await page.getByRole('article', { name: s.card }).getByRole('button', { name: 'Enrol' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByRole('button', { name: 'Enrol' }).click()
    await expect(dialog.getByRole('alert')).toBeVisible()
    await expect(dialog.getByRole('alert')).toContainText('30')

    const farmer = await Api.as(s.account.username, s.account.password)
    expect((await farmer.get('/me/enrolments')).totalElements).toBe(0)
    await farmer.dispose()
  })

  test('3c. Finance records 20,000 and confirms it: 40% paid', async ({ page }) => {
    await logIn(page, ACCOUNTS.finance)
    await page.goto(`/contributions/${s.contribution.id}`)
    await page.getByRole('button', { name: 'Record payment' }).click()
    const record = page.getByRole('dialog', { name: 'Record a payment' })
    await record.getByLabel('Amount').fill('20000')
    await record.getByLabel('Method').selectOption({ label: 'Mobile money' })
    await record.getByRole('button', { name: 'Save' }).click()
    await expect(page.getByText(/Payment of 20,000 recorded/)).toBeVisible()

    await page.getByRole('button', { name: /^Confirm ACMS-/ }).click()
    const confirm = page.getByRole('dialog', { name: 'Confirm this payment?' })
    await confirm.getByLabel('Provider reference').fill(unique('MTN'))
    await confirm.getByRole('button', { name: 'Confirm payment' }).click()
    await expect(page.getByText('Payment confirmed; the balance is updated')).toBeVisible()

    const balance = await s.finance.get(`/contributions/${s.contribution.id}`)
    expect(Number(balance.confirmedPaid)).toBe(20000)
    expect(Number(balance.paidPercent)).toBe(40)
  })

  test('3d. now the farmer enrols; a second enrolment is refused (409)', async ({ page }) => {
    await logIn(page, s.account.username, s.account.password)
    await page.goto('/my/offerings')
    await page.getByRole('article', { name: s.card }).getByRole('button', { name: 'Enrol' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Enrol' }).click()
    await expect(page.getByText(`Enrolled in ${s.programme.name}, ${s.season.name} (0.5 ha)`)).toBeVisible()

    const farmer = await Api.as(s.account.username, s.account.password)
    const mine = await farmer.get('/me/enrolments')
    expect(mine.content).toHaveLength(1)
    expect(mine.content[0]).toMatchObject({ offeringId: s.offering.id, status: 'ENROLLED' })
    s.enrolment = mine.content[0]
    expect((await farmer.call('POST', `/offerings/${s.offering.id}/enrolments`)).status).toBe(409)
    await farmer.dispose()
  })

  test('4. a payment sent twice is stored once; the same reference with another amount is refused (409)', async () => {
    const reference = unique('E2E-RETRY')
    const payment = {
      transactionReference: reference,
      contributionId: s.contribution.id,
      amount: 1000,
      method: 'BANK_TRANSFER',
    }
    const first = await s.finance.call('POST', '/payments', { data: payment })
    const again = await s.finance.call('POST', '/payments', { data: payment })
    expect(first.status).toBe(201)
    expect(again.status).toBe(200)
    expect(again.data.id).toBe(first.data.id)
    const different = await s.finance.call('POST', '/payments', { data: { ...payment, amount: 2000 } })
    expect(different.status).toBe(409)
    // Not confirmed, so it does not count: the 40% from 3c is unchanged.
    expect(Number((await s.finance.get(`/contributions/${s.contribution.id}`)).paidPercent)).toBe(40)
    await s.finance.post(`/payments/${first.data.id}/fail`, { reason: 'E2E test payment, not real' })
  })

  test('5a. the Field Officer scores and submits; another district’s officer is refused (403)', async () => {
    const officer = await Api.as(ACCOUNTS.officer)
    const inspection = await officer.post(`/offerings/${s.offering.id}/inspections`, {
      name: 'Field visit',
      maxScore: 100,
      weight: 100,
    })
    s.inspection = inspection
    const draft = await officer.put(`/enrolments/${s.enrolment.id}/scores`, {
      scores: [{ inspectionId: inspection.id, score: 85 }],
    })
    const submitted = await officer.post(`/results/${draft.id}/submit`)
    expect(submitted).toMatchObject({ status: 'SUBMITTED', finalScore: 85 })
    s.result = submitted
    await officer.dispose()

    const musanze = await Api.as(ACCOUNTS.officerMusanze)
    expect((await musanze.call('GET', `/offerings/${s.offering.id}/results`)).status).toBe(403)
    expect((await musanze.call('POST', `/results/${s.result.id}/approve`)).status).toBe(403)
    await musanze.dispose()
  })

  test('5b. the Agronomist approves; the farmer sees the grade; a correction keeps its history', async ({ page }) => {
    await logIn(page, ACCOUNTS.agronomist)
    await page.goto(`/results/${s.result.id}`)
    await page.getByRole('button', { name: 'Approve and publish' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Approve and publish' }).click()
    const agronomist = await Api.as(ACCOUNTS.agronomist)
    await expect.poll(async () => (await agronomist.get(`/results/${s.result.id}`)).status).toBe('PUBLISHED')
    const published = await agronomist.get(`/results/${s.result.id}`)
    expect(published.grade).toBeTruthy()

    await page.getByRole('button', { name: 'Log out' }).click()
    await logIn(page, s.account.username, s.account.password)
    await page.goto('/my/results')
    const row = page.getByRole('row').filter({ hasText: s.programme.name })
    await expect(row).toContainText('85')
    await expect(row).toContainText(published.grade)

    const corrected = await agronomist.post(`/results/${s.result.id}/corrections`, {
      inspectionId: s.inspection.id,
      correctedScore: 90,
      reason: 'E2E: score sheet re-checked',
    })
    expect(Number(corrected.finalScore)).toBe(90)
    const history = await agronomist.get(`/results/${s.result.id}/corrections`)
    expect(history).toHaveLength(1)
    expect(Number(history[0].originalScore)).toBe(85)
    expect(Number(history[0].correctedScore)).toBe(90)
    await agronomist.dispose()
  })

  test('6. the permit is refused until 100% is paid, then issued once; the store verifies it', async ({ page }) => {
    const getPermit = async () => {
      await page.goto('/my/enrolments')
      await page.getByRole('button', { name: `Get the permit for ${s.programme.name}` }).click()
      return page.getByRole('dialog')
    }
    await logIn(page, s.account.username, s.account.password)
    let dialog = await getPermit()
    await expect(dialog.getByRole('alert')).toBeVisible()
    await dialog.getByRole('button', { name: 'Close' }).click()

    await payConfirmed(s.finance, s.contribution.id, 30000)
    dialog = await getPermit()
    await expect(dialog.getByText('Your permit has been issued.', { exact: false })).toBeVisible()
    const number = (await dialog.textContent()).match(/CP-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}/)[0]
    await dialog.getByRole('button', { name: 'Close' }).click()

    dialog = await getPermit()
    await expect(dialog.getByText('You already have this permit', { exact: false })).toBeVisible()
    await expect(dialog).toContainText(number)
    await dialog.getByRole('button', { name: 'Close' }).click()

    await page.getByRole('button', { name: 'Log out' }).click()
    await logIn(page, ACCOUNTS.registrar)
    await page.goto('/permits/verify')
    await page.getByLabel('Permit number').fill(number)
    await page.getByRole('button', { name: 'Check' }).click()
    await expect(page.getByRole('region', { name: 'Verification result' })).toContainText('✓ VALID')
  })
})

test.describe('7. Reports', () => {
  const sum = (rows, key) => rows.reduce((total, row) => total + Number(row[key] ?? 0), 0)

  test('every report’s totals are the sums of its rows', async () => {
    const farmers = await registrar.get('/reports/farmers-per-cooperative')
    expect(farmers.totals.total).toBe(sum(farmers.rows, 'total'))
    expect(farmers.totals.cooperatives).toBe(farmers.rows.length)

    const capacity = await registrar.get('/reports/offering-capacity')
    expect(capacity.totals.enrolled).toBe(sum(capacity.rows, 'enrolled'))
    expect(capacity.totals.maxParticipants).toBe(sum(capacity.rows, 'maxParticipants'))
    expect(capacity.totals.offerings).toBe(capacity.rows.length)

    const money = await registrar.get('/reports/outstanding-contributions')
    expect(Number(money.totals.amountDue)).toBeCloseTo(sum(money.rows, 'amountDue'), 2)
    expect(Number(money.totals.outstanding)).toBeCloseTo(sum(money.rows, 'outstanding'), 2)

    const grades = await registrar.get('/reports/grade-distribution')
    expect(grades.totals.published).toBe(sum(grades.rows, 'published'))
  })

  test('a Field Officer’s capacity report counts only their own offerings, and the screen shows them', async ({
    page,
  }) => {
    const officer = await Api.as(ACCOUNTS.officer)
    const me = await officer.get('/auth/me')
    const report = await officer.get('/reports/offering-capacity')
    expect(report.scope.fieldOfficerId).toBe(me.id)
    for (const row of report.rows) expect(row.fieldOfficerId).toBe(me.id)
    await officer.dispose()

    await logIn(page, ACCOUNTS.officer)
    await page.goto('/reports/capacity')
    await expect(page.getByText('Only the offerings assigned to you are counted.')).toBeVisible()
    if (report.rows.length > 0) {
      await expect(page.getByRole('table', { name: 'Enrolments and capacity per offering' })).toContainText(
        report.rows[0].programmeName,
      )
    }
  })
})
