// The same input rules as ProgrammeOfferingRequest / ProgrammeOfferingUpdateRequest. The backend checks them
// again, together with the business rules the form leaves to it (S4): the season's land limit, an ended season,
// and a Field Officer of another district.

/** @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) */
export const hectaresRules = {
  required: 'Enter the hectares per participant.',
  validate: (v) =>
    (/^\d{1,4}(\.\d{1,2})?$/.test(String(v)) && Number(v) >= 0.01) ||
    'Enter at least 0.01, with at most 4 digits before the point and 2 after.',
}

/** @Min(1) @Max(100000), a whole number */
export const maxParticipantsRules = {
  required: 'Enter the maximum number of participants.',
  validate: (v) =>
    (/^\d+$/.test(String(v)) && Number(v) >= 1 && Number(v) <= 100000) || 'Enter a whole number from 1 to 100,000.',
}

/** S5: the fields the offering lists can sort by (ProgrammeOfferingService.SORTABLE_FIELDS); not by name. */
export const OFFERING_SORTABLE = ['id', 'createdAt', 'maxParticipants', 'enrolledCount', 'hectaresPerParticipant']
