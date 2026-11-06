import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import { ApiError } from '../../api/errors'
import * as referenceApi from '../../api/reference'
import { saveSession } from '../../auth/tokenStore'
import { renderRoute } from '../../test/renderRoute'
import { ROLE_PERMISSIONS } from '../../test/rolePermissions'
import { registrar } from '../../test/users'

vi.mock('../../api/auth')
vi.mock('../../api/reference')

const scale = [
  { grade: 'C', minScore: 0, description: 'Needs improvement' },
  { grade: 'A', minScore: 80, description: 'Excellent' },
  { grade: 'B', minScore: 60, description: null },
]

function logInAs(user) {
  saveSession('token', 1800)
  vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
}

describe('GradingScalePage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.getGradingScale).mockResolvedValue(scale)
    vi.mocked(referenceApi.getDistrict).mockResolvedValue({
      id: 15,
      name: 'Huye',
      provinceId: 4,
      provinceName: 'Southern Province',
    })
  })

  it('shows the bands from the highest score down, read-only for a Field Officer', async () => {
    logInAs({ ...registrar, role: 'FIELD_OFFICER', districtId: 15, permissions: ROLE_PERMISSIONS.FIELD_OFFICER })
    renderRoute('/grading-scale')
    await screen.findByRole('cell', { name: 'A' })
    const grades = screen
      .getAllByRole('row')
      .slice(1)
      .map((row) => within(row).getAllByRole('cell')[0].textContent)
    expect(grades).toEqual(['A', 'B', 'C'])
    expect(screen.queryByRole('button', { name: 'Edit scale' })).not.toBeInTheDocument()
  })

  it('lets the Registrar replace the scale', async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    vi.mocked(referenceApi.replaceGradingScale).mockImplementation(async (bands) => bands)
    renderRoute('/grading-scale')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit scale' }))
    const gradeA = screen.getAllByLabelText('Grade')[0]
    await user.clear(gradeA)
    await user.type(gradeA, 'A+')
    await user.click(screen.getByRole('button', { name: 'Save scale' }))

    expect(await screen.findByText('Grading scale saved')).toBeInTheDocument()
    expect(referenceApi.replaceGradingScale).toHaveBeenCalledWith([
      { grade: 'A+', minScore: 80, description: 'Excellent' },
      { grade: 'B', minScore: 60, description: null },
      { grade: 'C', minScore: 0, description: 'Needs improvement' },
    ])
  })

  it('refuses duplicate grades and a scale without a band at 0 before calling the API', async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    renderRoute('/grading-scale')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit scale' }))
    const grades = screen.getAllByLabelText('Grade')
    await user.clear(grades[1])
    await user.type(grades[1], 'A')
    await user.click(screen.getByRole('button', { name: 'Save scale' }))
    expect(await screen.findAllByText('Used twice.')).toHaveLength(2)

    await user.clear(grades[1])
    await user.type(grades[1], 'B')
    const minScores = screen.getAllByLabelText('From score')
    await user.clear(minScores[2])
    await user.type(minScores[2], '10')
    await user.click(screen.getByRole('button', { name: 'Save scale' }))
    expect(await screen.findByText('One band must start at 0, so that every score gets a grade.')).toBeInTheDocument()
    expect(referenceApi.replaceGradingScale).not.toHaveBeenCalled()
  })

  it('shows backend field errors under the right band', async () => {
    logInAs({ ...registrar, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR })
    vi.mocked(referenceApi.replaceGradingScale).mockRejectedValue(
      new ApiError(400, 'Validation failed', [
        { field: 'bands[1].description', message: 'description must be at most 100 characters' },
      ]),
    )
    renderRoute('/grading-scale')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit scale' }))
    await user.click(screen.getByRole('button', { name: 'Save scale' }))
    expect(await screen.findByText('description must be at most 100 characters')).toBeInTheDocument()
  })
})
