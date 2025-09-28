<template>

  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- ✅ HEADER -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex justify-between items-center mb-4">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">📦 Orders Management</h1>
            <p class="text-gray-600">Real orders from Pokemon card database</p>
          </div>

          <div class="flex gap-3">
            <button
              @click="refreshOrders"
              :disabled="loading"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
            >
              <svg v-if="loading" class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span v-else>🔄</span>
              {{ loading ? 'Loading...' : 'Refresh' }}
            </button>

            <button
              @click="debugWorkingEndpoint"
              class="bg-orange-600 text-white px-4 py-2 rounded-lg hover:bg-orange-700"
            >
              🔧 Debug API
            </button>
            <button
              @click="testPriorityMapping"
              class="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700"
            >
              🧪 Test Priority Mapping
            </button>
          </div>
        </div>
      </div>

      <!-- ✅ DATE FILTER CONTROLS -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-lg font-semibold text-gray-900">📅 Date Filtering</h3>
            <p class="text-sm text-gray-600">Filter orders by creation date</p>
          </div>

          <div class="flex items-center gap-4">
            <label class="flex items-center gap-2">
              <input
                type="checkbox"
                v-model="enableDateFilter"
                class="rounded border-gray-300"
              />
              <span class="text-sm font-medium">Enable date filter</span>
            </label>

            <div v-if="enableDateFilter" class="flex items-center gap-2">
              <label class="text-sm font-medium">Show orders since:</label>
              <input
                type="date"
                v-model="filterFromDate"
                class="border border-gray-300 rounded-md px-3 py-2 text-sm"
              />
            </div>
          </div>
        </div>

        <div class="bg-gray-50 p-3 rounded-lg">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <span class="font-medium">Total in DB:</span>
              <span class="ml-2">{{ statistics.totalInDb }} orders</span>
            </div>
            <div>
              <span class="font-medium">Currently shown:</span>
              <span class="ml-2">{{ statistics.total }} orders</span>
            </div>
            <div>
              <span class="font-medium">Filter active:</span>
              <span class="ml-2">{{ enableDateFilter ? `Since ${filterFromDate}` : 'No filter' }}</span>
            </div>
            <div>
              <span class="font-medium">Cards:</span>
              <span class="ml-2">{{ statistics.totalCards }} total</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ QUICK STATISTICS -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <!-- To Receive -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-blue-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">To Receive</p>
              <p class="text-2xl font-bold text-blue-600">{{ statistics.toReceive }}</p>
            </div>
            <div class="text-3xl text-blue-600">📦</div>
          </div>
        </div>

        <!-- Package Accepted -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-indigo-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Package Accepted</p>
              <p class="text-2xl font-bold text-indigo-600">{{ statistics.packageAccepted }}</p>
            </div>
            <div class="text-3xl text-indigo-600">✅</div>
          </div>
        </div>

        <!-- In Processing -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-yellow-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">In Processing</p>
              <p class="text-2xl font-bold text-yellow-600">{{ statistics.inProcessing }}</p>
            </div>
            <div class="text-3xl text-yellow-600">⚙️</div>
          </div>
        </div>

        <!-- To Deliver -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-orange-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">To Deliver</p>
              <p class="text-2xl font-bold text-orange-600">{{ statistics.toDeliver }}</p>
            </div>
            <div class="text-3xl text-orange-600">🚚</div>
          </div>
        </div>

        <!-- Completed -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-green-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Completed</p>
              <p class="text-2xl font-bold text-green-600">{{ statistics.completed }}</p>
            </div>
            <div class="text-3xl text-green-600">🎉</div>
          </div>
        </div>
      </div>
      <!-- Replace the priority statistics section with: -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <!-- Excelsior Priority -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-red-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Excelsior</p>
              <p class="text-2xl font-bold text-red-600">{{ statistics.excelsior + statistics.urgent }}</p>
              <p class="text-xs text-gray-500">1 semaine</p>
            </div>
            <div class="text-3xl text-red-600">🔴</div>
          </div>
        </div>

        <!-- Fast+ Priority -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-orange-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Fast+</p>
              <p class="text-2xl font-bold text-orange-600">{{ statistics.fastPlus + statistics.high }}</p>
              <p class="text-xs text-gray-500">2 semaines</p>
            </div>
            <div class="text-3xl text-orange-600">🟠</div>
          </div>
        </div>

        <!-- Fast Priority -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-yellow-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Fast</p>
              <p class="text-2xl font-bold text-yellow-600">{{ statistics.fast + statistics.medium }}</p>
              <p class="text-xs text-gray-500">4 semaines</p>
            </div>
            <div class="text-3xl text-yellow-600">🟡</div>
          </div>
        </div>

        <!-- Classic Priority -->
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-green-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Classic</p>
              <p class="text-2xl font-bold text-green-600">{{ statistics.classic + statistics.low }}</p>
              <p class="text-xs text-gray-500">8 semaines</p>
            </div>
            <div class="text-3xl text-green-600">🟢</div>
          </div>
        </div>
      </div>

      <!-- ✅ SEARCH & FILTERS -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">🔍 Search & Filters</h3>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Search</label>
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Order number..."
              class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select v-model="filterStatus" class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500">
              <option value="all">All Statuses</option>
              <option value="1">To be received</option>
              <option value="9">Package accepted</option>
              <option value="10">To be scanned</option>
              <option value="11">To be opened</option>
              <option value="2">To be evaluated</option>
              <option value="3">To be encapsulated</option>
              <option value="4">To be prepared</option>
              <option value="7">To be unsealed</option>
              <option value="6">To be seen</option>
              <option value="41">To be delivered</option>
              <option value="42">To be sent</option>
              <option value="5">Sent</option>
              <option value="8">Received</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <select v-model="filterPriority" class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500">
              <option value="all">All Priorities</option>
              <option value="EXCELSIOR">Priorité Excelsior</option>
              <option value="FAST_PLUS">Priorité Fast+</option>
              <option value="FAST">Priorité Fast</option>
              <option value="CLASSIC">Priorité Classique</option>
            </select>
          </div>


          <div class="flex items-end">
            <button
              @click="loadOrders"
              class="w-full bg-gray-600 text-white px-4 py-2 rounded-md hover:bg-gray-700 transition-colors"
            >
              🔄 Reload Data
            </button>
          </div>
        </div>
      </div>

      <!-- ✅ LOADING STATE -->
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <span class="text-gray-600 mt-3 block">Loading real orders from database...</span>
      </div>

      <!-- ✅ ORDERS TABLE -->
      <div v-else-if="filteredOrders.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            📋 {{ filteredOrders.length }} Order{{ filteredOrders.length > 1 ? 's' : '' }}
          </h2>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cards</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quality</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Priority</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="order in filteredOrders" :key="order.id" class="hover:bg-gray-50 transition-colors">
              <!-- Order -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div>
                    <div class="text-sm font-medium text-gray-900">{{ order.orderNumber }}</div>
                    <div class="text-sm text-gray-500">ID: {{ order.id.slice(-8) }}</div>
                  </div>
                </div>
              </td>

              <!-- Date -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ formatDate(order.createdDate) }}</div>
              </td>

              <!-- Cards -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ order.cardCount }} cards</div>
                <div class="text-sm text-gray-500">
                  {{ order.cardsWithName || 0 }} with names ({{ order.namePercentage || 0 }}%)
                  <span :class="(order.namePercentage || 0) >= 95 ? 'text-green-600' : 'text-orange-600'">
                      {{ (order.namePercentage || 0) >= 95 ? '✅' : '⚠️' }}
                    </span>
                </div>
              </td>

              <!-- Quality -->
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="text-2xl">{{ getQualityIndicator(order.namePercentage || 0) }}</span>
              </td>

              <!-- Priority -->
              <td class="px-6 py-4 whitespace-nowrap">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                        :class="getPriorityColor(order.priority)">
                    {{ getPriorityLabel(order.priority) }}
                  </span>
              </td>

              <!-- Status -->
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  class="inline-flex px-2 py-1 text-xs font-semibold rounded-full"
                  :class="getStatusColor(order.status)"
                >
                  {{ getStatusText(order.status) }}
                </span>
              </td>

              <!-- Duration -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ order.estimatedDuration || 0 }} min</div>
                <div class="text-xs text-gray-500">{{ formatDuration(order.estimatedDuration || 0) }}</div>
              </td>

              <!-- Actions -->
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button
                    @click="viewDetails(order)"
                    class="text-blue-600 hover:text-blue-900 p-1 rounded hover:bg-blue-50"
                    title="View details"
                  >
                    👁️
                  </button>
                  <button
                    @click="viewCards(order)"
                    class="text-green-600 hover:text-green-900 p-1 rounded hover:bg-green-50"
                    title="View cards"
                  >
                    🃏
                  </button>
                  <button
                    v-if="order.status === 'PENDING'"
                    @click="startOrder(order.id)"
                    class="text-purple-600 hover:text-purple-900 p-1 rounded hover:bg-purple-50"
                    title="Start"
                  >
                    ▶️
                  </button>
                  <button
                    v-if="order.status === 'IN_PROGRESS'"
                    @click="completeOrder(order.id)"
                    class="text-green-600 hover:text-green-900 p-1 rounded hover:bg-green-50"
                    title="Complete"
                  >
                    ✅
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ✅ EMPTY STATE -->
      <div v-else-if="!loading" class="text-center py-12">
        <div class="text-gray-500">
          <div class="text-4xl mb-4">📦</div>
          <div class="text-lg font-medium mb-2">{{ orders.length === 0 ? 'No orders found' : 'No orders match your filters' }}</div>
          <div class="text-sm">{{ orders.length === 0 ? 'Check if your backend is running' : 'Try adjusting your search criteria' }}</div>
          <button
            @click="loadOrders"
            class="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
            🔄 Reload
          </button>
        </div>
      </div>

      <!-- ✅ MODALS -->

      <!-- Details Modal -->
      <div v-if="showDetailsModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-lg font-semibold">Details - {{ selectedOrder?.orderNumber }}</h3>
              <button @click="showDetailsModal = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>

            <div v-if="selectedOrder" class="space-y-3">
              <p><strong>ID:</strong> {{ selectedOrder.id }}</p>
              <p><strong>Created Date:</strong> {{ selectedOrder.createdDate }}</p>
              <p><strong>Card Count:</strong> {{ selectedOrder.cardCount }}</p>
              <p><strong>Cards with Names:</strong> {{ selectedOrder.cardsWithName }} ({{ selectedOrder.namePercentage }}%)</p>
              <p><strong>Priority:</strong> {{ selectedOrder.priority }}</p>
              <p><strong>Status:</strong> {{ selectedOrder.status }}</p>
              <p><strong>Estimated Duration:</strong> {{ selectedOrder.estimatedDuration }} minutes</p>
              <p><strong>Total Price:</strong> ${{ selectedOrder.totalPrice || 0 }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Cards Modal -->
      <div v-if="showCardsModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg max-w-4xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-lg font-semibold">🃏 Cards - {{ selectedOrder?.orderNumber }}</h3>
              <button @click="showCardsModal = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>

            <div v-if="loadingCards" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <span class="text-gray-600 mt-2 block">Loading cards...</span>
            </div>

            <div v-else-if="orderCards" class="space-y-4">
              <!-- Summary Statistics -->
              <div class="bg-gray-50 p-4 rounded-lg">
                <h4 class="font-semibold text-gray-700 mb-3">📊 Card Statistics</h4>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                  <div class="text-center">
                    <span class="block text-xs font-medium text-gray-600">Total Cards</span>
                    <span class="text-lg font-bold text-blue-600">{{ orderCards.cardCount }}</span>
                  </div>
                  <div class="text-center">
                    <span class="block text-xs font-medium text-gray-600">With Names</span>
                    <span class="text-lg font-bold text-green-600">{{ orderCards.cardsWithName }}</span>
                  </div>
                  <div class="text-center">
                    <span class="block text-xs font-medium text-gray-600">Percentage</span>
                    <span class="text-lg font-bold" :class="orderCards.namePercentage >= 95 ? 'text-green-600' : orderCards.namePercentage >= 80 ? 'text-yellow-600' : 'text-red-600'">
                {{ orderCards.namePercentage }}%
              </span>
                  </div>
                  <div class="text-center">
                    <span class="block text-xs font-medium text-gray-600">Est. Duration</span>
                    <span class="text-lg font-bold text-purple-600">{{ orderCards.cardCount * 3 }}min</span>
                  </div>
                </div>

                <!-- Status indicators -->
                <div class="flex items-center justify-between mt-3 pt-3 border-t">
                  <div class="flex items-center space-x-4 text-xs">
              <span v-if="orderCards.success" class="flex items-center text-green-600">
                <span class="w-2 h-2 bg-green-500 rounded-full mr-1"></span>
                API Success
              </span>
                    <span v-if="orderCards.fallback" class="flex items-center text-yellow-600">
                <span class="w-2 h-2 bg-yellow-500 rounded-full mr-1"></span>
                Fallback Endpoint
              </span>
                    <span v-if="orderCards.error" class="flex items-center text-red-600">
                <span class="w-2 h-2 bg-red-500 rounded-full mr-1"></span>
                API Error
              </span>
                  </div>
                </div>
              </div>

              <!-- Error message if API failed -->
              <div v-if="orderCards.error" class="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                <div class="flex items-center">
                  <span class="text-yellow-600 mr-2">⚠️</span>
                  <div>
                    <span class="text-sm text-yellow-800 font-medium">API Issues:</span>
                    <span class="text-sm text-yellow-700 ml-1">{{ orderCards.error }}</span>
                    <div class="text-xs text-yellow-600 mt-1">Showing cached order data instead</div>
                  </div>
                </div>
              </div>

              <!-- Individual Cards List -->
              <div v-if="orderCards.cards && orderCards.cards.length > 0" class="space-y-2">
                <div class="flex justify-between items-center">
                  <h4 class="font-semibold text-gray-700">📋 Individual Cards</h4>
                  <span class="text-sm text-gray-500">{{ orderCards.cards.length }} cards loaded</span>
                </div>

                <div class="max-h-64 overflow-y-auto space-y-1 border rounded-lg p-2 bg-white">
                  <div
                    v-for="(card, index) in orderCards.cards"
                    :key="card.id || index"
                    class="flex items-center justify-between bg-gray-50 border rounded p-2 text-sm hover:bg-gray-100 transition-colors"
                  >
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center space-x-2">
                  <span class="font-mono text-xs text-gray-500 bg-white px-2 py-1 rounded border">
                    {{ card.code_barre || 'N/A' }}
                  </span>
                        <span class="text-gray-900 truncate">
                    {{ card.name || 'Unnamed Card' }}
                  </span>
                      </div>
                      <div v-if="card.label_name && card.label_name !== card.name" class="text-xs text-gray-500 mt-1">
                        Label: {{ card.label_name }}
                      </div>
                    </div>
                    <div class="flex items-center space-x-2 text-xs text-gray-500">
                <span class="bg-blue-100 text-blue-700 px-2 py-1 rounded">
                  Pokemon
                </span>
                      <span class="text-purple-600 font-medium">{{ card.duration || 3 }}min</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- No individual cards available -->
              <div v-else class="text-center py-6 text-gray-500 bg-gray-50 rounded-lg">
                <span class="text-3xl block mb-2">🃏</span>
                <p class="font-medium">No individual card details available</p>
                <p class="text-xs mt-1">Order has {{ orderCards.cardCount }} cards but details are not loaded</p>
              </div>

              <!-- Quality Indicator -->
              <div class="bg-white border rounded-lg p-3">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-gray-600">Quality Status:</span>
                  <div class="flex items-center space-x-2">
              <span class="text-lg">
                {{ orderCards.namePercentage >= 95 ? '🟢' : orderCards.namePercentage >= 80 ? '🟡' : '🔴' }}
              </span>
                    <span class="text-sm font-medium" :class="orderCards.namePercentage >= 95 ? 'text-green-600' : orderCards.namePercentage >= 80 ? 'text-yellow-600' : 'text-red-600'">
                {{ orderCards.namePercentage >= 95 ? 'Excellent' : orderCards.namePercentage >= 80 ? 'Good' : 'Needs Attention' }}
              </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="text-gray-500 text-center py-8">
              <span class="text-3xl block mb-2">❌</span>
              <p>No card data available</p>
              <p class="text-xs mt-1">Unable to load card information for this order</p>
            </div>

            <!-- Action Buttons -->
            <div class="flex justify-end space-x-2 mt-4 pt-4 border-t">
              <button
                @click="refreshOrderCards"
                :disabled="loadingCards"
                class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                🔄 Refresh
              </button>
              <button
                @click="showCardsModal = false"
                class="bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api'
// ========== STATE ==========
const loading = ref(false)
const orders = ref([])
const selectedOrder = ref(null)
const showDetailsModal = ref(false)
const showCardsModal = ref(false)
const orderCards = ref(null)
const loadingCards = ref(false)

// Date filtering
const enableDateFilter = ref(true)
const filterFromDate = ref('2025-06-01')

// Other filters
const filterStatus = ref('all')
const filterPriority = ref('all')
const searchTerm = ref('')


const testLoad = async () => {
  console.log('🧪 TEST LOAD START')

  try {
    loading.value = true
    console.log('🧪 Loading set to true')

    const response = await fetch('http://localhost:8080/api/orders')
    console.log('🧪 Response status:', response.status)
    console.log('🧪 Response ok:', response.ok)

    if (response.ok) {
      const data = await response.json()
      console.log('🧪 Data received:', data)
      console.log('🧪 Data type:', typeof data)
      console.log('🧪 Is array:', Array.isArray(data))
      console.log('🧪 Length:', data.length)

      if (data.length > 0) {
        console.log('🧪 First item:', data[0])
      }

      // DIRECT assignment
      orders.value = data
      console.log('🧪 Orders.value after assignment:', orders.value)
      console.log('🧪 Orders.value length:', orders.value.length)

    } else {
      console.error('🧪 Response not OK')
    }

  } catch (error) {
    console.error('🧪 Error:', error)
  } finally {
    loading.value = false
    console.log('🧪 Loading set to false')
  }

  console.log('🧪 TEST LOAD END')
}



// ========== COMPUTED ==========
const filteredOrders = computed(() => {
  let filtered = [...orders.value]

  // Date filter (applied first)
  if (enableDateFilter.value && filterFromDate.value) {
    filtered = filtered.filter(order => {
      const orderDate = order.createdDate || order.orderDate || order.date
      if (!orderDate) return true

      const dateOnly = orderDate.split('T')[0]
      return dateOnly >= filterFromDate.value
    })
  }

  // Status filter
  if (filterStatus.value !== 'all') {
    // Convert filterStatus to number for comparison
    const statusToMatch = parseInt(filterStatus.value)
    filtered = filtered.filter(order => order.status === statusToMatch)
  }


  // Priority filter
  if (filterPriority.value !== 'all') {
    filtered = filtered.filter(order => order.priority === filterPriority.value)
  }

  // Search filter
  if (searchTerm.value) {
    const term = searchTerm.value.toLowerCase()
    filtered = filtered.filter(order =>
      order.orderNumber.toLowerCase().includes(term) ||
      order.id?.toString().toLowerCase().includes(term)
    )
  }

  return filtered
})

// Add this to Orders.vue for testing the corrected mapping:
const testPriorityMapping = async () => {
  try {
    console.log('🧪 Testing priority mapping...')

    const response = await fetch(`${API_BASE_URL}/api/orders/test/priority-mapping`)
    if (response.ok) {
      const data = await response.json()
      console.log('✅ Priority mapping test results:', data)

      // Show priority distribution
      console.log('📊 Delai distribution in database:')
      Object.entries(data.delaiStatistics).forEach(([delai, count]) => {
        const mapped = data.mapping[delai] || 'UNMAPPED'
        console.log(`  ${delai} → ${mapped}: ${count} orders`)
      })

      // Show sample orders
      console.log('📋 Sample orders with mapping:')
      data.sampleOrders.slice(0, 10).forEach(order => {
        console.log(`  ${order.orderNumber}: ${order.delai} → ${order.mappedPriority}`)
      })

      alert(`✅ Test completed! Check console for details.\n\nFound ${Object.keys(data.delaiStatistics).length} different delai values in database.`)
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('❌ Priority mapping test failed:', error)
    alert('❌ Test failed. Check console for details.')
  }
}


// In the frontend, update the priority mapping to handle the new values:
const mapPriority = (priority) => {
  if (!priority) return 'FAST' // Default changed from MEDIUM to FAST
  const p = String(priority).toUpperCase()

  // Handle new priority values
  if (p === 'EXCELSIOR' || p === 'X') return 'EXCELSIOR'
  if (p === 'FAST_PLUS' || p === 'FAST+' || p === 'FPLUS') return 'FAST_PLUS'
  if (p === 'FAST' || p === 'F') return 'FAST'
  if (p === 'CLASSIC' || p === 'C' || p === 'E') return 'CLASSIC'

  // Legacy mapping for compatibility
  if (p.includes('URGENT')) return 'EXCELSIOR'
  if (p.includes('HIGH') || p.includes('HAUTE')) return 'FAST_PLUS'
  if (p.includes('MEDIUM')) return 'FAST'
  if (p.includes('LOW') || p.includes('BASSE')) return 'CLASSIC'

  return 'FAST' // Default fallback
}

// Replace the getPriorityColor function:
const getPriorityColor = (priority) => {
  switch (priority?.toUpperCase()) {
    case 'EXCELSIOR': return 'bg-red-100 text-red-800'
    case 'FAST_PLUS': return 'bg-orange-100 text-orange-800'
    case 'FAST': return 'bg-yellow-100 text-yellow-800'
    case 'CLASSIC': return 'bg-green-100 text-green-800'
    // Keep legacy support for transition period
    case 'URGENT': return 'bg-red-100 text-red-800'
    case 'HIGH': return 'bg-orange-100 text-orange-800'
    case 'MEDIUM': return 'bg-yellow-100 text-yellow-800'
    case 'LOW': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

// Replace the getPriorityLabel function:
const getPriorityLabel = (priority) => {
  switch (priority?.toUpperCase()) {
    case 'EXCELSIOR': return '🔴 Priorité Excelsior'
    case 'FAST_PLUS': return '🟠 Priorité Fast+'
    case 'FAST': return '🟡 Priorité Fast'
    case 'CLASSIC': return '🟢 Priorité Classique'
    // Keep legacy support for transition period
    case 'URGENT': return '🔴 Urgent'
    case 'HIGH': return '🟠 High'
    case 'MEDIUM': return '🟡 Medium'
    case 'LOW': return '🟢 Low'
    default: return '⚪ Unknown'
  }
}



const statistics = computed(() => {
  const total = filteredOrders.value.length
  const totalInDb = orders.value.length

  // Use the actual status codes instead of string values
  const toReceive = filteredOrders.value.filter(o => o.status === 1).length
  const packageAccepted = filteredOrders.value.filter(o => o.status === 9).length
  const inProcessing = filteredOrders.value.filter(o =>
    [10, 11, 2, 3, 4, 7, 6].includes(o.status)
  ).length
  const toDeliver = filteredOrders.value.filter(o => [41, 42].includes(o.status)).length
  const completed = filteredOrders.value.filter(o => [5, 8].includes(o.status)).length

  // Count new priorities
  const excelsior = filteredOrders.value.filter(o => o.priority === 'EXCELSIOR').length
  const fastPlus = filteredOrders.value.filter(o => o.priority === 'FAST_PLUS').length
  const fast = filteredOrders.value.filter(o => o.priority === 'FAST').length
  const classic = filteredOrders.value.filter(o => o.priority === 'CLASSIC').length

  // For backward compatibility, also count legacy priorities
  const urgent = filteredOrders.value.filter(o => o.priority === 'URGENT').length
  const high = filteredOrders.value.filter(o => o.priority === 'HIGH').length
  const medium = filteredOrders.value.filter(o => o.priority === 'MEDIUM').length
  const low = filteredOrders.value.filter(o => o.priority === 'LOW').length



  const totalCards = filteredOrders.value.reduce((sum, o) => sum + o.cardCount, 0)

  return {
    total,
    totalInDb,
    // New priorities
    excelsior,
    fastPlus,
    fast,
    classic,
    // Legacy priorities (for transition)
    urgent,
    high,
    medium,
    low,

    toReceive,
    packageAccepted,
    inProcessing,
    toDeliver,
    completed,
    totalCards
  }
})

// ========== METHODS ==========

const loadOrders = async () => {
  loading.value = true
  try {
    console.log('📦 Loading orders using SAME endpoint as Dashboard...')

    // Try commandes endpoint like Dashboard
    const commandesResponse = await fetch(API_BASE_URL+'/api/orders', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })

    if (commandesResponse.ok) {
      const commandesData = await commandesResponse.json()
      console.log('✅ Commandes data retrieved:', commandesData.length)

      // Map French commandes to English orders
      const mappedOrders = commandesData.map(cmd => ({
        id: cmd.id,
        orderNumber: cmd.orderNumber,
        createdDate: (cmd.creationDate || cmd.dateReception || cmd.date || '').split('T')[0],
        cardCount: cmd.cardCount || 0,
        cardsWithName: cmd.nombreAvecNom || 0,
        namePercentage: cmd.pourcentageAvecNom || 0,
        priority: mapPriority(cmd.priority),
        totalPrice: cmd.totalPrice || 0,
        status: mapStatus(cmd.status || cmd.statut),
        estimatedDuration: cmd.estimatedTimeMinutes || cmd.tempsEstimeMinutes || 0
      })).filter(order => order !== null)

      orders.value = mappedOrders
      console.log('📊 French commandes mapped to English orders:', mappedOrders.length)
      showNotification(`✅ ${mappedOrders.length} real orders loaded`)
      return
    }

    // Fallback to /api/orders
    const response = await fetch(API_BASE_URL+'/api/orders')
    if (response.ok) {
      const data = await response.json()
      orders.value = data.map(mapOrderFromApi).filter(order => order !== null)
      showNotification(`✅ ${orders.value.length} orders loaded (fallback)`)
    }

  } catch (error) {
    console.error('❌ Error loading orders:', error)
    showNotification('❌ Error loading orders', 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const mapOrderFromApi = (order) => {
  let orderDate = order.createdDate || order.dateCreation || order.date || '2025-06-01'
  if (orderDate.includes('T')) {
    orderDate = orderDate.split('T')[0]
  }

  return {
    id: order.id,
    orderNumber: order.orderNumber || order.numeroCommande || `ORD-${order.id}`,
    createdDate: order.creationDate,
    cardCount: order.cardCount || order.nombreCartes || 1,
    cardsWithName: order.cardsWithName || order.nombreAvecNom || 0,
    namePercentage: order.namePercentage || order.pourcentageAvecNom || 0,
    priority: mapPriority(order.priority || order.priorite || 'MEDIUM'),
    totalPrice: order.totalPrice || order.prixTotal || 0,
    status: mapStatus(order.status || order.statut || 'PENDING'),
    estimatedDuration: order.estimatedDuration || order.estimatedTimeMinutes || 0
  }
}


const mapStatus = (status) => {
  // If status is already a number, return it as is
  if (typeof status === 'number') {
    return status
  }

  // If status is a string, try to convert it
  const statusNum = parseInt(status)
  if (!isNaN(statusNum)) {
    return statusNum
  }

  // Fallback mapping for string values (if any old data exists)
  const stringToStatusMap = {
    'PENDING': 1,
    'TO_RECEIVE': 1,
    'PACKAGE_ACCEPTED': 9,
    'TO_SCAN': 10,
    'TO_OPEN': 11,
    'TO_EVALUATE': 2,
    'TO_CERTIFY': 3,
    'TO_PREPARE': 4,
    'TO_UNSEAL': 7,
    'TO_SEE': 6,
    'TO_DISTRIBUTE': 41,
    'TO_SEND': 42,
    'SENT': 5,
    'RECEIVED': 8,
    'IN_PROGRESS': 4, // Default to "to be prepared"
    'COMPLETED': 5    // Default to "sent"
  }

  return stringToStatusMap[String(status).toUpperCase()] || 1 // Default to "to be received"
}

const refreshOrders = () => loadOrders()

const debugWorkingEndpoint = async () => {
  try {
    const response = await fetch(API_BASE_URL+'/api/orders')
    if (response.ok) {
      const data = await response.json()
      console.log('🔍 Debug data:', data)
      alert(`Found ${data.length} orders. Check console for details.`)
    }
  } catch (error) {
    alert(`Error: ${error.message}`)
  }
}

const viewDetails = (order) => {
  selectedOrder.value = order
  showDetailsModal.value = true
}

const startOrder = (id) => console.log('Start order:', id)
const completeOrder = (id) => console.log('Complete order:', id)




const getStatusLabel = (status) => {
  switch (status?.toUpperCase()) {
    case 'PENDING': return 'Pending'
    case 'SCHEDULED': return 'Scheduled'
    case 'IN_PROGRESS': return 'In Progress'
    case 'COMPLETED': return 'Completed'
    default: return 'Unknown'
  }
}

const getStatusText = (status) => {
  const statusMap = {
    1: 'To be received',
    9: 'Package accepted',
    10: 'To be scanned',
    11: 'To be opened',
    2: 'To be evaluated',
    3: 'To be encapsulated',
    4: 'To be prepared',
    7: 'To be unsealed',
    6: 'To be seen',
    41: 'To be delivered',
    42: 'To be sent',
    5: 'Sent',
    8: 'Received'
  }
  return statusMap[status] || `Status ${status}`
}

const getStatusColor = (status) => {
  // Reception stage - blue
  if ([1, 9].includes(status)) return 'bg-blue-100 text-blue-800'

  // Processing stage - yellow
  if ([10, 11, 2, 3, 4, 7, 6].includes(status)) return 'bg-yellow-100 text-yellow-800'

  // Shipping stage - orange
  if ([41, 42].includes(status)) return 'bg-orange-100 text-orange-800'

  // Completed stage - green
  if ([5, 8].includes(status)) return 'bg-green-100 text-green-800'

  // Default - gray
  return 'bg-gray-100 text-gray-800'
}



const getQualityIndicator = (percentage) => {
  if (percentage >= 95) return '🟢'
  if (percentage >= 80) return '🟡'
  return '🔴'
}

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A'
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  } catch {
    return dateStr
  }
}

const formatDuration = (minutes) => {
  if (!minutes) return 'N/A'
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  if (hours > 0) {
    return `${hours}h ${remainingMinutes}m`
  }
  return `${remainingMinutes}m`
}

const showNotification = (message, type = 'success') => {
  console.log(`${type === 'success' ? '✅' : '❌'} ${message}`)
}

const refreshOrderCards = () => {
  if (selectedOrder.value) {
    viewCards(selectedOrder.value)
  }
}

// The correct endpoint is in EmployeesPlanningController: /api/planning/order/{orderId}/cards

const viewCards = async (order) => {
  console.log('🃏 Loading cards for order:', order.id)

  selectedOrder.value = order
  showCardsModal.value = true
  loadingCards.value = true
  orderCards.value = null

  try {
    // ✅ CORRECT ENDPOINT: Use EmployeesPlanningController endpoint
    const response = await fetch(`${API_BASE_URL}/api/planning/order/${order.id}/cards`)

    if (response.ok) {
      const cardData = await response.json()
      console.log('✅ Cards loaded from correct endpoint:', cardData)

      if (cardData.success && cardData.cards) {
        // Map the response from EmployeesPlanningController
        const cards = cardData.cards
        const cardsWithName = cards.filter(card =>
          card.name &&
          card.name.trim() !== '' &&
          !card.name.startsWith('Card #')
        ).length

        orderCards.value = {
          cards: cards,
          cardCount: cardData.total || cards.length,
          cardsWithName: cardsWithName,
          namePercentage: cards.length > 0 ? Math.round((cardsWithName / cards.length) * 100) : 0,
          orderId: order.id,
          success: true
        }

        console.log('📊 Card statistics from API:', {
          total: orderCards.value.cardCount,
          withNames: orderCards.value.cardsWithName,
          percentage: orderCards.value.namePercentage
        })

      } else {
        throw new Error(cardData.error || 'Invalid response format')
      }

    } else {
      console.error('❌ API Error:', response.status, response.statusText)

      // Try alternative endpoint as fallback
      await tryFallbackEndpoint(order)
    }

  } catch (error) {
    console.error('❌ Error loading cards:', error)

    // Try fallback endpoint
    await tryFallbackEndpoint(order)
  } finally {
    loadingCards.value = false
  }
}

// Fallback function to try alternative endpoints
const tryFallbackEndpoint = async (order) => {
  try {
    console.log('🔄 Trying fallback endpoint...')

    // Try OrderController endpoint as fallback
    const fallbackResponse = await fetch(`${API_BASE_URL}/api/orders/${order.id}/cards`)

    if (fallbackResponse.ok) {
      const fallbackData = await fallbackResponse.json()
      console.log('✅ Fallback data loaded:', fallbackData)

      orderCards.value = {
        cards: fallbackData.cards || [],
        cardCount: fallbackData.totalCards || fallbackData.cards?.length || order.cardCount,
        cardsWithName: fallbackData.cardsWithName || order.cardsWithName,
        namePercentage: fallbackData.namePercentage || order.namePercentage,
        orderId: order.id,
        fallback: true
      }
    } else {
      throw new Error(`Fallback also failed: ${fallbackResponse.status}`)
    }

  } catch (fallbackError) {
    console.error('❌ Fallback also failed:', fallbackError)

    // Ultimate fallback - use order data
    orderCards.value = {
      cards: [],
      cardCount: order.cardCount,
      cardsWithName: order.cardsWithName,
      namePercentage: order.namePercentage,
      orderId: order.id,
      error: `Both API endpoints failed. Main: ${fallbackError.message}`
    }
  }
}


onMounted(() => {
  console.log('📦 OrdersView mounted - Loading real orders...')
  loadOrders()
})
</script>
<style scoped>
.orders-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* Loading spinner */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Hover effects */
.hover\:bg-gray-50:hover {
  background-color: #f9fafb;
}

.transition-colors {
  transition-property: color, background-color, border-color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* Responsive design */
@media (max-width: 768px) {
  .orders-view {
    padding: 16px;
  }

  .overflow-x-auto {
    font-size: 0.875rem;
  }

  .grid-cols-1 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .md\:grid-cols-4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
