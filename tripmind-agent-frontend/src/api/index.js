import axios from 'axios'

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD ? '/api' : 'http://localhost:8123/api')
).replace(/\/$/, '')

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

const parseSseFrame = (frame) => {
  const lines = frame.split(/\r?\n/)
  let event = 'message'
  const dataLines = []

  for (const line of lines) {
    if (!line || line.startsWith(':')) {
      continue
    }
    if (line.startsWith('event:')) {
      event = line.slice(6).trim() || 'message'
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  if (dataLines.length === 0) {
    return null
  }

  return {
    event,
    data: dataLines.join('\n')
  }
}

/**
 * Create trip plan (stream mode)
 * @param {Object} data request payload: { userPrompt, userId }
 * @param {Function} onMessage callback receives { event, data }
 * @param {Function} onError error callback
 * @returns {{close: Function}}
 */
export const createTripPlan = (data, onMessage, onError) => {
  const url = `${API_BASE_URL}/trip/plan/stream`
  const controller = new AbortController()

  ;(async () => {
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify(data),
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      if (!response.body) {
        throw new Error('Empty response body')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const normalized = buffer.replace(/\r\n/g, '\n')
        const frames = normalized.split('\n\n')
        buffer = frames.pop() || ''

        for (const frame of frames) {
          const parsed = parseSseFrame(frame)
          if (parsed && onMessage) {
            onMessage(parsed)
          }
        }
      }

      if (buffer.trim()) {
        const parsed = parseSseFrame(buffer)
        if (parsed && onMessage) {
          onMessage(parsed)
        }
      }

      if (onMessage) {
        onMessage({ event: 'done', data: '[DONE]' })
      }
    } catch (error) {
      if (error?.name === 'AbortError') {
        return
      }
      if (onError) {
        onError(error)
      }
    }
  })()

  return {
    close: () => controller.abort()
  }
}

/**
 * Create trip plan (sync mode)
 */
export const createTripPlanSync = (data) => {
  return request.post('/trip/plan', data)
}

/**
 * Query trip plan by id
 */
export const getTripPlan = (planId) => {
  return request.get(`/trip/plan/${planId}`)
}

/**
 * Download plan PDF
 */
export const downloadPdf = (planId) => {
  return request.get(`/trip/plan/${planId}/pdf`, {
    responseType: 'blob'
  })
}

export default {
  createTripPlan,
  createTripPlanSync,
  getTripPlan,
  downloadPdf
}
