<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'

const lookups = useLookups()
onMounted(() => lookups.ensure('groups'))

const groupFilter = ref<number | null>(null)
const params = computed(() => ({ groupId: groupFilter.value }))

const columns: ColumnDef[] = [
  { title: 'Semigrupă', key: 'name' },
  { title: 'Grupă', key: 'groupCode' },
  { title: 'Studenți', key: 'studentCount', align: 'end' },
]

const fields = computed<FieldDef[]>(() => [
  { key: 'name', label: 'Nume semigrupă', required: true, cols: 6, hint: 'Ex: 30141/1' },
  { key: 'groupId', label: 'Grupă', type: 'select', options: lookups.groupOptions.value, required: true, cols: 6 },
  {
    key: 'studentCount',
    label: 'Număr de studenți',
    type: 'number',
    required: true,
    min: 0,
    hint: 'Folosit la verificarea capacității sălilor',
  },
])
</script>

<template>
  <CrudPage
    title="Semigrupe"
    subtitle="Laboratoarele și proiectele se țin pe semigrupă."
    entity-name="semigrupa"
    :api="api.subgroups"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ name: '', groupId: null, studentCount: 14 })"
    :item-label="(s) => String(s.name)"
    :reload-lookups="['subgroups']"
  >
    <template #filters>
      <v-select
        v-model="groupFilter"
        :items="lookups.groupOptions.value"
        label="Grupă"
        density="compact"
        hide-details
        clearable
        style="max-width: 200px"
      />
    </template>
  </CrudPage>
</template>
