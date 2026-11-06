import { Select } from '../../components/ui/Select'
import { useSeasonOptions } from '../seasons/useSeasonOptions'

/** A season dropdown for filtering lists, newest season first. */
export function SeasonFilter({ value, onChange, label = 'Season', allLabel = 'All seasons' }) {
  const seasons = useSeasonOptions()
  return (
    <Select label={label} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{allLabel}</option>
      {seasons.data?.content.map((s) => (
        <option key={s.id} value={s.id}>
          {s.name}
        </option>
      ))}
    </Select>
  )
}
