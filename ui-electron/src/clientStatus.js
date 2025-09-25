import { getServiceBaseUrl } from './config.js';

function valueOrNull(input) {
  const trimmed = input.trim();
  return trimmed.length === 0 ? null : trimmed;
}

export function initClientStatusForm() {
  const form = document.getElementById('clientStatusForm');
  if (!form) {
    return;
  }
  const messageEl = document.getElementById('clientStatusMessage');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const baseUrl = getServiceBaseUrl();

    const submitButton = form.querySelector('button[type="submit"]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Submitting...';
    }
    messageEl.textContent = '';
    messageEl.classList.remove('error', 'success');

    const payload = {
      rootApptId: form.rootApptId.value.trim(),
      salesStage: form.salesStage.value.trim(),
      conversionStatus: form.conversionStatus.value.trim(),
      customOrderStatus: valueOrNull(form.customOrderStatus.value),
      inProductionStatus: valueOrNull(form.inProductionStatus.value),
      centerStoneOrderStatus: valueOrNull(form.centerStoneOrderStatus.value),
      nextSteps: valueOrNull(form.nextSteps.value),
      assistedRep: valueOrNull(form.assistedRep.value),
      updatedBy: form.updatedBy.value.trim(),
    };

    try {
      const response = await fetch(`${baseUrl}/client-status/submit`, {
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
      messageEl.textContent = `Client status saved for ${data.rootApptId}. Updated at ${data.updatedAt}.`;
      messageEl.classList.add('success');
    } catch (error) {
      console.error(error);
      messageEl.textContent = `Failed to submit client status: ${error.message}`;
      messageEl.classList.add('error');
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Submit Client Status';
      }
    }
  });
}
