// src/router/index.ts
// Vue Router with OAuth2 authentication guards

import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import authService from '@/services/authService'

// Views
import Dashboard from '@/views/Dashboard.vue'
import Orders from '@/views/Orders.vue'
import Employees from '@/views/Employees.vue'
import Planning from '@/views/Planning.vue'
import Teams from '@/views/Teams.vue'
import DataSync from '@/views/DataSync.vue'

// Auth views
import OAuthCallback from '@/views/auth/OAuthCallback.vue'
import SilentRenew from '@/views/auth/SilentRenew.vue'
import Unauthorized from '@/views/auth/Unauthorized.vue'

/**
 * Route meta interface for TypeScript
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: string[]
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ==================== AUTH ROUTES ====================
    {
      path: '/callback',
      name: 'oauth-callback',
      component: OAuthCallback,
      meta: { requiresAuth: false, title: 'Logging in...' }
    },
    {
      path: '/silent-renew',
      name: 'silent-renew',
      component: SilentRenew,
      meta: { requiresAuth: false }
    },
    {
      path: '/unauthorized',
      name: 'unauthorized',
      component: Unauthorized,
      meta: { requiresAuth: false, title: 'Unauthorized' }
    },

    // ==================== APP ROUTES ====================
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard,
      meta: { requiresAuth: true, title: 'Dashboard' }
    },
    {
      path: '/orders',
      name: 'orders',
      component: Orders,
      meta: { requiresAuth: true, title: 'Orders' }
    },
    {
      path: '/employees',
      name: 'employees',
      component: Employees,
      meta: { 
        requiresAuth: true, 
        roles: ['ROLE_ADMIN', 'ROLE_MANAGER'],
        title: 'Employees' 
      }
    },
    {
      path: '/planning',
      name: 'planning',
      component: Planning,
      meta: { requiresAuth: true, title: 'Planning' }
    },
    {
      path: '/groups',
      name: 'groups',
      component: Teams,
      meta: { 
        requiresAuth: true, 
        roles: ['ROLE_ADMIN', 'ROLE_MANAGER'],
        title: 'Teams' 
      }
    },
    {
      path: '/sync',
      name: 'data-sync',
      component: DataSync,
      meta: { 
        requiresAuth: true, 
        roles: ['ROLE_ADMIN'],
        title: 'Data Sync' 
      }
    },

    // ==================== CATCH-ALL ====================
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

// ==================== NAVIGATION GUARDS ====================

/**
 * Global before guard - handles authentication
 */
router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized) => {
  // Update page title
  document.title = to.meta.title 
    ? `${to.meta.title} | Pokemon Card Planning` 
    : 'Pokemon Card Planning'

  // Skip auth check for non-protected routes
  if (!to.meta.requiresAuth) {
    return true
  }

  // Check authentication
  const isAuthenticated = authService.isAuthenticated()

  if (!isAuthenticated) {
    console.log(' Route requires auth, redirecting to login...')
    // Store intended destination
    await authService.login(to.fullPath)
    return false // Redirect will happen in login()
  }

  // Check role requirements
  if (to.meta.roles && to.meta.roles.length > 0) {
    const hasRequiredRole = authService.hasAnyRole(to.meta.roles)
    
    if (!hasRequiredRole) {
      console.warn(' User lacks required roles:', to.meta.roles)
      return { name: 'unauthorized' }
    }
  }

  return true
})

/**
 * Global after guard - analytics, cleanup, etc.
 */
router.afterEach((to: RouteLocationNormalized) => {
  // Could add analytics tracking here
  console.log(` Navigated to: ${to.path}`)
})

/**
 * Global error handler
 */
router.onError((error) => {
  console.error(' Router error:', error)
})

export default router
