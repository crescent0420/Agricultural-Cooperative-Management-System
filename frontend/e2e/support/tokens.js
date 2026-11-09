/**
 * The same token with one character of its signature changed. The FIRST signature character is changed:
 * the last one also holds 2 unused filler bits, so A and B there decode to the same signature (a test that
 * changed the last character passed or failed depending on the token).
 */
export function tamper(token) {
  const at = token.lastIndexOf('.') + 1
  const changed = token[at] === 'A' ? 'B' : 'A'
  return token.slice(0, at) + changed + token.slice(at + 1)
}
