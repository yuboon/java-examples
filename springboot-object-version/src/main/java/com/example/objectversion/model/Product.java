package com.example.objectversion.model;

import java.math.BigDecimal;

public record Product(
        String id,
        String name,
        BigDecimal price,
        String description
) {
}
