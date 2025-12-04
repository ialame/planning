<script setup lang="ts">
// views/auth/OAuthCallback.vue
// Handles the OAuth2 redirect callback from Authentik

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { handleCallback, getReturnUrl, error } = useAuth()

const status = ref<'processing' | 'success' | 'error'>('processing')
const errorMessage = ref<string>('')

onMounted(async () => {
  try {
    console.log(' Processing OAuth callback...')

    // Process the callback
    const user = await handleCallback()

    status.value = 'success'
    console.log(' Login successful:', user.email)

    // Get return URL or default to dashboard
    let returnUrl = getReturnUrl() || '/'
// Validate it's a proper path
    if (!returnUrl.startsWith('/')) {
      returnUrl = '/'
    }

    // Small delay to show success message
    setTimeout(() => {
      router.push(returnUrl)
    }, 500)

  } catch (err: any) {
    console.error(' OAuth callback error:', err)
    status.value = 'error'
    errorMessage.value = err.message || 'Authentication failed'
  }
})

function retryLogin() {
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <div class="bg-white p-8 rounded-lg shadow-lg max-w-md w-full text-center">

      <!-- Processing -->
      <template v-if="status === 'processing'">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <h2 class="text-xl font-semibold text-gray-800 mb-2">Signing you in...</h2>
        <p class="text-gray-600">Please wait while we complete your authentication.</p>
      </template>

      <!-- Success -->
      <template v-else-if="status === 'success'">
        <div class="text-green-500 mb-4">
          <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-gray-800 mb-2">Welcome!</h2>
        <p class="text-gray-600">Redirecting to your dashboard...</p>
      </template>

      <!-- Error -->
      <template v-else-if="status === 'error'">
        <div class="text-red-500 mb-4">
          <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-gray-800 mb-2">Authentication Failed</h2>
        <p class="text-red-600 mb-4">{{ errorMessage || error }}</p>
        <button
          @click="retryLogin"
          class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
        >
          Try Again
        </button>
      </template>

    </div>
  </div>
</template>
