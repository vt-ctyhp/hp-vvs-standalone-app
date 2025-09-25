package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.constraints.NotBlank;

public class DiamondsOrderApprovalItem {

    @NotBlank
    private String rootApptId;

    @NotBlank
    private String decision;

    private String orderedBy;
    private String orderedDate;

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(String orderedBy) {
        this.orderedBy = orderedBy;
    }

    public String getOrderedDate() {
        return orderedDate;
    }

    public void setOrderedDate(String orderedDate) {
        this.orderedDate = orderedDate;
    }
}
