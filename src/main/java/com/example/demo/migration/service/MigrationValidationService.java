package com.example.demo.migration.service;

import com.example.demo.migration.domain.MigrationModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MigrationValidationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MigrationLayoutRegistry layoutRegistry;

    public MigrationValidationService(MigrationLayoutRegistry layoutRegistry) {
        this.layoutRegistry = layoutRegistry;
    }

    public ValidationResult validate(MigrationModule module, SpreadsheetData spreadsheetData) {
        LayoutSpec layout = layoutRegistry.get(module);
        List<RowIssue> layoutIssues = validateLayout(layout, spreadsheetData.headers());
        List<ValidatedRow> rows = new ArrayList<>();
        Map<String, Integer> keyOccurrences = new HashMap<>();

        for (SheetRow sheetRow : spreadsheetData.rows()) {
            Map<String, String> normalizedValues = new LinkedHashMap<>();
            List<RowIssue> errors = new ArrayList<>();
            List<RowIssue> warnings = new ArrayList<>();

            for (FieldSpec field : layout.fields()) {
                String rawValue = sheetRow.values().get(field.name());
                String normalized = normalizeValue(field, rawValue, errors, sheetRow.rowNumber());
                if (field.required() && isBlank(normalized)) {
                    errors.add(issue(sheetRow.rowNumber(), field.name(), "Campo obrigatorio nao informado", "ERROR"));
                }
                if (!isBlank(normalized) && field.maxLength() > 0 && normalized.length() > field.maxLength()) {
                    errors.add(issue(sheetRow.rowNumber(), field.name(), "Tamanho maior que o permitido (" + field.maxLength() + ")", "ERROR"));
                }
                normalizedValues.put(field.name(), normalized);
            }

            validateDomain(module, sheetRow.rowNumber(), normalizedValues, errors, warnings);
            String keyValue = normalizedValues.get(layout.keyField());
            if (!isBlank(keyValue)) {
                keyOccurrences.merge(keyValue, 1, Integer::sum);
            }
            rows.add(new ValidatedRow(sheetRow.rowNumber(), normalizedValues, errors, warnings));
        }

        addDuplicateKeyErrors(layout, rows, keyOccurrences);
        return new ValidationResult(rows, layoutIssues);
    }

    private List<RowIssue> validateLayout(LayoutSpec layout, List<String> headers) {
        if (headers.isEmpty()) {
            return List.of(issue(1, null, "Linha de cabecalho nao encontrada", "ERROR"));
        }
        Set<String> expectedHeaders = layout.fields().stream()
                .map(FieldSpec::name)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<String> actualHeaders = new HashSet<>(headers);
        List<RowIssue> issues = new ArrayList<>();
        for (FieldSpec field : layout.fields()) {
            if (!actualHeaders.contains(field.name())) {
                issues.add(issue(1, field.name(), "Coluna obrigatoria do layout nao encontrada", "ERROR"));
            }
        }
        for (String header : headers) {
            if (!isBlank(header) && !expectedHeaders.contains(header)) {
                issues.add(issue(1, header, "Coluna fora do layout esperado", "ERROR"));
            }
        }
        return issues;
    }

    private void addDuplicateKeyErrors(LayoutSpec layout, List<ValidatedRow> rows, Map<String, Integer> keyOccurrences) {
        for (ValidatedRow row : rows) {
            String keyValue = row.normalizedValues().get(layout.keyField());
            if (!isBlank(keyValue) && keyOccurrences.getOrDefault(keyValue, 0) > 1) {
                row.errors().add(issue(row.rowNumber(), layout.keyField(), "Codigo duplicado na planilha", "ERROR"));
            }
        }
    }

    private String normalizeValue(FieldSpec field, String value, List<RowIssue> errors, int rowNumber) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        return switch (field.type()) {
            case TEXT -> normalizeText(field.name(), trimmed);
            case INTEGER -> normalizeInteger(field.name(), trimmed, errors, rowNumber);
            case MONETARY -> normalizeMonetary(field.name(), trimmed, errors, rowNumber);
            case DATE -> normalizeDate(field.name(), trimmed, errors, rowNumber);
        };
    }

    private String normalizeText(String fieldName, String value) {
        String normalized = value.replaceAll("\\s+", " ");
        if (Set.of("EMAIL").contains(fieldName)) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        if (Set.of("TIPOPESSOA", "BLOQUEADO", "FRACIONAR", "INATIVO").contains(fieldName)) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    private String normalizeInteger(String fieldName, String value, List<RowIssue> errors, int rowNumber) {
        String normalized = value.replace(".", "").replace(",", "").trim();
        if (!normalized.matches("\\d+")) {
            errors.add(issue(rowNumber, fieldName, "Informe apenas numeros inteiros", "ERROR"));
            return value;
        }
        return normalized;
    }

    private String normalizeMonetary(String fieldName, String value, List<RowIssue> errors, int rowNumber) {
        String normalized = value.replace("R$", "").replace(" ", "").trim();
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else {
            normalized = normalized.replace(",", ".");
        }
        try {
            return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            errors.add(issue(rowNumber, fieldName, "Valor monetario invalido", "ERROR"));
            return value;
        }
    }

    private String normalizeDate(String fieldName, String value, List<RowIssue> errors, int rowNumber) {
        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMAT);
            return DATE_FORMAT.format(date);
        } catch (DateTimeParseException exception) {
            errors.add(issue(rowNumber, fieldName, "Data deve estar no formato dd/mm/aaaa", "ERROR"));
            return value;
        }
    }

    private void validateDomain(
            MigrationModule module,
            int rowNumber,
            Map<String, String> values,
            List<RowIssue> errors,
            List<RowIssue> warnings) {
        String keyField = layoutRegistry.get(module).keyField();
        if ("0".equals(values.get(keyField))) {
            errors.add(issue(rowNumber, keyField, "Codigo deve iniciar em 1; ID 0 nao pode ser migrado", "ERROR"));
        }
        if (values.containsKey("TIPOPESSOA") && !isBlank(values.get("TIPOPESSOA"))
                && !Set.of("F", "J").contains(values.get("TIPOPESSOA"))) {
            errors.add(issue(rowNumber, "TIPOPESSOA", "Use F para fisica ou J para juridica", "ERROR"));
        }
        for (String field : List.of("BLOQUEADO", "FRACIONAR", "INATIVO")) {
            if (values.containsKey(field) && !isBlank(values.get(field)) && !Set.of("S", "N").contains(values.get(field))) {
                errors.add(issue(rowNumber, field, "Use S ou N", "ERROR"));
            }
        }
        if (values.containsKey("TIPO_IE") && !isBlank(values.get("TIPO_IE"))
                && !Set.of("1", "2", "9").contains(values.get("TIPO_IE"))) {
            errors.add(issue(rowNumber, "TIPO_IE", "Use 1, 2 ou 9", "ERROR"));
        }
        if ((module == MigrationModule.CLIENTES || module == MigrationModule.FORNECEDORES)
                && isBlank(values.get("CNPJ")) && isBlank(values.get("CPF"))) {
            warnings.add(issue(rowNumber, "CNPJ", "Cliente/fornecedor sem CNPJ e CPF; conferir antes de migrar", "WARNING"));
        }
        if (module == MigrationModule.PRODUTOS && isBlank(values.get("COD_NCM"))) {
            warnings.add(issue(rowNumber, "COD_NCM", "Produto sem NCM; tributacao deve ser conferida pela contabilidade", "WARNING"));
        }
    }

    private RowIssue issue(int rowNumber, String field, String message, String severity) {
        return new RowIssue(rowNumber, field, message, severity);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
