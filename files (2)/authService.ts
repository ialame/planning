// src/services/authService.ts
// OAuth2/OIDC Authentication Service for Authentik
// Replaces the previous JWT-based authentication

import { UserManager, User, Log } from 'oidc-client-ts'
import { oidcConfig, claimMappings } from '@/config/oidc.config'

// Enable logging in development
if (import.meta.env.DEV) {
  Log.setLogger(console)
  Log.setLevel(Log.DEBUG)
}

/**
 * Application User interface
 * Extends OIDC User with mapped roles
 */
export interface AppUser {
  id: string
  email: string
  name: string
  firstName?: string
  lastName?: string
  roles: string[]
  groups: string[]
  accessToken: string
  idToken: string
  expiresAt?: number
  raw: User
}

/**
 * OAuth2/OIDC Authentication Service
 * Handles authentication flow with Authentik
 */
class AuthService {
  private userManager: UserManager
  private currentUser: AppUser | null = null

  constructor() {
    this.userManager = new UserManager(oidcConfig)
    this.setupEventListeners()
    this.initializeUser()
  }

  /**
   * Setup OIDC event listeners
   */
  private setupEventListeners(): void {
    // User loaded (login or token refresh)
    this.userManager.events.addUserLoaded((user) => {
      console.log('🔐 User loaded:', user.profile.email)
      this.currentUser = this.mapUser(user)
      window.dispatchEvent(new CustomEvent('auth:userLoaded', { detail: this.currentUser }))
    })

    // User unloaded (logout)
    this.userManager.events.addUserUnloaded(() => {
      console.log('🔓 User unloaded')
      this.currentUser = null
      window.dispatchEvent(new CustomEvent('auth:userUnloaded'))
    })

    // Access token expired
    this.userManager.events.addAccessTokenExpired(() => {
      console.warn('⚠️ Access token expired')
      window.dispatchEvent(new CustomEvent('auth:tokenExpired'))
    })

    // Access token expiring (before actual expiration)
    this.userManager.events.addAccessTokenExpiring(() => {
      console.log('🔄 Access token expiring, will renew automatically')
    })

    // Silent renew error
    this.userManager.events.addSilentRenewError((error) => {
      console.error('❌ Silent renew error:', error)
      window.dispatchEvent(new CustomEvent('auth:silentRenewError', { detail: error }))
    })

    // User signed out from another tab/window
    this.userManager.events.addUserSignedOut(() => {
      console.log('🔓 User signed out (from another session)')
      this.currentUser = null
      window.dispatchEvent(new CustomEvent('auth:userSignedOut'))
    })
  }

  /**
   * Initialize user from storage on startup
   */
  private async initializeUser(): Promise<void> {
    try {
      const user = await this.userManager.getUser()
      if (user && !user.expired) {
        this.currentUser = this.mapUser(user)
        console.log('✅ User restored from storage:', this.currentUser.email)
      }
    } catch (error) {
      console.error('Failed to initialize user:', error)
    }
  }

  /**
   * Map OIDC User to AppUser with roles
   */
  private mapUser(user: User): AppUser {
    const profile = user.profile
    
    // Extract groups from Authentik claims
    const groups: string[] = (profile[claimMappings.groupsClaim] as string[]) || []
    
    // Map groups to roles
    const roles = groups.map(group => {
      const mappedRole = claimMappings.roleMapping[group.toLowerCase()]
      return mappedRole || `${claimMappings.rolePrefix}${group.toUpperCase()}`
    })

    return {
      id: profile.sub,
      email: profile.email || '',
      name: profile.name || profile.preferred_username || '',
      firstName: profile.given_name,
      lastName: profile.family_name,
      roles,
      groups,
      accessToken: user.access_token,
      idToken: user.id_token || '',
      expiresAt: user.expires_at,
      raw: user,
    }
  }

  // ==================== PUBLIC API ====================

  /**
   * Start login flow - redirects to Authentik
   */
  async login(returnUrl?: string): Promise<void> {
    try {
      // Store return URL for after login
      if (returnUrl) {
        sessionStorage.setItem('auth_return_url', returnUrl)
      }
      
      console.log('🔐 Starting login flow...')
      await this.userManager.signinRedirect()
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  /**
   * Handle OAuth2 callback after login
   */
  async handleCallback(): Promise<AppUser> {
    try {
      console.log('🔐 Processing login callback...')
      const user = await this.userManager.signinRedirectCallback()
      this.currentUser = this.mapUser(user)
      console.log('✅ Login successful:', this.currentUser.email)
      return this.currentUser
    } catch (error) {
      console.error('Callback error:', error)
      throw error
    }
  }

  /**
   * Logout - redirects to Authentik logout
   */
  async logout(): Promise<void> {
    try {
      console.log('🔓 Starting logout...')
      await this.userManager.signoutRedirect()
    } catch (error) {
      console.error('Logout error:', error)
      // Force local logout even if redirect fails
      await this.userManager.removeUser()
      this.currentUser = null
      throw error
    }
  }

  /**
   * Silent logout (no redirect)
   */
  async silentLogout(): Promise<void> {
    await this.userManager.removeUser()
    this.currentUser = null
    console.log('🔓 Silent logout completed')
  }

  /**
   * Get current user
   */
  getUser(): AppUser | null {
    return this.currentUser
  }

  /**
   * Get access token for API calls
   */
  getAccessToken(): string | null {
    return this.currentUser?.accessToken || null
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.currentUser !== null && !this.isTokenExpired()
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(): boolean {
    if (!this.currentUser?.expiresAt) return true
    return Date.now() / 1000 > this.currentUser.expiresAt
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    if (!this.currentUser) return false
    const normalizedRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`
    return this.currentUser.roles.includes(normalizedRole)
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role))
  }

  /**
   * Check if user has all specified roles
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role))
  }

  /**
   * Manually refresh the access token
   */
  async refreshToken(): Promise<AppUser | null> {
    try {
      const user = await this.userManager.signinSilent()
      if (user) {
        this.currentUser = this.mapUser(user)
        console.log('🔄 Token refreshed successfully')
        return this.currentUser
      }
      return null
    } catch (error) {
      console.error('Token refresh error:', error)
      throw error
    }
  }

  /**
   * Get stored return URL after login
   */
  getReturnUrl(): string | null {
    const url = sessionStorage.getItem('auth_return_url')
    sessionStorage.removeItem('auth_return_url')
    return url
  }

  /**
   * Handle silent renew callback (for iframe)
   */
  async handleSilentRenewCallback(): Promise<void> {
    await this.userManager.signinSilentCallback()
  }
}

// Export singleton instance
const authService = new AuthService()
export default authService

// Export types
export type { User }
