import { NavLink } from 'react-router'
import { LazyOutlet } from '../../components/layout/LazyOutlet'
import { PageHeader } from '../../components/layout/PageHeader'

const TABS = [
  { to: 'districts', label: 'Districts' },
  { to: 'cooperatives', label: 'Cooperatives' },
  { to: 'programmes', label: 'Crop programmes' },
  { to: 'trainings', label: 'Trainings' },
]

/** /reference: the four reference lists as tabs (URD FR-REF-01..06). */
export function ReferencePage() {
  return (
    <>
      <PageHeader
        title="Reference data"
        description="Districts, cooperatives, crop programmes and trainings used across ACMS."
      />
      <nav aria-label="Reference data" className="mb-5 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              `-mb-px border-b-2 px-4 py-2 text-sm font-semibold whitespace-nowrap ${
                isActive ? 'border-brand-600 text-brand-700' : 'border-transparent text-muted hover:text-ink'
              }`
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <LazyOutlet />
    </>
  )
}
