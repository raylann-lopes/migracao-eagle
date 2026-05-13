package com.example.demo.migration.service;

import com.example.demo.migration.controller.dto.CreateMigrationProcessRequest;
import com.example.demo.migration.controller.dto.MigrationProcessResponse;
import com.example.demo.migration.controller.dto.ProcedureExecutionResponse;
import com.example.demo.migration.controller.dto.RowIssueResponse;
import com.example.demo.migration.controller.dto.SheetDetailResponse;
import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.domain.MigrationProcessEntity;
import com.example.demo.migration.domain.MigrationRowEntity;
import com.example.demo.migration.domain.MigrationSheetEntity;
import com.example.demo.migration.domain.MigrationStatus;
import com.example.demo.migration.domain.ProcedureExecutionEntity;
import com.example.demo.migration.domain.ProcedureExecutionStatus;
import com.example.demo.migration.exception.BusinessException;
import com.example.demo.migration.exception.ResourceNotFoundException;
import com.example.demo.migration.integration.FirebirdMigradorClient;
import com.example.demo.migration.repository.MigrationProcessRepository;
import com.example.demo.migration.repository.MigrationSheetRepository;
import com.example.demo.migration.repository.ProcedureExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MigrationProcessService {

    private final MigrationProcessRepository processRepository;
    private final MigrationSheetRepository sheetRepository;
    private final ProcedureExecutionRepository procedureRepository;
    private final MigrationSpreadsheetReader spreadsheetReader;
    private final MigrationValidationService validationService;
    private final MigrationLayoutRegistry layoutRegistry;
    private final MigrationMapper mapper;
    private final ObjectMapper objectMapper;
    private final FirebirdMigradorClient firebirdClient;
    private final MigrationDatabaseFileService databaseFileService;
    private final CleanDatabaseTemplateService cleanDatabaseTemplateService;
    private final MigrationProperties properties;

    public MigrationProcessService(
            MigrationProcessRepository processRepository,
            MigrationSheetRepository sheetRepository,
            ProcedureExecutionRepository procedureRepository,
            MigrationSpreadsheetReader spreadsheetReader,
            MigrationValidationService validationService,
            MigrationLayoutRegistry layoutRegistry,
            MigrationMapper mapper,
            ObjectMapper objectMapper,
            FirebirdMigradorClient firebirdClient,
            MigrationDatabaseFileService databaseFileService,
            CleanDatabaseTemplateService cleanDatabaseTemplateService,
            MigrationProperties properties) {
        this.processRepository = processRepository;
        this.sheetRepository = sheetRepository;
        this.procedureRepository = procedureRepository;
        this.spreadsheetReader = spreadsheetReader;
        this.validationService = validationService;
        this.layoutRegistry = layoutRegistry;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.firebirdClient = firebirdClient;
        this.databaseFileService = databaseFileService;
        this.cleanDatabaseTemplateService = cleanDatabaseTemplateService;
        this.properties = properties;
    }

    @Transactional
    public MigrationProcessResponse create(CreateMigrationProcessRequest request) {
        MigrationProcessEntity process = new MigrationProcessEntity();
        process.setClientName(request.clientName().trim());
        process.setCnpj(normalizeDocument(request.cnpj()));
        process.setEagleVersion(request.eagleVersion().trim());
        process.setMigratorDatabase(migratorDatabase());
        process.setCleanDatabasePath(cleanDatabaseTemplateService.templateReferenceForVersion(request.eagleVersion().trim()));
        process.setDefaultDistrictId(request.config().defaultDistrictId());
        process.setDefaultCep(normalizeBlank(request.config().defaultCep()));
        process.setCompanyState(request.config().companyState().trim().toUpperCase());
        process.setMigrateReceivables(request.config().migrateReceivables());
        process.setStatus(MigrationStatus.CRIADO);
        initializeProcedurePlan(process);

        return mapper.toResponse(processRepository.save(process));
    }

    private void runSetupProcedures(MigrationProcessEntity process) {
        for (ProcedureExecutionEntity execution : process.getProcedureExecutions()) {
            if (execution.getProcedureName().startsWith("UTIL_")) {
                try {
                    firebirdClient.executeProcedure(process.getMigratorDatabase(), execution.getProcedureName());
                    execution.setStatus(ProcedureExecutionStatus.SUCCESS);
                    execution.setStartedAt(OffsetDateTime.now());
                    execution.setFinishedAt(OffsetDateTime.now());
                } catch (Exception e) {
                    execution.setStatus(ProcedureExecutionStatus.FAILED);
                    execution.setErrorMessage("Falha no setup inicial: " + e.getMessage());
                    // Não travamos a criação se o setup falhar, mas o status ficará visível
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MigrationProcessResponse> list() {
        return processRepository.findAll().stream()
                .sorted(Comparator.comparing(MigrationProcessEntity::getCreatedAt).reversed())
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MigrationProcessResponse get(UUID processId) {
        return mapper.toResponse(getProcess(processId));
    }

    @Transactional
    public SheetDetailResponse uploadSheet(UUID processId, MigrationModule module, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Arquivo vazio.");
        }

        MigrationProcessEntity process = getProcess(processId);
        process.setStatus(MigrationStatus.VALIDANDO);
        process.getSheets().removeIf(sheet -> sheet.getModule() == module);

        SpreadsheetData spreadsheetData = spreadsheetReader.read(file);
        ValidationResult validation = validationService.validate(module, spreadsheetData);
        MigrationSheetEntity sheet = new MigrationSheetEntity();
        sheet.setProcess(process);
        sheet.setModule(module);
        sheet.setOriginalFilename(file.getOriginalFilename() == null ? module.name() : file.getOriginalFilename());
        sheet.setTotalRows(validation.totalRows());
        sheet.setValidRows(validation.validRows());
        sheet.setErrorCount(validation.errorCount());
        sheet.setWarningCount(validation.warningCount());
        sheet.setValidatedAt(OffsetDateTime.now());
        sheet.setStatus(statusForSheet(validation));

        for (ValidatedRow row : validation.rows()) {
            MigrationRowEntity rowEntity = new MigrationRowEntity();
            rowEntity.setSheet(sheet);
            rowEntity.setRowNumber(row.rowNumber());
            rowEntity.setValid(row.valid());
            rowEntity.setNormalizedJson(toJson(row.normalizedValues()));
            rowEntity.setErrorsJson(toJson(row.errors()));
            rowEntity.setWarningsJson(toJson(row.warnings()));
            sheet.getRows().add(rowEntity);
        }
        for (RowIssue layoutIssue : validation.layoutIssues()) {
            MigrationRowEntity rowEntity = new MigrationRowEntity();
            rowEntity.setSheet(sheet);
            rowEntity.setRowNumber(layoutIssue.rowNumber());
            rowEntity.setValid(false);
            rowEntity.setNormalizedJson("{}");
            rowEntity.setErrorsJson(toJson(List.of(layoutIssue)));
            rowEntity.setWarningsJson("[]");
            sheet.getRows().add(rowEntity);
        }

        process.getSheets().add(sheet);
        process.setStatus(statusForProcess(process));
        process.setLastError(null);
        processRepository.saveAndFlush(process);
        return toSheetDetail(sheet, 20);
    }

    @Transactional(readOnly = true)
    public SheetDetailResponse getSheet(UUID processId, MigrationModule module) {
        MigrationSheetEntity sheet = sheetRepository.findByProcessIdAndModule(processId, module)
                .orElseThrow(() -> new ResourceNotFoundException("Planilha nao encontrada para " + module));
        return toSheetDetail(sheet, 100);
    }

    @Transactional
    public MigrationProcessResponse importValidSheets(UUID processId) {
        MigrationProcessEntity process = getProcess(processId);
        List<MigrationSheetEntity> sheets = process.getSheets();
        if (sheets.isEmpty()) {
            throw new BusinessException("Envie e valide pelo menos uma planilha antes de importar.");
        }
        if (sheets.stream().anyMatch(sheet -> sheet.getErrorCount() > 0)) {
            throw new BusinessException("Existem erros de validacao. Corrija as planilhas antes de importar.");
        }
        for (MigrationSheetEntity sheet : sheets) {
            List<Map<String, String>> rows = validRows(sheet);
            firebirdClient.replaceTable(process.getMigratorDatabase(), sheet.getModule(), layoutRegistry.get(sheet.getModule()), rows);
            sheet.setImportedAt(OffsetDateTime.now());
        }
        process.setStatus(MigrationStatus.IMPORTADO_MIGRADOR);
        process.setLastError(null);
        return mapper.toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcedureExecutionResponse executeNextProcedure(UUID processId) {
        MigrationProcessEntity process = getProcess(processId);
        ProcedureExecutionEntity execution = procedureRepository
                .findFirstByProcessIdAndStatusOrderByStepOrder(processId, ProcedureExecutionStatus.PENDING)
                .orElseThrow(() -> new BusinessException("Nao ha procedures pendentes."));
        return executeProcedure(process, execution);
    }

    @Transactional
    public MigrationProcessResponse runCompleteMigration(UUID processId) {
        MigrationProcessEntity process = getProcess(processId);
        try {
            // Atualiza o caminho do banco limpo caso a configuracao tenha mudado
            process.setCleanDatabasePath(cleanDatabaseTemplateService.templateReferenceForVersion(process.getEagleVersion()));

            validateReadyForFullMigration(process);
            firebirdClient.assertEnabledForFullMigration();

            PreparedDatabase preparedDatabase = databaseFileService.prepareFinalDatabase(process);
            process.setEagleWorkingDatabasePath(preparedDatabase.workingDatabasePath().toString());
            process.setFinalDatabasePath(null);
            process.setFinalDatabaseFilename(null);
            resetProcedureExecutions(process);

            // Força o flush para garantir que o caminho do banco de trabalho esteja salvo
            processRepository.saveAndFlush(process);

            // Aguarda um pequeno instante para o Docker/File System sincronizar
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            firebirdClient.assertEagleAliasAvailable();

            importSheetsIntoMigrador(process);
            process.setStatus(MigrationStatus.IMPORTADO_MIGRADOR);

            for (ProcedureExecutionEntity execution : process.getProcedureExecutions().stream()
                    .sorted(Comparator.comparingInt(ProcedureExecutionEntity::getStepOrder))
                    .toList()) {
                executeProcedureStep(process, execution);
                if (execution.getStatus() == ProcedureExecutionStatus.FAILED) {
                    return mapper.toResponse(processRepository.save(process));
                }
            }

            String finalStorageUri = databaseFileService.publishFinalDatabase(preparedDatabase);
            process.setFinalDatabasePath(finalStorageUri);
            process.setFinalDatabaseFilename(preparedDatabase.finalDatabaseFilename());
            process.setStatus(MigrationStatus.CONCLUIDO);
            process.setLastError(null);
            return mapper.toResponse(processRepository.save(process));
        } catch (BusinessException exception) {
            process.setStatus(MigrationStatus.FALHOU);
            process.setLastError(exception.getMessage());
            return mapper.toResponse(processRepository.save(process));
        }
    }

    @Transactional
    public ProcedureExecutionResponse executeProcedure(UUID processId, String procedureName) {
        MigrationProcessEntity process = getProcess(processId);
        ProcedureExecutionEntity execution = process.getProcedureExecutions().stream()
                .filter(item -> item.getProcedureName().equalsIgnoreCase(procedureName))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Procedure fora do plano controlado: " + procedureName));
        return executeProcedure(process, execution);
    }

    @Transactional(readOnly = true)
    public String errorsCsv(UUID processId, MigrationModule module) {
        MigrationSheetEntity sheet = sheetRepository.findByProcessIdAndModule(processId, module)
                .orElseThrow(() -> new ResourceNotFoundException("Planilha nao encontrada para " + module));
        StringBuilder builder = new StringBuilder("linha,campo,severidade,mensagem\n");
        for (RowIssueResponse issue : collectIssues(sheet)) {
            builder.append(issue.rowNumber()).append(',')
                    .append(csv(issue.field())).append(',')
                    .append(issue.severity()).append(',')
                    .append(csv(issue.message())).append('\n');
        }
        return builder.toString();
    }

    @Transactional(readOnly = true)
    public Resource finalDatabaseResource(UUID processId) {
        return databaseFileService.finalDatabaseResource(getProcess(processId));
    }

    @Transactional(readOnly = true)
    public String finalDatabaseFilename(UUID processId) {
        MigrationProcessEntity process = getProcess(processId);
        if (process.getFinalDatabaseFilename() == null || process.getFinalDatabaseFilename().isBlank()) {
            throw new BusinessException("Banco final ainda nao foi gerado.");
        }
        return process.getFinalDatabaseFilename();
    }

    private ProcedureExecutionResponse executeProcedure(MigrationProcessEntity process, ProcedureExecutionEntity execution) {
        if (process.getStatus() != MigrationStatus.IMPORTADO_MIGRADOR
                && process.getStatus() != MigrationStatus.PROCEDURES_EM_EXECUCAO
                && process.getStatus() != MigrationStatus.FALHOU) {
            throw new BusinessException("Importe as planilhas validas para o MIGRADOR antes de executar procedures.");
        }

        // Garante que o banco Eagle esteja preparado antes de qualquer procedure
        if (process.getEagleWorkingDatabasePath() == null) {
            databaseFileService.prepareFinalDatabase(process);
        }

        execution.setStatus(ProcedureExecutionStatus.RUNNING);
        execution.setStartedAt(OffsetDateTime.now());
        execution.setErrorMessage(null);
        process.setStatus(MigrationStatus.PROCEDURES_EM_EXECUCAO);
        try {
            if (!shouldExecuteProcedure(process, execution.getProcedureName())) {
                skipProcedure(execution);
                return mapper.toProcedureResponse(execution);
            }
            executeFirebirdProcedure(process, execution.getProcedureName());
            execution.setStatus(ProcedureExecutionStatus.SUCCESS);
            execution.setFinishedAt(OffsetDateTime.now());
            if (process.getProcedureExecutions().stream().allMatch(this::isTerminalSuccess)) {
                process.setStatus(MigrationStatus.CONCLUIDO);
            }
        } catch (BusinessException exception) {
            execution.setStatus(ProcedureExecutionStatus.FAILED);
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setErrorMessage(exception.getMessage());
            process.setStatus(MigrationStatus.FALHOU);
            process.setLastError(exception.getMessage());
        }
        return mapper.toProcedureResponse(execution);
    }

    private void initializeProcedurePlan(MigrationProcessEntity process) {
        int order = 1;
        for (String procedure : layoutRegistry.procedurePlan()) {
            ProcedureExecutionEntity execution = new ProcedureExecutionEntity();
            execution.setProcess(process);
            execution.setStepOrder(order++);
            execution.setProcedureName(procedure);
            process.getProcedureExecutions().add(execution);
        }
    }

    private void validateReadyForFullMigration(MigrationProcessEntity process) {
        if (process.getSheets().isEmpty()) {
            throw new BusinessException("Envie e valide as planilhas antes de executar a migracao completa.");
        }
        if (process.getSheets().stream().anyMatch(sheet -> sheet.getErrorCount() > 0)) {
            throw new BusinessException("Existem erros de validacao. Corrija as planilhas antes de executar a migracao.");
        }
    }

    private void importSheetsIntoMigrador(MigrationProcessEntity process) {
        for (MigrationSheetEntity sheet : process.getSheets()) {
            List<Map<String, String>> rows = validRows(sheet);
            firebirdClient.replaceTable(process.getMigratorDatabase(), sheet.getModule(), layoutRegistry.get(sheet.getModule()), rows);
            sheet.setImportedAt(OffsetDateTime.now());
        }
    }

    private void executeProcedureStep(MigrationProcessEntity process, ProcedureExecutionEntity execution) {
        if (execution.getStatus() == ProcedureExecutionStatus.SUCCESS
                && !"CONFIGURAR_MIGRACAO".equalsIgnoreCase(execution.getProcedureName())) {
            return;
        }
        execution.setStatus(ProcedureExecutionStatus.RUNNING);
        execution.setStartedAt(OffsetDateTime.now());
        execution.setFinishedAt(null);
        execution.setErrorMessage(null);
        process.setStatus(MigrationStatus.PROCEDURES_EM_EXECUCAO);
        try {
            if (!shouldExecuteProcedure(process, execution.getProcedureName())) {
                skipProcedure(execution);
                return;
            }
            executeFirebirdProcedure(process, execution.getProcedureName());
            execution.setStatus(ProcedureExecutionStatus.SUCCESS);
            execution.setFinishedAt(OffsetDateTime.now());
        } catch (BusinessException exception) {
            execution.setStatus(ProcedureExecutionStatus.FAILED);
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setErrorMessage(exception.getMessage());
            process.setStatus(MigrationStatus.FALHOU);
            process.setLastError(exception.getMessage());
        }
    }

    private void resetProcedureExecutions(MigrationProcessEntity process) {
        for (ProcedureExecutionEntity execution : process.getProcedureExecutions()) {
            execution.setStatus(ProcedureExecutionStatus.PENDING);
            execution.setStartedAt(null);
            execution.setFinishedAt(null);
            execution.setErrorMessage(null);
        }
    }

    private void executeFirebirdProcedure(MigrationProcessEntity process, String procedureName) {
        if ("CONFIGURAR_MIGRACAO".equalsIgnoreCase(procedureName)) {
            firebirdClient.configurarMigracao(
                    process.getMigratorDatabase(),
                    process.getDefaultDistrictId(),
                    process.getDefaultCep(),
                    process.getCompanyState(),
                    process.isMigrateReceivables() && hasSheet(process, MigrationModule.ARECEBER),
                    hasSheet(process, MigrationModule.CLIENTES),
                    hasSheet(process, MigrationModule.FORNECEDORES),
                    false,
                    false,
                    hasSheet(process, MigrationModule.PRODUTOS),
                    false,
                    false);
            return;
        }
        firebirdClient.executeProcedure(process.getMigratorDatabase(), procedureName);
    }

    private boolean shouldExecuteProcedure(MigrationProcessEntity process, String procedureName) {
        String name = procedureName.toUpperCase();
        return switch (name) {
            case "MIGRAR_00_INICIAR" -> true;
            case "MIGRAR_02_CLIENTES" -> hasSheet(process, MigrationModule.CLIENTES);
            case "MIGRAR_03_FORNECEDORES" -> hasSheet(process, MigrationModule.FORNECEDORES);
            case "MIGRAR_05_PRODUTOS" -> hasSheet(process, MigrationModule.PRODUTOS);
            case "MIGRAR_14_ARECEBER" -> process.isMigrateReceivables() && hasSheet(process, MigrationModule.ARECEBER);
            case "MIGRAR_15_APAGAR" -> hasSheet(process, MigrationModule.APAGAR);
            default -> {
                // Se for uma procedure entre 04 e 13, depende da planilha de produtos
                if (name.startsWith("MIGRAR_")) {
                    try {
                        int code = Integer.parseInt(name.substring(7, 9));
                        if (code >= 4 && code <= 13) {
                            yield hasSheet(process, MigrationModule.PRODUTOS);
                        }
                    } catch (Exception e) {
                        // Se nao conseguir ler o numero, deixa rodar por padrao
                    }
                }
                yield true;
            }
        };
    }

    private void skipProcedure(ProcedureExecutionEntity execution) {
        execution.setStatus(ProcedureExecutionStatus.SKIPPED);
        execution.setFinishedAt(OffsetDateTime.now());
        execution.setErrorMessage(null);
    }

    private boolean isTerminalSuccess(ProcedureExecutionEntity execution) {
        return execution.getStatus() == ProcedureExecutionStatus.SUCCESS
                || execution.getStatus() == ProcedureExecutionStatus.SKIPPED;
    }

    private boolean hasSheet(MigrationProcessEntity process, MigrationModule module) {
        return process.getSheets().stream().anyMatch(sheet -> sheet.getModule() == module);
    }

    private MigrationStatus statusForSheet(ValidationResult validation) {
        if (validation.errorCount() > 0) {
            return MigrationStatus.VALIDADO_COM_ERROS;
        }
        if (validation.warningCount() > 0) {
            return MigrationStatus.VALIDADO_COM_ALERTAS;
        }
        return MigrationStatus.PADRONIZADO;
    }

    private MigrationStatus statusForProcess(MigrationProcessEntity process) {
        if (process.getSheets().isEmpty()) {
            return MigrationStatus.CRIADO;
        }
        if (process.getSheets().stream().anyMatch(sheet -> sheet.getErrorCount() > 0)) {
            return MigrationStatus.VALIDADO_COM_ERROS;
        }
        if (process.getSheets().stream().anyMatch(sheet -> sheet.getWarningCount() > 0)) {
            return MigrationStatus.VALIDADO_COM_ALERTAS;
        }
        return MigrationStatus.PADRONIZADO;
    }

    private SheetDetailResponse toSheetDetail(MigrationSheetEntity sheet, int previewLimit) {
        return new SheetDetailResponse(
                sheet.getModule(),
                mapper.toSheetSummary(sheet),
                previewRows(sheet, previewLimit),
                collectIssues(sheet));
    }

    private List<Map<String, String>> previewRows(MigrationSheetEntity sheet, int limit) {
        return sheet.getRows().stream()
                .filter(MigrationRowEntity::isValid)
                .sorted(Comparator.comparingInt(MigrationRowEntity::getRowNumber))
                .limit(limit)
                .map(row -> readMap(row.getNormalizedJson()))
                .toList();
    }

    private List<RowIssueResponse> collectIssues(MigrationSheetEntity sheet) {
        List<RowIssueResponse> issues = new ArrayList<>();
        for (MigrationRowEntity row : sheet.getRows()) {
            issues.addAll(readIssues(row.getErrorsJson()));
            issues.addAll(readIssues(row.getWarningsJson()));
        }
        return issues.stream()
                .sorted(Comparator.comparingInt(RowIssueResponse::rowNumber))
                .toList();
    }

    private List<Map<String, String>> validRows(MigrationSheetEntity sheet) {
        return sheet.getRows().stream()
                .filter(MigrationRowEntity::isValid)
                .sorted(Comparator.comparingInt(MigrationRowEntity::getRowNumber))
                .map(row -> (Map<String, String>) new LinkedHashMap<>(readMap(row.getNormalizedJson())))
                .toList();
    }

    private String normalizeDocument(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String migratorDatabase() {
        String migratorDatabase = properties.getFirebird().getMigratorDatabase();
        if (migratorDatabase == null || migratorDatabase.isBlank()) {
            throw new BusinessException("Banco MIGRADOR nao configurado no ambiente.");
        }
        return migratorDatabase.trim();
    }

    private MigrationProcessEntity getProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo de migracao nao encontrado."));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Falha ao serializar dados tratados.", exception);
        }
    }

    private Map<String, String> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Falha ao ler dados tratados.", exception);
        }
    }

    private List<RowIssueResponse> readIssues(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<RowIssue> issues = objectMapper.readValue(json, new TypeReference<>() {
            });
            return issues.stream()
                    .map(issue -> new RowIssueResponse(issue.rowNumber(), issue.field(), issue.message(), issue.severity()))
                    .toList();
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Falha ao ler inconsistencias da planilha.", exception);
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
