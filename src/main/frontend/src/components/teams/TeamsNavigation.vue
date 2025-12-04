<template>
  <div class="teams-navigation">
    <!-- Navigation Header -->
    <div class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <!-- Breadcrumb -->
          <nav class="flex items-center space-x-2 text-sm">
            <button
              @click="currentView = 'overview'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors flex items-center gap-2',
                currentView === 'overview'
                  ? 'bg-[#fde8e8] text-[#730d10] font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
              </svg>
              Overview
            </button>
            <span class="text-gray-400">/</span>
            <button
              @click="currentView = 'teams'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors flex items-center gap-2',
                currentView === 'teams'
                  ? 'bg-[#fde8e8] text-[#730d10] font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
              </svg>
              Teams
            </button>
            <span class="text-gray-400">/</span>
            <button
              @click="currentView = 'employees'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors flex items-center gap-2',
                currentView === 'employees'
                  ? 'bg-[#fde8e8] text-[#730d10] font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
              </svg>
              Employees
            </button>
          </nav>

          <!-- Quick Actions -->
          <div class="flex items-center space-x-3">
            <div class="text-sm text-gray-600">
              Last updated: {{ lastUpdated }}
            </div>
            <button
              @click="refreshData"
              :disabled="loading"
              class="p-2 text-[#730d10] hover:text-[#5a0a0c] transition-colors"
              :class="{ 'animate-spin': loading }"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
            </button>
            <div class="h-4 w-px bg-gray-300"></div>
            <button
              v-if="currentView === 'teams'"
              @click="showCreateTeam = true"
              class="btn-primary text-sm"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              New Team
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <!-- Overview -->
      <div v-if="currentView === 'overview'">
        <TeamOverview
          :stats="stats"
          @view-teams="currentView = 'teams'"
          @view-employees="currentView = 'employees'"
        />
      </div>

      <!-- Teams Management -->
      <div v-else-if="currentView === 'teams'">
        <TeamManagement @updated="refreshData" />
      </div>

      <!-- Employees with Teams -->
      <div v-else-if="currentView === 'employees'">
        <EmployeesWithTeams @updated="refreshData" />
      </div>
    </div>

    <!-- Create Team Modal -->
    <CreateTeamModal
      v-if="showCreateTeam"
      @close="showCreateTeam = false"
      @created="onTeamCreated"
      :permission-levels="permissionLevels"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TeamManagement from './TeamManagement.vue'
import TeamOverview from './TeamOverview.vue'
import EmployeesWithTeams from './EmployeesWithTeams.vue'
import CreateTeamModal from './CreateTeamModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface TeamStats {
  totalTeams: number
  adminTeams: number
  managerTeams: number
  totalMembers: number
  emptyTeams: number
  averagePermissionLevel: number
}

interface PermissionLevelInfo {
  level: number
  name: string
  description: string
  colorCode: string
}

// ========== STATE ==========
const currentView = ref<'overview' | 'teams' | 'employees'>('overview')
const loading = ref(false)
const lastUpdated = ref('')
const showCreateTeam = ref(false)
const stats = ref<TeamStats>({
  totalTeams: 0,
  adminTeams: 0,
  managerTeams: 0,
  totalMembers: 0,
  emptyTeams: 0,
  averagePermissionLevel: 0
})

// ========== PERMISSION LEVELS ==========
const permissionLevels: PermissionLevelInfo[] = [
  { level: 10, name: 'SUPER_ADMIN', description: 'Super administrator with unrestricted access', colorCode: '#730d10' },
  { level: 9, name: 'ADMIN', description: 'System administrator with full management rights', colorCode: '#8a4a4d' },
  { level: 8, name: 'SENIOR_ADMIN', description: 'Senior administrator with most privileges', colorCode: '#a1686a' },
  { level: 7, name: 'MANAGER', description: 'Team manager with planning and oversight privileges', colorCode: '#b88687' },
  { level: 6, name: 'SENIOR_SUPERVISOR', description: 'Senior supervisor with extended monitoring rights', colorCode: '#cfa4a5' },
  { level: 5, name: 'SUPERVISOR', description: 'Supervisor with monitoring and basic management', colorCode: '#e6c2c3' },
  { level: 4, name: 'SENIOR_PROCESSOR', description: 'Senior processor with advanced processing rights', colorCode: '#f5e0e0' },
  { level: 3, name: 'PROCESSOR', description: 'Card processor with standard processing access', colorCode: '#faf0f0' },
  { level: 2, name: 'JUNIOR_PROCESSOR', description: 'Junior processor with limited processing access', colorCode: '#fdf7f7' },
  { level: 1, name: 'VIEWER', description: 'Read-only access for viewing and reports', colorCode: '#fefcfc' }
]

// ========== METHODS ==========
const loadStats = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/statistics`)
    if (response.ok) {
      const data = await response.json()

      stats.value = {
        totalTeams: data.totalTeams || 0,
        adminTeams: data.teamStatistics?.filter((g: any) => g.permissionLevel >= 8).length || 0,
        managerTeams: data.teamStatistics?.filter((g: any) => g.permissionLevel >= 5 && g.permissionLevel < 8).length || 0,
        totalMembers: data.totalEmployeesInTeams || 0,
        emptyTeams: data.emptyTeamsCount || 0,
        averagePermissionLevel: calculateAveragePermissionLevel(data.teamStatistics || [])
      }

      lastUpdated.value = new Date().toLocaleTimeString()
    }
  } catch (error) {
    console.error('Error loading stats:', error)
  } finally {
    loading.value = false
  }
}

const calculateAveragePermissionLevel = (teamStats: any[]) => {
  if (teamStats.length === 0) return 0

  const totalWeightedLevel = teamStats.reduce((sum, team) => {
    return sum + (team.permissionLevel * team.employeeCount)
  }, 0)

  const totalEmployees = teamStats.reduce((sum, team) => sum + team.employeeCount, 0)

  return totalEmployees > 0 ? Math.round((totalWeightedLevel / totalEmployees) * 10) / 10 : 0
}

const refreshData = async () => {
  await loadStats()
}

const onTeamCreated = () => {
  showCreateTeam.value = false
  refreshData()
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.btn-primary {
  background: linear-gradient(135deg, #730d10 0%, #5a0a0c 100%);
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

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>