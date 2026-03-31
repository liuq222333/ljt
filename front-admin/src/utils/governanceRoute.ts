export function readQueryString(value: unknown, fallback = '') {
  return typeof value === 'string' ? value : fallback
}

export function readQueryNumber(value: unknown, fallback = 0) {
  if (typeof value !== 'string' || value.trim() === '') {
    return fallback
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export function readQueryEnum<T extends string>(value: unknown, candidates: readonly T[], fallback: T) {
  if (typeof value === 'string' && candidates.includes(value as T)) {
    return value as T
  }
  return fallback
}

export function cleanQueryRecord(record: Record<string, unknown>) {
  const next: Record<string, string> = {}
  Object.entries(record).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    next[key] = String(value)
  })
  return next
}
