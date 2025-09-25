package com.hpvvssalesautomation.alias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AliasRegistry {

    private final Map<String, List<String>> masterAliases;
    private final Map<String, List<String>> ledgerAliases;
    private final Map<String, List<String>> perClientAliases;
    private final Map<String, List<String>> dashboardAliases;
    private final Map<String, List<String>> diamondsOrderAliases;
    private final Map<String, List<String>> paymentsLedgerAliases;

    public AliasRegistry() {
        this.masterAliases = buildMasterAliases();
        this.ledgerAliases = buildLedgerAliases();
        this.perClientAliases = buildPerClientAliases();
        this.dashboardAliases = buildDashboardAliases();
        this.diamondsOrderAliases = buildDiamondsOrderAliases();
        this.paymentsLedgerAliases = buildPaymentsLedgerAliases();
    }

    private Map<String, List<String>> buildMasterAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Visit Date", List.of("Visit Date", "Appt Date", "Appointment Date", "Visit_Date"));
        map.put("RootApptID", List.of("RootApptID", "Root Appt ID", "Root ID", "ROOT", "Root", "Root_ID", "RootID"));
        map.put("Customer", List.of("Customer", "Customer Name", "Client", "Client Name", "Name"));
        map.put("Phone", List.of("Phone", "Primary Phone", "Phone Number", "Contact Phone", "PhoneNorm"));
        map.put("Phone (Normalized)", List.of("Phone (Normalized)", "PhoneNorm", "Phone Normalized"));
        map.put("Email", List.of("Email", "Primary Email", "Email Address", "Contact Email", "EmailLower"));
        map.put("Email Lower", List.of("Email Lower", "EmailLower"));
        map.put("Visit Type", List.of("Visit Type", "Appt Type", "Appointment Type", "Type", "VisitType"));
        map.put("Visit #", List.of("Visit #", "Visit Number", "# Visit"));
        map.put("SO#", List.of("SO#", "SO Number", "SO", "Sales Order #", "Sales Order"));
        map.put("Brand", List.of("Brand", "Store", "Division"));
        map.put("Sales Stage", List.of("Sales Stage", "Stage", "SalesStage"));
        map.put("Conversion Status", List.of("Conversion Status", "Status", "Conversion", "ConversionStatus"));
        map.put("Custom Order Status", List.of("Custom Order Status", "Custom Status", "COS", "CustomOrderStatus"));
        map.put("Center Stone Order Status", List.of("Center Stone Order Status", "Center Stone Status", "Center Stone", "CSOS", "CenterStoneOrderStatus"));
        map.put("Assigned Rep", List.of("Assigned Rep", "Owner", "Primary Rep", "AssignedRep", "Rep", "Sales Rep"));
        map.put("Assisted Rep", List.of("Assisted Rep", "Assistant", "Secondary Rep", "AssistedRep", "Assistant Rep"));
        map.put("In Production Status", List.of("In Production Status", "IPS", "InProductionStatus"));
        map.put("Next Steps", List.of("Next Steps", "NextSteps", "Action Items"));
        return map;
    }

    private Map<String, List<String>> buildLedgerAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("RootApptID", List.of("RootApptID", "Root Appt ID", "Root ID", "ROOT", "Root", "Root_ID", "RootID"));
        map.put("PaymentDateTime", List.of("PaymentDateTime", "Payment DateTime", "Payment Date", "Paid At", "Date"));
        map.put("DocType", List.of("DocType", "Document Type", "Type"));
        map.put("AmountNet", List.of("AmountNet", "Net", "Net Amount", "Amount", "Amount (Net)"));
        map.put("DocStatus", List.of("DocStatus", "Status", "State"));
        map.put("SO#", List.of("SO#", "SO Number", "SO", "Sales Order #", "Sales Order"));
        map.put("Method", List.of("Method", "Payment Method"));
        map.put("Reference", List.of("Reference", "Ref #", "Transaction ID"));
        map.put("AmountGross", List.of("AmountGross", "Gross", "Amount"));
        map.put("FeePercent", List.of("FeePercent", "Fee %", "Processing %"));
        map.put("FeeAmount", List.of("FeeAmount", "Processing Fee", "Fees"));
        map.put("Submitted By", List.of("Submitted By", "Entered By"));
        map.put("Submitted Date/Time", List.of("Submitted Date/Time", "Submitted At", "Entry Time"));
        return map;
    }

    private Map<String, List<String>> buildPerClientAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Log Date", List.of("Log Date", "Date", "Status Date"));
        map.put("Log Type", List.of("Log Type", "Type"));
        map.put("Notes", List.of("Notes", "Detail"));
        map.put("Sales Stage", List.of("Sales Stage", "Stage"));
        map.put("Conversion Status", List.of("Conversion Status", "Conversion"));
        map.put("Custom Order Status", List.of("Custom Order Status", "Custom Order", "COS"));
        map.put("Center Stone Order Status", List.of("Center Stone Order Status", "Center Stone", "CSOS"));
        map.put("Next Steps", List.of("Next Steps", "NextSteps", "Action Items"));
        map.put("Deadline Type", List.of("Deadline Type", "Type", "Deadline"));
        map.put("Deadline Date", List.of("Deadline Date", "Due Date", "Target Date"));
        map.put("Move Count", List.of("Move Count", "Moves", "# Moved"));
        map.put("Assisted Rep", List.of("Assisted Rep", "Assisted", "Assistant Rep"));
        map.put("Updated By", List.of("Updated By", "Owner", "Updater", "UpdatedBy"));
        map.put("Updated At", List.of("Updated At", "UpdatedAt", "Last Updated"));
        return map;
    }

    private Map<String, List<String>> buildDashboardAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Order Total", List.of("Order Total", "OrderTotal", "Total"));
        map.put("Paid-to-Date", List.of("Paid-to-Date", "Paid To Date", "PaidToDate", "PTD"));
        map.put("Remaining Balance", List.of("Remaining Balance", "Balance", "Outstanding", "Remaining"));
        map.put("Production Deadline", List.of("Production Deadline", "Prod Deadline", "Production Due"));
        map.put("3D Deadline", List.of("3D Deadline", "3D Due", "Design Due"));
        return map;
    }

    private Map<String, List<String>> buildDiamondsOrderAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("RootApptID", List.of("RootApptID", "Root Appt ID", "Root ID", "ROOT", "Root", "Root_ID", "RootID"));
        map.put("Stone Reference", List.of("Stone Reference", "StoneRef", "Stone ID", "StoneId", "Reference"));
        map.put("Stone Type", List.of("Stone Type", "Type", "Category", "StoneType"));
        map.put("Stone Status", List.of("Stone Status", "Status", "StoneStatus"));
        map.put("Order Status", List.of("Order Status", "OrderStatus", "Status (Order)", "Order"));
        map.put("Ordered By", List.of("Ordered By", "OrderedBy", "Buyer", "Ordered_By"));
        map.put("Ordered Date", List.of("Ordered Date", "Order Date", "OrderedOn", "Ordered_On"));
        map.put("Memo/Invoice Date", List.of("Memo/Invoice Date", "Memo Date", "Invoice Date", "MemoDate"));
        map.put("Return Due Date", List.of("Return Due Date", "Return Date", "Due Back", "ReturnDue"));
        map.put("Decided By", List.of("Decided By", "Decision By", "Decider", "DecidedBy"));
        map.put("Decided Date", List.of("Decided Date", "Decision Date", "DecidedOn", "Decided_On"));
        return map;
    }

    private Map<String, List<String>> buildPaymentsLedgerAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("DocNumber", List.of("DocNumber", "Doc #", "Document Number", "PAYMENT_ID", "Payment ID", "Doc"));
        map.put("DocRole", List.of("DocRole", "Document Role", "Role"));
        map.put("AnchorType", List.of("AnchorType", "Anchor", "Anchor Type", "Anchor_Type"));
        map.put("RootApptID", List.of("RootApptID", "Root Appt ID", "Root ID", "ROOT", "Root", "Root_ID", "RootID"));
        map.put("SO#", List.of("SO#", "SO Number", "SO", "Sales Order #", "Sales Order"));
        map.put("BasketID", List.of("BasketID", "Basket ID", "Basket"));
        map.put("DocType", List.of("DocType", "Document Type", "Type"));
        map.put("DocStatus", List.of("DocStatus", "Status", "State"));
        map.put("PaymentDateTime", List.of("PaymentDateTime", "Payment DateTime", "Payment Date", "Paid At", "PaidAt", "Date"));
        map.put("Method", List.of("Method", "Payment Method", "Tender"));
        map.put("Reference", List.of("Reference", "Ref #", "Transaction ID", "Txn Ref"));
        map.put("Notes", List.of("Notes", "Memo", "Internal Notes"));
        map.put("AmountGross", List.of("AmountGross", "Gross", "Gross Amount", "Amount"));
        map.put("FeePercent", List.of("FeePercent", "Fee %", "Processing %", "FeePercent%"));
        map.put("FeeAmount", List.of("FeeAmount", "Processing Fee", "Fees"));
        map.put("Subtotal", List.of("Subtotal", "Lines Subtotal", "Line Subtotal"));
        map.put("AmountNet", List.of("AmountNet", "Net", "Net Amount", "Amount (Net)", "Net$"));
        map.put("AllocatedToSO", List.of("AllocatedToSO", "Allocated", "Allocated Amount"));
        map.put("LinesJSON", List.of("LinesJSON", "Lines", "Line Items", "Entries", "Items"));
        map.put("Order Total_SO", List.of("Order Total_SO", "Order Total", "SO Order Total"));
        map.put("Paid-To-Date_SO", List.of("Paid-To-Date_SO", "Paid To Date", "Paid-to-Date", "PTD"));
        map.put("Balance_SO", List.of("Balance_SO", "Remaining Balance", "Outstanding"));
        map.put("Submitted By", List.of("Submitted By", "Entered By", "SubmittedBy"));
        map.put("Submitted Date/Time", List.of("Submitted Date/Time", "Submitted At", "Entry Time", "SubmittedAt"));
        return map;
    }

    public Map<String, List<String>> masterAppointmentAliases() {
        return masterAliases;
    }

    public Map<String, List<String>> ledgerAliases() {
        return ledgerAliases;
    }

    public Map<String, List<String>> perClientAliases() {
        return perClientAliases;
    }

    public Map<String, List<String>> dashboardAliases() {
        return dashboardAliases;
    }

    public Map<String, List<String>> diamondsOrderAliases() {
        return diamondsOrderAliases;
    }

    public Map<String, List<String>> paymentsLedgerAliases() {
        return paymentsLedgerAliases;
    }

    public List<String> canonicalMasterColumns() {
        return new ArrayList<>(masterAliases.keySet());
    }

    public List<String> canonicalLedgerColumns() {
        return new ArrayList<>(ledgerAliases.keySet());
    }

    public List<String> canonicalPerClientColumns() {
        return new ArrayList<>(perClientAliases.keySet());
    }

    public List<String> canonicalDashboardColumns() {
        return new ArrayList<>(dashboardAliases.keySet());
    }

    public List<String> canonicalDiamondsOrderColumns() {
        return new ArrayList<>(diamondsOrderAliases.keySet());
    }

    public List<String> canonicalPaymentsLedgerColumns() {
        return new ArrayList<>(paymentsLedgerAliases.keySet());
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.US);
    }
}
