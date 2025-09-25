package com.hpvvssalesautomation.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppointmentSummaryRow {

    public static final String[] COLUMN_ORDER = new String[] {
            "Visit Date",
            "RootApptID",
            "Customer",
            "Phone",
            "Email",
            "Visit Type",
            "Visit #",
            "SO#",
            "Brand",
            "Sales Stage",
            "Conversion Status",
            "Custom Order Status",
            "Center Stone Order Status",
            "Assigned Rep",
            "Assisted Rep"
    };

    private String visitDate;
    private String rootApptId;
    private String customer;
    private String phone;
    private String email;
    private String visitType;
    private Integer visitNumber;
    private String soNumber;
    private String brand;
    private String salesStage;
    private String conversionStatus;
    private String customOrderStatus;
    private String centerStoneOrderStatus;
    private String assignedRep;
    private String assistedRep;

    public Map<String, Object> toOrderedMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Visit Date", visitDate);
        map.put("RootApptID", rootApptId);
        map.put("Customer", customer);
        map.put("Phone", phone);
        map.put("Email", email);
        map.put("Visit Type", visitType);
        map.put("Visit #", visitNumber);
        map.put("SO#", soNumber);
        map.put("Brand", brand);
        map.put("Sales Stage", salesStage);
        map.put("Conversion Status", conversionStatus);
        map.put("Custom Order Status", customOrderStatus);
        map.put("Center Stone Order Status", centerStoneOrderStatus);
        map.put("Assigned Rep", assignedRep);
        map.put("Assisted Rep", assistedRep);
        return map;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Integer getVisitNumber() {
        return visitNumber;
    }

    public void setVisitNumber(Integer visitNumber) {
        this.visitNumber = visitNumber;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public void setSoNumber(String soNumber) {
        this.soNumber = soNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public String getCenterStoneOrderStatus() {
        return centerStoneOrderStatus;
    }

    public void setCenterStoneOrderStatus(String centerStoneOrderStatus) {
        this.centerStoneOrderStatus = centerStoneOrderStatus;
    }

    public String getAssignedRep() {
        return assignedRep;
    }

    public void setAssignedRep(String assignedRep) {
        this.assignedRep = assignedRep;
    }

    public String getAssistedRep() {
        return assistedRep;
    }

    public void setAssistedRep(String assistedRep) {
        this.assistedRep = assistedRep;
    }
}
