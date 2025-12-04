<template>
  <div class="photo-uploader">
    <!-- Photo Display -->
    <div class="photo-container">
      <div v-if="photoUrl" class="photo-wrapper">
        <img :src="photoUrl" alt="Employee Photo" class="photo-image" />
        <div class="photo-overlay">
          <button @click="triggerFileInput" class="overlay-button" title="Change Photo">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
          </button>
          <button @click="deletePhoto" class="overlay-button delete" title="Delete Photo">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </div>

      <div v-else class="photo-placeholder" @click="triggerFileInput">
        <div class="placeholder-content">
          <span class="placeholder-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
          </span>
          <span class="placeholder-text">Add Photo</span>
        </div>
      </div>
    </div>

    <!-- Hidden File Input -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      @change="handleFileSelect"
      style="display: none"
    />

    <!-- Upload Progress -->
    <div v-if="uploading" class="upload-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <p class="progress-text">Uploading... {{ uploadProgress }}%</p>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="error-message">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="display: inline; margin-right: 8px;">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
      </svg>
      {{ error }}
    </div>

    <!-- Success Message -->
    <div v-if="success" class="success-message">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="display: inline; margin-right: 8px;">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
      </svg>
      {{ success }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface Props {
  employeeId: string
  size?: 'small' | 'medium' | 'large'
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  editable: true
})

const emit = defineEmits<{
  photoUpdated: []
  photoDeleted: []
}>()

// State
const photoUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const error = ref<string | null>(null)
const success = ref<string | null>(null)

// Load photo on mount
onMounted(() => {
  loadPhoto()
})

// Watch for employee ID changes
watch(() => props.employeeId, () => {
  loadPhoto()
})

/**
 * Load employee photo from server
 */
const loadPhoto = async () => {
  try {
    console.log(` Loading photo for employee: ${props.employeeId}`)

    // First check if photo exists
    const existsResponse = await fetch(
      `${API_BASE_URL}/api/employees/${props.employeeId}/photo/exists`
    )

    if (!existsResponse.ok) {
      photoUrl.value = null
      return
    }

    const existsData = await existsResponse.json()

    if (!existsData.hasPhoto) {
      console.log('No photo found for employee')
      photoUrl.value = null
      return
    }

    // Get the photo URL
    const photoResponse = await fetch(
      `${API_BASE_URL}/api/employees/${props.employeeId}/photo`
    )

    if (photoResponse.ok) {
      const data = await photoResponse.json()
      photoUrl.value = data.photoUrl
      console.log(' Photo loaded successfully')
    } else {
      photoUrl.value = null
    }
  } catch (err) {
    console.error('Error loading photo:', err)
    photoUrl.value = null
  }
}

/**
 * Trigger file input click
 */
const triggerFileInput = () => {
  if (props.editable && fileInput.value) {
    fileInput.value.click()
  }
}

/**
 * Handle file selection
 */
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (file) {
    uploadPhoto(file)
  }
}

/**
 * Upload photo to server
 */
const uploadPhoto = async (file: File) => {
  try {
    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      showError('File size must be less than 5MB')
      return
    }

    // Validate file type
    if (!file.type.startsWith('image/')) {
      showError('Please select a valid image file')
      return
    }

    uploading.value = true
    uploadProgress.value = 0

    const formData = new FormData()
    formData.append('file', file)

    // Use XMLHttpRequest for progress tracking
    const xhr = new XMLHttpRequest()

    // Track upload progress
    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        uploadProgress.value = Math.round((e.loaded / e.total) * 100)
      }
    })

    // Handle completion
    xhr.addEventListener('load', async () => {
      if (xhr.status === 200) {
        const response = JSON.parse(xhr.responseText)
        if (response.success) {
          showSuccess('Photo uploaded successfully')
          await loadPhoto()
          emit('photoUpdated')
        } else {
          showError(response.error || 'Upload failed')
        }
      } else {
        showError('Upload failed. Please try again.')
      }
      uploading.value = false
    })

    // Handle errors
    xhr.addEventListener('error', () => {
      showError('Upload failed. Please check your connection.')
      uploading.value = false
    })

    // Send request
    xhr.open('POST', `${API_BASE_URL}/api/employees/${props.employeeId}/photo`)
    xhr.send(formData)

  } catch (err) {
    console.error('Upload error:', err)
    showError('Upload failed. Please try again.')
    uploading.value = false
  }
}

/**
 * Delete photo
 */
const deletePhoto = async () => {
  if (!confirm('Are you sure you want to delete this photo?')) {
    return
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/employees/${props.employeeId}/photo`, {
      method: 'DELETE'
    })

    if (response.ok) {
      photoUrl.value = null
      showSuccess('Photo deleted successfully')
      emit('photoDeleted')
    } else {
      showError('Failed to delete photo')
    }
  } catch (err) {
    console.error('Delete error:', err)
    showError('Failed to delete photo')
  }
}

/**
 * Show error message
 */
const showError = (message: string) => {
  error.value = message
  setTimeout(() => {
    error.value = null
  }, 5000)
}

/**
 * Show success message
 */
const showSuccess = (message: string) => {
  success.value = message
  setTimeout(() => {
    success.value = null
  }, 3000)
}
</script>

<style scoped>
.photo-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.photo-container {
  position: relative;
  width: 200px;
  height: 200px;
}

.photo-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(115, 13, 16, 0.1);
  border: 3px solid #730d10;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  inset: 0;
  background: rgba(115, 13, 16, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.3s;
}

.photo-wrapper:hover .photo-overlay {
  opacity: 1;
}

.overlay-button {
  background: white;
  border: none;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #730d10;
}

.overlay-button:hover {
  transform: scale(1.1);
  background: #730d10;
  color: white;
}

.overlay-button.delete {
  background: #ef4444;
  color: white;
}

.overlay-button.delete:hover {
  background: #dc2626;
}

.photo-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #730d10 0%, #5a0a0d 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 3px solid #fae6e6;
}

.photo-placeholder:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 15px rgba(115, 13, 16, 0.3);
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
}

.placeholder-icon {
  opacity: 0.8;
}

.placeholder-text {
  font-size: 14px;
  font-weight: 500;
  opacity: 0.9;
}

.upload-progress {
  width: 100%;
  max-width: 300px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #730d10 0%, #5a0a0d 100%);
  transition: width 0.3s ease;
}

.progress-text {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

.error-message {
  padding: 12px 16px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.success-message {
  padding: 12px 16px;
  background: #fdf2f2;
  color: #730d10;
  border: 1px solid #f5bfc0;
  border-radius: 6px;
  font-size: 14px;
  display: flex;
  align-items: center;
}
</style>