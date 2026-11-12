<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import type { ActivityType, Frequency } from '@/api/types'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef, FormItem } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'
import { activityTypeColor, activityTypeLabel, enumOptions, frequencyLabel } from '@/utils/labels'

const lookups = useLookups()
onMounted(() => lookups.ensure('disciplines', 'professors', 'rooms', 'series', 'groups', 'subgroups'))

const professorFilter = ref<number | null>(null)
const typeFilter = ref<ActivityType | null>(null)
const params = computed(() => ({ professorId: professorFilter.value, type: typeFilter.value }))

const activityTypes = enumOptions(activityTypeLabel)
const frequencies = enumOptions(frequencyLabel)

const columns: ColumnDef[] = [
  { title: 'Disciplină', key: 'disciplineCode', width: 110 },
  { title: 'Tip', key: 'type', width: 120 },
  { title: 'Public', key: 'targetName' },
  { title: 'Profesor', key: 'professorName' },
  { title: 'Frecvență', key: 'frequency' },
  { title: 'Sală cerută', key: 'preferredRoomCode' },
]

const isType = (item: FormItem, ...types: ActivityType[]) => types.includes(item.type as ActivityType)

/** Publicul depinde de tip: curs -> serie, seminar -> grupă, laborator/proiect -> semigrupă. */
const fields = computed<FieldDef[]>(() => [
  { key: 'disciplineId', label: 'Disciplină', type: 'autocomplete', options: lookups.disciplineOptions.value, required: true },
  { key: 'type', label: 'Tip activitate', type: 'select', options: activityTypes, required: true, cols: 6 },
  { key: 'professorId', label: 'Profesor', type: 'autocomplete', options: lookups.professorOptions.value, required: true, cols: 6 },
  {
    key: 'seriesId',
    label: 'Serie',
    type: 'select',
    options: lookups.seriesOptions.value,
    required: true,
    visible: (item) => isType(item, 'COURSE'),
  },
  {
    key: 'groupId',
    label: 'Grupă',
    type: 'select',
    options: lookups.groupOptions.value,
    required: true,
    visible: (item) => isType(item, 'SEMINAR'),
  },
  {
    key: 'subgroupId',
    label: 'Semigrupă',
    type: 'autocomplete',
    options: lookups.subgroupOptions.value,
    required: true,
    visible: (item) => isType(item, 'LAB', 'PROJECT'),
  },
  { key: 'frequency', label: 'Frecvență', type: 'select', options: frequencies, required: true, cols: 6 },
  { key: 'hoursPerSession', label: 'Ore pe ședință', type: 'number', required: true, min: 1, max: 4, cols: 6 },
  {
    key: 'preferredRoomId',
    label: 'Sală cerută (opțional)',
    type: 'autocomplete',
    options: lookups.roomOptions.value,
    clearable: true,
    hint: 'Dacă e completată, generatorul folosește doar această sală',
  },
])
</script>

<template>
  <CrudPage
    title="Asignări didactice"
    subtitle="Tot ce trebuie pus în orar: ce disciplină, ce tip, cine o predă și cui. Generatorul le plasează automat."
    entity-name="asignarea"
    :api="api.assignments"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="
      () => ({
        disciplineId: null,
        professorId: null,
        type: 'LAB',
        hoursPerSession: 2,
        frequency: 'WEEKLY',
        preferredRoomId: null,
        seriesId: null,
        groupId: null,
        subgroupId: null,
      })
    "
    :item-label="(a) => `${a.disciplineCode} ${activityTypeLabel[a.type as ActivityType]} - ${a.targetName}`"
  >
    <template #filters>
      <v-autocomplete
        v-model="professorFilter"
        :items="lookups.professorOptions.value"
        label="Profesor"
        density="compact"
        hide-details
        clearable
        style="max-width: 240px"
      />
      <v-select
        v-model="typeFilter"
        :items="activityTypes"
        label="Tip"
        density="compact"
        hide-details
        clearable
        style="max-width: 170px"
      />
    </template>
    <template #[`item.disciplineCode`]="{ item }">
      <span class="font-weight-bold">{{ item.disciplineCode }}</span>
    </template>
    <template #[`item.type`]="{ item }">
      <v-chip size="small" variant="tonal" :color="activityTypeColor[item.type as ActivityType]">
        {{ activityTypeLabel[item.type as ActivityType] }}
      </v-chip>
    </template>
    <template #[`item.frequency`]="{ item }">
      {{ frequencyLabel[item.frequency as Frequency] }}
    </template>
    <template #[`item.preferredRoomCode`]="{ item }">
      {{ item.preferredRoomCode ?? '—' }}
    </template>
  </CrudPage>
</template>
