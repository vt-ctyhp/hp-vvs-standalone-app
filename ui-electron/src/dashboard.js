import { getServiceBaseUrl } from './config.js';

export function initDashboardPanel() {
  const form = document.getElementById('dashboardKpiForm');
  const messageEl = document.getElementById('dashboardMessage');
  const resultsEl = document.getElementById('dashboardResults');
  const panel = document.getElementById('dashboardPanel');

  if (panel) {
    panel.style.display = 'block';
  }

  if (!form || !messageEl || !resultsEl) {
    return;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const baseUrl = getServiceBaseUrl();
    const dateFrom = form.dateFrom.value;
    const dateTo = form.dateTo.value;

    messageEl.textContent = '';
    messageEl.classList.remove('error', 'success');
    resultsEl.innerHTML = '';

    try {
      const url = new URL(`${baseUrl}/dashboard/kpis`);
      if (dateFrom) {
        url.searchParams.set('dateFrom', dateFrom);
      }
      if (dateTo) {
        url.searchParams.set('dateTo', dateTo);
      }

      const response = await fetch(url.toString());
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
      }
      const data = await response.json();
      const entries = Object.entries(data ?? {});
      if (entries.length === 0) {
        resultsEl.textContent = 'No KPI data available for the selected window.';
      } else {
        const list = document.createElement('ul');
        entries.forEach(([key, value]) => {
          const item = document.createElement('li');
          item.textContent = `${key}: ${value}`;
          list.appendChild(item);
        });
        resultsEl.appendChild(list);
      }
      messageEl.classList.add('success');
      messageEl.textContent = 'Dashboard KPIs loaded.';
    } catch (error) {
      console.error(error);
      messageEl.classList.add('error');
      messageEl.textContent = `Failed to load KPIs: ${error.message}`;
    }
  });
}
