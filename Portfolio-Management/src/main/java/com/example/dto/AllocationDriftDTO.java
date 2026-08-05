package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Difference between current allocation and target allocation")
public class AllocationDriftDTO {
    private String assetType;
    private BigDecimal currentPercentage;
    private BigDecimal targetPercentage;
    private BigDecimal driftPercentage;

    public AllocationDriftDTO() {
    }

    public AllocationDriftDTO(String assetType, BigDecimal currentPercentage, BigDecimal targetPercentage, BigDecimal driftPercentage) {
        this.assetType = assetType;
        this.currentPercentage = currentPercentage;
        this.targetPercentage = targetPercentage;
        this.driftPercentage = driftPercentage;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public BigDecimal getCurrentPercentage() {
        return currentPercentage;
    }

    public void setCurrentPercentage(BigDecimal currentPercentage) {
        this.currentPercentage = currentPercentage;
    }

    public BigDecimal getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(BigDecimal targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    public BigDecimal getDriftPercentage() {
        return driftPercentage;
    }

    public void setDriftPercentage(BigDecimal driftPercentage) {
        this.driftPercentage = driftPercentage;
    }
}


