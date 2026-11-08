import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as auditApi from '../../api/audit'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as permissionsApi from '../../api/permissions'
import * as referenceApi from '../../api/reference'
import * as usersApi from '../../api/users'
import {
  adminAccount,
  auditEvents,
  linkedFarmerAccount,
  officerAccount,
  permissionMatrix,
  unlinkedFarmerAccount,
} from '../../test/admin'
import { huye, logInAs, musanze, USERS, uwaseSummary } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/audit')
vi.mock('../../api/farmers')
vi.mock('../../api/permissions')
vi.mock('../../api/reference')
vi.mock('../../api/users')

/** Types into the farmer search and picks Uwase Marie. */
async function pickUwase(user, scope = screen) {
  await user.type(scope.getByLabelText('Farmer record'), 'Uwase')
  await user.click(await scope.findByRole('button', { name: /Uwase Marie/ }))
}

describe('Administration (FR-ADM-01..07)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.admin)
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(farmersApi.searchFarmers).mockResolvedValue(paged([uwaseSummary]))
  })

  describe('users list', () => {
    it('lists the accounts with district name, farmer record and account state, sortable by role', async () => {
      vi.mocked(usersApi.listUsers).mockResolvedValue(
        paged([adminAccount, officerAccount, linkedFarmerAccount, unlinkedFarmerAccount]),
      )
      renderRoute('/admin/users')
      const table = await screen.findByRole('table', { name: 'User accounts' })
      expect(await within(table).findByRole('link', { name: 'demo.officer' })).toHaveAttribute('href', '/admin/users/5')
      await waitFor(() => expect(within(table).getByRole('cell', { name: 'Huye' })).toBeInTheDocument())
      expect(within(table).getByRole('cell', { name: 'Farmer #7' })).toBeInTheDocument()
      expect(within(table).getByText('Not linked')).toBeInTheDocument()
      expect(within(table).getByText('Email not verified')).toBeInTheDocument()
      expect(usersApi.listUsers).toHaveBeenCalledWith({ page: 0, size: 20, sortBy: 'username', direction: 'asc' })

      await userEvent.setup().click(screen.getByRole('button', { name: /Role/ }))
      await waitFor(() =>
        expect(usersApi.listUsers).toHaveBeenLastCalledWith({ page: 0, size: 20, sortBy: 'role', direction: 'asc' }),
      )
      expect(screen.getByRole('link', { name: 'Create account' })).toHaveAttribute('href', '/admin/users/new')
    })

    it.each([
      ['Union Registrar', USERS.registrar],
      ['Field Officer', USERS.officer],
    ])('is not available to a %s', async (_, user) => {
      logInAs(user)
      renderRoute('/admin/users')
      expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
      expect(usersApi.listUsers).not.toHaveBeenCalled()
    })
  })

  describe('create account', () => {
    it('checks the fields like the backend before calling the API', async () => {
      renderRoute('/admin/users/new')
      const user = userEvent.setup()
      await user.type(await screen.findByLabelText('Username'), 'Bad Name')
      await user.type(screen.getByLabelText('Email'), 'not-an-email')
      await user.click(screen.getByRole('button', { name: 'Create account' }))

      expect(
        await screen.findByText('3 to 50 characters: lowercase letters, digits, dot, underscore or hyphen.'),
      ).toBeInTheDocument()
      expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument()
      expect(screen.getByText('Choose a role.')).toBeInTheDocument()
      expect(usersApi.createUser).not.toHaveBeenCalled()
    })

    it('asks a Field Officer for a district, then creates the account and opens it', async () => {
      vi.mocked(usersApi.createUser).mockResolvedValue({ ...officerAccount, id: 30, username: 'new.officer' })
      vi.mocked(usersApi.getUser).mockResolvedValue({ ...officerAccount, id: 30, username: 'new.officer' })
      const { router } = renderRoute('/admin/users/new')
      const user = userEvent.setup()
      await user.type(await screen.findByLabelText('Username'), 'new.officer')
      await user.type(screen.getByLabelText('Email'), 'officer@example.test')
      expect(screen.queryByLabelText('District')).not.toBeInTheDocument()
      await user.selectOptions(screen.getByLabelText('Role'), 'Field Officer')
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      expect(await screen.findByText('Choose a district.')).toBeInTheDocument()

      await user.selectOptions(screen.getByLabelText('District'), 'Huye')
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      expect(
        await screen.findByText(/Account new.officer created. A one-time password was emailed/),
      ).toBeInTheDocument()
      expect(usersApi.createUser).toHaveBeenCalledWith({
        username: 'new.officer',
        email: 'officer@example.test',
        role: 'FIELD_OFFICER',
        districtId: 15,
        farmerId: null,
      })
      await waitFor(() => expect(router.state.location.pathname).toBe('/admin/users/30'))
    })

    it('requires the farmer record for a farmer account (decision A1) and sends its id', async () => {
      vi.mocked(usersApi.createUser).mockResolvedValue(linkedFarmerAccount)
      vi.mocked(usersApi.getUser).mockResolvedValue(linkedFarmerAccount)
      renderRoute('/admin/users/new')
      const user = userEvent.setup()
      await user.type(await screen.findByLabelText('Username'), 'uwase')
      await user.type(screen.getByLabelText('Email'), 'uwase@example.test')
      await user.selectOptions(screen.getByLabelText('Role'), 'Farmer')
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      expect(await screen.findByText('Choose the farmer record this account belongs to.')).toBeInTheDocument()
      expect(usersApi.createUser).not.toHaveBeenCalled()

      await pickUwase(user)
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      await waitFor(() =>
        expect(usersApi.createUser).toHaveBeenCalledWith(
          expect.objectContaining({ role: 'FARMER', districtId: null, farmerId: 7 }),
        ),
      )
    })

    it('sends neither district nor farmer for the other roles', async () => {
      vi.mocked(usersApi.createUser).mockResolvedValue({ ...adminAccount, id: 31, username: 'new.finance' })
      vi.mocked(usersApi.getUser).mockResolvedValue({ ...adminAccount, id: 31, username: 'new.finance' })
      renderRoute('/admin/users/new')
      const user = userEvent.setup()
      await user.type(await screen.findByLabelText('Username'), 'new.finance')
      await user.type(screen.getByLabelText('Email'), 'finance@example.test')
      await user.selectOptions(screen.getByLabelText('Role'), 'Field Officer')
      await user.selectOptions(screen.getByLabelText('District'), 'Huye')
      await user.selectOptions(screen.getByLabelText('Role'), 'Finance Officer')
      expect(screen.queryByLabelText('District')).not.toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      await waitFor(() =>
        expect(usersApi.createUser).toHaveBeenCalledWith(
          expect.objectContaining({ role: 'FINANCE_OFFICER', districtId: null, farmerId: null }),
        ),
      )
    })

    it('shows a username already in use (409) under that field', async () => {
      vi.mocked(usersApi.createUser).mockRejectedValue(new ApiError(409, 'Username is already in use.'))
      renderRoute('/admin/users/new')
      const user = userEvent.setup()
      await user.type(await screen.findByLabelText('Username'), 'demo.officer')
      await user.type(screen.getByLabelText('Email'), 'other@example.test')
      await user.selectOptions(screen.getByLabelText('Role'), 'Union Registrar')
      await user.click(screen.getByRole('button', { name: 'Create account' }))
      expect(await screen.findByText('Username is already in use.')).toBeInTheDocument()
      expect(screen.getByLabelText('Username')).toHaveAttribute('aria-invalid', 'true')
    })
  })

  describe('user page', () => {
    it('links an unlinked farmer account to its farmer record (FR-ADM-03)', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue(unlinkedFarmerAccount)
      vi.mocked(usersApi.changeUserRole).mockResolvedValue({ ...unlinkedFarmerAccount, farmerId: 7 })
      renderRoute('/admin/users/22')
      const user = userEvent.setup()
      expect(await screen.findByText(/not linked to a farmer record yet/)).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Link to a farmer record' }))
      const dialog = screen.getByRole('dialog', { name: 'Link to a farmer record' })
      expect(within(dialog).getByLabelText('Role')).toHaveValue('FARMER')
      await pickUwase(user, within(dialog))
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      expect(await screen.findByText('self.registered is now linked to Farmer #7')).toBeInTheDocument()
      expect(usersApi.changeUserRole).toHaveBeenCalledWith(22, { role: 'FARMER', districtId: null, farmerId: 7 })
      expect(screen.getAllByText('Farmer #7').length).toBeGreaterThan(0)
    })

    it('changes a Field Officer into an Agronomist, keeping the district', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue(officerAccount)
      vi.mocked(usersApi.changeUserRole).mockResolvedValue({ ...officerAccount, role: 'DISTRICT_AGRONOMIST' })
      renderRoute('/admin/users/5')
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name: 'Change role' }))
      const dialog = screen.getByRole('dialog', { name: 'Change role' })
      expect(within(dialog).getByText(/applies from demo.officer's next request/)).toBeInTheDocument()
      await user.selectOptions(within(dialog).getByLabelText('Role'), 'District Agronomist')
      await waitFor(() => expect(within(dialog).getByLabelText('District')).toHaveValue('15'))
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      expect(await screen.findByText('demo.officer is now District Agronomist')).toBeInTheDocument()
      expect(usersApi.changeUserRole).toHaveBeenCalledWith(5, {
        role: 'DISTRICT_AGRONOMIST',
        districtId: 15,
        farmerId: null,
      })
    })

    it('warns in yellow, only when tried, that an administrator cannot change or disable their own account', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue(adminAccount)
      renderRoute('/admin/users/1')
      const user = userEvent.setup()
      expect(await screen.findByText('This is your own account.')).toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: 'Change role' }))
      expect(screen.getByRole('alert')).toHaveTextContent('You cannot change your own role.')
      await user.click(screen.getByRole('button', { name: 'Disable account' }))
      expect(screen.getByRole('alert')).toHaveTextContent('You cannot disable your own account.')
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(usersApi.changeUserRole).not.toHaveBeenCalled()
      expect(usersApi.setUserEnabled).not.toHaveBeenCalled()
    })

    it('disables another account after confirmation', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue(officerAccount)
      vi.mocked(usersApi.setUserEnabled).mockResolvedValue({ ...officerAccount, enabled: false })
      renderRoute('/admin/users/5')
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name: 'Disable account' }))
      const dialog = screen.getByRole('dialog', { name: 'Disable demo.officer?' })
      expect(dialog).toHaveTextContent('refused from their next request')
      await user.click(within(dialog).getByRole('button', { name: 'Disable' }))

      expect(await screen.findByText('demo.officer disabled')).toBeInTheDocument()
      expect(usersApi.setUserEnabled).toHaveBeenCalledWith(5, false)
      expect(screen.getByText('Disabled')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Enable account' })).toBeInTheDocument()
    })

    it('emails a new one-time password and shows the backend message', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue(officerAccount)
      vi.mocked(usersApi.resetUserPassword).mockResolvedValue('A new one-time password has been emailed to the user.')
      renderRoute('/admin/users/5')
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name: 'Reset password' }))
      await user.click(screen.getByRole('button', { name: 'Email a new password' }))
      expect(await screen.findByText('A new one-time password has been emailed to the user.')).toBeInTheDocument()
      expect(usersApi.resetUserPassword).toHaveBeenCalledWith(5)
    })

    it('shows the backend refusal in red when the password cannot be reset (422)', async () => {
      vi.mocked(usersApi.getUser).mockResolvedValue({ ...officerAccount, enabled: false })
      vi.mocked(usersApi.resetUserPassword).mockRejectedValue(
        new ApiError(422, 'Enable the account before resetting its password.'),
      )
      renderRoute('/admin/users/5')
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name: 'Reset password' }))
      const dialog = screen.getByRole('dialog')
      await user.click(within(dialog).getByRole('button', { name: 'Email a new password' }))
      expect(await within(dialog).findByRole('alert')).toHaveTextContent(
        'Enable the account before resetting its password.',
      )
    })
  })

  describe('audit log', () => {
    it('lists the newest events first with outcome, who and what, and sorts by event', async () => {
      vi.mocked(auditApi.listAuditLogs).mockResolvedValue(paged(auditEvents))
      renderRoute('/admin/audit-log')
      const table = await screen.findByRole('table', { name: 'Audit log' })
      await within(table).findByRole('link', { name: 'demo.admin' })
      expect(auditApi.listAuditLogs).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        sortBy: 'occurredAt',
        direction: 'desc',
      })
      expect(within(table).getByRole('cell', { name: 'User role changed' })).toBeInTheDocument()
      expect(within(table).getByRole('cell', { name: 'Login failure' })).toBeInTheDocument()
      expect(within(table).getAllByText('Succeeded')).toHaveLength(2)
      expect(within(table).getByText('Failed')).toBeInTheDocument()
      expect(within(table).getByRole('link', { name: 'demo.admin' })).toHaveAttribute('href', '/admin/users/1')
      expect(within(table).getByRole('link', { name: 'User #22' })).toHaveAttribute('href', '/admin/users/22')
      expect(within(table).getByRole('cell', { name: 'Farmer #7' })).toBeInTheDocument()
      expect(within(table).getByRole('cell', { name: 'unknown.person' })).toBeInTheDocument()

      await userEvent.setup().click(screen.getByRole('button', { name: /Event/ }))
      await waitFor(() =>
        expect(auditApi.listAuditLogs).toHaveBeenLastCalledWith({
          page: 0,
          size: 20,
          sortBy: 'eventType',
          direction: 'asc',
        }),
      )
    })

    it('is not available to the Union Registrar', async () => {
      logInAs(USERS.registrar)
      renderRoute('/admin/audit-log')
      expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
      expect(auditApi.listAuditLogs).not.toHaveBeenCalled()
    })
  })

  describe('permission matrix', () => {
    it('shows every permission by area with a tick per role, and highlights the viewer’s role', async () => {
      vi.mocked(permissionsApi.getPermissionMatrix).mockResolvedValue(permissionMatrix)
      renderRoute('/permissions')
      const table = await screen.findByRole('table', { name: 'Permission matrix' })
      expect(
        within(table)
          .getAllByRole('rowheader')
          .map((h) => h.textContent),
      ).toEqual([
        'Reference data',
        'Read reference dataREFERENCE_READ',
        'Manage reference dataREFERENCE_MANAGE',
        'Administration',
        'Manage user accountsUSER_MANAGE',
      ])
      const header = within(table).getByRole('columnheader', { name: /System Administrator/ })
      expect(header).toHaveTextContent('(you)')
      const manage = within(table).getByRole('row', { name: /Manage user accounts/ })
      expect(within(manage).getAllByText('Yes')).toHaveLength(1)
      expect(within(manage).getAllByText('No')).toHaveLength(5)
      expect(screen.getByText(/holds 2 of 3 permissions/)).toBeInTheDocument()
    })

    it('is open to every role, a farmer included', async () => {
      logInAs(USERS.farmer)
      vi.mocked(permissionsApi.getPermissionMatrix).mockResolvedValue(permissionMatrix)
      renderRoute('/permissions')
      expect(await screen.findByRole('columnheader', { name: /Farmer/ })).toHaveTextContent('(you)')
    })
  })
})
