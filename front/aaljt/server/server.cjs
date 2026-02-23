// Minimal Node backend without external dependencies
// Provides POST /api/login that validates a test user and returns a token

const http = require('http')
const crypto = require('crypto')

const PORT = 3000

// In-memory token store for demo purposes
const tokens = new Map()

function sendJson(res, status, data, extraHeaders = {}) {
  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    ...extraHeaders,
  }
  res.writeHead(status, headers)
  res.end(JSON.stringify(data))
}

function parseBody(req) {
  return new Promise((resolve) => {
    let data = ''
    req.on('data', (chunk) => { data += chunk })
    req.on('end', () => {
      try {
        const json = JSON.parse(data || '{}')
        resolve(json)
      } catch (e) {
        resolve({})
      }
    })
  })
}

const server = http.createServer(async (req, res) => {
  const { method, url } = req

  // Basic CORS support for direct calls (proxy also in place)
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    })
    return res.end()
  }

  if (url === '/api/login' && method === 'POST') {
    const body = await parseBody(req)
    const { username, password } = body
    // Demo validation: admin / 123123
    if ((username === 'admin' || username === 'admin@example.com') && password === '123123') {
      const token = crypto.randomBytes(24).toString('hex')
      tokens.set(token, { username, issuedAt: Date.now() })
      return sendJson(res, 200, { ok: true, token, user: { username } })
    }
    return sendJson(res, 401, { ok: false, message: 'Invalid credentials' })
  }

  if (url === '/api/me' && method === 'GET') {
    const auth = req.headers['authorization'] || ''
    const token = auth.replace(/^Bearer\s+/i, '')
    if (token && tokens.has(token)) {
      return sendJson(res, 200, { ok: true, user: tokens.get(token) })
    }
    return sendJson(res, 401, { ok: false })
  }

  // Fallback
  sendJson(res, 404, { ok: false, message: 'Not found' })
})

server.listen(PORT, () => {
  console.log(`[backend] server running on http://localhost:${PORT}`)
})