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
public class ClientStatusService {

    private final SheetsAdapter sheetsAdapter;
    private final PerClientReportAdapter perClientReportAdapter;
    private final TimeUtil timeUtil;

    public ClientStatusService(SheetsAdapter sheetsAdapter,
                               PerClientReportAdapter perClientReportAdapter,
                               TimeUtil timeUtil) {
        this.sheetsAdapter = sheetsAdapter;
        this.perClientReportAdapter = perClientReportAdapter;
        this.timeUtil = timeUtil;
    }

    public ClientStatusResponse submit(ClientStatusSubmitRequest request) {
        MasterDetail masterDetail = sheetsAdapter.findMasterDetail(request.getRootApptId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown RootApptID: " + request.getRootApptId()));

        String salesStage = request.getSalesStage().trim();
        String conversionStatus = request.getConversionStatus().trim();
        String customOrderStatus = normalize(request.getCustomOrderStatus());
        String inProductionStatus = normalize(request.getInProductionStatus());
        String centerStoneOrderStatus = normalize(request.getCenterStoneOrderStatus());
        String nextSteps = normalize(request.getNextSteps());
        String assistedRep = normalize(request.getAssistedRep());
        String updatedBy = request.getUpdatedBy().trim();

        perClientReportAdapter.ensureReportExists(masterDetail);

        boolean duplicate = sheetsAdapter.findLatestStatusLog(request.getRootApptId())
                .map(latest -> matches(latest, salesStage, conversionStatus, customOrderStatus, inProductionStatus, centerStoneOrderStatus, nextSteps, assistedRep))
                .orElse(false);

        if (duplicate) {
            OffsetDateTime snapshotUpdatedAt = perClientReportAdapter.findSnapshotUpdatedAt(request.getRootApptId())
                    .orElseGet(() -> timeUtil.nowZoned().toOffsetDateTime());
            return new ClientStatusResponse(request.getRootApptId(), snapshotUpdatedAt, "UNCHANGED");
        }

        LocalDate logDate = timeUtil.today();
        ZonedDateTime updatedAt = timeUtil.nowZoned();

        Map<String, Object> payload = new HashMap<>();
        payload.put("salesStage", salesStage);
        payload.put("conversionStatus", conversionStatus);
        payload.put("customOrderStatus", customOrderStatus);
        payload.put("inProductionStatus", inProductionStatus);
        payload.put("centerStoneOrderStatus", centerStoneOrderStatus);
        payload.put("nextSteps", nextSteps);
        payload.put("assistedRep", assistedRep);
        payload.put("updatedBy", updatedBy);

        sheetsAdapter.appendClientStatusLog(
                request.getRootApptId(),
                logDate,
                salesStage,
                conversionStatus,
                customOrderStatus,
                inProductionStatus,
                centerStoneOrderStatus,
                nextSteps,
                assistedRep,
                updatedBy,
                updatedAt,
                payload
        );

        sheetsAdapter.updateMasterFromStatus(
                request.getRootApptId(),
                salesStage,
                conversionStatus,
                customOrderStatus,
                inProductionStatus,
                centerStoneOrderStatus,
                nextSteps,
                assistedRep,
                updatedAt
        );

        perClientReportAdapter.appendStatusEntry(
                request.getRootApptId(),
                logDate,
                salesStage,
                conversionStatus,
                customOrderStatus,
                centerStoneOrderStatus,
                nextSteps,
                assistedRep,
                updatedBy,
                updatedAt
        );
        perClientReportAdapter.updateStatusSnapshot(
                masterDetail,
                salesStage,
                conversionStatus,
                customOrderStatus,
                inProductionStatus,
                centerStoneOrderStatus,
                nextSteps,
                assistedRep,
                updatedBy,
                updatedAt
        );

        return new ClientStatusResponse(
                request.getRootApptId(),
                updatedAt.toOffsetDateTime(),
                "OK"
        );
    }

    private boolean matches(SheetsAdapter.StatusLogView latest,
                             String salesStage,
                             String conversionStatus,
                             String customOrderStatus,
                             String inProductionStatus,
                             String centerStoneOrderStatus,
                             String nextSteps,
                             String assistedRep) {
        return equals(latest.salesStage(), salesStage)
                && equals(latest.conversionStatus(), conversionStatus)
                && equals(latest.customOrderStatus(), customOrderStatus)
                && equals(latest.inProductionStatus(), inProductionStatus)
                && equals(latest.centerStoneOrderStatus(), centerStoneOrderStatus)
                && equals(latest.nextSteps(), nextSteps)
                && equals(latest.assistedRep(), assistedRep);
    }

    private boolean equals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
