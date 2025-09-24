import { getServiceBaseUrl } from './config.js';

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

const runButton = document.getElementById('run-summary');
const resultsContainer = document.getElementById('results');
const errorContainer = document.getElementById('error-message');

runButton.addEventListener('click', async () => {
  clearError();
  resultsContainer.innerHTML = '';
  try {
    const response = await fetch(`${getServiceBaseUrl()}/appointment-summary/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Service error (${response.status}): ${text}`);
    }

    const rows = await response.json();
    renderTable(rows);
  } catch (err) {
    showError(err.message || 'Unknown error');
  }
});

function renderTable(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    resultsContainer.textContent = 'No rows returned.';
    return;
  }

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

  resultsContainer.appendChild(table);
}

function showError(message) {
  errorContainer.textContent = message;
}

function clearError() {
  errorContainer.textContent = '';
}
