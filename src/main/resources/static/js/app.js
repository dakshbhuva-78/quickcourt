/* QuickCourt - shared helpers used by every page.
   Loaded after Bootstrap. Provides: api(), Auth, navbar, toast, formatting helpers, venue cards. */

const API_BASE = '/api';

const SPORTS = {
  BADMINTON: { label: 'Badminton', emoji: '🏸', bg: 'linear-gradient(135deg,#0F6B3C,#2FA866)' },
  FOOTBALL: { label: 'Football', emoji: '⚽', bg: 'linear-gradient(135deg,#0B1F3A,#1c5d8f)' },
  CRICKET: { label: 'Cricket', emoji: '🏏', bg: 'linear-gradient(135deg,#0F6B3C,#0B1F3A)' },
  TENNIS: { label: 'Tennis', emoji: '🎾', bg: 'linear-gradient(135deg,#2FA866,#0B1F3A)' },
  TABLE_TENNIS: { label: 'Table Tennis', emoji: '🏓', bg: 'linear-gradient(135deg,#1c5d8f,#0F6B3C)' },
  BASKETBALL: { label: 'Basketball', emoji: '🏀', bg: 'linear-gradient(135deg,#0B1F3A,#0F6B3C)' }
};

/* ---------- session (token is kept in the browser's localStorage) ---------- */
const Auth = {
  get token() { return localStorage.getItem('qc_token'); },
  get user() {
    try { return JSON.parse(localStorage.getItem('qc_user')); } catch (e) { return null; }
  },
  save(resp) {
    localStorage.setItem('qc_token', resp.token);
    localStorage.setItem('qc_user', JSON.stringify({
      id: resp.id, name: resp.name, email: resp.email, role: resp.role, dashboard: resp.dashboard
    }));
  },
  clear() { localStorage.removeItem('qc_token'); localStorage.removeItem('qc_user'); }
};

/* ---------- small helpers ---------- */
function esc(v) { // ALWAYS escape server data before putting it in innerHTML (prevents XSS)
  return String(v === null || v === undefined ? '' : v).replace(/[&<>"']/g, function (c) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
  });
}
function money(n) { return '₹' + Number(n || 0).toLocaleString('en-IN'); }
function pad(n) { return String(n).padStart(2, '0'); }
function todayStr(offset) {
  const d = new Date(); d.setDate(d.getDate() + (offset || 0));
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}
function fmtDate(s) {
  if (!s) return '';
  const p = s.split('-');
  return new Date(p[0], p[1] - 1, p[2]).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}
function shortDay(s) { const p = s.split('-'); return p[2] + '/' + p[1]; }
function qs(name) { return new URLSearchParams(location.search).get(name); }
function sportMeta(key) { return SPORTS[key] || { label: key, emoji: '🏟️', bg: 'linear-gradient(135deg,#0B1F3A,#0F6B3C)' }; }
function shorten(t, n) { t = t || ''; return t.length > n ? t.substring(0, n - 1) + '…' : t; }
function statusBadge(s) { return '<span class="status status-' + esc(s) + '">' + esc(s) + '</span>'; }
function currentPage() { return (location.pathname.split('/').pop() || 'index.html') + location.search; }
function slotStart(slot) { return slot.substring(0, 5); }
function genSlots(open, close) { // same rule as the Java SlotUtil: hourly slots between open and close
  const toMin = function (t) { return parseInt(t.substring(0, 2), 10) * 60 + parseInt(t.substring(3, 5), 10); };
  const out = [];
  for (let m = toMin(open); m + 60 <= toMin(close); m += 60) {
    out.push(pad(Math.floor(m / 60)) + ':' + pad(m % 60) + '-' + pad(Math.floor((m + 60) / 60)) + ':' + pad((m + 60) % 60));
  }
  return out;
}

/* ---------- talking to the backend ---------- */
async function api(path, options) {
  options = options || {};
  const headers = { 'Content-Type': 'application/json' };
  if (Auth.token) headers['Authorization'] = 'Bearer ' + Auth.token;
  const res = await fetch(API_BASE + path, {
    method: options.method || 'GET',
    headers: headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined
  });
  const text = await res.text();
  let data = null;
  if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }
  if (!res.ok) {
    if (res.status === 401 && Auth.token) {          // session expired
      Auth.clear();
      if (!location.pathname.endsWith('login.html')) {
        location.href = 'login.html?next=' + encodeURIComponent(currentPage());
      }
    }
    const err = new Error((data && data.message) || 'Something went wrong. Please try again.');
    err.status = res.status;
    throw err;
  }
  return data;
}

async function downloadFile(path, filename) { // CSV / report download with the login token
  const res = await fetch(API_BASE + path, { headers: { 'Authorization': 'Bearer ' + Auth.token } });
  if (!res.ok) { toast('Download failed', 'error'); return; }
  const blob = await res.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  URL.revokeObjectURL(a.href);
}

/* ---------- UI helpers ---------- */
function toast(message, type) {
  type = type || 'success';
  let box = document.getElementById('toastBox');
  if (!box) {
    box = document.createElement('div');
    box.id = 'toastBox';
    box.className = 'toast-container position-fixed top-0 end-0 p-3';
    box.style.zIndex = 2000;
    document.body.appendChild(box);
  }
  const el = document.createElement('div');
  el.className = 'toast align-items-center border-0 text-bg-' + (type === 'error' ? 'danger' : type);
  el.setAttribute('role', 'alert');
  el.innerHTML = '<div class="d-flex"><div class="toast-body">' + esc(message) +
    '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
  box.appendChild(el);
  const t = new bootstrap.Toast(el, { delay: 4000 });
  el.addEventListener('hidden.bs.toast', function () { el.remove(); });
  t.show();
}

function requireRole(roles) { // call at the top of protected pages
  const u = Auth.user;
  if (!u || !Auth.token) {
    location.href = 'login.html?next=' + encodeURIComponent(currentPage());
    return false;
  }
  if (roles && roles.indexOf(u.role) === -1) {
    toast('You do not have access to this page', 'error');
    setTimeout(function () { location.href = 'index.html'; }, 900);
    return false;
  }
  return true;
}

async function logout() {
  try { await api('/auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
  Auth.clear();
  location.href = 'index.html';
}

function renderNavbar() {
  const el = document.getElementById('navbar');
  if (!el) return;
  const u = Auth.user;
  const page = location.pathname.split('/').pop() || 'index.html';
  const link = function (href, label) {
    return '<li class="nav-item"><a class="nav-link ' + (page === href ? 'active' : '') + '" href="' + href + '">' + label + '</a></li>';
  };
  let links = link('index.html', 'Home') + link('venues.html', 'Venues');
  if (u && u.role === 'PLAYER') links += link('my-bookings.html', 'My Bookings');
  if (u && u.role === 'VENUE_OWNER') links += link('owner-dashboard.html', 'Owner Dashboard');
  if (u && u.role === 'ADMIN') links += link('admin-dashboard.html', 'Admin Dashboard');

  const right = u
    ? '<div class="dropdown"><button class="btn btn-outline-light dropdown-toggle" data-bs-toggle="dropdown">👤 ' + esc(u.name) + '</button>' +
    '<ul class="dropdown-menu dropdown-menu-end"><li><span class="dropdown-item-text small text-muted">' + esc(u.role.replace('_', ' ')) + '</span></li>' +
    '<li><a class="dropdown-item" href="#" id="profileLink">Profile</a></li>' +
    '<li><a class="dropdown-item" href="#" id="logoutLink">Logout</a></li></ul></div>'
    : '<a class="btn btn-outline-light me-2" href="login.html">Login</a><a class="btn btn-qc" href="register.html">Sign up</a>';

  el.innerHTML = '<nav class="navbar navbar-expand-lg navbar-qc sticky-top"><div class="container">' +
    '<a class="navbar-brand" href="index.html">' +
    '<img src="/images/quickcourt-logo.png" alt="QuickCourt Logo" class="brand-logo">' +
    'QuickCourt</a>' +
    '<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu" aria-label="Menu"><span class="navbar-toggler-icon"></span></button>' +
    '<div class="collapse navbar-collapse" id="navMenu"><ul class="navbar-nav me-auto">' + links + '</ul>' +
    '<div class="d-flex align-items-center">' + right + '</div></div></div></nav>';

  const lo = document.getElementById('logoutLink');
  if (lo) lo.addEventListener('click', function (e) { e.preventDefault(); logout(); });
  const pr = document.getElementById('profileLink');
  if (pr) pr.addEventListener('click', function (e) { e.preventDefault(); openProfileModal(); });
}

function renderFooter() {
  const el = document.getElementById('footer');
  if (!el) return;
  el.innerHTML = '<footer class="footer-qc"><div class="container d-flex flex-wrap justify-content-between gap-2">' +
    '<div><strong>QuickCourt</strong> &middot; Less Coordination. More Playing.</div>' +
    '<div class="small">Semester 5 academic project &middot; Payments are simulated &middot; Demo data only</div></div></footer>';
}

function openProfileModal() {
  const u = Auth.user;
  let m = document.getElementById('profileModal');
  if (m) m.remove();
  document.body.insertAdjacentHTML('beforeend',
    '<div class="modal fade" id="profileModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">' +
    '<div class="modal-header"><h5 class="modal-title">My profile</h5><button class="btn-close" data-bs-dismiss="modal"></button></div>' +
    '<div class="modal-body"><div class="mb-3"><label class="form-label">Full name</label><input id="pfName" class="form-control" maxlength="60" value="' + esc(u.name) + '"></div>' +
    '<div class="mb-3"><label class="form-label">Email</label><input class="form-control" value="' + esc(u.email) + '" disabled></div>' +
    '<div class="mb-1"><label class="form-label">Role</label><input class="form-control" value="' + esc(u.role) + '" disabled></div></div>' +
    '<div class="modal-footer"><button class="btn btn-qc" id="pfSave">Save</button></div></div></div></div>');
  const modal = new bootstrap.Modal(document.getElementById('profileModal'));
  modal.show();
  document.getElementById('pfSave').addEventListener('click', async function () {
    try {
      const updated = await api('/users/me', { method: 'PUT', body: { name: document.getElementById('pfName').value } });
      const stored = Auth.user; stored.name = updated.name;
      localStorage.setItem('qc_user', JSON.stringify(stored));
      modal.hide(); renderNavbar(); toast('Profile updated');
    } catch (e) { toast(e.message, 'error'); }
  });
}

/* ---------- venue visuals ---------- */
function venueImage(v, large) {
  const s = sportMeta(v.sportType);
  const cls = 'venue-img' + (large ? ' venue-img-lg' : '');
  if (v.imageUrl && /^https?:\/\//.test(v.imageUrl)) {
    return '<div class="' + cls + '" style="background-image:url(\'' + encodeURI(v.imageUrl).replace(/'/g, '%27') + '\')" role="img" aria-label="' + esc(v.name) + '"></div>';
  }
  return '<div class="' + cls + '" style="background:' + s.bg + '" role="img" aria-label="' + esc(s.label) + '">' + s.emoji + '</div>';
}

function venueCard(v) {
  const s = sportMeta(v.sportType);
  return '<div class="col-md-6 col-lg-4"><div class="card venue-card h-100">' + venueImage(v, false) +
    '<div class="card-body d-flex flex-column"><div class="d-flex justify-content-between align-items-start gap-2">' +
    '<h5 class="card-title mb-1">' + esc(v.name) + '</h5><span class="badge badge-sport">' + s.emoji + ' ' + esc(s.label) + '</span></div>' +
    '<p class="text-muted small mb-2">📍 ' + esc(v.location) + '</p>' +
    '<p class="small mb-3">' + esc(shorten(v.description, 90)) + '</p>' +
    '<div class="mt-auto d-flex justify-content-between align-items-center"><div><span class="price">' + money(v.pricePerHour) +
    '</span><span class="text-muted small"> / hour</span></div>' +
    '<a href="venue-details.html?id=' + encodeURIComponent(v.id) + '" class="btn btn-qc btn-sm">View &amp; Book</a></div></div></div></div>';
}

document.addEventListener('DOMContentLoaded', function () { renderNavbar(); renderFooter(); });
