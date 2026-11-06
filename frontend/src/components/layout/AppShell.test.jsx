import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import * as referenceApi from '../../api/reference'
import { saveSession } from '../../auth/tokenStore'
import { renderRoute } from '../../test/renderRoute'
import { ROLE_PERMISSIONS } from '../../test/rolePermissions'
import { registrar } from '../../test/users'

vi.mock('../../api/auth')
vi.mock('../../api/reference')

const fieldOfficer = {
  ...registrar,
  id: 5,
  username: 'demo.officer',
  role: 'FIELD_OFFICER',
  districtId: 15,
  permissions: ROLE_PERMISSIONS.FIELD_OFFICER,
}
const farmer = {
  ...registrar,
  id: 7,
  username: 'demo.farmer',
  role: 'FARMER',
  districtId: null,
  farmerId: 1,
  permissions: ROLE_PERMISSIONS.FARMER,
}

function logInAs(user) {
  saveSession('token', 1800)
  vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
}

describe('AppShell', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.getDistrict).mockResolvedValue({
      id: 15,
      name: 'Huye',
      provinceId: 4,
      provinceName: 'Southern Province',
    })
  })

  it('shows the username, role and district name in the header', async () => {
    logInAs(fieldOfficer)
    renderRoute('/')
    expect(await screen.findByText('Field Officer · Huye')).toBeInTheDocument()
    expect(screen.getByText('demo.officer')).toBeInTheDocument()
    expect(referenceApi.getDistrict).toHaveBeenCalledWith(15)
  })

  it('shows the role without a district for Union-wide roles', async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    renderRoute('/')
    expect(await screen.findByTestId('role-line')).toHaveTextContent('Union Registrar')
    expect(referenceApi.getDistrict).not.toHaveBeenCalled()
  })

  it("shows the role's menu and marks the current page", async () => {
    logInAs(fieldOfficer)
    renderRoute('/seasons')
    const nav = await screen.findByRole('navigation', { name: 'Main' })
    expect(within(nav).getByRole('link', { name: 'Seasons' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'My assigned offerings' })).toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: 'Users' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Seasons' })).toBeInTheDocument()
  })

  it('shows "Not available to you" when a farmer types the address of a staff page', async () => {
    logInAs(farmer)
    renderRoute('/farmers')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Farmers' })).not.toBeInTheDocument()
  })

  it('opens the phone menu with the menu button and closes it with Esc', async () => {
    logInAs(fieldOfficer)
    renderRoute('/')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Open menu' }))
    expect(document.getElementById('mobile-menu')).toBeInTheDocument()
    await user.keyboard('{Escape}')
    expect(document.getElementById('mobile-menu')).not.toBeInTheDocument()
  })

  it('logs out from the header', async () => {
    logInAs(fieldOfficer)
    renderRoute('/')
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Log out' }))
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(sessionStorage.getItem('acms.session')).toBeNull()
  })
})
