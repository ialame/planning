// router/index.ts
// Vue Router with OIDC Authentication Guards

import { createRouter, createWebHistory, RouteRecordRaw, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import oidcAuthService from '@/services/oidcAuthService'

// Views
import Dashboard from '@/views/Dashboard.vue'
import Orders from '@/views/Orders.vue'
import Employees from '@/views/Employees.vue'
import Planning from '@/views/Planning.vue'
import Teams from '@/views/Teams.vue'
import DataSync from '@/views/DataSync.vue'
import Callback from '@/views/Callback.vue'

/**
 * Route meta interface
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: string[]
    title?: string
  }
}

/**
 * Application routes
 */
const routes: RouteRecordRaw[] = [
  // ==================== PUBLIC ROUTES ====================
  {
    path: '/callback',
    name: 'callback',
    component: Callback,
    meta: {
      requiresAuth: false,
      title: 'Authenticating...'
    }
  },
  {
    path: '/login',
    name: 'login',
    // Redirect to Authentik login
    beforeEnter: async () => {
      await oidcAuthService.login()
      return false // Prevent navigation (redirect happens externally)
    },
    component: { template: '<div>Redirecting to login...</div>' }
  },
  {
    path: '/logout',
    name: 'logout',
    beforeEnter: async () => {
      await oidcAuthService.logout()
      return false
    },
    component: { template: '<div>Logging out...</div>' }
  },

  // ==================== PROTECTED ROUTES ====================
  {
    path: '/',
    name: 'dashboard',
    component: Dashboard,
    meta: {
      requiresAuth: true,
      title: 'Dashboard'
    }
  },
  {
    path: '/orders',
    name: 'orders',
    component: Orders,
    meta: {
      requiresAuth: true,
      title: 'Orders'
    }
  },
  {
    path: '/employees',
    name: 'employees',
    component: Employees,
    meta: {
      requiresAuth: true,
      roles: ['ADMIN', 'MANAGER'],
      title: 'Employees'
    }
  },
  {
    path: '/planning',
    name: 'planning',
    component: Planning,
    meta: {
      requiresAuth: true,
      title: 'Planning'
    }
  },
  {
    path: '/groups',
    name: 'groups',
    component: Teams,
    meta: {
      requiresAuth: true,
      roles: ['ADMIN', 'MANAGER'],
      title: 'Teams'
    }
  },
  {
    path: '/sync',
    name: 'DataSync',
    component: DataSync,
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Data Sync'
    }
  },

  // ==================== ERROR ROUTES ====================
  {
    path: '/unauthorized',
    name: 'unauthorized',
    component: {
      template: `
        <div class="min-h-screen flex items-center justify-center bg-gray-100">
          <div class="bg-white p-8 rounded-lg shadow-lg text-center">
            <div class="text-red-500 text-6xl mb-4">🚫</div>
            <h1 class="text-2xl font-bold text-gray-800 mb-2">Access Denied</h1>
            <p class="text-gray-600 mb-4">You don't have permission to access this page.</p>
            <router-link to="/" class="text-blue-600 hover:underline">Go to Dashboard</router-link>
          </div>
        </div>
      `
    },
    meta: {
      requiresAuth: true,
      title: 'Access Denied'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: {
      template: `
        <div class="min-h-screen flex items-center justify-center bg-gray-100">
          <div class="bg-white p-8 rounded-lg shadow-lg text-center">
            <div class="text-gray-400 text-6xl mb-4">404</div>
            <h1 class="text-2xl font-bold text-gray-800 mb-2">Page Not Found</h1>
            <p class="text-gray-600 mb-4">The page you're looking for doesn't exist.</p>
            <router-link to="/" class="text-blue-600 hover:underline">Go to Dashboard</router-link>
          </div>
        </div>
      `
    },
    meta: {
      title: 'Page Not Found'
    }
  }
]

/**
 * Create router instance
 */
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * Global navigation guard
 */
router.beforeEach(async (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  // Update document title
  document.title = to.meta.title 
    ? `${to.meta.title} | Pokemon Planning`
    : 'Pokemon Planning'

  // Skip auth check for callback route
  if (to.name === 'callback') {
    return next()
  }

  // Check if route requires authentication
  if (to.meta.requiresAuth) {
    const isAuthenticated = await oidcAuthService.isAuthenticated()
    
    if (!isAuthenticated) {
      console.log('🔐 Not authenticated, redirecting to login...')
      
      // Save intended destination
      sessionStorage.setItem('auth_redirect_url', to.fullPath)
      
      // Redirect to Authentik login
      await oidcAuthService.login()
      return // Login redirects externally
    }

    // Check role requirements
    if (to.meta.roles && to.meta.roles.length > 0) {
      const hasRequiredRole = oidcAuthService.hasAnyRole(to.meta.roles)
      
      if (!hasRequiredRole) {
        console.log('🚫 Insufficient permissions for route:', to.path)
        return next({ name: 'unauthorized' })
      }
    }
  }

  next()
})

/**
 * After each navigation
 */
router.afterEach((to) => {
  // Analytics, logging, etc.
  console.log(`📍 Navigated to: ${to.path}`)
})

export default router
