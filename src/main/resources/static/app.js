const messageEl = document.getElementById('message');
const conversationIdEl = document.getElementById('conversationId');
const promptTemplateEl = document.getElementById('promptTemplate');
const endpointModeEl = document.getElementById('endpointMode');
const autoGenerateIdEl = document.getElementById('autoGenerateId');
const sendBtn = document.getElementById('sendBtn');
const newConversationBtn = document.getElementById('newConversation');
const clearBtn = document.getElementById('clearBtn');
const outputEl = document.getElementById('output');
const statusEl = document.getElementById('status');

function setStatus(message, error = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle('error', error);
}

function ensureConversationId() {
  if (!conversationIdEl.value.trim() && autoGenerateIdEl.checked) {
    conversationIdEl.value = crypto.randomUUID();
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function appendLine(role, text, className = '') {
  const block = document.createElement('div');
  block.className = `message-line ${className}`.trim();
  block.innerHTML = `<span class="role">${role}:</span> ${escapeHtml(text).replace(/\n/g, '<br>')}`;
  outputEl.appendChild(block);
  outputEl.scrollTop = outputEl.scrollHeight;
  return block;
}

function appendAssistantChunk(block, text) {
  block.innerHTML = `<span class="role">assistant:</span> ${escapeHtml(text).replace(/\n/g, '<br>')}`;
  outputEl.scrollTop = outputEl.scrollHeight;
}

function newConversation() {
  conversationIdEl.value = crypto.randomUUID();
  setStatus('Generated a new conversation ID.');
}

function clearOutput() {
  outputEl.innerHTML = '';
  setStatus('Output cleared.');
}

async function loadPromptTemplates() {
  try {
    const res = await fetch('/api/chat/prompts');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const templates = await res.json();
    const names = Object.keys(templates);

    if (!names.length) return;

    promptTemplateEl.innerHTML = '';
    names.forEach(name => {
      const option = document.createElement('option');
      option.value = name;
      option.textContent = name;
      promptTemplateEl.appendChild(option);
    });
  } catch (err) {
    console.warn('Could not load prompt templates:', err);
  }
}

async function sendNormalMessage(payload) {
  const res = await fetch('/api/chat/message', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(extractErrorMessage(data, `HTTP ${res.status}`));
  }

  return data;
}

function extractErrorMessage(data, fallback) {
  if (!data || typeof data !== 'object') {
    return fallback;
  }

  if (typeof data.message === 'string' && data.message.trim()) {
    return data.message.trim();
  }

  if (typeof data.error === 'string' && data.error.trim()) {
    return data.error.trim();
  }

  return fallback;
}

async function readErrorBody(response) {
  const text = await response.text().catch(() => '');
  if (!text.trim()) {
    return `HTTP ${response.status}`;
  }

  try {
    const parsed = JSON.parse(text);
    return extractErrorMessage(parsed, text);
  } catch (e) {
    return text;
  }
}

function parseSseEvent(rawEvent) {
  let eventName = 'message';
  const dataLines = [];

  rawEvent.split(/\r?\n/).forEach(line => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      const raw = line.slice(5);
      try {
        dataLines.push(JSON.parse(raw));
      } catch {
        dataLines.push(raw);
      }
    }
  });

  return { eventName, data: dataLines.join('\n') };
}

async function sendStreamingMessage(payload) {
  const res = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    throw new Error(await readErrorBody(res));
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let gotComplete = false;
  let gotContent = false;
  const assistantBlock = appendLine('assistant', '', 'assistant');
  assistantBlock.dataset.content = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    let boundary;
    while ((boundary = buffer.indexOf('\n\n')) !== -1) {
      const rawEvent = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      const { eventName, data } = parseSseEvent(rawEvent);

      if (eventName === 'message' && data) {
        gotContent = true;
        assistantBlock.dataset.content += data;
        appendAssistantChunk(assistantBlock, assistantBlock.dataset.content);
      } else if (eventName === 'error') {
        throw new Error(data || 'Streaming request failed.');
      } else if (eventName === 'complete') {
        gotComplete = true;
        setStatus('Streaming complete.');
      }
    }
  }

  if (!gotComplete && !gotContent) {
    throw new Error('Streaming ended without a response. Please check server logs.');
  }

  return { streamed: true };
}

async function handleSend() {
  const message = messageEl.value.trim();
  if (!message) {
    setStatus('Please enter a message.', true);
    return;
  }

  ensureConversationId();

  const payload = {
    message,
    conversationId: conversationIdEl.value.trim() || null,
    stream: endpointModeEl.value === 'stream',
    promptTemplate: promptTemplateEl.value
  };

  sendBtn.disabled = true;
  setStatus('Sending request...');
  appendLine('user', message);

  try {
    if (payload.stream) {
      await sendStreamingMessage(payload);
      setStatus(`Done. Conversation ID: ${payload.conversationId || '(new)'}`);
    } else {
      const result = await sendNormalMessage(payload);
      appendLine('assistant', result.response ?? '', 'assistant');
      setStatus(`Done. Conversation ID: ${result.conversationId || payload.conversationId || '(new)'}`);
    }
    messageEl.value = '';
  } catch (err) {
    console.error(err);
    setStatus(err.message || 'Request failed.', true);
  } finally {
    sendBtn.disabled = false;
  }
}

newConversationBtn.addEventListener('click', newConversation);
clearBtn.addEventListener('click', clearOutput);
sendBtn.addEventListener('click', handleSend);
messageEl.addEventListener('keydown', (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    handleSend();
  }
});

loadPromptTemplates();
newConversation();

