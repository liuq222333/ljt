export function readGovernanceStorage<T>(key: string, fallback: T): T {
  if (typeof window === 'undefined') {
    return fallback
  }
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

export function writeGovernanceStorage(key: string, value: unknown) {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // ignore storage failures
  }
}

export function removeGovernanceStorage(key: string) {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.removeItem(key)
  } catch {
    // ignore storage failures
  }
}
