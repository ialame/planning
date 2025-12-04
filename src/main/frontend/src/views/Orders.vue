<template>
  <div class="orders-page">
    <h1 class="text-3xl font-bold text-gray-900 mb-8 flex items-center gap-3">
      <svg class="w-8 h-8 text-[#730d10]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
      Orders Management
    </h1>

    <!-- DELAI STATISTICS -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-[#730d10]">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Excelsior (X)</p>
            <p class="text-2xl font-bold text-[#730d10]">{{ formatNumber(statistics.X?.count || 0) }}</p>
            <p class="text-xs text-gray-500 mt-1">{{ formatNumber(statistics.X?.cards || 0) }} cards</p>
          </div>
          <div class="text-[#730d10]">
            <svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" fill="#730d10"/>
              <circle cx="12" cy="12" r="8" fill="white"/>
              <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
            </svg>
          </div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-[#730d10]">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Fast+ (F+)</p>
            <p class="text-2xl font-bold text-[#730d10]">{{ formatNumber(statistics['F+']?.count || 0) }}</p>
            <p class="text-xs text-gray-500 mt-1">{{ formatNumber(statistics['F+']?.cards || 0) }} cards</p>
          </div>
          <div class="text-[#730d10]">
            <svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" fill="#730d10"/>
              <circle cx="12" cy="12" r="8" fill="white"/>
              <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
            </svg>
          </div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-[#730d10]">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Fast (F)</p>
            <p class="text-2xl font-bold text-[#730d10]">{{ formatNumber(statistics.F?.count || 0) }}</p>
            <p class="text-xs text-gray-500 mt-1">{{ formatNumber(statistics.F?.cards || 0) }} cards</p>
          </div>
          <div class="text-[#730d10]">
            <svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" fill="#730d10"/>
              <circle cx="12" cy="12" r="8" fill="white"/>
              <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
            </svg>
          </div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-[#730d10]">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Classic (C)</p>
            <p class="text-2xl font-bold text-[#730d10]">{{ formatNumber(statistics.C?.count || 0) }}</p>
            <p class="text-xs text-gray-500 mt-1">{{ formatNumber(statistics.C?.cards || 0) }} cards</p>
          </div>
          <div class="text-[#730d10]">
            <svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" fill="#730d10"/>
              <circle cx="12" cy="12" r="8" fill="white"/>
              <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
              <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- STATUS STATISTICS -->
    <div class="bg-gray-50 rounded-lg p-6 mb-6" v-if="statusStatistics">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div v-for="(stat, status) in statusStatistics" :key="status" class="bg-white p-4 rounded-lg shadow-sm">
          <p class="text-sm font-medium text-gray-600">{{ getStatusText(status as string) }}</p>
          <p class="text-xl font-bold text-[#730d10]">{{ formatNumber(stat.count || 0) }}</p>
          <p class="text-xs text-gray-500 mt-1">{{ formatNumber(stat.cards || 0) }} cards</p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Search by Customer</label>
          <input
            v-model="filters.search"
            @input="debouncedSearch"
            type="text"
            placeholder="Customer name..."
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#730d10] focus:border-[#730d10]"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
          <select
            v-model="filters.status"
            @change="loadOrders(0)"
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#730d10] focus:border-[#730d10]"
          >
            <option value="all">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="GRADING">Grading</option>
            <option value="CERTIFYING">Certifying</option>
            <option value="SCANNING">Scanning</option>
            <option value="PACKAGING">Packaging</option>
            <option value="DELIVERED">Delivered</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Priority (Delai)</label>
          <select
            v-model="filters.delai"
            @change="loadOrders(0)"
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-[#730d10] focus:border-[#730d10]"
          >
            <option value="all">All Priorities</option>
            <option value="X">Excelsior (X)</option>
            <option value="F+">Fast+ (F+)</option>
            <option value="F">Fast (F)</option>
            <option value="C">Classic (C)</option>
          </select>
        </div>

        <div class="flex items-end">
          <button
            @click="loadOrders(0)"
            class="w-full bg-[#730d10] text-white px-4 py-2 rounded-md hover:bg-[#8a1c20] transition-colors flex items-center justify-center gap-2"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            Reload
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination Info with Card Totals -->
    <div class="bg-white rounded-lg shadow p-4 mb-6" v-if="pagination">
      <div class="flex items-center justify-between">
        <!-- Left: Order range -->
        <div class="text-sm text-gray-600">
          Showing {{ (pagination.page * pagination.size) + 1 }} to {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }} of {{ pagination.totalElements }} orders
        </div>

        <!-- Center: Card totals -->
        <div class="text-sm font-semibold text-[#730d10] flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
          </svg>
          {{ formatNumber(pagination.pageCardTotal) }} / {{ formatNumber(pagination.totalCards) }} cards
        </div>

        <!-- Right: Page number -->
        <div class="text-sm text-gray-600">
          Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-[#730d10] mx-auto"></div>
      <span class="text-gray-600 mt-3 block">Loading orders...</span>
    </div>

    <!-- Table -->
    <div v-else-if="orders.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Order</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Customer</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cards</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Delai</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
          </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ order.orderNumber }}</div>
              <div class="text-sm text-gray-500">{{ order.clientOrderNumber }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ order.customerName }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ order.date }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ order.cardCount }} cards</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="['inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium gap-1', getDelaiColor(order.delai)]">
                <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" :fill="getDelaiFillColor(order.delai)"/>
                  <circle cx="12" cy="12" r="8" fill="white"/>
                  <path d="M2 12H22" :stroke="getDelaiFillColor(order.delai)" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" fill="white" :stroke="getDelaiFillColor(order.delai)" stroke-width="2"/>
                  <circle cx="12" cy="12" r="1.5" :fill="getDelaiFillColor(order.delai)"/>
                </svg>
                {{ getDelaiLabel(order.delai) }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span class="text-sm text-gray-900">{{ getStatusText(order.status) }}</span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              €{{ formatPrice(order.totalPrice) }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else class="bg-white rounded-lg shadow p-12 text-center">
      <div class="flex justify-center mb-4">
        <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
        </svg>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">No Orders Found</h3>
      <p class="text-gray-600">No orders match your current filters</p>
    </div>

    <!-- Pagination Controls -->
    <div v-if="pagination && pagination.totalPages > 1" class="bg-white rounded-lg shadow p-4 mt-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <button
            @click="loadOrders(0)"
            :disabled="pagination.page === 0"
            :class="[
              'px-3 py-2 text-sm rounded-md flex items-center gap-1',
              pagination.page > 0
                ? 'bg-[#730d10] text-white hover:bg-[#8a1c20]'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7"/>
            </svg>
            First
          </button>

          <button
            @click="loadOrders(pagination.page - 1)"
            :disabled="pagination.page === 0"
            :class="[
              'px-3 py-2 text-sm rounded-md flex items-center gap-1',
              pagination.page > 0
                ? 'bg-[#730d10] text-white hover:bg-[#8a1c20]'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
            Previous
          </button>

          <span class="px-4 py-2 text-sm text-gray-700">
            Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}
          </span>

          <button
            @click="loadOrders(pagination.page + 1)"
            :disabled="pagination.page >= pagination.totalPages - 1"
            :class="[
              'px-3 py-2 text-sm rounded-md flex items-center gap-1',
              pagination.page < pagination.totalPages - 1
                ? 'bg-[#730d10] text-white hover:bg-[#8a1c20]'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            Next
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>

          <button
            @click="loadOrders(pagination.totalPages - 1)"
            :disabled="pagination.page >= pagination.totalPages - 1"
            :class="[
              'px-3 py-2 text-sm rounded-md flex items-center gap-1',
              pagination.page < pagination.totalPages - 1
                ? 'bg-[#730d10] text-white hover:bg-[#8a1c20]'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            Last
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import authService from '@/services/authService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// TypeScript Interfaces
interface Order {
  id: string
  orderNumber: string
  clientOrderNumber?: string
  customerName: string
  status: string
  delai: string
  cardCount: number
  totalPrice: number
  date: string
}

interface Pagination {
  page: number
  size: number
  totalElements: number
  totalPages: number
  pageCardTotal?: number
  totalCards?: number
}

interface DelaiStat {
  count: number
  cards: number
}

interface StatusStat {
  count: number
  cards: number
}

interface OrdersResponse {
  success?: boolean
  orders?: Order[]        //  Backend uses 'orders'
  content?: Order[]       // Support for 'content' too
  pagination: Pagination
  delaiStatistics: Record<string, DelaiStat>
  statusStatistics: Record<string, StatusStat>
}

// Reactive state
const orders = ref<Order[]>([])
const loading = ref<boolean>(false)
const error = ref<string | null>(null)
const pagination = ref<Pagination | null>(null)
const statistics = ref<Record<string, DelaiStat>>({})
const statusStatistics = ref<Record<string, StatusStat>>({})

const filters = ref({
  search: '',
  status: 'all',
  delai: 'all'
})

// Helper functions
const getDelaiLabel = (delai: string): string => {
  const labels: Record<string, string> = {
    'X': 'Excelsior',
    'F+': 'Fast+',
    'F': 'Fast',
    'C': 'Classic',
    'E': 'Economy'
  }
  return labels[delai] || delai
}

const getDelaiColor = (delai: string): string => {
  const colors: Record<string, string> = {
    'X': 'bg-[#730d10] text-white',
    'F+': 'bg-[#730d10] text-white',
    'F': 'bg-[#730d10] text-white',
    'C': 'bg-[#730d10] text-white',
    'E': 'bg-[#730d10] text-white'
  }
  return colors[delai] || 'bg-gray-100 text-gray-800'
}

const getDelaiFillColor = (delai: string): string => {
  const colors: Record<string, string> = {
    'X': '#730d10',
    'F+': '#730d10',
    'F': '#730d10',
    'C': '#730d10',
    'E': '#730d10'
  }
  return colors[delai] || '#6B7280'
}

const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    'PENDING': 'Pending',
    'GRADING': 'Grading',
    'CERTIFYING': 'Certifying',
    'SCANNING': 'Scanning',
    'PACKAGING': 'Packaging',
    'DELIVERED': 'Delivered'
  }
  return statusMap[status] || status
}

const formatPrice = (price: number): string => {
  return price ? price.toFixed(2) : '0.00'
}

const formatNumber = (num: number): string => {
  return num ? num.toLocaleString() : '0'
}

// Debounced search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
const debouncedSearch = (): void => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadOrders(0)
  }, 300)
}

// Flag to prevent concurrent loads
let isLoadingOrders = false

// Load orders with pagination
const loadOrders = async (page: number = 0): Promise<void> => {
  if (isLoadingOrders) {
    console.log(' Already loading orders, skipping...')
    return
  }

  if (!authService.isAuthenticated()) {
    console.log(' Not authenticated, cannot load orders')
    return
  }

  isLoadingOrders = true
  loading.value = true
  error.value = null

  try {
    console.log(' Loading orders - page:', page, 'filters:', filters.value)

    // Build query parameters
    const params = new URLSearchParams({
      page: page.toString(),
      size: '20'
    })

    if (filters.value.search) params.append('search', filters.value.search)
    if (filters.value.status !== 'all') params.append('status', filters.value.status)
    if (filters.value.delai !== 'all') params.append('delai', filters.value.delai)

    console.log(' Request URL:', `/api/orders?${params}`)

    // Use authService for authenticated request
    const ordersData = await authService.get<OrdersResponse>(`/api/orders?${params}`)

    console.log(' Full Response:', ordersData)

    //  Extract orders (support both 'orders' and 'content' fields)
    const ordersArray = ordersData.orders || ordersData.content || []

    console.log(' Extracted orders:', ordersArray.length)

    // Update state
    orders.value = ordersArray
    pagination.value = ordersData.pagination || null
    statistics.value = ordersData.delaiStatistics || {}
    statusStatistics.value = ordersData.statusStatistics || {}

    console.log(' State updated:')
    console.log('   - Orders:', orders.value.length)
    console.log('   - Pagination:', pagination.value)

    if (orders.value.length > 0) {
      console.log('   - First order:', orders.value[0].orderNumber)
    }

  } catch (err) {
    console.error(' Error loading orders:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load orders'

    if (err instanceof Error && err.message.includes('Authentication required')) {
      console.log(' Authentication required - please login')
    }
  } finally {
    loading.value = false
    isLoadingOrders = false
  }
}

// Load once on mount
onMounted(() => {
  console.log(' Orders component mounted')
  console.log(' Authenticated:', authService.isAuthenticated())
  console.log(' User:', authService.getUser())

  if (authService.isAuthenticated()) {
    loadOrders()
  } else {
    console.log(' Not authenticated - waiting for login')
  }
})
</script>

<style scoped>
.orders-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}
</style>