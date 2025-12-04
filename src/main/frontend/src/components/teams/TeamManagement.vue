<template>
  <div class="team-management-page">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <svg class="w-8 h-8 text-burgundy" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
            Team Management
          </h1>
          <p class="text-gray-600 mt-1">Manage roles and permissions for your team</p>
        </div>
        <div class="flex space-x-3">
          <button
            @click="initializeDefaultTeams"
            class="btn-secondary"
            :disabled="loading"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            Initialize Default Teams
          </button>
          <button
            @click="showCreateForm = true"
            class="btn-primary"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            Create Team
          </button>
        </div>
      </div>
    </div>

    <!-- Notification -->
    <div v-if="notification" class="mb-6">
      <div :class="[
        'p-4 rounded-lg border',
        notification.type === 'success' ? 'bg-burgundy-50 border-burgundy-200 text-burgundy-800' :
        notification.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' :
        'bg-burgundy-50 border-burgundy-200 text-burgundy-800'
      ]">
        <div class="flex items-center">
          <svg v-if="notification.type === 'success'" class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <svg v-else-if="notification.type === 'error'" class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <span>{{ notification.message }}</span>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      <div class="card">
        <div class="flex items-center">
          <svg class="w-8 h-8 text-burgundy mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
          <div>
            <p class="text-sm text-gray-600">Total Teams</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalTeams }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <svg class="w-8 h-8 text-burgundy mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
          </svg>
          <div>
            <p class="text-sm text-gray-600">Admin Teams</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.adminTeams }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <svg class="w-8 h-8 text-burgundy-light mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
          <div>
            <p class="text-sm text-gray-600">Manager Teams</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.managerTeams }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <svg class="w-8 h-8 text-burgundy-lighter mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"/>
          </svg>
          <div>
            <p class="text-sm text-gray-600">Total Members</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalMembers }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="card mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <div class="relative">
            <svg class="w-5 h-5 absolute left-3 top-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Search teams..."
              class="input-field pl-10"
            >
          </div>
        </div>
        <div class="flex gap-2">
          <select
            v-model="permissionFilter"
            class="input-field min-w-[200px]"
          >
            <option :value="null">All Permission Levels</option>
            <option v-for="level in permissionLevels" :key="level.level" :value="level.level">
              {{ level.name }} ({{ level.level }})
            </option>
          </select>
          <button
            @click="clearFilters"
            class="btn-secondary"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
            Clear
          </button>
        </div>
      </div>
    </div>

    <!-- Teams Grid -->
    <div v-if="!loading && filteredTeams.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="team in filteredTeams"
        :key="team.id"
        class="card hover:shadow-lg transition-shadow cursor-pointer"
        @click="selectTeam(team)"
      >
        <!-- Team Header -->
        <div class="flex justify-between items-start mb-4">
          <div class="flex items-center space-x-3">
            <div :class="[
              'w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg',
              getPermissionLevelColor(team.permissionLevel)
            ]">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">{{ team.name }}</h3>
              <p class="text-sm text-gray-600 truncate">{{ team.description }}</p>
            </div>
          </div>
          <div class="flex space-x-1">
            <button
              @click.stop="editTeam(team)"
              class="p-1 text-gray-400 hover:text-burgundy"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
            <button
              @click.stop="deleteTeam(team.id)"
              class="p-1 text-gray-400 hover:text-red-600"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Permission Level Badge -->
        <div class="mb-3">
          <span :class="[
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getPermissionBadgeColor(team.permissionLevel)
          ]">
            <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
            Level {{ team.permissionLevel }} - {{ getPermissionInfo(team.permissionLevel).name }}
          </span>
        </div>

        <!-- Employee Count -->
        <div class="flex justify-between items-center mb-4">
          <span class="text-sm text-gray-600">Members:</span>
          <span class="font-medium text-gray-900">{{ team.employeeCount }}</span>
        </div>

        <!-- Status -->
        <div class="flex justify-between items-center">
          <span :class="[
            'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
            team.active ? 'bg-burgundy-100 text-burgundy-800' : 'bg-gray-100 text-gray-800'
          ]">
            <svg v-if="team.active" class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            <svg v-else class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
            {{ team.active ? 'Active' : 'Inactive' }}
          </span>
          <button
            @click.stop="manageTeamEmployees(team)"
            class="text-burgundy hover:text-burgundy-800 text-sm font-medium flex items-center gap-1"
          >
            Manage Members
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && filteredTeams.length === 0" class="card text-center py-12">
      <svg class="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">No teams found</h3>
      <p class="text-gray-600 mb-4">
        {{ searchTerm || permissionFilter ? 'Try adjusting your filters' : 'Create your first team to get started' }}
      </p>
      <button
        v-if="!searchTerm && !permissionFilter"
        @click="showCreateForm = true"
        class="btn-primary"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Create First Team
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="card text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-burgundy mx-auto mb-4"></div>
      <p class="text-gray-600">Loading teams...</p>
    </div>

    <!-- Create Team Modal -->
    <CreateTeamModal
      v-if="showCreateForm"
      @close="showCreateForm = false"
      @created="onTeamCreated"
      :permission-levels="permissionLevels"
    />

    <!-- Team Detail Modal -->
    <TeamDetailModal
      v-if="selectedTeam"
      :team="selectedTeam"
      @close="selectedTeam = null"
      @updated="onTeamUpdated"
      @deleted="onTeamDeleted"
    />

    <!-- Employee Assignment Modal -->
    <EmployeeAssignmentModal
      v-if="showEmployeeAssignment"
      :team="selectedTeamForAssignment"
      :employees="employees"
      @close="showEmployeeAssignment = false"
      @updated="onAssignmentUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import CreateTeamModal from './CreateTeamModal.vue'
import TeamDetailModal from './TeamDetailModal.vue'
import EmployeeAssignmentModal from './EmployeeAssignmentModal.vue'

import { API_BASE_URL } from '@/config/api.ts'
import authService from "@/services/authService.ts";

// ========== INTERFACES ==========
interface Team {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount: number
  creationDate: string
  modificationDate: string
}

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

interface PermissionLevelInfo {
  level: number
  name: string
  description: string
  colorCode: string
}

interface Notification {
  message: string
  type: 'success' | 'error' | 'info'
}

// ========== STATE ==========
const teams = ref<Team[]>([])
const employees = ref<Employee[]>([])
const selectedTeam = ref<Team | null>(null)
const selectedTeamForAssignment = ref<Team | null>(null)
const loading = ref(false)
const searchTerm = ref('')
const permissionFilter = ref<number | null>(null)
const showCreateForm = ref(false)
const showEmployeeAssignment = ref(false)
const notification = ref<Notification | null>(null)

// ========== PERMISSION LEVELS ==========
const permissionLevels: PermissionLevelInfo[] = [
  { level: 10, name: 'SUPER_ADMIN', description: 'Super administrator with unrestricted access', colorCode: '#5a0a0d' },
  { level: 9, name: 'ADMIN', description: 'System administrator with full management rights', colorCode: '#730d10' },
  { level: 8, name: 'SENIOR_ADMIN', description: 'Senior administrator with most privileges', colorCode: '#8a3e41' },
  { level: 7, name: 'MANAGER', description: 'Team manager with planning and oversight privileges', colorCode: '#a26f71' },
  { level: 6, name: 'SENIOR_SUPERVISOR', description: 'Senior supervisor with extended monitoring rights', colorCode: '#bc0004' },
  { level: 5, name: 'SUPERVISOR', description: 'Supervisor with monitoring and basic management', colorCode: '#d10004' },
  { level: 4, name: 'SENIOR_PROCESSOR', description: 'Senior processor with advanced processing rights', colorCode: '#f5bfc0' },
  { level: 3, name: 'PROCESSOR', description: 'Card processor with standard processing access', colorCode: '#fae6e6' },
  { level: 2, name: 'JUNIOR_PROCESSOR', description: 'Junior processor with limited processing access', colorCode: '#fdf2f2' },
  { level: 1, name: 'VIEWER', description: 'Read-only access for viewing and reports', colorCode: '#fef7f7' }
]

// ========== COMPUTED ==========
const stats = computed(() => ({
  totalTeams: teams.value.length,
  adminTeams: teams.value.filter(g => g.permissionLevel >= 8).length,
  managerTeams: teams.value.filter(g => g.permissionLevel >= 5 && g.permissionLevel < 8).length,
  totalMembers: teams.value.reduce((sum, g) => sum + g.employeeCount, 0)
}))

const filteredTeams = computed(() => {
  let filtered = teams.value

  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(team =>
      team.name.toLowerCase().includes(search) ||
      team.description.toLowerCase().includes(search)
    )
  }

  if (permissionFilter.value !== null) {
    filtered = filtered.filter(team => team.permissionLevel === permissionFilter.value)
  }

  return filtered.sort((a, b) => b.permissionLevel - a.permissionLevel)
})

// ========== METHODS ==========
const loadTeams = async () => {
  try {
    console.log(' Loading teams...')

    //  Utiliser authService
    const data = await authService.get('/api/v2/teams?includeEmployeeCount=true')

    teams.value = data.teams || []
    console.log(` Loaded ${teams.value.length} teams`)

  } catch (error) {
    console.error(' Error loading teams:', error)
  }
}

const loadEmployees = async () => {
  try {
    console.log(' Loading employees...')

    //  Utiliser authService
    const data = await authService.get('/api/employees')

    employees.value = data || []
    console.log(` Loaded ${employees.value.length} employees`)

  } catch (error) {
    console.error(' Error loading employees:', error)
  }
}
const initializeDefaultTeams = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/init-defaults`, {
      method: 'POST'
    })

    if (response.ok) {
      showNotification('Default teams initialized successfully!', 'success')
      await loadTeams()
    }
  } catch (error) {
    showNotification('Error initializing default teams', 'error')
  }
}

const deleteTeam = async (teamId: string) => {
  if (!confirm('Are you sure you want to delete this team?')) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${teamId}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      showNotification('Team deleted successfully!', 'success')
      await loadTeams()
    } else {
      showNotification('Error deleting team', 'error')
    }
  } catch (error) {
    showNotification('Error deleting team', 'error')
  }
}

const selectTeam = (team: Team) => {
  selectedTeam.value = team
}

const editTeam = (team: Team) => {
  selectedTeam.value = team
}

const manageTeamEmployees = (team: Team) => {
  selectedTeamForAssignment.value = team
  showEmployeeAssignment.value = true
}

const clearFilters = () => {
  searchTerm.value = ''
  permissionFilter.value = null
}

const showNotification = (message: string, type: 'success' | 'error' | 'info') => {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionInfo = (level: number): PermissionLevelInfo => {
  return permissionLevels.find(p => p.level === level) || permissionLevels[permissionLevels.length - 1]
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-burgundy-100 text-burgundy-800'
  if (level >= 5) return 'bg-burgundy-100 text-burgundy-800'
  if (level >= 3) return 'bg-burgundy-100 text-burgundy-800'
  return 'bg-burgundy-100 text-burgundy-800'
}

const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-burgundy'
  if (level >= 5) return 'bg-burgundy-light'
  if (level >= 3) return 'bg-burgundy-lighter'
  return 'bg-burgundy-300'
}

// ========== EVENT HANDLERS ==========
const onTeamCreated = () => {
  showCreateForm.value = false
  loadTeams()
}

const onTeamUpdated = () => {
  selectedTeam.value = null
  loadTeams()
}

const onTeamDeleted = () => {
  selectedTeam.value = null
  loadTeams()
}

const onAssignmentUpdated = () => {
  showEmployeeAssignment.value = false
  selectedTeamForAssignment.value = null
  loadTeams()
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadTeams()
  loadEmployees()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.btn-primary {
  background: linear-gradient(135deg, #730d10 0%, #5a0a0d 100%);
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(115, 13, 16, 0.4);
}

.btn-secondary {
  background: #fdf2f2;
  color: #730d10;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  border: 1px solid #f5bfc0;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.btn-secondary:hover {
  background: #fae6e6;
}

.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg;
  &:focus {
    outline: none;
    border-color: #730d10;
    box-shadow: 0 0 0 2px rgba(115, 13, 16, 0.2);
  }
}

.transition-shadow {
  transition: box-shadow 0.2s ease;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Custom burgundy color classes */
.text-burgundy {
  color: #730d10;
}

.text-burgundy-800 {
  color: #5a0a0d;
}

.bg-burgundy {
  background-color: #730d10;
}

.bg-burgundy-light {
  background-color: #8a3e41;
}

.bg-burgundy-lighter {
  background-color: #a26f71;
}

.bg-burgundy-300 {
  background-color: #bc0004;
}

.bg-burgundy-50 {
  background-color: #fdf2f2;
}

.bg-burgundy-100 {
  background-color: #fae6e6;
}

.border-burgundy-200 {
  border-color: #f5bfc0;
}

.text-burgundy-800 {
  color: #5a0a0d;
}

.hover\:text-burgundy:hover {
  color: #730d10;
}

.hover\:text-burgundy-800:hover {
  color: #5a0a0d;
}

.border-b-burgundy {
  border-bottom-color: #730d10;
}
</style>