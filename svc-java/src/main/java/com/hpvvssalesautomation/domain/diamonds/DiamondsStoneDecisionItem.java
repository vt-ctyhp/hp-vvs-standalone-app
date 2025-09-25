package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.constraints.NotBlank;

public class DiamondsStoneDecisionItem {

    @NotBlank
    private String rootApptId;

    @NotBlank
    private String decision;

    private String decidedBy;
    private String decidedDate;

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

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecidedDate() {
        return decidedDate;
    }

    public void setDecidedDate(String decidedDate) {
        this.decidedDate = decidedDate;
    }
}
