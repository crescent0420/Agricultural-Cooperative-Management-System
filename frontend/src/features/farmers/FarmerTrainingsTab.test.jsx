import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as referenceApi from '../../api/reference'
import { logInAs, USERS, uwase } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/farmers')
vi.mock('../../api/reference')

const soil = { id: 3, name: 'Soil and land preparation' }
const pests = { id: 4, name: 'Pest and disease control' }
const record = { id: 11, farmerId: 7, trainingId: 3, trainingName: soil.name, completedOn: '2026-10-14' }
const maize = { id: 30, name: 'Maize', districtId: 15, districtName: 'Huye', prerequisites: [soil, pests] }

describe('Farmer trainings (FR-FAR-06, 07, 08)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(farmersApi.getFarmer).mockResolvedValue(uwase)
    vi.mocked(farmersApi.listFarmerTrainings).mockResolvedValue(paged([record]))
    vi.mocked(referenceApi.listTrainings).mockResolvedValue(paged([soil, pests]))
    vi.mocked(referenceApi.listProgrammes).mockResolvedValue(paged([maize]))
  })

  it('lists the completed trainings, newest first', async () => {
    logInAs(USERS.officer)
    renderRoute('/farmers/7/trainings')
    expect(await screen.findByRole('cell', { name: soil.name })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '14 Oct 2026' })).toBeInTheDocument()
    expect(farmersApi.listFarmerTrainings).toHaveBeenCalledWith(7, {
      page: 0,
      size: 20,
      sortBy: 'completedOn',
      direction: 'desc',
    })
    expect(screen.queryByRole('button', { name: 'Record training' })).not.toBeInTheDocument()
  })

  it('lets the Registrar record a training, then refreshes the list', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.recordFarmerTraining).mockResolvedValue({
      ...record,
      id: 12,
      trainingId: 4,
      trainingName: pests.name,
    })
    renderRoute('/farmers/7/trainings')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Record training' }))
    const dialog = screen.getByRole('dialog', { name: 'Record a completed training' })
    await within(dialog).findByRole('option', { name: pests.name })
    await user.selectOptions(within(dialog).getByLabelText('Training'), pests.name)
    const date = within(dialog).getByLabelText('Completed on')
    await user.clear(date)
    await user.type(date, '2026-10-01')
    await user.click(within(dialog).getByRole('button', { name: 'Record' }))

    expect(await screen.findByText('Pest and disease control recorded')).toBeInTheDocument()
    expect(farmersApi.recordFarmerTraining).toHaveBeenCalledWith(7, 4, '2026-10-01')
    expect(vi.mocked(farmersApi.listFarmerTrainings).mock.calls.length).toBeGreaterThan(1)
  })

  it('refuses a completion date in the future before calling the API', async () => {
    logInAs(USERS.registrar)
    renderRoute('/farmers/7/trainings')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Record training' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: soil.name })
    await user.selectOptions(within(dialog).getByLabelText('Training'), soil.name)
    const date = within(dialog).getByLabelText('Completed on')
    await user.clear(date)
    await user.type(date, '2099-01-01')
    await user.click(within(dialog).getByRole('button', { name: 'Record' }))

    expect(await within(dialog).findByText('The completion date cannot be in the future.')).toBeInTheDocument()
    expect(farmersApi.recordFarmerTraining).not.toHaveBeenCalled()
  })

  it('shows the backend message when the training is already recorded (409)', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.recordFarmerTraining).mockRejectedValue(
      new ApiError(409, "Training 'Soil and land preparation' is already recorded for this farmer."),
    )
    renderRoute('/farmers/7/trainings')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Record training' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: soil.name })
    await user.selectOptions(within(dialog).getByLabelText('Training'), soil.name)
    await user.click(within(dialog).getByRole('button', { name: 'Record' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('already recorded for this farmer')
  })

  it('checks the missing prerequisites of a programme of the farmer’s district', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(farmersApi.getMissingPrerequisites).mockResolvedValue([pests])
    renderRoute('/farmers/7/prerequisites')
    const user = userEvent.setup()
    await screen.findByRole('option', { name: 'Maize' })
    expect(referenceApi.listProgrammes).toHaveBeenCalledWith(expect.anything(), 15)
    await user.selectOptions(screen.getByLabelText('Crop programme'), 'Maize')

    expect(await screen.findByText(/still needs this training to enrol in Maize/)).toBeInTheDocument()
    expect(within(screen.getByRole('list', { name: 'Missing trainings' })).getByRole('listitem')).toHaveTextContent(
      pests.name,
    )
    expect(farmersApi.getMissingPrerequisites).toHaveBeenCalledWith(7, 30)
  })

  it('says "None missing" when every prerequisite is complete', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.getMissingPrerequisites).mockResolvedValue([])
    renderRoute('/farmers/7/prerequisites')
    const user = userEvent.setup()
    await screen.findByRole('option', { name: 'Maize' })
    await user.selectOptions(screen.getByLabelText('Crop programme'), 'Maize')
    expect(await screen.findByText(/None missing/)).toBeInTheDocument()
  })
})

describe('Farmer results (FR-FAR-09)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(farmersApi.getFarmer).mockResolvedValue(uwase)
  })

  it('lists the results the backend returns for this role, read-only', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(
      paged([
        {
          id: 40,
          enrolmentId: 22,
          offeringId: 9,
          programmeName: 'Maize',
          seasonName: '2026A',
          farmerId: 7,
          membershipNumber: uwase.membershipNumber,
          farmerName: uwase.fullName,
          status: 'PUBLISHED',
          finalScore: 82.5,
          grade: 'A',
          publishedAt: '2026-10-30T10:00:00Z',
        },
      ]),
    )
    renderRoute('/farmers/7/results')
    expect(await screen.findByRole('cell', { name: '82.5' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Published' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'A' })).toBeInTheDocument()
    await waitFor(() =>
      expect(farmersApi.listFarmerResults).toHaveBeenCalledWith(7, {
        page: 0,
        size: 20,
        sortBy: 'id',
        direction: 'desc',
      }),
    )
  })
})
