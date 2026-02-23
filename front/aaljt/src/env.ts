// Safe accessor for Vite env without direct `import.meta` in SFCs.
// Uses eval to avoid TypeScript diagnostics when module option is misdetected.
let ev: Record<string, any> = {};
try {
  const im = (0, eval)('import.meta');
  ev = im && im.env ? im.env : {};
} catch {
  ev = {};
}
export const env = ev;