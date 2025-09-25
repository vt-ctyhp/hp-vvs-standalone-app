package com.hpvvssalesautomation.domain.payments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentRecordRequest {

    @NotBlank
    private String anchorType;

    private String rootApptId;

    private String soNumber;

    private String docNumber;

    private String docRole;

    @NotBlank
    private String docType;

    private String docStatus;

    @NotBlank
    private String paymentDateTime;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal amountGross;

    private BigDecimal feePercent;

    private BigDecimal feeAmount;

    private BigDecimal subtotal;

    @NotBlank
    private String method;

    private String reference;

    private String notes;

    @Valid
    private List<PaymentRecordRequestLine> lines = new ArrayList<>();

    public String getAnchorType() {
        return anchorType;
    }

    public void setAnchorType(String anchorType) {
        this.anchorType = anchorType;
    }

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public void setSoNumber(String soNumber) {
        this.soNumber = soNumber;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getDocRole() {
        return docRole;
    }

    public void setDocRole(String docRole) {
        this.docRole = docRole;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(String docStatus) {
        this.docStatus = docStatus;
    }

    public String getPaymentDateTime() {
        return paymentDateTime;
    }

    public void setPaymentDateTime(String paymentDateTime) {
        this.paymentDateTime = paymentDateTime;
    }

    public BigDecimal getAmountGross() {
        return amountGross;
    }

    public void setAmountGross(BigDecimal amountGross) {
        this.amountGross = amountGross;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(BigDecimal feePercent) {
        this.feePercent = feePercent;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PaymentRecordRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<PaymentRecordRequestLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }

    public static class PaymentRecordRequestLine {

        private String desc;

        private BigDecimal qty;

        private BigDecimal amt;

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public BigDecimal getAmt() {
            return amt;
        }

        public void setAmt(BigDecimal amt) {
            this.amt = amt;
        }
    }
}
