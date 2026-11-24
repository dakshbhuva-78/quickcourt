/* Owner dashboard and Admin dashboard logic (charts use Chart.js). */

const DASH = document.body.dataset.page;
const charts = {};
const PALETTE = ['#0F6B3C', '#0B1F3A', '#2FA866', '#E8A33D', '#5B6B7B', '#1c5d8f'];

function drawChart(id, type, labels, data, label, colors) {
  const canvas = document.getElementById(id);
  if (!canvas || typeof Chart === 'undefined') return;
  if (charts[id]) charts[id].destroy();
  const round = type === 'doughnut';
  charts[id] = new Chart(canvas, {
    type: type,
    data: { labels: labels, datasets: [{ label: label, data: data, backgroundColor: colors || PALETTE, borderColor: type === 'line' ? '#0F6B3C' : undefined, tension: .3, borderRadius: type === 'bar' ? 6 : 0 }] },
    options: {
      responsive: true,
      plugins: { legend: { display: round } },
      scales: round ? {} : { y: { beginAtZero: true, ticks: { precision: 0 } } }
    }
  });
}

function kpi(value, label) {
  return '<div class="col-6 col-lg-3"><div class="kpi"><div class="value">' + esc(value) + '</div><div class="label">' + esc(label) + '</div></div></div>';
}
function keys(obj) { return Object.keys(obj || {}); }
function vals(obj) { return keys(obj).map(function (k) { return Number(obj[k]); }); }
function sportLabels(obj) { return keys(obj).map(function (k) { return sportMeta(k).label; }); }

function bookingRows(list, emptyMsg) {
  if (!list.length) return '<tr><td colspan="7" class="text-center text-muted py-4">' + emptyMsg + '</td></tr>';
  return list.map(function (b) {
    return '<tr><td>' + b.id + '</td><td>' + esc(b.user.name) + '</td><td>' + esc(b.venue.name) + '<div class="small text-muted">' + esc(b.court.courtName) + '</div></td>' +
      '<td>' + esc(fmtDate(b.bookingDate)) + '</td><td>' + esc(b.timeSlot) + '</td><td>' + money(b.amount) + '</td><td>' + statusBadge(b.bookingStatus) + '</td></tr>';
  }).join('');
}

/* ====================== OWNER ====================== */
function initOwner() {
  if (!requireRole(['VENUE_OWNER'])) return;
  document.getElementById('welcome').textContent = 'Welcome, ' + Auth.user.name;

  let myVenues = [];
  let editingId = null;
  let ownerBookings = [];
  const venueModal = new bootstrap.Modal(document.getElementById('venueModal'));

  const sportSel = document.getElementById('vSport');
  Object.keys(SPORTS).forEach(function (k) {
    const o = document.createElement('option'); o.value = k; o.textContent = SPORTS[k].emoji + ' ' + SPORTS[k].label; sportSel.appendChild(o);
  });

  async function loadOverview() {
    try {
      const s = await api('/owner/stats');
      document.getElementById('kpis').innerHTML =
        kpi(s.totalBookings, 'Total bookings') + kpi(s.activeCourts, 'Active courts') +
        kpi(money(s.earnings), 'Earnings (simulated)') + kpi(s.totalVenues, 'Venues listed');
      drawChart('chDay', 'bar', keys(s.bookingsByDay).map(shortDay), vals(s.bookingsByDay), 'Bookings', ['#0F6B3C']);
      drawChart('chEarn', 'doughnut', sportLabels(s.earningsBySport), vals(s.earningsBySport), 'Earnings');
      drawChart('chHour', 'bar', keys(s.bookingsByHour), vals(s.bookingsByHour), 'Bookings', ['#0B1F3A']);
    } catch (e) { toast(e.message, 'error'); }
  }

  async function loadVenues() {
    try { myVenues = await api('/owner/venues'); } catch (e) { toast(e.message, 'error'); return; }
    document.getElementById('venueRows').innerHTML = myVenues.length ? myVenues.map(function (v) {
      return '<tr><td><div class="fw-semibold">' + esc(v.name) + '</div><div class="small text-muted">' + esc(v.location) + '</div>' +
        (v.adminComment ? '<div class="small text-danger">Admin: ' + esc(v.adminComment) + '</div>' : '') + '</td>' +
        '<td>' + esc(sportMeta(v.sportType).label) + '</td><td>' + money(v.pricePerHour) + '</td>' +
        '<td>' + esc(String(v.openingTime).substring(0, 5)) + '–' + esc(String(v.closingTime).substring(0, 5)) + '</td><td>' + statusBadge(v.status) + '</td>' +
        '<td><button class="btn btn-sm btn-outline-qc" data-edit="' + v.id + '">Edit</button></td></tr>';
    }).join('') : '<tr><td colspan="6" class="text-center text-muted py-4">No venues yet. Click "Add venue".</td></tr>';
    document.querySelectorAll('[data-edit]').forEach(function (b) {
      b.addEventListener('click', function () { openVenueForm(myVenues.find(function (v) { return String(v.id) === b.dataset.edit; })); });
    });
    const sel = document.getElementById('cvVenue');
    const keep = sel.value;
    sel.innerHTML = myVenues.map(function (v) { return '<option value="' + v.id + '">' + esc(v.name) + '</option>'; }).join('');
    if (keep) sel.value = keep;
    loadCourts();
  }

  function openVenueForm(v) {
    editingId = v ? v.id : null;
    document.getElementById('venueModalTitle').textContent = v ? 'Edit venue' : 'Add venue';
    document.getElementById('venueErr').classList.add('d-none');
    document.getElementById('vName').value = v ? v.name : '';
    document.getElementById('vLocation').value = v ? v.location : '';
    document.getElementById('vSport').value = v ? v.sportType : 'BADMINTON';
    document.getElementById('vPrice').value = v ? v.pricePerHour : '';
    document.getElementById('vOpen').value = v ? String(v.openingTime).substring(0, 5) : '06:00';
    document.getElementById('vClose').value = v ? String(v.closingTime).substring(0, 5) : '22:00';
    document.getElementById('vAmen').value = v ? (v.amenities || '') : '';
    document.getElementById('vImg').value = v ? (v.imageUrl || '') : '';
    document.getElementById('vDesc').value = v ? (v.description || '') : '';
    venueModal.show();
  }
  document.getElementById('addVenueBtn').addEventListener('click', function () { openVenueForm(null); });

  document.getElementById('venueForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const body = {
      name: document.getElementById('vName').value.trim(), location: document.getElementById('vLocation').value.trim(),
      description: document.getElementById('vDesc').value, sportType: document.getElementById('vSport').value,
      pricePerHour: Number(document.getElementById('vPrice').value),
      openingTime: document.getElementById('vOpen').value, closingTime: document.getElementById('vClose').value,
      amenities: document.getElementById('vAmen').value, imageUrl: document.getElementById('vImg').value
    };
    try {
      if (editingId) await api('/owner/venues/' + editingId, { method: 'PUT', body: body });
      else await api('/owner/venues', { method: 'POST', body: body });
      venueModal.hide(); toast(editingId ? 'Venue updated' : 'Venue added. Waiting for admin approval.');
      loadVenues(); loadOverview();
    } catch (err) {
      const box = document.getElementById('venueErr'); box.textContent = err.message; box.classList.remove('d-none');
    }
  });

  /* ---- courts and blocked slots ---- */
  let courts = [];
  function selectedVenue() { return myVenues.find(function (v) { return String(v.id) === document.getElementById('cvVenue').value; }); }

  async function loadCourts() {
    const v = selectedVenue();
    const list = document.getElementById('courtRows');
    if (!v) { list.innerHTML = '<li class="list-group-item text-muted">Add a venue first.</li>'; document.getElementById('bkCourt').innerHTML = ''; document.getElementById('blockRows').innerHTML = ''; return; }
    try { courts = await api('/owner/venues/' + v.id + '/courts'); } catch (e) { toast(e.message, 'error'); return; }
    list.innerHTML = courts.length ? courts.map(function (c) {
      return '<li class="list-group-item d-flex justify-content-between align-items-center">' + esc(c.courtName) +
        '<button class="btn btn-sm btn-outline-danger" data-del="' + c.id + '">Remove</button></li>';
    }).join('') : '<li class="list-group-item text-muted">No courts yet.</li>';
    list.querySelectorAll('[data-del]').forEach(function (b) {
      b.addEventListener('click', async function () {
        if (!confirm('Remove this court? Old bookings are kept.')) return;
        try { await api('/owner/courts/' + b.dataset.del, { method: 'DELETE' }); toast('Court removed'); loadCourts(); loadOverview(); }
        catch (e) { toast(e.message, 'error'); }
      });
    });
    document.getElementById('bkCourt').innerHTML = courts.map(function (c) { return '<option value="' + c.id + '">' + esc(c.courtName) + '</option>'; }).join('');
    document.getElementById('bkSlot').innerHTML = genSlots(String(v.openingTime), String(v.closingTime)).map(function (s) { return '<option>' + s + '</option>'; }).join('');
    loadBlocks();
  }

  async function loadBlocks() {
    const cid = document.getElementById('bkCourt').value;
    const list = document.getElementById('blockRows');
    if (!cid) { list.innerHTML = ''; return; }
    try {
      const blocks = await api('/owner/courts/' + cid + '/blocks');
      list.innerHTML = blocks.length ? blocks.map(function (b) {
        return '<li class="list-group-item d-flex justify-content-between align-items-center"><span>' + esc(fmtDate(b.blockDate)) + ' · ' + esc(b.timeSlot) +
          ' <span class="text-muted small">(' + esc(b.reason) + ')</span></span><button class="btn btn-sm btn-outline-secondary" data-unblock="' + b.id + '">Unblock</button></li>';
      }).join('') : '<li class="list-group-item text-muted">No blocked slots.</li>';
      list.querySelectorAll('[data-unblock]').forEach(function (btn) {
        btn.addEventListener('click', async function () {
          try { await api('/owner/blocks/' + btn.dataset.unblock, { method: 'DELETE' }); toast('Slot unblocked'); loadBlocks(); }
          catch (e) { toast(e.message, 'error'); }
        });
      });
    } catch (e) { toast(e.message, 'error'); }
  }

  document.getElementById('cvVenue').addEventListener('change', loadCourts);
  document.getElementById('bkCourt').addEventListener('change', loadBlocks);
  const bkDate = document.getElementById('bkDate'); bkDate.min = todayStr(0); bkDate.value = todayStr(0);

  document.getElementById('courtForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const v = selectedVenue(); if (!v) return;
    try {
      await api('/owner/venues/' + v.id + '/courts', { method: 'POST', body: { courtName: document.getElementById('courtName').value.trim(), sportType: v.sportType } });
      document.getElementById('courtName').value = ''; toast('Court added'); loadCourts(); loadOverview();
    } catch (err) { toast(err.message, 'error'); }
  });
  document.getElementById('blockForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const cid = document.getElementById('bkCourt').value; if (!cid) { toast('Add a court first', 'error'); return; }
    try {
      await api('/owner/courts/' + cid + '/blocks', { method: 'POST', body: {
        date: bkDate.value, timeSlot: document.getElementById('bkSlot').value, reason: document.getElementById('bkReason').value } });
      toast('Slot blocked'); document.getElementById('bkReason').value = ''; loadBlocks();
    } catch (err) { toast(err.message, 'error'); }
  });

  /* ---- bookings ---- */
  function renderOwnerBookings() {
    const f = document.getElementById('obFilter').value;
    const now = new Date();
    const list = ownerBookings.filter(function (b) {
      const start = new Date(b.bookingDate + 'T' + b.timeSlot.substring(0, 5) + ':00');
      return f === 'ALL' || (f === 'UPCOMING' ? start >= now : start < now);
    });
    document.getElementById('obRows').innerHTML = bookingRows(list, 'No bookings to show.');
  }
  async function loadOwnerBookings() {
    try { ownerBookings = await api('/owner/bookings'); renderOwnerBookings(); } catch (e) { toast(e.message, 'error'); }
  }
  document.getElementById('obFilter').addEventListener('change', renderOwnerBookings);
  document.getElementById('obExport').addEventListener('click', function () { downloadFile('/bookings/export', 'owner_bookings.csv'); });

  loadOverview(); loadVenues(); loadOwnerBookings();
}

/* ====================== ADMIN ====================== */
function initAdmin() {
  if (!requireRole(['ADMIN'])) return;
  const decisionModal = new bootstrap.Modal(document.getElementById('decisionModal'));
  let decision = null;

  async function loadOverview() {
    try {
      const s = await api('/admin/stats');
      document.getElementById('kpis').innerHTML =
        kpi(s.totalUsers, 'Total users') + kpi(s.totalOwners, 'Facility owners') +
        kpi(s.totalBookings, 'Total bookings') + kpi(s.activeCourts, 'Active courts') +
        kpi(s.totalVenues, 'Venues') + kpi(s.pendingVenues, 'Pending approval') +
        kpi(s.confirmedBookings, 'Confirmed / completed') + kpi(money(s.earnings), 'Earnings (simulated)');
      document.getElementById('pendingBadge').textContent = s.pendingVenues || '';
      drawChart('chDay', 'bar', keys(s.bookingsByDay).map(shortDay), vals(s.bookingsByDay), 'Bookings', ['#0F6B3C']);
      drawChart('chReg', 'line', keys(s.registrationsByDay).map(shortDay), vals(s.registrationsByDay), 'New users');
      drawChart('chVenue', 'doughnut', keys(s.venuesByStatus), vals(s.venuesByStatus), 'Venues', ['#E8A33D', '#0F6B3C', '#9b2020']);
      drawChart('chSport', 'bar', sportLabels(s.bookingsBySport), vals(s.bookingsBySport), 'Bookings');
      drawChart('chEarn', 'doughnut', sportLabels(s.earningsBySport), vals(s.earningsBySport), 'Earnings');
    } catch (e) { toast(e.message, 'error'); }
    try {
      const lines = await api('/admin/logs?lines=25');
      document.getElementById('logBox').textContent = lines.length ? lines.join('\n') : 'No log entries yet.';
    } catch (e) { document.getElementById('logBox').textContent = 'Could not read log.'; }
  }

  async function loadApprovals() {
    const status = document.getElementById('avFilter').value;
    const box = document.getElementById('avList');
    try {
      const venues = await api('/admin/venues' + (status ? '?status=' + status : ''));
      box.innerHTML = venues.length ? venues.map(function (v) {
        const s = sportMeta(v.sportType);
        return '<div class="col-md-6"><div class="card venue-card h-100"><div class="card-body">' +
          '<div class="d-flex justify-content-between"><h5>' + esc(v.name) + '</h5>' + statusBadge(v.status) + '</div>' +
          '<div class="text-muted small mb-2">📍 ' + esc(v.location) + ' · ' + s.emoji + ' ' + esc(s.label) + ' · ' + money(v.pricePerHour) + '/hr · Owner: ' + esc(v.owner.name) + '</div>' +
          '<p class="small">' + esc(shorten(v.description, 160)) + '</p>' +
          '<div class="small text-muted mb-2">Hours ' + esc(String(v.openingTime).substring(0, 5)) + '–' + esc(String(v.closingTime).substring(0, 5)) + ' · ' + esc(v.amenities || 'No amenities listed') + '</div>' +
          (v.adminComment ? '<div class="small text-danger mb-2">Comment: ' + esc(v.adminComment) + '</div>' : '') +
          '<div class="d-flex gap-2">' +
          (v.status !== 'APPROVED' ? '<button class="btn btn-sm btn-qc" data-approve="' + v.id + '">Approve</button>' : '') +
          (v.status !== 'REJECTED' ? '<button class="btn btn-sm btn-outline-danger" data-reject="' + v.id + '">Reject</button>' : '') +
          '</div></div></div></div>';
      }).join('') : '<div class="empty">Nothing here.</div>';
      box.querySelectorAll('[data-approve]').forEach(function (b) { b.addEventListener('click', function () { askDecision(b.dataset.approve, true); }); });
      box.querySelectorAll('[data-reject]').forEach(function (b) { b.addEventListener('click', function () { askDecision(b.dataset.reject, false); }); });
    } catch (e) { toast(e.message, 'error'); }
  }

  function askDecision(id, approve) {
    decision = { id: id, approve: approve };
    document.getElementById('decisionTitle').textContent = approve ? 'Approve venue' : 'Reject venue';
    document.getElementById('decisionComment').value = '';
    decisionModal.show();
  }
  document.getElementById('decisionOk').addEventListener('click', async function () {
    try {
      await api('/admin/venues/' + decision.id + '/' + (decision.approve ? 'approve' : 'reject'),
        { method: 'PUT', body: { comment: document.getElementById('decisionComment').value } });
      decisionModal.hide(); toast(decision.approve ? 'Venue approved' : 'Venue rejected');
      loadApprovals(); loadOverview();
    } catch (e) { toast(e.message, 'error'); }
  });
  document.getElementById('avFilter').addEventListener('change', loadApprovals);

  async function loadUsers() {
    const p = new URLSearchParams();
    if (document.getElementById('uQ').value.trim()) p.set('q', document.getElementById('uQ').value.trim());
    if (document.getElementById('uRole').value) p.set('role', document.getElementById('uRole').value);
    try {
      const users = await api('/admin/users' + (p.toString() ? '?' + p.toString() : ''));
      const rows = document.getElementById('userRows');
      rows.innerHTML = users.map(function (u) {
        return '<tr><td>' + esc(u.name) + '</td><td>' + esc(u.email) + '</td><td>' + esc(u.role.replace('_', ' ')) + '</td>' +
          '<td>' + esc(String(u.createdAt).substring(0, 10)) + '</td><td>' + (u.active ? '<span class="status status-APPROVED">Active</span>' : '<span class="status status-REJECTED">Banned</span>') + '</td>' +
          '<td class="text-end"><button class="btn btn-sm btn-outline-secondary" data-hist="' + u.id + '" data-name="' + esc(u.name) + '">Bookings</button> ' +
          (u.role === 'ADMIN' ? '' : (u.active ? '<button class="btn btn-sm btn-outline-danger" data-ban="' + u.id + '">Ban</button>' : '<button class="btn btn-sm btn-qc" data-unban="' + u.id + '">Unban</button>')) + '</td></tr>';
      }).join('') || '<tr><td colspan="6" class="text-center text-muted py-4">No users found.</td></tr>';
      rows.querySelectorAll('[data-ban]').forEach(function (b) { b.addEventListener('click', function () { setBan(b.dataset.ban, 'ban'); }); });
      rows.querySelectorAll('[data-unban]').forEach(function (b) { b.addEventListener('click', function () { setBan(b.dataset.unban, 'unban'); }); });
      rows.querySelectorAll('[data-hist]').forEach(function (b) { b.addEventListener('click', function () { showHistory(b.dataset.hist, b.dataset.name); }); });
    } catch (e) { toast(e.message, 'error'); }
  }
  async function setBan(id, action) {
    if (!confirm('Are you sure you want to ' + action + ' this user?')) return;
    try { await api('/admin/users/' + id + '/' + action, { method: 'PUT' }); toast(action === 'ban' ? 'User banned' : 'User unbanned'); loadUsers(); }
    catch (e) { toast(e.message, 'error'); }
  }
  async function showHistory(id, name) {
    document.getElementById('ubTitle').textContent = 'Booking history: ' + name;
    const body = document.getElementById('ubBody');
    body.innerHTML = 'Loading…';
    new bootstrap.Modal(document.getElementById('userBookingsModal')).show();
    try {
      const list = await api('/admin/users/' + id + '/bookings');
      body.innerHTML = '<div class="table-responsive"><table class="table"><thead><tr><th>#</th><th>Player</th><th>Venue / Court</th><th>Date</th><th>Time</th><th>Amount</th><th>Status</th></tr></thead><tbody>' +
        bookingRows(list, 'No bookings.') + '</tbody></table></div>';
    } catch (e) { body.textContent = e.message; }
  }
  document.getElementById('userFilter').addEventListener('submit', function (e) { e.preventDefault(); loadUsers(); });

  async function loadBookings() {
    try { document.getElementById('abRows').innerHTML = bookingRows(await api('/admin/bookings'), 'No bookings yet.'); }
    catch (e) { toast(e.message, 'error'); }
  }
  document.getElementById('abExport').addEventListener('click', function () { downloadFile('/bookings/export', 'all_bookings.csv'); });
  document.getElementById('reportBtn').addEventListener('click', function () { downloadFile('/admin/report', 'quickcourt_report.txt'); });

  loadOverview(); loadApprovals(); loadUsers(); loadBookings();
}

if (DASH === 'owner') initOwner();
if (DASH === 'admin') initAdmin();
