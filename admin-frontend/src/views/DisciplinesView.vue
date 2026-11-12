<script setup lang="ts">
import { computed, ref } from 'vue'

import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'

const optionalFilter = ref<boolean | null>(null)
const params = computed(() => ({ optional: optionalFilter.value }))

const columns: ColumnDef[] = [
  { title: 'Cod', key: 'code', width: 100 },
  { title: 'Denumire', key: 'name' },
  { title: 'An', key: 'studyYear', align: 'center', width: 80 },
  { title: 'Credite', key: 'credits', align: 'center', width: 100 },
  { title: 'Tip', key: 'optional', width: 140 },
]

const fields: FieldDef[] = [
  { key: 'code', label: 'Cod', required: true, cols: 4, hint: 'Ex: SCD' },
  { key: 'name', label: 'Denumire', required: true, cols: 8 },
  { key: 'studyYear', label: 'An de studiu', type: 'number', required: true, min: 1, max: 6, cols: 6 },
  { key: 'credits', label: 'Credite', type: 'number', required: true, min: 1, max: 30, cols: 6 },
  {
    key: 'optional',
    label: 'Disciplină opțională',
    type: 'switch',
    hint: 'Studenții se înrolează la ea din aplicația mobilă',
  },
]
</script>

<template>
  <CrudPage
    title="Discipline"
    subtitle="Materiile din planul de învățământ."
    entity-name="disciplina"
    :api="api.disciplines"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ code: '', name: '', studyYear: 4, credits: 5, optional: false })"
    :item-label="(d) => `${d.code} - ${d.name}`"
    :reload-lookups="['disciplines']"
  >
    <template #filters>
      <v-btn-toggle v-model="optionalFilter" density="compact" variant="outlined" divided rounded="lg">
        <v-btn :value="false">Obligatorii</v-btn>
        <v-btn :value="true">Opționale</v-btn>
      </v-btn-toggle>
    </template>
    <template #[`item.optional`]="{ item }">
      <v-chip size="small" variant="tonal" :color="item.optional ? 'info' : 'secondary'">
        {{ item.optional ? 'Opțională' : 'Obligatorie' }}
      </v-chip>
    </template>
  </CrudPage>
</template>
