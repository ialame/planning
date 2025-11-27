// src/services/api.js - VERSION THAT WORKS WITH YOUR ENDPOINTS
//const API_BASE_URL = 'http://146.190.204.228:8080'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api'
// Définir l'interface
interface PlanningConfig {
  startDate?: string;
  timePerCard?: number;
  cleanFirst?: boolean;
  planningDate?: string;
  [key: string]: any; // To allow for other properties
}
class ApiService {

  /**
   *  Generic query method
   */
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`
    const config = {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      },
      ...options,
    }

    try {
      console.log(` API Request: ${options.method || 'GET'} ${url}`)
      const response = await fetch(url, config)

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const data = await response.json()
      console.log(` API Response: ${endpoint} - ${Array.isArray(data) ? data.length : 'object'} items`)
      return data

    } catch (error) {
      console.error(` API Error for ${endpoint}:`, error)
      throw error
    }
  }

  /**
   *  EMPLOYEES - Uses the working endpoint
   */
  async getEmployees() {
    try {
      const employees = await this.request(`/api/employees`)
      console.log(` ${employees.length} employés récupérés`)

      // Map to the format expected by the frontend
      return employees.map(emp => ({
        id: emp.id,
        firstName: emp.firstName,
        lastName: emp.lastName,
        fullName: emp.fullName || `${emp.firstName} ${emp.lastName}`,
        email: emp.email,
        workHoursPerDay: emp.workHoursPerDay,
        active: emp.active,
        available: emp.available,
        currentLoad: emp.currentLoad || 0,
        name: emp.name || emp.fullName
      }))
    } catch (error) {
      console.error(' Erreur récupération employés:', error)
      return []
    }
  }

  /**
   *  COMMANDS - Uses the working endpoint
   */
  async getOrders() {
    try {
      const orders : Order[] = await this.request('/api/orders')
      console.log(` ${orders.length} commandes récupérées`)

      // Map to the format expected by the frontend
      return orders.map(order  => ({
        id: order.id,
        orderNumber: order.orderNumber,
        reference: order.reference,
        cardCount: order.cardCount,
        totalPrice: order.totalPrice,
        priority: order.priority,
        status: order.status,
        statusText: order.statusText,
        estimatedTimeMinutes: order.estimatedTimeMinutes,
        estimatedTimeHours: order.estimatedTimeHours,
        creationDate: order.creationDate,
        orderDate: order.orderDate,
        deadline: order.deadline,
        qualityIndicator: order.qualityIndicator,
        minimumGrade: order.minimumGrade,
        type: order.type,
        unsealing: order.unsealing
      }))
    } catch (error) {
      console.error(' Order recovery error:', error)
      return []
    }
  }

  /**
   *  PLANNING - Test several possible endpoints
   */
  async getPlanning() {
    const endpoints = [
      `/api/planning`,
      `/api/planning/view-simple`
    ]

    for (const endpoint of endpoints) {
      try {
        const data = await this.request(endpoint)
        console.log(`Schedule retrieved via: ${endpoint}`)
        return Array.isArray(data) ? data : []
      } catch (error) {
        console.log(`  Endpoint Failure ${endpoint}`)
        continue
      }
    }

    console.warn(' No endpoint plans available')
    return []
  }

  /**
   *  PLANNING GENERATION
   */
  // Type method
    async generatePlanning(config: PlanningConfig = {}) {
    try {
      const body = {
        startDate: config.startDate || '2025-06-01',
        timePerCard: config.timePerCard || 3,
        cleanFirst: config.cleanFirst || false,
        ...config
      }
      console.log(' Generation planning with:', body)

      const result = await this.request(`/api/planning/generate`, {
        method: 'POST',
        body: JSON.stringify(body)
      })

      console.log(' Generated planning:', result)
      return result

    } catch (error) {
      console.error(' Planning generation error:', error)
      throw error
    }
  }

  /**
   *  STATISTICS - Calculated from existing data
   */
  async getStats() {
    try {
      const [employees, orders, planning] = await Promise.all([
        this.getEmployees(),
        this.getOrders(),
        this.getPlanning()
      ])

      // Calculate useful statistics
      const stats = {
        employeesCount: employees.length,
        activeEmployees: employees.filter( (e: any) => e.active).length,
        ordersCount: orders.length,
        planningCount: planning.length,

        // Order statistics by priority
        ordersByPriority: {
          URGENT: orders.filter((o: any) => o.priority === 'URGENT').length,
          HIGH: orders.filter((o: any) => o.priority === 'HIGH').length,
          MEDIUM: orders.filter((o: any) => o.priority === 'MEDIUM').length,
          LOW: orders.filter((o: any) => o.priority === 'LOW').length
        },

        // Order statistics by status
        ordersByStatus: {
          PENDING: orders.filter((o: any) => o.status === 1).length,
          IN_PROGRESS: orders.filter((o: any) => o.status === 2).length,
          COMPLETED: orders.filter((o: any) => o.status === 3).length
        },

        // Estimated total time
        totalEstimatedMinutes: orders.reduce((sum, o) => sum + (o.estimatedTimeMinutes || 0), 0),
        totalCards: orders.reduce((sum, o) => sum + (o.cardCount || 0), 0),
        totalPrice: orders.reduce((sum, o) => sum + (o.totalPrice || 0), 0),

        // Date of last update
        lastUpdate: new Date().toISOString()
      }

      console.log(' Calculated statistics:', stats)
      return stats

    } catch (error) {
      console.error(' Statistics calculation error:', error)
      return {
        employeesCount: 0,
        ordersCount: 0,
        planningCount: 0,
        error: true,
        message: error.message
      }
    }
  }

  /**
   *  BACKEND HEALTH TEST
   */
  async healthCheck() {
    try {
      // Direct test on a working endpoint
      const response = await fetch(`${API_BASE_URL}/api/employees`)
      const isHealthy = response.ok

      if (isHealthy) {
        console.log(' Backend Health: OK')
      } else {
        console.log(` Backend Health: HTTP ${response.status}`)
      }

      return isHealthy

    } catch (error) {
      console.error(' Backend Health Check Failed:', error)
      return false
    }
  }

  /**
   *  SPECIFIC METHODS FOR POKÉMON
   */

  // Receive orders from June 2025
  async getOrdersSinceJune2025() {
    try {
      const allOrders = await this.getOrders()

      // Filter orders since June 2025
      const june2025 = new Date('2025-06-01')
      const filteredOrders = allOrders.filter(order => {
        const orderDate = new Date(order.creationDate || order.orderDate)
        return orderDate >= june2025
      })

      console.log(` ${filteredOrders.length} orders since June 2025`)
      return filteredOrders

    } catch (error) {
      console.error(' Order error June 2025:', error)
      return []
    }
  }

  // Obtain active employees with their workload
  async getActiveEmployeesWithLoad() {
    try {
      const employees: Employee[] = await this.getEmployees()
      const activeEmployees = employees.filter(emp => emp.active && emp.available)

      console.log(` ${activeEmployees.length} active employees`)
      return activeEmployees

    } catch (error) {
      console.error(' Active employees error:', error)
      return []
    }
  }
}

// Export singleton
const apiService = new ApiService()
export default apiService

// To debug in the console
if (typeof window !== 'undefined') {
  window.apiService = apiService
}
