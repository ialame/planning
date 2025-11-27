// composables/useOidcAuth.ts
// Reactive Vue composable for OIDC Authentication

import { ref, computed, readonly } from 'vue'
import oidcAuthService, { type AppUser } from '@/services/oidcAuthService'

// Reactive state (shared across all components)
const user = ref<AppUser | null>(null)
const isLoading = ref<boolean>(true)
const error = ref<string | null>(null)

// Initialize on module load
async function initialize() {
  try {
    const isAuth = await oidcAuthService.isAuthenticated()
    if (isAuth) {
      user.value = oidcAuthService.getUser()
    }
  } catch (err) {
    console.error('❌ Auth initialization error:', err)
  } finally {
    isLoading.value = false
  }
}

// Call initialize
initialize()

/**
 * OIDC Authentication Composable
 * Provides reactive authentication state and methods
 */
export function useOidcAuth() {
  // Computed properties
  const isAuthenticated = computed(() => !!user.value)
  const userRoles = computed(() => user.value?.roles || [])
  const userGroups = computed(() => user.value?.groups || [])
  const userFullName = computed(() => user.value?.fullName || '')
  const userEmail = computed(() => user.value?.email || '')

  /**
   * Login - redirects to Authentik
   */
  async function login(): Promise<void> {
    error.value = null
    try {
      await oidcAuthService.login()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Login failed'
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
      const appUser = await oidcAuthService.handleCallback()
      user.value = appUser
      return appUser
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Callback processing failed'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Logout - redirects to Authentik logout
   */
  async function logout(): Promise<void> {
    try {
      await oidcAuthService.logout()
      user.value = null
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Logout failed'
      // Clear local state even if remote logout fails
      user.value = null
    }
  }

  /**
   * Refresh authentication state
   */
  async function refreshAuth(): Promise<void> {
    isLoading.value = true
    try {
      const isAuth = await oidcAuthService.isAuthenticated()
      if (isAuth) {
        user.value = oidcAuthService.getUser()
      } else {
        user.value = null
      }
    } catch (err) {
      console.error('❌ Auth refresh error:', err)
      user.value = null
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Check if user has a specific role
   */
  function hasRole(role: string): boolean {
    return oidcAuthService.hasRole(role)
  }

  /**
   * Check if user has any of the specified roles
   */
  function hasAnyRole(roles: string[]): boolean {
    return oidcAuthService.hasAnyRole(roles)
  }

  /**
   * Check if user is admin
   */
  const isAdmin = computed(() => hasRole('ADMIN'))

  /**
   * Check if user is manager
   */
  const isManager = computed(() => hasAnyRole(['ADMIN', 'MANAGER']))

  return {
    // Reactive state (readonly to prevent external mutation)
    user: readonly(user),
    isLoading: readonly(isLoading),
    error: readonly(error),
    
    // Computed
    isAuthenticated,
    userRoles,
    userGroups,
    userFullName,
    userEmail,
    isAdmin,
    isManager,
    
    // Methods
    login,
    logout,
    handleCallback,
    refreshAuth,
    hasRole,
    hasAnyRole,
    
    // Direct access to service for API calls
    authService: oidcAuthService,
  }
}

export default useOidcAuth
