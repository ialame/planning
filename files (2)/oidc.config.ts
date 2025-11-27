// src/config/oidc.config.ts
// OpenID Connect configuration for Authentik

import { UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts'

/**
 * OIDC Configuration for Authentik
 * 
 * Environment variables (in .env):
 * - VITE_AUTHENTIK_AUTHORITY: Authentik issuer URL
 * - VITE_AUTHENTIK_CLIENT_ID: OAuth2 Client ID
 * - VITE_APP_URL: Application base URL
 */
export const oidcConfig: UserManagerSettings = {
  // Authentik server URL (issuer)
  authority: import.meta.env.VITE_AUTHENTIK_AUTHORITY || 'https://auth.pcagrade.com/application/o/pokemon-planning/',
  
  // Client ID from Authentik application
  client_id: import.meta.env.VITE_AUTHENTIK_CLIENT_ID || 'pokemon-planning-client',
  
  // Redirect URI after login (must match Authentik config)
  redirect_uri: `${import.meta.env.VITE_APP_URL || window.location.origin}/callback`,
  
  // Redirect URI after logout
  post_logout_redirect_uri: `${import.meta.env.VITE_APP_URL || window.location.origin}/`,
  
  // Silent renew redirect
  silent_redirect_uri: `${import.meta.env.VITE_APP_URL || window.location.origin}/silent-renew`,
  
  // Response type (authorization code flow with PKCE)
  response_type: 'code',
  
  // Scopes requested
  scope: 'openid profile email offline_access',
  
  // Automatically renew tokens
  automaticSilentRenew: true,
  
  // Include ID token in silent renew
  includeIdTokenInSilentRenew: true,
  
  // Load user info from userinfo endpoint
  loadUserInfo: true,
  
  // Token storage
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  
  // Monitor session (single sign-out)
  monitorSession: true,
  
  // Check session interval (in seconds)
  checkSessionIntervalInSeconds: 30,
  
  // Revoke tokens on logout
  revokeTokensOnSignout: true,
  
  // Filter protocol claims from profile
  filterProtocolClaims: true,
}

/**
 * Authentik-specific claim mappings
 */
export const claimMappings = {
  // Authentik puts groups in 'groups' claim
  groupsClaim: 'groups',
  
  // Role prefix to add
  rolePrefix: 'ROLE_',
  
  // Map Authentik groups to application roles
  roleMapping: {
    'noteurs': 'ROLE_NOTEUR',
    'certificateurs': 'ROLE_CERTIFICATEUR',
    'scanneurs': 'ROLE_SCANNEUR',
    'admins': 'ROLE_ADMIN',
    'managers': 'ROLE_MANAGER',
  } as Record<string, string>
}

export default oidcConfig
