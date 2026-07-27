import { describe, expect, it } from 'vitest'
import { summarizeBody, toNewsItems } from '../src/main/news'

describe('summarizeBody', () => {
  it('flattens markdown bullets into a readable sentence', () => {
    expect(summarizeBody('- Added capes\n- Fixed a crash')).toBe('Added capes Fixed a crash')
  })

  it('strips headings', () => {
    expect(summarizeBody('## What changed\n- Skin changer')).toBe('What changed Skin changer')
  })

  it('drops the auto-generated changelog footer and bare links', () => {
    const body = '- Real change\n\n**Full Changelog**: v1...v2\nhttps://github.com/x/y/compare/v1...v2'
    expect(summarizeBody(body)).toBe('Real change')
  })

  it('truncates long bodies with an ellipsis', () => {
    const summary = summarizeBody('x'.repeat(500), 50)
    expect(summary).toHaveLength(50)
    expect(summary.endsWith('…')).toBe(true)
  })

  it('returns an empty string for missing bodies', () => {
    expect(summarizeBody(null)).toBe('')
    expect(summarizeBody(undefined)).toBe('')
    expect(summarizeBody('')).toBe('')
  })
})

describe('toNewsItems', () => {
  it('maps a release to the renderer shape', () => {
    const items = toNewsItems([
      {
        name: 'Axo 0.2.0',
        tag_name: 'launcher-v0.2.0',
        body: '- Added the skin changer',
        published_at: '2026-07-27T12:00:00Z',
        html_url: 'https://example.invalid/r/1'
      }
    ])
    expect(items).toEqual([
      {
        title: 'Axo 0.2.0',
        body: 'Added the skin changer',
        tag: 'Release',
        date: '2026-07-27T12:00:00Z',
        url: 'https://example.invalid/r/1'
      }
    ])
  })

  it('hides drafts, which are not public yet', () => {
    const items = toNewsItems([
      { name: 'Secret', draft: true },
      { name: 'Public' }
    ])
    expect(items.map((i) => i.title)).toEqual(['Public'])
  })

  it('labels pre-releases as Beta', () => {
    expect(toNewsItems([{ name: 'RC', prerelease: true }])[0].tag).toBe('Beta')
  })

  it('falls back to the tag, then a generic title', () => {
    expect(toNewsItems([{ tag_name: 'launcher-v0.1.0' }])[0].title).toBe('launcher-v0.1.0')
    expect(toNewsItems([{}])[0].title).toBe('Update')
  })

  it('handles an empty release list', () => {
    expect(toNewsItems([])).toEqual([])
  })
})
