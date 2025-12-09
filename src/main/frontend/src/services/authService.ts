// src/services/authService.ts
// Simple OIDC Authentication Service for Authentik
// Supports Docker mode with authentication disabled

import { UserManager, User } from 'oidc-client-ts'
import { oidcConfig, claimMappings } from '@/config/oidc.config'

/**
 * Check if authentication is disabled (Docker dev mode)
 */
const isAuthDisabled = (): boolean => {
  return import.meta.env.VITE_AUTH_DISABLED === 'true'
}

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
  raw: User | null
}

/**
 * Mock user for Docker dev mode
 */
const MOCK_USER: AppUser = {
  id: 'docker-dev-user',
  email: 'dev@localhost',
  name: 'Docker Dev User',
  firstName: 'Docker',
  lastName: 'Dev',
  roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_USER'],
  groups: ['admin', 'managers'],
  accessToken: 'docker-dev-token',
  idToken: 'docker-dev-id-token',
  expiresAt: Date.now() / 1000 + 86400 * 365, // 1 year
  raw: null
}

/**
 * Simple Authentication Service
 * Handles basic login/logout with Authentik
 * Supports Docker mode with authentication disabled
 */
class AuthService {
  private userManager: UserManager | null = null
  private currentUser: AppUser | null = null

  constructor() {
    if (isAuthDisabled()) {
      console.log('🔓 Auth DISABLED (Docker dev mode)')
      this.currentUser = MOCK_USER
    } else {
      this.userManager = new UserManager(oidcConfig)
      this.setupEventListeners()
      this.initializeUser()
    }
  }

  /**
   * Simple event listeners - no token refresh logic
   */
  private setupEventListeners(): void {
    if (!this.userManager) return

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
    if (!this.userManager) return

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
    // In Docker mode, just redirect to home
    if (isAuthDisabled()) {
      console.log('🔓 Auth disabled - skipping login')
      window.location.href = returnUrl || '/'
      return
    }

    try {
      if (returnUrl) {
        sessionStorage.setItem('auth_return_url', returnUrl)
      }

      console.log('Starting login...')
      await this.userManager!.signinRedirect()
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  /**
   * Handle callback after login
   */
  async handleCallback(): Promise<AppUser> {
    // In Docker mode, return mock user
    if (isAuthDisabled()) {
      return MOCK_USER
    }

    try {
      console.log('Processing callback...')
      const user = await this.userManager!.signinRedirectCallback()
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
    // In Docker mode, just redirect to home
    if (isAuthDisabled()) {
      console.log('🔓 Auth disabled - skipping logout')
      window.location.href = '/'
      return
    }

    try {
      console.log('Logging out...')
      await this.userManager!.signoutRedirect()
    } catch (error) {
      console.error('Logout error:', error)
      // Local logout if redirect fails
      await this.userManager!.removeUser()
      this.currentUser = null
      throw error
    }
  }

  /**
   * Silent logout (local only)
   */
  async silentLogout(): Promise<void> {
    if (isAuthDisabled()) {
      return
    }

    await this.userManager!.removeUser()
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
    // In Docker mode, return mock token
    if (isAuthDisabled()) {
      return 'docker-dev-token'
    }

    // Return token if user exists and token not expired
    if (this.currentUser?.accessToken && !this.isTokenExpired()) {
      return this.currentUser.accessToken
    }

    // Try to get from storage
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
    // In Docker mode, always authenticated
    if (isAuthDisabled()) {
      return true
    }

    return this.currentUser !== null && !this.isTokenExpired()
  }

  /**
   * Check if token expired
   */
  isTokenExpired(): boolean {
    // In Docker mode, never expired
    if (isAuthDisabled()) {
      return false
    }

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

    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010'
    const cleanBase = baseUrl.replace(/\/$/, '')
    const cleanUrl = url.startsWith('/') ? url : `/${url}`

    return `${cleanBase}${cleanUrl}`
  }

  /**
   * GET request
   */
  async get(url: string): Promise<any> {
    const fullUrl = this.buildUrl(url)
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Only add auth header if not in Docker mode
    if (!isAuthDisabled()) {
      const token = this.getAccessToken()
      if (!token) {
        throw new Error('No access token available')
      }
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(fullUrl, { headers })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    return response.json()
  }

  /**
   * POST request
   */
  async post(url: string, data?: any): Promise<any> {
    const fullUrl = this.buildUrl(url)
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Only add auth header if not in Docker mode
    if (!isAuthDisabled()) {
      const token = this.getAccessToken()
      if (!token) {
        throw new Error('No access token available')
      }
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(fullUrl, {
      method: 'POST',
      headers,
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
    const fullUrl = this.buildUrl(url)
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Only add auth header if not in Docker mode
    if (!isAuthDisabled()) {
      const token = this.getAccessToken()
      if (!token) {
        throw new Error('No access token available')
      }
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(fullUrl, {
      method: 'PUT',
      headers,
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
    const fullUrl = this.buildUrl(url)
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Only add auth header if not in Docker mode
    if (!isAuthDisabled()) {
      const token = this.getAccessToken()
      if (!token) {
        throw new Error('No access token available')
      }
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(fullUrl, {
      method: 'DELETE',
      headers
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
