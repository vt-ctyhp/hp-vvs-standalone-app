package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class DiamondsConfirmDeliveryRequest {

    @NotEmpty
    @Valid
    private List<DiamondsConfirmDeliveryItem> items = new ArrayList<>();

    private String defaultMemoDate;
    private Boolean applyDefaultToAll;

    public List<DiamondsConfirmDeliveryItem> getItems() {
        return items;
    }

    public void setItems(List<DiamondsConfirmDeliveryItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getDefaultMemoDate() {
        return defaultMemoDate;
    }

    public void setDefaultMemoDate(String defaultMemoDate) {
        this.defaultMemoDate = defaultMemoDate;
    }

    public Boolean getApplyDefaultToAll() {
        return applyDefaultToAll;
    }

    public void setApplyDefaultToAll(Boolean applyDefaultToAll) {
        this.applyDefaultToAll = applyDefaultToAll;
    }
}
