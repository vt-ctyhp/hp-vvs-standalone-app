package com.hpvvssalesautomation.domain.payments;

import com.hpvvssalesautomation.util.TimeUtil;
import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PaymentsService {

    private static final Set<String> RECEIPT_ROLES = Set.of("RECEIPT");
    private static final Set<String> BLOCKED_STATUSES = Set.of("VOID", "VOIDED", "CANCELLED", "CANCELED", "REVERSED");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PaymentsValidator validator;
    private final PaymentsMapper mapper;
    private final TimeUtil timeUtil;
    private final ZoneId zoneId;

    public PaymentsService(NamedParameterJdbcTemplate jdbcTemplate,
                           PaymentsValidator validator,
                           PaymentsMapper mapper,
                           TimeUtil timeUtil,
                           ZoneId zoneId) {
        this.jdbcTemplate = jdbcTemplate;
        this.validator = validator;
        this.mapper = mapper;
        this.timeUtil = timeUtil;
        this.zoneId = zoneId;
    }

    public PaymentRecordResult record(PaymentRecordRequest request) {
        ValidatedPaymentRecord validated = validator.validate(request);
        String requestHash = computeRequestHash(validated);
        String docNumber = findDocNumberByHash(validated.anchorType(), requestHash)
                .orElseGet(() -> Objects.requireNonNullElseGet(
                        validated.docNumber(),
                        () -> generateDocNumber(validated, requestHash)
                ));
        boolean existing = exists(docNumber);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("doc_number", docNumber)
                .addValue("doc_role", validated.docRole())
                .addValue("anchor_type", validated.anchorType())
                .addValue("root_appt_id", validated.rootApptId())
                .addValue("so_number", validated.soNumber())
                .addValue("basket_id", null)
                .addValue("doc_type", validated.docType())
                .addValue("doc_status", validated.docStatus())
                .addValue("payment_datetime", Optional.ofNullable(validated.paymentDateTime()).map(ZonedDateTime::toOffsetDateTime).orElse(null))
                .addValue("method", validated.method())
                .addValue("reference", validated.reference())
                .addValue("notes", validated.notes())
                .addValue("amount_gross", validated.amountGross())
                .addValue("fee_percent", validated.feePercent())
                .addValue("fee_amount", validated.feeAmount())
                .addValue("subtotal", validated.subtotal())
                .addValue("amount_net", validated.amountNet())
                .addValue("allocated_to_so", null)
                .addValue("lines_json", toJsonb(mapper.linesToJson(validated.lines())))
                .addValue("order_total_so", null)
                .addValue("paid_to_date_so", null)
                .addValue("balance_so", null)
                .addValue("submitted_by", "payments-service")
                .addValue("submitted_at", timeUtil.nowZoned().toOffsetDateTime())
                .addValue("request_hash", requestHash);

        jdbcTemplate.update("INSERT INTO payments_ledger (doc_number, doc_role, anchor_type, root_appt_id, so_number, basket_id, doc_type, doc_status, payment_datetime, method, reference, notes, amount_gross, fee_percent, fee_amount, subtotal, amount_net, allocated_to_so, lines_json, order_total_so, paid_to_date_so, balance_so, submitted_by, submitted_at, request_hash, created_at, updated_at) " +
                        "VALUES (:doc_number, :doc_role, :anchor_type, :root_appt_id, :so_number, :basket_id, :doc_type, :doc_status, :payment_datetime, :method, :reference, :notes, :amount_gross, :fee_percent, :fee_amount, :subtotal, :amount_net, :allocated_to_so, :lines_json, :order_total_so, :paid_to_date_so, :balance_so, :submitted_by, :submitted_at, :request_hash, NOW(), NOW()) " +
                        "ON CONFLICT (doc_number) DO UPDATE SET doc_role = EXCLUDED.doc_role, anchor_type = EXCLUDED.anchor_type, root_appt_id = EXCLUDED.root_appt_id, so_number = EXCLUDED.so_number, basket_id = EXCLUDED.basket_id, doc_type = EXCLUDED.doc_type, doc_status = EXCLUDED.doc_status, payment_datetime = EXCLUDED.payment_datetime, method = EXCLUDED.method, reference = EXCLUDED.reference, notes = EXCLUDED.notes, amount_gross = EXCLUDED.amount_gross, fee_percent = EXCLUDED.fee_percent, fee_amount = EXCLUDED.fee_amount, subtotal = EXCLUDED.subtotal, amount_net = EXCLUDED.amount_net, allocated_to_so = EXCLUDED.allocated_to_so, lines_json = EXCLUDED.lines_json, order_total_so = EXCLUDED.order_total_so, paid_to_date_so = EXCLUDED.paid_to_date_so, balance_so = EXCLUDED.balance_so, submitted_by = COALESCE(payments_ledger.submitted_by, EXCLUDED.submitted_by), submitted_at = COALESCE(payments_ledger.submitted_at, EXCLUDED.submitted_at), request_hash = EXCLUDED.request_hash, updated_at = NOW()",
                params
        );

        PaymentSummaryEntry entry = fetchByDocNumber(docNumber)
                .orElseThrow(() -> new IllegalStateException("Failed to load payment record " + docNumber));

        return new PaymentRecordResult(
                existing ? PaymentRecordStatus.UPDATED : PaymentRecordStatus.CREATED,
                entry.docNumber(),
                entry.docRole(),
                entry.anchorType(),
                entry.rootApptId(),
                entry.soNumber(),
                entry.docType(),
                entry.docStatus(),
                entry.paymentDateTime(),
                entry.method(),
                entry.reference(),
                entry.notes(),
                entry.amountGross(),
                entry.feePercent(),
                entry.feeAmount(),
                entry.subtotal(),
                entry.amountNet(),
                entry.lines(),
                entry.requestHash()
        );
    }

    public PaymentsSummaryResponse summarize(String rootApptId, String soNumber) {
        String trimmedRoot = trimToNull(rootApptId);
        String trimmedSo = trimToNull(soNumber);
        if (trimmedRoot == null && trimmedSo == null) {
            throw new IllegalArgumentException("rootApptId or soNumber is required");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", trimmedRoot)
                .addValue("so_number", trimmedSo);

        StringBuilder sql = new StringBuilder("SELECT * FROM payments_ledger WHERE 1=1");
        if (trimmedRoot != null) {
            sql.append(" AND root_appt_id = :root_appt_id");
        }
        if (trimmedSo != null) {
            sql.append(" AND so_number = :so_number");
        }
        sql.append(" ORDER BY COALESCE(payment_datetime, submitted_at)");

        List<PaymentSummaryEntry> entries = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            List<PaymentLine> lines = mapper.linesFromJson(rs.getString("lines_json"));
            OffsetDateTime paymentDateTime = rs.getObject("payment_datetime", OffsetDateTime.class);
            OffsetDateTime submittedAt = rs.getObject("submitted_at", OffsetDateTime.class);
            return new PaymentSummaryEntry(
                    rs.getString("doc_number"),
                    rs.getString("doc_role"),
                    rs.getString("anchor_type"),
                    rs.getString("root_appt_id"),
                    rs.getString("so_number"),
                    rs.getString("doc_type"),
                    rs.getString("doc_status"),
                    paymentDateTime == null ? null : timeUtil.formatDateTime(paymentDateTime.atZoneSameInstant(zoneId)),
                    rs.getString("method"),
                    rs.getString("reference"),
                    rs.getString("notes"),
                    rs.getBigDecimal("amount_gross"),
                    rs.getBigDecimal("fee_percent"),
                    rs.getBigDecimal("fee_amount"),
                    rs.getBigDecimal("subtotal"),
                    rs.getBigDecimal("amount_net"),
                    lines,
                    rs.getString("request_hash"),
                    submittedAt == null ? null : timeUtil.formatDateTime(submittedAt.atZoneSameInstant(zoneId))
            );
        });

        BigDecimal invoicesLinesSubtotal = sum(entries.stream()
                .filter(entry -> "INVOICE".equalsIgnoreCase(entry.docRole()))
                .map(PaymentSummaryEntry::subtotal)
                .toList());

        BigDecimal totalPayments = sum(entries.stream()
                .filter(entry -> RECEIPT_ROLES.contains(entry.docRole()))
                .filter(entry -> entry.amountNet() != null && entry.amountNet().compareTo(BigDecimal.ZERO) > 0)
                .filter(entry -> entry.docStatus() == null || !BLOCKED_STATUSES.contains(entry.docStatus().toUpperCase(Locale.US)))
                .map(PaymentSummaryEntry::amountNet)
                .toList());

        Map<String, BigDecimal> byMethod = entries.stream()
                .filter(entry -> RECEIPT_ROLES.contains(entry.docRole()))
                .filter(entry -> entry.amountNet() != null && entry.amountNet().compareTo(BigDecimal.ZERO) > 0)
                .filter(entry -> entry.docStatus() == null || !BLOCKED_STATUSES.contains(entry.docStatus().toUpperCase(Locale.US)))
                .filter(entry -> entry.method() != null)
                .collect(Collectors.toMap(
                        entry -> entry.method(),
                        PaymentSummaryEntry::amountNet,
                        BigDecimal::add,
                        LinkedHashMap::new
                ));

        byMethod = byMethod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().setScale(2, RoundingMode.HALF_UP),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        BigDecimal netLinesMinusPayments = invoicesLinesSubtotal.subtract(totalPayments);
        if (netLinesMinusPayments.compareTo(BigDecimal.ZERO) < 0) {
            netLinesMinusPayments = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            netLinesMinusPayments = netLinesMinusPayments.setScale(2, RoundingMode.HALF_UP);
        }

        return new PaymentsSummaryResponse(
                invoicesLinesSubtotal,
                totalPayments,
                netLinesMinusPayments,
                byMethod,
                entries
        );
    }

    private Optional<PaymentSummaryEntry> fetchByDocNumber(String docNumber) {
        MapSqlParameterSource params = new MapSqlParameterSource("doc_number", docNumber);
        List<PaymentSummaryEntry> rows = jdbcTemplate.query(
                "SELECT * FROM payments_ledger WHERE doc_number = :doc_number",
                params,
                (rs, rowNum) -> {
                    List<PaymentLine> lines = mapper.linesFromJson(rs.getString("lines_json"));
                    OffsetDateTime paymentDateTime = rs.getObject("payment_datetime", OffsetDateTime.class);
                    OffsetDateTime submittedAt = rs.getObject("submitted_at", OffsetDateTime.class);
                    return new PaymentSummaryEntry(
                            rs.getString("doc_number"),
                            rs.getString("doc_role"),
                            rs.getString("anchor_type"),
                            rs.getString("root_appt_id"),
                            rs.getString("so_number"),
                            rs.getString("doc_type"),
                            rs.getString("doc_status"),
                            paymentDateTime == null ? null : timeUtil.formatDateTime(paymentDateTime.atZoneSameInstant(zoneId)),
                            rs.getString("method"),
                            rs.getString("reference"),
                            rs.getString("notes"),
                            rs.getBigDecimal("amount_gross"),
                            rs.getBigDecimal("fee_percent"),
                            rs.getBigDecimal("fee_amount"),
                            rs.getBigDecimal("subtotal"),
                            rs.getBigDecimal("amount_net"),
                            lines,
                            rs.getString("request_hash"),
                            submittedAt == null ? null : timeUtil.formatDateTime(submittedAt.atZoneSameInstant(zoneId))
                    );
                }
        );
        return rows.stream().findFirst();
    }

    private Optional<String> findDocNumberByHash(String anchorType, String requestHash) {
        if (requestHash == null || anchorType == null) {
            return Optional.empty();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("anchor_type", anchorType)
                .addValue("request_hash", requestHash);
        List<String> docNumbers = jdbcTemplate.query(
                "SELECT doc_number FROM payments_ledger WHERE anchor_type = :anchor_type AND request_hash = :request_hash LIMIT 1",
                params,
                (rs, rowNum) -> rs.getString("doc_number")
        );
        return docNumbers.stream().findFirst();
    }

    private boolean exists(String docNumber) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM payments_ledger WHERE doc_number = :doc_number",
                    new MapSqlParameterSource("doc_number", docNumber),
                    Integer.class
            );
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String computeRequestHash(ValidatedPaymentRecord record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Objects.toString(record.anchorType(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.rootApptId(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.soNumber(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.docType(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.docStatus(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.docRole(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.method(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.reference(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.notes(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.amountGross(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.feePercent(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.feeAmount(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.subtotal(), "").getBytes(StandardCharsets.UTF_8));
            digest.update(Objects.toString(record.amountNet(), "").getBytes(StandardCharsets.UTF_8));
            if (record.paymentDateTime() != null) {
                digest.update(timeUtil.formatDateTime(record.paymentDateTime()).getBytes(StandardCharsets.UTF_8));
            }
            if (record.lines() != null) {
                digest.update(mapper.linesToJson(record.lines()).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String generateDocNumber(ValidatedPaymentRecord record, String requestHash) {
        String base = String.join("-",
                Optional.ofNullable(record.anchorType()).orElse("NA"),
                normalizeToken(record.docRole()),
                normalizeToken(record.docType()),
                record.paymentDateTime() == null ? "TS0" : String.valueOf(record.paymentDateTime().toInstant().toEpochMilli())
        );
        String hash = Optional.ofNullable(requestHash).orElseGet(() -> computeRequestHash(record));
        return (base + "-" + hash.substring(0, 8)).toUpperCase(Locale.US);
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "NA";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PGobject toJsonb(String json) {
        if (json == null) {
            return null;
        }
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(json);
            return object;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert payload to jsonb", e);
        }
    }
}
