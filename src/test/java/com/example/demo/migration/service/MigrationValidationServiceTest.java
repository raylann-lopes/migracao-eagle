package com.example.demo.migration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.migration.domain.MigrationModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MigrationValidationServiceTest {

    private final MigrationValidationService service = new MigrationValidationService(new MigrationLayoutRegistry());

    @Test
    @DisplayName("Should validate data rows after header")
    void shouldValidateDataRowsAfterHeader() {
        ValidationResult result = service.validate(MigrationModule.PRODUTOS, data(MigrationModule.PRODUTOS, List.of(
                row(2, MigrationModule.PRODUTOS, Map.of(
                        "CODIGO", "0",
                        "TIPO", "00",
                        "PRODUTO", "Produto A",
                        "UNIDADE", "UN",
                        "PCO_VENDA", "10,50")),
                row(3, MigrationModule.PRODUTOS, Map.of(
                        "CODIGO", "10",
                        "TIPO", "00",
                        "PRODUTO", "Produto B",
                        "UNIDADE", "UN")),
                row(4, MigrationModule.PRODUTOS, Map.of(
                        "CODIGO", "10",
                        "TIPO", "00",
                        "PRODUTO", "Produto C",
                        "UNIDADE", "UN")))));

        assertThat(result.errorCount()).isEqualTo(3);
        assertThat(result.validRows()).isEqualTo(0);
        assertThat(result.rows().getFirst().normalizedValues().get("PCO_VENDA")).isEqualTo("10.5");
        assertThat(result.rows())
                .flatExtracting(ValidatedRow::errors)
                .extracting(RowIssue::message)
                .contains("Codigo deve iniciar em 1; ID 0 nao pode ser migrado", "Codigo duplicado na planilha");
    }

    @Test
    @DisplayName("Should accept spreadsheet with only header and no data rows")
    void shouldAcceptSpreadsheetWithOnlyHeaderAndNoDataRows() {
        ValidationResult result = service.validate(MigrationModule.CLIENTES, data(MigrationModule.CLIENTES, List.of()));

        assertThat(result.errorCount()).isZero();
        assertThat(result.totalRows()).isZero();
    }

    @Test
    @DisplayName("Should validate required fields and normalize values")
    void shouldValidateRequiredFieldsAndNormalizeValues() {
        ValidationResult result = service.validate(MigrationModule.CLIENTES, data(MigrationModule.CLIENTES, List.of(
                row(2, MigrationModule.CLIENTES, Map.of(
                        "CODIGO", "",
                        "TIPOPESSOA", "f",
                        "NOME", " Maria  Silva ",
                        "CADASTRO", "data-invalida")))));

        assertThat(result.errorCount()).isEqualTo(3);
        assertThat(result.warningCount()).isEqualTo(1);
        assertThat(result.rows().getFirst().normalizedValues())
                .containsEntry("TIPOPESSOA", "F")
                .containsEntry("NOME", "Maria Silva")
                .containsEntry("CADASTRO", "data-invalida");
        assertThat(result.rows().getFirst().errors())
                .extracting(RowIssue::message)
                .contains("Campo obrigatorio nao informado", "Data deve estar no formato dd/mm/aaaa");
    }

    @Test
    @DisplayName("Should report missing layout columns")
    void shouldReportMissingLayoutColumns() {
        ValidationResult result = service.validate(MigrationModule.ARECEBER, new SpreadsheetData(List.of("CODIGO"), List.of(row(2, Map.of("CODIGO", "1")))));

        assertThat(result.layoutIssues())
                .extracting(RowIssue::field)
                .contains("CLIENTES_ID", "DOCUMENTO", "EMISSAO", "VENCIMENTO", "VALOR");
    }

    @Test
    @DisplayName("Should report unexpected layout columns")
    void shouldReportUnexpectedLayoutColumns() {
        List<String> headers = new java.util.ArrayList<>(headers(MigrationModule.CLIENTES));
        headers.add("COLUNA_ERRADA");

        ValidationResult result = service.validate(MigrationModule.CLIENTES, new SpreadsheetData(headers, List.of()));

        assertThat(result.layoutIssues())
                .extracting(RowIssue::field)
                .contains("COLUNA_ERRADA");
    }

    @Test
    @DisplayName("Should validate payable layout")
    void shouldValidatePayableLayout() {
        ValidationResult result = service.validate(MigrationModule.APAGAR, data(MigrationModule.APAGAR, List.of(
                row(2, MigrationModule.APAGAR, Map.of(
                        "CODIGO", "1",
                        "FORNECEDORES_ID", "4",
                        "DOCUMENTO", "NF123",
                        "EMISSAO", "01/05/2026",
                        "VENCIMENTO", "10/05/2026",
                        "VALOR", "150,75")))));

        assertThat(result.errorCount()).isZero();
        assertThat(result.rows().getFirst().normalizedValues())
                .containsEntry("VALOR", "150.75")
                .containsEntry("VENCIMENTO", "10/05/2026");
    }

    private SpreadsheetData data(MigrationModule module, List<SheetRow> rows) {
        return new SpreadsheetData(headers(module), rows);
    }

    private List<String> headers(MigrationModule module) {
        return new MigrationLayoutRegistry().get(module).fields().stream().map(FieldSpec::name).toList();
    }

    private SheetRow row(int number, Map<String, String> values) {
        return new SheetRow(number, values);
    }

    private SheetRow row(int number, MigrationModule module, Map<String, String> values) {
        Map<String, String> complete = new HashMap<>();
        new MigrationLayoutRegistry().get(module).fields().forEach(field -> complete.put(field.name(), null));
        complete.putAll(values);
        return new SheetRow(number, complete);
    }
}
