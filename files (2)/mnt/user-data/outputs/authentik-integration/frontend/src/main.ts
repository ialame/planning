// main.ts
// Application entry point

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Import Tailwind CSS
import './style.css'

console.log('🚀 Starting Pokemon Card Planning App...')
console.log('🔐 Authentication: Authentik OIDC')

// Create Vue app
const app = createApp(App)

// Use router
app.use(router)

// Mount app
app.mount('#app')

console.log('✅ Vue.js application mounted successfully')
