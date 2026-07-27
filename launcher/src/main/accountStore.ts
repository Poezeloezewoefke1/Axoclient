import type { AccountInfo } from '../shared/types'

/**
 * Pure state logic for the multi-account store. Deliberately free of
 * electron/msmc imports so it can be unit-tested directly — auth.ts owns the
 * I/O (encryption, token refresh) and delegates every state transition here.
 * All functions return a new store rather than mutating in place.
 */

export interface StoredAccount {
  uuid: string
  username: string
  /** msmc refresh token (xboxManager.save()). Encrypted at rest by tokens.ts. */
  refresh: string
}

export interface AccountStore {
  activeUuid: string | null
  accounts: StoredAccount[]
}

export function emptyStore(): AccountStore {
  return { activeUuid: null, accounts: [] }
}

function isStoredAccount(value: unknown): value is StoredAccount {
  const account = value as StoredAccount | null
  return (
    typeof account === 'object' &&
    account !== null &&
    typeof account.uuid === 'string' &&
    account.uuid.length > 0 &&
    typeof account.username === 'string' &&
    typeof account.refresh === 'string'
  )
}

/**
 * Read a persisted store. Anything unparseable, malformed, or from an older
 * format yields an empty store — the user simply signs in again, which is
 * always preferable to crashing on startup.
 */
export function parseStore(raw: string | null): AccountStore {
  if (!raw) {
    return emptyStore()
  }
  try {
    const parsed = JSON.parse(raw) as Partial<AccountStore>
    if (!parsed || !Array.isArray(parsed.accounts)) {
      return emptyStore()
    }
    const accounts = parsed.accounts.filter(isStoredAccount)
    const activeUuid =
      typeof parsed.activeUuid === 'string' &&
      accounts.some((a) => a.uuid === parsed.activeUuid)
        ? parsed.activeUuid
        : (accounts[0]?.uuid ?? null)
    return { activeUuid, accounts }
  } catch {
    return emptyStore()
  }
}

export function serializeStore(store: AccountStore): string {
  return JSON.stringify(store)
}

/**
 * Add an account, or refresh the name/token of one already stored. The
 * upserted account always becomes active — this runs after a successful
 * sign-in or token refresh, which is exactly when it should take over.
 */
export function upsertAccount(store: AccountStore, account: StoredAccount): AccountStore {
  const existing = store.accounts.some((a) => a.uuid === account.uuid)
  const accounts = existing
    ? store.accounts.map((a) => (a.uuid === account.uuid ? { ...a, ...account } : a))
    : [...store.accounts, account]
  return { activeUuid: account.uuid, accounts }
}

/**
 * Drop an account. Removing the active one promotes the first remaining
 * account so the launcher still has someone to play as; removing the last
 * account leaves no active session.
 */
export function removeAccount(store: AccountStore, uuid: string): AccountStore {
  const accounts = store.accounts.filter((a) => a.uuid !== uuid)
  if (store.activeUuid !== uuid) {
    return { activeUuid: store.activeUuid, accounts }
  }
  return { activeUuid: accounts[0]?.uuid ?? null, accounts }
}

export function findAccount(store: AccountStore, uuid: string): StoredAccount | undefined {
  return store.accounts.find((a) => a.uuid === uuid)
}

/** Renderer-facing view: names + uuids only, never refresh tokens. */
export function toAccountInfos(store: AccountStore): AccountInfo[] {
  return store.accounts.map((a) => ({
    username: a.username,
    uuid: a.uuid,
    active: a.uuid === store.activeUuid
  }))
}
