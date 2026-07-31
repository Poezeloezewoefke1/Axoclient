import { describe, expect, it } from 'vitest'
import { redactSecrets } from '../src/main/logger'

/**
 * Regression guard for the credential leak found in a user-supplied
 * launcher.log: MCLC echoed the whole java command line, access token
 * included, and the file was then shared for crash diagnosis.
 */
describe('redactSecrets', () => {
  const jwt =
    'eyJraWQiOiIwNDkxODEiLCJhbGciOiJSUzI1NiJ9.eyJ4dWlkIjoiMjUzNTQzMzcwODQ2MjkxMyJ9.EdBadcVuuXHh5aOxUAU79rRRoinRTiuGceR9VD5a7Ic'

  it('removes the access token from an MCLC launch line', () => {
    const line = `[MCLC]: Launching with arguments --username Pyro_blitz --uuid 3bee2adf1eae45209273f35c596a29ab --accessToken ${jwt} --clientId ed87d6-10c7e4f --xuid 2535433708462913`
    const out = redactSecrets(line)

    expect(out).not.toContain(jwt)
    expect(out).not.toContain('2535433708462913')
    expect(out).not.toContain('3bee2adf1eae45209273f35c596a29ab')
    expect(out).toContain('--accessToken <redacted>')
    expect(out).toContain('--xuid <redacted>')
    // Non-secret context is preserved so the log stays useful.
    expect(out).toContain('--username Pyro_blitz')
  })

  it('handles --accessToken=value as well as space-separated', () => {
    expect(redactSecrets(`--accessToken=${jwt}`)).toBe('--accessToken=<redacted>')
  })

  it('redacts bearer headers and bare JWTs', () => {
    expect(redactSecrets(`Authorization: Bearer ${jwt}`)).toBe('Authorization: Bearer <redacted>')
    expect(redactSecrets(`token was ${jwt} here`)).toBe('token was <redacted-jwt> here')
  })

  it('leaves ordinary log lines untouched', () => {
    const line = 'pipeline start: 1.21.11-r1 for Pyro_blitz'
    expect(redactSecrets(line)).toBe(line)
  })
})
