<script setup lang="ts">
import { computed } from 'vue'

import type { DayOfWeek, ScheduleEntry } from '@/api/types'
import { activityTypeColor, activityTypeLabel, parityShort } from '@/utils/labels'

/**
 * Grila saptamanala a orarului: zilele pe coloane, intervalele de 2 ore pe randuri.
 * Activitatile din doua in doua saptamani apar cu eticheta S.I. (impare) / S.P. (pare).
 */
const props = defineProps<{
  entries: ScheduleEntry[]
  loading?: boolean
  /** Ce se afiseaza pe a doua linie: profesorul (vedere pe grupa) sau publicul (vedere pe profesor/sala). */
  detail: 'professor' | 'target'
}>()

const emit = defineEmits<{ select: [entry: ScheduleEntry] }>()

const days: { key: DayOfWeek; label: string }[] = [
  { key: 'MONDAY', label: 'Luni' },
  { key: 'TUESDAY', label: 'Marți' },
  { key: 'WEDNESDAY', label: 'Miercuri' },
  { key: 'THURSDAY', label: 'Joi' },
  { key: 'FRIDAY', label: 'Vineri' },
]

const hours = [8, 10, 12, 14, 16, 18, 20]

/** "MONDAY-8" -> activitatile din acel slot */
const byCell = computed(() => {
  const map = new Map<string, ScheduleEntry[]>()
  for (const entry of props.entries) {
    const key = `${entry.dayOfWeek}-${Number(entry.startTime.slice(0, 2))}`
    const list = map.get(key) ?? []
    list.push(entry)
    map.set(key, list)
  }
  return map
})

function cell(day: DayOfWeek, hour: number): ScheduleEntry[] {
  return byCell.value.get(`${day}-${hour}`) ?? []
}

const pad = (h: number) => String(h).padStart(2, '0')
</script>

<template>
  <div class="grid-wrapper">
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />
    <table class="tt-grid">
      <thead>
        <tr>
          <th class="hour-col" />
          <th v-for="day in days" :key="day.key">{{ day.label }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="hour in hours" :key="hour">
          <td class="hour-col">
            <div class="text-body-2 font-weight-bold">{{ pad(hour) }}:00</div>
            <div class="text-caption text-medium-emphasis">{{ pad(hour + 2) }}:00</div>
          </td>
          <td v-for="day in days" :key="day.key" class="tt-cell">
            <div class="d-flex flex-column ga-1">
              <button
                v-for="entry in cell(day.key, hour)"
                :key="entry.id"
                type="button"
                class="tt-entry"
                :class="[`tt-${activityTypeColor[entry.type]}`, { 'tt-half': entry.parity !== 'ALL' }]"
                :title="`${entry.disciplineName} - ${activityTypeLabel[entry.type]}`"
                @click="emit('select', entry)"
              >
                <div class="d-flex align-center ga-1">
                  <span class="font-weight-bold">{{ entry.disciplineCode }}</span>
                  <span class="text-caption">{{ activityTypeLabel[entry.type] }}</span>
                  <v-spacer />
                  <span v-if="entry.parity !== 'ALL'" class="parity-tag">{{ parityShort[entry.parity] }}</span>
                </div>
                <div class="text-caption text-truncate">
                  <v-icon icon="mdi-map-marker-outline" size="12" /> {{ entry.roomCode }}
                  <span class="mx-1">·</span>
                  {{ detail === 'professor' ? entry.professorName : entry.targetName }}
                </div>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.grid-wrapper {
  overflow-x: auto;
}
.tt-grid {
  width: 100%;
  min-width: 860px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}
.tt-grid th {
  text-align: left;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(var(--v-theme-on-surface), 0.65);
  padding: 10px 8px;
  border-bottom: 1px solid rgba(var(--v-border-color), 0.12);
}
.hour-col {
  width: 72px;
  vertical-align: top;
  padding: 10px 8px;
}
.tt-grid td {
  border-bottom: 1px solid rgba(var(--v-border-color), 0.08);
}
.tt-cell {
  vertical-align: top;
  padding: 6px;
  height: 84px;
  border-left: 1px solid rgba(var(--v-border-color), 0.08);
}
.tt-entry {
  width: 100%;
  text-align: left;
  border-radius: 8px;
  padding: 6px 8px;
  font-size: 0.82rem;
  line-height: 1.35;
  border-left: 4px solid currentColor;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
  cursor: pointer;
}
.tt-entry:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.tt-entry > div {
  color: rgb(var(--v-theme-on-surface));
}
.tt-primary {
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.1);
}
.tt-info {
  color: rgb(var(--v-theme-info));
  background: rgba(var(--v-theme-info), 0.1);
}
.tt-success {
  color: rgb(var(--v-theme-success));
  background: rgba(var(--v-theme-success), 0.1);
}
.tt-warning {
  color: rgb(var(--v-theme-warning));
  background: rgba(var(--v-theme-warning), 0.12);
}
.tt-half {
  border-left-style: dashed;
}
.parity-tag {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0 6px;
  border-radius: 6px;
  background: rgba(var(--v-theme-on-surface), 0.08);
}
</style>
