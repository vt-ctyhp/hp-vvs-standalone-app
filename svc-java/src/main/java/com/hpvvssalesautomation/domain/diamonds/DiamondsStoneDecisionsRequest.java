package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class DiamondsStoneDecisionsRequest {

    @NotEmpty
    @Valid
    private List<DiamondsStoneDecisionItem> items = new ArrayList<>();

    private String defaultDecidedBy;
    private String defaultDecidedDate;
    private Boolean applyDefaultsToAll;

    public List<DiamondsStoneDecisionItem> getItems() {
        return items;
    }

    public void setItems(List<DiamondsStoneDecisionItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getDefaultDecidedBy() {
        return defaultDecidedBy;
    }

    public void setDefaultDecidedBy(String defaultDecidedBy) {
        this.defaultDecidedBy = defaultDecidedBy;
    }

    public String getDefaultDecidedDate() {
        return defaultDecidedDate;
    }

    public void setDefaultDecidedDate(String defaultDecidedDate) {
        this.defaultDecidedDate = defaultDecidedDate;
    }

    public Boolean getApplyDefaultsToAll() {
        return applyDefaultsToAll;
    }

    public void setApplyDefaultsToAll(Boolean applyDefaultsToAll) {
        this.applyDefaultsToAll = applyDefaultsToAll;
    }
}
