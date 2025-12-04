<template>
  <div id="app" class="min-h-screen bg-gray-50">

    <!-- Callback/Silent-renew routes - bypass auth check -->
    <template v-if="isCallbackRoute">
      <router-view />
    </template>

    <!-- Loading Overlay -->
    <template v-else-if="isLoading">
      <div class="fixed inset-0 bg-white bg-opacity-80 flex items-center justify-center z-50">
        <div class="text-center">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p class="text-gray-600">Loading...</p>
        </div>
      </div>
    </template>

    <!-- Main App (authenticated) -->
    <template v-else-if="isAuthenticated">
      <nav class="bg-gray-800 text-white shadow-lg">
        <div class="max-w-7xl mx-auto px-4">
          <div class="flex justify-between items-center h-16">
            <!-- Logo -->
            <div class="flex items-center gap-3">
              <span class="text-2xl"></span>
              <h1 class="text-xl font-bold">Pokemon Card Planning</h1>
            </div>

            <!-- User Info & Logout -->
            <div class="flex items-center gap-4">
              <span class="text-sm text-gray-300">{{ user?.email }}</span>
              <button
                @click="logout"
                class="px-3 py-1.5 bg-red-600 hover:bg-red-700 rounded text-sm transition"
              >
                Logout
              </button>
            </div>
          </div>

          <!-- Tab Navigation -->
          <div class="flex space-x-1 pb-2 overflow-x-auto">
            <router-link
              to="/"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Dashboard
            </router-link>
            <router-link
              to="/orders"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/orders' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Orders
            </router-link>
            <router-link
              to="/employees"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/employees' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Employees
            </router-link>
            <router-link
              to="/planning"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/planning' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Planning
            </router-link>
            <router-link
              to="/groups"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/groups' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Teams
            </router-link>
            <router-link
              to="/sync"
              class="px-4 py-2 rounded-t-lg transition-colors"
              :class="route.path === '/sync' ? 'bg-gray-50 text-gray-800' : 'text-gray-400 hover:text-white hover:bg-gray-700'"
            >
               Sync
            </router-link>
          </div>
        </div>
      </nav>

      <main class="max-w-7xl mx-auto px-4 py-6">
        <router-view />
      </main>

      <footer class="bg-gray-100 border-t mt-8 py-4">
        <div class="max-w-7xl mx-auto px-4 text-center text-gray-500 text-sm">
          Pokemon Card Planning System © 2025 PCA Grade
        </div>
      </footer>
    </template>

    <!-- Not authenticated - show login -->
    <template v-else>
      <LoginModal @login-success="onLoginSuccess" />
    </template>

    <!-- Notification Toast -->
    <Transition name="fade">
      <div
        v-if="notification.show"
        :class="[
          'fixed bottom-4 right-4 px-4 py-2 rounded-lg shadow-lg text-white',
          notification.type === 'success' ? 'bg-green-600' : 'bg-red-600'
        ]"
      >
        {{ notification.message }}
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, provide, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// Computed for callback detection
const isCallbackRoute = computed(() => route.path === '/callback' || route.path === '/silent-renew')
import DashboardView from './views/Dashboard.vue'
import OrdersView from './views/Orders.vue'
import EmployeesView from './views/Employees.vue'
import PlanningView from './views/Planning.vue'
import TeamsView from "./views/Teams.vue";
import SyncView from "./views/DataSync.vue";
import authService, { type User } from '@/services/authService'
import { useAuth } from '@/composables/useAuth'
import LoginModal from '@/components/LoginModal.vue'

//const user = ref<User | null>(authService.getUser())
//const isAuthenticated = computed(() => authService.isAuthenticated())


const { isAuthenticated, user, isLoading } = useAuth()

function onLoginSuccess(userData: any) {
  console.log(' Login successful:', userData)
  // Le computed isAuthenticated se met à jour automatiquement !
}

function logout(): void {
  authService.logout()
  user.value = null
  location.reload()
}

// État global
const activeTab = ref('dashboard')
const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const tabs = [
  { id: 'dashboard', label: ' Dashboard' },
  { id: 'orders', label: ' Orders' },
  { id: 'employees', label: ' Employees' },
  { id: 'planning', label: ' Planning' },
  { id: 'teams', label: ' Teams' },
  { id: 'sync', label: 'ApiSync' },
]
// Fonction pour changer d'onglet
const changeTab = (tabId: string) => {
  console.log('Changing to tab:', tabId)
  activeTab.value = tabId
}

// Fonction pour afficher les notifications
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

// Provide pour les composants enfants
provide('showNotification', showNotification)
provide('changeTab', changeTab)
</script>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
