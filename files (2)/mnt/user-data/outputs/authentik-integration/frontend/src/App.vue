<template>
  <div class="app min-h-screen bg-gray-100">
    <!-- Loading Overlay -->
    <div 
      v-if="isLoading" 
      class="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center z-50"
    >
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent mx-auto mb-4"></div>
        <p class="text-gray-600">Loading...</p>
      </div>
    </div>

    <!-- Navigation Bar -->
    <nav v-if="isAuthenticated && !isCallbackRoute" class="bg-white shadow-sm border-b">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <!-- Logo & Main Navigation -->
          <div class="flex items-center space-x-8">
            <!-- Logo -->
            <router-link to="/" class="flex items-center space-x-2">
              <span class="text-2xl">🎴</span>
              <span class="font-bold text-gray-800">PCA Planning</span>
            </router-link>

            <!-- Main Navigation -->
            <div class="hidden md:flex space-x-1">
              <router-link 
                v-for="item in navigationItems" 
                :key="item.path"
                :to="item.path"
                class="nav-link"
                :class="{ 'nav-link-active': isActiveRoute(item.path) }"
              >
                <span class="mr-1">{{ item.icon }}</span>
                {{ item.label }}
              </router-link>
            </div>
          </div>

          <!-- User Menu -->
          <div class="flex items-center space-x-4">
            <!-- User Info -->
            <div class="hidden sm:flex items-center space-x-3">
              <!-- Avatar -->
              <div class="relative">
                <img 
                  v-if="user?.avatarUrl" 
                  :src="user.avatarUrl" 
                  :alt="user.fullName"
                  class="h-8 w-8 rounded-full object-cover"
                />
                <div 
                  v-else 
                  class="h-8 w-8 rounded-full bg-blue-500 flex items-center justify-center text-white font-medium"
                >
                  {{ userInitials }}
                </div>
              </div>
              
              <!-- Name & Role -->
              <div class="text-sm">
                <div class="font-medium text-gray-800">{{ user?.fullName }}</div>
                <div class="text-gray-500 text-xs">{{ primaryRole }}</div>
              </div>
            </div>

            <!-- Logout Button -->
            <button 
              @click="handleLogout"
              class="flex items-center px-3 py-2 text-sm text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
              title="Logout"
            >
              <span class="mr-1">🚪</span>
              <span class="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile Navigation -->
      <div class="md:hidden border-t px-4 py-2 space-x-2 overflow-x-auto flex">
        <router-link 
          v-for="item in navigationItems" 
          :key="item.path"
          :to="item.path"
          class="nav-link-mobile"
          :class="{ 'nav-link-mobile-active': isActiveRoute(item.path) }"
        >
          {{ item.icon }}
        </router-link>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Footer -->
    <footer v-if="isAuthenticated && !isCallbackRoute" class="bg-white border-t py-4 mt-8">
      <div class="max-w-7xl mx-auto px-4 text-center text-sm text-gray-500">
        <p>Pokemon Card Planning System &copy; {{ currentYear }} PCA Grade</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useOidcAuth } from '@/composables/useOidcAuth'

const route = useRoute()
const { user, isLoading, isAuthenticated, logout, hasRole } = useOidcAuth()

// Navigation items with role-based visibility
const allNavigationItems = [
  { path: '/', label: 'Dashboard', icon: '📊', roles: [] },
  { path: '/orders', label: 'Orders', icon: '📦', roles: [] },
  { path: '/planning', label: 'Planning', icon: '📅', roles: [] },
  { path: '/employees', label: 'Employees', icon: '👥', roles: ['ADMIN', 'MANAGER'] },
  { path: '/groups', label: 'Teams', icon: '🏢', roles: ['ADMIN', 'MANAGER'] },
  { path: '/sync', label: 'Sync', icon: '🔄', roles: ['ADMIN'] },
]

// Filter navigation items based on user roles
const navigationItems = computed(() => {
  return allNavigationItems.filter(item => {
    if (item.roles.length === 0) return true
    return item.roles.some(role => hasRole(role))
  })
})

// Check if current route is active
const isActiveRoute = (path: string): boolean => {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}

// Check if on callback route (hide nav)
const isCallbackRoute = computed(() => route.name === 'callback')

// User initials for avatar fallback
const userInitials = computed(() => {
  if (!user.value) return '?'
  const first = user.value.firstName?.[0] || ''
  const last = user.value.lastName?.[0] || ''
  return (first + last).toUpperCase() || user.value.email[0].toUpperCase()
})

// Primary role display
const primaryRole = computed(() => {
  if (!user.value?.roles.length) return 'User'
  // Get first role without ROLE_ prefix
  const role = user.value.roles[0]
  return role.replace('ROLE_', '').charAt(0) + role.replace('ROLE_', '').slice(1).toLowerCase()
})

// Current year for footer
const currentYear = new Date().getFullYear()

// Logout handler
async function handleLogout() {
  if (confirm('Are you sure you want to logout?')) {
    await logout()
  }
}
</script>

<style scoped>
.nav-link {
  @apply px-3 py-2 text-sm font-medium text-gray-600 rounded-lg transition;
}

.nav-link:hover {
  @apply bg-gray-100 text-gray-900;
}

.nav-link-active {
  @apply bg-blue-50 text-blue-700;
}

.nav-link-mobile {
  @apply flex-shrink-0 p-2 rounded-lg text-lg;
}

.nav-link-mobile:hover {
  @apply bg-gray-100;
}

.nav-link-mobile-active {
  @apply bg-blue-50;
}
</style>
