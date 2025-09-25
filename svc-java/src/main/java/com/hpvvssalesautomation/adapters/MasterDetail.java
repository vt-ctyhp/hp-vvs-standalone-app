package com.hpvvssalesautomation.adapters;

import java.time.LocalDate;

public record MasterDetail(
        String rootApptId,
        String customerName,
        String brand,
        String salesStage,
        String conversionStatus,
        String customOrderStatus,
        String inProductionStatus,
        String centerStoneOrderStatus,
        String assignedRep,
        String assistedRep,
        String soNumber,
        String nextSteps,
        LocalDate threeDDeadline,
        Integer threeDDeadlineMoves,
        LocalDate productionDeadline,
        Integer productionDeadlineMoves
) {
}
