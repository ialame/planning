// src/services/api.js - VERSION QUI FONCTIONNE AVEC VOS ENDPOINTS
//const API_BASE_URL = 'http://146.190.204.228:8080'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api'
class ApiService {

  /**
   * 🔧 Méthode générique de requête
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
      console.log(`🔄 API Request: ${options.method || 'GET'} ${url}`)
      const response = await fetch(url, config)

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const data = await response.json()
      console.log(`✅ API Response: ${endpoint} - ${Array.isArray(data) ? data.length : 'object'} items`)
      return data

    } catch (error) {
      console.error(`❌ API Error for ${endpoint}:`, error)
      throw error
    }
  }

  /**
   * 👥 EMPLOYÉS - Utilise l'endpoint qui fonctionne
   */
  async getEmployees() {
    try {
      const employees = await this.request(`/api/employees`)
      console.log(`✅ ${employees.length} employés récupérés`)

      // Mapper vers le format attendu par le frontend
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
      console.error('❌ Erreur récupération employés:', error)
      return []
    }
  }

  /**
   * 📦 COMMANDES - Utilise l'endpoint qui fonctionne
   */
  async getOrders() {
    try {
      const orders = await this.request('/api/orders')
      console.log(`✅ ${orders.length} commandes récupérées`)

      // Mapper vers le format attendu par le frontend
      return orders.map(order => ({
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
      console.error('❌ Erreur récupération commandes:', error)
      return []
    }
  }

  /**
   * 📅 PLANIFICATIONS - Test plusieurs endpoints possibles
   */
  async getPlanning() {
    const endpoints = [
      `/api/planning`,
      `/api/planning/view-simple`
    ]

    for (const endpoint of endpoints) {
      try {
        const data = await this.request(endpoint)
        console.log(`✅ Planning récupéré via: ${endpoint}`)
        return Array.isArray(data) ? data : []
      } catch (error) {
        console.log(`❌ Échec endpoint ${endpoint}`)
        continue
      }
    }

    console.warn('⚠️ Aucun endpoint planifications disponible')
    return []
  }

  /**
   * 🚀 GÉNÉRATION DE PLANIFICATION
   */
  async generatePlanning(config = {}) {
    try {
      const body = {
        startDate: config.startDate || '2025-06-01',
        timePerCard: config.timePerCard || 3,
        cleanFirst: config.cleanFirst || false,
        ...config
      }

      console.log('🚀 Génération planification avec:', body)

      const result = await this.request(`/api/planning/generate`, {
        method: 'POST',
        body: JSON.stringify(body)
      })

      console.log('✅ Planification générée:', result)
      return result

    } catch (error) {
      console.error('❌ Erreur génération planification:', error)
      throw error
    }
  }

  /**
   * 📊 STATISTIQUES - Calculées à partir des données existantes
   */
  async getStats() {
    try {
      const [employees, orders, planning] = await Promise.all([
        this.getEmployees(),
        this.getOrders(),
        this.getPlanning()
      ])

      // Calculer des statistiques utiles
      const stats = {
        employeesCount: employees.length,
        activeEmployees: employees.filter(e => e.active).length,
        ordersCount: orders.length,
        planningCount: planning.length,

        // Statistiques des commandes par priorité
        ordersByPriority: {
          URGENT: orders.filter(o => o.priority === 'URGENT').length,
          HIGH: orders.filter(o => o.priority === 'HIGH').length,
          MEDIUM: orders.filter(o => o.priority === 'MEDIUM').length,
          LOW: orders.filter(o => o.priority === 'LOW').length
        },

        // Statistiques des commandes par statut
        ordersByStatus: {
          PENDING: orders.filter(o => o.status === 1).length,
          IN_PROGRESS: orders.filter(o => o.status === 2).length,
          COMPLETED: orders.filter(o => o.status === 3).length
        },

        // Temps total estimé
        totalEstimatedMinutes: orders.reduce((sum, o) => sum + (o.estimatedTimeMinutes || 0), 0),
        totalCards: orders.reduce((sum, o) => sum + (o.cardCount || 0), 0),
        totalPrice: orders.reduce((sum, o) => sum + (o.totalPrice || 0), 0),

        // Date de dernière mise à jour
        lastUpdate: new Date().toISOString()
      }

      console.log('📊 Statistiques calculées:', stats)
      return stats

    } catch (error) {
      console.error('❌ Erreur calcul statistiques:', error)
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
   * 🏥 TEST DE SANTÉ DU BACKEND
   */
  async healthCheck() {
    try {
      // Test direct sur un endpoint qui fonctionne
      const response = await fetch(`${API_BASE_URL}/api/employees`)
      const isHealthy = response.ok

      if (isHealthy) {
        console.log('✅ Backend Health: OK')
      } else {
        console.log(`❌ Backend Health: HTTP ${response.status}`)
      }

      return isHealthy

    } catch (error) {
      console.error('❌ Backend Health Check Failed:', error)
      return false
    }
  }

  /**
   * 🎯 MÉTHODES SPÉCIFIQUES POUR POKÉMON
   */

  // Obtenir les commandes depuis juin 2025
  async getOrdersSinceJune2025() {
    try {
      const allOrders = await this.getOrders()

      // Filtrer les commandes depuis juin 2025
      const june2025 = new Date('2025-06-01')
      const filteredOrders = allOrders.filter(order => {
        const orderDate = new Date(order.creationDate || order.orderDate)
        return orderDate >= june2025
      })

      console.log(`📦 ${filteredOrders.length} commandes depuis juin 2025`)
      return filteredOrders

    } catch (error) {
      console.error('❌ Erreur commandes juin 2025:', error)
      return []
    }
  }

  // Obtenir les employés actifs avec leur charge
  async getActiveEmployeesWithLoad() {
    try {
      const employees = await this.getEmployees()
      const activeEmployees = employees.filter(emp => emp.active && emp.available)

      console.log(`👥 ${activeEmployees.length} employés actifs`)
      return activeEmployees

    } catch (error) {
      console.error('❌ Erreur employés actifs:', error)
      return []
    }
  }
}

// Export singleton
const apiService = new ApiService()
export default apiService

// Pour debug dans la console
if (typeof window !== 'undefined') {
  window.apiService = apiService
}
