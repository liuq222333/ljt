import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const files = [
  'components/governance/GovernanceReplayCenter.vue',
  'components/governance/GovernanceEvalCenter.vue',
  'components/governance/GovernanceReleaseCenter.vue',
  'components/governance/GovernanceDiagnosticsCenter.vue',
]

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('governance workbench pages', () => {
  it('use shared admin workbench classes and remove legacy visual effects', () => {
    for (const file of files) {
      const source = readSource(file)
      expect(source).toContain('class="admin-page')
      expect(source).toMatch(/admin-toolbar|AdminToolbar/)
      expect(source).toMatch(/admin-panel|AdminPanel/)
      expect(source).not.toMatch(/linear-gradient|radial-gradient|backdrop-filter|translateY|translateX/)
    }
  })
})
