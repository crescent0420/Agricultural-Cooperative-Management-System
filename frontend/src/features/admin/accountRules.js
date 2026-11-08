import { ROLE_LABELS } from '../../types/auth'

/** The roles in the order the forms list them. */
export const ROLES = Object.keys(ROLE_LABELS)

/**
 * What a role is attached to, as UserService.resolveScope requires: Field Officers and Agronomists to a
 * district, a farmer account to a farmer record, every other role to nothing.
 * @returns {'district' | 'farmer' | 'none'}
 */
export function roleScope(role) {
  if (role === 'FIELD_OFFICER' || role === 'DISTRICT_AGRONOMIST') return 'district'
  if (role === 'FARMER') return 'farmer'
  return 'none'
}

/** The body for POST /users and PATCH /users/{id}/role: only the attachment the role takes is sent. */
export function scopeFields(role, districtId, farmer) {
  const scope = roleScope(role)
  return {
    districtId: scope === 'district' ? Number(districtId) : null,
    farmerId: scope === 'farmer' ? (farmer?.id ?? null) : null,
  }
}

/** Same pattern as CreateUserRequest. */
export const USERNAME_RULES = {
  required: 'Enter a username.',
  pattern: {
    value: /^[a-z0-9._-]{3,50}$/,
    message: '3 to 50 characters: lowercase letters, digits, dot, underscore or hyphen.',
  },
}

export const EMAIL_RULES = {
  required: 'Enter an email address.',
  maxLength: { value: 254, message: 'The email address is too long.' },
  pattern: { value: /^[^\s@]+@[^\s@]+$/, message: 'Enter a valid email address.' },
}

/**
 * Which form field a 409 belongs to, from the backend's message ("Username is already in use.",
 * "Email address is already in use.", "This farmer already has a login account."); null = show it on top.
 */
export function conflictField(message) {
  if (/username/i.test(message)) return 'username'
  if (/email/i.test(message)) return 'email'
  if (/farmer/i.test(message)) return 'farmer'
  return null
}

/** Decision A1 (7 Nov 2026): the admin forms always link a farmer account to its farmer record. */
export const FARMER_REQUIRED = 'Choose the farmer record this account belongs to.'

/** LOGIN_FAILURE → "Login failure". Works for event types added later too. */
export function humanize(code) {
  if (!code) return '—'
  const words = code.toLowerCase().replaceAll('_', ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}
