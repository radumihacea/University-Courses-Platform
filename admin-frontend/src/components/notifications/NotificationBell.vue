<script setup lang="ts">
import { useLiveNotifications } from '@/composables/useLiveNotifications'
import { formatDateTime } from '@/utils/labels'

const { state, unreadCount, markAllRead } = useLiveNotifications()
</script>

<template>
  <v-menu location="bottom end" :close-on-content-click="false" offset="8">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        icon
        variant="text"
        :title="state.connected ? 'Notificări (conectat live)' : 'Notificări (server deconectat)'"
      >
        <v-badge :model-value="unreadCount > 0" :content="unreadCount" color="primary">
          <v-icon icon="mdi-bell-outline" />
        </v-badge>
        <!-- punct verde = conexiune live activa, gri = serverul nu raspunde -->
        <span class="live-dot" :class="state.connected ? 'bg-success' : 'bg-grey'" />
      </v-btn>
    </template>

    <v-card width="380" max-height="480" class="d-flex flex-column">
      <v-card-title class="d-flex align-center py-3">
        <span class="text-subtitle-1 font-weight-bold">Notificări</span>
        <v-chip
          size="x-small"
          variant="tonal"
          :color="state.connected ? 'success' : 'secondary'"
          class="ml-2"
        >
          {{ state.connected ? 'Live' : 'Deconectat' }}
        </v-chip>
        <v-spacer />
        <v-btn
          v-if="unreadCount > 0"
          size="small"
          variant="text"
          color="primary"
          @click="markAllRead"
        >
          Marchează citite
        </v-btn>
      </v-card-title>
      <v-divider />
      <div class="overflow-y-auto">
        <v-list v-if="state.items.length" lines="three" density="compact">
          <v-list-item
            v-for="n in state.items.slice(0, 30)"
            :key="n.id"
            :class="{ 'bg-surface-variant': !n.read }"
          >
            <template #prepend>
              <v-icon
                :icon="n.read ? 'mdi-bell-outline' : 'mdi-bell-ring'"
                :color="n.read ? undefined : 'primary'"
                size="small"
              />
            </template>
            <v-list-item-title class="text-body-2 text-wrap">{{ n.message }}</v-list-item-title>
            <v-list-item-subtitle class="text-caption">{{ formatDateTime(n.createdAt) }}</v-list-item-subtitle>
          </v-list-item>
        </v-list>
        <div v-else class="pa-6 text-center text-medium-emphasis text-body-2">
          Nicio notificare încă. Aici apar live cererile noi de la profesori.
        </div>
      </div>
    </v-card>
  </v-menu>
</template>

<style scoped>
.live-dot {
  position: absolute;
  bottom: 9px;
  left: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1.5px solid rgb(var(--v-theme-surface));
}
</style>
