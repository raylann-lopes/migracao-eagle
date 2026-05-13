package com.example.demo.migration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(MigrationProperties.class)
public class MigrationConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    S3Client s3Client(MigrationProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getCleanDatabase().getS3().getRegion()))
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
    }
}
