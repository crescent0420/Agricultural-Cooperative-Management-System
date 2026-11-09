import { describe, expect, it } from 'vitest'
import eagerSource from './eagerPages.js?raw'
import lazySource from '../routes/lazyPages.js?raw'

/** "Name <- module path" for every page a file exports. */
const pages = (source, pattern) => [...source.matchAll(pattern)].map((m) => `${m[1]} <- ${m[2]}`).sort()

describe("eagerPages (the component tests' copy of lazyPages)", () => {
  it('lists exactly the same pages, from the same files, as src/routes/lazyPages.js', () => {
    const lazy = pages(lazySource, /export const (\w+) = named\(\s*\(\) => import\('([^']+)'\)/g)
    const eager = pages(eagerSource, /export \{ (\w+) \} from '([^']+)'/g)
    expect(lazy.length).toBeGreaterThan(0)
    expect(eager).toEqual(lazy)
  })
})
