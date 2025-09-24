const button = document.getElementById('runSummary');
const results = document.getElementById('results');

function renderTable(summary) {
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const headerRow = document.createElement('tr');
  summary.headers.forEach((header) => {
    const th = document.createElement('th');
    th.textContent = header;
    headerRow.appendChild(th);
  });
  thead.appendChild(headerRow);
  table.appendChild(thead);

  const tbody = document.createElement('tbody');
  summary.rows.forEach((row) => {
    const tr = document.createElement('tr');
    row.forEach((cell) => {
      const td = document.createElement('td');
      td.textContent = cell ?? '';
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  results.innerHTML = '';
  results.appendChild(table);
}

button.addEventListener('click', async () => {
  button.disabled = true;
  button.textContent = 'Running...';
  try {
    const summary = await window.appAPI.runAppointmentSummary();
    renderTable(summary);
  } catch (error) {
    results.textContent = `Failed to run summary: ${error.message}`;
  } finally {
    button.disabled = false;
    button.textContent = 'Run Appointment Summary';
  }
});
