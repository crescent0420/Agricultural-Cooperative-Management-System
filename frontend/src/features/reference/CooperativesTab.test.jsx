import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import { ApiError } from '../../api/errors'
import * as referenceApi from '../../api/reference'
import { saveSession } from '../../auth/tokenStore'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { ROLE_PERMISSIONS } from '../../test/rolePermissions'
import { registrar } from '../../test/users'

vi.mock('../../api/auth')
vi.mock('../../api/reference')

const huye = { id: 15, name: 'Huye', provinceId: 4, provinceName: 'Southern Province' }
const musanze = { id: 21, name: 'Musanze', provinceId: 2, provinceName: 'Northern Province' }
const coopA = { id: 1, name: 'Huye Maize Growers Cooperative', districtId: 15, districtName: 'Huye' }

const theRegistrar = { ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR }
const officer = {
  ...registrar,
  id: 5,
  username: 'demo.officer',
  role: 'FIELD_OFFICER',
  districtId: 15,
  permissions: ROLE_PERMISSIONS.FIELD_OFFICER,
}

function logInAs(user) {
  saveSession('token', 1800)
  vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
}

describe('CooperativesTab', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(referenceApi.getDistrict).mockResolvedValue(huye)
    vi.mocked(referenceApi.listCooperatives).mockResolvedValue(paged([coopA]))
  })

  it('lists cooperatives with paging and sorting sent to the API', async () => {
    logInAs(theRegistrar)
    renderRoute('/reference/cooperatives')
    expect(await screen.findByRole('cell', { name: coopA.name })).toBeInTheDocument()
    expect(referenceApi.listCooperatives).toHaveBeenCalledWith(
      { page: 0, size: 20, sortBy: 'name', direction: 'asc' },
      undefined,
    )

    await userEvent.setup().click(screen.getByRole('button', { name: /Name/ }))
    await waitFor(() =>
      expect(referenceApi.listCooperatives).toHaveBeenLastCalledWith(
        { page: 0, size: 20, sortBy: 'name', direction: 'desc' },
        undefined,
      ),
    )
  })

  it('lets the Registrar create a cooperative, then refreshes the list and confirms', async () => {
    logInAs(theRegistrar)
    vi.mocked(referenceApi.createCooperative).mockResolvedValue({
      id: 2,
      name: 'Nyanza Beans',
      districtId: 15,
      districtName: 'Huye',
    })
    renderRoute('/reference/cooperatives')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'New cooperative' }))

    const dialog = screen.getByRole('dialog', { name: 'New cooperative' })
    await user.type(within(dialog).getByLabelText('Name'), '  Nyanza Beans ')
    await user.selectOptions(within(dialog).getByLabelText('District'), 'Huye')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await screen.findByText('Cooperative Nyanza Beans created')).toBeInTheDocument()
    expect(referenceApi.createCooperative).toHaveBeenCalledWith('Nyanza Beans', 15)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(vi.mocked(referenceApi.listCooperatives).mock.calls.length).toBeGreaterThan(1)
  })

  it('keeps the dialog open and shows the backend message for a duplicate name (409)', async () => {
    logInAs(theRegistrar)
    vi.mocked(referenceApi.createCooperative).mockRejectedValue(
      new ApiError(409, "Cooperative 'Huye Maize Growers Cooperative' already exists in district Huye."),
    )
    renderRoute('/reference/cooperatives')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'New cooperative' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText('Name'), coopA.name)
    await user.selectOptions(within(dialog).getByLabelText('District'), 'Huye')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('already exists')
  })

  it('requires a name and a district before calling the API', async () => {
    logInAs(theRegistrar)
    renderRoute('/reference/cooperatives')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'New cooperative' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Create' }))
    expect(await screen.findByText('Enter a name.')).toBeInTheDocument()
    expect(screen.getByText('Choose a district.')).toBeInTheDocument()
    expect(referenceApi.createCooperative).not.toHaveBeenCalled()
  })

  it('shows a Field Officer only their district, read-only', async () => {
    logInAs(officer)
    renderRoute('/reference/cooperatives')
    expect(await screen.findByRole('cell', { name: coopA.name })).toBeInTheDocument()
    expect(referenceApi.listCooperatives).toHaveBeenCalledWith(expect.anything(), 15)
    expect(screen.getByLabelText('District')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'New cooperative' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Rename/ })).not.toBeInTheDocument()
  })
})
