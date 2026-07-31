package com.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PortfolioDTO(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PortfolioItemDTO> items
) {}
