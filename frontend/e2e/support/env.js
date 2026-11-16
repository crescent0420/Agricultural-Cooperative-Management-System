import { existsSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * Settings for the end-to-end tests, read from the backend's .env (one folder above "frontend"), so no
 * password is typed into a command or stored in the frontend. An environment variable of the same name wins.
 */
const here = dirname(fileURLToPath(import.meta.url))
const envFile = join(here, '..', '..', '..', '.env')

function readDotEnv() {
  if (!existsSync(envFile)) return {}
  const values = {}
  for (const line of readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/)
    if (match) values[match[1]] = match[2].replace(/^["']|["']$/g, '')
  }
  return values
}

const dotEnv = readDotEnv()
const setting = (name, fallback) => process.env[name] ?? dotEnv[name] ?? fallback

/** The password of every demo account (DEMO_SEED_PASSWORD). */
export const DEMO_PASSWORD = setting('DEMO_SEED_PASSWORD', '')
/**
 * The backend, called directly by the tests to prepare data and check results. E2E_API_URL points the tests at
 * a deployed system instead (docs/deployment.md, "Test the deployed build").
 */
export const API_URL = setting('E2E_API_URL', `http://localhost:${setting('SERVER_PORT', '8081')}/api`)
/** Mailpit's web inbox and API (compose.yaml). */
export const MAILPIT_URL = setting('E2E_MAILPIT_URL', 'http://localhost:8025')
export const ENV_FILE = envFile
