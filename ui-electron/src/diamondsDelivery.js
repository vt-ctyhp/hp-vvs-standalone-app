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

export function initDiamondsDeliveryForm() {
  const form = document.getElementById('diamondsDeliveryForm');
  if (!form) {
    return;
  }
  const messageEl = document.getElementById('diamondsDeliveryMessage');
  const submitButton = form.querySelector('button[type="submit"]');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const roots = parseRoots(form.rootApptIds.value || '');
    if (roots.length === 0) {
      renderError(messageEl, 'Enter at least one Root Appt ID.');
      return;
    }
    const memoDate = form.memoDate.value;
    const applyDefault = form.applyDefault.checked;

    const items = roots.map((root) => ({
      rootApptId: root,
      memoDate: applyDefault ? null : (memoDate || null),
      selected: true,
    }));

    const payload = {
      items,
      defaultMemoDate: memoDate || null,
      applyDefaultToAll: applyDefault,
    };

    const baseUrl = getServiceBaseUrl();
    messageEl.textContent = '';
    messageEl.classList.remove('success', 'error');
    setBusy(submitButton, true, 'Confirming...', 'Confirm Delivery');

    try {
      const response = await fetch(`${baseUrl}/diamonds/confirm-delivery`, {
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
      renderSuccess(
        messageEl,
        `Delivery confirmed for ${totalRoots} roots (${totalRows} rows).`,
      );
    } catch (error) {
      console.error(error);
      renderError(messageEl, `Failed to confirm delivery: ${error.message}`);
    } finally {
      setBusy(submitButton, false, '', 'Confirm Delivery');
    }
  });
}
