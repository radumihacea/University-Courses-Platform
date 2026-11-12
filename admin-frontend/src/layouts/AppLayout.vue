<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDisplay } from 'vuetify'

import { api } from '@/api/crud'
import NotificationBell from '@/components/notifications/NotificationBell.vue'
import { useAppTheme } from '@/composables/useAppTheme'
import { useAuth } from '@/composables/useAuth'
import { useLiveNotifications } from '@/composables/useLiveNotifications'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const { mdAndUp } = useDisplay()
const { isDark, toggle: toggleTheme } = useAppTheme()
const toast = useToast()
const { onNotification, start: startLive, stop: stopLive } = useLiveNotifications()
const auth = useAuth()

// Conexiunea WebSocket porneste doar dupa autentificare (layout-ul e vizibil doar atunci)
startLive()

function logout() {
  stopLive()
  auth.logout()
  router.push('/login')
}

const initials = computed(() =>
  (auth.user.value?.fullName ?? 'A')
    .split(' ')
    .map((w) => w.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase(),
)

const drawer = ref(true)

// ---------------------------------------------------------------- meniul
/** Paginile folosite zilnic - mereu vizibile. */
const mainItems = [
  { title: 'Acasă', icon: 'mdi-home-outline', to: '/acasa' },
  { title: 'Orar', icon: 'mdi-calendar-month-outline', to: '/orar' },
  { title: 'Cereri', icon: 'mdi-swap-horizontal', to: '/cereri', badge: true },
  { title: 'Statistici', icon: 'mdi-chart-box-outline', to: '/statistici' },
  { title: 'Notificări', icon: 'mdi-bell-outline', to: '/notificari' },
]

/** Paginile de date (CRUD) - grupate sub "Gestionare date", fara iconite, ca meniul sa fie aerisit. */
const dataItems = [
  { title: 'Asignări didactice', to: '/asignari' },
  { title: 'Restricții profesori', to: '/restrictii' },
  { title: 'Discipline', to: '/discipline' },
  { title: 'Profesori', to: '/profesori' },
  { title: 'Studenți', to: '/studenti' },
  { title: 'Serii', to: '/serii' },
  { title: 'Grupe', to: '/grupe' },
  { title: 'Semigrupe', to: '/semigrupe' },
  { title: 'Clădiri', to: '/cladiri' },
  { title: 'Săli', to: '/sali' },
]

// Grupul "Gestionare date" e deschis automat daca esti pe una din paginile lui
const openGroups = ref<string[]>(dataItems.some((i) => i.to === route.path) ? ['data'] : [])

// ---------------------------------------------------------------- cereri in asteptare
const pendingCount = ref(0)

async function loadPending() {
  try {
    pendingCount.value = (await api.changeRequests.list({ status: 'PENDING' })).length
  } catch {
    /* fara badge daca serverul nu raspunde */
  }
}

onMounted(loadPending)
watch(() => route.path, loadPending)

// O notificare noua primita live apare ca mesaj scurt si actualizeaza numarul de cereri
onNotification((n) => {
  toast.info(n.message)
  loadPending()
})

const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : ''))
</script>

<template>
  <v-navigation-drawer v-model="drawer" :permanent="mdAndUp" width="248" class="app-drawer">
    <!-- Sigla -->
    <router-link to="/acasa" class="brand d-flex align-center ga-3 px-5 py-5">
      <v-avatar color="primary" rounded="lg" size="36">
        <span class="text-subtitle-1 font-weight-bold">U</span>
      </v-avatar>
      <span class="text-subtitle-1 font-weight-bold">UCP Admin</span>
    </router-link>

    <v-list v-model:opened="openGroups" nav density="comfortable" class="px-3">
      <v-list-item
        v-for="item in mainItems"
        :key="item.to"
        :to="item.to"
        :prepend-icon="item.icon"
        :title="item.title"
        color="primary"
        rounded="lg"
        class="mb-1"
      >
        <template v-if="item.badge && pendingCount > 0" #append>
          <v-chip size="x-small" color="primary" variant="flat" class="font-weight-bold">{{ pendingCount }}</v-chip>
        </template>
      </v-list-item>

      <v-divider class="my-3" />

      <v-list-group value="data">
        <template #activator="{ props }">
          <v-list-item
            v-bind="props"
            prepend-icon="mdi-database-outline"
            title="Gestionare date"
            rounded="lg"
          />
        </template>
        <v-list-item
          v-for="item in dataItems"
          :key="item.to"
          :to="item.to"
          :title="item.title"
          color="primary"
          rounded="lg"
          density="compact"
          class="sub-item"
        />
      </v-list-group>
    </v-list>
  </v-navigation-drawer>

  <v-app-bar flat border="b" height="64">
    <v-app-bar-nav-icon v-if="!mdAndUp" @click="drawer = !drawer" />
    <v-app-bar-title class="font-weight-bold" :class="{ 'ml-2': mdAndUp }">{{ pageTitle }}</v-app-bar-title>

    <template #append>
      <NotificationBell />
      <v-btn
        :icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
        variant="text"
        :title="isDark ? 'Temă luminoasă' : 'Temă întunecată'"
        @click="toggleTheme"
      />
      <v-menu location="bottom end" offset="8">
        <template #activator="{ props }">
          <v-btn v-bind="props" variant="text" class="ml-1 mr-2 px-2" height="44">
            <v-avatar color="primary" size="32" class="mr-2">
              <span class="text-caption font-weight-bold">{{ initials }}</span>
            </v-avatar>
            <span class="d-none d-md-inline text-body-2">{{ auth.user.value?.fullName }}</span>
            <v-icon icon="mdi-chevron-down" size="small" class="ml-1" />
          </v-btn>
        </template>
        <v-card min-width="260">
          <v-card-item>
            <template #prepend>
              <v-avatar color="primary" size="40">
                <span class="font-weight-bold">{{ initials }}</span>
              </v-avatar>
            </template>
            <v-card-title class="text-body-1 font-weight-bold">{{ auth.user.value?.fullName }}</v-card-title>
            <v-card-subtitle>{{ auth.user.value?.email }}</v-card-subtitle>
          </v-card-item>
          <v-divider />
          <v-list density="compact" nav>
            <v-list-item prepend-icon="mdi-logout" title="Deconectare" base-color="error" @click="logout" />
          </v-list>
        </v-card>
      </v-menu>
    </template>
  </v-app-bar>

  <v-main>
    <v-container fluid class="pa-4 pa-md-6 page-container">
      <router-view v-slot="{ Component }">
        <v-fade-transition mode="out-in">
          <component :is="Component" />
        </v-fade-transition>
      </router-view>
    </v-container>
  </v-main>
</template>

<style scoped>
.app-drawer {
  border-right: 1px solid rgba(var(--v-border-color), 0.12) !important;
}
.app-drawer :deep(.v-navigation-drawer__content) {
  /* fara bara de derulare vizibila */
  scrollbar-width: none;
}
.app-drawer :deep(.v-navigation-drawer__content::-webkit-scrollbar) {
  display: none;
}
.brand {
  color: inherit;
  text-decoration: none;
}
/* Sub-paginile din "Gestionare date": aliniate sub text, fara iconite */
.sub-item {
  padding-inline-start: 52px !important;
  min-height: 36px !important;
}
.page-container {
  max-width: 1600px;
}
</style>
