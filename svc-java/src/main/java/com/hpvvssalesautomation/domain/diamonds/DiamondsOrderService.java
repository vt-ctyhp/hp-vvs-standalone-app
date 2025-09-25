package com.hpvvssalesautomation.domain.diamonds;

import com.hpvvssalesautomation.adapters.DiamondsAdapter;
import com.hpvvssalesautomation.config.FeatureFlags;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DiamondsOrderService {

    private final FeatureFlags featureFlags;
    private final DiamondsAdapter diamondsAdapter;
    private final TimeUtil timeUtil;

    public DiamondsOrderService(FeatureFlags featureFlags,
                                DiamondsAdapter diamondsAdapter,
                                TimeUtil timeUtil) {
        this.featureFlags = featureFlags;
        this.diamondsAdapter = diamondsAdapter;
        this.timeUtil = timeUtil;
    }

    public DiamondsActionResponse approve(DiamondsOrderApprovalsRequest request) {
        featureFlags.requireDiamondsEnabled();

        Map<String, AggregatedResult> aggregatedResults = new LinkedHashMap<>();
        boolean applyDefaults = Boolean.TRUE.equals(request.getApplyDefaultsToAll());
        String defaultOrderedBy = trim(request.getDefaultOrderedBy());
        LocalDate defaultOrderedDate = parseDate(request.getDefaultOrderedDate(), "defaultOrderedDate");

        for (DiamondsOrderApprovalItem item : request.getItems()) {
            String rootApptId = requireRoot(item.getRootApptId());
            String decision = normalizeDecision(item.getDecision());

            String itemOrderedBy = trim(item.getOrderedBy());
            LocalDate itemOrderedDate = parseDate(item.getOrderedDate(), "orderedDate");

            String orderedBy = applyDefaults ? defaultOrderedBy : (itemOrderedBy != null ? itemOrderedBy : defaultOrderedBy);
            LocalDate orderedDate = applyDefaults ? defaultOrderedDate : (itemOrderedDate != null ? itemOrderedDate : defaultOrderedDate);

            ZonedDateTime now = timeUtil.nowZoned();
            int updatedRows = diamondsAdapter.applyOrderDecision(rootApptId, decision, orderedBy, orderedDate, now);

            DiamondsCounts counts = diamondsAdapter.readCounts(rootApptId);
            String label = DiamondsSummaryResolver.resolve(counts);
            diamondsAdapter.persistSummary(counts, label, now);

            DiamondsSummary summary = new DiamondsSummary(
                    rootApptId,
                    counts.totalCount(),
                    counts.proposingCount(),
                    counts.notApprovedCount(),
                    counts.onTheWayCount(),
                    counts.deliveredCount(),
                    counts.inStockCount(),
                    counts.keepCount(),
                    counts.returnCount(),
                    counts.replaceCount(),
                    label,
                    now.toOffsetDateTime()
            );

            aggregatedResults.merge(
                    rootApptId,
                    new AggregatedResult(updatedRows, summary),
                    (existing, current) -> new AggregatedResult(existing.affectedRows() + current.affectedRows(), current.summary())
            );
        }

        List<DiamondsActionResult> responses = aggregatedResults.values().stream()
                .map(result -> new DiamondsActionResult(
                        result.summary().rootApptId(),
                        result.affectedRows(),
                        result.summary().centerStoneOrderStatus(),
                        result.summary().countsMap(),
                        result.summary().updatedAt()
                ))
                .toList();

        return new DiamondsActionResponse(responses);
    }

    private record AggregatedResult(int affectedRows, DiamondsSummary summary) {
    }

    private String requireRoot(String value) {
        String trimmed = trim(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("rootApptId is required");
        }
        return trimmed;
    }

    private String normalizeDecision(String decision) {
        String trimmed = trim(decision);
        if (trimmed == null) {
            throw new IllegalArgumentException("decision is required");
        }
        String normalized = trimmed.toLowerCase(Locale.US);
        if (Objects.equals(normalized, "on the way") || Objects.equals(normalized, "on_the_way") || Objects.equals(normalized, "on-the-way")) {
            return "On the Way";
        }
        if (Objects.equals(normalized, "not approved") || Objects.equals(normalized, "not_approved") || Objects.equals(normalized, "not-approved")) {
            return "Not Approved";
        }
        throw new IllegalArgumentException("Unsupported order decision: " + decision);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return timeUtil.parseDate(value)
                .orElseThrow(() -> new IllegalArgumentException("Invalid " + fieldName + ": " + value));
    }
}
