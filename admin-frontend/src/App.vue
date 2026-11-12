<script setup lang="ts">
import AppToast from '@/components/common/AppToast.vue'
</script>

<template>
  <v-app>
    <router-view />
    <AppToast />
  </v-app>
</template>
