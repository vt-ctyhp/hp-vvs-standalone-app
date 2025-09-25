package com.hpvvssalesautomation.adapters;

import java.time.LocalDate;

public record MasterRecord(
        LocalDate visitDate,
        String rootApptId,
        String customerName,
        String phone,
        String email,
        String visitType,
        Integer visitNumber,
        String soNumber,
        String brand,
        String salesStage,
        String conversionStatus,
        String customOrderStatus,
        String centerStoneOrderStatus,
        String assignedRep,
        String assistedRep
) {}
