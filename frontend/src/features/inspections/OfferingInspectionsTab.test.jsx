import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as inspectionsApi from '../../api/inspections'
import * as offeringsApi from '../../api/offerings'
import { logInAs, USERS } from '../../test/farmers'
import { renderRoute } from '../../test/renderRoute'
import { completeList, soil } from '../../test/results'
import { maizeOffering } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/inspections')
vi.mock('../../api/offerings')

const halfList = { ...completeList, inspections: [soil], totalWeight: 40, weightsComplete: false }

describe('Offering page, Inspections tab (FR-INS-01, FR-INS-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(maizeOffering)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(completeList)
  })

  it('shows the inspections with the weights’ total, and the tools to the assigned Field Officer', async () => {
    logInAs(USERS.officer) // id 5 = maizeOffering.fieldOfficerId
    renderRoute('/offerings/12/inspections')
    expect(await screen.findByRole('cell', { name: 'Soil and land preparation' })).toBeInTheDocument()
    expect(screen.getByText('100%')).toBeInTheDocument()
    expect(screen.getByText('Complete')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add inspection' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Soil and land preparation' })).toBeInTheDocument()
    expect(inspectionsApi.listInspections).toHaveBeenCalledWith(12)
  })

  it('is read-only for the Agronomist', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(halfList)
    renderRoute('/offerings/12/inspections')
    expect(await screen.findByText('Not complete')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add inspection' })).not.toBeInTheDocument()
  })

  it('adds an inspection, showing how much weight is free', async () => {
    logInAs(USERS.officer)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(halfList)
    vi.mocked(inspectionsApi.createInspection).mockResolvedValue({
      id: 103,
      offeringId: 12,
      name: 'Crop husbandry',
      maxScore: 10,
      weight: 60,
    })
    renderRoute('/offerings/12/inspections')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Add inspection' }))
    const dialog = screen.getByRole('dialog', { name: 'Add an inspection' })
    expect(within(dialog).getByText('60% of the 100% is still free.')).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText('Name'), ' Crop husbandry ')
    await user.type(within(dialog).getByLabelText('Maximum score'), '10')
    await user.type(within(dialog).getByLabelText('Weight (%)'), '60')
    await user.click(within(dialog).getByRole('button', { name: 'Add' }))
    expect(await screen.findByText('Inspection Crop husbandry saved')).toBeInTheDocument()
    expect(inspectionsApi.createInspection).toHaveBeenCalledWith(12, {
      name: 'Crop husbandry',
      maxScore: 10,
      weight: 60,
    })
  })

  it('shows the backend refusal when the weights would pass 100% (422)', async () => {
    logInAs(USERS.officer)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(halfList)
    vi.mocked(inspectionsApi.createInspection).mockRejectedValue(
      new ApiError(
        422,
        "The weights of an offering's inspections must add up to 100%; this change would make them 110%.",
      ),
    )
    renderRoute('/offerings/12/inspections')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Add inspection' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText('Name'), 'Crop husbandry')
    await user.type(within(dialog).getByLabelText('Maximum score'), '10')
    await user.type(within(dialog).getByLabelText('Weight (%)'), '70')
    await user.click(within(dialog).getByRole('button', { name: 'Add' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('would make them 110%')
  })

  it('says why nothing can change once frozen, only when a change is tried', async () => {
    logInAs(USERS.officer)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue({ ...completeList, frozen: true })
    renderRoute('/offerings/12/inspections')
    const user = userEvent.setup()
    const add = await screen.findByRole('button', { name: 'Add inspection' })
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await user.click(add)
    expect(screen.getByRole('alert')).toHaveTextContent('can no longer change')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('deletes after a confirmation, and shows a refusal for an inspection with scores (409)', async () => {
    logInAs(USERS.officer)
    vi.mocked(inspectionsApi.deleteInspection).mockRejectedValue(
      new ApiError(409, "Scores have already been entered for inspection 'Soil and land preparation'."),
    )
    renderRoute('/offerings/12/inspections')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Delete Soil and land preparation' }))
    const dialog = screen.getByRole('dialog', { name: 'Delete Soil and land preparation?' })
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Scores have already been entered')
    expect(inspectionsApi.deleteInspection).toHaveBeenCalledWith(101)
  })
})
