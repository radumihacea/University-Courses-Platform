<script setup lang="ts">
import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'

const columns: ColumnDef[] = [
  { title: 'Nume', key: 'fullName' },
  { title: 'Titlu', key: 'title' },
  { title: 'Email', key: 'email' },
  { title: 'Departament', key: 'department' },
]

const fields: FieldDef[] = [
  { key: 'fullName', label: 'Nume complet', required: true, cols: 6 },
  { key: 'title', label: 'Titlu didactic', cols: 6, hint: 'Ex: Prof. dr. ing.' },
  { key: 'email', label: 'Email (folosit la autentificare)', type: 'email', required: true, cols: 6 },
  { key: 'password', label: 'Parolă', type: 'password', requiredOnCreate: true, cols: 6 },
  { key: 'department', label: 'Departament' },
]
</script>

<template>
  <CrudPage
    title="Profesori"
    subtitle="Titularii de curs și asistenții. Fiecare are un cont cu care se autentifică pe mobil."
    entity-name="profesorul"
    :api="api.professors"
    :columns="columns"
    :fields="fields"
    :new-item="() => ({ fullName: '', title: '', email: '', password: '', department: 'Automatică' })"
    :item-label="(p) => String(p.fullName)"
    :reload-lookups="['professors']"
  >
    <template #[`item.fullName`]="{ item }">
      <div class="d-flex align-center ga-3 py-2">
        <v-avatar color="primary" variant="tonal" size="32">
          <span class="text-caption font-weight-bold">{{ String(item.fullName).slice(0, 2).toUpperCase() }}</span>
        </v-avatar>
        <span class="font-weight-medium">{{ item.fullName }}</span>
      </div>
    </template>
  </CrudPage>
</template>
