<script setup lang="ts">
// views/auth/SilentRenew.vue
// Handles silent token renewal in an iframe

import { onMounted } from 'vue'
import authService from '@/services/authService'

onMounted(async () => {
  try {
    console.log(' Processing silent renew callback...')
    await authService.handleSilentRenewCallback()
    console.log(' Silent renew completed')
  } catch (error) {
    console.error(' Silent renew error:', error)
  }
})
</script>

<template>
  <!-- This component is loaded in a hidden iframe for token renewal -->
  <div></div>
</template>
