package com.hpvvssalesautomation.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ClientStatusSubmitRequest {

    @NotBlank
    @JsonProperty("rootApptId")
    private String rootApptId;

    @NotBlank
    @JsonProperty("salesStage")
    private String salesStage;

    @NotBlank
    @JsonProperty("conversionStatus")
    private String conversionStatus;

    @JsonProperty("customOrderStatus")
    private String customOrderStatus;

    @JsonProperty("inProductionStatus")
    private String inProductionStatus;

    @JsonProperty("centerStoneOrderStatus")
    private String centerStoneOrderStatus;

    @JsonProperty("nextSteps")
    private String nextSteps;

    @JsonProperty("assistedRep")
    private String assistedRep;

    @NotBlank
    @JsonProperty("updatedBy")
    private String updatedBy;

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getSalesStage() {
        return salesStage;
    }

    public void setSalesStage(String salesStage) {
        this.salesStage = salesStage;
    }

    public String getConversionStatus() {
        return conversionStatus;
    }

    public void setConversionStatus(String conversionStatus) {
        this.conversionStatus = conversionStatus;
    }

    public String getCustomOrderStatus() {
        return customOrderStatus;
    }

    public void setCustomOrderStatus(String customOrderStatus) {
        this.customOrderStatus = customOrderStatus;
    }

    public String getInProductionStatus() {
        return inProductionStatus;
    }

    public void setInProductionStatus(String inProductionStatus) {
        this.inProductionStatus = inProductionStatus;
    }

    public String getCenterStoneOrderStatus() {
        return centerStoneOrderStatus;
    }

    public void setCenterStoneOrderStatus(String centerStoneOrderStatus) {
        this.centerStoneOrderStatus = centerStoneOrderStatus;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    public String getAssistedRep() {
        return assistedRep;
    }

    public void setAssistedRep(String assistedRep) {
        this.assistedRep = assistedRep;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
