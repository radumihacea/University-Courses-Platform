<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/crud'
import type { RoomType } from '@/api/types'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'
import { enumOptions, roomTypeLabel } from '@/utils/labels'

const lookups = useLookups()
onMounted(() => lookups.ensure('buildings'))

const typeFilter = ref<RoomType | null>(null)
const params = computed(() => ({ type: typeFilter.value }))
const roomTypes = enumOptions(roomTypeLabel)

const columns: ColumnDef[] = [
  { title: 'Cod', key: 'code' },
  { title: 'Clădire', key: 'buildingName' },
  { title: 'Tip', key: 'type' },
  { title: 'Capacitate', key: 'capacity', align: 'end' },
]

const fields = computed<FieldDef[]>(() => [
  { key: 'code', label: 'Cod sală', required: true, cols: 6, hint: 'Ex: BT 6.02, G6' },
  { key: 'capacity', label: 'Capacitate (locuri)', type: 'number', required: true, min: 1, cols: 6 },
  { key: 'type', label: 'Tip', type: 'select', options: roomTypes, required: true, cols: 6 },
  { key: 'buildingId', label: 'Clădire', type: 'select', options: lookups.buildingOptions.value, required: true, cols: 6 },
])
</script>

<template>
  <CrudPage
    title="Săli"
    subtitle="Amfiteatre și laboratoare, cu tipul și capacitatea folosite de generatorul de orar."
    entity-name="sala"
    :api="api.rooms"
    :columns="columns"
    :fields="fields"
    :params="params"
    :new-item="() => ({ code: '', capacity: null, type: null, buildingId: null })"
    :item-label="(r) => String(r.code)"
    :reload-lookups="['rooms']"
  >
    <template #filters>
      <v-select
        v-model="typeFilter"
        :items="roomTypes"
        label="Tip"
        density="compact"
        hide-details
        clearable
        style="max-width: 220px"
      />
    </template>
    <template #[`item.type`]="{ item }">
      <v-chip size="small" variant="tonal" :color="item.type === 'LAB' ? 'success' : 'primary'">
        {{ roomTypeLabel[item.type as RoomType] }}
      </v-chip>
    </template>
  </CrudPage>
</template>
