<script setup lang="ts">
import { computed, onMounted, ref, useSlots, watch } from 'vue'

import type { CrudApi, QueryParams } from '@/api/crud'
import { errorMessage } from '@/api/http'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useLookups, type LookupName } from '@/composables/useLookups'
import { useToast } from '@/composables/useToast'

import type { ColumnDef, FieldDef, FormItem } from './types'

/**
 * Pagina CRUD generica: tabel cu cautare + formular de adaugare/editare + stergere cu confirmare.
 * Fiecare entitate (sali, discipline, profesori...) doar o configureaza prin coloane si campuri.
 */
const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    api: CrudApi<any>
    columns: ColumnDef[]
    fields: FieldDef[]
    newItem: () => FormItem
    /** Numele entitatii la singular, pentru titlurile dialogurilor (ex: "sală"). */
    entityName: string
    /** Filtre trimise la listare (ex: { subgroupId: 3 }). */
    params?: QueryParams
    /** Cum se descrie un element in dialogul de stergere. */
    itemLabel?: (item: FormItem) => string
    /** Transformari inainte de trimitere (ex: golirea campurilor ascunse). */
    toPayload?: (item: FormItem, isNew: boolean) => FormItem
    /** Listele din campurile de selectie care trebuie reincarcate dupa o modificare. */
    reloadLookups?: LookupName[]
    canCreate?: boolean
    canEdit?: boolean
    canDelete?: boolean
    searchable?: boolean
  }>(),
  {
    subtitle: undefined,
    params: undefined,
    itemLabel: undefined,
    toPayload: undefined,
    reloadLookups: () => [],
    canCreate: true,
    canEdit: true,
    canDelete: true,
    searchable: true,
  },
)

const emit = defineEmits<{ changed: [] }>()

const toast = useToast()
const lookups = useLookups()
const slots = useSlots()

const items = ref<FormItem[]>([])
const loading = ref(false)
const search = ref('')

const dialog = ref(false)
const isNew = ref(true)
const form = ref<FormItem>({})
const formValid = ref(false)
const saving = ref(false)

const deleteDialog = ref(false)
const toDelete = ref<FormItem | null>(null)
const deleting = ref(false)

const tableHeaders = computed(() => {
  const headers = props.columns.map((c) => ({ ...c, sortable: c.sortable ?? true }))
  if (props.canEdit || props.canDelete) {
    headers.push({ title: '', key: 'actions', sortable: false, align: 'end', width: 110 })
  }
  return headers
})

/** Sloturile "item.<coloana>" primite de la pagina parinte se transmit mai departe tabelului. */
const itemSlots = computed(() => Object.keys(slots).filter((name) => name.startsWith('item.')))

const visibleFields = computed(() =>
  props.fields.filter((f) => (f.visible ? f.visible(form.value, isNew.value) : true)),
)

async function load() {
  loading.value = true
  try {
    items.value = (await props.api.list(props.params)) as FormItem[]
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isNew.value = true
  form.value = props.newItem()
  dialog.value = true
}

function openEdit(item: FormItem) {
  isNew.value = false
  form.value = { ...item }
  // Parola nu vine niciodata de la server; campul porneste gol la editare
  if (props.fields.some((f) => f.type === 'password')) form.value.password = null
  dialog.value = true
}

function rulesFor(field: FieldDef) {
  const rules: ((v: unknown) => true | string)[] = []
  if (field.required || (field.requiredOnCreate && isNew.value)) {
    rules.push((v) => (v !== null && v !== undefined && v !== '') || 'Câmp obligatoriu')
  }
  if (field.type === 'email') {
    rules.push((v) => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(v)) || 'Adresă de email invalidă')
  }
  if (field.type === 'password') {
    rules.push((v) => !v || String(v).length >= 6 || 'Minimum 6 caractere')
  }
  if (field.type === 'number' && field.min !== undefined) {
    const min = field.min
    rules.push((v) => v === null || v === '' || Number(v) >= min || `Minimum ${min}`)
  }
  if (field.type === 'number' && field.max !== undefined) {
    const max = field.max
    rules.push((v) => v === null || v === '' || Number(v) <= max || `Maximum ${max}`)
  }
  return rules
}

/** Campurile numerice se trimit ca numere; campurile ascunse nu se trimit. */
function buildPayload(): FormItem {
  const payload: FormItem = { ...form.value }
  for (const field of props.fields) {
    const hidden = field.visible && !field.visible(form.value, isNew.value)
    if (hidden) {
      payload[field.key] = null
    } else if (field.type === 'number') {
      const v = payload[field.key]
      payload[field.key] = v === '' || v === null || v === undefined ? null : Number(v)
    } else if (field.type === 'password' && !payload[field.key]) {
      payload[field.key] = null
    }
  }
  return props.toPayload ? props.toPayload(payload, isNew.value) : payload
}

async function save() {
  if (!formValid.value) return
  saving.value = true
  try {
    const payload = buildPayload()
    if (isNew.value) {
      await props.api.create(payload)
      toast.success(`Am adăugat ${props.entityName}.`)
    } else {
      await props.api.update(form.value.id as number, payload)
      toast.success('Modificările au fost salvate.')
    }
    dialog.value = false
    await afterChange()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    saving.value = false
  }
}

function askDelete(item: FormItem) {
  toDelete.value = item
  deleteDialog.value = true
}

async function confirmDelete() {
  if (!toDelete.value) return
  deleting.value = true
  try {
    await props.api.remove(toDelete.value.id as number)
    toast.success(`Am șters ${props.entityName}.`)
    deleteDialog.value = false
    await afterChange()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    deleting.value = false
  }
}

async function afterChange() {
  await load()
  if (props.reloadLookups.length) await lookups.reload(...props.reloadLookups)
  emit('changed')
}

const deleteText = computed(() => {
  if (!toDelete.value) return ''
  const label = props.itemLabel ? props.itemLabel(toDelete.value) : `#${String(toDelete.value.id)}`
  return `Sigur vrei să ștergi ${props.entityName} „${label}”? Operația nu poate fi anulată.`
})

watch(() => props.params, load, { deep: true })
onMounted(load)

defineExpose({ reload: load })
</script>

<template>
  <div>
    <PageHeader :title="title" :subtitle="subtitle">
      <template #actions>
        <slot name="actions" />
        <v-btn v-if="canCreate" color="primary" prepend-icon="mdi-plus" @click="openCreate">
          Adaugă
        </v-btn>
      </template>
    </PageHeader>

    <v-card>
      <div class="d-flex flex-wrap align-center ga-3 pa-4">
        <v-text-field
          v-if="searchable"
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          placeholder="Caută..."
          density="compact"
          hide-details
          clearable
          class="search-field"
        />
        <slot name="filters" />
        <v-spacer />
        <span class="text-body-2 text-medium-emphasis">{{ items.length }} înregistrări</span>
        <v-btn icon="mdi-refresh" variant="text" size="small" title="Reîncarcă" @click="load" />
      </div>
      <v-divider />

      <v-data-table
        :headers="tableHeaders"
        :items="items"
        :search="search"
        :loading="loading"
        :items-per-page="15"
        item-value="id"
        no-data-text="Nu există înregistrări."
        loading-text="Se încarcă..."
      >
        <template v-for="name in itemSlots" #[name]="slotProps">
          <slot :name="name" v-bind="slotProps" />
        </template>

        <template #[`item.actions`]="{ item }">
          <div class="d-flex justify-end ga-1">
            <v-btn
              v-if="canEdit"
              icon="mdi-pencil-outline"
              variant="text"
              size="small"
              title="Editează"
              @click="openEdit(item)"
            />
            <v-btn
              v-if="canDelete"
              icon="mdi-trash-can-outline"
              variant="text"
              size="small"
              color="error"
              title="Șterge"
              @click="askDelete(item)"
            />
          </div>
        </template>
      </v-data-table>
    </v-card>

    <!-- Formular adaugare / editare -->
    <v-dialog v-model="dialog" max-width="640" persistent>
      <v-card>
        <v-card-title class="d-flex align-center pa-5 pb-2">
          <span class="text-h6 font-weight-bold">
            {{ isNew ? `Adaugă ${entityName}` : `Editează ${entityName}` }}
          </span>
          <v-spacer />
          <v-btn icon="mdi-close" variant="text" size="small" @click="dialog = false" />
        </v-card-title>

        <v-form v-model="formValid" @submit.prevent="save">
          <v-card-text class="pt-4">
            <v-row dense>
              <v-col v-for="field in visibleFields" :key="field.key" cols="12" :md="field.cols ?? 12">
                <v-switch
                  v-if="field.type === 'switch'"
                  v-model="form[field.key]"
                  :label="field.label"
                  :hint="field.hint"
                  persistent-hint
                />
                <v-select
                  v-else-if="field.type === 'select'"
                  v-model="form[field.key]"
                  :label="field.label"
                  :items="field.options ?? []"
                  :rules="rulesFor(field)"
                  :hint="field.hint"
                  :clearable="field.clearable"
                  persistent-hint
                />
                <v-autocomplete
                  v-else-if="field.type === 'autocomplete'"
                  v-model="form[field.key]"
                  :label="field.label"
                  :items="field.options ?? []"
                  :rules="rulesFor(field)"
                  :hint="field.hint"
                  :clearable="field.clearable"
                  persistent-hint
                  no-data-text="Nicio potrivire"
                />
                <v-textarea
                  v-else-if="field.type === 'textarea'"
                  v-model="form[field.key]"
                  :label="field.label"
                  :rules="rulesFor(field)"
                  :hint="field.hint"
                  rows="3"
                  auto-grow
                  persistent-hint
                />
                <v-text-field
                  v-else
                  v-model="form[field.key]"
                  :label="field.label"
                  :type="field.type ?? 'text'"
                  :rules="rulesFor(field)"
                  :hint="field.type === 'password' && !isNew ? 'Lasă gol ca să păstrezi parola actuală' : field.hint"
                  :min="field.min"
                  :max="field.max"
                  persistent-hint
                />
              </v-col>
            </v-row>
          </v-card-text>

          <v-card-actions class="pa-5 pt-0">
            <v-spacer />
            <v-btn variant="text" @click="dialog = false">Renunță</v-btn>
            <v-btn type="submit" color="primary" variant="flat" :loading="saving" :disabled="!formValid">
              Salvează
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="deleteDialog"
      title="Confirmă ștergerea"
      :text="deleteText"
      confirm-text="Șterge"
      :loading="deleting"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.search-field {
  max-width: 320px;
  min-width: 220px;
}
</style>
