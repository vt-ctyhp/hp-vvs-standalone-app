import { getServiceBaseUrl } from './config.js';

function parseRoots(input) {
  return input
    .split(/\r?\n|,/)
    .map((value) => value.trim())
    .filter((value) => value.length > 0);
}

function setBusy(submitButton, busy, busyText, idleText) {
  if (!submitButton) {
    return;
  }
  submitButton.disabled = busy;
  submitButton.textContent = busy ? busyText : idleText;
}

function renderSuccess(messageEl, text) {
  messageEl.textContent = text;
  messageEl.classList.remove('error');
  messageEl.classList.add('success');
}

function renderError(messageEl, text) {
  messageEl.textContent = text;
  messageEl.classList.remove('success');
  messageEl.classList.add('error');
}

export function initDiamondsOrderForm() {
  const form = document.getElementById('diamondsOrderForm');
  if (!form) {
    return;
  }
  const messageEl = document.getElementById('diamondsOrderMessage');
  const submitButton = form.querySelector('button[type="submit"]');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const baseUrl = getServiceBaseUrl();
    const roots = parseRoots(form.rootApptIds.value || '');
    if (roots.length === 0) {
      renderError(messageEl, 'Enter at least one Root Appt ID.');
      return;
    }

    const decision = form.decision.value;
    const orderedBy = form.orderedBy.value.trim();
    const orderedDate = form.orderedDate.value;
    const applyDefaults = form.applyDefaults.checked;

    const items = roots.map((root) => ({
      rootApptId: root,
      decision,
      orderedBy: applyDefaults ? null : (orderedBy || null),
      orderedDate: applyDefaults ? null : (orderedDate || null),
    }));

    const payload = {
      items,
      defaultOrderedBy: orderedBy || null,
      defaultOrderedDate: orderedDate || null,
      applyDefaultsToAll: applyDefaults,
    };

    messageEl.textContent = '';
    messageEl.classList.remove('success', 'error');
    setBusy(submitButton, true, 'Applying...', 'Apply Decisions');

    try {
      const response = await fetch(`${baseUrl}/diamonds/order-approvals`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status} ${response.statusText}: ${errorText}`);
      }
      const data = await response.json();
      const totalRoots = Array.isArray(data.results) ? data.results.length : 0;
      const totalRows = Array.isArray(data.results)
        ? data.results.reduce((sum, item) => sum + (item.affectedRows ?? 0), 0)
        : 0;
      const latest = Array.isArray(data.results) && data.results.length > 0 ? data.results[0] : null;
      const statusLabel = latest ? `${latest.centerStoneOrderStatus} (${latest.rootApptId})` : 'no updates';
      renderSuccess(
        messageEl,
        `Order approvals processed for ${totalRoots} roots (${totalRows} rows). Latest status: ${statusLabel}.`,
      );
    } catch (error) {
      console.error(error);
      renderError(messageEl, `Failed to submit order approvals: ${error.message}`);
    } finally {
      setBusy(submitButton, false, '', 'Apply Decisions');
    }
  });
}
