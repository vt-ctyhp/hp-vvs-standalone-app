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

export function initDiamondsDecisionForm() {
  const form = document.getElementById('diamondsDecisionForm');
  if (!form) {
    return;
  }
  const messageEl = document.getElementById('diamondsDecisionMessage');
  const submitButton = form.querySelector('button[type="submit"]');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const roots = parseRoots(form.rootApptIds.value || '');
    if (roots.length === 0) {
      renderError(messageEl, 'Enter at least one Root Appt ID.');
      return;
    }
    const decision = form.decision.value;
    const decidedBy = form.decidedBy.value.trim();
    const decidedDate = form.decidedDate.value;
    const applyDefaults = form.applyDefaults.checked;

    const items = roots.map((root) => ({
      rootApptId: root,
      decision,
      decidedBy: applyDefaults ? null : (decidedBy || null),
      decidedDate: applyDefaults ? null : (decidedDate || null),
    }));

    const payload = {
      items,
      defaultDecidedBy: decidedBy || null,
      defaultDecidedDate: decidedDate || null,
      applyDefaultsToAll: applyDefaults,
    };

    const baseUrl = getServiceBaseUrl();
    messageEl.textContent = '';
    messageEl.classList.remove('success', 'error');
    setBusy(submitButton, true, 'Saving...', 'Submit Decisions');

    try {
      const response = await fetch(`${baseUrl}/diamonds/stone-decisions`, {
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
      renderSuccess(
        messageEl,
        `Decisions applied to ${totalRoots} roots.`,
      );
    } catch (error) {
      console.error(error);
      renderError(messageEl, `Failed to submit stone decisions: ${error.message}`);
    } finally {
      setBusy(submitButton, false, '', 'Submit Decisions');
    }
  });
}
