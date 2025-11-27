<template>
  <div id="app" class="min-h-screen bg-gray-50">
    <!-- Navigation -->
    <nav class="bg-gray-800 text-white shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <!-- Login Modal -->
        <LoginModal v-if="!isAuthenticated" @login-success="onLoginSuccess" />
        <!-- Main App (only if authenticated) -->
        <div v-if="isAuthenticated">
          <nav class="bg-gray-700 text-white p-4 flex justify-between items-center">
            <span class="flex items-center gap-2">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="10" fill="white" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="12" r="8" fill="white" stroke="currentColor" stroke-width="1"/>
                <path d="M2 12H22" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="12" r="3" fill="white" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="12" r="1.5" fill="currentColor"/>
              </svg>
              Welcome, {{ user?.email }}
            </span>
            <button
              @click="logout"
              class="text-gray-300 hover:text-[#730d10] px-4 py-2 flex items-center gap-2 transition-colors duration-200"
            >
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
              </svg>
              Logout
            </button>
          </nav>

          <div class="flex justify-between items-center h-16">
            <div class="flex items-center">
              <h1 class="text-xl font-bold flex items-center gap-2">
                <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="12" r="10" fill="white" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="8" fill="white" stroke="currentColor" stroke-width="1"/>
                  <path d="M2 12H22" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" fill="white" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="1.5" fill="currentColor"/>
                  <path d="M4 12C4 7.58172 7.58172 4 12 4" stroke="currentColor" stroke-width="2"/>
                  <path d="M20 12C20 7.58172 16.4183 4 12 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                Pokemon Card Planning
              </h1>
            </div>
            
            <!-- Clean Navigation Menu -->
            <ul class="flex items-center space-x-0">
              <li class="nav-item" v-for="tab in tabs" :key="tab.id">
                <button
                  @click="changeTab(tab.id)"
                  :class="[
                    'nav-link py-0 px-4 transition-colors duration-200 uppercase text-sm font-semibold',
                    activeTab === tab.id ? 'text-white' : 'text-gray-300 hover:text-[#730d10]'
                  ]"
                >
                  <component :is="tab.icon" class="w-4 h-4 align-top hidden xl:inline-block mr-2" />
                  {{ tab.label }}
                </button>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 py-6">
      <DashboardView v-if="activeTab === 'dashboard'" @go-to-tab="changeTab" />
      <OrdersView v-if="activeTab === 'orders'" />
      <EmployeesView v-if="activeTab === 'employees'" />
      <PlanningView v-if="activeTab === 'planning'" />
      <TeamsView v-if="activeTab === 'teams'" />
      <SyncView v-if="activeTab === 'sync'" />
    </main>

    <!-- Notifications -->
    <div
      v-if="notification.show"
      :class="[
        'fixed top-4 right-4 p-4 rounded-lg shadow-lg transition-all duration-300 z-50 flex items-center gap-2 border-l-4',
        notification.type === 'success' 
          ? 'bg-gray-50 text-gray-800 border-gray-500' 
          : 'bg-red-50 text-red-800 border-red-500'
      ]"
    >
      <svg v-if="notification.type === 'success'" class="w-5 h-5 text-gray-600" viewBox="0 0 24 24" fill="currentColor">
        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
      </svg>
      <svg v-else class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
      </svg>
      <span class="font-medium">{{ notification.message }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, provide, computed } from 'vue'
import DashboardView from './views/Dashboard.vue'
import OrdersView from './views/Orders.vue'
import EmployeesView from './views/Employees.vue'
import PlanningView from './views/Planning.vue'
import TeamsView from "./views/Teams.vue";
import SyncView from "./views/DataSync.vue";
import authService, { type User } from '@/services/authService'
import { useAuth } from '@/composables/useAuth'
import LoginModal from '@/components/LoginModal.vue'

// SVG Icon Components
const DashboardIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/>
    </svg>
  `
}

const OrdersIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
    </svg>
  `
}

const EmployeesIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A2.01 2.01 0 0 0 18.06 7h-2.12c-.93 0-1.76.53-2.18 1.37L11.5 16H14v6h6zm-7.5-10.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5zM5.5 6c1.11 0 2-.89 2-2s-.89-2-2-2-2 .89-2 2 .89 2 2 2zm2 16v-7H9V9c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v6h1.5v7h4zm6.5 0v-4h1v-4c0-.55-.45-1-1-1h-2c-.55 0-1 .45-1 1v4h1v4h2z"/>
    </svg>
  `
}

const PlanningIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V9h14v10zM5 7V5h14v2H5zm2 4h10v2H7zm0 4h7v2H7z"/>
    </svg>
  `
}

const TeamsIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
    </svg>
  `
}

const SyncIcon = {
  template: `
    <svg fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"/>
    </svg>
  `
}

const { isAuthenticated, user } = useAuth()

function onLoginSuccess(userData: any) {
  console.log('Login successful:', userData)
}

function logout(): void {
  authService.logout()
  user.value = null
  location.reload()
}

// Global state
const activeTab = ref('dashboard')
const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const tabs = [
  { id: 'dashboard', label: 'Dashboard', icon: DashboardIcon },
  { id: 'orders', label: 'Orders', icon: OrdersIcon },
  { id: 'employees', label: 'Employees', icon: EmployeesIcon },
  { id: 'planning', label: 'Planning', icon: PlanningIcon },
  { id: 'teams', label: 'Teams', icon: TeamsIcon },
  { id: 'sync', label: 'ApiSync', icon: SyncIcon },
]

// Function to change tab
const changeTab = (tabId: string) => {
  console.log('Changing to tab:', tabId)
  activeTab.value = tabId
}

// Function to show notifications
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

// Provide for child components
provide('showNotification', showNotification)
provide('changeTab', changeTab)
</script>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Custom scrollbar for grey theme */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: #f3f4f6;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #9ca3af;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}

/* Navigation styles */
.nav-item {
  position: relative;
}

.nav-link {
  background: none !important;
  border: none !important;
  color: inherit;
  font-family: inherit;
  font-size: inherit;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  font-weight: 500;
  letter-spacing: 0.5px;
  outline: none !important;
  box-shadow: none !important;
}

.nav-link:hover {
  text-decoration: none;
  background: transparent !important;
  border: none !important;
  transform: none !important;
}

/* Ensure no background or border on hover */
.nav-link:hover * {
  background: transparent !important;
}

/* Smooth transitions for all interactive elements */
button, a {
  transition: all 0.2s ease-in-out;
}

/* Enhanced focus states for accessibility */
button:focus-visible {
  outline: 2px solid #6b7280;
  outline-offset: 2px;
}

/* Custom burgundy color for hover */
.hover-burgundy-custom:hover {
  color: #730d10 !important;
}
</style>