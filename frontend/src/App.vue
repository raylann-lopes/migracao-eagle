<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Database,
  FileDown,
  FileSpreadsheet,
  Play,
  RefreshCw,
  Server,
  ShieldCheck,
  Upload,
  Workflow,
} from 'lucide-vue-next'
import {
  api,
  type CleanDatabaseTemplate,
  type CreateMigrationProcessRequest,
  type Layout,
  type MigrationModule,
  type MigrationProcess,
  type SheetDetail,
} from './api'

const modules: MigrationModule[] = ['CLIENTES', 'FORNECEDORES', 'PRODUTOS', 'ARECEBER', 'APAGAR']
const moduleLabels: Record<MigrationModule, string> = {
  CLIENTES: 'Clientes',
  FORNECEDORES: 'Fornecedores',
  PRODUTOS: 'Produtos',
  ARECEBER: 'Contas a receber',
  APAGAR: 'Contas a pagar',
}
const defaultCleanDatabaseTemplates: CleanDatabaseTemplate[] = [
  {
    version: '2025.001',
    description: 'Eagle Gestao 2025.001',
  },
]
const processes = ref<MigrationProcess[]>([])
const layouts = ref<Layout[]>([])
const cleanDatabaseTemplates = ref<CleanDatabaseTemplate[]>(defaultCleanDatabaseTemplates)
const selected = ref<MigrationProcess | null>(null)
const selectedSheet = ref<SheetDetail | null>(null)
const loading = ref(false)
const message = ref('')
const error = ref('')
const uploadingModule = ref<MigrationModule | null>(null)
const operationLabel = ref('')

const form = ref<CreateMigrationProcessRequest>({
  clientName: '',
  cnpj: '',
  eagleVersion: '2025.001',
  config: {
    defaultDistrictId: null,
    defaultCep: '',
    companyState: 'MG',
    migrateReceivables: true,
  },
})
const selectedCleanDatabaseVersion = ref(defaultCleanDatabaseTemplates[0].version)

const canImport = computed(() => {
  if (!selected.value || selected.value.sheets.length === 0) return false
  return selected.value.sheets.every((sheet) => sheet.errorCount === 0)
})

const nextProcedure = computed(() => selected.value?.procedures.find((procedure) => procedure.status === 'PENDING') ?? null)
const canRunComplete = computed(() => canImport.value && !loading.value)
const sheetStats = computed(() => {
  const sheets = selected.value?.sheets ?? []
  return {
    total: sheets.length,
    valid: sheets.reduce((sum, sheet) => sum + sheet.validRows, 0),
    errors: sheets.reduce((sum, sheet) => sum + sheet.errorCount, 0),
    warnings: sheets.reduce((sum, sheet) => sum + sheet.warningCount, 0),
  }
})
const procedureStats = computed(() => {
  const procedures = selected.value?.procedures ?? []
  const done = procedures.filter((procedure) => procedure.status === 'SUCCESS' || procedure.status === 'SKIPPED').length
  return {
    total: procedures.length,
    done,
    percent: procedures.length ? Math.round((done / procedures.length) * 100) : 0,
  }
})
const pendingProcedures = computed(() => selected.value?.procedures.filter((procedure) => procedure.status === 'PENDING').length ?? 0)
const failedProcedures = computed(() => selected.value?.procedures.filter((procedure) => procedure.status === 'FAILED').length ?? 0)
const completedModules = computed(() => selected.value?.sheets.filter((sheet) => sheet.errorCount === 0).length ?? 0)
const showHeaderProgress = computed(() => loading.value || selected.value?.status === 'PROCEDURES_EM_EXECUCAO')
const headerProgressPercent = computed(() => {
  if (!selected.value) return loading.value ? 12 : 0
  if (selected.value.status === 'CONCLUIDO') return 100
  return Math.max(procedureStats.value.percent, loading.value ? 12 : 0)
})
const headerProgressLabel = computed(() => {
  if (operationLabel.value) return operationLabel.value
  if (selected.value?.status === 'PROCEDURES_EM_EXECUCAO') return 'Procedures em execucao'
  return 'Processando'
})

onMounted(async () => {
  await Promise.all([loadProcesses(), loadLayouts(), loadCleanDatabaseTemplates()])
})

async function loadProcesses() {
  processes.value = await api.listProcesses()
  if (!selected.value && processes.value.length > 0) {
    await selectProcess(processes.value[0].id)
  }
}

async function loadLayouts() {
  layouts.value = await api.layouts()
}

async function loadCleanDatabaseTemplates() {
  cleanDatabaseTemplates.value = await api.cleanDatabaseTemplates()
  if (cleanDatabaseTemplates.value.length > 0) {
    selectedCleanDatabaseVersion.value = cleanDatabaseTemplates.value[0].version
    applyCleanDatabasePreset()
  }
}

async function createProcess() {
  await run(async () => {
    const created = await api.createProcess(form.value)
    processes.value = [created, ...processes.value]
    selected.value = created
    selectedSheet.value = null
    message.value = 'Processo criado.'
  }, true, 'Criando processo')
}

async function selectProcess(id: string) {
  await run(async () => {
    selected.value = await api.getProcess(id)
    selectedSheet.value = null
  }, false)
}

async function uploadSheet(module: MigrationModule, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !selected.value) return
  uploadingModule.value = module
  await run(async () => {
    selectedSheet.value = await api.uploadSheet(selected.value!.id, module, file)
    selected.value = await api.getProcess(selected.value!.id)
    message.value = `${module}: planilha validada.`
  }, true, `Validando ${moduleLabels[module].toLowerCase()}`)
  uploadingModule.value = null
  input.value = ''
}

async function openSheet(module: MigrationModule) {
  if (!selected.value) return
  await run(async () => {
    selectedSheet.value = await api.getSheet(selected.value!.id, module)
  }, false)
}

async function importMigrador() {
  if (!selected.value) return
  await run(async () => {
    selected.value = await api.importMigrador(selected.value!.id)
    message.value = 'Planilhas importadas no MIGRADOR.'
  }, true, 'Importando planilhas no MIGRADOR')
}

async function runCompleteMigration() {
  if (!selected.value) return
  await run(async () => {
    selected.value = await api.runCompleteMigration(selected.value!.id)
    message.value =
      selected.value.status === 'CONCLUIDO'
        ? 'Migracao concluida. Banco final disponivel para download.'
        : 'Migracao finalizada com falha. Confira o status e as procedures.'
  }, true, 'Rodando migracao completa')
}

async function executeNext() {
  if (!selected.value) return
  await run(async () => {
    await api.executeNextProcedure(selected.value!.id)
    selected.value = await api.getProcess(selected.value!.id)
    message.value = 'Procedure executada.'
  }, true, 'Executando proxima procedure')
}

async function refreshSelected() {
  if (!selected.value) return
  await selectProcess(selected.value.id)
}

async function run(action: () => Promise<void>, showLoading = true, label = 'Processando') {
  error.value = ''
  message.value = ''
  if (showLoading) {
    loading.value = true
    operationLabel.value = label
  }
  try {
    await action()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Falha inesperada'
  } finally {
    loading.value = false
    operationLabel.value = ''
  }
}

function sheetFor(module: MigrationModule) {
  return selected.value?.sheets.find((sheet) => sheet.module === module) ?? null
}

function layoutFor(module: MigrationModule) {
  return layouts.value.find((layout) => layout.module === module)
}

function applyCleanDatabasePreset() {
  const option = cleanDatabaseTemplates.value.find((item) => item.version === selectedCleanDatabaseVersion.value)
  if (!option) return
  form.value.eagleVersion = option.version
  form.value.config.defaultDistrictId = null
  form.value.config.defaultCep = ''
}

function updateDefaultDistrict(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  const districtId = Number(value)
  form.value.config.defaultDistrictId = value && Number.isFinite(districtId) ? districtId : null
}

function moduleLabel(module: MigrationModule) {
  return moduleLabels[module]
}

function cleanDatabaseLabel(process: MigrationProcess) {
  return cleanDatabaseTemplates.value.find((item) => item.version === process.eagleVersion)?.description ?? process.eagleVersion
}

function statusLabel(status: string) {
  return status.replaceAll('_', ' ')
}

function statusClass(status: string) {
  if (['CONCLUIDO', 'PADRONIZADO', 'VALIDADO', 'IMPORTADO_MIGRADOR'].includes(status)) return 'success'
  if (['VALIDADO_COM_ALERTAS', 'PROCEDURES_EM_EXECUCAO', 'VALIDANDO'].includes(status)) return 'warning'
  if (['VALIDADO_COM_ERROS', 'FALHOU', 'CANCELADO'].includes(status)) return 'danger'
  return 'neutral'
}

function shortDate(value: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}
</script>

<template>
  <main class="app-shell">
    <header class="topbar command-header" :class="{ 'is-busy': showHeaderProgress }">
      <div class="brand-block">
        <div class="brand-mark">E</div>
        <div>
          <span class="eyebrow">Eagle Tecnologia</span>
          <h1>Migracao de Dados</h1>
          <p>Pipeline interno para conversao de bases Eagle Gestao.</p>
        </div>
      </div>
      <div class="header-actions">
        <span v-if="showHeaderProgress" class="operation-status">
          <Clock3 :size="14" />
          {{ headerProgressLabel }}
          <strong>{{ headerProgressPercent }}%</strong>
        </span>
        <span v-if="selected" class="status-pill" :class="statusClass(selected.status)">
          <Activity :size="15" />
          {{ statusLabel(selected.status) }}
        </span>
        <button class="icon-button" type="button" title="Atualizar" @click="refreshSelected">
          <RefreshCw :size="18" />
        </button>
      </div>
      <div v-if="showHeaderProgress" class="top-progress" :class="{ indeterminate: loading }">
        <div :style="{ width: loading ? undefined : `${headerProgressPercent}%` }"></div>
      </div>
    </header>

    <section v-if="message || error" class="feedback" :class="{ danger: error }">
      <CheckCircle2 v-if="message" :size="18" />
      <AlertTriangle v-else :size="18" />
      <span>{{ message || error }}</span>
    </section>

    <div class="grid">
      <aside class="panel">
        <div class="panel-title">
          <Server :size="18" />
          <h2>Novo processo</h2>
        </div>
        <form class="form" @submit.prevent="createProcess">
          <label>
            Cliente
            <input v-model="form.clientName" required maxlength="120" />
          </label>
          <label>
            CNPJ
            <input v-model="form.cnpj" maxlength="18" />
          </label>
          <label>
            Banco limpo da versao
            <select class="version-select" v-model="selectedCleanDatabaseVersion" @change="applyCleanDatabasePreset">
              <option v-for="option in cleanDatabaseTemplates" :key="option.version" :value="option.version">
                {{ option.description }}
              </option>
            </select>
          </label>
          <div class="form-grid">
            <label>
              Distrito padrao
              <input
                :value="form.config.defaultDistrictId ?? ''"
                inputmode="numeric"
                placeholder="Opcional"
                type="number"
                @input="updateDefaultDistrict"
              />
            </label>
            <label>
              CEP padrao
              <input v-model="form.config.defaultCep" maxlength="10" placeholder="Opcional" />
            </label>
            <label>
              Estado
              <input v-model="form.config.companyState" required maxlength="2" />
            </label>
            <label class="checkbox">
              <input v-model="form.config.migrateReceivables" type="checkbox" />
              Migrar financeiro
            </label>
          </div>
          <button class="primary" type="submit" :disabled="loading">Criar processo</button>
        </form>

        <div class="process-list">
          <div class="panel-title">
            <Clock3 :size="18" />
            <h2>Processos</h2>
          </div>
          <button
            v-for="process in processes"
            :key="process.id"
            class="process-item"
            :class="{ active: selected?.id === process.id }"
            type="button"
            @click="selectProcess(process.id)"
          >
            <strong>{{ process.clientName }}</strong>
            <span class="mini-status" :class="statusClass(process.status)">{{ statusLabel(process.status) }}</span>
          </button>
        </div>
      </aside>

      <section class="workspace">
        <div v-if="!selected" class="empty-state">
          Crie ou selecione um processo para iniciar.
        </div>

        <template v-else>
          <section id="overview" class="metrics-grid">
            <article>
              <span>Planilhas</span>
              <strong>{{ sheetStats.total }}/{{ modules.length }}</strong>
            </article>
            <article>
              <span>Linhas validas</span>
              <strong>{{ sheetStats.valid }}</strong>
            </article>
            <article>
              <span>Inconsistencias</span>
              <strong>{{ sheetStats.errors }}</strong>
            </article>
            <article>
              <span>Procedures</span>
              <strong>{{ procedureStats.done }}/{{ procedureStats.total }}</strong>
            </article>
          </section>

          <section class="summary-band">
            <div>
              <span>Cliente</span>
              <strong>{{ selected.clientName }}</strong>
            </div>
            <div>
              <span>Banco limpo</span>
              <strong>{{ cleanDatabaseLabel(selected) }}</strong>
            </div>
            <div>
              <span>Status</span>
              <strong>{{ statusLabel(selected.status) }}</strong>
            </div>
            <div>
              <span>Financeiro</span>
              <strong>{{ selected.config.migrateReceivables ? 'Receber e pagar' : 'Nao migrar' }}</strong>
            </div>
            <div>
              <span>Banco final</span>
              <strong>{{ selected.finalDatabaseFilename || 'Nao gerado' }}</strong>
            </div>
          </section>

          <section id="execution" class="panel execution-panel" :class="{ completed: selected.status === 'CONCLUIDO' }">
            <div>
              <div class="panel-title">
                <Workflow :size="18" />
                <h2>{{ selected.status === 'CONCLUIDO' ? 'Migracao concluida' : 'Execucao completa' }}</h2>
              </div>
              <p>
                {{ procedureStats.done }} de {{ procedureStats.total }} etapas finalizadas
                <template v-if="failedProcedures"> · {{ failedProcedures }} com falha</template>
              </p>
              <div class="progress-track">
                <div :style="{ width: `${procedureStats.percent}%` }"></div>
              </div>
            </div>
            <div class="execution-actions">
              <button class="primary" type="button" :disabled="!canRunComplete" @click="runCompleteMigration">
                <Play :size="17" />
                Rodar migracao completa
              </button>
              <a
                v-if="selected.finalDatabaseAvailable"
                class="download-button"
                :href="`/api/migration-processes/${selected.id}/final-database`"
              >
                <FileDown :size="17" />
                Baixar banco final
              </a>
            </div>
            <p v-if="selected.lastError" class="error-text">{{ selected.lastError }}</p>
          </section>

          <section id="sheets" class="panel">
            <div class="section-heading">
              <div>
                <div class="panel-title">
                  <FileSpreadsheet :size="18" />
                  <h2>Planilhas</h2>
                </div>
              </div>
              <button class="secondary-action" type="button" :disabled="!canImport || loading" @click="importMigrador">
                <Database :size="17" />
                Somente importar
              </button>
            </div>

            <div class="module-grid">
              <article v-for="module in modules" :key="module" class="module-card">
                <div class="module-title">
                  <ShieldCheck v-if="sheetFor(module) && !sheetFor(module)!.errorCount" :size="18" />
                  <FileSpreadsheet v-else :size="18" />
                  <strong>{{ moduleLabel(module) }}</strong>
                </div>
                <div class="module-meta">
                  <template v-if="sheetFor(module)">
                    <span class="status-pill compact" :class="statusClass(sheetFor(module)!.status)">
                      {{ statusLabel(sheetFor(module)!.status) }}
                    </span>
                    <span>{{ sheetFor(module)!.validRows }}/{{ sheetFor(module)!.totalRows }}</span>
                    <span>{{ sheetFor(module)!.errorCount }} erros</span>
                    <span>{{ sheetFor(module)!.warningCount }} alertas</span>
                  </template>
                  <span v-else>{{ layoutFor(module)?.fields.length ?? 0 }} colunas esperadas</span>
                </div>
                <div class="module-actions">
                  <label class="upload-button">
                    <Upload :size="16" />
                    {{ uploadingModule === module ? 'Enviando' : 'Enviar' }}
                    <input type="file" accept=".xls,.xlsx,.csv" @change="uploadSheet(module, $event)" />
                  </label>
                  <button v-if="sheetFor(module)" type="button" @click="openSheet(module)">Ver previa</button>
                  <a v-if="sheetFor(module)?.errorCount" :href="`/api/migration-processes/${selected.id}/sheets/${module}/errors.csv`">
                    <FileDown :size="16" />
                  </a>
                </div>
              </article>
            </div>
          </section>

          <section v-if="selectedSheet" id="preview" class="panel">
            <div class="section-heading">
              <div>
                <div class="panel-title">
                  <Database :size="18" />
                  <h2>{{ moduleLabel(selectedSheet.module) }}</h2>
                </div>
                <p>{{ selectedSheet.summary.originalFilename }} · {{ shortDate(selectedSheet.summary.validatedAt) }}</p>
              </div>
            </div>

            <div v-if="selectedSheet.issues.length" class="issues">
              <div v-for="issue in selectedSheet.issues.slice(0, 80)" :key="`${issue.rowNumber}-${issue.field}-${issue.message}`" class="issue-row" :class="{ warning: issue.severity === 'WARNING' }">
                <strong>Linha {{ issue.rowNumber }}</strong>
                <span>{{ issue.field || 'LAYOUT' }}</span>
                <p>{{ issue.message }}</p>
              </div>
            </div>

            <div class="table-wrap">
              <table v-if="selectedSheet.previewRows.length">
                <thead>
                  <tr>
                    <th v-for="field in Object.keys(selectedSheet.previewRows[0])" :key="field">{{ field }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in selectedSheet.previewRows.slice(0, 20)" :key="index">
                    <td v-for="field in Object.keys(selectedSheet.previewRows[0])" :key="field">{{ row[field] || '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-state">Sem linhas validas para previa.</div>
            </div>
          </section>

          <section id="procedures" class="panel">
            <div class="section-heading">
              <div>
                <div class="panel-title">
                  <Activity :size="18" />
                  <h2>Procedures</h2>
                </div>
              </div>
              <button class="primary" type="button" :disabled="!nextProcedure || loading" @click="executeNext">
                <Play :size="17" />
                Executar proxima
              </button>
            </div>
            <div class="procedure-list">
              <div v-for="procedure in selected.procedures" :key="procedure.id" class="procedure-row" :class="procedure.status.toLowerCase()">
                <span>{{ procedure.stepOrder }}</span>
                <strong>{{ procedure.procedureName }}</strong>
                <em>{{ procedure.status }}</em>
              </div>
            </div>
          </section>
        </template>
      </section>

      <aside v-if="selected" class="right-sidebar">
        <section class="side-card process-focus">
          <span class="side-label">Processo atual</span>
          <strong>{{ selected.clientName }}</strong>
          <span class="status-pill compact" :class="statusClass(selected.status)">
            {{ statusLabel(selected.status) }}
          </span>
        </section>

        <section class="side-card">
          <div class="side-title">
            <Activity :size="16" />
            <strong>Topicos</strong>
          </div>
          <nav class="topic-list" aria-label="Topicos do processo">
            <a href="#overview">
              <span>Visao geral</span>
              <strong>{{ sheetStats.valid }} linhas</strong>
            </a>
            <a href="#execution">
              <span>Execucao</span>
              <strong>{{ procedureStats.percent }}%</strong>
            </a>
            <a href="#sheets">
              <span>Planilhas</span>
              <strong>{{ completedModules }}/{{ modules.length }}</strong>
            </a>
            <a href="#procedures">
              <span>Procedures</span>
              <strong>{{ pendingProcedures }} pend.</strong>
            </a>
            <a v-if="selectedSheet" href="#preview">
              <span>Previa</span>
              <strong>{{ moduleLabel(selectedSheet.module) }}</strong>
            </a>
          </nav>
        </section>

        <section class="side-card final-card">
          <div class="side-title">
            <Database :size="16" />
            <strong>Banco final</strong>
          </div>
          <p>{{ selected.finalDatabaseFilename || 'Aguardando conclusao da migracao.' }}</p>
          <a
            v-if="selected.finalDatabaseAvailable"
            class="download-button full"
            :href="`/api/migration-processes/${selected.id}/final-database`"
          >
            <FileDown :size="16" />
            Baixar banco
          </a>
        </section>

        <section class="side-card">
          <div class="side-title">
            <ShieldCheck :size="16" />
            <strong>Conferencia</strong>
          </div>
          <div class="check-list">
            <span :class="{ ok: sheetStats.errors === 0 }">Sem erros de layout</span>
            <span :class="{ ok: selected.finalDatabaseAvailable }">Banco final gerado</span>
            <span :class="{ ok: failedProcedures === 0 }">Sem procedure com falha</span>
          </div>
        </section>
      </aside>
    </div>
  </main>
</template>
