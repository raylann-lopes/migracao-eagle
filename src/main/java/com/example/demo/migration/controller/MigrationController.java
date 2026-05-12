package com.example.demo.migration.controller;

import com.example.demo.migration.controller.dto.CleanDatabaseTemplateResponse;
import com.example.demo.migration.controller.dto.CreateMigrationProcessRequest;
import com.example.demo.migration.controller.dto.LayoutResponse;
import com.example.demo.migration.controller.dto.MigrationProcessResponse;
import com.example.demo.migration.controller.dto.ProcedureExecutionResponse;
import com.example.demo.migration.controller.dto.SheetDetailResponse;
import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.service.CleanDatabaseTemplateService;
import com.example.demo.migration.service.MigrationLayoutRegistry;
import com.example.demo.migration.service.MigrationMapper;
import com.example.demo.migration.service.MigrationProcessService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class MigrationController {

    private final MigrationProcessService processService;
    private final CleanDatabaseTemplateService cleanDatabaseTemplateService;
    private final MigrationLayoutRegistry layoutRegistry;
    private final MigrationMapper mapper;

    public MigrationController(
            MigrationProcessService processService,
            CleanDatabaseTemplateService cleanDatabaseTemplateService,
            MigrationLayoutRegistry layoutRegistry,
            MigrationMapper mapper) {
        this.processService = processService;
        this.cleanDatabaseTemplateService = cleanDatabaseTemplateService;
        this.layoutRegistry = layoutRegistry;
        this.mapper = mapper;
    }

    @PostMapping("/migration-processes")
    public ResponseEntity<MigrationProcessResponse> create(@Valid @RequestBody CreateMigrationProcessRequest request) {
        return ResponseEntity.ok(processService.create(request));
    }

    @GetMapping("/migration-processes")
    public List<MigrationProcessResponse> list() {
        return processService.list();
    }

    @GetMapping("/migration-processes/{processId}")
    public MigrationProcessResponse get(@PathVariable UUID processId) {
        return processService.get(processId);
    }

    @PostMapping(value = "/migration-processes/{processId}/sheets/{module}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SheetDetailResponse uploadSheet(
            @PathVariable UUID processId,
            @PathVariable MigrationModule module,
            @RequestParam("file") MultipartFile file) {
        return processService.uploadSheet(processId, module, file);
    }

    @GetMapping("/migration-processes/{processId}/sheets/{module}")
    public SheetDetailResponse getSheet(@PathVariable UUID processId, @PathVariable MigrationModule module) {
        return processService.getSheet(processId, module);
    }

    @GetMapping("/migration-processes/{processId}/sheets/{module}/errors.csv")
    public ResponseEntity<byte[]> errorsCsv(@PathVariable UUID processId, @PathVariable MigrationModule module) {
        byte[] body = processService.errorsCsv(processId, module).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(module.name().toLowerCase() + "-erros.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping("/migration-processes/{processId}/import-migrador")
    public MigrationProcessResponse importMigrador(@PathVariable UUID processId) {
        return processService.importValidSheets(processId);
    }

    @PostMapping("/migration-processes/{processId}/run-complete")
    public MigrationProcessResponse runComplete(@PathVariable UUID processId) {
        return processService.runCompleteMigration(processId);
    }

    @GetMapping("/migration-processes/{processId}/final-database")
    public ResponseEntity<Resource> downloadFinalDatabase(@PathVariable UUID processId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(processService.finalDatabaseFilename(processId)).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(processService.finalDatabaseResource(processId));
    }

    @PostMapping("/migration-processes/{processId}/procedures/execute-next")
    public ProcedureExecutionResponse executeNextProcedure(@PathVariable UUID processId) {
        return processService.executeNextProcedure(processId);
    }

    @PostMapping("/migration-processes/{processId}/procedures/{procedureName}/execute")
    public ProcedureExecutionResponse executeProcedure(
            @PathVariable UUID processId,
            @PathVariable String procedureName) {
        return processService.executeProcedure(processId, procedureName);
    }

    @GetMapping("/migration-layouts")
    public List<LayoutResponse> layouts() {
        return layoutRegistry.all().stream().map(mapper::toLayoutResponse).toList();
    }

    @GetMapping("/clean-database-templates")
    public List<CleanDatabaseTemplateResponse> cleanDatabaseTemplates() {
        return cleanDatabaseTemplateService.list();
    }
}
