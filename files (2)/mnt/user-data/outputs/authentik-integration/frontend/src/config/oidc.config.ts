// config/oidc.config.ts
// Authentik OIDC Configuration

import { UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts'

/**
 * OIDC Configuration for Authentik
 * 
 * Environment variables:
 * - VITE_AUTHENTIK_AUTHORITY: Authentik issuer URL
 * - VITE_AUTHENTIK_CLIENT_ID: OAuth2 Client ID
 * - VITE_APP_URL: Application base URL
 */

const authority = import.meta.env.VITE_AUTHENTIK_AUTHORITY || 'https://auth.pcagrade.com/application/o/pokemon-planning/'
const clientId = import.meta.env.VITE_AUTHENTIK_CLIENT_ID || 'your-client-id'
const appUrl = import.meta.env.VITE_APP_URL || 'http://localhost:5173'

export const oidcConfig: UserManagerSettings = {
  // Authentik server URL (issuer)
  authority: authority,
  
  // Client ID from Authentik application
  client_id: clientId,
  
  // Redirect URI after login (must match Authentik configuration)
  redirect_uri: `${appUrl}/callback`,
  
  // Redirect URI after logout
  post_logout_redirect_uri: `${appUrl}/`,
  
  // Silent renew URI for token refresh
  silent_redirect_uri: `${appUrl}/silent-renew.html`,
  
  // Response type (authorization code flow with PKCE)
  response_type: 'code',
  
  // Scopes to request
  scope: 'openid profile email',
  
  // Automatically renew tokens before expiration
  automaticSilentRenew: true,
  
  // Include ID token claims in user profile
  loadUserInfo: true,
  
  // Storage for tokens (localStorage persists across browser sessions)
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  
  // Monitor session state
  monitorSession: true,
  
  // Check session interval (in seconds)
  checkSessionIntervalInSeconds: 30,
  
  // Filter protocol claims from profile
  filterProtocolClaims: true,
  
  // Revoke tokens on signout
  revokeTokensOnSignout: true,
}

// Authentik-specific endpoints (auto-discovered from .well-known, but can be overridden)
export const authentikEndpoints = {
  wellKnown: `${authority}/.well-known/openid-configuration`,
  userInfo: `${authority}/userinfo/`,
  token: `${authority}/token/`,
  authorize: `${authority}/authorize/`,
  logout: `${authority}/end-session/`,
}

export default oidcConfig
