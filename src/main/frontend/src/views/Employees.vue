<template>
  <div class="employees-page">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-3xl font-bold text-gray-900">Team Management</h1>

      <!-- View Switcher -->
      <div class="flex space-x-2 bg-gray-100 rounded-lg p-1">
        <button
          @click="currentView = 'management'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors flex items-center gap-2',
            currentView === 'management'
              ? 'bg-white text-burgundy shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
          Management
        </button>
        <button
          @click="currentView = 'planning'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors flex items-center gap-2',
            currentView === 'planning'
              ? 'bg-white text-burgundy shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
          Planning
        </button>
      </div>
    </div>

    <!-- Date Selector (Planning view only) -->
    <div v-if="currentView === 'planning' && !selectedEmployeeId" class="mb-6">
      <div class="bg-white rounded-lg shadow-md p-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Select Date for Planning
        </label>
        <input
          v-model="selectedDate"
          type="date"
          @change="loadPlanningForSelectedDate"
          class="input-field max-w-xs"
        />
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <div class="card">
        <div class="flex items-center">
          <div class="bg-burgundy rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Employees</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.total }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-burgundy-light rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Available</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.available }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-burgundy-lighter rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Busy</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.busy }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-burgundy-dark rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Overloaded</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.overloaded }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Employee List -->
    <div v-if="!selectedEmployeeId">

      <!--  MANAGEMENT VIEW -->
      <div v-if="currentView === 'management'" class="bg-white rounded-lg shadow-md overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">Team Members</h2>
            <button @click="showAddForm = !showAddForm" class="btn-primary">
              {{ showAddForm ? 'Cancel' : '+ Add Employee' }}
            </button>
          </div>
        </div>

        <!-- Add Employee Form -->
        <div v-if="showAddForm" class="p-6 bg-gray-50 border-b">
          <form @submit.prevent="addEmployee" class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <input
              v-model="newEmployee.firstName"
              placeholder="First Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.lastName"
              placeholder="Last Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.email"
              type="email"
              placeholder="Email"
              class="input-field"
              required
            >
            <div class="flex space-x-2">
              <button type="submit" class="btn-primary flex-1">Add</button>
              <button type="button" @click="showAddForm = false" class="btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <!-- Employee Cards Grid (MANAGEMENT VIEW) -->
        <div class="p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div
              v-for="employee in managementEmployees"
              :key="employee.id"
              class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-gray-200"
            >
              <div class="p-6">
                <!-- Employee Header -->
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center">
                    <!--  Avatar Component -->
                    <EmployeeAvatar
                      :key="`avatar-${employee.id}-${avatarKey}`"
                      :employeeId="employee.id"
                      :employeeName="`${employee.firstName} ${employee.lastName}`"
                      size="md"
                    />

                    <div class="ml-3">
                      <h3 class="text-lg font-semibold text-gray-900">
                        {{ employee.firstName }} {{ employee.lastName }}
                      </h3>
                      <p class="text-sm text-gray-600">{{ employee.email }}</p>
                    </div>
                  </div>

                  <span :class="[
                    'px-2 py-1 rounded-full text-xs font-medium',
                    employee.status === 'AVAILABLE' ? 'bg-burgundy-100 text-burgundy-800' :
                    employee.status === 'BUSY' ? 'bg-burgundy-100 text-burgundy-800' :
                    employee.status === 'OVERLOADED' ? 'bg-burgundy-100 text-burgundy-800' :
                    'bg-gray-100 text-gray-800'
                  ]">
                    {{ employee.status || 'AVAILABLE' }}
                  </span>
                </div>

                <!-- Employee Details -->
                <div class="space-y-2 mb-4">
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Work Hours:</span>
                    <span class="font-medium">{{ employee.workHoursPerDay }}h/day</span>
                  </div>
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Teams:</span>
                    <span class="font-medium">{{ employee.teams?.length || 0 }}</span>
                  </div>
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Active:</span>
                    <span :class="employee.active ? 'text-burgundy' : 'text-burgundy-light'">
                      <svg v-if="employee.active" class="w-4 h-4 inline text-burgundy" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                      </svg>
                      <svg v-else class="w-4 h-4 inline text-burgundy-light" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                      </svg>
                      {{ employee.active ? 'Yes' : 'No' }}
                    </span>
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="flex space-x-2">
                  <!-- Upload Photo Button -->
                  <button
                    @click="openPhotoUpload(employee.id)"
                    class="flex-1 bg-burgundy-50 text-burgundy px-3 py-2 rounded text-sm font-medium hover:bg-burgundy-100 flex items-center justify-center gap-1"
                    title="Upload Photo"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                    Photo
                  </button>

                  <!-- View Details Button -->
                  <button
                    @click="viewEmployeeDetails(employee.id)"
                    class="flex-1 bg-burgundy-50 text-burgundy px-3 py-2 rounded text-sm font-medium hover:bg-burgundy-100 flex items-center justify-center gap-1"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                    </svg>
                    Details
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!--  PLANNING VIEW -->
      <div v-if="currentView === 'planning'" class="space-y-6">
        <div
          v-for="employee in employeesWithWorkload"
          :key="employee.id"
          class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
        >
          <div class="p-6">
            <!-- Employee Info -->
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center">
                <EmployeeAvatar
                  :employeeId="employee.id"
                  :employeeName="`${employee.firstName} ${employee.lastName}`"
                  size="lg"
                />
                <div class="ml-4">
                  <h3 class="text-xl font-semibold text-gray-900">
                    {{ employee.firstName }} {{ employee.lastName }}
                  </h3>
                  <p class="text-sm text-gray-600">{{ employee.email }}</p>
                </div>
              </div>

              <!-- Workload Badge -->
              <div :class="[
                'px-4 py-2 rounded-lg text-sm font-semibold',
                employee.workload < 0.8 ? 'bg-burgundy-100 text-burgundy-800' :
                employee.workload < 1.0 ? 'bg-burgundy-100 text-burgundy-800' :
                'bg-burgundy-100 text-burgundy-800'
              ]">
                {{ Math.round(employee.workload * 100) }}% Workload
              </div>
            </div>

            <!-- Workload Bar -->
            <div class="mb-4">
              <div class="flex justify-between text-sm text-gray-600 mb-1">
                <span>Capacity</span>
                <span>{{ employee.estimatedHours }}h / {{ employee.workHoursPerDay }}h</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-3">
                <div
                  :class="[
                    'h-3 rounded-full transition-all',
                    employee.workload < 0.8 ? 'bg-burgundy' :
                    employee.workload < 1.0 ? 'bg-burgundy-light' :
                    'bg-burgundy-dark'
                  ]"
                  :style="{ width: Math.min(employee.workload * 100, 100) + '%' }"
                ></div>
              </div>
            </div>

            <!-- Stats -->
            <div class="grid grid-cols-3 gap-4 mb-4">
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ employee.totalCards || 0 }}</p>
                <p class="text-xs text-gray-600">Total Cards</p>
              </div>
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ employee.activeOrders || 0 }}</p>
                <p class="text-xs text-gray-600">Active Orders</p>
              </div>
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ selectedDate }}</p>
                <p class="text-xs text-gray-600">Planning Date</p>
              </div>
            </div>

            <!--  PLANNING ACTIONS -->
            <div class="flex space-x-2">
              <button
                @click="viewEmployeePlanning(employee.id)"
                class="flex-1 bg-burgundy-50 text-burgundy px-3 py-2 rounded text-sm font-medium hover:bg-burgundy-100 flex items-center justify-center gap-1"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                View Orders & Cards
              </button>
              <button
                class="px-3 py-2 bg-burgundy-50 text-burgundy rounded text-sm font-medium hover:bg-burgundy-100 flex items-center justify-center gap-1"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                </svg>
                {{ employee.totalCards || 0 }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="employees.length === 0 && !loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <svg class="mx-auto h-12 w-12 text-burgundy mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">No employees found</h3>
        <p class="text-gray-600 mb-4">Start by adding your first team member</p>
        <button @click="showAddForm = true" class="btn-primary">
          + Add First Employee
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-burgundy mx-auto mb-4"></div>
        <p class="text-gray-600">Loading employees...</p>
      </div>
    </div>

    <!--  EMPLOYEE DETAIL VIEW -->
    <div v-if="selectedEmployeeId" class="space-y-6">
      <EmployeeDetailPage
        :key="`${selectedEmployeeId}-${selectedDate}`"
        :employeeId="selectedEmployeeId"
        :selectedDate="selectedDate"
        :mode="currentView"
        @back="handleEmployeeBack"
        @refresh="loadEmployees"
      />
    </div>

    <!--  PHOTO UPLOAD MODAL -->
    <div
      v-if="showPhotoUploadModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closePhotoUpload"
    >
      <div class="bg-white rounded-lg p-6 max-w-md w-full mx-4 shadow-2xl">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold text-gray-900">Upload Employee Photo</h3>
          <button
            @click="closePhotoUpload"
            class="text-burgundy hover:text-burgundy-dark text-2xl leading-none hover:bg-burgundy-50 rounded-full w-8 h-8 flex items-center justify-center transition-colors"
            title="Close"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Photo Uploader Component -->
        <EmployeePhotoUploader
          v-if="selectedPhotoEmployeeId"
          :employeeId="selectedPhotoEmployeeId"
          :editable="true"
          @photo-updated="handlePhotoUploaded"
          @photo-deleted="handlePhotoDeleted"
        />

        <div class="mt-4 text-sm text-burgundy text-center flex items-center justify-center gap-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          Upload a photo (max 5MB)
        </div>
        <p class="mt-1 text-xs text-burgundy-light text-center">Supported formats: JPG, PNG, GIF</p>

        <!-- Close Button -->
        <div class="mt-6 flex justify-end">
          <button
            @click="closePhotoUpload"
            class="px-4 py-2 bg-burgundy-100 text-burgundy rounded-lg hover:bg-burgundy-200 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import EmployeeDetailPage from '../components/EmployeeDetailPage.vue'
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import EmployeePhotoUploader from '@/components/EmployeePhotoUploader.vue'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api.ts'
import authService from "@/services/authService.ts";

// ========== INTERFACES ==========
interface Employee {
  id: string
  firstName: string
  lastName: string
  fullName?: string
  name?: string
  email: string
  workHoursPerDay: number
  active: boolean
  status?: string
  workload?: number
  estimatedHours?: number
  totalCards?: number
  activeOrders?: number
  teams?: any[]
}

interface NewEmployee {
  firstName: string
  lastName: string
  email: string
}

// ========== STATE ==========
const currentView = ref<'management' | 'planning'>('management')
const selectedEmployeeId = ref<string | null>(null)
const loading = ref(false)
const showAddForm = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])

// Photo upload modal state
const showPhotoUploadModal = ref(false)
const selectedPhotoEmployeeId = ref<string | null>(null)
const avatarKey = ref(0) // Key to force avatar reload

const employees = ref<Employee[]>([])

const error = ref<string | null>(null)

const newEmployee = ref<NewEmployee>({
  firstName: '',
  lastName: '',
  email: ''
})

/**
 * View employee details
 */
const viewEmployeeDetails = (employeeId: string) => {
  console.log(' Viewing employee details:', employeeId)
  currentView.value = 'planning'  //  Change the mode to scheduling
  selectedEmployeeId.value = employeeId
}

// ========== COMPUTED ==========
const stats = computed(() => ({
  total: employees.value.length,
  available: employees.value.filter(e => e.workload && e.workload < 0.8).length,
  busy: employees.value.filter(e => e.workload && e.workload >= 0.8 && e.workload < 1.0).length,
  overloaded: employees.value.filter(e => e.workload && e.workload >= 1.0).length
}))

const managementEmployees = computed(() => {
  return employees.value.filter(emp => emp)
})

const employeesWithWorkload = computed(() => {
  if (currentView.value !== 'planning') {
    return []
  }

  return employees.value.map(emp => ({
    ...emp,
    workload: emp.workload || Math.random() * 1.2,
    estimatedHours: emp.estimatedHours || Math.round((emp.workload || 0) * (emp.workHoursPerDay || 8)),
    totalCards: emp.totalCards || Math.floor(Math.random() * 200),
    activeOrders: emp.activeOrders || Math.floor(Math.random() * 5)
  }))
})

// ========== METHODS ==========

/**
 * Load employees
 */
//  Flag to prevent multiple calls
let isLoadingEmployees = false

/**
 * Load employees
 */
const loadEmployees = async () => {
  // Protection against multiple calls
  if (isLoadingEmployees) {
    console.log('⏭ Already loading employees, skipping...')
    return
  }

  isLoadingEmployees = true
  loading.value = true
  error.value = null

  try {
    console.log(' Loading employees...')

    const data = await authService.get('/api/employees')

    console.log(' API Response:', data)
    console.log(' Is Array?', Array.isArray(data))
    console.log(' Length:', Array.isArray(data) ? data.length : 'N/A')

    // Handle different response formats
    if (Array.isArray(data)) {
      employees.value = data
      console.log(' Loaded as array:', data.length, 'employees')
    } else if (data.content) {
      employees.value = data.content
      console.log(' Loaded from data.content:', data.content.length, 'employees')
    } else if (data.employees) {
      employees.value = data.employees
      console.log(' Loaded from data.employees:', data.employees.length, 'employees')
    } else {
      console.warn(' Unknown response format:', Object.keys(data))
      employees.value = []
    }

    console.log(' Final employees:', employees.value.length)

  } catch (err: any) {
    console.error(' Error loading employees:', err)
    error.value = err.message || 'Failed to load employees'
    employees.value = []
  } finally {
    loading.value = false
    isLoadingEmployees = false
    console.log(' Loading complete')
  }
}

/**
 * Add new employee
 */
/**
 * Add new employee
 * FIXED: Use API_ENDPOINTS.EMPLOYEES constant for correct URL
 */
const addEmployee = async () => {
    try {
      console.log(' Adding employee:', newEmployee.value)

      // FIXED: Use the predefined API_ENDPOINTS constant
      // This ensures consistency across the application
      const data = await authService.post(API_ENDPOINTS.EMPLOYEES, newEmployee.value)

      console.log(' Employee added successfully:', data)

      newEmployee.value = { firstName: '', lastName: '', email: '' }
      showAddForm.value = false
      await loadEmployees()
    } catch (error) {
      console.error(' Failed to add employee:', error)
      alert(`Error: ${error.message || 'Failed to add employee'}`)
    }
}

/**
 * Load planning for selected date
 */
const loadPlanningForSelectedDate = async () => {
  if (selectedEmployeeId.value && currentView.value === 'planning') {
    console.log(' Loading planning for date:', selectedDate.value)
  }
}

/**
 * View employee planning
 */
const viewEmployeePlanning = (employeeId: string) => {
  console.log(' Viewing employee planning:', employeeId)
  selectedEmployeeId.value = employeeId
}

/**
 * Handle back from employee detail
 */
const handleEmployeeBack = () => {
  selectedEmployeeId.value = null
  loadEmployees()
}

/**
 * Open photo upload modal
 */
const openPhotoUpload = (employeeId: string) => {
  console.log(' Opening photo upload for employee:', employeeId)
  selectedPhotoEmployeeId.value = employeeId
  showPhotoUploadModal.value = true
}

/**
 * Close photo upload modal
 */
const closePhotoUpload = () => {
  showPhotoUploadModal.value = false
  selectedPhotoEmployeeId.value = null
}

/**
 * Handle photo uploaded
 */
const handlePhotoUploaded = () => {
  closePhotoUpload()
  avatarKey.value++ //  Force reload
  setTimeout(() => loadEmployees(), 300)
}

/**
 * Handle photo deleted
 */
const handlePhotoDeleted = () => {
  console.log(' Photo deleted')
  // Close modal
  closePhotoUpload()
  // Refresh employee list
  setTimeout(() => {
    loadEmployees()
  }, 500)
}

// Mock teams for display (temporary)
const getEmployeeRole = (email: string): string => {
  if (email.includes('admin')) return 'Admin'
  if (email.includes('manager')) return 'Manager'
  if (email.includes('grader')) return 'Grader'
  if (email.includes('certifier')) return 'Certifier'
  if (email.includes('scanner')) return 'Scanner'
  if (email.includes('preparer')) return 'Preparer'
  return 'Employee'
}
// ========== LIFECYCLE ==========
onMounted(() => {
  console.log(' Employees component mounted')

  //  Charge once
  if (authService.isAuthenticated()) {
    loadEmployees()
  } else {
    console.log(' Not authenticated')
  }
})
</script>

<style scoped>
.employees-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: linear-gradient(135deg, #730d10 0%, #5a0a0d 100%);
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(115, 13, 16, 0.4);
}

.btn-secondary {
  background: white;
  color: #730d10;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  border: 2px solid #730d10;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #fdf2f2;
}

.input-field {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: #730d10;
  box-shadow: 0 0 0 3px rgba(115, 13, 16, 0.1);
}
</style>
