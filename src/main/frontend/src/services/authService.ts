// src/services/authService.ts
// Simple OIDC Authentication Service for Authentik
// No token refresh logic - simple authentication only

import { UserManager, User } from 'oidc-client-ts'
import { oidcConfig, claimMappings } from '@/config/oidc.config'

/**
 * Simple User interface
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
 * Simple Authentication Service
 * Handles basic login/logout with Authentik
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
   * Simple event listeners - no token refresh logic
   */
  private setupEventListeners(): void {
    // User loaded on login
    this.userManager.events.addUserLoaded((user) => {
      console.log('User loaded:', user.profile.email)
      this.currentUser = this.mapUser(user)
    })

    // User logged out
    this.userManager.events.addUserUnloaded(() => {
      console.log('User unloaded')
      this.currentUser = null
    })

    // Token expired - simple notification
    this.userManager.events.addAccessTokenExpired(() => {
      console.log('Access token expired')
      this.currentUser = null
    })
  }

  /**
   * Initialize user from storage
   */
  private async initializeUser(): Promise<void> {
    try {
      const user = await this.userManager.getUser()
      if (user && !user.expired) {
        this.currentUser = this.mapUser(user)
        console.log('User restored:', this.currentUser.email)
      }
    } catch (error) {
      console.error('Failed to initialize user:', error)
    }
  }

  /**
   * Map OIDC User to AppUser
   */
  private mapUser(user: User): AppUser {
    const profile = user.profile

    // Get groups
    const groups: string[] = (profile[claimMappings.groupsClaim] as string[]) || []

    // Map groups to roles
    const roles = groups.map(group => {
      const mappedRole = claimMappings.roleMapping[group.toLowerCase()]
      return mappedRole || `${claimMappings.rolePrefix}${group.toUpperCase()}`
    })

    // Add default role if none
    if (roles.length === 0) {
      roles.push('ROLE_USER')
    }

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
   * Start login
   */
  async login(returnUrl?: string): Promise<void> {
    try {
      if (returnUrl) {
        sessionStorage.setItem('auth_return_url', returnUrl)
      }

      console.log('Starting login...')
      await this.userManager.signinRedirect()
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  /**
   * Handle callback after login
   */
  async handleCallback(): Promise<AppUser> {
    try {
      console.log('Processing callback...')
      const user = await this.userManager.signinRedirectCallback()
      this.currentUser = this.mapUser(user)
      console.log('Login successful:', this.currentUser.email)
      return this.currentUser
    } catch (error) {
      console.error('Callback error:', error)
      throw error
    }
  }

  /**
   * Logout
   */
  async logout(): Promise<void> {
    try {
      console.log('Logging out...')
      await this.userManager.signoutRedirect()
    } catch (error) {
      console.error('Logout error:', error)
      // Local logout if redirect fails
      await this.userManager.removeUser()
      this.currentUser = null
      throw error
    }
  }

  /**
   * Silent logout (local only)
   */
  async silentLogout(): Promise<void> {
    await this.userManager.removeUser()
    this.currentUser = null
    console.log('Silent logout completed')
  }

  /**
   * Get current user
   */
  getUser(): AppUser | null {
    return this.currentUser
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    // Return token if user exists and token not expired
    if (this.currentUser?.accessToken && !this.isTokenExpired()) {
      return this.currentUser.accessToken
    }
    
    // Tryin to get from storage
    const storageKey = `oidc.user:${oidcConfig.authority}:${oidcConfig.client_id}`
    const stored = localStorage.getItem(storageKey)
    
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        if (parsed.access_token) {
          return parsed.access_token
        }
      } catch (e) {
        console.warn('Could not parse stored token')
      }
    }
    
    return null
  }

  /**
   * Check if authenticated
   */
  isAuthenticated(): boolean {
    return this.currentUser !== null && !this.isTokenExpired()
  }

  /**
   * Check if token expired
   */
  isTokenExpired(): boolean {
    if (!this.currentUser?.expiresAt) return true
    return Date.now() / 1000 > this.currentUser.expiresAt
  }

  /**
   * Check role
   */
  hasRole(role: string): boolean {
    if (!this.currentUser) return false
    const normalizedRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`
    return this.currentUser.roles.includes(normalizedRole)
  }

  /**
   * Check any role
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role))
  }

  /**
   * Check all roles
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role))
  }

  /**
   * Get return URL after login
   */
  getReturnUrl(): string | null {
    const url = sessionStorage.getItem('auth_return_url')
    sessionStorage.removeItem('auth_return_url')
    return url
  }

  // ==================== SIMPLE HTTP HELPERS ====================

  /**
   * Build URL
   */
  private buildUrl(url: string): string {
    if (url.startsWith('http')) {
      return url
    }
    
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    const cleanBase = baseUrl.replace(/\/$/, '')
    const cleanUrl = url.startsWith('/') ? url : `/${url}`
    
    return `${cleanBase}${cleanUrl}`
  }

  /**
   * GET request
   */
  async get(url: string): Promise<any> {
    const token = this.getAccessToken()
    if (!token) {
      throw new Error('No access token available')
    }

    const fullUrl = this.buildUrl(url)
    const response = await fetch(fullUrl, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    return response.json()
  }

  /**
   * POST request
   */
  async post(url: string, data?: any): Promise<any> {
    const token = this.getAccessToken()
    if (!token) {
      throw new Error('No access token available')
    }

    const fullUrl = this.buildUrl(url)
    const response = await fetch(fullUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: data ? JSON.stringify(data) : undefined
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const text = await response.text()
    return text ? JSON.parse(text) : {}
  }

  /**
   * PUT request
   */
  async put(url: string, data?: any): Promise<any> {
    const token = this.getAccessToken()
    if (!token) {
      throw new Error('No access token available')
    }

    const fullUrl = this.buildUrl(url)
    const response = await fetch(fullUrl, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: data ? JSON.stringify(data) : undefined
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const text = await response.text()
    return text ? JSON.parse(text) : {}
  }

  /**
   * DELETE request
   */
  async delete(url: string): Promise<any> {
    const token = this.getAccessToken()
    if (!token) {
      throw new Error('No access token available')
    }

    const fullUrl = this.buildUrl(url)
    const response = await fetch(fullUrl, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const text = await response.text()
    return text ? JSON.parse(text) : {}
  }

  /**
   * Get user info
   */
  getUserInfo() {
    if (!this.currentUser) return null
    
    return {
      name: this.currentUser.name,
      email: this.currentUser.email,
      firstName: this.currentUser.firstName,
      lastName: this.currentUser.lastName,
      roles: this.currentUser.roles,
      groups: this.currentUser.groups
    }
  }
}

// Export singleton
const authService = new AuthService()
export default authService

// Export types
export type { User }
