<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'

const lookups = useLookups()
onMounted(() => lookups.ensure('series'))

const seriesFilter = ref<number | null>(null)
const params = computed(() => ({ seriesId: seriesFilter.value }))

const columns: ColumnDef[] = [
  { title: 'Grupă', key: 'code' },
  { title: 'Serie', key: 'seriesName' },
]

const fields = computed<FieldDef[]>(() => [
  { key: 'code', label: 'Cod grupă', required: true, cols: 6, hint: 'Ex: 30141' },
  { key: 'seriesId', label: 'Serie', type: 'select', options: lookups.seriesOptions.value, required: true, cols: 6 },
])
</script>

<template>
  <CrudPage
    title="Grupe"
    subtitle="Seminariile se țin pe grupă."
    entity-name="grupa"
    :api="api.groups"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ code: '', seriesId: null })"
    :item-label="(g) => String(g.code)"
    :reload-lookups="['groups']"
  >
    <template #filters>
      <v-select
        v-model="seriesFilter"
        :items="lookups.seriesOptions.value"
        label="Serie"
        density="compact"
        hide-details
        clearable
        style="max-width: 200px"
      />
    </template>
  </CrudPage>
</template>
