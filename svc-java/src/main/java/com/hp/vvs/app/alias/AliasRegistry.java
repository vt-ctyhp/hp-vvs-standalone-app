package com.hp.vvs.app.alias;

import com.hp.vvs.app.util.HeaderMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AliasRegistry {

    private static final Map<String, List<String>> MASTER_ALIASES = Map.ofEntries(
            Map.entry("Visit Date", List.of("Visit Date", "Appt Date", "VisitDate")),
            Map.entry("RootApptID", List.of("RootApptID", "Root Appt ID", "ROOT", "Root_ID")),
            Map.entry("Customer", List.of("Customer", "Client", "Customer Name")),
            Map.entry("Phone", List.of("Phone", "PhoneNorm", "Primary Phone")),
            Map.entry("Email", List.of("Email", "EmailLower", "Primary Email")),
            Map.entry("Visit Type", List.of("Visit Type", "Type", "Appt Type")),
            Map.entry("Visit #", List.of("Visit #", "Visit Number", "VisitNum")),
            Map.entry("SO#", List.of("SO#", "SO Number", "Sales Order")),
            Map.entry("Brand", List.of("Brand", "Brand Name")),
            Map.entry("Sales Stage", List.of("Sales Stage", "Stage", "SalesStage")),
            Map.entry("Conversion Status", List.of("Conversion Status", "Conversion", "Conv Status")),
            Map.entry("Custom Order Status", List.of("Custom Order Status", "CO Status", "CustomOrderStatus")),
            Map.entry("Center Stone Order Status", List.of("Center Stone Order Status", "CS Order Status", "CenterStoneStatus")),
            Map.entry("Assigned Rep", List.of("Assigned Rep", "Assigned Sales Rep", "AssignedRep")),
            Map.entry("Assisted Rep", List.of("Assisted Rep", "Assisted Sales Rep", "AssistedRep"))
    );

    private static final Map<String, List<String>> LEDGER_ALIASES = Map.ofEntries(
            Map.entry("PaymentDateTime", List.of("PaymentDateTime", "Payment DateTime", "Payment Date", "Paid At")),
            Map.entry("AmountNet", List.of("AmountNet", "Net", "Net Amount")),
            Map.entry("DocType", List.of("DocType", "Document Type", "Type")),
            Map.entry("DocStatus", List.of("DocStatus", "Status", "Document Status")),
            Map.entry("RootApptID", MASTER_ALIASES.get("RootApptID")),
            Map.entry("SO#", MASTER_ALIASES.get("SO#"))
    );

    private static final Map<String, List<String>> CLIENT_STATUS_ALIASES = Map.ofEntries(
            Map.entry("RootApptID", MASTER_ALIASES.get("RootApptID")),
            Map.entry("Log Date", List.of("Log Date", "Status Date", "LogDate")),
            Map.entry("Status", List.of("Status", "Client Status", "Current Status")),
            Map.entry("Updated At", List.of("Updated At", "Last Updated", "UpdatedAt"))
    );

    private AliasRegistry() {
    }

    public static List<String> masterAliases(String canonical) {
        return MASTER_ALIASES.getOrDefault(canonical, List.of(canonical));
    }

    public static List<String> ledgerAliases(String canonical) {
        return LEDGER_ALIASES.getOrDefault(canonical, List.of(canonical));
    }

    public static List<String> clientStatusAliases(String canonical) {
        return CLIENT_STATUS_ALIASES.getOrDefault(canonical, List.of(canonical));
    }

    public static Optional<String> resolve(HeaderMap headerMap, List<String> aliases) {
        for (String alias : aliases) {
            Optional<String> header = headerMap.findHeader(alias);
            if (header.isPresent()) {
                return header;
            }
        }
        return Optional.empty();
    }

    public static Optional<String> resolveMaster(HeaderMap headerMap, String canonical) {
        return resolve(headerMap, masterAliases(canonical));
    }

    public static Optional<String> resolveLedger(HeaderMap headerMap, String canonical) {
        return resolve(headerMap, ledgerAliases(canonical));
    }

    public static Optional<String> resolveClientStatus(HeaderMap headerMap, String canonical) {
        return resolve(headerMap, clientStatusAliases(canonical));
    }

    public static Map<String, List<String>> masterAliasMap() {
        return Collections.unmodifiableMap(MASTER_ALIASES);
    }

    public static Map<String, List<String>> ledgerAliasMap() {
        return Collections.unmodifiableMap(LEDGER_ALIASES);
    }

    public static Map<String, List<String>> clientStatusAliasMap() {
        return Collections.unmodifiableMap(CLIENT_STATUS_ALIASES);
    }
}
