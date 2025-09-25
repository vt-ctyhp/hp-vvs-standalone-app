# Phase 2 UI Checklist

> Manual verification of the HTTP-only renderer flows.

1. **Launch**
   - `SERVICE_BASE_URL=http://localhost:8080 npm run dev`
   - Confirm the landing page renders Appointment Summary, Client Status Submit, and Record Deadline sections.

2. **Client Status Submit**
   - Enter a known `Root Appt ID` from the seeded fixtures (e.g., `HP-1001`).
   - Fill `Sales Stage`, `Conversion Status`, optional statuses, and `Updated By`.
   - Click **Submit Client Status**.
   - Expected: success toast/message appears, no console errors.
   - Optional: query the database (`SELECT * FROM client_status_log WHERE root_appt_id='HP-1001' ORDER BY updated_at DESC LIMIT 1`) to confirm the row.

3. **Record Deadline**
   - Use the same appointment, choose `Deadline Type = 3D`, set a future date, and enter `Moved By`.
   - Click **Record Deadline**.
   - Expected: success message; Master `three_d_deadline_moves` increments by 1 and per-client log gains a 3D entry.

4. **Repeat Submission**
   - Re-submit the identical Client Status payload.
   - Expected: the UI reports success, and the database shows no additional status log row (idempotent behavior).

5. **Error Handling**
   - Temporarily stop the backend or use invalid data to confirm error messages appear and the UI recovers when resubmitted correctly.

6. **Summary View**
   - Run Appointment Summary and confirm the table updates with current data.

Document any discrepancies or regressions before sign-off.
