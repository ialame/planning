// services/oidcAuthService.ts
// OIDC Authentication Service for Authentik

import { UserManager, User, UserManagerEvents } from 'oidc-client-ts'
import { oidcConfig } from '@/config/oidc.config'
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'

/**
 * Application User (mapped from OIDC user)
 */
export interface AppUser {
  id: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  roles: string[]
  groups: string[]
  avatarUrl?: string
}

/**
 * OIDC Authentication Service
 * Handles OAuth2/OIDC authentication flow with Authentik
 */
class OidcAuthService {
  private userManager: UserManager
  private axiosInstance: AxiosInstance
  private currentUser: AppUser | null = null

  constructor() {
    // Initialize OIDC UserManager
    this.userManager = new UserManager(oidcConfig)
    
    // Initialize Axios with base URL
    this.axiosInstance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Add request interceptor to attach access token
    this.axiosInstance.interceptors.request.use(
      async (config) => {
        const user = await this.userManager.getUser()
        if (user?.access_token) {
          config.headers.Authorization = `Bearer ${user.access_token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Add response interceptor for 401 handling
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          console.log('🔐 401 Unauthorized - attempting token refresh...')
          try {
            await this.userManager.signinSilent()
            // Retry original request
            const user = await this.userManager.getUser()
            if (user?.access_token) {
              error.config.headers.Authorization = `Bearer ${user.access_token}`
              return this.axiosInstance.request(error.config)
            }
          } catch (refreshError) {
            console.error('❌ Token refresh failed:', refreshError)
            await this.logout()
          }
        }
        return Promise.reject(error)
      }
    )

    // Setup event listeners
    this.setupEventListeners()
    
    // Restore user from storage
    this.restoreUser()
  }

  /**
   * Setup OIDC event listeners
   */
  private setupEventListeners(): void {
    const events: UserManagerEvents = this.userManager.events

    events.addUserLoaded((user) => {
      console.log('✅ User loaded:', user.profile.email)
      this.currentUser = this.mapOidcUserToAppUser(user)
    })

    events.addUserUnloaded(() => {
      console.log('👋 User unloaded')
      this.currentUser = null
    })

    events.addAccessTokenExpiring(() => {
      console.log('⏰ Access token expiring...')
    })

    events.addAccessTokenExpired(() => {
      console.log('⏰ Access token expired')
    })

    events.addSilentRenewError((error) => {
      console.error('❌ Silent renew error:', error)
    })

    events.addUserSignedOut(() => {
      console.log('👋 User signed out from Authentik')
      this.currentUser = null
      window.location.href = '/'
    })
  }

  /**
   * Restore user from storage on init
   */
  private async restoreUser(): Promise<void> {
    try {
      const user = await this.userManager.getUser()
      if (user && !user.expired) {
        console.log('✅ User restored from storage:', user.profile.email)
        this.currentUser = this.mapOidcUserToAppUser(user)
      }
    } catch (error) {
      console.error('❌ Error restoring user:', error)
    }
  }

  /**
   * Map OIDC User to Application User
   */
  private mapOidcUserToAppUser(oidcUser: User): AppUser {
    const profile = oidcUser.profile
    
    // Extract groups/roles from Authentik claims
    // Authentik puts groups in the 'groups' claim
    const groups = (profile.groups as string[]) || []
    
    // Map Authentik groups to application roles
    const roles = groups.map(group => {
      // Remove 'authentik ' prefix if present
      const cleanGroup = group.replace(/^authentik\s+/i, '')
      return `ROLE_${cleanGroup.toUpperCase()}`
    })

    return {
      id: profile.sub,
      email: profile.email || '',
      firstName: profile.given_name || profile.name?.split(' ')[0] || '',
      lastName: profile.family_name || profile.name?.split(' ').slice(1).join(' ') || '',
      fullName: profile.name || `${profile.given_name || ''} ${profile.family_name || ''}`.trim(),
      roles: roles,
      groups: groups,
      avatarUrl: profile.picture,
    }
  }

  // ==================== PUBLIC METHODS ====================

  /**
   * Initiate login flow - redirects to Authentik
   */
  async login(): Promise<void> {
    console.log('🔐 Initiating OIDC login...')
    await this.userManager.signinRedirect()
  }

  /**
   * Handle callback after Authentik redirect
   */
  async handleCallback(): Promise<AppUser> {
    console.log('🔄 Processing OIDC callback...')
    const user = await this.userManager.signinRedirectCallback()
    this.currentUser = this.mapOidcUserToAppUser(user)
    console.log('✅ Login successful:', this.currentUser.email)
    return this.currentUser
  }

  /**
   * Logout - redirects to Authentik logout
   */
  async logout(): Promise<void> {
    console.log('👋 Initiating logout...')
    this.currentUser = null
    await this.userManager.signoutRedirect()
  }

  /**
   * Silent token renewal
   */
  async renewToken(): Promise<User | null> {
    try {
      const user = await this.userManager.signinSilent()
      if (user) {
        this.currentUser = this.mapOidcUserToAppUser(user)
      }
      return user
    } catch (error) {
      console.error('❌ Silent renew failed:', error)
      return null
    }
  }

  /**
   * Check if user is authenticated
   */
  async isAuthenticated(): Promise<boolean> {
    const user = await this.userManager.getUser()
    return !!user && !user.expired
  }

  /**
   * Check authentication synchronously (from cache)
   */
  isAuthenticatedSync(): boolean {
    return !!this.currentUser
  }

  /**
   * Get current user
   */
  getUser(): AppUser | null {
    return this.currentUser
  }

  /**
   * Get OIDC user (with tokens)
   */
  async getOidcUser(): Promise<User | null> {
    return await this.userManager.getUser()
  }

  /**
   * Get access token
   */
  async getAccessToken(): Promise<string | null> {
    const user = await this.userManager.getUser()
    return user?.access_token || null
  }

  /**
   * Check if user has a specific role
   */
  hasRole(role: string): boolean {
    if (!this.currentUser) return false
    const normalizedRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`
    return this.currentUser.roles.includes(normalizedRole.toUpperCase())
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role))
  }

  // ==================== HTTP METHODS ====================

  /**
   * GET request with authentication
   */
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.get<T>(url, config)
    return response.data
  }

  /**
   * POST request with authentication
   */
  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.post<T>(url, data, config)
    return response.data
  }

  /**
   * PUT request with authentication
   */
  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.put<T>(url, data, config)
    return response.data
  }

  /**
   * DELETE request with authentication
   */
  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.delete<T>(url, config)
    return response.data
  }
}

// Export singleton instance
const oidcAuthService = new OidcAuthService()
export default oidcAuthService

// Also export class for testing
export { OidcAuthService }
