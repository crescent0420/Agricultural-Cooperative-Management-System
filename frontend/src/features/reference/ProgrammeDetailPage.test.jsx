import { screen, within } from '@testing-library/react'
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

const soil = { id: 1, name: 'Soil preparation' }
const seed = { id: 2, name: 'Seed handling' }
const maize = { id: 7, name: 'Maize', districtId: 15, districtName: 'Huye', prerequisites: [soil] }

describe('ProgrammeDetailPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    saveSession('token', 1800)
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    vi.mocked(referenceApi.getProgramme).mockResolvedValue(maize)
    vi.mocked(referenceApi.listTrainings).mockResolvedValue(paged([soil, seed]))
  })

  it('shows the prerequisites and offers only trainings not yet linked', async () => {
    renderRoute('/reference/programmes/7')
    expect(await screen.findByRole('heading', { name: 'Maize' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Soil preparation' })).toBeInTheDocument()
    const options = within(screen.getByLabelText('Training to add'))
      .getAllByRole('option')
      .map((o) => o.textContent)
    expect(options).toEqual(['Choose a training…', 'Seed handling'])
  })

  it('adds a prerequisite training', async () => {
    vi.mocked(referenceApi.addPrerequisite).mockResolvedValue({ ...maize, prerequisites: [soil, seed] })
    renderRoute('/reference/programmes/7')
    const user = userEvent.setup()
    await user.selectOptions(await screen.findByLabelText('Training to add'), 'Seed handling')
    await user.click(screen.getByRole('button', { name: 'Add training' }))
    expect(await screen.findByText('Prerequisite training added')).toBeInTheDocument()
    expect(referenceApi.addPrerequisite).toHaveBeenCalledWith(7, 2)
  })

  it('asks for confirmation before removing a prerequisite', async () => {
    vi.mocked(referenceApi.removePrerequisite).mockResolvedValue({ ...maize, prerequisites: [] })
    renderRoute('/reference/programmes/7')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Remove Soil preparation' }))
    expect(referenceApi.removePrerequisite).not.toHaveBeenCalled()

    const dialog = screen.getByRole('dialog', { name: 'Remove prerequisite training?' })
    await user.click(within(dialog).getByRole('button', { name: 'Remove' }))
    expect(await screen.findByText('Prerequisite training removed')).toBeInTheDocument()
    expect(referenceApi.removePrerequisite).toHaveBeenCalledWith(7, 1)
  })
})
