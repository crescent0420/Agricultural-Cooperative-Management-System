import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as farmersApi from '../../api/farmers'
import { logInAs, USERS, uwase } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/farmers')

describe('MyProfilePage (FR-FAR-10)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(farmersApi.getMyProfile).mockResolvedValue(uwase)
    vi.mocked(farmersApi.listFarmerTrainings).mockResolvedValue(
      paged([
        { id: 11, farmerId: 7, trainingId: 3, trainingName: 'Soil and land preparation', completedOn: '2026-09-14' },
      ]),
    )
  })

  it('shows the farmer their own record and completed trainings', async () => {
    logInAs(USERS.farmer)
    renderRoute('/my/profile')
    expect(await screen.findByText('RCU-000007')).toBeInTheDocument()
    expect(screen.getByText(uwase.cooperativeName)).toBeInTheDocument()
    expect(screen.getByText('Huye')).toBeInTheDocument()
    expect(screen.getByText('Active')).toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: 'Soil and land preparation' })).toBeInTheDocument()
    // The trainings belong to the profile the backend returned, not to an id from the address.
    expect(farmersApi.listFarmerTrainings).toHaveBeenCalledWith(7, expect.anything())
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('is not available to staff', async () => {
    logInAs(USERS.registrar)
    renderRoute('/my/profile')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(farmersApi.getMyProfile).not.toHaveBeenCalled()
  })
})
