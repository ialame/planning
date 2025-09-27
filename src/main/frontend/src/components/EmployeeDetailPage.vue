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
                  Client Order #
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Priority
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Cards
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Duration
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Total Price
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Created
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

                  <!-- Client Order Number -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ order.clientOrderNumber || order.num_commande_client || '-' }}
                    </div>
                  </td>

                  <!-- Status -->
                  <td class="px-6 py-4 whitespace-nowrap">
            <span :class="[
              'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
              getStatusColor(order.status || 'PENDING')
            ]">
              <span class="w-1.5 h-1.5 mr-1.5 rounded-full" :class="getStatusDotColor(order.status || 'PENDING')"></span>
              {{ order.status || 'PENDING' }}
            </span>
                  </td>

                  <!-- Priority -->
                  <td class="px-6 py-4 whitespace-nowrap">
            <span :class="[
              'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
              getPriorityColor(order.priority || 'MEDIUM')
            ]">
              {{ getPriorityIcon(order.priority || 'MEDIUM') }} {{ order.priority || 'MEDIUM' }}
            </span>
                  </td>

                  <!-- Card Count -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                      <div class="text-sm font-medium text-gray-900">
                        {{ order.cardCount || order.cardsWithName || order.totalCards || 0 }}
                      </div>
                      <div class="text-xs text-gray-500 ml-1">cards</div>
                      <!-- Expand/Collapse indicator -->
                      <div
                        v-if="(order.cardCount || 0) > 0"
                        class="ml-2 transition-transform duration-200"
                        :class="{ 'rotate-90': order.showCards }"
                      >
                        <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                      </div>
                    </div>
                  </td>

                  <!-- Duration -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ formatDuration(order.estimatedDuration || order.durationMinutes || (order.cardCount || 0) * 3) }}
                    </div>
                    <div class="text-xs text-gray-500">
                      (~{{ Math.ceil((order.estimatedDuration || order.durationMinutes || (order.cardCount || 0) * 3) / 60) }}h)
                    </div>
                  </td>

                  <!-- Total Price -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm font-medium text-gray-900">
                      {{ formatPrice(order.totalPrice || order.prix_total || 0) }}
                    </div>
                  </td>

                  <!-- Created Date -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ formatDate(order.creationDate || order.date) }}
                    </div>
                    <div class="text-xs text-gray-500">
                      {{ formatTimeAgo(order.creationDate || order.date) }}
                    </div>
                  </td>

                  <!-- Actions -->
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div class="flex items-center space-x-2">
                      <button
                        @click="toggleOrderCards(order)"
                        class="text-blue-600 hover:text-blue-900 transition-colors p-1 rounded"
                        :class="{ 'bg-blue-100': order.showCards }"
                        title="View Cards"
                      >
                        <div class="flex items-center space-x-1">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                          </svg>
                          <span v-if="order.loadingCards" class="text-xs">...</span>
                        </div>
                      </button>
                      <button
                        @click="startOrder(order)"
                        class="text-green-600 hover:text-green-900 transition-colors p-1 rounded"
                        title="Start Processing"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1.586a1 1 0 01.707.293l2.414 2.414a1 1 0 00.707.293H15M9 10v4a2 2 0 002 2h2a2 2 0 002-2v-4M9 10V9a2 2 0 012-2h2a2 2 0 012 2v1.01"></path>
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>

                <!-- ✅ EXPANDABLE CARDS ROW -->
                <tr v-if="order.showCards" class="bg-gray-50">
                  <td colspan="9" class="px-6 py-0">
                    <!-- Sliding animation container -->
                    <div
                      class="overflow-hidden transition-all duration-500 ease-in-out"
                      :class="{
                'max-h-96 opacity-100 py-4': order.showCards && !order.loadingCards,
                'max-h-0 opacity-0 py-0': !order.showCards || order.loadingCards
              }"
                    >
                      <!-- Loading State -->
                      <div v-if="order.loadingCards" class="flex items-center justify-center py-8">
                        <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mr-3"></div>
                        <span class="text-gray-600">Loading cards...</span>
                      </div>


                      <!-- Cards Display -->
                      <div v-else-if="order.cards && order.cards.length > 0">
                        <!-- Cards Header -->
                        <div class="flex items-center justify-between mb-4">
                          <h4 class="text-sm font-medium text-gray-900">
                            📋 Order Cards ({{ order.cards.length }})
                          </h4>
                          <button
                            @click="order.showCards = false"
                            class="text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-gray-100 transition-colors"
                            title="Close"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                          </button>
                        </div>

                        <!-- ✅ RESPONSIVE CARDS GRID -->
                        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 max-h-80 overflow-y-auto">
                          <div
                            v-for="card in order.cards"
                            :key="card.id"
                            class="bg-white rounded-lg border border-gray-200 shadow-sm hover:shadow-md hover:border-blue-300 transition-all duration-200 group"
                          >
                            <div class="p-4">
                              <!-- Card Header with Icon -->
                              <div class="flex items-start justify-between mb-3">
                                <div class="flex items-start space-x-2 flex-1 min-w-0">
                                  <!-- Card Type Icon -->
                                  <div class="flex-shrink-0">
                                    <div class="w-8 h-8 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-lg flex items-center justify-center">
                                      <span class="text-white text-xs font-bold">🃏</span>
                                    </div>
                                  </div>

                                  <!-- Card Info -->
                                  <div class="flex-1 min-w-0">
                                    <h5 class="text-sm font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
                                      {{ card.name || `Card #${card.code_barre}` }}
                                    </h5>
                                    <p class="text-xs text-gray-500 mt-1 line-clamp-2">
                                      {{ card.label_name || card.code_barre }}
                                    </p>
                                  </div>
                                </div>

                                <!-- Status Badge -->
                                <div class="flex-shrink-0 ml-2">
            <span class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-green-100 text-green-800">
              ✅ Ready
            </span>
                                </div>
                              </div>

                              <!-- Card Details Grid -->
                              <div class="space-y-2">
                                <!-- Barcode -->
                                <div class="flex items-center justify-between py-1">
            <span class="text-xs text-gray-500 flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
              Code
            </span>
                                  <span class="font-mono text-xs text-gray-900 bg-gray-100 px-2 py-1 rounded">
              {{ card.code_barre || '-' }}
            </span>
                                </div>

                                <!-- Duration -->
                                <div class="flex items-center justify-between py-1">
            <span class="text-xs text-gray-500 flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              Time
            </span>
                                  <span class="text-xs font-medium text-blue-600">
              {{ card.duration || 3 }} min
            </span>
                                </div>

                                <!-- Amount (if exists) -->
                                <div v-if="card.amount && card.amount > 0" class="flex items-center justify-between py-1">
            <span class="text-xs text-gray-500 flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"></path>
              </svg>
              Value
            </span>
                                  <span class="text-xs font-semibold text-green-600">
              {{ formatPrice(card.amount) }}
            </span>
                                </div>
                              </div>

                              <!-- Card Actions -->
                              <div class="mt-3 pt-3 border-t border-gray-100 flex justify-between items-center">
                                <button class="text-xs text-blue-600 hover:text-blue-800 font-medium hover:underline transition-all">
                                  View Details
                                </button>

                                <!-- Quick Actions -->
                                <div class="flex space-x-1">
                                  <button
                                    class="p-1 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded transition-all"
                                    title="Mark as processed"
                                  >
                                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                                    </svg>
                                  </button>
                                  <button
                                    class="p-1 text-gray-400 hover:text-yellow-600 hover:bg-yellow-50 rounded transition-all"
                                    title="Add note"
                                  >
                                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                                    </svg>
                                  </button>
                                </div>
                              </div>
                            </div>

                            <!-- Hover effect overlay -->
                            <div class="absolute inset-0 bg-blue-50 opacity-0 group-hover:opacity-10 rounded-lg transition-opacity pointer-events-none"></div>
                          </div>
                        </div>

                        <!-- Cards Summary Footer -->
                        <div class="mt-4 pt-4 border-t border-gray-200 bg-gray-50 -mx-6 -mb-4 px-6 py-3 rounded-b-lg">
                          <div class="flex flex-wrap items-center justify-between text-xs text-gray-600 gap-2">
                            <div class="flex items-center space-x-4">
        <span class="flex items-center">
          <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
          </svg>
          {{ order.cards.length }} cards
        </span>
                              <span class="flex items-center">
          <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          {{ formatDuration(order.cards.reduce((sum, card) => sum + (card.duration || 3), 0)) }}
        </span>
                            </div>

                            <div class="flex items-center space-x-2">
                              <!-- Progress indicator -->
                              <div class="flex items-center space-x-1">
                                <div class="w-2 h-2 bg-green-400 rounded-full"></div>
                                <span>{{ Math.floor(Math.random() * order.cards.length) }} processed</span>
                              </div>

                              <!-- Collapse button -->
                              <button
                                @click="order.showCards = false"
                                class="text-gray-500 hover:text-gray-700 text-xs underline ml-3"
                              >
                                Collapse ↑
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- Empty State -->
                      <div v-else class="text-center py-6 text-gray-500">
                        <svg class="mx-auto h-8 w-8 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                        </svg>
                        <p class="text-sm">No cards found for this order</p>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
              </tbody>
            </table>

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
const selectedDate = ref(props.selectedDate)

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

// ========== LIFECYCLE HOOKS ==========

import { onMounted, onUnmounted } from 'vue'

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
