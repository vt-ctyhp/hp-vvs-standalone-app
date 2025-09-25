import { getServiceBaseUrl } from './config.js';

function optional(value) {
  if (typeof value !== 'string') {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function toNullableNumber(value) {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function toCurrency(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toFixed(2);
}

function toIsoDateTime(value) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}

function buildLines(form, amountGross) {
  const lineAmount = toNullableNumber(form.lineAmt.value) ?? amountGross;
  if (!Number.isFinite(lineAmount)) {
    throw new Error('Line amount is required');
  }
  const quantity = toNullableNumber(form.lineQty.value) ?? 1;
  return [
    {
      desc: optional(form.lineDesc.value) ?? 'Line Item',
      qty: quantity,
      amt: lineAmount,
    },
  ];
}

function buildRecordPayload(form) {
  const anchorType = form.anchorType.value;
  const amountGross = toNullableNumber(form.amountGross.value);
  if (!Number.isFinite(amountGross) || amountGross <= 0) {
    throw new Error('Amount gross must be greater than zero');
  }
  const paymentDateTime = toIsoDateTime(form.paymentDateTime.value);
  if (!paymentDateTime) {
    throw new Error('Payment date/time is required');
  }
  const payload = {
    anchorType,
    rootApptId: optional(form.rootApptId.value),
    soNumber: optional(form.soNumber.value),
    docType: form.docType.value,
    docStatus: 'ISSUED',
    paymentDateTime,
    method: form.method.value,
    amountGross,
    reference: optional(form.reference.value),
    notes: optional(form.notes.value),
    feePercent: toNullableNumber(form.feePercent.value),
    feeAmount: toNullableNumber(form.feeAmount.value),
    lines: buildLines(form, amountGross),
  };

  if (payload.anchorType === 'APPT' && !payload.rootApptId) {
    throw new Error('Root Appt ID is required when anchor type is APPT');
  }
  if (payload.anchorType === 'SO' && !payload.soNumber) {
    throw new Error('SO Number is required when anchor type is SO');
  }

  if (!payload.lines[0].amt) {
    payload.lines[0].amt = amountGross;
  }

  // Remove null fields so the request stays minimal.
  Object.keys(payload).forEach((key) => {
    if (payload[key] === null || payload[key] === undefined) {
      delete payload[key];
    }
  });

  payload.lines = payload.lines.map((line) => {
    const clean = { ...line };
    Object.keys(clean).forEach((key) => {
      if (clean[key] === null || clean[key] === undefined) {
        delete clean[key];
      }
    });
    return clean;
  });

  return payload;
}

function renderSummary(container, data) {
  container.innerHTML = '';

  const totals = document.createElement('div');
  totals.innerHTML = `
    <p><strong>Invoices Subtotal:</strong> $${toCurrency(data.invoicesLinesSubtotal)}</p>
    <p><strong>Total Payments:</strong> $${toCurrency(data.totalPayments)}</p>
    <p><strong>Net (Invoices - Payments):</strong> $${toCurrency(data.netLinesMinusPayments)}</p>
  `;
  container.appendChild(totals);

  const methods = data.byMethod ?? {};
  const methodKeys = Object.keys(methods);
  if (methodKeys.length > 0) {
    const methodList = document.createElement('ul');
    methodList.innerHTML = methodKeys
      .map((method) => `<li>${method}: $${toCurrency(methods[method])}</li>`)
      .join('');
    container.appendChild(methodList);
  }

  const entries = Array.isArray(data.entries) ? data.entries : [];
  if (entries.length === 0) {
    const empty = document.createElement('p');
    empty.textContent = 'No ledger entries found for this anchor.';
    container.appendChild(empty);
    return;
  }

  const table = document.createElement('table');
  const header = document.createElement('tr');
  ['Doc #', 'Type', 'Role', 'Status', 'Paid At', 'Method', 'Net'].forEach((label) => {
    const th = document.createElement('th');
    th.textContent = label;
    header.appendChild(th);
  });
  table.appendChild(header);

  entries.forEach((entry) => {
    const row = document.createElement('tr');
    const cells = [
      entry.docNumber ?? '',
      entry.docType ?? '',
      entry.docRole ?? '',
      entry.docStatus ?? '',
      entry.paymentDateTime ?? '',
      entry.method ?? '',
      `$${toCurrency(entry.amountNet)}`,
    ];
    cells.forEach((value) => {
      const td = document.createElement('td');
      td.textContent = value;
      row.appendChild(td);
    });
    table.appendChild(row);
  });

  container.appendChild(table);
}

export function initPaymentsPanel() {
  const section = document.getElementById('paymentsPanel');
  const recordForm = document.getElementById('paymentsRecordForm');
  const summaryForm = document.getElementById('paymentsSummaryForm');
  if (!section || !recordForm || !summaryForm) {
    return;
  }
  section.style.display = 'block';

  const recordMessage = document.getElementById('paymentsRecordMessage');
  const summaryMessage = document.getElementById('paymentsSummaryMessage');
  const summaryResults = document.getElementById('paymentsSummaryResults');
  const baseUrl = getServiceBaseUrl();

  recordForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    recordMessage.textContent = '';
    recordMessage.classList.remove('error', 'success');

    const submitButton = recordForm.querySelector('button[type="submit"]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Recording...';
    }

    try {
      const payload = buildRecordPayload(recordForm);
      const response = await fetch(`${baseUrl}/payments/record`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `HTTP ${response.status}`);
      }
      const data = await response.json();
      recordMessage.textContent = `Saved ${data.docNumber} (${data.status}). Net $${toCurrency(data.amountNet)}.`;
      recordMessage.classList.add('success');
    } catch (error) {
      console.error(error);
      recordMessage.textContent = `Failed to record payment: ${error.message}`;
      recordMessage.classList.add('error');
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Record Payment';
      }
    }
  });

  summaryForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    summaryMessage.textContent = '';
    summaryMessage.classList.remove('error', 'success');
    summaryResults.innerHTML = '';

    const root = optional(summaryForm.summaryRoot.value);
    const so = optional(summaryForm.summarySo.value);
    if (!root && !so) {
      summaryMessage.textContent = 'Enter a Root Appt ID or SO Number to run a summary.';
      summaryMessage.classList.add('error');
      return;
    }

    const submitButton = summaryForm.querySelector('button[type="submit"]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Loading...';
    }

    try {
      const params = new URLSearchParams();
      if (root) {
        params.set('rootApptId', root);
      }
      if (so) {
        params.set('soNumber', so);
      }
      const response = await fetch(`${baseUrl}/payments/summary?${params.toString()}`);
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `HTTP ${response.status}`);
      }
      const data = await response.json();
      renderSummary(summaryResults, data);
      summaryMessage.textContent = `Loaded ${Array.isArray(data.entries) ? data.entries.length : 0} ledger entries.`;
      summaryMessage.classList.add('success');
    } catch (error) {
      console.error(error);
      summaryMessage.textContent = `Failed to load summary: ${error.message}`;
      summaryMessage.classList.add('error');
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Get Payments Summary';
      }
    }
  });
}
