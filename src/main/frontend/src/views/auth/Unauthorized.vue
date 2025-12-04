<script setup lang="ts">
// views/auth/Unauthorized.vue
// Displayed when user lacks required permissions

import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { user, userRoles, logout } = useAuth()

function goToDashboard() {
  router.push('/')
}

async function handleLogout() {
  await logout()
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <div class="bg-white p-8 rounded-lg shadow-lg max-w-md w-full text-center">
      
      <!-- Icon -->
      <div class="text-yellow-500 mb-4">
        <svg class="w-20 h-20 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z">
          </path>
        </svg>
      </div>

      <!-- Title -->
      <h2 class="text-2xl font-bold text-gray-800 mb-2">Access Denied</h2>
      
      <!-- Message -->
      <p class="text-gray-600 mb-6">
        You don't have permission to access this page.
      </p>

      <!-- User info -->
      <div v-if="user" class="bg-gray-50 rounded-lg p-4 mb-6 text-left">
        <p class="text-sm text-gray-500 mb-1">Logged in as:</p>
        <p class="font-medium text-gray-800">{{ user.email }}</p>
        <p class="text-sm text-gray-500 mt-2">Your roles:</p>
        <div class="flex flex-wrap gap-1 mt-1">
          <span 
            v-for="role in userRoles" 
            :key="role"
            class="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded"
          >
            {{ role.replace('ROLE_', '') }}
          </span>
          <span v-if="userRoles.length === 0" class="text-gray-400 text-sm italic">
            No roles assigned
          </span>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-3 justify-center">
        <button 
          @click="goToDashboard"
          class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
        >
          Go to Dashboard
        </button>
        <button 
          @click="handleLogout"
          class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors"
        >
          Logout
        </button>
      </div>

      <!-- Help text -->
      <p class="text-sm text-gray-500 mt-6">
        If you believe this is an error, please contact your administrator.
      </p>
    </div>
  </div>
</template>
