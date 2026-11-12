<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import type { ConstraintType } from '@/api/types'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'
import { constraintTypeLabel, enumOptions } from '@/utils/labels'

const lookups = useLookups()
onMounted(() => lookups.ensure('professors', 'timeSlots'))

const professorFilter = ref<number | null>(null)
const params = computed(() => ({ professorId: professorFilter.value }))
const constraintTypes = enumOptions(constraintTypeLabel)

const columns: ColumnDef[] = [
  { title: 'Profesor', key: 'professorName' },
  { title: 'Interval', key: 'timeSlotLabel' },
  { title: 'Tip', key: 'type' },
  { title: 'Text original', key: 'originalText' },
]

const fields = computed<FieldDef[]>(() => [
  { key: 'professorId', label: 'Profesor', type: 'autocomplete', options: lookups.professorOptions.value, required: true },
  { key: 'timeSlotId', label: 'Interval orar', type: 'autocomplete', options: lookups.timeSlotOptions.value, required: true, cols: 6 },
  { key: 'type', label: 'Tip', type: 'select', options: constraintTypes, required: true, cols: 6 },
  {
    key: 'mandatory',
    label: 'Obligatorie',
    type: 'switch',
    hint: 'Obligatorie = generatorul nu folosește niciodată intervalul; altfel doar îl evită',
  },
  { key: 'originalText', label: 'Ce a scris profesorul (opțional)', type: 'textarea' },
])
</script>

<template>
  <CrudPage
    title="Restricții profesori"
    subtitle="Intervalele în care profesorii nu pot sau preferă să predea. Generatorul de orar le respectă."
    entity-name="restricția"
    :api="api.constraints"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ professorId: null, timeSlotId: null, type: 'UNAVAILABLE', mandatory: true, originalText: '' })"
    :item-label="(c) => `${c.professorName}, ${c.timeSlotLabel}`"
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
    </template>
    <template #[`item.type`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.type === 'PREFERRED' ? 'success' : item.mandatory ? 'error' : 'warning'"
      >
        {{ constraintTypeLabel[item.type as ConstraintType] }}{{ item.type === 'UNAVAILABLE' && !item.mandatory ? ' (evită)' : '' }}
      </v-chip>
    </template>
    <template #[`item.originalText`]="{ item }">
      <span class="text-medium-emphasis font-italic">{{ item.originalText || '—' }}</span>
    </template>
  </CrudPage>
</template>
