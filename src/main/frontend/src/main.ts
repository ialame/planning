// src/main.ts
// Application entry point with OAuth2 setup

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Tailwind CSS
import './style.css'

console.log(' Starting Pokemon Card Planning App...')
console.log(' OAuth2/OIDC authentication enabled')

// Create Vue app
const app = createApp(App)

// Use router
app.use(router)

// Global error handler
app.config.errorHandler = (err, instance, info) => {
  console.error(' Vue Error:', err)
  console.error('Info:', info)
}

// Mount app
app.mount('#app')

console.log(' Vue.js application mounted successfully')
