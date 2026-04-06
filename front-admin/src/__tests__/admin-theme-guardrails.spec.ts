import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('admin visual guardrails', () => {
  it('defines shared admin tokens and bans gradients/glass effects', () => {
    const tokens = readSource('styles/admin-tokens.css')
    const theme = readSource('styles/admin-theme.css')

    expect(tokens).toContain('--admin-bg-canvas')
    expect(tokens).toContain('--admin-radius-panel: 8px')
    expect(theme).toContain('.admin-page')
    expect(`${tokens}\n${theme}`).not.toMatch(/gradient/i)
    expect(`${tokens}\n${theme}`).not.toMatch(/backdrop-filter/i)
  })
})
