import { getServiceBaseUrl } from './config.js';

export function initDeadlinesForm() {
  const form = document.getElementById('deadlinesForm');
  if (!form) {
    return;
  }
  const messageEl = document.getElementById('deadlinesMessage');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const baseUrl = getServiceBaseUrl();
    const submitButton = form.querySelector('button[type="submit"]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Recording...';
    }
    messageEl.textContent = '';
    messageEl.classList.remove('error', 'success');

    const payload = {
      rootApptId: form.rootApptId.value.trim(),
      deadlineType: form.deadlineType.value.trim(),
      deadlineDate: form.deadlineDate.value,
      movedBy: form.movedBy.value.trim(),
      assistedRep: form.assistedRep.value.trim() || null,
    };

    try {
      const response = await fetch(`${baseUrl}/deadlines/record`, {
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
      messageEl.textContent = `Deadline ${data.deadlineType} recorded for ${data.rootApptId}. Move count: ${data.moveCount}.`;
      messageEl.classList.add('success');
    } catch (error) {
      console.error(error);
      messageEl.textContent = `Failed to record deadline: ${error.message}`;
      messageEl.classList.add('error');
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Record Deadline';
      }
    }
  });
}
