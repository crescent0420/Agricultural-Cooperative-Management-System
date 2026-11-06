import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import * as referenceApi from '../../api/reference'
import { saveSession } from '../../auth/tokenStore'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { ROLE_PERMISSIONS } from '../../test/rolePermissions'
import { registrar } from '../../test/users'

vi.mock('../../api/auth')
vi.mock('../../api/reference')

const musanze = { id: 7, name: 'Musanze', provinceId: 2, provinceName: 'Northern Province' }
const huye = { id: 15, name: 'Huye', provinceId: 4, provinceName: 'Southern Province' }

function logInAs(user) {
  saveSession('token', 1800)
  vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
}

describe('DistrictsTab', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.listProvinces).mockResolvedValue(
      paged([
        { id: 2, name: 'Northern Province' },
        { id: 4, name: 'Southern Province' },
      ]),
    )
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
  })

  it('filters by province', async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    renderRoute('/reference/districts')
    await screen.findByRole('cell', { name: 'Musanze' })
    await userEvent.setup().selectOptions(screen.getByLabelText('Province'), 'Northern Province')
    await waitFor(() => expect(referenceApi.listDistricts).toHaveBeenLastCalledWith(expect.anything(), 2))
  })

  it("warns, when renaming, that district names come from Rwanda's official list (G11)", async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    vi.mocked(referenceApi.renameDistrict).mockResolvedValue(musanze)
    renderRoute('/reference/districts')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Rename Musanze' }))

    const dialog = screen.getByRole('dialog', { name: 'Rename Musanze' })
    expect(within(dialog).getByRole('alert')).toHaveTextContent("Rwanda's official list")
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('District renamed to Musanze')).toBeInTheDocument()
    expect(referenceApi.renameDistrict).toHaveBeenCalledWith(7, 'Musanze')
  })

  it('offers no Rename to roles without REFERENCE_MANAGE', async () => {
    logInAs({ ...registrar, role: 'FINANCE_OFFICER', permissions: ROLE_PERMISSIONS.FINANCE_OFFICER })
    renderRoute('/reference/districts')
    await screen.findByRole('cell', { name: 'Musanze' })
    expect(screen.queryByRole('button', { name: /Rename/ })).not.toBeInTheDocument()
  })
})
