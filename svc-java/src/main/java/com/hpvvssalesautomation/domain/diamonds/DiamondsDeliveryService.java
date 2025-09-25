package com.hpvvssalesautomation.domain.diamonds;

import com.hpvvssalesautomation.adapters.DiamondsAdapter;
import com.hpvvssalesautomation.config.FeatureFlags;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiamondsDeliveryService {

    private final FeatureFlags featureFlags;
    private final DiamondsAdapter diamondsAdapter;
    private final TimeUtil timeUtil;

    public DiamondsDeliveryService(FeatureFlags featureFlags,
                                   DiamondsAdapter diamondsAdapter,
                                   TimeUtil timeUtil) {
        this.featureFlags = featureFlags;
        this.diamondsAdapter = diamondsAdapter;
        this.timeUtil = timeUtil;
    }

    public DiamondsActionResponse confirm(DiamondsConfirmDeliveryRequest request) {
        featureFlags.requireDiamondsEnabled();

        Map<String, AggregatedResult> aggregatedResults = new LinkedHashMap<>();
        boolean applyDefaults = Boolean.TRUE.equals(request.getApplyDefaultToAll());
        LocalDate defaultMemoDate = parseDate(request.getDefaultMemoDate(), "defaultMemoDate");

        for (DiamondsConfirmDeliveryItem item : request.getItems()) {
            if (!Boolean.TRUE.equals(item.getSelected())) {
                continue;
            }
            String rootApptId = requireRoot(item.getRootApptId());
            LocalDate itemMemoDate = parseDate(item.getMemoDate(), "memoDate");
            LocalDate memoDate;
            if (applyDefaults) {
                memoDate = defaultMemoDate != null ? defaultMemoDate : timeUtil.today();
            } else {
                memoDate = itemMemoDate != null ? itemMemoDate : (defaultMemoDate != null ? defaultMemoDate : timeUtil.today());
            }
            LocalDate returnDueDate = memoDate.plusDays(20);

            ZonedDateTime now = timeUtil.nowZoned();
            int updatedRows = diamondsAdapter.confirmDelivery(rootApptId, memoDate, returnDueDate, now);

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
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("rootApptId is required");
        }
        return trimmed;
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return timeUtil.parseDate(value)
                .orElseThrow(() -> new IllegalArgumentException("Invalid " + fieldName + ": " + value));
    }
}
