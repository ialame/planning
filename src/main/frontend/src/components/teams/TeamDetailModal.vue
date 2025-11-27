<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
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
              <h2 class="text-lg font-semibold text-gray-900">{{ team.name }}</h2>
              <p class="text-sm text-gray-600">{{ team.description }}</p>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-gray-600"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="border-b border-gray-200">
        <nav class="flex">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors flex items-center',
              activeTab === tab.id
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            ]"
          >
            <component :is="tab.icon" class="w-4 h-4 mr-2" />
            {{ tab.label }}
          </button>
        </nav>
      </div>

      <!-- Content -->
      <div class="p-6">
        <!-- Overview Tab -->
        <div v-if="activeTab === 'overview'" class="space-y-6">
          <!-- Basic Info -->
          <div class="grid grid-cols-2 gap-6">
            <div class="space-y-4">
              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Team Information</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Name:</span>
                    <span class="text-sm font-medium">{{ team.name }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Status:</span>
                    <span :class="[
                      'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                      team.active ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'
                    ]">
                      <svg v-if="team.active" class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                      </svg>
                      <svg v-else class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                      </svg>
                      {{ team.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Members:</span>
                    <span class="text-sm font-medium">{{ teamMembers.length }}</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Permissions</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Level:</span>
                    <span :class="[
                      'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                      getPermissionBadgeColor(team.permissionLevel)
                    ]">
                      <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                      </svg>
                      {{ team.permissionLevel }} - {{ getPermissionInfo(team.permissionLevel).name }}
                    </span>
                  </div>
                  <div class="text-sm text-gray-600">
                    {{ getPermissionInfo(team.permissionLevel).description }}
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-4">
              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Timestamps</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Created:</span>
                    <span class="text-sm">{{ formatDate(team.creationDate) }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Modified:</span>
                    <span class="text-sm">{{ formatDate(team.modificationDate) }}</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Statistics</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Active Members:</span>
                    <span class="text-sm font-medium">{{ activeMembers }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Avg. Efficiency:</span>
                    <span class="text-sm font-medium">{{ averageEfficiency }}x</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Total Work Hours:</span>
                    <span class="text-sm font-medium">{{ totalWorkHours }}h/day</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Members Preview -->
          <div>
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-sm font-medium text-gray-700">Recent Members</h3>
              <button
                @click="activeTab = 'members'"
                class="text-blue-600 hover:text-blue-800 text-sm flex items-center gap-1"
              >
                View all {{ teamMembers.length }}
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div
                v-for="member in teamMembers.slice(0, 4)"
                :key="member.id"
                class="flex items-center space-x-2 p-2 bg-gray-50 rounded"
              >
                <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white text-sm font-medium">
                  {{ getEmployeeInitials(member) }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-900 truncate">{{ member.fullName }}</p>
                  <p class="text-xs text-gray-600 truncate">{{ member.email }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Members Tab -->
        <div v-else-if="activeTab === 'members'" class="space-y-4">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium text-gray-900">Team Members ({{ teamMembers.length }})</h3>
            <button
              @click="openEmployeeAssignment"
              class="btn-primary text-sm"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"/>
              </svg>
              Manage Members
            </button>
          </div>

          <div v-if="teamMembers.length === 0" class="text-center py-8">
            <svg class="w-12 h-12 text-gray-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
            <p class="text-gray-500 mb-4">No members assigned to this team</p>
            <button
              @click="openEmployeeAssignment"
              class="btn-primary"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              Add First Member
            </button>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="member in teamMembers"
              :key="member.id"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg"
            >
              <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                  {{ getEmployeeInitials(member) }}
                </div>
                <div>
                  <p class="font-medium text-gray-900">{{ member.fullName }}</p>
                  <p class="text-sm text-gray-600">{{ member.email }}</p>
                  <div class="flex items-center space-x-2 mt-1">
                    <span class="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                      {{ member.workHoursPerDay }}h/day
                    </span>
                    <span class="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                      {{ member.efficiencyRating }}x efficiency
                    </span>
                    <span :class="[
                      'text-xs px-2 py-1 rounded',
                      member.active ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'
                    ]">
                      {{ member.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                </div>
              </div>
              <button
                @click="removeMemberFromTeam(member)"
                class="text-red-600 hover:text-red-800 p-2"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M22 10.5h-6m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.766z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Settings Tab -->
        <div v-else-if="activeTab === 'settings'" class="space-y-6">
          <form @submit.prevent="updateTeam">
            <!-- Description -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                v-model="editForm.description"
                rows="3"
                class="input-field resize-none"
                maxlength="255"
              ></textarea>
              <p class="text-gray-500 text-sm mt-1">{{ editForm.description.length }}/255 characters</p>
            </div>

            <!-- Permission Level -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Permission Level
              </label>
              <select
                v-model="editForm.permissionLevel"
                class="input-field"
              >
                <option v-for="level in permissionLevels" :key="level.level" :value="level.level">
                  Level {{ level.level }} - {{ level.name }}
                </option>
              </select>
              <p class="text-sm text-gray-600 mt-1">
                {{ getPermissionInfo(editForm.permissionLevel).description }}
              </p>
            </div>

            <!-- Active Status -->
            <div class="mb-6">
              <label class="flex items-center">
                <input
                  v-model="editForm.active"
                  type="checkbox"
                  class="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                >
                <span class="ml-2 text-sm text-gray-700">Team is active</span>
              </label>
              <p class="text-xs text-gray-500 mt-1">
                Inactive teams cannot be assigned to employees
              </p>
            </div>

            <!-- Actions -->
            <div class="flex justify-between">
              <button
                type="button"
                @click="confirmDelete"
                class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors flex items-center"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
                Delete Team
              </button>

              <div class="flex space-x-3">
                <button
                  type="button"
                  @click="resetForm"
                  class="btn-secondary"
                >
                  Reset
                </button>
                <button
                  type="submit"
                  :disabled="!hasChanges || loading"
                  class="btn-primary"
                  :class="{ 'opacity-50 cursor-not-allowed': !hasChanges || loading }"
                >
                  <div v-if="loading" class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  {{ loading ? 'Saving...' : 'Save Changes' }}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Employee Assignment Modal -->
    <EmployeeAssignmentModal
      v-if="showEmployeeAssignment"
      :team="team"
      :employees="allEmployees"
      @close="showEmployeeAssignment = false"
      @updated="onMembersUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import EmployeeAssignmentModal from './EmployeeAssignmentModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

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

// ========== PROPS & EMITS ==========
const props = defineProps<{
  team: Team
}>()

const emit = defineEmits<{
  close: []
  updated: []
  deleted: []
}>()

// ========== STATE ==========
const loading = ref(false)
const activeTab = ref('overview')
const teamMembers = ref<Employee[]>([])
const allEmployees = ref<Employee[]>([])
const showEmployeeAssignment = ref(false)

const editForm = ref({
  description: props.team.description,
  permissionLevel: props.team.permissionLevel,
  active: props.team.active
})

// ========== PERMISSION LEVELS ==========
const permissionLevels: PermissionLevelInfo[] = [
  { level: 10, name: 'SUPER_ADMIN', description: 'Super administrator with unrestricted access', colorCode: '#1E3A8A' },
  { level: 9, name: 'ADMIN', description: 'System administrator with full management rights', colorCode: '#1D4ED8' },
  { level: 8, name: 'SENIOR_ADMIN', description: 'Senior administrator with most privileges', colorCode: '#3B82F6' },
  { level: 7, name: 'MANAGER', description: 'Team manager with planning and oversight privileges', colorCode: '#60A5FA' },
  { level: 6, name: 'SENIOR_SUPERVISOR', description: 'Senior supervisor with extended monitoring rights', colorCode: '#93C5FD' },
  { level: 5, name: 'SUPERVISOR', description: 'Supervisor with monitoring and basic management', colorCode: '#BFDBFE' },
  { level: 4, name: 'SENIOR_PROCESSOR', description: 'Senior processor with advanced processing rights', colorCode: '#DBEAFE' },
  { level: 3, name: 'PROCESSOR', description: 'Card processor with standard processing access', colorCode: '#E0F2FE' },
  { level: 2, name: 'JUNIOR_PROCESSOR', description: 'Junior processor with limited processing access', colorCode: '#F0F9FF' },
  { level: 1, name: 'VIEWER', description: 'Read-only access for viewing and reports', colorCode: '#F8FAFC' }
]

// ========== TABS ==========
const tabs = [
  { 
    id: 'overview', 
    label: 'Overview', 
    icon: { 
      template: `<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
      </svg>` 
    } 
  },
  { 
    id: 'members', 
    label: 'Members', 
    icon: { 
      template: `<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
      </svg>` 
    } 
  },
  { 
    id: 'settings', 
    label: 'Settings', 
    icon: { 
      template: `<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
      </svg>` 
    } 
  }
]

// ========== COMPUTED ==========
const activeMembers = computed(() => {
  return teamMembers.value.filter(m => m.active).length
})

const averageEfficiency = computed(() => {
  if (teamMembers.value.length === 0) return 0
  const sum = teamMembers.value.reduce((acc, m) => acc + m.efficiencyRating, 0)
  return Math.round((sum / teamMembers.value.length) * 100) / 100
})

const totalWorkHours = computed(() => {
  return teamMembers.value.reduce((acc, m) => acc + m.workHoursPerDay, 0)
})

const hasChanges = computed(() => {
  return editForm.value.description !== props.team.description ||
    editForm.value.permissionLevel !== props.team.permissionLevel ||
    editForm.value.active !== props.team.active
})

// ========== METHODS ==========
const loadTeamMembers = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}?includeEmployees=true`)
    if (response.ok) {
      const data = await response.json()
      teamMembers.value = data.employees || []
    }
  } catch (error) {
    console.error('Error loading team members:', error)
  } finally {
    loading.value = false
  }
}

const loadAllEmployees = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees`)
    if (response.ok) {
      const data = await response.json()
      allEmployees.value = data.employees || []
    }
  } catch (error) {
    console.error('Error loading employees:', error)
  }
}

const updateTeam = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editForm.value)
    })

    if (response.ok) {
      emit('updated')
    } else {
      console.error('Error updating team')
    }
  } catch (error) {
    console.error('Error updating team:', error)
  } finally {
    loading.value = false
  }
}

const confirmDelete = () => {
  if (confirm(`Are you sure you want to delete the team "${props.team.name}"? This action cannot be undone.`)) {
    deleteTeam()
  }
}

const deleteTeam = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      emit('deleted')
    } else {
      console.error('Error deleting team')
    }
  } catch (error) {
    console.error('Error deleting team:', error)
  } finally {
    loading.value = false
  }
}

const removeMemberFromTeam = async (employee: Employee) => {
  if (!confirm(`Remove ${employee.fullName} from this team?`)) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}/employees/${employee.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      teamMembers.value = teamMembers.value.filter(m => m.id !== employee.id)
    }
  } catch (error) {
    console.error('Error removing member:', error)
  }
}

const openEmployeeAssignment = () => {
  showEmployeeAssignment.value = true
}

const onMembersUpdated = () => {
  showEmployeeAssignment.value = false
  loadTeamMembers()
  emit('updated')
}

const resetForm = () => {
  editForm.value = {
    description: props.team.description,
    permissionLevel: props.team.permissionLevel,
    active: props.team.active
  }
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionInfo = (level: number): PermissionLevelInfo => {
  return permissionLevels.find(p => p.level === level) || permissionLevels[permissionLevels.length - 1]
}

const getPermissionBadgeColor = (level: number) => {
  return 'bg-blue-100 text-blue-800'
}

const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-blue-600'
  if (level >= 5) return 'bg-blue-500'
  if (level >= 3) return 'bg-blue-400'
  return 'bg-blue-300'
}

const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const formatDate = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return 'Invalid date'
  }
}

// ========== WATCHERS ==========
watch(() => props.team, (newTeam) => {
  if (newTeam) {
    editForm.value = {
      description: newTeam.description,
      permissionLevel: newTeam.permissionLevel,
      active: newTeam.active
    }
    loadTeamMembers()
  }
}, { immediate: true })

// ========== LIFECYCLE ==========
onMounted(() => {
  loadTeamMembers()
  loadAllEmployees()
})
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.btn-primary {
  background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
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
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn-secondary {
  background: #EFF6FF;
  color: #1D4ED8;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  border: 1px solid #BFDBFE;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #DBEAFE;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>