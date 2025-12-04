<template>
  <div class="employees-with-teams">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            <svg class="inline w-8 h-8 mr-2" viewBox="0 0 24 24" fill="currentColor" style="color: #730d10;">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
            Employee Role Management
          </h1>
          <p class="text-gray-600 mt-1">Assign and manage employee team memberships</p>
        </div>
        <div class="flex space-x-3">
          <select
            v-model="roleFilter"
            class="border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:border-gray-400"
            style="focus-ring-color: #730d10;"
          >
            <option value="">All Roles</option>
            <option value="admin">Administrators</option>
            <option value="manager">Managers</option>
            <option value="processor">Processors</option>
            <option value="viewer">Viewers</option>
            <option value="unassigned">Unassigned</option>
          </select>
          <button
            @click="exportEmployeeRoles"
            class="btn-secondary"
          >
            <Download class="w-4 h-4 mr-2" />
            Export
          </button>
        </div>
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
      <div class="card text-center">
        <p class="text-2xl font-bold text-gray-900">{{ employeeStats.total }}</p>
        <p class="text-sm text-gray-600">Total Employees</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold" style="color: #730d10;">{{ employeeStats.admins }}</p>
        <p class="text-sm text-gray-600">Administrators</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-yellow-600">{{ employeeStats.managers }}</p>
        <p class="text-sm text-gray-600">Managers</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold" style="color: #8a4a4d;">{{ employeeStats.processors }}</p>
        <p class="text-sm text-gray-600">Processors</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-gray-600">{{ employeeStats.unassigned }}</p>
        <p class="text-sm text-gray-600">Unassigned</p>
      </div>
    </div>

    <!-- Search and Filters -->
    <div class="card mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <div class="relative">
            <Search class="w-5 h-5 absolute left-3 top-3 text-gray-400" />
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Search employees..."
              class="input-field pl-10"
            >
          </div>
        </div>
        <div class="flex gap-2">
          <button
            @click="clearFilters"
            class="btn-secondary"
          >
            <Filter class="w-4 h-4 mr-2" />
            Clear Filters
          </button>
        </div>
      </div>
    </div>

    <!-- Employee List -->
    <div v-if="!loading && filteredEmployees.length > 0" class="space-y-4">
      <div
        v-for="employee in filteredEmployees"
        :key="employee.id"
        class="card hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center">
          <!-- Employee Info -->
          <div class="flex items-center space-x-4 w-1/3">
            <div class="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg" style="background-color: #730d10;">
              {{ getEmployeeInitials(employee) }}
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">{{ employee.fullName }}</h3>
              <p class="text-sm text-gray-600">{{ employee.email }}</p>
              <div class="mt-1">
                <span :class="[
                  'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                  employee.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ employee.active ? 'Active' : 'Inactive' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Teams -->
          <div class="flex-1 mx-8">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="team in employee.teams"
                :key="team.id"
                :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-sm font-medium',
                  getPermissionBadgeColor(team.permissionLevel)
                ]"
              >
                <span class="mr-1">
                  <svg v-if="team.permissionLevel >= 8" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3H5v2h14v-2z"/>
                  </svg>
                  <svg v-else-if="team.permissionLevel >= 5" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
                  </svg>
                  <svg v-else-if="team.permissionLevel >= 3" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
                  </svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                  </svg>
                </span>
                {{ team.name }}
                <span class="ml-1 text-xs">({{ team.permissionLevel }})</span>
              </span>
              <span
                v-if="employee.teams.length === 0"
                class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800"
              >
                No roles assigned
              </span>
            </div>

            <!-- Primary Role -->
            <div class="mt-2">
              <span class="text-sm text-gray-600">
                Primary Role:
                <span :class="[
                  'font-medium',
                  getPrimaryRoleColor(employee)
                ]">
                  {{ getPrimaryRole(employee) }}
                </span>
              </span>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex space-x-2">
            <button
              @click="manageEmployeeTeams(employee)"
              class="btn-primary text-sm"
            >
              <Settings class="w-4 h-4 mr-2" />
              Manage Roles
            </button>
            <button
              @click="viewEmployeeDetails(employee)"
              class="btn-secondary text-sm"
            >
              <Eye class="w-4 h-4 mr-2" />
              View Details
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && filteredEmployees.length === 0" class="card text-center py-12">
      <Users class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-medium text-gray-900 mb-2">No employees found</h3>
      <p class="text-gray-600 mb-4">
        {{ searchTerm || roleFilter ? 'Try adjusting your filters' : 'No employees available' }}
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="card text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto mb-4" style="border-bottom-color: #730d10;"></div>
      <p class="text-gray-600">Loading employees...</p>
    </div>

    <!-- Employee Team Management Modal - FIXED -->
    <EmployeeTeamManagementModal
      v-if="selectedEmployee"
      :employee="selectedEmployee"
      :availableTeams="allTeams"
      @close="handleModalClose"
      @saved="handleModalSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  Search,
  Filter,
  Download,
  Settings,
  Eye,
  Users
} from 'lucide-vue-next'
import EmployeeTeamManagementModal from './EmployeeTeamManagementModal.vue'
import { API_BASE_URL } from '@/config/api.ts'
import authService from "@/services/authService.ts"

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
  teams: Team[]
}

interface Team {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount?: number
}

// ========== PROPS & EMITS ==========
const emit = defineEmits<{
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const searchTerm = ref('')
const roleFilter = ref('')
const employees = ref<Employee[]>([])
const allTeams = ref<Team[]>([])
const selectedEmployee = ref<Employee | null>(null)

// ========== COMPUTED ==========
const employeeStats = computed(() => {
  const stats = {
    total: employees.value.length,
    admins: 0,
    managers: 0,
    processors: 0,
    unassigned: 0
  }

  employees.value.forEach(emp => {
    if (emp.teams.length === 0) {
      stats.unassigned++
    } else {
      const highestLevel = Math.max(...emp.teams.map(t => t.permissionLevel))
      if (highestLevel >= 8) stats.admins++
      else if (highestLevel >= 5) stats.managers++
      else stats.processors++
    }
  })

  return stats
})

const filteredEmployees = computed(() => {
  let filtered = employees.value

  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(emp =>
      emp.fullName.toLowerCase().includes(search) ||
      emp.email.toLowerCase().includes(search) ||
      emp.teams.some(t => t.name.toLowerCase().includes(search))
    )
  }

  if (roleFilter.value) {
    filtered = filtered.filter(emp => {
      if (roleFilter.value === 'unassigned') {
        return emp.teams.length === 0
      }

      const highestLevel = emp.teams.length > 0
        ? Math.max(...emp.teams.map(t => t.permissionLevel))
        : 0

      switch (roleFilter.value) {
        case 'admin': return highestLevel >= 8
        case 'manager': return highestLevel >= 5 && highestLevel < 8
        case 'processor': return highestLevel >= 3 && highestLevel < 5
        case 'viewer': return highestLevel > 0 && highestLevel < 3
        default: return true
      }
    })
  }

  return filtered
})

// ========== METHODS ==========
const loadEmployees = async () => {
  loading.value = true
  console.log(' Starting to load employees...')

  try {
    const data = await authService.get('/api/employees')
    let employeeList: Employee[] = []

    if (Array.isArray(data)) {
      employeeList = data.map(emp => ({
        id: emp.id,
        firstName: emp.firstName,
        lastName: emp.lastName,
        fullName: emp.fullName || `${emp.firstName} ${emp.lastName}`,
        email: emp.email,
        active: emp.active,
        workHoursPerDay: emp.workHoursPerDay,
        efficiencyRating: emp.efficiencyRating || 1.0,
        teams: []
      }))
    }

    const employeesWithTeams = await Promise.all(
      employeeList.map(async (emp: Employee) => {
        try {
          const formattedId = formatUUID(emp.id)
          const teamsData = await authService.get(`/api/v2/teams/employee/${formattedId}?_t=${Date.now()}`)
          emp.teams = teamsData.teams || []
        } catch (error) {
          console.error(` Error loading teams for employee ${emp.id}:`, error)
          emp.teams = []
        }
        return emp
      })
    )

    employees.value = employeesWithTeams
    console.log(' SUCCESS! Employees loaded:', employees.value.length)


  } catch (error) {
    console.error(' Exception during loadEmployees:', error)
  } finally {
    loading.value = false
  }
}

const loadAllTeams = async () => {
  try {
    const data = await authService.get('/api/v2/teams')
    allTeams.value = data.teams || data || []
    console.log(` Loaded ${allTeams.value.length} teams`)
  } catch (error) {
    console.error(' Error loading teams:', error)
  }
}

const manageEmployeeTeams = (employee: Employee) => {
  console.log(' Opening modal for employee:', employee.fullName)
  selectedEmployee.value = employee
}

const handleModalClose = () => {
  console.log(' Modal closed')
  selectedEmployee.value = null
}

const handleModalSaved = async () => {
  console.log(' Modal saved, updating employee')

  try {
    if (selectedEmployee.value) {
      const formattedId = formatUUID(selectedEmployee.value.id)
      const teamsData = await authService.get(`/api/v2/teams/employee/${formattedId}?t=${Date.now()}`)

      const index = employees.value.findIndex(e => e.id === selectedEmployee.value!.id)
      if (index !== -1) {
        employees.value[index].teams = teamsData.teams || []
      }
    }
  } catch (error) {
    console.error(' Error:', error)
  }

  await nextTick()
  selectedEmployee.value = null
  emit('updated')
}

const viewEmployeeDetails = (employee: Employee) => {
  alert(`Employee Details:\n\nName: ${employee.fullName}\nEmail: ${employee.email}\nWork Hours: ${employee.workHoursPerDay}h/day\nTeams: ${employee.teams.length}`)
}

const clearFilters = () => {
  searchTerm.value = ''
  roleFilter.value = ''
}

const exportEmployeeRoles = () => {
  const headers = ['Name', 'Email', 'Teams', 'Highest Permission', 'Primary Role', 'Status']
  const rows = filteredEmployees.value.map(emp => [
    emp.fullName,
    emp.email,
    emp.teams.map(t => t.name).join('; '),
    emp.teams.length > 0 ? Math.max(...emp.teams.map(t => t.permissionLevel)) : '0',
    getPrimaryRole(emp),
    emp.active ? 'Active' : 'Inactive'
  ])

  const csv = [headers, ...rows].map(row =>
    row.map(cell => `"${cell}"`).join(',')
  ).join('\n')

  const blob = new Blob([csv], { type: 'text/csv' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `employee-roles-${new Date().toISOString().split('T')[0]}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`.toUpperCase()
}

const getPrimaryRole = (employee: Employee) => {
  if (employee.teams.length === 0) return 'No Role'
  const highestLevel = Math.max(...employee.teams.map(t => t.permissionLevel))
  if (highestLevel >= 8) return 'Administrator'
  if (highestLevel >= 5) return 'Manager'
  if (highestLevel >= 3) return 'Processor'
  return 'Viewer'
}

const getPrimaryRoleColor = (employee: Employee) => {
  if (employee.teams.length === 0) return 'text-gray-600'
  const highestLevel = Math.max(...employee.teams.map(t => t.permissionLevel))
  if (highestLevel >= 8) return 'text-[#730d10]'
  if (highestLevel >= 5) return 'text-yellow-600'
  if (highestLevel >= 3) return 'text-[#8a4a4d]'
  return 'text-gray-600'
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-[#fde8e8] text-[#730d10]'
  if (level >= 5) return 'bg-yellow-100 text-yellow-800'
  if (level >= 3) return 'bg-[#fdf2f2] text-[#8a4a4d]'
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

const formatUUID = (uuid: string) => {
  if (!uuid) return uuid
  const cleaned = uuid.replace(/-/g, '').toLowerCase()
  if (cleaned.length === 32) {
    return `${cleaned.slice(0, 8)}-${cleaned.slice(8, 12)}-${cleaned.slice(12, 16)}-${cleaned.slice(16, 20)}-${cleaned.slice(20)}`
  }
  return uuid
}

onMounted(() => {
  loadEmployees()
  loadAllTeams()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.btn-primary {
  background-color: #730d10;
  @apply text-white px-4 py-2 rounded-lg transition-colors flex items-center;
}

.btn-primary:hover {
  background-color: #5a0a0c;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors flex items-center;
}

.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:border-gray-400;
}

.input-field:focus {
  --tw-ring-color: #730d10;
  border-color: #730d10;
}
</style>
