package com.example.demo.migration.integration;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.FieldType;
import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.exception.BusinessException;
import com.example.demo.migration.service.FieldSpec;
import com.example.demo.migration.service.LayoutSpec;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FirebirdMigradorClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MigrationProperties properties;

    public FirebirdMigradorClient(MigrationProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.getFirebird().isEnabled();
    }

    public int replaceTable(String migratorDatabase, MigrationModule module, LayoutSpec layout, List<Map<String, String>> rows) {
        ensureEnabled();
        String tableName = tableName(module);
        validateSqlIdentifier(tableName);
        layout.fields().forEach(field -> validateSqlIdentifier(field.name()));

        try (Connection connection = connection(migratorDatabase)) {
            List<FieldSpec> importFields = importableFields(connection, tableName, layout);
            String columns = importFields.stream().map(FieldSpecSql::quoted).collect(Collectors.joining(", "));
            String placeholders = importFields.stream().map(field -> "?").collect(Collectors.joining(", "));
            String sql = "INSERT INTO " + quoted(tableName) + " (" + columns + ") VALUES (" + placeholders + ")";

            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + quoted(tableName));
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Map<String, String> row : rows) {
                    for (int index = 0; index < importFields.size(); index++) {
                        bindValue(statement, index + 1, importFields.get(index), row.get(importFields.get(index).name()));
                    }
                    statement.addBatch();
                }
                int[] affected = statement.executeBatch();
                connection.commit();
                int total = 0;
                for (int count : affected) {
                    if (count > 0) {
                        total += count;
                    }
                }
                return total;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new BusinessException("Falha ao importar para o MIGRADOR: " + exception.getMessage(), exception);
        }
    }

    private void bindValue(PreparedStatement statement, int parameterIndex, FieldSpec field, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(parameterIndex, sqlType(field.type()));
            return;
        }
        try {
            switch (field.type()) {
                case INTEGER -> statement.setLong(parameterIndex, Long.parseLong(value));
                case MONETARY -> statement.setBigDecimal(parameterIndex, new BigDecimal(value));
                case DATE -> statement.setDate(parameterIndex, Date.valueOf(LocalDate.parse(value, DATE_FORMAT)));
                case TEXT -> statement.setString(parameterIndex, value);
            }
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new BusinessException("Valor invalido para " + field.name() + ": " + value, exception);
        }
    }

    private int sqlType(FieldType fieldType) {
        return switch (fieldType) {
            case INTEGER -> Types.BIGINT;
            case MONETARY -> Types.NUMERIC;
            case DATE -> Types.DATE;
            case TEXT -> Types.VARCHAR;
        };
    }

    private List<FieldSpec> importableFields(Connection connection, String tableName, LayoutSpec layout) throws SQLException {
        Set<String> tableColumns = tableColumns(connection, tableName);
        List<FieldSpec> fields = layout.fields().stream()
                .filter(field -> tableColumns.contains(field.name().toUpperCase(Locale.ROOT)))
                .toList();
        if (fields.isEmpty()) {
            throw new BusinessException("Tabela " + tableName + " nao possui colunas compativeis com o layout " + layout.module() + ".");
        }
        return fields;
    }

    private Set<String> tableColumns(Connection connection, String tableName) throws SQLException {
        String sql = """
                select trim(rf.rdb$field_name) as column_name
                from rdb$relation_fields rf
                where upper(trim(rf.rdb$relation_name)) = ?
                order by rf.rdb$field_position
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<String> columns = new java.util.LinkedHashSet<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name").toUpperCase(Locale.ROOT));
                }
                if (columns.isEmpty()) {
                    throw new BusinessException("Tabela " + tableName + " nao encontrada no banco MIGRADOR.");
                }
                return columns;
            }
        }
    }

    public void executeProcedure(String migratorDatabase, String procedureName) {
        ensureEnabled();
        validateSqlIdentifier(procedureName);
        try (Connection connection = connection(migratorDatabase); Statement statement = connection.createStatement()) {
            statement.execute("EXECUTE PROCEDURE " + quoted(procedureName));
        } catch (SQLException exception) {
            throw new BusinessException("Falha ao executar procedure " + procedureName + ": " + exception.getMessage(), exception);
        }
    }

    public void configurarMigracao(
            String migratorDatabase,
            Integer defaultDistrictId,
            String defaultCep,
            String companyState,
            boolean migrateReceivables,
            boolean migrateClientes,
            boolean migrateFornecedores,
            boolean migrateGrupos,
            boolean migrateMarcas,
            boolean migrateProdutos,
            boolean migrateUnidades,
            boolean migrateKardexNegativo) {
        ensureEnabled();
        String sql = "EXECUTE PROCEDURE " + quoted("CONFIGURAR_MIGRACAO")
                + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection(migratorDatabase);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (defaultDistrictId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, defaultDistrictId);
            }
            if (defaultCep == null || defaultCep.isBlank()) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, defaultCep.trim());
            }
            statement.setString(3, companyState == null ? null : companyState.trim().toUpperCase());
            statement.setInt(4, migrateReceivables ? 1 : 0);
            statement.setInt(5, migrateClientes ? 1 : 0);
            statement.setInt(6, migrateFornecedores ? 1 : 0);
            statement.setInt(7, migrateGrupos ? 1 : 0);
            statement.setInt(8, migrateMarcas ? 1 : 0);
            statement.setInt(9, migrateProdutos ? 1 : 0);
            statement.setInt(10, migrateUnidades ? 1 : 0);
            statement.setInt(11, migrateKardexNegativo ? 1 : 0);
            statement.execute();
        } catch (SQLException exception) {
            throw new BusinessException("Falha ao configurar migracao: " + exception.getMessage(), exception);
        }
    }

    public void assertEnabledForFullMigration() {
        ensureEnabled();
    }

    public void assertEagleAliasAvailable() {
        ensureEnabled();
        String alias = properties.getFirebird().getEagleAliasName();
        try (Connection ignored = connection(alias)) {
            // Apenas valida se o alias configurado no Firebird consegue abrir o banco limpo.
        } catch (SQLException exception) {
            throw new BusinessException("Alias Firebird " + alias + " nao esta acessivel. "
                    + "No Docker, monte data/aliases.conf em /firebird/etc/aliases.conf "
                    + "e confirme que ele aponta para /firebird/data/work/EAGLEERP.FDB. "
                    + "Erro original: " + exception.getMessage(), exception);
        }
    }

    private Connection connection(String database) throws SQLException {
        MigrationProperties.Firebird firebird = properties.getFirebird();
        return DriverManager.getConnection(jdbcUrl(database), firebird.getUsername(), firebird.getPassword());
    }

    private String jdbcUrl(String database) {
        String configured = properties.getFirebird().getJdbcUrl();
        if (database != null && database.startsWith("jdbc:")) {
            return database;
        }
        if (configured != null && configured.contains("{database}")) {
            return configured.replace("{database}", database == null ? "" : database);
        }
        return configured;
    }

    private String tableName(MigrationModule module) {
        String tableName = properties.getFirebird().getTableMap().get(module);
        if (tableName == null || tableName.isBlank()) {
            throw new BusinessException("Tabela do MIGRADOR nao configurada para " + module);
        }
        return tableName;
    }

    private void ensureEnabled() {
        if (!enabled()) {
            throw new BusinessException("Integracao com Firebird MIGRADOR desabilitada. Configure app.migration.firebird.enabled=true.");
        }
    }

    private void validateSqlIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new BusinessException("Identificador SQL invalido: " + identifier);
        }
    }

    private String quoted(String identifier) {
        return "\"" + identifier + "\"";
    }

    private record FieldSpecSql() {
        private static String quoted(com.example.demo.migration.service.FieldSpec field) {
            return "\"" + field.name() + "\"";
        }
    }
}
