import { getSheetsService } from '../adapters/SheetsService.js';
import { APPOINTMENT_SUMMARY_HEADERS } from '../main/alias-registry.js';
import { formatDate } from '../main/utils/time.js';

export class AppointmentSummary {
  constructor() {
    this.sheetsService = getSheetsService();
  }

  run(filters = {}) {
    const rows = this.sheetsService.getMasterRows({
      brand: filters.brand,
      rep: filters.rep,
      startDate: filters.startDate,
      endDate: filters.endDate,
    });
    const data = rows.map((row) => [
      formatDate(row.VisitDate) || row.VisitDate,
      row.RootApptID,
      row.Customer,
      row.Phone,
      row.Email,
      row.VisitType,
      row.VisitNumber,
      row.SO,
      row.Brand,
      row.SalesStage,
      row.ConversionStatus,
      row.CustomOrderStatus,
      row.CenterStoneOrderStatus,
      row.AssignedRep,
      row.AssistedRep,
    ]);
    return {
      headers: APPOINTMENT_SUMMARY_HEADERS,
      rows: data,
    };
  }
}

export default AppointmentSummary;
