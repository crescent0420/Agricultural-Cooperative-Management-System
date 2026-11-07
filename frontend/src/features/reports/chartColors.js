// Chart colours, checked with the dataviz validator against the white card (light mode; the app has no dark mode):
//   node scripts/validate_palette.js "#2a78d6,#eb6834,#1baf7a,#eda100" --mode light --surface "#ffffff"
// All checks pass for stacked (adjacent) use. Aqua and yellow are below 3:1 against white, so every chart
// is followed by its full table (the "relief" the validator asks for). Colours are given in a fixed order and
// follow the thing they show (Active is always blue), never its rank.
export const SERIES = {
  blue: '#2a78d6',
  orange: '#eb6834',
  aqua: '#1baf7a',
  yellow: '#eda100',
}

/** A remainder that is not a measure of its own (places still free): a quiet grey. */
export const REMAINDER = '#c3c2b7'

export const CHROME = {
  grid: '#e1e0d9',
  axis: '#c3c2b7',
  tick: '#51655f', // the app's muted text colour
}

/** The chart shows at most this many bars; the table under it always lists every row. */
export const CHART_LIMIT = 15

/** "The chart shows 15 of 40 cooperatives; …" or nothing when every row fits. */
export function limitNote(total, what) {
  return total > CHART_LIMIT
    ? `The chart shows ${CHART_LIMIT} of ${total} ${what}; the table below lists all of them.`
    : undefined
}
