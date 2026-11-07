/** amountDue / newAmountDue / amount: @DecimalMin("0.01") @Digits(integer = 10, fraction = 2). */
export const amountRules = (label = 'the amount') => ({
  required: `Enter ${label}.`,
  validate: (v) =>
    (/^\d{1,10}(\.\d{1,2})?$/.test(String(v).trim()) && Number(v) >= 0.01) ||
    'Enter at least 0.01, with at most 10 digits before the point and 2 after.',
})

/** The fields the contribution list can sort by (ContributionService.SORTABLE_FIELDS). */
export const CONTRIBUTION_SORTABLE = ['id', 'createdAt', 'amountDue']

/** The fields a contribution's payments can sort by (PaymentService.SORTABLE_FIELDS). */
export const PAYMENT_SORTABLE = ['id', 'requestedAt', 'amount', 'status']

export const METHOD_LABELS = { MOBILE_MONEY: 'Mobile money', BANK_TRANSFER: 'Bank transfer' }
