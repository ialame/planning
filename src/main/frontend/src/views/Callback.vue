<template>
  <div class="callback-page min-h-screen flex items-center justify-center bg-gray-100">
    <div class="bg-white p-8 rounded-lg shadow-lg text-center max-w-md w-full">
      <!-- Loading State -->
      <div v-if="isProcessing" class="space-y-4">
        <div class="animate-spin rounded-full h-16 w-16 border-4 border-blue-500 border-t-transparent mx-auto"></div>
        <h2 class="text-xl font-semibold text-gray-700">Authenticating...</h2>
        <p class="text-gray-500">Please wait while we complete your login.</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="space-y-4">
        <div class="text-red-500 text-6xl">Alert</div>
        <h2 class="text-xl font-semibold text-red-600">Authentication Failed</h2>
        <p class="text-gray-600">{{ error }}</p>
        <div class="space-y-2 pt-4">
          <button
            @click="retryLogin"
            class="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Try Again
          </button>
          <button
            @click="goHome"
            class="w-full px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
          >
            Go to Home
          </button>
        </div>
      </div>

      <!-- Success State (brief flash before redirect) -->
      <div v-else-if="success" class="space-y-4">
        <div class="text-green-500 text-6xl">✓</div>
        <h2 class="text-xl font-semibold text-green-600">Login Successful!</h2>
        <p class="text-gray-600">Welcome, {{ userName }}!</p>
        <p class="text-gray-500 text-sm">Redirecting to dashboard...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOidcAuth } from '@/composables/useOidcAuth'

const router = useRouter()
const { handleCallback, login } = useOidcAuth()

// State
const isProcessing = ref(true)
const error = ref<string | null>(null)
const success = ref(false)
const userName = ref('')

// Process OAuth callback on mount
onMounted(async () => {
  console.log(' Processing OAuth2 callback...')
  
  try {
    const user = await handleCallback()
    
    // Success!
    success.value = true
    userName.value = user.fullName || user.email
    
    console.log(' Login successful:', user.email)
    
    // Redirect to dashboard after brief delay
    setTimeout(() => {
      // Check for saved redirect URL
      const redirectUrl = sessionStorage.getItem('auth_redirect_url') || '/'
      sessionStorage.removeItem('auth_redirect_url')
      
      router.push(redirectUrl)
    }, 1000)
    
  } catch (err) {
    console.error(' Callback error:', err)
    error.value = err instanceof Error ? err.message : 'Authentication failed. Please try again.'
    isProcessing.value = false
  }
})

// Retry login
async function retryLogin() {
  error.value = null
  isProcessing.value = true
  
  try {
    await login()
  } catch (err) {
    error.value = 'Failed to initiate login'
    isProcessing.value = false
  }
}

// Go to home page
function goHome() {
  router.push('/')
}
</script>

<style scoped>
.callback-page {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>
