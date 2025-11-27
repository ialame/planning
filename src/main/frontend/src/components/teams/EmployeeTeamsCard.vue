<template>
  <div class="employee-teams-card card">
    <!-- Header -->
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center space-x-3">
        <Shield class="w-6 h-6 text-blue-600" />
        <div>
          <h3 class="font-semibold text-gray-900">Teams & Roles</h3>
          <p class="text-sm text-gray-600">{{ employee.fullName }}</p>
        </div>
      </div>
      <button
        @click="openTeamManagement"
        class="text-blue-600 hover:text-blue-800 text-sm font-medium"
      >
        Manage Teams →
      </button>
    </div>

    <!-- Current Teams -->
    <div v-if="employeeTeams.length > 0" class="space-y-3 mb-4">
      <div
        v-for="team in employeeTeams"
        :key="team.id"
        class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
      >
        <div class="flex items-center space-x-3">
          <div :class="[
            'w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm',
            getPermissionLevelColor(team.permissionLevel)
          ]">
            {{ getTeamIcon(team.permissionLevel) }}
          </div>
          <div>
            <p class="font-medium text-gray-900">{{ team.name }}</p>
            <p class="text-sm text-gray-600">{{ team.description }}</p>
            <span :class="[
              'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium mt-1',
              getPermissionBadgeColor(team.permissionLevel)
            ]">
              <Shield class="w-3 h-3 mr-1" />
              Level {{ team.permissionLevel }}
            </span>
          </div>
        </div>
        <button
          @click="removeFromTeam(team)"
          class="text-red-600 hover:text-red-800 p-1"
          :disabled="loading"
        >
          <UserMinus class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-6 bg-gray-50 rounded-lg mb-4">
      <Users class="w-12 h-12 text-gray-400 mx-auto mb-2" />
      <p class="text-gray-500 mb-2">No teams assigned</p>
      <p class="text-sm text-gray-400">This employee has no role assignments</p>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
      <div class="text-center">
        <p class="text-lg font-bold text-gray-900">{{ employeeTeams.length }}</p>
        <p class="text-xs text-gray-600">Teams</p>
      </div>
      <div class="text-center">
        <p class="text-lg font-bold text-gray-900">{{ highestPermissionLevel }}</p>
        <p class="text-xs text-gray-600">Max Level</p>
      </div>
      <div class="text-center">
        <p :class="[
          'text-lg font-bold',
          isAdmin ? 'text-red-600' : isManager ? 'text-yellow-600' : 'text-blue-600'
        ]">
          {{ primaryRole }}
        </p>
        <p class="text-xs text-gray-600">Primary Role</p>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="mt-4 pt-4 border-t border-gray-200">
      <div class="flex space-x-2">
        <button
          @click="addToTeam('PROCESSOR')"
          :disabled="loading || hasTeam('PROCESSOR')"
          class="flex-1 text-sm bg-blue-50 text-blue-600 px-3 py-2 rounded hover:bg-blue-100 disabled:opacity-50"
        >
          + Processor
        </button>
        <button
          @click="addToTeam('SUPERVISOR')"
          :disabled="loading || hasTeam('SUPERVISOR')"
          class="flex-1 text-sm bg-yellow-50 text-yellow-600 px-3 py-2 rounded hover:bg-yellow-100 disabled:opacity-50"
        >
          + Supervisor
        </button>
        <button
          @click="addToTeam('MANAGER')"
          :disabled="loading || hasTeam('MANAGER')"
          class="flex-1 text-sm bg-red-50 text-red-600 px-3 py-2 rounded hover:bg-red-100 disabled:opacity-50"
        >
          + Manager
        </button>
      </div>
    </div>

    <!-- Employee Team Management Modal -->
    <EmployeeTeamManagementModal
      v-if="showTeamManagement"
      :employee="employee"
      :current-teams="employeeTeams"
      @close="showTeamManagement = false"
      @updated="onTeamsUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Shield, Users, UserMinus } from 'lucide-vue-next'
import EmployeeTeamManagementModal from './EmployeeTeamManagementModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface Employee {
  id: string
  firstName: string
  lastName: string
  fullName: string
  email: string
  active: boolean
  workHoursPerDay: number
  efficiencyRating: number
}

interface Team {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
}

// ========== PROPS & EMITS ==========
const props = defineProps<{
  employee: Employee
}>()

const emit = defineEmits<{
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const employeeTeams = ref<Team[]>([])
const showTeamManagement = ref(false)

// ========== COMPUTED ==========
const highestPermissionLevel = computed(() => {
  if (employeeTeams.value.length === 0) return 0
  return Math.max(...employeeTeams.value.map(g => g.permissionLevel))
})

const isAdmin = computed(() => {
  return employeeTeams.value.some(g => g.permissionLevel >= 8)
})

const isManager = computed(() => {
  return employeeTeams.value.some(g => g.permissionLevel >= 5 && g.permissionLevel < 8)
})

const primaryRole = computed(() => {
  const level = highestPermissionLevel.value
  if (level >= 8) return 'Admin'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  if (level >= 1) return 'Viewer'
  return 'No Role'
})

// ========== METHODS ==========
const loadEmployeeTeams = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/employee/${props.employee.id}`)
    if (response.ok) {
      const data = await response.json()
      employeeTeams.value = data.teams || []
    }
  } catch (error) {
    console.error('Error loading employee teams:', error)
  } finally {
    loading.value = false
  }
}

const addToTeam = async (teamName: string) => {
  try {
    // First find the team by name
    const teamsResponse = await fetch(`${API_BASE_URL}/api/v2/teams?search=${teamName}`)
    if (!teamsResponse.ok) return

    const teamsData = await teamsResponse.json()
    const team = teamsData.teams?.find((g: Team) => g.name === teamName)

    if (!team) {
      console.error(`Team ${teamName} not found`)
      return
    }

    // Add employee to team
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`, {
      method: 'POST'
    })

    if (response.ok) {
      await loadEmployeeTeams()
      emit('updated')
    }
  } catch (error) {
    console.error('Error adding to team:', error)
  }
}

const removeFromTeam = async (team: Team) => {
  if (!confirm(`Remove ${props.employee.fullName} from ${team.name}?`)) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      await loadEmployeeTeams()
      emit('updated')
    }
  } catch (error) {
    console.error('Error removing from team:', error)
  }
}

const hasTeam = (teamName: string) => {
  return employeeTeams.value.some(g => g.name === teamName)
}

const openTeamManagement = () => {
  showTeamManagement.value = true
}

const onTeamsUpdated = () => {
  showTeamManagement.value = false
  loadEmployeeTeams()
  emit('updated')
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-red-500'
  if (level >= 5) return 'bg-yellow-500'
  if (level >= 3) return 'bg-blue-500'
  return 'bg-gray-500'
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-red-100 text-red-800'
  if (level >= 5) return 'bg-yellow-100 text-yellow-800'
  if (level >= 3) return 'bg-blue-100 text-blue-800'
  return 'bg-gray-100 text-gray-800'
}

const getTeamIcon = (level: number) => {
  if (level >= 8) {
    return `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
      <path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3H5v2h14v-2z"/>
    </svg>`
  }
  if (level >= 5) {
    return `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
    </svg>`
  }
  if (level >= 3) {
    return `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
      <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
    </svg>`
  }
  return `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
  </svg>`
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployeeTeams()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}
</style>
