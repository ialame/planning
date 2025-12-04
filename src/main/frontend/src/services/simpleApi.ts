// src/services/simpleApi.ts
// Simple API service for Docker development without OAuth2

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * Check if auth is disabled (Docker dev mode)
 */
export function isAuthDisabled(): boolean {
  return import.meta.env.VITE_AUTH_DISABLED === 'true'
}

/**
 * Simple fetch wrapper without authentication
 */
async function simpleFetch(endpoint: string, options: RequestInit = {}): Promise<Response> {
  const url = endpoint.startsWith('http')
    ? endpoint
    : `${API_BASE_URL}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`

  const defaultHeaders: HeadersInit = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }

  return fetch(url, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  })
}

/**
 * GET request
 */
export async function get<T = any>(endpoint: string): Promise<T> {
  const response = await simpleFetch(endpoint, { method: 'GET' })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json()
}

/**
 * POST request
 */
export async function post<T = any>(endpoint: string, data?: any): Promise<T> {
  const response = await simpleFetch(endpoint, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  const text = await response.text()
  return text ? JSON.parse(text) : {} as T
}

/**
 * PUT request
 */
export async function put<T = any>(endpoint: string, data?: any): Promise<T> {
  const response = await simpleFetch(endpoint, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  const text = await response.text()
  return text ? JSON.parse(text) : {} as T
}

/**
 * DELETE request
 */
export async function del<T = any>(endpoint: string): Promise<T> {
  const response = await simpleFetch(endpoint, { method: 'DELETE' })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  const text = await response.text()
  return text ? JSON.parse(text) : {} as T
}

// Export as default object for compatibility
export default {
  get,
  post,
  put,
  delete: del,
  isAuthDisabled
}
