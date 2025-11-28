<template>
  <div class="pokemon-dashboard">
    <!-- Hero Section with Grey and Burgundy Theme -->
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="white" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="8" fill="white" stroke="#730d10" stroke-width="1"/>
            <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
            <path d="M4 12C4 7.58172 7.58172 4 12 4" stroke="#730d10" stroke-width="2"/>
            <path d="M20 12C20 7.58172 16.4183 4 12 4" stroke="#730d10" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="hero-title">Pokemon Card Planning System</h1>
        <p class="hero-subtitle">Streamline your grading, certification, and scanning workflow</p>
        <div class="hero-timestamp">
          <svg class="timestamp-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          Last updated: {{ currentTime }}
        </div>
      </div>
      <div class="hero-decoration">
        <!-- Floating Pokemon Elements -->
        <div class="floating-pokemon pokeball-1">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="white" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="8" fill="white" stroke="#730d10" stroke-width="1"/>
            <path d="M2 12H22" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="3" fill="white" stroke="#730d10" stroke-width="2"/>
            <circle cx="12" cy="12" r="1.5" fill="#730d10"/>
          </svg>
        </div>
        <div class="floating-pokemon pokeball-2">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="white" stroke="#8a1c20" stroke-width="2"/>
            <circle cx="12" cy="12" r="8" fill="white" stroke="#8a1c20" stroke-width="1"/>
            <path d="M2 12H22" stroke="#8a1c20" stroke-width="2"/>
            <circle cx="12" cy="12" r="3" fill="white" stroke="#8a1c20" stroke-width="2"/>
            <circle cx="12" cy="12" r="1.5" fill="#8a1c20"/>
          </svg>
        </div>
        <div class="floating-pokemon energy-symbol">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M13 2L4 14H11L9 22L20 10H13L15 2Z" fill="#730d10" stroke="#8a1c20" stroke-width="1"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- Quick Stats Bar -->
    <div class="quick-stats">
      <div class="stat-item" v-for="stat in quickStats" :key="stat.label">
        <div class="stat-icon" v-html="stat.icon"></div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Main Content Grid - Two Column Layout -->
    <div class="content-wrapper">
      <!-- Left Column -->
      <div class="content-left">
        <!-- System Status Card -->
        <div class="card card-status">
          <div class="card-header">
            <h3>
              <svg class="header-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
              System Health
            </h3>
            <div :class="['status-badge', backendConnected ? 'status-online' : 'status-offline']">
              {{ backendConnected ? 'Online' : 'Offline' }}
            </div>
          </div>
          <div class="card-body">
            <div class="connection-status">
              <div class="status-item">
                <div class="status-indicator" :class="{ 'active': backendConnected }"></div>
                <div class="status-info">
                  <div class="status-title">Backend API</div>
                  <div class="status-desc">Spring Boot Service</div>
                </div>
              </div>
              <button @click="checkBackend" class="btn btn-outline" :disabled="checkingBackend">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"/>
                  <polyline points="1 20 1 14 7 14"/>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                </svg>
                <span v-if="!checkingBackend">Test Connection</span>
                <span v-else>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12a9 9 0 11-6.219-8.56" stroke-linecap="round"/>
                  </svg>
                  Testing...
                </span>
              </button>
            </div>
          </div>
        </div>

        <!-- Priority Distribution Chart -->
        <div class="card card-chart">
          <div class="card-header">
            <h3>
              <svg class="header-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10"/>
                <line x1="12" y1="20" x2="12" y2="4"/>
                <line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
              Orders by Priority
            </h3>
          </div>
          <div class="card-body">
            <div class="priority-chart">
              <div v-for="priority in priorityData" :key="priority.name" class="priority-bar-container">
                <div class="priority-info">
                  <span class="priority-name">{{ priority.name }}</span>
                  <span class="priority-count">{{ priority.count }}</span>
                </div>
                <div class="priority-bar-bg">
                  <div
                    class="priority-bar"
                    :class="`priority-${priority.level}`"
                    :style="{ width: `${(priority.count / maxPriorityCount) * 100}%` }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Actions Card -->
        <div class="card card-actions card-stretch">
          <div class="card-header">
            <h3>
              <svg class="header-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
              Quick Actions
            </h3>
          </div>
          <div class="card-body">
            <div class="action-grid">
              <button @click="generatePlanning" class="action-btn primary" :disabled="generatingPlanning">
                <div class="action-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                </div>
                <div class="action-text">
                  <div class="action-title">Generate Planning</div>
                  <div class="action-desc">Create work schedule</div>
                </div>
                <div v-if="generatingPlanning" class="action-loading">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12a9 9 0 11-6.219-8.56" stroke-linecap="round"/>
                  </svg>
                </div>
              </button>
              <button @click="refreshStats" class="action-btn" :disabled="loadingStats">
                <div class="action-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                </div>
                <div class="action-text">
                  <div class="action-title">Refresh Data</div>
                  <div class="action-desc">Update statistics</div>
                </div>
                <div v-if="loadingStats" class="action-loading">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12a9 9 0 11-6.219-8.56" stroke-linecap="round"/>
                  </svg>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="content-right">
        <!-- Symfony Backend Status Card -->
        <div class="card card-symfony">
          <div class="card-header">
            <h3>
              <svg class="header-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
              </svg>
              Symfony API
            </h3>
            <div :class="['status-badge', symfonyConnected ? 'status-online' : 'status-offline']">
              {{ symfonyConnected ? 'Online' : 'Offline' }}
            </div>
          </div>
          <div class="card-body">
            <div class="connection-status">
              <div class="status-item">
                <div class="status-indicator" :class="{ 'active': symfonyConnected }"></div>
                <div class="status-info">
                  <div class="status-title">Symfony Backend</div>
                  <div class="status-desc">Data Synchronization Service</div>
                </div>
              </div>

              <div class="sync-info" v-if="lastSyncDate">
                <div class="sync-label">Last Sync</div>
                <div class="sync-date">{{ formatSyncDate(lastSyncDate) }}</div>
                <div class="sync-time">{{ formatSyncTime(lastSyncDate) }}</div>
              </div>
              <div class="sync-info" v-else>
                <div class="sync-label">Last Sync</div>
                <div class="sync-never">Never synchronized</div>
              </div>

              <div class="symfony-actions">
                <button @click="checkSymfony" class="btn btn-outline" :disabled="checkingSymfony">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                  <span v-if="!checkingSymfony">Test</span>
                  <span v-else>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 12a9 9 0 11-6.219-8.56" stroke-linecap="round"/>
                    </svg>
                    Testing...
                  </span>
                </button>
                <button @click="syncData" class="btn btn-sync" :disabled="syncing || !symfonyConnected">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                  <span v-if="!syncing">Sync Now</span>
                  <span v-else>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 12a9 9 0 11-6.219-8.56" stroke-linecap="round"/>
                    </svg>
                    Syncing...
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity Card -->
        <div class="card card-activity card-full-height">
          <div class="card-header">
            <h3>
              <svg class="header-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
              Recent Activity
            </h3>
          </div>
          <div class="card-body">
            <div class="activity-list">
              <div v-for="(employee, idx) in (employees || []).slice(0, 5)" :key="employee.id" class="activity-item">
                <div class="activity-avatar">{{ employee.fullName.charAt(0) }}</div>
                <div class="activity-content">
                  <div class="activity-title">{{ employee.fullName }}</div>
                  <div class="activity-meta">
                    {{ employee.workHoursPerDay }}h/day •
                    <span :class="employee.active ? 'text-success' : 'text-muted'">
                      {{ employee.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                </div>
                <div class="activity-badge">{{ employee.email.split('@')[0] }}</div>
              </div>
              <div v-if="(employees || []).length > 5" class="activity-more">
                + {{ employees.length - 5 }} more employees
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Navigation Cards -->
    <div class="nav-section">
      <h2 class="section-title">Navigate to</h2>
      <div class="nav-grid">
        <router-link
          v-for="navItem in navigationItems"
          :key="navItem.id"
          :to="navItem.path"
          class="nav-card"
        >
          <div class="nav-icon" v-html="navItem.icon"></div>
          <div class="nav-content">
            <div class="nav-title">{{ navItem.title }}</div>
            <div class="nav-count">{{ navItem.count }} items</div>
          </div>
          <div class="nav-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </router-link>
      </div>
    </div>

    <!-- System Message Toast -->
    <transition name="slide-up">
      <div v-if="systemMessage" :class="['toast', `toast-${systemMessage.type}`]">
        <div class="toast-icon">
          <svg v-if="systemMessage.type === 'success'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <div class="toast-content">
          <div class="toast-title">{{ systemMessage.title }}</div>
          <div class="toast-message">{{ systemMessage.message }}</div>
        </div>
        <button @click="systemMessage = null" class="toast-close">×</button>
      </div>
    </transition>
  </div>
</template>

<script>
import authService from '@/services/authService'
export default {
  name: 'PokemonDashboard',
  data() {
    return {
      currentTime: new Date().toLocaleString(),
      backendConnected: false,
      checkingBackend: false,
      symfonyConnected: false,
      checkingSymfony: false,
      syncing: false,
      lastSyncDate: null,
      loadingStats: false,
      generatingPlanning: false,
      systemMessage: null,
      stats: {
        ordersCount: 0,
        employeesCount: 0,
        planningCount: 0
      },
      employees: [],
      orders: []
    }
  },
  computed: {
    quickStats() {
      return [
        {
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><path d="m16.5 9.4-9-5.19M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>',
          label: 'Total Orders',
          value: this.stats.ordersCount || 0
        },
        {
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
          label: 'Employees',
          value: this.stats.employeesCount || 0
        },
        {
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
          label: 'Active Plans',
          value: this.stats.planningCount || 0
        },
        {
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
          label: 'Completed',
          value: Math.floor((this.stats.ordersCount || 0) * 0.6)
        }
      ]
    },
    priorityData() {
      return [
        { name: 'Express', level: 'urgent', count: 15 },
        { name: 'Fast+', level: 'high', count: 28 },
        { name: 'Fast', level: 'medium', count: 42 },
        { name: 'Classic', level: 'low', count: 35 }
      ]
    },
    maxPriorityCount() {
      return Math.max(...this.priorityData.map(p => p.count))
    },
    navigationItems() {
      return [
        {
          id: 'orders',
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>',
          title: 'Orders',
          path: '/orders',
          count: this.stats.ordersCount
        },
        {
          id: 'employees',
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
          title: 'Employees',
          path: '/employees',
          count: this.stats.employeesCount
        },
        {
          id: 'planning',
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
          title: 'Planning',
          path: '/planning',
          count: this.stats.planningCount
        },
        {
          id: 'teams',
          icon: '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#730d10" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>',
          title: 'Teams',
          path: '/groups',
          count: 7
        }
      ]
    }
  },
  mounted() {
    this.loadAllData()
    setInterval(() => {
      this.currentTime = new Date().toLocaleString()
    }, 1000)
    this.checkSymfony()
    this.loadLastSyncDate()
  },
  methods: {
    async loadAllData() {
      await Promise.all([
        this.checkBackend(),
        this.loadStats(),
        this.loadEmployees()
      ])
    },
    async checkBackend() {
      this.checkingBackend = true
      try {
        const response = await authService.get('/actuator/health')
        this.backendConnected = response.ok
      } catch (error) {
        this.backendConnected = false
      } finally {
        this.checkingBackend = false
      }
    },
    async checkSymfony() {
      this.checkingSymfony = true
      try {
        const response = await authService.get('/api/health', {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        })
        this.symfonyConnected = response.ok
      } catch (error) {
        console.error('Symfony connection error:', error)
        this.symfonyConnected = false
      } finally {
        this.checkingSymfony = false
      }
    },
    async syncData() {
      this.syncing = true
      try {
        const response = await authService.get('/api/sync/all', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        })
        if (response.ok) {
          this.lastSyncDate = new Date()
          this.saveLastSyncDate()
          this.showMessage('success', 'Sync Complete', 'Data synchronized successfully from Symfony API')
          await this.loadStats()
        } else {
          throw new Error('Sync failed')
        }
      } catch (error) {
        console.error('Sync error:', error)
        this.showMessage('error', 'Sync Failed', 'Failed to synchronize data from Symfony API')
      } finally {
        this.syncing = false
      }
    },
    loadLastSyncDate() {
      const savedDate = localStorage.getItem('lastSyncDate')
      if (savedDate) {
        this.lastSyncDate = new Date(savedDate)
      }
    },
    saveLastSyncDate() {
      if (this.lastSyncDate) {
        localStorage.setItem('lastSyncDate', this.lastSyncDate.toISOString())
      }
    },
    formatSyncDate(date) {
      if (!date) return 'Never'
      const now = new Date()
      const syncDate = new Date(date)
      const diffMs = now - syncDate
      const diffMins = Math.floor(diffMs / 60000)
      const diffHours = Math.floor(diffMs / 3600000)
      const diffDays = Math.floor(diffMs / 86400000)
      if (diffMins < 1) return 'Just now'
      if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`
      if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
      if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
      return syncDate.toLocaleDateString()
    },
    formatSyncTime(date) {
      if (!date) return ''
      return new Date(date).toLocaleTimeString()
    },
    async loadStats() {
      this.loadingStats = true
      try {
        const [ordersData, employeesData, planningData] = await Promise.all([
          authService.get('/api/orders?page=0&size=1'),
          authService.get('/api/employees'),
          authService.get('/api/planning/assignments')
        ])
        this.stats = {
          ordersCount: ordersData.totalElements || 0,
          employeesCount: Array.isArray(employeesData) ? employeesData.length : (employeesData.content?.length || 0),
          planningCount: Array.isArray(planningData) ? planningData.length : 0
        }
      } catch (error) {
        console.error('Error loading stats:', error)
      } finally {
        this.loadingStats = false
      }
    },
    async loadEmployees() {
      try {
        const data = await authService.get('/api/employees')

        if (Array.isArray(data)) {
          this.employees = data
        } else if (data && Array.isArray(data.content)) {
          this.employees = data.content
        } else {
          this.employees = []
        }
      } catch (error) {
        console.error('Error loading employees:', error)
        this.employees = []
      }
    },
    async refreshStats() {
      await this.loadStats()
      this.showMessage('success', 'Updated', 'Statistics refreshed successfully')
    },
    async generatePlanning() {
      this.generatingPlanning = true
      try {
        const response = await authService.get('/api/planning/generate', {
          method: 'POST'
        })
        if (response.ok) {
          this.showMessage('success', 'Success', 'Planning generated successfully')
          await this.loadStats()
        } else {
          throw new Error('Failed to generate planning')
        }
      } catch (error) {
        this.showMessage('error', 'Error', 'Failed to generate planning')
      } finally {
        this.generatingPlanning = false
      }
    },
    showMessage(type, title, message) {
      this.systemMessage = { type, title, message }
      if (type === 'success') {
        setTimeout(() => {
          this.systemMessage = null
        }, 5000)
      }
    }
  }
}
</script>

<style scoped>
.pokemon-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #374151 0%, #6B7280 100%);
  padding: 2rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.hero-section {
  position: relative;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 3rem;
  margin-bottom: 2rem;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  border: 2px solid #730d10;
}

.hero-decoration {
  position: relative;
}

.floating-pokemon {
  position: absolute;
  animation: float 6s ease-in-out infinite;
  opacity: 0.7;
}

.pokeball-1 {
  top: 20%;
  right: 15%;
  animation-delay: 0s;
}

.pokeball-2 {
  top: 60%;
  right: 25%;
  animation-delay: 2s;
}

.energy-symbol {
  top: 40%;
  right: 10%;
  animation-delay: 4s;
  animation-duration: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(10deg);
  }
}

.hero-icon svg {
  color: #730d10;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.hero-decoration {
  position: absolute;
  top: -50%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(115, 13, 16, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
  color: #730d10;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #4B5563 0%, #730d10 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 0.5rem 0;
}

.hero-subtitle {
  font-size: 1.125rem;
  color: #6b7280;
  margin: 0 0 1.5rem 0;
}

.hero-timestamp {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #f3f4f6;
  padding: 0.5rem 1rem;
  border-radius: 100px;
  font-size: 0.875rem;
  color: #4b5563;
}

.timestamp-icon {
  animation: rotate 2s linear infinite;
  color: #730d10;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-item {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid #E5E7EB;
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-color: #730d10;
}

.stat-icon {
  color: #730d10;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.content-left {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 1.5rem;
}

.content-right {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 1.5rem;
}

.card-full-height {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-full-height .card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: auto;
}

.card-full-height .activity-list {
  flex: 1;
}

.card-stretch {
  display: flex;
  flex-direction: column;
}

.card-stretch .card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #E5E7EB;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-color: #730d10;
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  color: #730d10;
}

.card-body {
  padding: 1.5rem;
}

.status-badge {
  padding: 0.375rem 0.75rem;
  border-radius: 100px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-online {
  background: #F3F4F6;
  color: #730d10;
}

.status-offline {
  background: #F3F4F6;
  color: #6B7280;
}

.connection-status {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #6B7280;
  position: relative;
}

.status-indicator.active {
  background: #730d10;
}

.status-indicator.active::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid #730d10;
  animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes ping {
  75%, 100% {
    transform: scale(2);
    opacity: 0;
  }
}

.status-info {
  flex: 1;
}

.status-title {
  font-weight: 600;
  color: #1f2937;
}

.status-desc {
  font-size: 0.875rem;
  color: #6b7280;
}

.sync-info {
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 8px;
  margin-top: 1rem;
}

.sync-label {
  font-size: 0.75rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.sync-date {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1f2937;
}

.sync-time {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.125rem;
}

.sync-never {
  font-size: 0.875rem;
  color: #730d10;
  font-style: italic;
}

.symfony-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.symfony-actions .btn {
  flex: 1;
}

.btn-sync {
  background: linear-gradient(135deg, #730d10 0%, #8a1c20 100%);
  color: white;
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-sync:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(115, 13, 16, 0.3);
  transform: translateY(-1px);
}

.btn-sync:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.priority-chart {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.priority-bar-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.priority-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.priority-name {
  font-weight: 600;
  color: #1f2937;
}

.priority-count {
  font-weight: 700;
  color: #6b7280;
}

.priority-bar-bg {
  height: 8px;
  background: #f3f4f6;
  border-radius: 100px;
  overflow: hidden;
}

.priority-bar {
  height: 100%;
  border-radius: 100px;
  transition: width 0.5s ease;
}

.priority-urgent { background: linear-gradient(90deg, #730d10, #8a1c20); }
.priority-high { background: linear-gradient(90deg, #9CA3AF, #6B7280); }
.priority-medium { background: linear-gradient(90deg, #D1D5DB, #9CA3AF); }
.priority-low { background: linear-gradient(90deg, #E5E7EB, #D1D5DB); }

.action-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 2px solid transparent;
  border-radius: 12px;
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  position: relative;
  color: #1f2937; /* Default text color */
}

.action-btn:hover:not(:disabled) {
  transform: translateX(4px);
  border-color: #730d10;
  background: #f3f4f6;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.action-btn:disabled:hover {
  transform: none !important;
  border-color: transparent !important;
  background: #f9fafb !important;
}

.action-btn.primary {
  background: linear-gradient(135deg, #730d10 0%, #8a1c20 100%);
  color: white;
}

.action-btn.primary:hover:not(:disabled) {
  border-color: transparent;
  box-shadow: 0 8px 16px rgba(115, 13, 16, 0.3);
  transform: translateX(4px) scale(1.02);
  background: linear-gradient(135deg, #730d10 0%, #8a1c20 100%); /* Keep gradient on hover */
  color: white; /* Force white text on hover */
}

.action-btn.primary:disabled {
  background: linear-gradient(135deg, #9CA3AF 0%, #6B7280 100%);
}

.action-icon {
  color: inherit;
}

.action-text {
  flex: 1;
}

.action-title {
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.action-desc {
  font-size: 0.875rem;
  opacity: 0.8;
}

.action-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 12px;
  transition: background 0.2s;
}

.activity-item:hover {
  background: #f3f4f6;
}

.activity-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #730d10 0%, #8a1c20 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.125rem;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-weight: 600;
  color: #1f2937;
}

.activity-meta {
  font-size: 0.875rem;
  color: #6b7280;
}

.activity-badge {
  padding: 0.25rem 0.75rem;
  background: #e5e7eb;
  border-radius: 100px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #4b5563;
}

.activity-more {
  text-align: center;
  padding: 0.75rem;
  color: #6b7280;
  font-size: 0.875rem;
  font-style: italic;
}

.nav-section {
  margin-bottom: 2rem;
}

.section-title {
  color: white;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.nav-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid #E5E7EB;
}

.nav-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
  border-color: #730d10;
}

.nav-icon {
  color: #730d10;
}

.nav-content {
  flex: 1;
}

.nav-title {
  font-weight: 700;
  font-size: 1.125rem;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.nav-count {
  font-size: 0.875rem;
  color: #6b7280;
}

.nav-arrow {
  color: #9ca3af;
  transition: transform 0.2s;
}

.nav-card:hover .nav-arrow {
  transform: translateX(4px);
  color: #730d10;
}

.btn {
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  border: 2px solid #730d10;
  color: #730d10;
}

.btn-outline:hover:not(:disabled) {
  background: #730d10;
  color: white;
}

.toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  z-index: 1000;
  border-left: 4px solid #730d10;
}

.toast-success {
  border-left: 4px solid #10b981;
}

.toast-error {
  border-left: 4px solid #730d10;
}

.toast-icon {
  color: #10b981;
}

.toast-error .toast-icon {
  color: #730d10;
}

.toast-content {
  flex: 1;
}

.toast-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.toast-message {
  font-size: 0.875rem;
  color: #6b7280;
}

.toast-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.toast-close:hover {
  color: #4b5563;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

.text-success {
  color: #10b981;
}

.text-muted {
  color: #6b7280;
}

@media (max-width: 768px) {
  .pokemon-dashboard {
    padding: 1rem;
  }
  .hero-section {
    padding: 2rem 1.5rem;
  }
  .hero-title {
    font-size: 1.75rem;
  }
  .quick-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .content-wrapper {
    grid-template-columns: 1fr;
  }
  .nav-grid {
    grid-template-columns: 1fr;
  }
  .toast {
    left: 1rem;
    right: 1rem;
    bottom: 1rem;
  }
}
</style>
