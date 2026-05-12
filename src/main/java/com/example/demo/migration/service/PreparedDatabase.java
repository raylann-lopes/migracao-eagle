package com.example.demo.migration.service;

import java.nio.file.Path;

public record PreparedDatabase(
        Path workingDatabasePath,
        Path finalDatabasePath,
        String finalDatabaseFilename) {
}
