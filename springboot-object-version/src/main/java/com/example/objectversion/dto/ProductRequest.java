package com.example.objectversion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must be >= 0")
        BigDecimal price,
        String description
) {
}
