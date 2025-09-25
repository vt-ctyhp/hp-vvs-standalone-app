package com.hpvvssalesautomation.domain.diamonds;

public final class DiamondsSummaryResolver {

    private DiamondsSummaryResolver() {
    }

    public static String resolve(DiamondsCounts counts) {
        if (counts.totalCount() == 0) {
            return "No Stones";
        }
        if (counts.proposingCount() > 0) {
            return "Proposing";
        }
        if (counts.notApprovedCount() == counts.totalCount()) {
            return "Not Approved";
        }
        if (counts.onTheWayCount() > 0) {
            return "On the Way";
        }
        if (counts.deliveredCount() > 0 || counts.inStockCount() > 0) {
            if (counts.keepCount() == counts.totalCount() && counts.totalCount() > 0) {
                return "Keep";
            }
            if (counts.returnCount() == counts.totalCount() && counts.totalCount() > 0) {
                return "Return Pending";
            }
            if (counts.replaceCount() == counts.totalCount() && counts.totalCount() > 0) {
                return "Replacement Needed";
            }
            if (counts.returnCount() > 0) {
                return "Return Pending";
            }
            if (counts.replaceCount() > 0) {
                return "Replacement Needed";
            }
            if (counts.inStockCount() == counts.totalCount()) {
                return "Delivered";
            }
            return "Delivered";
        }
        if (counts.keepCount() > 0 && counts.keepCount() == counts.totalCount()) {
            return "Keep";
        }
        if (counts.returnCount() > 0) {
            return "Return Pending";
        }
        if (counts.replaceCount() > 0) {
            return "Replacement Needed";
        }
        if (counts.inStockCount() == counts.totalCount() && counts.totalCount() > 0) {
            return "In Stock";
        }
        return "In Progress";
    }
}
