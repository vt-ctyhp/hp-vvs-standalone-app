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
public class DiamondsDecisionService {

    private final FeatureFlags featureFlags;
    private final DiamondsAdapter diamondsAdapter;
    private final TimeUtil timeUtil;

    public DiamondsDecisionService(FeatureFlags featureFlags,
                                   DiamondsAdapter diamondsAdapter,
                                   TimeUtil timeUtil) {
        this.featureFlags = featureFlags;
        this.diamondsAdapter = diamondsAdapter;
        this.timeUtil = timeUtil;
    }

    public DiamondsActionResponse decide(DiamondsStoneDecisionsRequest request) {
        featureFlags.requireDiamondsEnabled();

        Map<String, AggregatedResult> aggregatedResults = new LinkedHashMap<>();
        boolean applyDefaults = Boolean.TRUE.equals(request.getApplyDefaultsToAll());
        String defaultDecidedBy = trim(request.getDefaultDecidedBy());
        LocalDate defaultDecidedDate = parseDate(request.getDefaultDecidedDate(), "defaultDecidedDate");

        for (DiamondsStoneDecisionItem item : request.getItems()) {
            String rootApptId = requireRoot(item.getRootApptId());
            String stoneStatus = normalizeDecision(item.getDecision());

            String itemDecidedBy = trim(item.getDecidedBy());
            LocalDate itemDecidedDate = parseDate(item.getDecidedDate(), "decidedDate");

            String decidedBy = applyDefaults ? defaultDecidedBy : (itemDecidedBy != null ? itemDecidedBy : defaultDecidedBy);
            LocalDate decidedDate = applyDefaults ? defaultDecidedDate : (itemDecidedDate != null ? itemDecidedDate : defaultDecidedDate);
            if (decidedDate == null) {
                decidedDate = timeUtil.today();
            }

            ZonedDateTime now = timeUtil.nowZoned();
            int updatedRows = diamondsAdapter.applyStoneDecision(rootApptId, stoneStatus, decidedBy, decidedDate, now);

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
        if (Objects.equals(normalized, "keep")) {
            return "Keep";
        }
        if (Objects.equals(normalized, "return")) {
            return "Return";
        }
        if (Objects.equals(normalized, "replace")) {
            return "Replace";
        }
        throw new IllegalArgumentException("Unsupported stone decision: " + decision);
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
