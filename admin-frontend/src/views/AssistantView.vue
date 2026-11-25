<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { aiApi } from '@/api/ai'
import { errorMessage } from '@/api/http'
import type { AiStatus, ChatMessage } from '@/api/types'
import PageHeader from '@/components/common/PageHeader.vue'
import { useLookups } from '@/composables/useLookups'

const lookups = useLookups()

// ---------------------------------------------------------------- al cui orar
type Mode = 'subgroup' | 'professor'
const mode = ref<Mode>('subgroup')
const subgroupId = ref<number | null>(null)
const professorId = ref<number | null>(null)

const options = computed(() =>
  mode.value === 'subgroup' ? lookups.subgroupOptions.value : lookups.professorOptions.value,
)
const selection = computed({
  get: () => (mode.value === 'subgroup' ? subgroupId.value : professorId.value),
  set: (v: number | null) => {
    if (mode.value === 'subgroup') subgroupId.value = v
    else professorId.value = v
  },
})

// ---------------------------------------------------------------- conversatia
interface UiMessage extends ChatMessage {
  error?: boolean
  meta?: string
  /** raspunsul inca se scrie */
  streaming?: boolean
}

const messages = ref<UiMessage[]>([])
const input = ref('')
const thinking = ref(false)
const scroller = ref<HTMLElement | null>(null)

const suggestions = computed(() =>
  mode.value === 'subgroup'
    ? ['Ce ore am azi?', 'Când am ore miercuri?', 'În ce sală e laboratorul de SCD?', 'Am ore vineri?']
    : ['Ce ore am azi?', 'Ce predau mâine?', 'În ce săli predau marți?', 'Câte ore am pe săptămână?'],
)

/** Derulare doar daca utilizatorul e deja jos (nu il tragem inapoi daca citeste mai sus). */
async function scrollToBottom(force = false) {
  const el = scroller.value
  const nearBottom = !el || el.scrollHeight - el.scrollTop - el.clientHeight < 120
  await nextTick()
  if (force || nearBottom) el?.scrollTo({ top: el.scrollHeight })
}

let controller: AbortController | null = null

async function send(text?: string) {
  const question = (text ?? input.value).trim()
  if (!question || thinking.value || !selection.value) return
  input.value = ''

  // conversatia de pana acum (fara mesajele de eroare), pentru intrebari de continuare
  const history: ChatMessage[] = messages.value
    .filter((m) => !m.error && m.content)
    .map((m) => ({ role: m.role, content: m.content }))

  messages.value.push({ role: 'user', content: question })
  // bula asistentului apare imediat (cu punctele animate) si se umple pe masura ce sosesc cuvintele
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const answer = messages.value[messages.value.length - 1]!
  thinking.value = true
  scrollToBottom(true)

  controller = new AbortController()
  try {
    const done = await aiApi.chatStream(
      {
        message: question,
        history,
        subgroupId: mode.value === 'subgroup' ? subgroupId.value : null,
        professorId: mode.value === 'professor' ? professorId.value : null,
      },
      (token) => {
        answer.content += token
        scrollToBottom()
      },
      controller.signal,
    )
    answer.content = answer.content.trim()
    const first = done.firstTokenMs != null ? `primul cuvânt după ${(done.firstTokenMs / 1000).toFixed(1)} s · ` : ''
    answer.meta = `${done.model} · ${first}total ${(done.durationMs / 1000).toFixed(1)} s`
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      answer.meta = 'oprit'
      if (!answer.content) answer.content = '(răspuns oprit)'
    } else if (answer.content) {
      // s-a intrerupt la jumatate: pastram ce a venit si aratam eroarea dedesubt
      answer.meta = errorMessage(e)
    } else {
      answer.content = errorMessage(e)
      answer.error = true
    }
  } finally {
    answer.streaming = false
    thinking.value = false
    controller = null
    scrollToBottom()
  }
}

/** Opreste raspunsul in curs (modelul se opreste si pe server). */
function stop() {
  controller?.abort()
}

function onKeydown(event: KeyboardEvent) {
  // Enter trimite, Shift+Enter face rand nou
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

// O alta persoana = o conversatie noua
watch([mode, subgroupId, professorId], () => {
  stop()
  messages.value = []
})

onBeforeUnmount(stop)

// ---------------------------------------------------------------- starea modelului
const status = ref<AiStatus | null>(null)
const statusLoading = ref(false)

async function loadStatus() {
  statusLoading.value = true
  try {
    status.value = await aiApi.status()
  } catch {
    status.value = null
  } finally {
    statusLoading.value = false
  }
}

const statusChip = computed(() => {
  if (!status.value || !status.value.ollamaRunning) {
    return { color: 'error', icon: 'mdi-robot-dead-outline', text: 'Ollama nu rulează' }
  }
  if (!status.value.modelReady) {
    return { color: 'warning', icon: 'mdi-download-outline', text: `Modelul ${status.value.model} nu e descărcat` }
  }
  return { color: 'success', icon: 'mdi-robot-happy-outline', text: `${status.value.model} pregătit` }
})

onMounted(async () => {
  loadStatus()
  await lookups.ensure('subgroups', 'professors')
  subgroupId.value = lookups.subgroupOptions.value[0]?.value ?? null
  professorId.value = lookups.professorOptions.value[0]?.value ?? null
})
</script>

<template>
  <div class="assistant-page d-flex flex-column">
    <PageHeader
      title="Asistent AI"
      subtitle="Chatbot-ul pe care studenții și profesorii îl folosesc în aplicația mobilă. Răspunde doar pe baza orarului publicat."
    >
      <template #actions>
        <v-chip :color="statusChip.color" variant="tonal" :prepend-icon="statusChip.icon">
          {{ statusChip.text }}
        </v-chip>
        <v-btn icon="mdi-refresh" variant="text" :loading="statusLoading" title="Verifică din nou" @click="loadStatus" />
      </template>
    </PageHeader>

    <v-card class="d-flex flex-column flex-grow-1 chat-card">
      <!-- Al cui orar -->
      <div class="d-flex flex-wrap align-center ga-3 pa-4">
        <span class="text-body-2 text-medium-emphasis">Testează ca:</span>
        <v-btn-toggle v-model="mode" mandatory density="compact" variant="outlined" divided rounded="lg" color="primary">
          <v-btn value="subgroup" prepend-icon="mdi-school-outline">Student</v-btn>
          <v-btn value="professor" prepend-icon="mdi-account-tie-outline">Profesor</v-btn>
        </v-btn-toggle>
        <v-autocomplete
          v-model="selection"
          :items="options"
          :label="mode === 'subgroup' ? 'Semigrupa studentului' : 'Profesor'"
          density="compact"
          hide-details
          style="max-width: 280px; min-width: 220px"
        />
        <v-spacer />
        <v-btn
          v-if="messages.length"
          variant="text"
          prepend-icon="mdi-broom"
          size="small"
          @click="messages = []"
        >
          Conversație nouă
        </v-btn>
      </div>
      <v-divider />

      <!-- Mesajele -->
      <div ref="scroller" class="messages flex-grow-1 pa-4 pa-md-6">
        <div v-if="!messages.length" class="empty d-flex flex-column align-center justify-center text-center">
          <v-avatar color="primary" variant="tonal" size="64" class="mb-4">
            <v-icon icon="mdi-robot-happy-outline" size="34" />
          </v-avatar>
          <div class="text-h6 font-weight-bold mb-1">Întreabă orice despre orar</div>
          <div class="text-body-2 text-medium-emphasis mb-6">
            Asistentul știe ziua de azi, dacă săptămâna e pară sau impară și orarul persoanei alese.
          </div>
          <div class="d-flex flex-wrap justify-center ga-2">
            <v-chip
              v-for="s in suggestions"
              :key="s"
              variant="outlined"
              prepend-icon="mdi-message-question-outline"
              :disabled="!selection"
              @click="send(s)"
            >
              {{ s }}
            </v-chip>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="d-flex mb-4" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
          <v-avatar v-if="m.role === 'assistant'" :color="m.error ? 'error' : 'primary'" variant="tonal" size="32" class="mr-3 mt-1">
            <v-icon :icon="m.error ? 'mdi-alert-outline' : 'mdi-robot-happy-outline'" size="18" />
          </v-avatar>
          <div>
            <div
              class="bubble"
              :class="m.role === 'user' ? 'bubble-user' : m.error ? 'bubble-error' : 'bubble-assistant'"
            >
              <span v-if="m.streaming && !m.content" class="d-inline-flex align-center ga-1">
                <span class="dot" /><span class="dot" /><span class="dot" />
                <span class="text-body-2 text-medium-emphasis ml-2">citește orarul...</span>
              </span>
              <template v-else>{{ m.content }}<span v-if="m.streaming" class="caret" /></template>
            </div>
            <div v-if="m.meta" class="text-caption text-medium-emphasis mt-1 ml-1">{{ m.meta }}</div>
          </div>
        </div>

      </div>

      <!-- Intrebarea -->
      <v-divider />
      <div class="d-flex align-end ga-2 pa-3">
        <v-textarea
          v-model="input"
          placeholder="Scrie o întrebare, de ex. „Când am laborator de SCD?”"
          rows="1"
          auto-grow
          max-rows="4"
          hide-details
          :disabled="!selection"
          @keydown="onKeydown"
        />
        <v-btn
          v-if="thinking"
          color="primary"
          variant="tonal"
          icon="mdi-stop"
          height="48"
          width="48"
          rounded="lg"
          title="Oprește răspunsul"
          @click="stop"
        />
        <v-btn
          v-else
          color="primary"
          icon="mdi-send"
          height="48"
          width="48"
          rounded="lg"
          :disabled="!input.trim() || !selection"
          title="Trimite (Enter)"
          @click="send()"
        />
      </div>
    </v-card>
  </div>
</template>

<style scoped>
.assistant-page {
  height: calc(100vh - 64px - 48px);
  min-height: 520px;
}
.chat-card {
  min-height: 0;
}
.messages {
  overflow-y: auto;
  min-height: 0;
}
.empty {
  height: 100%;
  min-height: 260px;
}
.bubble {
  max-width: min(640px, 75vw);
  padding: 10px 14px;
  border-radius: 14px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 0.95rem;
}
.bubble-user {
  background: rgb(var(--v-theme-primary));
  color: rgb(var(--v-theme-on-primary));
  border-bottom-right-radius: 4px;
}
.bubble-assistant {
  background: rgba(var(--v-theme-on-surface), 0.06);
  border-bottom-left-radius: 4px;
}
.bubble-error {
  background: rgba(var(--v-theme-error), 0.1);
  color: rgb(var(--v-theme-error));
  border-bottom-left-radius: 4px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgb(var(--v-theme-primary));
  animation: bounce 1.2s infinite ease-in-out;
}
.dot:nth-child(2) {
  animation-delay: 0.15s;
}
.dot:nth-child(3) {
  animation-delay: 0.3s;
}
.caret {
  display: inline-block;
  width: 2px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: rgb(var(--v-theme-primary));
  animation: blink 1s steps(1) infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
@keyframes bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  40% {
    transform: translateY(-5px);
    opacity: 1;
  }
}
</style>
