import { describe, expect, it } from 'vitest'
import {
  emptyStore,
  findAccount,
  parseStore,
  removeAccount,
  serializeStore,
  toAccountInfos,
  upsertAccount,
  type AccountStore,
  type StoredAccount
} from '../src/main/accountStore'

const alice: StoredAccount = { uuid: 'uuid-a', username: 'Alice', refresh: 'tok-a' }
const bob: StoredAccount = { uuid: 'uuid-b', username: 'Bob', refresh: 'tok-b' }

function storeWith(...accounts: StoredAccount[]): AccountStore {
  return accounts.reduce<AccountStore>((acc, account) => upsertAccount(acc, account), emptyStore())
}

describe('parseStore', () => {
  it('returns an empty store for missing data', () => {
    expect(parseStore(null)).toEqual(emptyStore())
  })

  it('returns an empty store for corrupt JSON', () => {
    expect(parseStore('{not json')).toEqual(emptyStore())
  })

  it('returns an empty store when accounts is not an array', () => {
    expect(parseStore(JSON.stringify({ activeUuid: 'x', accounts: 'nope' }))).toEqual(emptyStore())
  })

  it('drops malformed account entries', () => {
    const raw = JSON.stringify({
      activeUuid: 'uuid-a',
      accounts: [alice, { uuid: '', username: 'X', refresh: 'y' }, { nonsense: true }, null]
    })
    const store = parseStore(raw)
    expect(store.accounts).toEqual([alice])
    expect(store.activeUuid).toBe('uuid-a')
  })

  it('round-trips a serialized store', () => {
    const store = storeWith(alice, bob)
    expect(parseStore(serializeStore(store))).toEqual(store)
  })

  it('repairs an activeUuid that names no stored account', () => {
    const raw = JSON.stringify({ activeUuid: 'ghost', accounts: [alice, bob] })
    expect(parseStore(raw).activeUuid).toBe('uuid-a')
  })

  it('leaves activeUuid null when there are no accounts', () => {
    expect(parseStore(JSON.stringify({ activeUuid: 'ghost', accounts: [] })).activeUuid).toBeNull()
  })
})

describe('upsertAccount', () => {
  it('adds a new account and makes it active', () => {
    const store = upsertAccount(emptyStore(), alice)
    expect(store.accounts).toEqual([alice])
    expect(store.activeUuid).toBe('uuid-a')
  })

  it('keeps existing accounts when adding another', () => {
    const store = storeWith(alice, bob)
    expect(store.accounts).toHaveLength(2)
    expect(store.activeUuid).toBe('uuid-b')
  })

  it('updates a stored account in place rather than duplicating it', () => {
    const renamed = { ...alice, username: 'Alice2', refresh: 'tok-a2' }
    const store = upsertAccount(storeWith(alice, bob), renamed)
    expect(store.accounts).toHaveLength(2)
    expect(findAccount(store, 'uuid-a')).toEqual(renamed)
    expect(store.activeUuid).toBe('uuid-a')
  })

  it('does not mutate the input store', () => {
    const original = storeWith(alice)
    upsertAccount(original, bob)
    expect(original.accounts).toEqual([alice])
    expect(original.activeUuid).toBe('uuid-a')
  })
})

describe('removeAccount', () => {
  it('removing an inactive account leaves the active one alone', () => {
    const store = removeAccount(storeWith(bob, alice), 'uuid-b')
    expect(store.accounts).toEqual([alice])
    expect(store.activeUuid).toBe('uuid-a')
  })

  it('removing the active account promotes the next one', () => {
    const store = removeAccount(storeWith(alice, bob), 'uuid-b')
    expect(store.activeUuid).toBe('uuid-a')
    expect(store.accounts).toEqual([alice])
  })

  it('removing the last account clears the active session', () => {
    const store = removeAccount(storeWith(alice), 'uuid-a')
    expect(store.accounts).toEqual([])
    expect(store.activeUuid).toBeNull()
  })

  it('ignores an unknown uuid', () => {
    const before = storeWith(alice, bob)
    expect(removeAccount(before, 'uuid-zzz')).toEqual(before)
  })

  it('does not mutate the input store', () => {
    const original = storeWith(alice, bob)
    removeAccount(original, 'uuid-b')
    expect(original.accounts).toHaveLength(2)
  })
})

describe('toAccountInfos', () => {
  it('flags exactly one account as active and omits refresh tokens', () => {
    const infos = toAccountInfos(storeWith(alice, bob))
    expect(infos.filter((a) => a.active)).toHaveLength(1)
    expect(infos.find((a) => a.active)?.username).toBe('Bob')
    expect(JSON.stringify(infos)).not.toContain('tok-')
  })

  it('flags nothing active for an empty store', () => {
    expect(toAccountInfos(emptyStore())).toEqual([])
  })
})
