import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const files = [
  'components/ActivityReview.vue',
  'components/NotificationPublish.vue',
  'components/UserManagement.vue',
  'components/TaskReview.vue',
  'components/ApiManagement.vue',
  'components/MarketProductsManagement.vue',
]

const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('secondary admin pages', () => {
  it('use the shared compact admin structure and avoid legacy visual effects', () => {
    for (const file of files) {
      const source = readSource(file)
      expect(source).toContain('class="admin-page')
      expect(source).toMatch(/AdminPageHeader|admin-page-header/)
      expect(source).toMatch(/AdminToolbar|admin-toolbar/)
      expect(source).toMatch(/AdminPanel|admin-panel/)
      expect(source).not.toMatch(/linear-gradient|radial-gradient|backdrop-filter/)
    }
  })
})
