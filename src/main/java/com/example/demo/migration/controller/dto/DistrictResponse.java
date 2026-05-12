package com.example.demo.migration.controller.dto;

public record DistrictResponse(
        Integer id,
        String description,
        String cep,
        Integer stateId) {
}
