<!-- ============= CORRECTION EmployeeDetailPage.vue - FOCUS SUR LES COMMANDES ============= -->

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- Breadcrumb et retour -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center space-x-4">
          <button
            @click="$emit('back')"
            class="flex items-center text-blue-600 hover:text-blue-800 font-medium"
          >
            ← Back to Employees
          </button>
          <div class="text-gray-400">/</div>
          <h1 class="text-2xl font-bold text-gray-900">
            📋 {{ mode === 'planning' ? 'Planning View' : 'Employee Details' }}
          </h1>
        </div>
        <div class="flex items-center space-x-3">
          <input
            v-model="selectedDate"
            type="date"
            class="border rounded-lg px-3 py-2"
            @change="loadEmployeeOrders"
          >
          <button
            @click="refreshData"
            :disabled="loading"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {{ loading ? '⏳' : '🔄' }} Refresh
          </button>
        </div>
      </div>

      <!-- En-tête employé compact -->
      <div v-if="employeeData" class="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <div class="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
              <span class="text-white font-bold">
                {{ getEmployeeInitials(employeeData) }}
              </span>
            </div>
            <div class="ml-4">
              <h2 class="text-lg font-bold text-gray-900">{{ employeeData.fullName || employeeData.name }}</h2>
              <p class="text-sm text-gray-600">{{ employeeData.email }}</p>
            </div>
          </div>
          <div class="text-right">
            <div class="text-sm text-gray-600">Workload for {{ selectedDate }}</div>
            <div class="text-lg font-bold" :class="workloadPercentage > 100 ? 'text-red-600' : workloadPercentage > 80 ? 'text-orange-600' : 'text-green-600'">
              {{ workloadPercentage }}%
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ SECTION PRINCIPALE : COMMANDES ASSIGNÉES -->
      <div class="bg-white rounded-lg shadow-sm border mb-6">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">📦 Assigned Orders</h3>
              <p class="text-sm text-gray-600 mt-1">Orders assigned to this employee for {{ selectedDate }}</p>
            </div>
            <div class="text-right">
              <div class="text-2xl font-bold text-blue-600">{{ orders.length }}</div>
              <div class="text-sm text-gray-600">Total Orders</div>
            </div>
          </div>
        </div>

        <!-- Statistics Cards -->
        <div class="p-6 border-b border-gray-200">
          <div class="grid grid-cols-3 gap-4">
            <div class="text-center">
              <div class="text-xl font-bold text-purple-600">{{ totalCards }}</div>
              <div class="text-sm text-gray-600">Total Cards</div>
            </div>
            <div class="text-center">
              <div class="text-xl font-bold text-green-600">{{ formatDuration(totalDuration) }}</div>
              <div class="text-sm text-gray-600">Total Duration</div>
            </div>
            <div class="text-center">
              <div class="text-xl font-bold text-gray-600">{{ employeeData?.workHoursPerDay || 8 }}h</div>
              <div class="text-sm text-gray-600">Daily Capacity</div>
            </div>
          </div>
        </div>

        <!-- Orders List -->
        <div class="p-6">
          <!-- Loading State -->
          <div v-if="loading" class="text-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p class="text-gray-600">Loading assigned orders...</p>
          </div>

          <!-- Empty State -->
          <div v-else-if="orders.length === 0" class="text-center py-8">
            <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
            </svg>
            <h3 class="text-lg font-medium text-gray-900 mb-2">No Orders Assigned</h3>
            <p class="text-gray-600 mb-4">No orders have been assigned to this employee for {{ selectedDate }}</p>
            <button
              @click="generatePlanningForEmployee"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
            >
              📋 Generate Planning for This Employee
            </button>
          </div>

          <!-- Orders Grid -->
          <!-- Orders Table -->
          <!-- Replace the current table in EmployeeDetailPage.vue -->
          <div v-else class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
              <tr>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order #
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Delai
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Cards
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Start Time
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  End Time
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Duration
                </th>
                <th scope="col" class="relative px-6 py-3">
                  <span class="sr-only">Actions</span>
                </th>
              </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
              <template v-for="order in orders" :key="order.id || order.orderId">
                <!-- Main Order Row -->
                <tr
                  class="hover:bg-gray-50 transition-colors duration-150"
                  :class="{ 'bg-blue-50': order.showCards }"
                >
                  <!-- Order Number -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                      <div class="text-sm font-medium text-gray-900">
                        {{ order.orderNumber || order.num_commande || 'N/A' }}
                      </div>
                    </div>
                  </td>

                  <!-- Status -->
                  <td class="px-6 py-4 whitespace-nowrap">
          <span :class="[
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getOrderStatusColor(order.orderStatus)
          ]">
            <span class="w-1.5 h-1.5 mr-1.5 rounded-full" :class="getOrderStatusDotColor(order.orderStatus)"></span>
            {{ getOrderStatusText(order.orderStatus) }}
          </span>
                  </td>

                  <!-- Delai (Priority) -->
                  <td class="px-6 py-4 whitespace-nowrap">
          <span :class="[
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getDelaiColor(order.delai)
          ]">
            {{ getDelaiLabel(order.delai) }}
          </span>
                  </td>

                  <!-- Card Count -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                      <div class="text-sm font-medium text-gray-900">
                        {{ order.cardCount || 0 }}
                      </div>
                      <div class="text-xs text-gray-500 ml-1">cards</div>
                      <!-- Expand/Collapse indicator -->
                      <div
                        v-if="(order.cardCount || 0) > 0"
                        class="ml-2 cursor-pointer text-blue-600 hover:text-blue-800"
                        @click="toggleOrderCards(order)"
                      >
                        <svg class="w-4 h-4" :class="{ 'transform rotate-180': order.showCards }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                        </svg>
                      </div>
                    </div>
                  </td>

                  <!-- Start Time -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ formatTime(order.startTime) }}
                    </div>
                    <div class="text-xs text-gray-500">
                      {{ order.planningDate || selectedDate }}
                    </div>
                  </td>

                  <!-- End Time -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ formatTime(order.endTime) }}
                    </div>
                    <div class="text-xs text-gray-500">
                      Est. {{ formatDuration(order.durationMinutes || order.estimatedDuration || 0) }}
                    </div>
                  </td>

                  <!-- Duration -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm font-medium text-gray-900">
                      {{ formatDuration(order.durationMinutes || order.estimatedDuration || (order.cardCount * 3)) }}
                    </div>
                    <div class="text-xs text-gray-500">
                      {{ order.cardCount || 0 }} × 3min
                    </div>
                  </td>


                  <!-- Actions -->
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      @click="viewOrderDetails(order)"
                      class="text-blue-600 hover:text-blue-900 mr-3"
                    >
                      View
                    </button>
                    <button
                      v-if="order.status !== 'COMPLETED'"
                      @click="startOrder(order)"
                      class="text-green-600 hover:text-green-900"
                    >
                      Start
                      </button>
                  </td>
                </tr>

                <!-- ✅ EXPANDABLE CARDS ROW -->

                <!-- Expanded Cards Section -->
                <tr v-if="order.showCards" class="bg-gray-50">
                  <td colspan="8" class="px-6 py-4">
                    <!-- Loading cards -->
                    <div v-if="order.loadingCards" class="text-center py-4">
                      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto"></div>
                      <p class="text-sm text-gray-600 mt-2">Loading cards...</p>
                    </div>

                    <!-- Cards list -->
                    <div v-else-if="order.cards && order.cards.length > 0" class="space-y-2">
                      <h4 class="font-semibold text-gray-900 mb-3">📋 Cards in this order ({{ order.cards.length }})</h4>
                      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                        <div
                          v-for="(card, idx) in order.cards"
                          :key="card.id || idx"
                          class="bg-white p-3 rounded border border-gray-200 hover:shadow-sm transition-shadow"
                        >
                          <div class="flex items-start justify-between">
                            <div class="flex-1">
                              <div class="font-medium text-gray-900">
                                {{ card.name || card.label_name || `Card #${idx + 1}` }}
                              </div>
                              <div class="text-xs text-gray-500 mt-1">
                                Code: {{ card.code_barre || 'N/A' }}
                              </div>
                            </div>
                            <div class="ml-2 text-right">
                              <div class="text-xs font-medium text-blue-600">
                                ~{{ card.duration || 3 }}min
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- No cards -->
                    <div v-else class="text-center py-4 text-gray-500">
                      No card details available
                    </div>
                  </td>
                </tr>
              </template>
              </tbody>
            </table>
          </div>
            <!-- Table Footer with Summary -->
            <div class="bg-gray-50 px-6 py-3 border-t border-gray-200">
              <div class="flex justify-between items-center">
                <div class="flex items-center space-x-4 text-sm text-gray-600">
                  <span>Total: {{ orders.length }} orders</span>
                  <span>•</span>
                  <span>{{ totalCards }} cards</span>
                  <span>•</span>
                  <span>{{ formatDuration(totalDuration) }} estimated</span>
                </div>
                <div class="text-sm text-gray-600">
                  Capacity: {{ Math.round((totalDuration / ((employeeData?.workHoursPerDay || 8) * 60)) * 100) }}%
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ SECTION SECONDAIRE : GROUPES (réduite) -->
      <div v-if="mode !== 'planning' && employeeData" class="bg-white rounded-lg shadow-sm border p-4">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">👥 Employee Groups</h3>
        <EmployeeGroupsCard
          :employee="employeeData"
          @updated="refreshData"
        />
      </div>

    </div>

</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { API_BASE_URL } from '@/config/api.ts'
import EmployeeGroupsCard from "./groups/EmployeeGroupsCard.vue"

// ========== PROPS ==========
const props = defineProps<{
  employeeId: string
  selectedDate: string
  mode?: string
}>()

import { inject } from 'vue'

// ========== INJECT PARENT SERVICES ==========
const showNotification = inject('showNotification')
// ========== EMITS DECLARATION ==========
const emit = defineEmits(['refresh', 'back'])

// ========== STATE ==========
const loading = ref(false)
const loadingEmployee = ref(false)
const employeeData = ref<any>(null)
const orders = ref<any[]>([])
//const selectedDate = ref(props.selectedDate)
const selectedDate = ref('2025-06-01')
const debugEmployeePlanning = async (employeeId: string) => {
  console.log('🔍 === DEBUGGING EMPLOYEE PLANNING ===')
  console.log('Employee ID:', employeeId)
  console.log('Selected Date:', selectedDate.value)

  try {
    // Test with current date
    console.log('Testing with current date...')
    const currentResponse = await fetch(`${API_BASE_URL}/api/planning/employee/${employeeId}?date=${selectedDate.value}`)
    console.log('Current date response:', currentResponse.status)
    if (currentResponse.ok) {
      const currentData = await currentResponse.json()
      console.log('Current date data:', currentData)
    }

    // Test without date filter
    console.log('Testing without date filter...')
    const noDateResponse = await fetch(`${API_BASE_URL}/api/planning/employee/${employeeId}`)
    console.log('No date response:', currentResponse.status)
    if (noDateResponse.ok) {
      const noDateData = await noDateResponse.json()
      console.log('No date data:', noDateData)
    }

    // Test with planning generation date
    console.log('Testing with 2025-06-01...')
    const planningDateResponse = await fetch(`${API_BASE_URL}/api/planning/employee/${employeeId}?date=2025-06-01`)
    console.log('Planning date response:', planningDateResponse.status)
    if (planningDateResponse.ok) {
      const planningDateData = await planningDateResponse.json()
      console.log('Planning date data:', planningDateData)
    }

  } catch (error) {
    console.error('Debug error:', error)
  }

  console.log('🏁 === DEBUG COMPLETE ===')
}
// Make sure the back button emits the correct event
const goBackToEmployees = () => {
  console.log('🔙 Going back to employees list')
  emit('back')
}

const workloadPercentage = computed(() => {
  const maxMinutes = (employeeData.value?.workHoursPerDay || 8) * 60
  return Math.round((totalDuration.value / maxMinutes) * 100)
})

// ========== METHODS ==========
const getEmployeeInitials = (employee: any) => {
  if (employee.firstName && employee.lastName) {
    return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`
  }
  if (employee.fullName) {
    const parts = employee.fullName.split(' ')
    return parts.length > 1 ? `${parts[0].charAt(0)}${parts[1].charAt(0)}` : parts[0].charAt(0)
  }
  return '??'
}

const formatDuration = (minutes: number) => {
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  if (hours > 0) {
    return `${hours}h ${remainingMinutes}m`
  }
  return `${remainingMinutes}m`
}

const calculateEndTime = (order: any) => {
  // Simple calculation - in a real app, this would be more sophisticated
  const startTime = order.startTime || '09:00'
  const duration = order.estimatedDuration || order.durationMinutes || (order.cardCount * 3)
  // Return estimated end time (simplified)
  return `${startTime} + ${formatDuration(duration)}`
}


const getStatusText = (status: any) => {
  switch (status?.toString()) {
    case '2':
    case 'COMPLETED': return 'Completed'
    case '1':
    case 'IN_PROGRESS': return 'In Progress'
    case '0':
    case 'PENDING': return 'Pending'
    default: return 'Unknown'
  }
}

const loadEmployeeData = async () => {
  loadingEmployee.value = true
  try {
    console.log('🔍 Loading employee data for ID:', props.employeeId)

    const response = await fetch(`${API_BASE_URL}/api/employees`)
    if (response.ok) {
      const data = await response.json()
      const employees = data.employees || data || []
      const employee = employees.find(emp => emp.id === props.employeeId)

      if (employee) {
        employeeData.value = {
          ...employee,
          fullName: employee.fullName || `${employee.firstName} ${employee.lastName}`,
          name: employee.name || employee.fullName || `${employee.firstName} ${employee.lastName}`
        }
        console.log('✅ Employee data loaded:', employeeData.value)
      }
    }
  } catch (error) {
    console.error('❌ Error loading employee data:', error)
  } finally {
    loadingEmployee.value = false
  }
}

const loadEmployeeOrders = async () => {
  loading.value = true
  try {
    console.log('📋 Loading orders for employee:', props.employeeId, 'date:', selectedDate.value)

    const response = await fetch(`${API_BASE_URL}/api/planning/employee/${props.employeeId}?date=${selectedDate.value}`)

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Employee orders loaded:', data)

      if (data.success) {
        orders.value = data.orders || []
        console.log(`📦 Found ${orders.value.length} orders for employee`)
      } else {
        orders.value = []
      }
    } else if (response.status === 404) {
      console.log('ℹ️ No orders found for employee')
      orders.value = []
    }

  } catch (error) {
    console.error('❌ Error loading employee orders:', error)
    orders.value = []
  } finally {
    loading.value = false
  }
}

const generatePlanningForEmployee = async () => {
  try {
    console.log('🚀 Generating planning for employee:', props.employeeId)
    // Redirect to main planning page or trigger generation
    alert('Planning generation for individual employees coming soon!')
  } catch (error) {
    console.error('Error generating planning:', error)
  }
}

const refreshData = async () => {
  await Promise.all([
    loadEmployeeData(),
    loadEmployeeOrders()
  ])
  emit('refresh')
}

// Add these methods to the <script setup> section of EmployeeDetailPage.vue
// Around line 150-200, after the existing methods

// ========== TABLE FORMATTING METHODS ==========

/**
 * Get status badge color classes
 */
const getStatusColor = (status: string) => {
  const colors = {
    'PENDING': 'bg-yellow-100 text-yellow-800',
    'IN_PROGRESS': 'bg-blue-100 text-blue-800',
    'PROCESSING': 'bg-blue-100 text-blue-800',
    'COMPLETED': 'bg-green-100 text-green-800',
    'ON_HOLD': 'bg-orange-100 text-orange-800',
    'CANCELLED': 'bg-red-100 text-red-800',
    'DELIVERED': 'bg-green-100 text-green-800'
  }
  return colors[status.toUpperCase()] || 'bg-gray-100 text-gray-800'
}

/**
 * Get status dot color for visual indicator
 */
const getStatusDotColor = (status: string) => {
  const colors = {
    'PENDING': 'bg-yellow-400',
    'IN_PROGRESS': 'bg-blue-400',
    'PROCESSING': 'bg-blue-400',
    'COMPLETED': 'bg-green-400',
    'ON_HOLD': 'bg-orange-400',
    'CANCELLED': 'bg-red-400',
    'DELIVERED': 'bg-green-400'
  }
  return colors[status.toUpperCase()] || 'bg-gray-400'
}

/**
 * Get priority badge color classes
 */
const getPriorityColor = (priority: string) => {
  const colors = {
    'HIGH': 'bg-red-100 text-red-800',
    'URGENT': 'bg-red-100 text-red-800',
    'MEDIUM': 'bg-yellow-100 text-yellow-800',
    'LOW': 'bg-green-100 text-green-800'
  }
  return colors[priority.toUpperCase()] || 'bg-gray-100 text-gray-800'
}

/**
 * Get priority icon
 */
const getPriorityIcon = (priority: string) => {
  const icons = {
    'HIGH': '🔥',
    'URGENT': '⚡',
    'MEDIUM': '➖',
    'LOW': '📝'
  }
  return icons[priority.toUpperCase()] || '➖'
}

/**
 * Format price with currency
 */
const formatPrice = (price: number | string) => {
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '-'

  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(numPrice)
}

/**
 * Format date for display
 */
const formatDate = (dateStr: string | Date) => {
  if (!dateStr) return '-'

  try {
    const date = new Date(dateStr)
    return new Intl.DateTimeFormat('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(date)
  } catch {
    return '-'
  }
}

/**
 * Format time ago (e.g., "2 days ago")
 */
const formatTimeAgo = (dateStr: string | Date) => {
  if (!dateStr) return ''

  try {
    const date = new Date(dateStr)
    const now = new Date()
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

    if (diffInSeconds < 60) return 'Just now'
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}min ago`
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`
    if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)}d ago`

    return formatDate(date)
  } catch {
    return ''
  }
}

/**
 * Start processing an order
 */
const startOrder = async (order: any) => {
  try {
    console.log('🚀 Starting order processing:', order.orderNumber)

    // Simulate API call to start order
    const response = await fetch(`${API_BASE_URL}/api/orders/${order.id || order.orderId}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.ok) {
      // Update order status locally
      order.status = 'IN_PROGRESS'

      // ✅ Use injected notification system (no emit error)
      if (showNotification) {
        showNotification(`Started processing order ${order.orderNumber}`, 'success')
      }

      // Refresh data
      await loadEmployeeOrders()
      emit('refresh')

    } else {
      throw new Error(`HTTP ${response.status}`)
    }

  } catch (error) {
    console.error('❌ Error starting order:', error)

    // ✅ Use injected notification system
    if (showNotification) {
      showNotification(`Failed to start order ${order.orderNumber}`, 'error')
    }
  }
}

// ========== COMPUTED PROPERTIES FOR TABLE ==========

/**
 * Calculate total cards across all orders
 */
const totalCards = computed(() => {
  return orders.value.reduce((sum, order) => {
    return sum + (order.cardCount || order.cardsWithName || order.totalCards || 0)
  }, 0)
})

/**
 * Calculate total duration across all orders
 */
const totalDuration = computed(() => {
  return orders.value.reduce((sum, order) => {
    const duration = order.estimatedDuration ||
      order.durationMinutes ||
      (order.cardCount || 0) * 3 // 3 minutes per card default
    return sum + duration
  }, 0)
})



/**
 * Toggle order cards display with loading and API call
 */
const toggleOrderCards = async (order: any) => {
  // If already showing, just hide
  if (order.showCards) {
    order.showCards = false
    return
  }

  // If cards not loaded yet, fetch them
  if (!order.cards || order.cards.length === 0) {
    await loadOrderCards(order)
  }

  // Show the cards
  order.showCards = true
}

/**
 * Load cards for a specific order
 */
const loadOrderCards = async (order: any) => {
  try {
    order.loadingCards = true

    const orderId = order.orderId || order.id
    console.log('🃏 Loading cards for order:', orderId)

    const response = await fetch(`${API_BASE_URL}/api/planning/order/${orderId}/cards`)

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Order cards loaded:', data)

      if (data.success && data.cards) {
        order.cards = data.cards
        console.log(`📦 Loaded ${data.cards.length} cards for order ${order.orderNumber}`)
      } else {
        order.cards = []
        console.log('ℹ️ No cards found for order')
      }
    } else if (response.status === 404) {
      order.cards = []
      console.log('ℹ️ No cards found for order (404)')
    } else {
      throw new Error(`HTTP ${response.status}`)
    }

  } catch (error) {
    console.error('❌ Error loading order cards:', error)
    order.cards = []

    // Show error notification if available
    if (showNotification) {
      showNotification(`Failed to load cards for order ${order.orderNumber}`, 'error')
    }
  } finally {
    order.loadingCards = false
  }
}

/**
 * Initialize orders with default card state
 */
const initializeOrdersCardState = () => {
  orders.value.forEach(order => {
    if (!order.hasOwnProperty('showCards')) {
      order.showCards = false
    }
    if (!order.hasOwnProperty('loadingCards')) {
      order.loadingCards = false
    }
    if (!order.hasOwnProperty('cards')) {
      order.cards = []
    }
  })
}

/**
 * Close all expanded cards
 */
const closeAllCards = () => {
  orders.value.forEach(order => {
    order.showCards = false
  })
}

/**
 * Enhanced viewOrderDetails - now toggles card display
 */
const viewOrderDetails = (order: any) => {
  console.log('👁️ View order details (toggling cards):', order.orderNumber)
  toggleOrderCards(order)
}

// ========== WATCHERS ==========

// Watch for orders changes to initialize card state
watch(orders, (newOrders) => {
  if (newOrders && newOrders.length > 0) {
    initializeOrdersCardState()
  }
}, { deep: true, immediate: true })

// ========== ENHANCED LOADING METHOD ==========

const loadEmployeeOrdersEnhanced = async () => {
  loading.value = true
  try {
    console.log('📋 Loading orders for employee:', props.employeeId, 'date:', selectedDate.value)

    const response = await fetch(`${API_BASE_URL}/api/planning/employee/${props.employeeId}?date=${selectedDate.value}`)

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Employee orders loaded:', data)

      if (data.success) {
        orders.value = data.orders || []

        // Initialize card state for all orders
        initializeOrdersCardState()

        console.log(`📦 Found ${orders.value.length} orders for employee`)
      } else {
        orders.value = []
      }
    } else if (response.status === 404) {
      console.log('ℹ️ No orders found for employee')
      orders.value = []
    }

  } catch (error) {
    console.error('❌ Error loading employee orders:', error)
    orders.value = []
  } finally {
    loading.value = false
  }
}

// ========== KEYBOARD SHORTCUTS ==========

/**
 * Handle keyboard shortcuts
 */
const handleKeydown = (event: KeyboardEvent) => {
  // ESC key - close all expanded cards
  if (event.key === 'Escape') {
    closeAllCards()
  }
}


/**
 * Format time from datetime string (HH:MM format)
 */
const formatTime = (datetime) => {
  if (!datetime) return 'N/A'

  try {
    // If it's already in HH:MM format
    if (typeof datetime === 'string' && datetime.match(/^\d{2}:\d{2}$/)) {
      return datetime
    }

    // If it's a full datetime string
    const date = new Date(datetime)
    if (isNaN(date.getTime())) return 'N/A'

    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return 'N/A'
  }
}

/**
 * Get color classes for order status (from order table, not planning)
 */
const getOrderStatusColor = (status) => {
  // Status from order table: 1=A_RECEPTIONNER, 2=A_NOTER, 3=A_CERTIFIER, etc.
  switch (parseInt(status)) {
    case 1: return 'bg-blue-100 text-blue-800'      // A_RECEPTIONNER
    case 2: return 'bg-yellow-100 text-yellow-800'  // A_NOTER
    case 3: return 'bg-purple-100 text-purple-800'  // A_CERTIFIER
    case 4: return 'bg-orange-100 text-orange-800'  // A_PREPARER
    case 5: return 'bg-green-100 text-green-800'    // ENVOYEE
    case 8: return 'bg-gray-100 text-gray-800'      // RECU
    default: return 'bg-gray-100 text-gray-800'
  }
}

/**
 * Get dot color for order status
 */
const getOrderStatusDotColor = (status) => {
  switch (parseInt(status)) {
    case 1: return 'bg-blue-600'
    case 2: return 'bg-yellow-600'
    case 3: return 'bg-purple-600'
    case 4: return 'bg-orange-600'
    case 5: return 'bg-green-600'
    case 8: return 'bg-gray-600'
    default: return 'bg-gray-600'
  }
}

/**
 * Get text for order status
 */
const getOrderStatusText = (status) => {
  switch (parseInt(status)) {
    case 1: return 'To Receive'
    case 2: return 'To Note'
    case 3: return 'To Certify'
    case 4: return 'To Prepare'
    case 5: return 'Sent'
    case 8: return 'Received'
    default: return 'Unknown'
  }
}

/**
 * Get color classes for delai
 */
const getDelaiColor = (delai:string) => {
  if (!delai) return 'bg-gray-100 text-gray-800'

  switch (delai.toUpperCase()) {
    case 'X': return 'bg-red-100 text-red-800'
    case 'F+': return 'bg-orange-100 text-orange-800'
    case 'F': return 'bg-yellow-100 text-yellow-800'
    case 'C':
    case 'E': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

/**
 * Get label for delai
 */
const getDelaiLabel = (delai:string) => {
  if (!delai) return 'Unknown'

  switch (delai.toUpperCase()) {
    case 'X': return '🔴 Excelsior'
    case 'F+': return '🟠 Fast+'
    case 'F': return '🟡 Fast'
    case 'C': return '🟢 Classic'
    case 'E': return '🟢 Economy'
    default: return delai
  }
}



// ========== LIFECYCLE HOOKS ==========

import { onUnmounted } from 'vue'

onMounted(() => {
  // Add keyboard event listener
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  // Remove keyboard event listener
  document.removeEventListener('keydown', handleKeydown)
})

// ========== WATCHERS ==========
watch(() => props.employeeId, refreshData, { immediate: false })
watch(selectedDate, loadEmployeeOrders)

// ========== LIFECYCLE ==========
onMounted(refreshData)
</script>
