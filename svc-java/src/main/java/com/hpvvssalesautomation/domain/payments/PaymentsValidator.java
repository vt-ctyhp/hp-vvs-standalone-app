package com.hpvvssalesautomation.domain.payments;

import com.hpvvssalesautomation.domain.payments.PaymentRecordRequest.PaymentRecordRequestLine;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class PaymentsValidator {

    private static final Map<String, String> ANCHOR_TYPES = Map.of(
            "so", "SO",
            "appt", "APPT"
    );

    private static final Map<String, String> DOC_TYPES = Map.ofEntries(
            Map.entry("deposit invoice", "Deposit Invoice"),
            Map.entry("deposit receipt", "Deposit Receipt"),
            Map.entry("sales invoice", "Sales Invoice"),
            Map.entry("sales receipt", "Sales Receipt"),
            Map.entry("credit memo", "Credit Memo"),
            Map.entry("payment receipt", "Payment Receipt")
    );

    private static final Map<String, String> DOC_STATUSES = Map.of(
            "draft", "DRAFT",
            "issued", "ISSUED"
    );

    private static final Map<String, String> METHODS = Map.ofEntries(
            Map.entry("card", "Card"),
            Map.entry("wire", "Wire"),
            Map.entry("zelle", "Zelle"),
            Map.entry("cash", "Cash"),
            Map.entry("check", "Check"),
            Map.entry("cheque", "Check"),
            Map.entry("other", "Other")
    );

    private static final Set<String> RECEIPT_TYPES = Set.of(
            "Deposit Receipt",
            "Sales Receipt",
            "Payment Receipt"
    );

    private final TimeUtil timeUtil;

    public PaymentsValidator(TimeUtil timeUtil) {
        this.timeUtil = timeUtil;
    }

    public ValidatedPaymentRecord validate(PaymentRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request is required");
        }

        String anchorType = resolveEnum(request.getAnchorType(), ANCHOR_TYPES, "anchorType");
        String rootApptId = trimToNull(request.getRootApptId());
        String soNumber = trimToNull(request.getSoNumber());

        if ("SO".equals(anchorType) && soNumber == null) {
            throw new IllegalArgumentException("soNumber is required when anchorType=SO");
        }
        if ("APPT".equals(anchorType) && rootApptId == null) {
            throw new IllegalArgumentException("rootApptId is required when anchorType=APPT");
        }

        String docType = resolveEnum(request.getDocType(), DOC_TYPES, "docType");
        String docStatus = Optional.ofNullable(trimToNull(request.getDocStatus()))
                .map(value -> resolveEnum(value, DOC_STATUSES, "docStatus"))
                .orElse("ISSUED");

        String method = resolveEnum(request.getMethod(), METHODS, "method");
        String docRole = Optional.ofNullable(trimToNull(request.getDocRole()))
                .map(value -> value.toUpperCase(Locale.US))
                .orElseGet(() -> inferDocRole(docType));

        String docNumber = trimToNull(request.getDocNumber());
        String reference = trimToNull(request.getReference());
        String notes = trimToNull(request.getNotes());

        BigDecimal amountGross = requirePositive(request.getAmountGross(), "amountGross");
        BigDecimal feePercent = normalizePercent(request.getFeePercent(), "feePercent");
        BigDecimal feeAmount = normalizeMoney(request.getFeeAmount(), "feeAmount");

        ZonedDateTime paymentDateTime = timeUtil.parseDateTime(request.getPaymentDateTime())
                .orElseThrow(() -> new IllegalArgumentException("paymentDateTime must be a valid ISO-8601 string"));

        List<PaymentLine> sanitizedLines = sanitizeLines(request.getLines());
        BigDecimal subtotal = sanitizedLines.stream()
                .map(PaymentLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountNet = computeNet(amountGross, feePercent, feeAmount);
        if (amountNet.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amountNet cannot be negative");
        }

        return new ValidatedPaymentRecord(
                docNumber,
                docRole,
                anchorType,
                rootApptId,
                soNumber,
                docType,
                docStatus,
                paymentDateTime,
                method,
                reference,
                notes,
                amountGross.setScale(2, RoundingMode.HALF_UP),
                feePercent,
                feeAmount,
                subtotal,
                amountNet.setScale(2, RoundingMode.HALF_UP),
                sanitizedLines
        );
    }

    private String resolveEnum(String rawValue, Map<String, String> allowed, String field) {
        String value = trimToNull(rawValue);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = allowed.get(value.trim().toLowerCase(Locale.US));
        if (normalized == null) {
            throw new IllegalArgumentException(field + " must be one of: " + String.join(", ", new ArrayList<>(allowed.values())));
        }
        return normalized;
    }

    private String inferDocRole(String docType) {
        if (RECEIPT_TYPES.contains(docType)) {
            return "RECEIPT";
        }
        if ("Credit Memo".equals(docType)) {
            return "CREDIT";
        }
        return "INVOICE";
    }

    private List<PaymentLine> sanitizeLines(List<PaymentRecordRequestLine> lines) {
        if (lines == null) {
            return List.of();
        }
        List<PaymentLine> sanitized = new ArrayList<>();
        for (PaymentRecordRequestLine line : lines) {
            if (line == null) {
                continue;
            }
            BigDecimal qty = Optional.ofNullable(line.getQty())
                    .map(value -> {
                        if (value.compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalArgumentException("lines[].qty must be >= 0");
                        }
                        return value;
                    })
                    .orElse(BigDecimal.ONE);
            BigDecimal amt = normalizeMoney(line.getAmt(), "lines[].amt");
            if (amt.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("lines[].amt must be >= 0");
            }
            BigDecimal lineTotal = amt.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            sanitized.add(new PaymentLine(trimToNull(line.getDesc()), qty, amt.setScale(2, RoundingMode.HALF_UP), lineTotal));
        }
        return sanitized;
    }

    private BigDecimal computeNet(BigDecimal amountGross, BigDecimal feePercent, BigDecimal feeAmount) {
        BigDecimal normalizedFeeAmount = Optional.ofNullable(feeAmount)
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElse(null);

        if (normalizedFeeAmount == null && feePercent != null) {
            BigDecimal percentFee = amountGross
                    .multiply(feePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            normalizedFeeAmount = percentFee;
        }

        if (normalizedFeeAmount == null) {
            normalizedFeeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (feePercent != null && feeAmount != null) {
            BigDecimal derived = amountGross
                    .multiply(feePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            if (derived.subtract(normalizedFeeAmount).abs().compareTo(new BigDecimal("0.02")) > 0) {
                throw new IllegalArgumentException("feePercent and feeAmount disagree; provide only one or ensure they align");
            }
        }

        return amountGross.subtract(normalizedFeeAmount);
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        BigDecimal normalized = normalizeMoney(value, field);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    private BigDecimal normalizePercent(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and 100");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
