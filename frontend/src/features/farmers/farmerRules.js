// The same input rules as the backend's request DTOs, checked in the form so mistakes show before saving.
// The backend checks them again (400 with field errors).

/** FarmerRegistrationRequest / FarmerUpdateRequest.fullName: @NotBlank @Size(max = 150) */
export const fullNameRules = {
  validate: (value) => value.trim().length > 0 || 'Enter the full name.',
  maxLength: { value: 150, message: 'Use at most 150 characters.' },
}

/** FarmerRegistrationRequest.nationalId: @NotBlank @Pattern("\\d{16}") */
export const nationalIdRules = {
  required: 'Enter the national ID.',
  pattern: { value: /^\d{16}$/, message: 'The national ID has exactly 16 digits.' },
}

/** phoneNumber: optional, @Pattern("\\+?[0-9]{9,15}") */
export const phoneRules = {
  validate: (value) =>
    value.trim() === '' || /^\+?[0-9]{9,15}$/.test(value.trim()) || 'Use 9 to 15 digits, optionally starting with +.',
}

/** An empty phone is sent as null: the backend's pattern would refuse "". */
export function phoneOrNull(value) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
