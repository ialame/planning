<template>
  <div
    v-if="!isAuthenticated"
    class="fixed inset-0 bg-gray-900 bg-opacity-50 flex items-center justify-center"
    style="z-index: 9999;"
  >
    <div class="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full mx-4">
      <div class="text-center mb-6">
        <h2 class="text-3xl font-bold text-gray-800 flex items-center justify-center gap-2">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
          </svg>
          Pokemon Card Planning
        </h2>
        <p class="text-gray-600 mt-2">Please login to continue</p>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            Email
          </label>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="email"
            class="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-blue-500 text-gray-900 bg-white placeholder-gray-400"
            placeholder="john.grader@pcagrade.com"
          />
        </div>

        <div class="mb-6">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            Password
          </label>
          <input
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            class="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-blue-500 text-gray-900 bg-white placeholder-gray-400"
            placeholder="Enter your password"
          />
        </div>

        <div
          v-if="error"
          class="mb-4 p-4 bg-red-50 border-2 border-red-400 text-red-700 rounded-lg flex items-start"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" class="mr-2 flex-shrink-0">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          <span>{{ error }}</span>
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded-lg focus:outline-none focus:ring-4 focus:ring-blue-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95 flex items-center justify-center gap-2"
        >
          <svg v-if="loading" class="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
            <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
          {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </form>

      <div class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p class="font-semibold text-blue-900 mb-3 flex items-center gap-2">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          Test Accounts
        </p>
        <div class="space-y-3 text-sm">
          <div class="bg-white p-3 rounded border border-blue-100">
            <div class="flex items-center mb-1 gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-blue-600">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
              </svg>
              <span class="text-gray-600 w-16">Grader:</span>
              <code class="text-blue-600 font-mono text-xs">john.grader@pcagrade.com</code>
            </div>
            <div class="flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-gray-600">
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
              </svg>
              <span class="text-gray-600 w-16">Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>

          <div class="bg-white p-3 rounded border border-blue-100">
            <div class="flex items-center mb-1 gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-blue-600">
                <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
              </svg>
              <span class="text-gray-600 w-16">Manager:</span>
              <code class="text-blue-600 font-mono text-xs">manager@pcagrade.com</code>
            </div>
            <div class="flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-gray-600">
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
              </svg>
              <span class="text-gray-600 w-16">Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>

          <div class="bg-white p-3 rounded border border-blue-100">
            <div class="flex items-center mb-1 gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-blue-600">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
              </svg>
              <span class="text-gray-600 w-16">Admin:</span>
              <code class="text-blue-600 font-mono text-xs">admin@pcagrade.com</code>
            </div>
            <div class="flex items-center gap-2">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="text-gray-600">
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
              </svg>
              <span class="text-gray-600 w-16">Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, login } = useAuth()

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
}

// Emits
const emit = defineEmits<{
  (e: 'login-success', user: User): void
}>()

// Reactive state
const email = ref<string>('john.grader@pcagrade.com')
const password = ref<string>('password123')
const loading = ref<boolean>(false)
const error = ref<string | null>(null)

// Methods
async function handleLogin() {
  loading.value = true
  error.value = null

  try {
    const result = await login(email.value, password.value)

    if (result.success) {
      emit('login-success', result.user)
    } else {
      error.value = result.error || 'Login failed'
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'An error occurred'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Force text visibility */
input {
  color: #1a202c !important;
  background-color: white !important;
}

input::placeholder {
  color: #a0aec0 !important;
}

/* Animation for spinner */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>