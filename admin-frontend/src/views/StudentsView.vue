<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'

const lookups = useLookups()
onMounted(() => lookups.ensure('subgroups'))

const subgroupFilter = ref<number | null>(null)
const params = computed(() => ({ subgroupId: subgroupFilter.value }))

const columns: ColumnDef[] = [
  { title: 'Nr. matricol', key: 'registrationNumber', width: 140 },
  { title: 'Nume', key: 'fullName' },
  { title: 'Email', key: 'email' },
  { title: 'Semigrupă', key: 'subgroupName' },
]

const fields = computed<FieldDef[]>(() => [
  { key: 'fullName', label: 'Nume complet', required: true, cols: 6 },
  { key: 'registrationNumber', label: 'Număr matricol', required: true, cols: 6 },
  { key: 'email', label: 'Email (folosit la autentificare)', type: 'email', required: true, cols: 6 },
  { key: 'password', label: 'Parolă', type: 'password', requiredOnCreate: true, cols: 6 },
  {
    key: 'subgroupId',
    label: 'Semigrupă',
    type: 'autocomplete',
    options: lookups.subgroupOptions.value,
    required: true,
  },
])
</script>

<template>
  <CrudPage
    title="Studenți"
    subtitle="Fiecare student aparține unei semigrupe și își vede orarul în aplicația mobilă."
    entity-name="studentul"
    :api="api.students"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ fullName: '', registrationNumber: '', email: '', password: '', subgroupId: null })"
    :item-label="(s) => String(s.fullName)"
    :reload-lookups="['students']"
  >
    <template #filters>
      <v-autocomplete
        v-model="subgroupFilter"
        :items="lookups.subgroupOptions.value"
        label="Semigrupă"
        density="compact"
        hide-details
        clearable
        style="max-width: 200px"
      />
    </template>
  </CrudPage>
</template>
