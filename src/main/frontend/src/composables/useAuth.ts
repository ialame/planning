// src/composables/useAuth.ts
// Reactive Vue composable for OAuth2/OIDC authentication

import { ref, computed, onMounted, onUnmounted } from 'vue'
import authService, { type AppUser } from '@/services/authService'

// Reactive state (shared across all components)
const user = ref<AppUser | null>(null)
const isLoading = ref(true)
const error = ref<string | null>(null)

// Initialize on first import
let initialized = false

/**
 * Vue composable for authentication
 * Provides reactive state and methods for OAuth2/OIDC auth
 */
export function useAuth() {
  // Computed properties
  const isAuthenticated = computed(() => user.value !== null)
  const accessToken = computed(() => user.value?.accessToken || null)
  const userRoles = computed(() => user.value?.roles || [])
  const userName = computed(() => user.value?.name || user.value?.email || '')
  const userEmail = computed(() => user.value?.email || '')

  /**
   * Initialize authentication state
   */
  async function initialize(): Promise<void> {
    if (initialized) return

    isLoading.value = true
    error.value = null

    try {
      // Check if we have a stored user
      const currentUser = authService.getUser()
      if (currentUser && !authService.isTokenExpired()) {
        user.value = currentUser
        console.log(' Auth initialized with user:', currentUser.email)
      }
    } catch (err) {
      console.error('Auth initialization error:', err)
      error.value = 'Failed to initialize authentication'
    } finally {
      isLoading.value = false
      initialized = true
    }
  }

  /**
   * Start login flow
   */
  async function login(returnUrl?: string): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      await authService.login(returnUrl || window.location.pathname)
    } catch (err) {
      console.error('Login error:', err)
      error.value = 'Failed to start login'
      isLoading.value = false
      throw err
    }
  }

  /**
   * Handle OAuth callback
   */
  async function handleCallback(): Promise<AppUser> {
    isLoading.value = true
    error.value = null

    try {
      const appUser = await authService.handleCallback()
      user.value = appUser
      return appUser
    } catch (err) {
      console.error('Callback error:', err)
      error.value = 'Login failed'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Logout
   */
  async function logout(): Promise<void> {
    isLoading.value = true
    error.value = null

    try {
      await authService.logout()
      user.value = null
    } catch (err) {
      console.error('Logout error:', err)
      // Still clear local state
      user.value = null
      error.value = 'Logout may not have completed fully'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Silent logout (no redirect)
   */
  async function silentLogout(): Promise<void> {
    await authService.silentLogout()
    user.value = null
  }

  /**
   * Refresh access token
   */
  async function refreshToken(): Promise<void> {
    try {
      const refreshedUser = await authService.refreshToken()
      if (refreshedUser) {
        user.value = refreshedUser
      }
    } catch (err) {
      console.error('Token refresh error:', err)
      error.value = 'Session expired'
      user.value = null
    }
  }

  /**
   * Check if user has role
   */
  function hasRole(role: string): boolean {
    return authService.hasRole(role)
  }

  /**
   * Check if user has any of the roles
   */
  function hasAnyRole(roles: string[]): boolean {
    return authService.hasAnyRole(roles)
  }

  /**
   * Check if user has all roles
   */
  function hasAllRoles(roles: string[]): boolean {
    return authService.hasAllRoles(roles)
  }

  /**
   * Get return URL after login
   */
  function getReturnUrl(): string | null {
    return authService.getReturnUrl()
  }

  // Event handlers for auth service events
  function onUserLoaded(event: CustomEvent<AppUser>) {
    user.value = event.detail
    isLoading.value = false
  }

  function onUserUnloaded() {
    user.value = null
  }

  function onTokenExpired() {
    error.value = 'Session expired'
  }

  function onSilentRenewError(event: CustomEvent<Error>) {
    console.error('Silent renew failed:', event.detail)
    error.value = 'Session renewal failed'
  }

  function onUserSignedOut() {
    user.value = null
    error.value = null
  }

  // Setup event listeners on mount
  onMounted(() => {
    window.addEventListener('auth:userLoaded', onUserLoaded as EventListener)
    window.addEventListener('auth:userUnloaded', onUserUnloaded)
    window.addEventListener('auth:tokenExpired', onTokenExpired)
    window.addEventListener('auth:silentRenewError', onSilentRenewError as EventListener)
    window.addEventListener('auth:userSignedOut', onUserSignedOut)

    // Initialize if not already done
    initialize()
  })

  // Cleanup event listeners on unmount
  onUnmounted(() => {
    window.removeEventListener('auth:userLoaded', onUserLoaded as EventListener)
    window.removeEventListener('auth:userUnloaded', onUserUnloaded)
    window.removeEventListener('auth:tokenExpired', onTokenExpired)
    window.removeEventListener('auth:silentRenewError', onSilentRenewError as EventListener)
    window.removeEventListener('auth:userSignedOut', onUserSignedOut)
  })

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    error,
    accessToken,
    userRoles,
    userName,
    userEmail,

    // Methods
    initialize,
    login,
    handleCallback,
    logout,
    silentLogout,
    refreshToken,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    getReturnUrl,

    // Direct access to service
    authService,
  }
}

export default useAuth
