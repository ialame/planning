<script setup lang="ts">
// App.vue
// Main application component with OAuth2 authentication

import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { user, isAuthenticated, isLoading, error, logout, hasRole } = useAuth()

// Navigation tabs
const tabs = [
  { id: 'dashboard', path: '/', label: 'Dashboard', icon: 'dashboard', roles: [] },
  { id: 'orders', path: '/orders', label: 'Orders', icon: 'orders', roles: [] },
  { id: 'employees', path: '/employees', label: 'Employees', icon: 'employees', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
  { id: 'planning', path: '/planning', label: 'Planning', icon: 'planning', roles: [] },
  { id: 'groups', path: '/groups', label: 'Teams', icon: 'teams', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
  { id: 'sync', path: '/sync', label: 'Data Sync', icon: 'sync', roles: ['ROLE_ADMIN'] },
]

// Filter tabs based on user roles
const visibleTabs = computed(() => {
  return tabs.filter(tab => {
    if (tab.roles.length === 0) return true
    return tab.roles.some(role => hasRole(role))
  })
})

// Current active tab
const activeTab = computed(() => {
  const currentPath = route.path
  const tab = tabs.find(t => t.path === currentPath)
  return tab?.id || 'dashboard'
})

// Notification system
const notification = ref({
  show: false,
  message: '',
  type: 'success' as 'success' | 'error' | 'warning'
})

function showNotification(message: string, type: 'success' | 'error' | 'warning' = 'success') {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

// Handle logout
async function handleLogout() {
  try {
    await logout()
  } catch (err) {
    console.error('Logout error:', err)
    // Force redirect to home even on error
    window.location.href = '/'
  }
}

// Navigate to tab
function navigateTo(path: string) {
  router.push(path)
}

// Watch for auth errors
watch(error, (newError) => {
  if (newError) {
    showNotification(newError, 'error')
  }
})

// Icons as inline SVG components
const icons = {
  dashboard: `<path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/>`,
  orders: `<path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>`,
  employees: `<path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>`,
  planning: `<path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V9h14v10zM5 7V5h14v2H5zm2 4h10v2H7zm0 4h7v2H7z"/>`,
  teams: `<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>`,
  sync: `<path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"/>`,
  logout: `<path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>`,
  pokeball: `<circle cx="12" cy="12" r="10" fill="white" stroke="currentColor" stroke-width="2"/><path d="M2 12H22" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="white" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/>`,
}
</script>

<template>
  <div id="app" class="min-h-screen bg-gray-50">
    
    <!-- Loading Overlay -->
    <div v-if="isLoading" class="fixed inset-0 bg-white bg-opacity-80 flex items-center justify-center z-50">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p class="text-gray-600">Loading...</p>
      </div>
    </div>

    <!-- Main App (authenticated) -->
    <template v-if="isAuthenticated && !isLoading">
      
      <!-- Header Navigation -->
      <nav class="bg-gray-800 text-white shadow-lg">
        <div class="max-w-7xl mx-auto px-4">
          <div class="flex justify-between items-center h-16">
            
            <!-- Logo & Title -->
            <div class="flex items-center gap-3">
              <svg class="w-8 h-8" viewBox="0 0 24 24" fill="currentColor" v-html="icons.pokeball"></svg>
              <h1 class="text-xl font-bold">Pokemon Card Planning</h1>
            </div>

            <!-- User Info & Logout -->
            <div class="flex items-center gap-4">
              <div class="text-sm">
                <span class="text-gray-400">Welcome,</span>
                <span class="ml-1 font-medium">{{ user?.name || user?.email }}</span>
              </div>
              
              <!-- User Roles Badge -->
              <div class="hidden md:flex gap-1">
                <span 
                  v-for="role in (user?.roles || []).slice(0, 2)" 
                  :key="role"
                  class="px-2 py-0.5 bg-blue-600 text-xs rounded"
                >
                  {{ role.replace('ROLE_', '') }}
                </span>
              </div>

              <button
                @click="handleLogout"
                class="flex items-center gap-2 px-3 py-2 text-gray-300 hover:text-white hover:bg-gray-700 rounded transition-colors"
                title="Logout"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor" v-html="icons.logout"></svg>
                <span class="hidden sm:inline">Logout</span>
              </button>
            </div>
          </div>

          <!-- Tab Navigation -->
          <div class="flex space-x-1 pb-2 overflow-x-auto">
            <button
              v-for="tab in visibleTabs"
              :key="tab.id"
              @click="navigateTo(tab.path)"
              :class="[
                'flex items-center gap-2 px-4 py-2 rounded-t-lg transition-colors whitespace-nowrap',
                activeTab === tab.id
                  ? 'bg-gray-50 text-gray-800'
                  : 'text-gray-400 hover:text-white hover:bg-gray-700'
              ]"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor" v-html="icons[tab.icon]"></svg>
              {{ tab.label }}
            </button>
          </div>
        </div>
      </nav>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto px-4 py-6">
        <router-view />
      </main>

      <!-- Footer -->
      <footer class="bg-gray-100 border-t mt-8 py-4">
        <div class="max-w-7xl mx-auto px-4 text-center text-gray-500 text-sm">
          Pokemon Card Planning System &copy; {{ new Date().getFullYear() }} PCA Grade
        </div>
      </footer>
    </template>

    <!-- Router View for non-authenticated routes (callback, etc.) -->
    <router-view v-else-if="!isAuthenticated && !isLoading" />

    <!-- Notification Toast -->
    <Transition name="slide">
      <div 
        v-if="notification.show"
        :class="[
          'fixed bottom-4 right-4 px-6 py-3 rounded-lg shadow-lg text-white z-50',
          {
            'bg-green-600': notification.type === 'success',
            'bg-red-600': notification.type === 'error',
            'bg-yellow-600': notification.type === 'warning',
          }
        ]"
      >
        {{ notification.message }}
      </div>
    </Transition>

  </div>
</template>

<style scoped>
/* Notification animation */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
