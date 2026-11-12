<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { errorMessage } from '@/api/http'
import { useAppTheme } from '@/composables/useAppTheme'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const { isDark, toggle: toggleTheme } = useAppTheme()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const valid = ref(false)
const loading = ref(false)
const error = ref<string | null>(route.query.expired ? 'Sesiunea a expirat. Autentifică-te din nou.' : null)

const required = (v: string) => !!v || 'Câmp obligatoriu'

async function submit() {
  if (!valid.value) return
  loading.value = true
  error.value = null
  try {
    await auth.login(email.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/acasa'
    await router.replace(redirect)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-main>
    <!-- Fundalul: element propriu, fix in spatele continutului -->
    <div class="login-bg" aria-hidden="true">
      <div class="blob blob-1" />
      <div class="blob blob-2" />
    </div>

    <div class="login-page">
    <v-btn
      class="theme-btn"
      :icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
      variant="text"
      :title="isDark ? 'Temă luminoasă' : 'Temă întunecată'"
      @click="toggleTheme"
    />

    <v-card class="pa-8" width="400" max-width="calc(100vw - 32px)">
      <div class="text-center mb-8">
        <v-avatar color="primary" rounded="lg" size="48" class="mb-4">
          <span class="text-h6 font-weight-bold">U</span>
        </v-avatar>
        <h1 class="text-h5 font-weight-bold">UCP Admin</h1>
      </div>

      <v-alert v-if="error" type="error" variant="tonal" rounded="lg" density="compact" class="mb-6">
        {{ error }}
      </v-alert>

      <v-form v-model="valid" @submit.prevent="submit">
        <v-text-field
          v-model="email"
          label="Email"
          type="email"
          autocomplete="username"
          :rules="[required]"
          class="mb-2"
          autofocus
        />
        <v-text-field
          v-model="password"
          label="Parolă"
          :type="showPassword ? 'text' : 'password'"
          :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          autocomplete="current-password"
          :rules="[required]"
          class="mb-4"
          @click:append-inner="showPassword = !showPassword"
        />
        <v-btn type="submit" color="primary" size="large" block :loading="loading" :disabled="!valid">
          Autentificare
        </v-btn>
      </v-form>
    </v-card>
    </div>
  </v-main>
</template>

<style scoped>
.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background-color: rgb(var(--v-theme-background));
  /* retea fina de puncte */
  background-image: radial-gradient(rgba(var(--v-theme-on-background), 0.12) 1px, transparent 1px);
  background-size: 22px 22px;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
}
.blob-1 {
  width: 560px;
  height: 560px;
  top: -200px;
  left: -180px;
  background: rgba(var(--v-theme-primary), 0.35);
}
.blob-2 {
  width: 480px;
  height: 480px;
  bottom: -220px;
  right: -160px;
  background: rgba(var(--v-theme-primary), 0.22);
}
.login-page {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.login-page :deep(.v-card) {
  background: rgba(var(--v-theme-surface), 0.85);
  backdrop-filter: blur(8px);
}
.theme-btn {
  position: absolute;
  top: 16px;
  right: 16px;
}
</style>
