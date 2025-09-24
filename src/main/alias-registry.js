import { normalizeHeaderName } from './utils/headerMap.js';

const MASTER_HEADER_ALIASES = {
  VisitDate: ['Visit Date', 'VisitDate', 'Appt Date', 'Appointment Date'],
  RootApptID: ['RootApptID', 'Root Appt ID', 'ROOT', 'Root_ID'],
  Customer: ['Customer', 'Customer Name', 'Client Name'],
  Phone: ['Phone', 'Phone Number', 'Primary Phone'],
  PhoneNorm: ['PhoneNorm', 'Phone Normalized', 'Phone (Normalized)'],
  Email: ['Email', 'Customer Email'],
  EmailLower: ['EmailLower', 'Email (Lower)', 'Email Lower'],
  VisitType: ['Visit Type', 'Appt Type', 'Type'],
  VisitNumber: ['Visit #', 'Visit Number', 'Appt #'],
  SO: ['SO#', 'SO Number', 'Sales Order'],
  Brand: ['Brand'],
  SalesStage: ['Sales Stage', 'Stage'],
  ConversionStatus: ['Conversion Status', 'Conversion'],
  CustomOrderStatus: ['Custom Order Status', 'Custom Status'],
  CenterStoneOrderStatus: ['Center Stone Order Status', 'Center Stone Status'],
  AssignedRep: ['Assigned Rep', 'Primary Rep'],
  AssistedRep: ['Assisted Rep', 'Secondary Rep'],
};

const LEDGER_HEADER_ALIASES = {
  RootApptID: ['RootApptID', 'Root Appt ID', 'ROOT', 'Root_ID'],
  PaymentDateTime: ['PaymentDateTime', 'Payment DateTime', 'Payment Date', 'Paid At'],
  DocType: ['DocType', 'Document Type', 'Type'],
  AmountNet: ['AmountNet', 'Net', 'Net Amount'],
  DocStatus: ['DocStatus', 'Status'],
};

const SNAPSHOT_ALIASES = {
  SnapshotDate: ['Snapshot Date'],
  CapturedAt: ['Captured At'],
  RootApptID: ['RootApptID', 'Root Appt ID'],
  Rep: ['Rep', 'Assigned Rep'],
  Role: ['Role'],
  ScopeGroup: ['Scope Group'],
  CustomerName: ['Customer Name', 'Customer'],
  SalesStage: ['Sales Stage', 'Stage'],
  ConversionStatus: ['Conversion Status'],
  CustomOrderStatus: ['Custom Order Status'],
  UpdatedBy: ['Updated By'],
  UpdatedAt: ['Updated At'],
  DaysSinceLastUpdate: ['Days Since Last Update'],
  ClientStatusReportURL: ['Client Status Report URL'],
};

const PER_CLIENT_LOG_HEADERS = {
  LogDate: ['Log Date'],
  SalesStage: ['Sales Stage'],
  ConversionStatus: ['Conversion Status'],
  CustomOrderStatus: ['Custom Order Status'],
  CenterStoneOrderStatus: ['Center Stone Order Status'],
  NextSteps: ['Next Steps'],
  DeadlineType: ['Deadline Type'],
  DeadlineDate: ['Deadline Date'],
  MoveCount: ['Move Count'],
  AssistedRep: ['Assisted Rep'],
  UpdatedBy: ['Updated By'],
  UpdatedAt: ['Updated At'],
};

export const APPOINTMENT_SUMMARY_HEADERS = [
  'Visit Date',
  'RootApptID',
  'Customer',
  'Phone',
  'Email',
  'Visit Type',
  'Visit #',
  'SO#',
  'Brand',
  'Sales Stage',
  'Conversion Status',
  'Custom Order Status',
  'Center Stone Order Status',
  'Assigned Rep',
  'Assisted Rep',
];

function resolveHeader(headers, aliases, canonical) {
  const candidates = aliases[canonical] || [canonical];
  const normalizedHeaders = headers.map((header) => normalizeHeaderName(header));
  for (const candidate of candidates) {
    const normalized = normalizeHeaderName(candidate);
    const index = normalizedHeaders.indexOf(normalized);
    if (index !== -1) {
      return headers[index];
    }
  }
  return null;
}

export function selectFirstMatchingHeader(headers, canonical, collection = MASTER_HEADER_ALIASES) {
  return resolveHeader(headers, collection, canonical);
}

export function getAliasSet(collectionName) {
  switch (collectionName) {
    case 'master':
      return MASTER_HEADER_ALIASES;
    case 'ledger':
      return LEDGER_HEADER_ALIASES;
    case 'snapshot':
      return SNAPSHOT_ALIASES;
    case 'perClient':
      return PER_CLIENT_LOG_HEADERS;
    default:
      return {};
  }
}

export {
  MASTER_HEADER_ALIASES,
  LEDGER_HEADER_ALIASES,
  SNAPSHOT_ALIASES,
  PER_CLIENT_LOG_HEADERS,
};
