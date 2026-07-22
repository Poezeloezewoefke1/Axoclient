import { readFile } from 'node:fs/promises'
import { logLine } from './logger'
import type { SkinInfo } from '../shared/types'

/**
 * Minecraft skin read/change. Reading a player's current skin is public
 * (session server → texture URL); changing it needs the account's Minecraft
 * access token and hits the authenticated Minecraft Services API. Skins are
 * returned to the renderer as data URLs so the strict CSP (no remote images)
 * still lets us draw them.
 */

export type { SkinInfo }

interface ProfileTextures {
  textures?: {
    SKIN?: { url?: string; metadata?: { model?: string } }
  }
}

async function toDataUrl(url: string): Promise<string> {
  const res = await fetch(url)
  if (!res.ok) {
    throw new Error(`skin texture HTTP ${res.status}`)
  }
  const buf = Buffer.from(await res.arrayBuffer())
  return `data:image/png;base64,${buf.toString('base64')}`
}

/** Fetch the player's current skin by (undashed) UUID. Never throws upward. */
export async function getSkin(uuid: string): Promise<SkinInfo> {
  try {
    const clean = uuid.replace(/-/g, '')
    const res = await fetch(
      `https://sessionserver.mojang.com/session/minecraft/profile/${clean}`
    )
    if (!res.ok) {
      return { dataUrl: null, slim: false }
    }
    const json = (await res.json()) as { properties?: { name: string; value: string }[] }
    const prop = json.properties?.find((p) => p.name === 'textures')
    if (!prop) {
      return { dataUrl: null, slim: false }
    }
    const decoded = JSON.parse(Buffer.from(prop.value, 'base64').toString('utf8')) as ProfileTextures
    const skinUrl = decoded.textures?.SKIN?.url
    const slim = decoded.textures?.SKIN?.metadata?.model === 'slim'
    if (!skinUrl) {
      return { dataUrl: null, slim }
    }
    return { dataUrl: await toDataUrl(skinUrl), slim }
  } catch (error) {
    logLine('skin', `read failed: ${error instanceof Error ? error.message : String(error)}`)
    return { dataUrl: null, slim: false }
  }
}

/**
 * Upload a new skin PNG for the signed-in account. `variant` is "classic" or
 * "slim". Needs a valid Minecraft access token. Throws on failure so the UI
 * can surface it.
 */
export async function applySkin(
  accessToken: string,
  filePath: string,
  variant: 'classic' | 'slim'
): Promise<void> {
  const bytes = await readFile(filePath)
  const form = new FormData()
  form.append('variant', variant)
  form.append('file', new Blob([bytes], { type: 'image/png' }), 'skin.png')

  const res = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: form
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Skin change failed (HTTP ${res.status})${text ? `: ${text.slice(0, 200)}` : ''}`)
  }
  logLine('skin', `applied ${variant} skin from ${filePath}`)
}
