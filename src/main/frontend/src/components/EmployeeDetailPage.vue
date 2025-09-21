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
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="order in orders"
              :key="order.id || order.orderId"
              class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
            >
              <!-- Order Header -->
              <div class="flex items-center justify-between mb-3">
                <div>
                  <h4 class="font-semibold text-gray-900">{{ order.orderNumber || order.numCommande }}</h4>
                  <p class="text-sm text-gray-600">{{ order.cardCount || 0 }} cards</p>
                </div>
                <span :class="[
                  'px-2 py-1 rounded text-xs font-medium',
                  getPriorityColor(order.priority)
                ]">
                  {{ order.priority || 'MEDIUM' }}
                </span>
              </div>

              <!-- Time Info -->
              <div class="space-y-2 mb-3">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Duration:</span>
                  <span class="font-medium">{{ formatDuration(order.estimatedDuration || order.durationMinutes || (order.cardCount * 3)) }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Start Time:</span>
                  <span class="font-medium">{{ order.startTime || '09:00' }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">End Time:</span>
                  <span class="font-medium">{{ order.endTime || calculateEndTime(order) }}</span>
                </div>
              </div>

              <!-- Progress Bar -->
              <div class="mb-3">
                <div class="flex justify-between text-xs text-gray-600 mb-1">
                  <span>Progress</span>
                  <span>{{ order.progressPercentage || 0 }}%</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2">
                  <div
                    class="bg-blue-600 h-2 rounded-full transition-all duration-300"
                    :style="`width: ${order.progressPercentage || 0}%`"
                  ></div>
                </div>
              </div>

              <!-- Status -->
              <div class="flex items-center justify-between">
                <span :class="[
                  'px-2 py-1 rounded text-xs font-medium',
                  getStatusColor(order.status)
                ]">
                  {{ getStatusText(order.status) }}
                </span>
                <button
                  @click="viewOrderDetails(order)"
                  class="text-blue-600 hover:text-blue-800 text-sm font-medium"
                >
                  View Details →
                </button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { API_BASE_URL } from '@/config/api'
import EmployeeGroupsCard from "./groups/EmployeeGroupsCard.vue"

// ========== PROPS ==========
const props = defineProps<{
  employeeId: string
  selectedDate: string
  mode?: string
}>()

const emit = defineEmits<{
  back: []
  refresh: []
}>()

// ========== STATE ==========
const loading = ref(false)
const loadingEmployee = ref(false)
const employeeData = ref<any>(null)
const orders = ref<any[]>([])
const selectedDate = ref(props.selectedDate)

// ========== COMPUTED ==========
const totalCards = computed(() => {
  return orders.value.reduce((sum, order) => sum + (order.cardCount || 0), 0)
})

const totalDuration = computed(() => {
  return orders.value.reduce((sum, order) => sum + (order.estimatedDuration || order.durationMinutes || (order.cardCount * 3)), 0)
})

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

const getPriorityColor = (priority: string) => {
  switch (priority?.toUpperCase()) {
    case 'URGENT': return 'bg-red-100 text-red-800'
    case 'HIGH': return 'bg-orange-100 text-orange-800'
    case 'MEDIUM': return 'bg-yellow-100 text-yellow-800'
    case 'LOW': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatusColor = (status: any) => {
  switch (status?.toString()) {
    case '2':
    case 'COMPLETED': return 'bg-green-100 text-green-800'
    case '1':
    case 'IN_PROGRESS': return 'bg-blue-100 text-blue-800'
    case '0':
    case 'PENDING': return 'bg-gray-100 text-gray-800'
    default: return 'bg-gray-100 text-gray-800'
  }
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

const viewOrderDetails = (order: any) => {
  console.log('📋 View order details:', order)
  // Implement order detail view
  alert(`Order Details: ${order.orderNumber}\nCards: ${order.cardCount}\nDuration: ${formatDuration(order.estimatedDuration || order.durationMinutes || 0)}`)
}

const refreshData = async () => {
  await Promise.all([
    loadEmployeeData(),
    loadEmployeeOrders()
  ])
  emit('refresh')
}

// ========== WATCHERS ==========
watch(() => props.employeeId, refreshData, { immediate: false })
watch(selectedDate, loadEmployeeOrders)

// ========== LIFECYCLE ==========
onMounted(refreshData)
</script>
