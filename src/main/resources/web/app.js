// Vanilla JS, no frameworks. Uses fetch() for all API calls.
// API_BASE is computed from the current path so this works correctly whether served
// at /swagapi/discordutils/ or any other prefix IWebService mounts it under.
var API_BASE = window.location.pathname.replace(/[^/]*$/, '');

function api(path, opts) {
  return fetch(API_BASE + path, opts || {}).then(function (r) {
    return r.json().then(function (body) {
      if (!r.ok) throw new Error(body.error || ('HTTP ' + r.status));
      return body;
    });
  });
}

function escapeHtml(s) {
  var div = document.createElement('div');
  div.textContent = s == null ? '' : String(s);
  return div.innerHTML;
}

function formatRelative(iso) {
  if (!iso) return 'never';
  var then = new Date(iso).getTime();
  var diffSec = Math.round((Date.now() - then) / 1000);
  if (diffSec < 60) return diffSec + 's ago';
  if (diffSec < 3600) return Math.round(diffSec / 60) + 'm ago';
  if (diffSec < 86400) return Math.round(diffSec / 3600) + 'h ago';
  return Math.round(diffSec / 86400) + 'd ago';
}

function renderRow(w) {
  var statusClass = 'status-unknown';
  var statusText = '—';
  if (w.lastSentAt) {
    statusClass = w.lastSuccess ? 'status-ok' : 'status-fail';
    statusText = w.lastSuccess ? 'OK' : ('Failed: ' + escapeHtml(w.lastError || ''));
  } else if (!w.configured) {
    statusText = 'not configured';
  }

  return '<tr>' +
    '<td>' + escapeHtml(w.name) + '</td>' +
    '<td><code>' + escapeHtml(w.urlMasked || '(none)') + '</code></td>' +
    '<td>' + formatRelative(w.lastSentAt) + '</td>' +
    '<td class="' + statusClass + '">' + statusText + '</td>' +
    '<td>' + (w.totalSent || 0) + '</td>' +
    '<td><button class="small" data-test="' + escapeHtml(w.name) + '">Test</button></td>' +
    '</tr>';
}

function loadWebhooks() {
  api('api/webhooks').then(function (data) {
    var body = document.getElementById('webhooks-body');
    if (!data.webhooks || data.webhooks.length === 0) {
      body.innerHTML = '<tr><td colspan="6" class="empty">No webhooks configured yet.</td></tr>';
      return;
    }
    body.innerHTML = data.webhooks.map(renderRow).join('');
  }).catch(function (e) {
    document.getElementById('webhooks-body').innerHTML =
      '<tr><td colspan="6" class="empty">Failed to load: ' + escapeHtml(e.message) + '</td></tr>';
  });
}

document.getElementById('webhooks-body').addEventListener('click', function (e) {
  var name = e.target.getAttribute('data-test');
  if (!name) return;
  e.target.disabled = true;
  e.target.textContent = 'Testing…';
  api('api/webhooks/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'name=' + encodeURIComponent(name)
  }).then(function () {
    loadWebhooks();
  }).catch(function (err) {
    alert('Test failed: ' + err.message);
    loadWebhooks();
  });
});

document.getElementById('webhook-form').addEventListener('submit', function (e) {
  e.preventDefault();
  var name = document.getElementById('name').value.trim();
  var url = document.getElementById('url').value.trim();
  var status = document.getElementById('form-status');
  status.textContent = 'Saving and testing...';

  api('api/webhooks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'name=' + encodeURIComponent(name) + '&url=' + encodeURIComponent(url)
  }).then(function (result) {
    status.textContent = result.testSuccess
      ? 'Saved — test message delivered successfully.'
      : 'Saved, but the test message failed. Check the URL.';
    document.getElementById('name').value = '';
    document.getElementById('url').value = '';
    loadWebhooks();
  }).catch(function (err) {
    status.textContent = 'Error: ' + err.message;
  });
});

loadWebhooks();
