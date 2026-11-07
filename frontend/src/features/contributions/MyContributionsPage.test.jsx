import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contributionsApi from '../../api/contributions'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { contribution } from '../../test/payments'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/contributions')

describe('MyContributionsPage (FR-FIN-07)', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows the farmer’s balance per season and what it allows', async () => {
    logInAs(USERS.farmer)
    vi.mocked(contributionsApi.listMyContributions).mockResolvedValue(paged([contribution]))
    renderRoute('/my/contributions')
    expect(await screen.findByRole('link', { name: '2026B' })).toHaveAttribute('href', '/contributions/70')
    expect(screen.getByText('40%')).toBeInTheDocument()
    expect(screen.getByText('May enrol')).toBeInTheDocument()
    expect(screen.getByText('Cannot collect yet')).toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Farmer' })).not.toBeInTheDocument()
  })

  it('is for farmers only', async () => {
    logInAs(USERS.finance)
    renderRoute('/my/contributions')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
