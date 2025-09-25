import { getServiceBaseUrl } from './config.js';
import { initClientStatusForm } from './clientStatus.js';
import { initDeadlinesForm } from './deadlines.js';
import { initDiamondsOrderForm } from './diamondsOrders.js';
import { initDiamondsDeliveryForm } from './diamondsDelivery.js';
import { initDiamondsDecisionForm } from './diamondsDecisions.js';
import { initPaymentsPanel } from './payments.js';
import { initReportsPanel } from './reports.js';
import { initDashboardPanel } from './dashboard.js';

const button = document.getElementById('runSummary');
const results = document.getElementById('results');
const message = document.getElementById('message');

const COLUMN_ORDER = [
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

function renderTable(rows) {
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const headerRow = document.createElement('tr');
  COLUMN_ORDER.forEach((column) => {
    const th = document.createElement('th');
    th.textContent = column;
    headerRow.appendChild(th);
  });
  thead.appendChild(headerRow);
  table.appendChild(thead);

  const tbody = document.createElement('tbody');
  rows.forEach((row) => {
    const tr = document.createElement('tr');
    COLUMN_ORDER.forEach((column) => {
      const td = document.createElement('td');
      td.textContent = row[column] ?? '';
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);

  results.innerHTML = '';
  results.appendChild(table);
}

async function runSummary() {
  const baseUrl = getServiceBaseUrl();
  message.textContent = '';
  message.classList.remove('error', 'success');
  results.innerHTML = '';
  button.disabled = true;
  button.textContent = 'Running...';

  try {
    const response = await fetch(`${baseUrl}/appointment-summary/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP ${response.status} ${response.statusText}: ${errorText}`);
    }

    const data = await response.json();
    if (!Array.isArray(data)) {
      throw new Error('Unexpected response shape');
    }
    renderTable(data);
    message.classList.add('success');
    message.textContent = `Loaded ${data.length} appointment rows.`;
  } catch (error) {
    console.error(error);
    message.textContent = `Failed to run summary: ${error.message}`;
    message.classList.add('error');
  } finally {
    button.disabled = false;
    button.textContent = 'Run Appointment Summary';
  }
}

button.addEventListener('click', runSummary);

initClientStatusForm();
initDeadlinesForm();

const featureFlags = globalThis.appConfig?.featureFlags ?? {};
const paymentsEnabled = Boolean(featureFlags.payments ?? globalThis.appConfig?.paymentsEnabled);
const diamondsEnabled = Boolean(featureFlags.diamonds);
const reportsEnabled = Boolean(featureFlags.reports ?? globalThis.appConfig?.reportsEnabled);

if (paymentsEnabled) {
  initPaymentsPanel();
} else {
  document.querySelectorAll('[data-feature="payments"]').forEach((section) => {
    section.classList.add('hidden-feature');
  });
}

if (diamondsEnabled) {
  initDiamondsOrderForm();
  initDiamondsDeliveryForm();
  initDiamondsDecisionForm();
} else {
  document.querySelectorAll('[data-feature="diamonds"]').forEach((section) => {
    section.classList.add('hidden-feature');
  });
}

if (reportsEnabled) {
  initReportsPanel();
  initDashboardPanel();
} else {
  document.querySelectorAll('[data-feature="reports"]').forEach((section) => {
    section.classList.add('hidden-feature');
  });
}
