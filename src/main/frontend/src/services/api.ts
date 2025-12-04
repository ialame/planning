// API Configuration for Pokemon Card Planning
// Automatically detects environment and uses appropriate API URL

/**
 * Determine API base URL based on environment
 * - Production: Use backend LoadBalancer external IP
 * - Development (localhost): Use localhost:8010 (Docker) or 8080 (local dev)
 * - Can be overridden with VITE_API_BASE_URL environment variable
 */
function getApiBaseUrl(): string {
  // Allow manual override via environment variable
  if (import.meta.env.VITE_API_BASE_URL !== undefined && import.meta.env.VITE_API_BASE_URL !== '') {
    console.log('✅ Using VITE_API_BASE_URL from environment:', import.meta.env.VITE_API_BASE_URL)
    return import.meta.env.VITE_API_BASE_URL
  }

  // Check if we're running on localhost (development)
  const hostname = window.location.hostname
  const isLocalhost = hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname.startsWith('192.168.') ||
    hostname.startsWith('10.')

  console.log('🔍 Detected hostname:', hostname)
  console.log('🔍 Is localhost:', isLocalhost)

  // In development, point to local backend
  // Port 8010 for Docker, port 8080 for direct local dev
  if (isLocalhost) {
    // Check which port is likely in use based on frontend port
    const frontendPort = window.location.port
    
    // If frontend is on 8012 (Docker), backend is on 8010
    // If frontend is on 3000/5173 (local dev), backend is on 8080
    if (frontendPort === '8012') {
      console.log('🐳 Docker mode detected - Using API URL: http://localhost:8010')
      return 'http://localhost:8010'
    }
    
    console.log('💻 Local dev mode - Using API URL: http://localhost:8080')
    return 'http://localhost:8080'
  }

  // In production on Kubernetes/DigitalOcean
  // Use the backend LoadBalancer service on same IP but port 8080
  const backendUrl = `http://${hostname}:8080`
  console.log('🌐 Using production API URL:', backendUrl)

  return backendUrl
}

export const API_BASE_URL: string = getApiBaseUrl()

export const API_ENDPOINTS = {
  EMPLOYEES: `${API_BASE_URL}/api/employees`,
  PLANNING: `${API_BASE_URL}/api/planning`,
  ORDERS: `${API_BASE_URL}/api/orders`
} as const

// Log the final configuration
console.log('📡  API Configuration loaded')
console.log('📡  Base URL:', API_BASE_URL || '(relative URL)')
console.log('📡  Employees:', API_ENDPOINTS.EMPLOYEES)
console.log('📡  Orders:', API_ENDPOINTS.ORDERS)
console.log('📡  Planning:', API_ENDPOINTS.PLANNING)
