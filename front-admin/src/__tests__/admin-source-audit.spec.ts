import { readdirSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const componentsRoot = resolve(__dirname, '..', 'components')
const bannedPatterns = /linear-gradient|radial-gradient|backdrop-filter/

const walkVueFiles = (directory: string): string[] =>
  readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = join(directory, entry.name)
    if (entry.isDirectory()) {
      return walkVueFiles(fullPath)
    }
    return entry.isFile() && entry.name.endsWith('.vue') ? [fullPath] : []
  })

describe('admin source audit', () => {
  it('keeps admin component sources free from gradients and glass effects', () => {
    for (const file of walkVueFiles(componentsRoot)) {
      const source = readFileSync(file, 'utf8')
      expect(source, file).not.toMatch(bannedPatterns)
    }
  })
})
