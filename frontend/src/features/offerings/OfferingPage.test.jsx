import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as offeringsApi from '../../api/offerings'
import { logInAs, USERS } from '../../test/farmers'
import { renderRoute } from '../../test/renderRoute'
import { fullOffering, huyeOfficer, huyeOfficer2, maizeOffering } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/offerings')

async function openEdit(user) {
  await user.click(await screen.findByRole('button', { name: 'Edit' }))
  const dialog = screen.getByRole('dialog', { name: 'Edit Maize, 2026B' })
  await within(dialog).findByRole('option', { name: 'huye.officer2' })
  return dialog
}

describe('OfferingPage (FR-OFF-04, FR-OFF-05)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(maizeOffering)
    vi.mocked(offeringsApi.listFieldOfficers).mockResolvedValue([huyeOfficer, huyeOfficer2])
  })

  it('shows the overview, read-only for a Field Officer', async () => {
    logInAs(USERS.officer)
    renderRoute('/offerings/12')
    expect(await screen.findByRole('heading', { name: 'Maize, 2026B', level: 1 })).toBeInTheDocument()
    // the Overview tab is downloaded on demand, after the page heading
    expect(await screen.findByRole('link', { name: '2026B' })).toHaveAttribute('href', '/seasons/3')
    expect(screen.getByText('12 / 40')).toBeInTheDocument()
    expect(screen.getByText('0.5 ha')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('shows a full offering with a Full badge', async () => {
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(fullOffering)
    logInAs(USERS.registrar)
    renderRoute('/offerings/13')
    expect(await screen.findByText('Full')).toBeInTheDocument()
  })

  it('locks the hectares once farmers have enrolled, and sends them unchanged (S3)', async () => {
    logInAs(USERS.registrar)
    vi.mocked(offeringsApi.updateOffering).mockResolvedValue({
      ...maizeOffering,
      fieldOfficerId: 8,
      fieldOfficerUsername: 'huye.officer2',
      maxParticipants: 50,
      remainingPlaces: 38,
    })
    renderRoute('/offerings/12')
    const user = userEvent.setup()
    const dialog = await openEdit(user)
    const hectares = within(dialog).getByLabelText('Hectares per participant')
    expect(hectares).toHaveAttribute('readonly')
    expect(within(dialog).queryByText(/12 farmer\(s\) already enrolled/)).not.toBeInTheDocument()
    await user.type(hectares, '9') // the attempt shows the warning and changes nothing
    expect(within(dialog).getByText(/12 farmer\(s\) already enrolled/)).toBeInTheDocument()
    expect(hectares).toHaveValue(0.5)
    expect(within(dialog).getByLabelText('Field Officer')).toHaveValue('5')

    await user.selectOptions(within(dialog).getByLabelText('Field Officer'), 'huye.officer2')
    const max = within(dialog).getByLabelText('Maximum participants')
    await user.clear(max)
    await user.type(max, '50')
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Offering saved')).toBeInTheDocument()
    expect(offeringsApi.updateOffering).toHaveBeenCalledWith(12, {
      fieldOfficerId: 8,
      hectaresPerParticipant: 0.5,
      maxParticipants: 50,
    })
  })

  it('lets the hectares change while nobody has enrolled', async () => {
    vi.mocked(offeringsApi.getOffering).mockResolvedValue({ ...maizeOffering, enrolledCount: 0, remainingPlaces: 40 })
    logInAs(USERS.registrar)
    renderRoute('/offerings/12')
    const dialog = await openEdit(userEvent.setup())
    expect(within(dialog).getByLabelText('Hectares per participant')).not.toHaveAttribute('readonly')
  })

  it('shows the backend refusal for a maximum below the enrolled count (409)', async () => {
    logInAs(USERS.registrar)
    vi.mocked(offeringsApi.updateOffering).mockRejectedValue(
      new ApiError(409, 'maxParticipants cannot be 10: 12 farmers are already enrolled.'),
    )
    renderRoute('/offerings/12')
    const user = userEvent.setup()
    const dialog = await openEdit(user)
    const max = within(dialog).getByLabelText('Maximum participants')
    await user.clear(max)
    await user.type(max, '10')
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('12 farmers are already enrolled')
  })

  it('keeps the current Field Officer selectable when no longer in the list', async () => {
    vi.mocked(offeringsApi.listFieldOfficers).mockResolvedValue([huyeOfficer2])
    logInAs(USERS.registrar)
    renderRoute('/offerings/12')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog')
    expect(
      await within(dialog).findByRole('option', { name: 'demo.officer (no longer available)' }),
    ).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Field Officer')).toHaveValue('5')
  })

  it('shows "Not available to you" for an offering of another district (403)', async () => {
    vi.mocked(offeringsApi.getOffering).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    logInAs(USERS.officer)
    renderRoute('/offerings/99')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
