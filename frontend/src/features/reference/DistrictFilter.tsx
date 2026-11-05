import { useAuth } from '../../auth/useAuth'
import { Select } from '../../components/ui/Select'
import type { District } from '../../types/reference'

interface DistrictFilterProps {
  value: string
  onChange: (value: string) => void
  districts: District[] | undefined
}

/**
 * District filter for cooperatives and programmes. District staff (users with a district: Field Officers,
 * Agronomists) are fixed to their own district by the backend, which refuses any other one with 403,
 * so for them the filter is shown but locked.
 */
export function DistrictFilter({ value, onChange, districts }: DistrictFilterProps) {
  const { user } = useAuth()
  const ownDistrict = user?.districtId ?? null
  return (
    <div className="max-w-xs">
      <Select
        label="District"
        value={ownDistrict !== null ? String(ownDistrict) : value}
        disabled={ownDistrict !== null}
        hint={ownDistrict !== null ? 'You see your own district only.' : undefined}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">All districts</option>
        {districts?.map((d) => (
          <option key={d.id} value={d.id}>
            {d.name}
          </option>
        ))}
      </Select>
    </div>
  )
}
