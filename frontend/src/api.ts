export type MigrationStatus =
  | 'CRIADO'
  | 'PLANILHAS_ENVIADAS'
  | 'VALIDANDO'
  | 'VALIDADO_COM_ERROS'
  | 'VALIDADO_COM_ALERTAS'
  | 'VALIDADO'
  | 'PADRONIZADO'
  | 'IMPORTADO_MIGRADOR'
  | 'PROCEDURES_EM_EXECUCAO'
  | 'CONCLUIDO'
  | 'FALHOU'
  | 'CANCELADO'

export type MigrationModule = 'CLIENTES' | 'FORNECEDORES' | 'PRODUTOS' | 'ARECEBER' | 'APAGAR'

export interface MigrationConfig {
  defaultDistrictId: number | null
  defaultCep: string
  companyState: string
  migrateReceivables: boolean
}

export interface CreateMigrationProcessRequest {
  clientName: string
  cnpj: string
  eagleVersion: string
  config: MigrationConfig
}

export interface SheetSummary {
  id: string
  module: MigrationModule
  status: MigrationStatus
  originalFilename: string
  totalRows: number
  validRows: number
  errorCount: number
  warningCount: number
  uploadedAt: string
  validatedAt: string | null
  importedAt: string | null
}

export interface ProcedureExecution {
  id: string
  stepOrder: number
  procedureName: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  startedAt: string | null
  finishedAt: string | null
  errorMessage: string | null
}

export interface MigrationProcess {
  id: string
  clientName: string
  cnpj: string | null
  eagleVersion: string
  eagleWorkingDatabasePath: string | null
  finalDatabasePath: string | null
  finalDatabaseFilename: string | null
  finalDatabaseAvailable: boolean
  status: MigrationStatus
  config: MigrationConfig
  sheets: SheetSummary[]
  procedures: ProcedureExecution[]
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export interface RowIssue {
  rowNumber: number
  field: string | null
  message: string
  severity: 'ERROR' | 'WARNING'
}

export interface SheetDetail {
  module: MigrationModule
  summary: SheetSummary
  previewRows: Record<string, string | null>[]
  issues: RowIssue[]
}

export interface LayoutField {
  name: string
  type: 'INTEGER' | 'TEXT' | 'MONETARY' | 'DATE'
  maxLength: number
  required: boolean
  description: string
}

export interface Layout {
  module: MigrationModule
  targetTable: string
  fields: LayoutField[]
}

export interface CleanDatabaseTemplate {
  version: string
  description: string
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options)
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message ?? 'Falha ao executar operacao')
  }
  return response.json()
}

export const api = {
  listProcesses: () => request<MigrationProcess[]>('/api/migration-processes'),
  getProcess: (id: string) => request<MigrationProcess>(`/api/migration-processes/${id}`),
  createProcess: (payload: CreateMigrationProcessRequest) =>
    request<MigrationProcess>('/api/migration-processes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  layouts: () => request<Layout[]>('/api/migration-layouts'),
  cleanDatabaseTemplates: () => request<CleanDatabaseTemplate[]>('/api/clean-database-templates'),
  uploadSheet: (processId: string, module: MigrationModule, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<SheetDetail>(`/api/migration-processes/${processId}/sheets/${module}`, {
      method: 'POST',
      body: formData,
    })
  },
  getSheet: (processId: string, module: MigrationModule) =>
    request<SheetDetail>(`/api/migration-processes/${processId}/sheets/${module}`),
  importMigrador: (processId: string) =>
    request<MigrationProcess>(`/api/migration-processes/${processId}/import-migrador`, { method: 'POST' }),
  runCompleteMigration: (processId: string) =>
    request<MigrationProcess>(`/api/migration-processes/${processId}/run-complete`, { method: 'POST' }),
  executeNextProcedure: (processId: string) =>
    request<ProcedureExecution>(`/api/migration-processes/${processId}/procedures/execute-next`, { method: 'POST' }),
}
