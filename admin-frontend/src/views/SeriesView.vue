<script setup lang="ts">
import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'

const columns: ColumnDef[] = [
  { title: 'Serie', key: 'name' },
  { title: 'An de studiu', key: 'studyYear', align: 'center' },
  { title: 'Specializare', key: 'specialization' },
]

const fields: FieldDef[] = [
  { key: 'name', label: 'Nume serie', required: true, cols: 6, hint: 'Ex: IV-A' },
  { key: 'studyYear', label: 'An de studiu', type: 'number', required: true, min: 1, max: 6, cols: 6 },
  { key: 'specialization', label: 'Specializare', required: true },
]
</script>

<template>
  <CrudPage
    title="Serii"
    subtitle="Cursurile se țin pe serie (toate grupele seriei împreună)."
    entity-name="seria"
    :api="api.series"
    :columns="columns"
    :fields="fields"
    :new-item="() => ({ name: '', studyYear: 4, specialization: '' })"
    :item-label="(s) => String(s.name)"
    :reload-lookups="['series']"
  />
</template>
