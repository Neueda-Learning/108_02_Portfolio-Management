package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Suggested or actual allocation share for a given asset type")
public class TargetAllocationDTO {
    private String assetType;
    private BigDecimal percentage;

    public TargetAllocationDTO() {
    }

    public TargetAllocationDTO(String assetType, BigDecimal percentage) {
        this.assetType = assetType;
        this.percentage = percentage;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}


