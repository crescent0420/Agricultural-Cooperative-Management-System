import { request } from '@playwright/test'
import { API_URL, DEMO_PASSWORD } from './env'

/**
 * Calls the backend directly, as a given user: to prepare data a test needs, and to check what a screen
 * action really stored. Every answer is the backend's envelope { success, message, data | error }.
 */
export class Api {
  constructor(context, token, username) {
    this.context = context
    this.token = token
    this.username = username
  }

  /** Logs in with the demo password (or the one given) and returns an Api for that user. */
  static async as(username, password = DEMO_PASSWORD) {
    const context = await request.newContext()
    const response = await context.post(`${API_URL}/auth/login`, { data: { username, password } })
    const body = await response.json().catch(() => ({}))
    if (!response.ok()) {
      throw new Error(`Login as ${username} failed (${response.status()}): ${body.message ?? 'no message'}`)
    }
    return new Api(context, body.data.accessToken, username)
  }

  /** Without a token, for the 401 checks. */
  static async anonymous() {
    return new Api(await request.newContext(), null, null)
  }

  async call(method, path, { data, params } = {}) {
    const response = await this.context.fetch(`${API_URL}${path}`, {
      method,
      data,
      params,
      headers: this.token ? { Authorization: `Bearer ${this.token}` } : {},
    })
    const body = await response.json().catch(() => ({}))
    return { status: response.status(), body, data: body.data }
  }

  /** Like call(), but fails the test unless the answer is 2xx; returns the data. */
  async ok(method, path, options) {
    const answer = await this.call(method, path, options)
    if (answer.status < 200 || answer.status > 299) {
      throw new Error(`${method} ${path} as ${this.username}: ${answer.status} ${answer.body.message ?? ''}`)
    }
    return answer.data
  }

  get(path, params) {
    return this.ok('GET', path, { params })
  }

  post(path, data) {
    return this.ok('POST', path, { data })
  }

  patch(path, data) {
    return this.ok('PATCH', path, { data })
  }

  put(path, data) {
    return this.ok('PUT', path, { data })
  }

  async dispose() {
    await this.context.dispose()
  }
}
