<template>
  <div class="group-overview">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900">👥 Groups & Roles Overview</h1>
      <p class="text-gray-600 mt-2">Manage team roles, permissions, and organizational structure</p>
    </div>

    <!-- Key Metrics -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Total Groups -->
      <div class="card metric-card">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-blue-500 rounded-lg flex items-center justify-center">
            <Users class="w-6 h-6 text-white" />
          </div>
          <div class="ml-4">
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalGroups }}</p>
            <p class="text-sm text-gray-600">Total Groups</p>
          </div>
        </div>
        <div class="mt-4">
          <button
            @click="$emit('view-groups')"
            class="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            Manage Groups →
          </button>
        </div>
      </div>

      <!-- Admin Groups -->
      <div class="card metric-card">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-red-500 rounded-lg flex items-center justify-center">
            <Crown class="w-6 h-6 text-white" />
          </div>
          <div class="ml-4">
            <p class="text-2xl font-bold text-gray-900">{{ stats.adminGroups }}</p>
            <p class="text-sm text-gray-600">Admin Groups</p>
          </div>
        </div>
        <div class="mt-4">
          <span class="text-xs bg-red-100 text-red-800 px-2 py-1 rounded-full">
            Level 8+ permissions
          </span>
        </div>
      </div>

      <!-- Manager Groups -->
      <div class="card metric-card">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-yellow-500 rounded-lg flex items-center justify-center">
            <Shield class="w-6 h-6 text-white" />
          </div>
          <div class="ml-4">
            <p class="text-2xl font-bold text-gray-900">{{ stats.managerGroups }}</p>
            <p class="text-sm text-gray-600">Manager Groups</p>
          </div>
        </div>
        <div class="mt-4">
          <span class="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded-full">
            Level 5-7 permissions
          </span>
        </div>
      </div>

      <!-- Total Members -->
      <div class="card metric-card">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-green-500 rounded-lg flex items-center justify-center">
            <UserPlus class="w-6 h-6 text-white" />
          </div>
          <div class="ml-4">
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalMembers }}</p>
            <p class="text-sm text-gray-600">Total Members</p>
          </div>
        </div>
        <div class="mt-4">
          <button
            @click="$emit('view-employees')"
            class="text-green-600 hover:text-green-800 text-sm font-medium"
          >
            View Employees →
          </button>
        </div>
      </div>
    </div>

    <!-- Quick Insights -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
      <!-- Permission Distribution -->
      <div class="card">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">Permission Level Distribution</h3>
        <div class="space-y-3">
          <div
            v-for="level in permissionDistribution"
            :key="level.level"
            class="flex items-center justify-between"
          >
            <div class="flex items-center space-x-3">
              <div :class="[
                'w-8 h-8 rounded-full flex items-center justify-center text-white text-sm font-bold',
                getPermissionLevelColor(level.level)
              ]">
                {{ level.level }}
              </div>
              <div>
                <p class="font-medium text-gray-900">{{ getPermissionName(level.level) }}</p>
                <p class="text-sm text-gray-600">{{ level.count }} groups</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-lg font-bold text-gray-900">{{ level.employeeCount }}</p>
              <p class="text-xs text-gray-600">employees</p>
            </div>
          </div>
        </div>
      </div>

      <!-- System Health -->
      <div class="card">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">System Health</h3>
        <div class="space-y-4">
          <!-- Security Score -->
          <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
            <div class="flex items-center space-x-3">
              <div :class="[
                'w-8 h-8 rounded-full flex items-center justify-center',
                securityScore >= 80 ? 'bg-green-500' :
                securityScore >= 60 ? 'bg-yellow-500' : 'bg-red-500'
              ]">
                <Shield class="w-4 h-4 text-white" />
              </div>
              <div>
                <p class="font-medium text-gray-900">Security Score</p>
                <p class="text-sm text-gray-600">Permission structure health</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-2xl font-bold text-gray-900">{{ securityScore }}%</p>
            </div>
          </div>

          <!-- Coverage -->
          <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
            <div class="flex items-center space-x-3">
              <div :class="[
                'w-8 h-8 rounded-full flex items-center justify-center',
                coverage >= 80 ? 'bg-green-500' :
                coverage >= 60 ? 'bg-yellow-500' : 'bg-red-500'
              ]">
                <Users class="w-4 h-4 text-white" />
              </div>
              <div>
                <p class="font-medium text-gray-900">Employee Coverage</p>
                <p class="text-sm text-gray-600">Employees with role assignments</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-2xl font-bold text-gray-900">{{ coverage }}%</p>
            </div>
          </div>

          <!-- Empty Groups Warning -->
          <div v-if="stats.emptyGroups > 0" class="flex items-center justify-between p-3 bg-orange-50 rounded-lg border border-orange-200">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center">
                <AlertTriangle class="w-4 h-4 text-white" />
              </div>
              <div>
                <p class="font-medium text-orange-900">Empty Groups</p>
                <p class="text-sm text-orange-700">Groups with no members</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-2xl font-bold text-orange-900">{{ stats.emptyGroups }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="card">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <button
          @click="$emit('view-groups')"
          class="p-4 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-left"
        >
          <div class="flex items-center space-x-3 mb-2">
            <Users class="w-6 h-6 text-blue-600" />
            <span class="font-medium text-gray-900">Manage Groups</span>
          </div>
          <p class="text-sm text-gray-600">Create, edit, and organize groups</p>
        </button>

        <button
          @click="$emit('view-employees')"
          class="p-4 border border-green-200 rounded-lg hover:bg-green-50 transition-colors text-left"
        >
          <div class="flex items-center space-x-3 mb-2">
            <UserPlus class="w-6 h-6 text-green-600" />
            <span class="font-medium text-gray-900">Assign Roles</span>
          </div>
          <p class="text-sm text-gray-600">Assign employees to groups</p>
        </button>

        <button
          @click="initializeDefaultGroups"
          :disabled="loading"
          class="p-4 border border-purple-200 rounded-lg hover:bg-purple-50 transition-colors text-left disabled:opacity-50"
        >
          <div class="flex items-center space-x-3 mb-2">
            <Settings class="w-6 h-6 text-purple-600" />
            <span class="font-medium text-gray-900">Setup Defaults</span>
          </div>
          <p class="text-sm text-gray-600">Initialize standard groups</p>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Users,
  Crown,
  Shield,
  UserPlus,
  Settings,
  AlertTriangle
} from 'lucide-vue-next'

const API_BASE_URL = 'http://localhost:8080'

// ========== INTERFACES ==========
interface GroupStats {
  totalGroups: number
  adminGroups: number
  managerGroups: number
  totalMembers: number
  emptyGroups: number
  averagePermissionLevel: number
}

// ========== PROPS & EMITS ==========
const props = defineProps<{
  stats: GroupStats
}>()

const emit = defineEmits<{
  'view-groups': []
  'view-employees': []
}>()

// ========== STATE ==========
const loading = ref(false)

// ========== COMPUTED ==========
const permissionDistribution = computed(() => {
  // Mock data - in real app, this would come from API
  return [
    { level: 10, count: 1, employeeCount: 2 },
    { level: 7, count: 2, employeeCount: 5 },
    { level: 5, count: 1, employeeCount: 8 },
    { level: 3, count: 1, employeeCount: 15 },
    { level: 1, count: 1, employeeCount: 3 }
  ].filter(item => item.count > 0)
})

const securityScore = computed(() => {
  // Calculate security score based on permission distribution
  const hasAdmin = props.stats.adminGroups > 0
  const hasManager = props.stats.managerGroups > 0
  const hasProcessor = props.stats.totalGroups - props.stats.adminGroups - props.stats.managerGroups > 0
  const noEmptyGroups = props.stats.emptyGroups === 0

  let score = 0
  if (hasAdmin) score += 30
  if (hasManager) score += 25
  if (hasProcessor) score += 25
  if (noEmptyGroups) score += 20

  return Math.min(score, 100)
})

const coverage = computed(() => {
  // Mock calculation - in real app, this would be based on actual employee data
  return Math.round((props.stats.totalMembers / Math.max(props.stats.totalMembers + 5, 1)) * 100)
})

// ========== METHODS ==========
const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-red-500'
  if (level >= 5) return 'bg-yellow-500'
  if (level >= 3) return 'bg-blue-500'
  return 'bg-gray-500'
}

const getPermissionName = (level: number) => {
  if (level >= 8) return 'Administrator'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  return 'Viewer'
}

const initializeDefaultGroups = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups/init-defaults`, {
      method: 'POST'
    })

    if (response.ok) {
