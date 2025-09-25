package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class DiamondsOrderApprovalsRequest {

    @NotEmpty
    @Valid
    private List<DiamondsOrderApprovalItem> items = new ArrayList<>();

    private String defaultOrderedBy;
    private String defaultOrderedDate;
    private Boolean applyDefaultsToAll;

    public List<DiamondsOrderApprovalItem> getItems() {
        return items;
    }

    public void setItems(List<DiamondsOrderApprovalItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getDefaultOrderedBy() {
        return defaultOrderedBy;
    }

    public void setDefaultOrderedBy(String defaultOrderedBy) {
        this.defaultOrderedBy = defaultOrderedBy;
    }

    public String getDefaultOrderedDate() {
        return defaultOrderedDate;
    }

    public void setDefaultOrderedDate(String defaultOrderedDate) {
        this.defaultOrderedDate = defaultOrderedDate;
    }

    public Boolean getApplyDefaultsToAll() {
        return applyDefaultsToAll;
    }

    public void setApplyDefaultsToAll(Boolean applyDefaultsToAll) {
        this.applyDefaultsToAll = applyDefaultsToAll;
    }
}
