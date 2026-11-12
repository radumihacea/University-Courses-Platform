<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import { api } from '@/api/crud'
import { errorMessage } from '@/api/http'
import { scheduleApi } from '@/api/schedule'
import type { ChangeRequest, RequestStatus } from '@/api/types'
import PageHeader from '@/components/common/PageHeader.vue'
import { useLiveNotifications } from '@/composables/useLiveNotifications'
import { useToast } from '@/composables/useToast'
import {
  activityTypeColor,
  activityTypeLabel,
  formatDateTime,
  parityLabel,
  requestStatusColor,
  requestStatusLabel,
} from '@/utils/labels'

const toast = useToast()
const { onNotification } = useLiveNotifications()

const status = ref<RequestStatus | 'ALL'>('PENDING')
const requests = ref<ChangeRequest[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    requests.value = await api.changeRequests.list({ status: status.value === 'ALL' ? null : status.value })
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
}

watch(status, load)
onMounted(load)

// Cand vine live o notificare despre o cerere noua, reincarcam lista
const stopListening = onNotification((n) => {
  if (n.message.startsWith('Cerere noua')) load()
})
onUnmounted(stopListening)

// ---------------------------------------------------------------- decizie
const decision = ref<{ request: ChangeRequest; approve: boolean } | null>(null)
const comment = ref('')
const deciding = ref(false)

const decisionOpen = computed({
  get: () => decision.value !== null,
  set: (v: boolean) => {
    if (!v) decision.value = null
  },
})

function openDecision(request: ChangeRequest, approve: boolean) {
  decision.value = { request, approve }
  comment.value = ''
}

async function decide() {
  if (!decision.value?.request.id) return
  deciding.value = true
  const { request, approve } = decision.value
  try {
    if (approve) {
      await scheduleApi.approveRequest(request.id as number, comment.value || null)
      toast.success('Cererea a fost aprobată: ora a fost mutată și cei afectați au fost notificați.')
    } else {
      await scheduleApi.rejectRequest(request.id as number, comment.value || null)
      toast.info('Cererea a fost respinsă, iar profesorul a fost notificat.')
    }
    decision.value = null
    await load()
  } catch (e) {
    // Ex: mutarea ar crea un conflict - backend-ul explica exact de ce
    toast.error(errorMessage(e))
  } finally {
    deciding.value = false
  }
}

const tabs: { value: RequestStatus | 'ALL'; title: string }[] = [
  { value: 'PENDING', title: 'În așteptare' },
  { value: 'APPROVED', title: 'Aprobate' },
  { value: 'REJECTED', title: 'Respinse' },
  { value: 'ALL', title: 'Toate' },
]
</script>

<template>
  <div>
    <PageHeader
      title="Cereri de modificare"
      subtitle="Profesorii cer mutarea unei ore din aplicația mobilă. La aprobare se verifică automat că mutarea nu creează conflicte."
    >
      <template #actions>
        <v-btn icon="mdi-refresh" variant="text" title="Reîncarcă" @click="load" />
      </template>
    </PageHeader>

    <v-tabs v-model="status" color="primary" class="mb-4">
      <v-tab v-for="t in tabs" :key="t.value" :value="t.value">{{ t.title }}</v-tab>
    </v-tabs>

    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />

    <v-card v-if="!loading && requests.length === 0" class="pa-10 text-center">
      <v-icon icon="mdi-inbox-outline" size="48" color="secondary" class="mb-3" />
      <div class="text-body-1 text-medium-emphasis">Nu există cereri în această categorie.</div>
    </v-card>

    <v-row>
      <v-col v-for="r in requests" :key="r.id" cols="12" md="6" xl="4">
        <v-card class="h-100 d-flex flex-column">
          <v-card-item>
            <template #prepend>
              <v-avatar color="primary" variant="tonal" rounded="lg">
                <v-icon icon="mdi-swap-horizontal" />
              </v-avatar>
            </template>
            <v-card-title class="d-flex align-center ga-2">
              <span class="font-weight-bold">{{ r.disciplineCode }}</span>
              <v-chip v-if="r.activityType" size="x-small" variant="tonal" :color="activityTypeColor[r.activityType]">
                {{ activityTypeLabel[r.activityType] }}
              </v-chip>
              <span class="text-body-2 text-medium-emphasis">{{ r.targetName }}</span>
            </v-card-title>
            <v-card-subtitle>{{ r.professorName }} · {{ formatDateTime(r.createdAt) }}</v-card-subtitle>
            <template #append>
              <v-chip v-if="r.status" size="small" variant="tonal" :color="requestStatusColor[r.status]">
                {{ requestStatusLabel[r.status] }}
              </v-chip>
            </template>
          </v-card-item>

          <v-card-text class="flex-grow-1">
            <div class="move d-flex align-center ga-3 pa-3 rounded-lg mb-3">
              <div class="flex-1-1">
                <div class="text-caption text-medium-emphasis">Acum</div>
                <div class="font-weight-medium">{{ r.currentTimeSlotLabel }}</div>
                <div class="text-caption">Sala {{ r.currentRoomCode }}</div>
              </div>
              <v-icon icon="mdi-arrow-right" color="primary" />
              <div class="flex-1-1">
                <div class="text-caption text-medium-emphasis">Propus</div>
                <div class="font-weight-medium">{{ r.proposedTimeSlotLabel }}</div>
                <div class="text-caption">Sala {{ r.proposedRoomCode ?? r.currentRoomCode }}</div>
              </div>
            </div>
            <div v-if="r.parity && r.parity !== 'ALL'" class="text-caption text-medium-emphasis mb-2">
              {{ parityLabel[r.parity] }}
            </div>
            <div class="text-body-2"><span class="text-medium-emphasis">Motiv:</span> {{ r.reason }}</div>
            <div v-if="r.adminComment" class="text-body-2 mt-2">
              <span class="text-medium-emphasis">Răspuns admin:</span> {{ r.adminComment }}
            </div>
          </v-card-text>

          <v-card-actions v-if="r.status === 'PENDING'" class="px-4 pb-4">
            <v-spacer />
            <v-btn variant="text" color="error" prepend-icon="mdi-close" @click="openDecision(r, false)">Respinge</v-btn>
            <v-btn variant="flat" color="success" prepend-icon="mdi-check" @click="openDecision(r, true)">Aprobă</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="decisionOpen" max-width="480">
      <v-card v-if="decision">
        <v-card-title class="pa-5 pb-2 text-h6 font-weight-bold">
          {{ decision.approve ? 'Aprobă cererea' : 'Respinge cererea' }}
        </v-card-title>
        <v-card-text>
          <p class="text-body-2 text-medium-emphasis mb-4">
            {{
              decision.approve
                ? 'Se verifică întâi că noul interval și noua sală sunt libere pentru profesor și studenți. Dacă da, ora este mutată și toți cei afectați primesc o notificare.'
                : 'Profesorul va primi o notificare cu motivul respingerii.'
            }}
          </p>
          <v-textarea v-model="comment" label="Comentariu (opțional)" rows="2" auto-grow />
        </v-card-text>
        <v-card-actions class="pa-5 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="decisionOpen = false">Renunță</v-btn>
          <v-btn
            :color="decision.approve ? 'success' : 'error'"
            variant="flat"
            :loading="deciding"
            @click="decide"
          >
            {{ decision.approve ? 'Aprobă' : 'Respinge' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.move {
  background: rgba(var(--v-theme-on-surface), 0.04);
}
</style>
