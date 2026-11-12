<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import { errorMessage } from '@/api/http'
import { scheduleApi } from '@/api/schedule'
import type { ChangeRequest, Timetable } from '@/api/types'
import StatCard from '@/components/common/StatCard.vue'
import { useAuth } from '@/composables/useAuth'
import { useLiveNotifications } from '@/composables/useLiveNotifications'
import { useLookups } from '@/composables/useLookups'
import { useToast } from '@/composables/useToast'
import { formatDateTime, timetableStatusColor, timetableStatusLabel } from '@/utils/labels'

const auth = useAuth()
const lookups = useLookups()
const toast = useToast()
const { state: live } = useLiveNotifications()

const timetables = ref<Timetable[]>([])
const pending = ref<ChangeRequest[]>([])
const loading = ref(true)

const firstName = computed(() => (auth.user.value?.fullName ?? '').split(' ')[0] || 'Admin')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Bună dimineața'
  if (hour < 18) return 'Bună ziua'
  return 'Bună seara'
})

const today = new Date().toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

const published = computed(() => timetables.value.find((t) => t.status === 'PUBLISHED') ?? null)
const latest = computed(() => timetables.value[0] ?? null)

/** Ce ar trebui facut in continuare, in functie de starea orarului. */
const nextStep = computed(() => {
  if (!latest.value) {
    return { icon: 'mdi-auto-fix', color: 'primary', text: 'Nu există încă niciun orar. Generează primul orar.', to: '/orar', action: 'Generează' }
  }
  if (latest.value.status === 'DRAFT') {
    return { icon: 'mdi-publish', color: 'warning', text: `Orarul „${latest.value.name}” este ciornă. Verifică-l și publică-l.`, to: '/orar', action: 'Deschide' }
  }
  if (pending.value.length) {
    return { icon: 'mdi-swap-horizontal', color: 'info', text: `Ai ${pending.value.length} cereri de modificare de analizat.`, to: '/cereri', action: 'Vezi cererile' }
  }
  return { icon: 'mdi-check-decagram-outline', color: 'success', text: 'Totul este la zi: orarul este publicat și nu există cereri în așteptare.', to: '/orar', action: 'Vezi orarul' }
})

const shortcuts = [
  { title: 'Orar', subtitle: 'Generează și publică', icon: 'mdi-calendar-month-outline', to: '/orar' },
  { title: 'Cereri', subtitle: 'Aprobă sau respinge', icon: 'mdi-swap-horizontal', to: '/cereri' },
  { title: 'Asignări', subtitle: 'Ce se predă și cui', icon: 'mdi-clipboard-list-outline', to: '/asignari' },
  { title: 'Statistici', subtitle: 'Profesori și săli', icon: 'mdi-chart-box-outline', to: '/statistici' },
]

onMounted(async () => {
  try {
    const [tt, requests] = await Promise.all([
      scheduleApi.timetables(),
      api.changeRequests.list({ status: 'PENDING' }),
      lookups.ensure('professors', 'students', 'rooms', 'disciplines'),
    ])
    timetables.value = tt
    pending.value = requests
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <!-- Salut -->
    <div class="mb-8">
      <div class="text-body-2 text-medium-emphasis text-capitalize mb-1">{{ today }}</div>
      <h1 class="text-h4 font-weight-bold">{{ greeting }}, {{ firstName }}</h1>
      <p class="text-body-1 text-medium-emphasis mt-2">Iată o privire de ansamblu asupra platformei.</p>
    </div>

    <!-- Pasul urmator -->
    <v-alert
      v-if="!loading"
      :color="nextStep.color"
      variant="tonal"
      rounded="lg"
      class="mb-6"
      :icon="nextStep.icon"
    >
      <div class="d-flex flex-wrap align-center ga-3">
        <span class="text-body-1">{{ nextStep.text }}</span>
        <v-spacer />
        <v-btn :to="nextStep.to" :color="nextStep.color" variant="flat" size="small" append-icon="mdi-arrow-right">
          {{ nextStep.action }}
        </v-btn>
      </div>
    </v-alert>

    <!-- Cifre -->
    <v-row class="mb-4">
      <v-col cols="6" lg="3">
        <StatCard label="Studenți" :value="lookups.students.value.length" icon="mdi-school-outline" />
      </v-col>
      <v-col cols="6" lg="3">
        <StatCard label="Profesori" :value="lookups.professors.value.length" icon="mdi-account-tie-outline" color="info" />
      </v-col>
      <v-col cols="6" lg="3">
        <StatCard label="Discipline" :value="lookups.disciplines.value.length" icon="mdi-book-open-variant-outline" color="success" />
      </v-col>
      <v-col cols="6" lg="3">
        <StatCard label="Săli" :value="lookups.rooms.value.length" icon="mdi-door" color="warning" />
      </v-col>
    </v-row>

    <v-row>
      <!-- Scurtaturi -->
      <v-col cols="12" lg="7">
        <div class="text-subtitle-1 font-weight-bold mb-3">Acces rapid</div>
        <v-row dense>
          <v-col v-for="s in shortcuts" :key="s.to" cols="12" sm="6">
            <v-card :to="s.to" class="pa-4 shortcut" link>
              <div class="d-flex align-center ga-4">
                <v-avatar color="primary" variant="tonal" rounded="lg" size="44">
                  <v-icon :icon="s.icon" />
                </v-avatar>
                <div>
                  <div class="text-body-1 font-weight-bold">{{ s.title }}</div>
                  <div class="text-body-2 text-medium-emphasis">{{ s.subtitle }}</div>
                </div>
                <v-spacer />
                <v-icon icon="mdi-chevron-right" class="text-medium-emphasis" />
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- Orarul publicat -->
        <div class="text-subtitle-1 font-weight-bold mt-6 mb-3">Orarul curent</div>
        <v-card class="pa-4">
          <div v-if="published" class="d-flex align-center ga-4">
            <v-avatar color="success" variant="tonal" rounded="lg" size="44">
              <v-icon icon="mdi-calendar-check-outline" />
            </v-avatar>
            <div class="flex-grow-1 min-w-0">
              <div class="text-body-1 font-weight-bold text-truncate">{{ published.name }}</div>
              <div class="text-body-2 text-medium-emphasis">
                {{ published.entryCount }} activități · publicat {{ formatDateTime(published.publishedAt) }}
              </div>
            </div>
            <v-chip :color="timetableStatusColor[published.status]" variant="tonal" size="small">
              {{ timetableStatusLabel[published.status] }}
            </v-chip>
          </div>
          <div v-else class="text-body-2 text-medium-emphasis">Niciun orar publicat încă.</div>
        </v-card>
      </v-col>

      <!-- Activitate recenta -->
      <v-col cols="12" lg="5">
        <div class="text-subtitle-1 font-weight-bold mb-3">Activitate recentă</div>
        <v-card>
          <v-list v-if="live.items.length" lines="two" density="compact">
            <v-list-item v-for="n in live.items.slice(0, 6)" :key="n.id">
              <template #prepend>
                <v-icon :icon="n.read ? 'mdi-bell-outline' : 'mdi-bell-ring'" :color="n.read ? undefined : 'primary'" size="small" />
              </template>
              <v-list-item-title class="text-body-2 text-wrap">{{ n.message }}</v-list-item-title>
              <v-list-item-subtitle class="text-caption">{{ formatDateTime(n.createdAt) }}</v-list-item-subtitle>
            </v-list-item>
          </v-list>
          <div v-else class="pa-6 text-center text-body-2 text-medium-emphasis">
            <v-icon icon="mdi-bell-sleep-outline" size="32" class="mb-2" /><br />
            Nicio activitate încă. Cererile noi de la profesori apar aici în timp real.
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.shortcut {
  transition: border-color 0.15s ease, transform 0.15s ease;
}
.shortcut:hover {
  border-color: rgb(var(--v-theme-primary)) !important;
  transform: translateY(-2px);
}
</style>
