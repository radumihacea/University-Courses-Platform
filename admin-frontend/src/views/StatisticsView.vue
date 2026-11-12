<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { errorMessage } from '@/api/http'
import { scheduleApi } from '@/api/schedule'
import type { ProfessorLoad, RoomOccupancy, Timetable } from '@/api/types'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import { useToast } from '@/composables/useToast'
import { timetableStatusLabel } from '@/utils/labels'

const toast = useToast()

const timetables = ref<Timetable[]>([])
const selectedId = ref<number | null>(null)
const professorLoad = ref<ProfessorLoad[]>([])
const roomOccupancy = ref<RoomOccupancy[]>([])
const loading = ref(false)

const timetableOptions = computed(() =>
  timetables.value.map((t) => ({ title: `${t.name} (${timetableStatusLabel[t.status]})`, value: t.id })),
)

async function loadStats() {
  if (!selectedId.value) return
  loading.value = true
  try {
    const [load, rooms] = await Promise.all([
      scheduleApi.professorLoad(selectedId.value),
      scheduleApi.roomOccupancy(selectedId.value),
    ])
    professorLoad.value = load
    roomOccupancy.value = rooms
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
}

watch(selectedId, loadStats)

onMounted(async () => {
  try {
    timetables.value = await scheduleApi.timetables()
    const published = timetables.value.find((t) => t.status === 'PUBLISHED')
    selectedId.value = published?.id ?? timetables.value[0]?.id ?? null
  } catch (e) {
    toast.error(errorMessage(e))
  }
})

const maxHours = computed(() => Math.max(1, ...professorLoad.value.map((p) => p.weeklyHours)))
const totalHours = computed(() => professorLoad.value.reduce((sum, p) => sum + p.weeklyHours, 0))
const averageOccupancy = computed(() => {
  if (!roomOccupancy.value.length) return 0
  const sum = roomOccupancy.value.reduce((s, r) => s + r.occupancyPercent, 0)
  return Math.round(sum / roomOccupancy.value.length)
})
const busiestRoom = computed(() => roomOccupancy.value[0])

function occupancyColor(percent: number) {
  if (percent >= 60) return 'error'
  if (percent >= 35) return 'warning'
  return 'success'
}

const fmt = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(1))
</script>

<template>
  <div>
    <PageHeader
      title="Statistici"
      subtitle="Calculate în baza de date prin interogări de agregare (GROUP BY) pe orarul ales."
    >
      <template #actions>
        <v-select
          v-model="selectedId"
          :items="timetableOptions"
          label="Orar"
          density="compact"
          hide-details
          style="min-width: 320px"
          no-data-text="Niciun orar generat încă"
        />
      </template>
    </PageHeader>

    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

    <v-row class="mb-2">
      <v-col cols="12" sm="6" lg="3">
        <StatCard label="Profesori cu ore" :value="professorLoad.length" icon="mdi-account-tie-outline" />
      </v-col>
      <v-col cols="12" sm="6" lg="3">
        <StatCard label="Ore predate pe săptămână" :value="fmt(totalHours)" icon="mdi-clock-outline" color="info" />
      </v-col>
      <v-col cols="12" sm="6" lg="3">
        <StatCard label="Ocupare medie a sălilor" :value="`${averageOccupancy}%`" icon="mdi-door" color="success" />
      </v-col>
      <v-col cols="12" sm="6" lg="3">
        <StatCard
          label="Cea mai folosită sală"
          :value="busiestRoom ? busiestRoom.roomCode : '-'"
          icon="mdi-fire"
          color="warning"
          :hint="busiestRoom ? `${busiestRoom.occupancyPercent}% din sloturi` : undefined"
        />
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" lg="6">
        <v-card class="h-100">
          <v-card-title class="pa-5 pb-1 font-weight-bold">Încărcarea profesorilor</v-card-title>
          <v-card-subtitle class="px-5">Ore pe săptămână (activitățile din 2 în 2 săptămâni contează pe jumătate)</v-card-subtitle>
          <v-card-text class="pt-4">
            <div v-for="p in professorLoad" :key="p.professorId" class="mb-3">
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="font-weight-medium">{{ p.professorName }}</span>
                <span class="text-medium-emphasis">{{ fmt(p.weeklyHours) }} h · {{ p.activities }} activități</span>
              </div>
              <v-progress-linear
                :model-value="(p.weeklyHours / maxHours) * 100"
                color="primary"
                height="10"
                rounded
              />
            </div>
            <div v-if="!professorLoad.length && !loading" class="text-medium-emphasis">Nu există date.</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="6">
        <v-card class="h-100">
          <v-card-title class="pa-5 pb-1 font-weight-bold">Ocuparea sălilor</v-card-title>
          <v-card-subtitle class="px-5">Procent din cele 35 de sloturi săptămânale (luni-vineri, 8-22)</v-card-subtitle>
          <v-card-text class="pt-4">
            <div v-for="r in roomOccupancy" :key="r.roomId" class="mb-3">
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="font-weight-medium">{{ r.roomCode }} <span class="text-medium-emphasis">({{ r.capacity }} locuri)</span></span>
                <span class="text-medium-emphasis">{{ r.occupancyPercent }}% · {{ fmt(r.occupiedSlotsPerWeek) }}/{{ r.availableSlotsPerWeek }}</span>
              </div>
              <v-progress-linear
                :model-value="r.occupancyPercent"
                :color="occupancyColor(r.occupancyPercent)"
                height="10"
                rounded
              />
            </div>
            <div v-if="!roomOccupancy.length && !loading" class="text-medium-emphasis">Nu există date.</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
