import { Controller, useWatch } from 'react-hook-form'
import { Select } from '../../components/ui/Select'
import { ROLE_LABELS } from '../../types/auth'
import { FarmerPicker } from '../contributions/FarmerPicker'
import { useDistrictOptions } from '../reference/useReferenceOptions'
import { FARMER_REQUIRED, roleScope, ROLES } from './accountRules'

/**
 * Role, then what the role is attached to (FR-ADM-02, 03): a district for Field Officers and Agronomists,
 * the farmer record for a farmer account, nothing for the other roles. Form fields: role, districtId, farmer.
 * Each extra field checks itself only while its role is chosen, so switching roles never leaves a stale error.
 */
export function AccountRoleFields({ register, control, errors }) {
  const role = useWatch({ control, name: 'role' })
  const scope = roleScope(role)
  const districts = useDistrictOptions()

  return (
    <>
      <Select label="Role" error={errors.role?.message} {...register('role', { required: 'Choose a role.' })}>
        <option value="">Choose a role…</option>
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {ROLE_LABELS[r]}
          </option>
        ))}
      </Select>
      {scope === 'district' && (
        <Select
          label="District"
          hint={`A ${ROLE_LABELS[role]} works only with this district's farmers and offerings.`}
          error={errors.districtId?.message}
          {...register('districtId', {
            validate: (value, all) => roleScope(all.role) !== 'district' || Boolean(value) || 'Choose a district.',
          })}
        >
          <option value="">Choose a district…</option>
          {districts.data?.content.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </Select>
      )}
      {scope === 'farmer' && (
        <Controller
          name="farmer"
          control={control}
          rules={{ validate: (value, all) => roleScope(all.role) !== 'farmer' || value != null || FARMER_REQUIRED }}
          render={({ field }) => (
            <FarmerPicker
              label="Farmer record"
              value={field.value}
              onChange={field.onChange}
              error={errors.farmer?.message}
              hint="Search the registered farmers by name or membership number."
            />
          )}
        />
      )}
    </>
  )
}
