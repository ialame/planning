// src/config/oidc.config.ts
// OpenID Connect configuration for Authentik with 1-day token refresh
// Silent renew is DISABLED due to X-Frame-Options: deny from Authentik

import { UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts'

/**
 * OIDC Configuration for Authentik
 * 
 * Environment variables (in .env):
 * - VITE_AUTHENTIK_AUTHORITY: Authentik issuer URL
 * - VITE_AUTHENTIK_CLIENT_ID: OAuth2 Client ID
 * - VITE_APP_URL: Application base URL
 * 
 * IMPORTANT: Configure Authentik provider with:
 * 1. Access token validity: 1 days (86400 seconds)
 * 2. Enable refresh tokens
 * 3. Add 'refresh_token' to allowed scopes
 */

// Helper to get environment variables with validation
const getEnv = (key: string, defaultValue?: string): string => {
  const value = import.meta.env[key] || defaultValue
  if (!value && key.startsWith('VITE_AUTHENTIK_')) {
    console.warn(` Missing environment variable: ${key}`)
  }
  return value || ''
}

// Base URL for the application
const appBaseUrl = getEnv('VITE_APP_URL', window.location.origin)

export const oidcConfig: UserManagerSettings = {
  // ==================== REQUIRED SETTINGS ====================
  
  // Authentik server URL (issuer) - remove trailing slash
  authority: getEnv('VITE_AUTHENTIK_AUTHORITY', 'https://auth.pcagrade.com/application/o/gpt-dev/').replace(/\/$/, ''),
  
  // Client ID from Authentik application
  client_id: getEnv('VITE_AUTHENTIK_CLIENT_ID', 'd2J6p8Y3ty6Sbxg7Xb0yBQwYGFOeMMhGDsfYuCFn'),
  
  // Redirect URI after login (must match Authentik config)
  redirect_uri: `${appBaseUrl}/callback`,
  
  // Redirect URI after logout
  post_logout_redirect_uri: `${appBaseUrl}/`,
  
  // Response type (authorization code flow with PKCE)
  response_type: 'code',
  
  // ==================== SCOPES ====================
  // IMPORTANT: Include 'refresh_token' for 1-day refresh capability
  scope: 'openid profile email groups offline_access refresh_token',
  
  // ==================== MANUAL METADATA ====================
  // Using manual metadata instead of discovery to avoid issues
  metadata: {
    // Issuer URL
    issuer: 'https://auth.pcagrade.com/application/o/gpt-dev/',
    
    // OAuth2/OIDC endpoints
    authorization_endpoint: 'https://auth.pcagrade.com/application/o/authorize/',
    token_endpoint: 'https://auth.pcagrade.com/application/o/token/',
    userinfo_endpoint: 'https://auth.pcagrade.com/application/o/userinfo/',
    end_session_endpoint: 'https://auth.pcagrade.com/application/o/end-session/',
    
    // JWKS endpoint for token validation
    jwks_uri: 'https://auth.pcagrade.com/application/o/pcagrade/.well-known/jwks.json',
    
    // Required OIDC metadata
    token_endpoint_auth_methods_supported: ['client_secret_basic', 'client_secret_post'],
    response_types_supported: ['code'],
    subject_types_supported: ['public'],
    id_token_signing_alg_values_supported: ['RS256'],
    
    // IMPORTANT: Include 'refresh_token' in supported scopes
    scopes_supported: ['openid', 'profile', 'email', 'offline_access', 'groups', 'refresh_token'],
    
    // Claims supported by Authentik
    claims_supported: [
      'sub', 
      'name', 
      'email', 
      'groups', 
      'preferred_username', 
      'given_name', 
      'family_name'
    ]
  },
  
  // Disable automatic metadata discovery (using manual config above)
  metadataUrl: undefined,
  
  // ==================== TOKEN SETTINGS ====================
  
  // DISABLED: Automatic silent renew (Authentik blocks iframes with X-Frame-Options: deny)
  automaticSilentRenew: false,
  
  // DISABLED: Include ID token in silent renew
  includeIdTokenInSilentRenew: false,
  
  // Load user info from userinfo endpoint
  loadUserInfo: true,
  
  // Token storage (localStorage for persistence)
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  
  // ==================== SESSION SETTINGS ====================
  
  // DISABLED: Monitor session (requires iframe which Authentik blocks)
  monitorSession: false,
  
  // DISABLED: Check session interval (not used when monitorSession is false)
  checkSessionIntervalInSeconds: 0,
  
  // Revoke tokens on logout
  revokeTokensOnSignout: true,
  
  // Filter protocol claims from profile
  filterProtocolClaims: true,
  
  // ==================== SECURITY SETTINGS ====================
  
  // PKCE (Proof Key for Code Exchange) for SPA security
  extraQueryParams: {
    code_challenge_method: 'S256'
  },
  
  // Access token expiration notification time (in seconds)
  // Will trigger 'accessTokenExpiring' event 5 minutes before expiry
  accessTokenExpiringNotificationTime: 300, // 5 minutes
  
  // Additional token parameters
  extraTokenParams: {
    // Can add additional parameters if needed by Authentik
    // audience: 'https://api.pcagrade.com'
  },
  
  // ==================== DISABLED IFRAME FEATURES ====================
  // All iframe-based features disabled due to X-Frame-Options restrictions
  
  validateSubOnSilentRenew: false,
  silentRequestTimeout: 0,
  
  // Use standard redirect method (not popup or iframe)
  redirectMethod: 'assign',
  
  // ==================== ADDITIONAL SETTINGS ====================
  
  // Response mode (default is good for SPAs)
  response_mode: 'query',
  
  // Should include user profile in storage
  storeUserInfo: true,
  
  // Automatic renew offset (not used when automaticSilentRenew is false)
  // automaticSilentRenewOffset: 60,
  
  // Clock skew tolerance (in seconds)
  clockSkew: 60,
}

/**
 * Authentik-specific claim mappings
 * Maps Authentik groups to application roles
 */
export const claimMappings = {
  // Authentik puts groups in 'groups' claim
  groupsClaim: 'groups',
  
  // Role prefix to add (makes roles consistent with Spring Security)
  rolePrefix: 'ROLE_',
  
  // Map Authentik groups to application roles
  // Add your specific group mappings here
  roleMapping: {
    // Example mappings - adjust based on your Authentik groups
    'noteurs': 'ROLE_NOTEUR',
    'certificateurs': 'ROLE_CERTIFICATEUR',
    'scanneurs': 'ROLE_SCANNEUR',
    'admins': 'ROLE_ADMIN',
    'managers': 'ROLE_MANAGER',
    'users': 'ROLE_USER',
  } as Record<string, string>,
  
  /**
   * Get application role from Authentik group
   */
  getRoleFromGroup: (group: string): string => {
    const normalizedGroup = group.toLowerCase()
    const mappedRole = claimMappings.roleMapping[normalizedGroup]
    return mappedRole || `${claimMappings.rolePrefix}${group.replace(/\s+/g, '_').toUpperCase()}`
  },
  
  /**
   * Get all roles from groups array
   */
  getRolesFromGroups: (groups: string[]): string[] => {
    return groups.map(group => claimMappings.getRoleFromGroup(group))
  }
}

// Default export for convenience
export default oidcConfig

// ==================== HELPER FUNCTIONS ====================

/**
 * Validate OIDC configuration on startup
 */
export const validateOidcConfig = (): string[] => {
  const warnings: string[] = []
  
  if (!oidcConfig.authority) {
    warnings.push('Missing authority URL')
  }
  
  if (!oidcConfig.client_id) {
    warnings.push('Missing client ID')
  }
  
  if (!oidcConfig.redirect_uri) {
    warnings.push('Missing redirect URI')
  }
  
  if (!oidcConfig.scope?.includes('refresh_token')) {
    warnings.push('Scope does not include refresh_token - 1-day refresh may not work')
  }
  
  if (oidcConfig.automaticSilentRenew) {
    warnings.push('automaticSilentRenew is enabled but will fail due to X-Frame-Options')
  }
  
  if (oidcConfig.monitorSession) {
    warnings.push('monitorSession is enabled but will fail due to X-Frame-Options')
  }
  
  return warnings
}

/**
 * Get token expiration time in millisec
 */
export const getTokenExpiryTime = (): number => {
  // Default to 1 day (86400 seconds) in milliseconds
  return 24 * 60 * 60 * 1000
}

/**
 * Get refresh threshold (when to start refreshing before expiry)
 */
export const getRefreshThreshold = (): number => {
  // Refresh 1 hour before token expires
  return 60 * 60 * 1000 // 1 hour in milliseconds
}

// Log configuration warnings in development
if (import.meta.env.DEV) {
  const warnings = validateOidcConfig()
  if (warnings.length > 0) {
    console.warn('OIDC Configuration Warnings:', warnings)
  } else {
    console.log(' OIDC configuration validated successfully')
  }
}
