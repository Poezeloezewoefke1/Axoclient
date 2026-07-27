import { logLine } from './logger'
import type { NewsItem } from '../shared/types'

/**
 * News panel content, sourced from the project's GitHub Releases so it can
 * never go stale the way hand-written copy does. Releases are public, so this
 * needs no token. Failure is silent — an empty list simply hides the panel
 * rather than blocking the launcher on a network hiccup.
 */

const RELEASES_URL = 'https://api.github.com/repos/Poezeloezewoefke1/Axoclient/releases?per_page=6'
const CACHE_MS = 15 * 60 * 1000

interface GitHubRelease {
  name?: string | null
  tag_name?: string | null
  body?: string | null
  published_at?: string | null
  draft?: boolean
  prerelease?: boolean
  html_url?: string | null
}

let cache: { at: number; items: NewsItem[] } | null = null

/**
 * Reduce a release body to a couple of readable lines: strip markdown
 * bullets/headings and drop the boilerplate footers that release tooling adds.
 */
export function summarizeBody(body: string | null | undefined, maxLength = 220): string {
  if (!body) {
    return ''
  }
  const lines = body
    .split(/\r?\n/)
    .map((line) => line.replace(/^\s*[-*+]\s+/, '').replace(/^#+\s*/, '').trim())
    .filter(
      (line) =>
        line.length > 0 &&
        !/^\*\*Full Changelog\*\*/i.test(line) &&
        !/^https?:\/\/\S+$/.test(line) &&
        !/^<!--/.test(line)
    )
  const text = lines.join(' ').replace(/\s+/g, ' ').trim()
  return text.length > maxLength ? `${text.slice(0, maxLength - 1).trimEnd()}…` : text
}

/** Map the GitHub payload to the renderer's shape, newest first. */
export function toNewsItems(releases: GitHubRelease[]): NewsItem[] {
  return releases
    .filter((release) => !release.draft)
    .map((release) => ({
      title: (release.name || release.tag_name || 'Update').trim(),
      body: summarizeBody(release.body),
      tag: release.prerelease ? 'Beta' : 'Release',
      date: release.published_at ?? undefined,
      url: release.html_url ?? undefined
    }))
}

export async function getNews(): Promise<NewsItem[]> {
  if (cache && Date.now() - cache.at < CACHE_MS) {
    return cache.items
  }
  try {
    const response = await fetch(RELEASES_URL, {
      headers: { Accept: 'application/vnd.github+json' }
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const items = toNewsItems((await response.json()) as GitHubRelease[])
    cache = { at: Date.now(), items }
    return items
  } catch (error) {
    logLine('news', `fetch failed: ${error instanceof Error ? error.message : error}`)
    return cache?.items ?? []
  }
}
