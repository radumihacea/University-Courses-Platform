<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    icon: string
    color?: string
    hint?: string
  }>(),
  { color: 'primary', hint: undefined },
)
</script>

<template>
  <v-card class="pa-4 h-100">
    <div class="d-flex align-center ga-4">
      <v-avatar :color="color" variant="tonal" rounded="lg" size="48">
        <v-icon :icon="icon" />
      </v-avatar>
      <div class="min-w-0">
        <div class="text-h5 font-weight-bold">{{ value }}</div>
        <div class="text-body-2 text-medium-emphasis">{{ label }}</div>
        <div v-if="hint" class="text-caption text-medium-emphasis text-truncate">{{ hint }}</div>
      </div>
    </div>
  </v-card>
</template>
