export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL

export const API_ENDPOINTS = {
  EMPLOYEES: `${API_BASE_URL}/api/employees`,
  PLANNING: `${API_BASE_URL}/api/planning`,
  ORDERS: `${API_BASE_URL}/api/orders`
} as const
