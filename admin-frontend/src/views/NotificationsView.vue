<script setup lang="ts">
import { computed, onMounted } from 'vue'

import { api } from '@/api/crud'
import CrudPage from '@/components/crud/CrudPage.vue'
import type { ColumnDef, FieldDef } from '@/components/crud/types'
import { useLookups } from '@/composables/useLookups'
import { formatDateTime } from '@/utils/labels'

const lookups = useLookups()
onMounted(() => lookups.ensure('professors', 'students'))

/** userId -> nume, pentru afisarea destinatarului in tabel */
const userNames = computed(() => new Map(lookups.userOptions.value.map((o) => [o.value, o.title])))

const columns: ColumnDef[] = [
  { title: 'Destinatar', key: 'userId', width: 200 },
  { title: 'Mesaj', key: 'message' },
  { title: 'Trimisă', key: 'createdAt', width: 170 },
  { title: 'Stare', key: 'read', width: 110 },
]

const fields = computed<FieldDef[]>(() => [
  {
    key: 'userId',
    label: 'Destinatar',
    type: 'autocomplete',
    options: lookups.userOptions.value,
    required: true,
    hint: 'Dacă destinatarul este conectat, primește mesajul instant (WebSocket)',
  },
  { key: 'message', label: 'Mesaj', type: 'textarea', required: true },
  { key: 'read', label: 'Citită', type: 'switch', visible: (_item, isNew) => !isNew },
])
</script>

<template>
  <CrudPage
    title="Notificări"
    subtitle="Toate notificările trimise. Cele noi ajung live pe mobil prin WebSocket."
    entity-name="notificarea"
    :api="api.notifications"
    :columns="columns"
    :fields="fields"
    :new-item="() => ({ userId: null, message: '', read: false })"
    :item-label="(n) => String(n.message).slice(0, 40)"
  >
    <template #[`item.userId`]="{ item }">
      {{ userNames.get(item.userId as number) ?? (item.userId === 1 ? 'Admin' : `Utilizator #${item.userId}`) }}
    </template>
    <template #[`item.createdAt`]="{ item }">
      {{ formatDateTime(item.createdAt as string) }}
    </template>
    <template #[`item.read`]="{ item }">
      <v-chip size="small" variant="tonal" :color="item.read ? 'secondary' : 'primary'">
        {{ item.read ? 'Citită' : 'Necitită' }}
      </v-chip>
    </template>
  </CrudPage>
</template>
