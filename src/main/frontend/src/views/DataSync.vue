<template>
  <div class="data-sync-container">
    <!-- Header -->
    <div class="sync-header">
      <h2>
        <svg class="header-icon" width="24" height="24" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
        </svg>
        Database Synchronization
      </h2>
      <p class="subtitle">Sync data between Symfony (dev) and Spring Boot (dev-planning)</p>
    </div>

    <!-- Real-time Progress (only shown during sync) -->
    <div v-if="syncing" class="card progress-card">
      <div class="progress-content">
        <div class="progress-header">
          <span class="progress-operation">{{ currentOperation }}</span>
          <span class="progress-percentage" style="color: #730d10;">{{ Math.round(progress) }}%</span>
        </div>

        <div class="progress-bar-container">
          <div class="progress-bar-fill" :style="{ width: progress + '%' }"></div>
        </div>

        <div class="progress-details">
          <div class="detail-row">
            <span class="detail-label">Status:</span>
            <span :class="['detail-value', phaseClass]">{{ progressMessage }}</span>
          </div>
          <div class="detail-row" v-if="totalItems > 0">
            <span class="detail-label">Progress:</span>
            <span class="detail-value">{{ itemsProcessed.toLocaleString() }} / {{ totalItems.toLocaleString() }} items</span>
          </div>
          <div class="detail-row" v-if="estimatedSecondsRemaining > 0">
            <span class="detail-label">Time remaining:</span>
            <span class="detail-value">~{{ Math.ceil(estimatedSecondsRemaining) }}s</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Status Check -->
    <div class="card status-card">
      <h3>
        <svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
          <path d="M9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4zm2.5 2.1h-15V5h15v14.1zm0-16.1h-15c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
        </svg>
        Current Synchronization Status
      </h3>
      <p class="text-muted">Compare data between Symfony (dev) and Spring Boot (dev-planning)</p>

      <button
        @click="checkSyncStatus"
        :disabled="loadingStatus"
        class="btn btn-secondary"
      >
        <svg v-if="loadingStatus" class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
          <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
          <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
          <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
        </svg>
        {{ loadingStatus ? 'Checking...' : 'Check Status' }}
      </button>

      <div v-if="syncStatus" class="status-table">
        <table>
          <thead>
          <tr>
            <th>Table</th>
            <th>Dev (Symfony)</th>
            <th>Dev-Planning</th>
            <th>Difference</th>
            <th>Status</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="table in syncStatus.tableComparison" :key="table.table">
            <td><strong>{{ table.table }}</strong></td>
            <td>{{ table.symfony?.toLocaleString() || 0 }}</td>
            <td>{{ table.local?.toLocaleString() || 0 }}</td>
            <td :class="{ 'text-danger': table.difference !== 0 }">
              {{ table.difference }}
            </td>
            <td>
              <span v-if="table.inSync" class="badge badge-success">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                </svg>
                In Sync
              </span>
              <span v-else class="badge badge-warning">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
                </svg>
                Out of Sync
              </span>
            </td>
          </tr>
          </tbody>
        </table>
        <p class="text-muted mt-2">Last checked: {{ formatTime(syncStatus.timestamp) }}</p>
      </div>
    </div>

    <!-- Sync Actions -->
    <div class="sync-actions">
      <div class="card action-card">
        <h3>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
          Full Synchronization
        </h3>
        <p>Sync all tables (orders, cards, translations, certifications)</p>
        <button
          @click="syncAll"
          :disabled="syncing"
          class="btn btn-primary"
        >
          <svg v-if="syncing" class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
            <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
          </svg>
          {{ syncing ? 'Syncing...' : 'Sync All Data' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
            <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
          </svg>
          Sync Orders Only
        </h3>
        <p>Sync only the orders table</p>
        <button
          @click="syncOrders"
          :disabled="syncing"
          class="btn btn-info"
        >
          <svg v-if="syncing" class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
            <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
          </svg>
          {{ syncing ? 'Syncing...' : 'Sync Orders' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
          </svg>
          Sync Cards Only
        </h3>
        <p>Sync cards, translations, and certifications</p>
        <button
          @click="syncCards"
          :disabled="syncing"
          class="btn btn-info"
        >
          <svg v-if="syncing" class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
            <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
          </svg>
          {{ syncing ? 'Syncing...' : 'Sync Cards' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
            <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9z"/>
          </svg>
          Incremental Sync
        </h3>
        <p>Sync only recent changes (last 24 hours)</p>
        <button
          @click="syncIncremental"
          :disabled="syncing"
          class="btn btn-success"
        >
          <svg v-if="syncing" class="spinner" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
            <path d="M20 12a8 8 0 0 1-8 8V4a8 8 0 0 1 8 8z"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9z"/>
          </svg>
          {{ syncing ? 'Syncing...' : 'Quick Sync' }}
        </button>
      </div>
    </div>

    <!-- Sync History -->
    <div v-if="syncHistory.length > 0" class="card history-card">
      <h3>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
        </svg>
        Sync History
      </h3>
      <div class="history-list">
        <div
          v-for="(entry, index) in syncHistory"
          :key="index"
          :class="['history-entry', entry.success ? 'success' : 'error']"
        >
          <span class="time">{{ formatTime(entry.timestamp) }}</span>
          <span class="message">{{ entry.message }}</span>
          <span v-if="entry.duration" class="duration">({{ formatDuration(entry.duration) }})</span>
          <span v-if="entry.success" class="badge badge-success">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
            Success
          </span>
          <span v-else class="badge badge-danger">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
            Failed
          </span>
        </div>
      </div>
    </div>

    <!-- Notifications -->
    <div v-if="notification" :class="['notification', notification.type]">
      <svg v-if="notification.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
      </svg>
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import authService from '@/services/authService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// State
const syncing = ref(false)
const loadingStatus = ref(false)
const syncStatus = ref<any>(null)
const syncHistory = ref<any[]>([])
const notification = ref<any>(null)

// Real-time progress state
const progress = ref(0)
const currentOperation = ref('')
const progressMessage = ref('')
const phase = ref('')
const itemsProcessed = ref(0)
const totalItems = ref(0)
const estimatedSecondsRemaining = ref(0)
const currentSyncId = ref('')

// SSE connection
let eventSource: EventSource | null = null

// Computed
const phaseClass = computed(() => {
  const phaseMap: Record<string, string> = {
    'STARTING': 'phase-starting',
    'FETCHING': 'phase-fetching',
    'PROCESSING': 'phase-processing',
    'SAVING': 'phase-saving',
    'COMPLETED': 'phase-completed',
    'ERROR': 'phase-error'
  }
  return phaseMap[phase.value] || 'phase-default'
})

// Format time
const formatTime = (timestamp: any) => {
  return new Date(timestamp).toLocaleString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Format duration
const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return mins > 0 ? `${mins}m ${secs}s` : `${secs}s`
}

// Show notification
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}

// Add to history
const addToHistory = (message: string, success: boolean, duration?: number) => {
  syncHistory.value.unshift({
    message,
    success,
    duration,
    timestamp: new Date().toISOString()
  })
  if (syncHistory.value.length > 10) {
    syncHistory.value.pop()
  }
}

// Close SSE connection
const closeSSEConnection = () => {
  if (eventSource) {
    console.log(' Closing SSE connection')
    eventSource.close()
    eventSource = null
  }
}

// Connect to SSE for real-time progress
const connectSSE = (syncId: string) => {
  closeSSEConnection()

  currentSyncId.value = syncId

  //  Add JWT token to URL (SSE doesn't support headers)
  const token = localStorage.getItem('jwt_token')
  const url = `${API_BASE_URL}/api/sync/progress/stream/${syncId}?token=${token}`

  console.log(' Connecting to SSE:', url)

  eventSource = new EventSource(url)

  eventSource.addEventListener('progress', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data)
      console.log(' Progress update:', data)

      progress.value = data.percentage || 0
      currentOperation.value = data.currentOperation || ''
      progressMessage.value = data.message || ''
      phase.value = data.phase || ''
      itemsProcessed.value = data.itemsProcessed || 0
      totalItems.value = data.totalItems || 0
      estimatedSecondsRemaining.value = data.estimatedSecondsRemaining || 0

      if (data.completed) {
        if (data.error) {
          showNotification(` ${data.errorMessage || 'Sync failed'}`, 'error')
        } else {
          showNotification(' Synchronization completed', 'success')
        }

        setTimeout(() => {
          closeSSEConnection()
          syncing.value = false
          progress.value = 100
          checkSyncStatus()
        }, 2000)
      }
    } catch (error) {
      console.error('Error parsing SSE:', error)
    }
  })

  eventSource.onerror = (error) => {
    console.error(' SSE error:', error)
    if (eventSource?.readyState === EventSource.CLOSED) {
      if (syncing.value && progress.value < 100) {
        showNotification('Connection lost', 'error')
        syncing.value = false
      }
    }
  }

  eventSource.onopen = () => {
    console.log(' SSE connected')
  }
}

// Generic sync function
const performSync = async (endpoint: string, operationName: string) => {
  if (syncing.value) {
    console.warn(' Sync already in progress')
    return
  }

  syncing.value = true
  progress.value = 0
  currentOperation.value = operationName
  progressMessage.value = 'Initializing...'
  phase.value = 'STARTING'
  itemsProcessed.value = 0
  totalItems.value = 0
  estimatedSecondsRemaining.value = 0

  const syncId = `sync-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  connectSSE(syncId)
  const startTime = Date.now()

  try {
    //  Use authService for authenticated requests
    const result = await authService.post(`/api/sync/${endpoint}?syncId=${syncId}`)

    const duration = Math.floor((Date.now() - startTime) / 1000)
    addToHistory(`${operationName} completed`, true, duration)
    showNotification(` ${operationName} completed`, 'success')

    progress.value = 100
    progressMessage.value = 'Completed!'

    setTimeout(() => {
      closeSSEConnection()
      syncing.value = false
      checkSyncStatus()
    }, 2000)

  } catch (error: any) {
    console.error('Sync error:', error)
    showNotification(` ${operationName} failed: ${error.message}`, 'error')
    addToHistory(`${operationName} failed`, false)
    closeSSEConnection()
    syncing.value = false
  }
}

// Check sync status
const checkSyncStatus = async () => {
  loadingStatus.value = true

  try {
    //  Use authService for authenticated requests
    const data = await authService.get('/api/sync/status')

    syncStatus.value = data
    showNotification('Status checked successfully', 'success')

  } catch (error: any) {
    console.error('Error checking sync status:', error)
    showNotification(`Failed to check sync status: ${error.message}`, 'error')
  } finally {
    loadingStatus.value = false
  }
}

// Sync all data
const syncAll = () => performSync('all', 'Full Synchronization')

// Sync orders only
const syncOrders = () => performSync('orders', 'Order Synchronization')

// Sync cards
const syncCards = () => performSync('cards', 'Card Synchronization')

// Full sync
const syncFull = () => performSync('full', 'Complete Data Sync')

// Incremental sync
const syncIncremental = () => performSync('incremental', 'Incremental Sync')

// Lifecycle
onMounted(async () => {
  console.log(' DataSync mounted')
  setTimeout(async () => {
    await checkSyncStatus()
  }, 100)
})

onUnmounted(() => {
  console.log(' DataSync unmounting')
  closeSSEConnection()
})
</script>

<style scoped>
.data-sync-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.sync-header {
  margin-bottom: 2rem;
}

.sync-header h2 {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  color: #730d10;
}

.subtitle {
  color: #6b7280;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

/* Progress Card */
.progress-card {
  border-left: 4px solid #730d10;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-operation {
  font-weight: 600;
  color: #374151;
  font-size: 1.1rem;
}

.progress-percentage {
  font-weight: 700;
  color: #730d10;
  font-size: 1.5rem;
}

.progress-bar-container {
  width: 100%;
  height: 16px;
  background: #e5e7eb;
  border-radius: 9999px;
  overflow: hidden;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #730d10, #5a0a0c);
  border-radius: 9999px;
  transition: width 0.5s ease-out;
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid #e5e7eb;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.detail-label {
  color: #6b7280;
  font-weight: 500;
}

.detail-value {
  color: #1f2937;
  font-weight: 600;
}

/* Phase classes */
.phase-starting {
  color: #730d10;
}

.phase-fetching {
  color: #8a4a4d;
}

.phase-processing {
  color: #a1686a;
}

.phase-saving {
  color: #b88687;
}

.phase-completed {
  color: #059669;
}

.phase-error {
  color: #ef4444;
}

/* Status Card */
.status-card h3 {
  margin-bottom: 0.5rem;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.text-muted {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.status-table {
  margin-top: 1.5rem;
}

.status-table table {
  width: 100%;
  border-collapse: collapse;
}

.status-table th {
  background: #f9fafb;
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 2px solid #e5e7eb;
}

.status-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}

.text-danger {
  color: #ef4444;
  font-weight: 600;
}

/* Badges */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-success {
  background: #d1fae5;
  color: #065f46;
}

.badge-warning {
  background: #fef3c7;
  color: #92400e;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #730d10;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5a0a0c;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-info {
  background: #8a4a4d;
  color: white;
}

.btn-info:hover:not(:disabled) {
  background: #730d10;
}

.btn-success {
  background: #a1686a;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #8a4a4d;
}

/* Spinner animation */
.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Sync Actions */
.sync-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.action-card {
  text-align: center;
}

.action-card h3 {
  font-size: 1.125rem;
  margin-bottom: 0.5rem;
  color: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.action-card p {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

/* History */
.history-card h3 {
  margin-bottom: 1rem;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.history-entry {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
}

.history-entry.success {
  background: #f0fdf4;
  border-left: 3px solid #10b981;
}

.history-entry.error {
  background: #fef2f2;
  border-left: 3px solid #ef4444;
}

.history-entry .time {
  color: #6b7280;
  font-size: 0.75rem;
  white-space: nowrap;
}

.history-entry .message {
  flex: 1;
  color: #1f2937;
  font-weight: 500;
}

.history-entry .duration {
  color: #6b7280;
  font-size: 0.75rem;
}

/* Notifications */
.notification {
  position: fixed;
  top: 1rem;
  right: 1rem;
  padding: 1rem 1.5rem;
  border-radius: 6px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  font-weight: 500;
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification.success {
  background: #d1fae5;
  color: #065f46;
  border-left: 4px solid #10b981;
}

.notification.error {
  background: #fee2e2;
  color: #991b1b;
  border-left: 4px solid #ef4444;
}

.mt-2 {
  margin-top: 0.5rem;
}

.icon {
  color: #730d10;
}
</style>