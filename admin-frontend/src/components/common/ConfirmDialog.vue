<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    text: string
    confirmText?: string
    color?: string
    loading?: boolean
  }>(),
  { confirmText: 'Confirmă', color: 'error', loading: false },
)

const open = defineModel<boolean>({ required: true })
const emit = defineEmits<{ confirm: [] }>()
</script>

<template>
  <v-dialog v-model="open" max-width="440">
    <v-card class="pa-2">
      <v-card-title class="text-h6 font-weight-bold">{{ title }}</v-card-title>
      <v-card-text class="text-body-1">{{ text }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="open = false">Renunță</v-btn>
        <v-btn :color="color" variant="flat" :loading="loading" @click="emit('confirm')">
          {{ confirmText }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
