import { ACCOUNTS, unique } from './accounts'
import { Api } from './api'
import { latestEmailTo, oneTimePassword, verifyEmail } from './mailpit'

// Test data made through the API (fast and exact), so each workflow test drives only the step it is about
// through the screens. Everything gets a unique name, so the tests can run again on the same database.

const isoDay = (offsetDays) => new Date(Date.now() + offsetDays * 86_400_000).toISOString().slice(0, 10)

/** 16 random digits, starting with 1 like a Rwandan national ID. */
export function nationalId() {
  let digits = '1'
  while (digits.length < 16) digits += Math.floor(Math.random() * 10)
  return digits
}

/** Huye district, its demo cooperative and the Huye Field Officer, as the backend knows them. */
export async function huye(registrar) {
  const districts = await registrar.get('/districts', { size: 100 })
  const district = districts.content.find((d) => d.name === 'Huye')
  const cooperatives = await registrar.get('/cooperatives', { districtId: district.id })
  const cooperative = cooperatives.content.find((c) => c.name === 'Huye Maize Growers Cooperative')
  const officer = await Api.as(ACCOUNTS.officer)
  const officerId = (await officer.get('/auth/me')).id
  await officer.dispose()
  return { district, cooperative, officerId }
}

/** A farmer registered by the Registrar in the given cooperative. */
export function registerFarmer(registrar, cooperativeId, name = unique('E2E Farmer')) {
  return registrar.post('/farmers', { fullName: name, nationalId: nationalId(), phoneNumber: null, cooperativeId })
}

/**
 * A login account for the farmer, made like in real life: the Administrator creates it, the one-time
 * password arrives by email (Mailpit), and the farmer replaces it. Returns { username, password }.
 */
export async function createFarmerAccount(farmer) {
  const admin = await Api.as(ACCOUNTS.admin)
  const username = unique('e2e.farmer').toLowerCase()
  await admin.post('/users', { username, email: `${username}@example.test`, role: 'FARMER', farmerId: farmer.id })
  await admin.dispose()
  const email = await latestEmailTo(`${username}@example.test`)
  await verifyEmail(email)
  const temporary = oneTimePassword(email)
  const password = `E2e-${unique('pw')}`
  const account = await Api.as(username, temporary)
  await account.ok('POST', '/auth/change-password', { data: { currentPassword: temporary, newPassword: password } })
  await account.dispose()
  return { username, password }
}

/** A season starting today, enrolment window closed; inputs are distributed in 90 days. */
export function createSeason(registrar) {
  return registrar.post('/seasons', {
    name: unique('E2E Season'),
    startsOn: isoDay(0),
    endsOn: isoDay(180),
    maxLandHectares: 2,
    distributionDate: isoDay(90),
  })
}

/** A Huye crop programme offered in the season, assigned to the Huye Field Officer: 0.5 ha, 10 places. */
export async function createOffering(registrar, season, place) {
  const programme = await registrar.post('/programmes', { name: unique('E2E Crop'), districtId: place.district.id })
  const offering = await registrar.post('/offerings', {
    programmeId: programme.id,
    seasonId: season.id,
    fieldOfficerId: place.officerId,
    hectaresPerParticipant: 0.5,
    maxParticipants: 10,
  })
  return { programme, offering }
}

/** Finance records a payment and confirms it (both through the API). */
export async function payConfirmed(finance, contributionId, amount) {
  const payment = await finance.post('/payments', {
    transactionReference: unique('E2E-PAY').replaceAll(' ', ''),
    contributionId,
    amount,
    method: 'MOBILE_MONEY',
  })
  return finance.post(`/payments/${payment.id}/confirm`, { providerReference: unique('MTN') })
}
