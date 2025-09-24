package com.hp.vvs.app.domain;

import com.hp.vvs.app.adapters.SheetsAdapter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AppointmentSummaryService {

    public static final List<String> COLUMN_ORDER = List.of(
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
    );

    private final SheetsAdapter sheetsAdapter;

    public AppointmentSummaryService(SheetsAdapter sheetsAdapter) {
        this.sheetsAdapter = sheetsAdapter;
    }

    public List<Map<String, String>> run(Optional<String> brand,
                                         Optional<String> rep,
                                         Optional<LocalDate> dateFrom,
                                         Optional<LocalDate> dateTo) {
        AppointmentSummaryRequest request = new AppointmentSummaryRequest(brand, rep, dateFrom, dateTo);
        return sheetsAdapter.fetchAppointmentSummary(request).stream()
                .map(row -> row.ordered(COLUMN_ORDER))
                .collect(Collectors.toList());
    }
}
