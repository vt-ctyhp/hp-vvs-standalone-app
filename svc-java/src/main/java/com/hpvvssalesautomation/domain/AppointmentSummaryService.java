package com.hpvvssalesautomation.domain;

import com.hpvvssalesautomation.adapters.MasterRecord;
import com.hpvvssalesautomation.adapters.SheetsAdapter;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppointmentSummaryService {

    private final SheetsAdapter sheetsAdapter;
    private final TimeUtil timeUtil;

    public AppointmentSummaryService(SheetsAdapter sheetsAdapter, TimeUtil timeUtil) {
        this.sheetsAdapter = sheetsAdapter;
        this.timeUtil = timeUtil;
    }

    public List<Map<String, Object>> run(AppointmentSummaryRequest request) {
        Optional<LocalDate> dateFrom = timeUtil.parseDate(request.getDateFrom());
        Optional<LocalDate> dateTo = timeUtil.parseDate(request.getDateTo());
        List<MasterRecord> records = sheetsAdapter.fetchMasterRows(request, dateFrom, dateTo);
        return records.stream()
                .map(this::toSummaryRow)
                .map(AppointmentSummaryRow::toOrderedMap)
                .toList();
    }

    private AppointmentSummaryRow toSummaryRow(MasterRecord record) {
        AppointmentSummaryRow row = new AppointmentSummaryRow();
        row.setVisitDate(timeUtil.formatDate(record.visitDate()));
        row.setRootApptId(record.rootApptId());
        row.setCustomer(record.customerName());
        row.setPhone(record.phone());
        row.setEmail(record.email());
        row.setVisitType(record.visitType());
        row.setVisitNumber(record.visitNumber());
        row.setSoNumber(record.soNumber());
        row.setBrand(record.brand());
        row.setSalesStage(record.salesStage());
        row.setConversionStatus(record.conversionStatus());
        row.setCustomOrderStatus(record.customOrderStatus());
        row.setCenterStoneOrderStatus(record.centerStoneOrderStatus());
        row.setAssignedRep(record.assignedRep());
        row.setAssistedRep(record.assistedRep());
        return row;
    }
}
