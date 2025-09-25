import { getServiceBaseUrl } from './config.js';

function renderRows(container, rows) {
  container.innerHTML = '';
  if (!Array.isArray(rows) || rows.length === 0) {
    container.textContent = 'No rows returned for the selected filters.';
    return;
  }

  const columns = Array.from(new Set(rows.flatMap((row) => Object.keys(row))));
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const headRow = document.createElement('tr');
  columns.forEach((column) => {
    const th = document.createElement('th');
    th.textContent = column;
    headRow.appendChild(th);
  });
  thead.appendChild(headRow);
  table.appendChild(thead);

  const tbody = document.createElement('tbody');
  rows.forEach((row) => {
    const tr = document.createElement('tr');
    columns.forEach((column) => {
      const td = document.createElement('td');
      const value = row[column];
      td.textContent = value == null ? '' : value;
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  container.appendChild(table);
}

async function loadReport(endpoint, filtersValue, resultsContainer, messageEl) {
  const baseUrl = getServiceBaseUrl();
  messageEl.textContent = '';
  messageEl.classList.remove('error', 'success');
  resultsContainer.innerHTML = '';
  try {
    const url = new URL(`${baseUrl}${endpoint}`);
    if (filtersValue) {
      url.searchParams.set('filters', filtersValue);
    }
    const response = await fetch(url.toString());
    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `HTTP ${response.status}`);
    }
    const data = await response.json();
    const rows = data?.rows ?? [];
    renderRows(resultsContainer, rows);
    messageEl.classList.add('success');
    messageEl.textContent = `Loaded ${rows.length} rows.`;
  } catch (error) {
    console.error(error);
    messageEl.classList.add('error');
    messageEl.textContent = `Failed to load report: ${error.message}`;
  }
}

export function initReportsPanel() {
  const panel = document.getElementById('reportsPanel');
  const statusForm = document.getElementById('reportsStatusForm');
  const statusResults = document.getElementById('reportsStatusResults');
  const statusMessage = document.getElementById('reportsStatusMessage');

  if (panel) {
    panel.style.display = 'block';
  }

  if (statusForm && statusResults && statusMessage) {
    statusForm.addEventListener('submit', (event) => {
      event.preventDefault();
      const filters = statusForm.filters.value.trim();
      loadReport('/reports/by-status', filters, statusResults, statusMessage);
    });
  }

  const repForm = document.getElementById('reportsRepForm');
  const repResults = document.getElementById('reportsRepResults');
  const repMessage = document.getElementById('reportsRepMessage');

  if (repForm && repResults && repMessage) {
    repForm.addEventListener('submit', (event) => {
      event.preventDefault();
      const filters = repForm.filters.value.trim();
      loadReport('/reports/by-rep', filters, repResults, repMessage);
    });
  }
}
