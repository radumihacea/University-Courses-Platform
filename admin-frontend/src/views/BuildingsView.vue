<script setup lang="ts">
import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'

const columns: ColumnDef[] = [
  { title: 'Nume', key: 'name' },
  { title: 'Adresă', key: 'address' },
]

const fields: FieldDef[] = [
  { key: 'name', label: 'Nume', required: true },
  { key: 'address', label: 'Adresă', required: true },
]
</script>

<template>
  <CrudPage
    title="Clădiri"
    subtitle="Clădirile în care se află sălile facultății."
    entity-name="clădirea"
    :api="api.buildings"
    :columns="columns"
    :fields="fields"
    :new-item="() => ({ name: '', address: '' })"
    :item-label="(b) => String(b.name)"
    :reload-lookups="['buildings']"
  />
</template>
