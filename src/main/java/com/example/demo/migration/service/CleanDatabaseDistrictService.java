package com.example.demo.migration.service;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.controller.dto.DistrictResponse;
import com.example.demo.migration.exception.BusinessException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CleanDatabaseDistrictService {

    private final CleanDatabaseTemplateService templateService;
    private final MigrationProperties properties;

    public CleanDatabaseDistrictService(CleanDatabaseTemplateService templateService, MigrationProperties properties) {
        this.templateService = templateService;
        this.properties = properties;
    }

    public List<DistrictResponse> search(String version, String search) {
        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT FIRST 20 DISTRITOS_ID, DESCRICAO, DISTRITO_CEP, ESTADOS_ID
                FROM DISTRITOS
                WHERE CAST(DISTRITOS_ID AS VARCHAR(20)) = ?
                   OR UPPER(DESCRICAO) LIKE UPPER(?)
                ORDER BY DESCRICAO
                """;
        try (Connection connection = connection(version);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSearch);
            statement.setString(2, "%" + normalizedSearch + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DistrictResponse> districts = new ArrayList<>();
                while (resultSet.next()) {
                    districts.add(mapDistrict(resultSet));
                }
                return districts;
            }
        } catch (SQLException exception) {
            throw new BusinessException("Nao foi possivel consultar distritos no banco limpo da versao " + version
                    + ". Verifique a conexao Firebird e o layout da tabela DISTRITOS.", exception);
        }
    }

    public DistrictResponse getById(String version, Integer districtId) {
        String sql = """
                SELECT FIRST 1 DISTRITOS_ID, DESCRICAO, DISTRITO_CEP, ESTADOS_ID
                FROM DISTRITOS
                WHERE DISTRITOS_ID = ?
                """;
        try (Connection connection = connection(version);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, districtId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDistrict(resultSet);
                }
                throw new BusinessException("Distrito " + districtId + " nao encontrado no banco limpo da versao " + version + ".");
            }
        } catch (SQLException exception) {
            throw new BusinessException("Nao foi possivel consultar o distrito " + districtId + " no banco limpo da versao " + version + ".", exception);
        }
    }

    private Connection connection(String version) throws SQLException {
        Path localTemplate = templateService.localTemplatePathForVersion(version);
        MigrationProperties.Firebird firebird = properties.getFirebird();
        return DriverManager.getConnection(jdbcUrl(localTemplate), firebird.getUsername(), firebird.getPassword());
    }

    private String jdbcUrl(Path databasePath) {
        String configured = properties.getFirebird().getJdbcUrl();
        if (configured != null && configured.contains("{database}")) {
            return configured.replace("{database}", databasePath.toString());
        }
        return configured;
    }

    private DistrictResponse mapDistrict(ResultSet resultSet) throws SQLException {
        return new DistrictResponse(
                resultSet.getInt("DISTRITOS_ID"),
                resultSet.getString("DESCRICAO"),
                resultSet.getString("DISTRITO_CEP"),
                resultSet.getInt("ESTADOS_ID"));
    }
}
