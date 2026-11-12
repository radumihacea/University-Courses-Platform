<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { errorMessage } from '@/api/http'
import { scheduleApi } from '@/api/schedule'
import type { Conflict, GenerationResult, Room, ScheduleEntry, Timetable } from '@/api/types'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import TimetableGrid from '@/components/timetable/TimetableGrid.vue'
import { useLookups } from '@/composables/useLookups'
import { useToast } from '@/composables/useToast'
import {
  activityTypeColor,
  activityTypeLabel,
  formatDateTime,
  formatTime,
  parityLabel,
  roomTypeLabel,
  timetableStatusColor,
  timetableStatusLabel,
} from '@/utils/labels'

const toast = useToast()
const lookups = useLookups()

// ---------------------------------------------------------------- orare
const timetables = ref<Timetable[]>([])
const selectedId = ref<number | null>(null)
const selected = computed(() => timetables.value.find((t) => t.id === selectedId.value) ?? null)

const timetableOptions = computed(() =>
  timetables.value.map((t) => ({
    title: t.name,
    value: t.id,
    subtitle: `${timetableStatusLabel[t.status]} · ${formatDateTime(t.createdAt)}`,
  })),
)

async function loadTimetables(selectId?: number) {
  try {
    timetables.value = await scheduleApi.timetables()
    if (selectId !== undefined) {
      selectedId.value = selectId
    } else if (!timetables.value.some((t) => t.id === selectedId.value)) {
      const published = timetables.value.find((t) => t.status === 'PUBLISHED')
      selectedId.value = published?.id ?? timetables.value[0]?.id ?? null
    }
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

// ---------------------------------------------------------------- vizualizare
type ViewMode = 'subgroup' | 'professor' | 'room'
const viewMode = ref<ViewMode>('subgroup')
const subgroupId = ref<number | null>(null)
const professorId = ref<number | null>(null)
const roomId = ref<number | null>(null)

const entries = ref<ScheduleEntry[]>([])
const conflicts = ref<Conflict[]>([])
const loadingEntries = ref(false)

const viewOptions = computed(() => {
  if (viewMode.value === 'subgroup') return lookups.subgroupOptions.value
  if (viewMode.value === 'professor') return lookups.professorOptions.value
  return lookups.roomOptions.value
})

const viewSelection = computed({
  get: () => (viewMode.value === 'subgroup' ? subgroupId.value : viewMode.value === 'professor' ? professorId.value : roomId.value),
  set: (value: number | null) => {
    if (viewMode.value === 'subgroup') subgroupId.value = value
    else if (viewMode.value === 'professor') professorId.value = value
    else roomId.value = value
  },
})

async function loadEntries() {
  if (!selectedId.value || !viewSelection.value) {
    entries.value = []
    return
  }
  loadingEntries.value = true
  try {
    entries.value = await scheduleApi.entries(selectedId.value, {
      subgroupId: viewMode.value === 'subgroup' ? subgroupId.value : null,
      professorId: viewMode.value === 'professor' ? professorId.value : null,
      roomId: viewMode.value === 'room' ? roomId.value : null,
    })
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loadingEntries.value = false
  }
}

async function loadConflicts() {
  if (!selectedId.value) {
    conflicts.value = []
    return
  }
  try {
    conflicts.value = await scheduleApi.conflicts(selectedId.value)
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

watch(selectedId, () => {
  loadEntries()
  loadConflicts()
})
watch([viewMode, subgroupId, professorId, roomId], loadEntries)

// ---------------------------------------------------------------- generare
const generateDialog = ref(false)
const generating = ref(false)
const generateValid = ref(false)
const generateForm = ref({ name: '', academicYear: '2026-2027', semester: 1, attempts: 30 })
const lastResult = ref<GenerationResult | null>(null)

function openGenerate() {
  const now = new Date()
  generateForm.value.name = `Orar AIA an IV · ${now.toLocaleDateString('ro-RO')} ${now.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}`
  generateDialog.value = true
}

async function generate() {
  generating.value = true
  try {
    const result = await scheduleApi.generate({
      name: generateForm.value.name,
      academicYear: generateForm.value.academicYear,
      semester: Number(generateForm.value.semester),
      attempts: Number(generateForm.value.attempts),
    })
    lastResult.value = result
    generateDialog.value = false
    await loadTimetables(result.timetable.id)
    if (result.unplaced.length === 0) {
      toast.success(`Orar generat: toate cele ${result.totalActivities} activități au fost plasate.`)
    } else {
      toast.warning(`Orar generat cu ${result.unplaced.length} activități neplasate.`)
    }
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    generating.value = false
  }
}

// ---------------------------------------------------------------- publicare / stergere
const publishDialog = ref(false)
const publishing = ref(false)
const deleteDialog = ref(false)
const deleting = ref(false)

async function publish() {
  if (!selectedId.value) return
  publishing.value = true
  try {
    await scheduleApi.publish(selectedId.value)
    toast.success('Orarul a fost publicat. Studenții și profesorii au fost notificați.')
    publishDialog.value = false
    await loadTimetables(selectedId.value)
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    publishing.value = false
  }
}

async function removeTimetable() {
  if (!selectedId.value) return
  deleting.value = true
  try {
    await scheduleApi.deleteTimetable(selectedId.value)
    toast.success('Orarul a fost șters.')
    deleteDialog.value = false
    selectedId.value = null
    lastResult.value = null
    await loadTimetables()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    deleting.value = false
  }
}

// ---------------------------------------------------------------- detalii ora + sali libere
const detailEntry = ref<ScheduleEntry | null>(null)
const freeRooms = ref<Room[]>([])
const loadingFreeRooms = ref(false)

async function openDetails(entry: ScheduleEntry) {
  detailEntry.value = entry
  freeRooms.value = []
  loadingFreeRooms.value = true
  try {
    freeRooms.value = await scheduleApi.availableRooms({
      timetableId: entry.timetableId,
      timeSlotId: entry.timeSlotId,
      parity: entry.parity,
    })
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loadingFreeRooms.value = false
  }
}

const detailOpen = computed({
  get: () => detailEntry.value !== null,
  set: (v: boolean) => {
    if (!v) detailEntry.value = null
  },
})

// ---------------------------------------------------------------- statistici sus
const conflictLabel: Record<Conflict['type'], string> = {
  ROOM: 'Sală',
  PROFESSOR: 'Profesor',
  STUDENTS: 'Studenți',
  CAPACITY: 'Capacitate',
}

onMounted(async () => {
  await lookups.ensure('subgroups', 'professors', 'rooms')
  subgroupId.value = lookups.subgroupOptions.value[0]?.value ?? null
  professorId.value = lookups.professorOptions.value[0]?.value ?? null
  roomId.value = lookups.roomOptions.value[0]?.value ?? null
  await loadTimetables()
})
</script>

<template>
  <div>
    <PageHeader title="Orar" subtitle="Generează, verifică și publică orarul. Click pe o oră pentru detalii și săli libere.">
      <template #actions>
        <v-select
          v-model="selectedId"
          :items="timetableOptions"
          label="Versiunea de orar"
          density="compact"
          hide-details
          style="min-width: 320px"
          no-data-text="Niciun orar generat încă"
        >
          <template #item="{ props: itemProps, item }">
            <v-list-item v-bind="itemProps" :subtitle="item.raw.subtitle" />
          </template>
        </v-select>
        <v-btn
          v-if="selected && selected.status === 'DRAFT'"
          color="success"
          variant="flat"
          prepend-icon="mdi-publish"
          @click="publishDialog = true"
        >
          Publică
        </v-btn>
        <v-btn
          v-if="selected && selected.status !== 'PUBLISHED'"
          variant="text"
          color="error"
          icon="mdi-trash-can-outline"
          title="Șterge versiunea"
          @click="deleteDialog = true"
        />
        <v-btn color="primary" prepend-icon="mdi-auto-fix" @click="openGenerate">Generează orar</v-btn>
      </template>
    </PageHeader>

    <!-- Fara niciun orar -->
    <v-card v-if="timetables.length === 0" class="pa-10 text-center">
      <v-icon icon="mdi-calendar-blank-outline" size="56" color="primary" class="mb-4" />
      <h2 class="text-h6 font-weight-bold mb-2">Niciun orar generat încă</h2>
      <p class="text-body-2 text-medium-emphasis mb-6">
        Generatorul plasează automat toate asignările didactice, respectând restricțiile profesorilor,
        capacitatea sălilor și fără suprapuneri.
      </p>
      <v-btn color="primary" size="large" prepend-icon="mdi-auto-fix" @click="openGenerate">Generează primul orar</v-btn>
    </v-card>

    <template v-else-if="selected">
      <!-- Carduri sumar -->
      <v-row class="mb-2">
        <v-col cols="12" sm="6" lg="3">
          <StatCard label="Activități în orar" :value="selected.entryCount" icon="mdi-calendar-check-outline" />
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <StatCard
            label="Conflicte"
            :value="conflicts.length"
            :icon="conflicts.length ? 'mdi-alert-outline' : 'mdi-check-decagram-outline'"
            :color="conflicts.length ? 'error' : 'success'"
            :hint="conflicts.length ? 'Rezolvă-le înainte de publicare' : 'Orarul este valid'"
          />
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <StatCard
            label="Stare"
            :value="timetableStatusLabel[selected.status]"
            icon="mdi-flag-outline"
            :color="timetableStatusColor[selected.status]"
            :hint="selected.publishedAt ? `Publicat ${formatDateTime(selected.publishedAt)}` : `Creat ${formatDateTime(selected.createdAt)}`"
          />
        </v-col>
        <v-col cols="12" sm="6" lg="3">
          <StatCard
            label="An universitar"
            :value="selected.academicYear"
            icon="mdi-school-outline"
            color="secondary"
            :hint="`Semestrul ${selected.semester}`"
          />
        </v-col>
      </v-row>

      <!-- Rezultatul ultimei generari -->
      <v-alert
        v-if="lastResult && lastResult.timetable.id === selected.id"
        :type="lastResult.unplaced.length ? 'warning' : 'success'"
        variant="tonal"
        rounded="lg"
        class="mb-4"
        closable
        @click:close="lastResult = null"
      >
        <div class="font-weight-bold">
          {{ lastResult.placed }} / {{ lastResult.totalActivities }} activități plasate în {{ lastResult.durationMs }} ms
          ({{ lastResult.attempts }} încercări, scor {{ lastResult.score }} - mai mic înseamnă mai puține ore târzii și ferestre).
        </div>
        <ul v-if="lastResult.unplaced.length" class="mt-2 ml-4">
          <li v-for="u in lastResult.unplaced" :key="u">{{ u }}</li>
        </ul>
      </v-alert>

      <!-- Conflicte -->
      <v-expansion-panels v-if="conflicts.length" class="mb-4">
        <v-expansion-panel rounded="lg">
          <v-expansion-panel-title>
            <v-icon icon="mdi-alert-outline" color="error" class="mr-2" />
            {{ conflicts.length }} conflicte găsite
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-list density="compact">
              <v-list-item v-for="(c, i) in conflicts" :key="i">
                <template #prepend>
                  <v-chip size="small" color="error" variant="tonal" class="mr-3">{{ conflictLabel[c.type] }}</v-chip>
                </template>
                <v-list-item-title class="text-wrap text-body-2">{{ c.description }}</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <!-- Grila -->
      <v-card>
        <div class="d-flex flex-wrap align-center ga-3 pa-4">
          <v-btn-toggle v-model="viewMode" mandatory density="compact" variant="outlined" divided rounded="lg" color="primary">
            <v-btn value="subgroup" prepend-icon="mdi-account-multiple-outline">Semigrupă</v-btn>
            <v-btn value="professor" prepend-icon="mdi-account-tie-outline">Profesor</v-btn>
            <v-btn value="room" prepend-icon="mdi-door">Sală</v-btn>
          </v-btn-toggle>
          <v-autocomplete
            v-model="viewSelection"
            :items="viewOptions"
            density="compact"
            hide-details
            style="max-width: 280px; min-width: 220px"
          />
          <v-spacer />
          <div class="d-flex flex-wrap ga-2">
            <v-chip
              v-for="(label, type) in activityTypeLabel"
              :key="type"
              size="small"
              variant="tonal"
              :color="activityTypeColor[type]"
            >
              {{ label }}
            </v-chip>
            <v-chip size="small" variant="outlined">S.I. / S.P. = săpt. impare / pare</v-chip>
          </div>
        </div>
        <v-divider />
        <div class="pa-2">
          <TimetableGrid
            :entries="entries"
            :loading="loadingEntries"
            :detail="viewMode === 'subgroup' ? 'professor' : 'target'"
            @select="openDetails"
          />
        </div>
      </v-card>
    </template>

    <!-- Dialog generare -->
    <v-dialog v-model="generateDialog" max-width="520" persistent>
      <v-card>
        <v-card-title class="pa-5 pb-2 text-h6 font-weight-bold">Generează un orar nou</v-card-title>
        <v-form v-model="generateValid" @submit.prevent="generate">
          <v-card-text>
            <p class="text-body-2 text-medium-emphasis mb-4">
              Algoritmul plasează toate asignările didactice fără suprapuneri (profesor, sală, studenți),
              respectând capacitatea sălilor și indisponibilitățile profesorilor. Rezultatul este o ciornă
              pe care o poți verifica înainte de publicare.
            </p>
            <v-text-field v-model="generateForm.name" label="Nume" :rules="[(v: string) => !!v || 'Câmp obligatoriu']" />
            <v-row dense>
              <v-col cols="6">
                <v-text-field
                  v-model="generateForm.academicYear"
                  label="An universitar"
                  :rules="[(v: string) => /^\d{4}-\d{4}$/.test(v) || 'Format: 2026-2027']"
                />
              </v-col>
              <v-col cols="6">
                <v-select v-model="generateForm.semester" label="Semestru" :items="[1, 2]" />
              </v-col>
            </v-row>
            <v-slider
              v-model="generateForm.attempts"
              label="Încercări"
              :min="1"
              :max="100"
              :step="1"
              thumb-label
              color="primary"
              hide-details
            >
              <template #append>
                <span class="text-body-2" style="width: 32px">{{ generateForm.attempts }}</span>
              </template>
            </v-slider>
            <p class="text-caption text-medium-emphasis mt-1">
              Mai multe încercări = rezultat mai bun (mai puține ferestre și ore târzii), dar durează mai mult.
            </p>
          </v-card-text>
          <v-card-actions class="pa-5 pt-0">
            <v-spacer />
            <v-btn variant="text" :disabled="generating" @click="generateDialog = false">Renunță</v-btn>
            <v-btn type="submit" color="primary" variant="flat" :loading="generating" :disabled="!generateValid">
              Generează
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card>
    </v-dialog>

    <!-- Detalii ora -->
    <v-dialog v-model="detailOpen" max-width="520">
      <v-card v-if="detailEntry">
        <v-card-title class="d-flex align-center pa-5 pb-1">
          <v-chip :color="activityTypeColor[detailEntry.type]" variant="tonal" size="small" class="mr-3">
            {{ activityTypeLabel[detailEntry.type] }}
          </v-chip>
          <span class="text-h6 font-weight-bold">{{ detailEntry.disciplineCode }}</span>
          <v-spacer />
          <v-btn icon="mdi-close" variant="text" size="small" @click="detailOpen = false" />
        </v-card-title>
        <v-card-text>
          <div class="text-body-1 mb-4">{{ detailEntry.disciplineName }}</div>
          <v-list density="compact" class="pa-0">
            <v-list-item prepend-icon="mdi-clock-outline" :title="`${detailEntry.dayName}, ${formatTime(detailEntry.startTime)} - ${formatTime(detailEntry.endTime)}`" :subtitle="parityLabel[detailEntry.parity]" />
            <v-list-item prepend-icon="mdi-map-marker-outline" :title="`Sala ${detailEntry.roomCode}`" />
            <v-list-item prepend-icon="mdi-account-tie-outline" :title="detailEntry.professorName" />
            <v-list-item prepend-icon="mdi-account-multiple-outline" :title="detailEntry.targetName ?? '-'" />
          </v-list>
          <v-divider class="my-3" />
          <div class="text-subtitle-2 font-weight-bold mb-2">Săli libere în același interval</div>
          <v-progress-linear v-if="loadingFreeRooms" indeterminate color="primary" />
          <div v-else-if="freeRooms.length" class="d-flex flex-wrap ga-2">
            <v-chip v-for="r in freeRooms" :key="r.id" size="small" variant="outlined">
              {{ r.code }} · {{ r.type ? roomTypeLabel[r.type] : '' }} · {{ r.capacity }} locuri
            </v-chip>
          </div>
          <div v-else class="text-body-2 text-medium-emphasis">Nicio sală liberă.</div>
        </v-card-text>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="publishDialog"
      title="Publică orarul"
      text="Orarul devine vizibil în aplicația mobilă, iar toți studenții și profesorii primesc o notificare. Orarul publicat anterior va fi arhivat."
      confirm-text="Publică"
      color="success"
      :loading="publishing"
      @confirm="publish"
    />
    <ConfirmDialog
      v-model="deleteDialog"
      title="Șterge versiunea de orar"
      text="Se șterg toate orele acestei versiuni și cererile de modificare legate de ea."
      confirm-text="Șterge"
      :loading="deleting"
      @confirm="removeTimetable"
    />
  </div>
</template>
