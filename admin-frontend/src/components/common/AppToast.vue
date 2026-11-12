<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { state } = useToast()

const icons = {
  success: 'mdi-check-circle-outline',
  error: 'mdi-alert-circle-outline',
  info: 'mdi-bell-ring-outline',
  warning: 'mdi-alert-outline',
} as const
</script>

<template>
  <v-snackbar
    v-model="state.visible"
    :color="state.color"
    :timeout="state.color === 'error' ? 7000 : 4000"
    location="bottom right"
    rounded="lg"
  >
    <div class="d-flex align-center ga-3">
      <v-icon :icon="icons[state.color]" />
      <span>{{ state.text }}</span>
    </div>
    <template #actions>
      <v-btn icon="mdi-close" variant="text" size="small" @click="state.visible = false" />
    </template>
  </v-snackbar>
</template>
