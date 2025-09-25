package com.hpvvssalesautomation.domain;

import com.hpvvssalesautomation.adapters.MasterDetail;
import com.hpvvssalesautomation.adapters.PerClientReportAdapter;
import com.hpvvssalesautomation.adapters.SheetsAdapter;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeadlinesService {

    private final SheetsAdapter sheetsAdapter;
    private final PerClientReportAdapter perClientReportAdapter;
    private final TimeUtil timeUtil;

    public DeadlinesService(SheetsAdapter sheetsAdapter,
                            PerClientReportAdapter perClientReportAdapter,
                            TimeUtil timeUtil) {
        this.sheetsAdapter = sheetsAdapter;
        this.perClientReportAdapter = perClientReportAdapter;
        this.timeUtil = timeUtil;
    }

    public DeadlineRecordResponse record(DeadlineRecordRequest request) {
        MasterDetail masterDetail = sheetsAdapter.findMasterDetail(request.getRootApptId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown RootApptID: " + request.getRootApptId()));

        DeadlineType type = DeadlineType.fromString(request.getDeadlineType());
        LocalDate deadlineDate = timeUtil.parseDate(request.getDeadlineDate())
                .orElseThrow(() -> new IllegalArgumentException("Invalid deadlineDate: " + request.getDeadlineDate()));

        String assistedRep = normalize(request.getAssistedRep());
        String movedBy = request.getMovedBy().trim();

        LocalDate logDate = timeUtil.today();
        ZonedDateTime updatedAt = timeUtil.nowZoned();

        int moveCount = sheetsAdapter.applyDeadline(request.getRootApptId(), type, deadlineDate, updatedAt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deadlineType", type.value());
        payload.put("deadlineDate", deadlineDate.toString());
        payload.put("assistedRep", assistedRep);
        payload.put("movedBy", movedBy);
        payload.put("moveCount", moveCount);

        sheetsAdapter.appendDeadlineLog(
                request.getRootApptId(),
                logDate,
                type,
                deadlineDate,
                moveCount,
                assistedRep,
                movedBy,
                updatedAt,
                payload
        );

        perClientReportAdapter.ensureReportExists(masterDetail);
        perClientReportAdapter.appendDeadlineEntry(
                request.getRootApptId(),
                logDate,
                type.value(),
                deadlineDate,
                moveCount,
                assistedRep,
                movedBy,
                updatedAt
        );
        perClientReportAdapter.updateDeadlineSnapshot(
                request.getRootApptId(),
                type.value(),
                deadlineDate,
                moveCount,
                movedBy,
                assistedRep,
                updatedAt
        );

        return new DeadlineRecordResponse(
                request.getRootApptId(),
                type.value(),
                deadlineDate,
                moveCount,
                updatedAt.toOffsetDateTime()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
