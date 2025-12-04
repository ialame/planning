// src/services/api.ts
// HTTP client with OAuth2 authentication

import authService from './authService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * API Error class
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string,
    public data?: any
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * Request options interface
 */
interface RequestOptions extends RequestInit {
  skipAuth?: boolean
}

/**
 * Make an authenticated API request
 */
async function request<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipAuth = false, ...fetchOptions } = options

  // Build URL
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`

  // Build headers
  const headers = new Headers(fetchOptions.headers)

  // Add Content-Type if not set and we have a body
  if (fetchOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  // Add Authorization header if authenticated and not skipped
  if (!skipAuth) {
    const token = authService.getAccessToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  // Make request
  const response = await fetch(url, {
    ...fetchOptions,
    headers,
  })

  // Handle response
  if (!response.ok) {
    // Handle 401 - token might be expired
    if (response.status === 401) {
      console.warn(' API returned 401, attempting token refresh...')
      
      try {
        await authService.refreshToken()
        // Retry request with new token
        const newToken = authService.getAccessToken()
        if (newToken) {
          headers.set('Authorization', `Bearer ${newToken}`)
          const retryResponse = await fetch(url, { ...fetchOptions, headers })
          
          if (retryResponse.ok) {
            const data = await retryResponse.json()
            return data as T
          }
        }
      } catch (refreshError) {
        console.error('Token refresh failed:', refreshError)
        // Redirect to login
        await authService.login(window.location.pathname)
        throw new ApiError(401, 'Unauthorized', 'Session expired')
      }
    }

    // Parse error response
    let errorData: any
    try {
      errorData = await response.json()
    } catch {
      errorData = { message: response.statusText }
    }

    throw new ApiError(
      response.status,
      response.statusText,
      errorData.message || errorData.error || 'API request failed',
      errorData
    )
  }

  // Parse successful response
  const contentType = response.headers.get('Content-Type')
  if (contentType?.includes('application/json')) {
    return response.json()
  }

  // Return empty object for non-JSON responses
  return {} as T
}

/**
 * API client with typed methods
 */
export const api = {
  /**
   * GET request
   */
  async get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'GET' })
  },

  /**
   * POST request
   */
  async post<T>(endpoint: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    })
  },

  /**
   * PUT request
   */
  async put<T>(endpoint: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    })
  },

  /**
   * PATCH request
   */
  async patch<T>(endpoint: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    })
  },

  /**
   * DELETE request
   */
  async delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'DELETE' })
  },
}

export default api
