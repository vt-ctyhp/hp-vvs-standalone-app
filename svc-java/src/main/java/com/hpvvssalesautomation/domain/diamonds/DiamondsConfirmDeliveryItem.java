package com.hpvvssalesautomation.domain.diamonds;

import jakarta.validation.constraints.NotBlank;

public class DiamondsConfirmDeliveryItem {

    @NotBlank
    private String rootApptId;

    private String memoDate;
    private Boolean selected;

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getMemoDate() {
        return memoDate;
    }

    public void setMemoDate(String memoDate) {
        this.memoDate = memoDate;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
