/* Home page, venue list (search / filter / pagination) and venue details. */

const PAGE = document.body.dataset.page;

function fillSportSelect(select, selected) {
  Object.keys(SPORTS).forEach(function (k) {
    const o = document.createElement('option');
    o.value = k; o.textContent = SPORTS[k].emoji + ' ' + SPORTS[k].label;
    if (k === selected) o.selected = true;
    select.appendChild(o);
  });
}

/* ================= HOME ================= */
async function initHome() {
  fillSportSelect(document.getElementById('hsSport'));
  document.getElementById('sportTiles').innerHTML = Object.keys(SPORTS).map(function (k) {
    const s = SPORTS[k];
    return '<div class="col-6 col-md-4 col-lg-2"><a class="sport-tile" href="venues.html?sport=' + k + '"><span class="emoji">' + s.emoji + '</span>' + esc(s.label) + '</a></div>';
  }).join('');

  document.getElementById('heroSearch').addEventListener('submit', function (e) {
    e.preventDefault();
    const p = new URLSearchParams();
    const sport = document.getElementById('hsSport').value;
    const loc = document.getElementById('hsLocation').value.trim();
    if (sport) p.set('sport', sport);
    if (loc) p.set('location', loc);
    location.href = 'venues.html' + (p.toString() ? '?' + p.toString() : '');
  });

  const box = document.getElementById('featured');
  try {
    const venues = await api('/venues');
    box.innerHTML = venues.length
      ? venues.slice(0, 6).map(venueCard).join('')
      : '<div class="empty">No venues yet. Owners can add the first one!</div>';
  } catch (e) { box.innerHTML = '<div class="empty">Could not load venues: ' + esc(e.message) + '</div>'; }
}

/* ================= VENUE LIST ================= */
const PER_PAGE = 6;
let allVenues = [];
let currentPageNo = 1;

async function loadVenues() {
  const p = new URLSearchParams();
  const q = document.getElementById('fQ').value.trim();
  const sport = document.getElementById('fSport').value;
  const loc = document.getElementById('fLocation').value.trim();
  const price = document.getElementById('fPrice').value;
  if (q) p.set('q', q);
  if (sport) p.set('sport', sport);
  if (loc) p.set('location', loc);
  if (price) p.set('maxPrice', price);
  try {
    allVenues = await api('/venues' + (p.toString() ? '?' + p.toString() : ''));
  } catch (e) {
    document.getElementById('venueList').innerHTML = '<div class="empty">' + esc(e.message) + '</div>';
    return;
  }
  currentPageNo = 1;
  renderVenuePage();
}

function renderVenuePage() {
  const sort = document.getElementById('fSort').value;
  const list = allVenues.slice();
  if (sort === 'low') list.sort(function (a, b) { return a.pricePerHour - b.pricePerHour; });
  if (sort === 'high') list.sort(function (a, b) { return b.pricePerHour - a.pricePerHour; });

  document.getElementById('resultCount').textContent = list.length + ' venue' + (list.length === 1 ? '' : 's') + ' found';
  const pages = Math.max(1, Math.ceil(list.length / PER_PAGE));
  if (currentPageNo > pages) currentPageNo = pages;
  const slice = list.slice((currentPageNo - 1) * PER_PAGE, currentPageNo * PER_PAGE);

  document.getElementById('venueList').innerHTML = slice.length
    ? slice.map(venueCard).join('')
    : '<div class="empty">No venues match your filters. Try removing some.</div>';

  let pager = '';
  for (let i = 1; i <= pages; i++) {
    pager += '<li class="page-item ' + (i === currentPageNo ? 'active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>';
  }
  const pagerEl = document.getElementById('pager');
  pagerEl.innerHTML = pages > 1 ? pager : '';
  pagerEl.querySelectorAll('a').forEach(function (a) {
    a.addEventListener('click', function (e) {
      e.preventDefault(); currentPageNo = parseInt(a.dataset.page, 10); renderVenuePage();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  });
}

function initVenueList() {
  fillSportSelect(document.getElementById('fSport'), qs('sport'));
  if (qs('location')) document.getElementById('fLocation').value = qs('location');
  document.getElementById('filterForm').addEventListener('submit', function (e) { e.preventDefault(); loadVenues(); });
  document.getElementById('fSort').addEventListener('change', renderVenuePage);
  loadVenues();
}

/* ================= VENUE DETAILS ================= */
async function initDetails() {
  const root = document.getElementById('detailRoot');
  const id = qs('id');
  if (!id) { root.innerHTML = '<div class="empty">No venue selected.</div>'; return; }
  let venue, courts;
  try {
    venue = await api('/venues/' + encodeURIComponent(id));
    courts = await api('/venues/' + encodeURIComponent(id) + '/courts');
  } catch (e) { root.innerHTML = '<div class="empty">' + esc(e.message) + '</div>'; return; }

  const s = sportMeta(venue.sportType);
  const amenities = (venue.amenities || '').split(',').map(function (a) { return a.trim(); }).filter(Boolean)
    .map(function (a) { return '<span class="badge text-bg-light border me-1 mb-1">' + esc(a) + '</span>'; }).join('');
  const u = Auth.user;
  let bookBtn;
  if (!u) bookBtn = '<a class="btn btn-qc btn-lg w-100" href="login.html?next=' + encodeURIComponent('booking.html?venueId=' + venue.id) + '">Log in to book</a>';
  else if (u.role === 'PLAYER') bookBtn = '<a class="btn btn-qc btn-lg w-100" href="booking.html?venueId=' + venue.id + '">Book now</a>';
  else bookBtn = '<div class="alert alert-light border mb-0 small">Only players can book courts.</div>';

  root.innerHTML =
    '<div class="row g-4"><div class="col-lg-8">' + venueImage(venue, true) +
    '<div class="d-flex flex-wrap justify-content-between align-items-start mt-3 gap-2"><div><h1 class="h2 mb-1">' + esc(venue.name) + '</h1>' +
    '<p class="text-muted mb-0">📍 ' + esc(venue.location) + '</p></div><span class="badge badge-sport fs-6">' + s.emoji + ' ' + esc(s.label) + '</span></div>' +
    '<h5 class="mt-4">About this venue</h5><p>' + esc(venue.description || 'No description provided.') + '</p>' +
    '<h5>Amenities</h5><div class="mb-4">' + (amenities || '<span class="text-muted">Not listed</span>') + '</div>' +
    '<h5>Courts (' + courts.length + ')</h5><div class="row g-2 mb-4">' +
    (courts.map(function (c) { return '<div class="col-sm-6 col-md-4"><div class="court-option">' + esc(c.courtName) + '</div></div>'; }).join('') || '<div class="text-muted">No courts added yet.</div>') + '</div>' +
    '<h5>Check available slots</h5><div class="row g-2 align-items-end mb-3">' +
    '<div class="col-sm-6"><label class="form-label small" for="slotCourt">Court</label><select id="slotCourt" class="form-select">' +
    courts.map(function (c) { return '<option value="' + c.id + '">' + esc(c.courtName) + '</option>'; }).join('') + '</select></div>' +
    '<div class="col-sm-6"><label class="form-label small" for="slotDate">Date</label><input type="date" id="slotDate" class="form-control" min="' + todayStr(0) + '" max="' + todayStr(90) + '" value="' + todayStr(0) + '"></div></div>' +
    '<div id="slotBox" class="slot-grid"></div></div>' +
    '<div class="col-lg-4"><div class="summary-card p-4"><div class="text-muted small">Price</div>' +
    '<div class="price fs-3 mb-1">' + money(venue.pricePerHour) + ' <span class="fs-6 text-muted fw-normal">/ hour</span></div>' +
    '<div class="small text-muted mb-3">Open ' + esc(String(venue.openingTime).substring(0, 5)) + ' – ' + esc(String(venue.closingTime).substring(0, 5)) + '</div>' +
    '<div class="mb-3">' + statusBadge(venue.status) + '</div>' + bookBtn + '</div></div></div>';

  async function loadSlots() {
    const box = document.getElementById('slotBox');
    const courtSel = document.getElementById('slotCourt');
    if (!courtSel.value) { box.innerHTML = '<div class="text-muted">Add a court to see slots.</div>'; return; }
    box.innerHTML = '<div class="text-muted">Loading slots…</div>';
    try {
      const slots = await api('/venues/' + venue.id + '/slots?courtId=' + courtSel.value + '&date=' + document.getElementById('slotDate').value);
      box.innerHTML = slots.map(function (sl) {
        return '<button class="slot-btn" disabled style="' + (sl.available ? 'background:var(--qc-green-pale);color:var(--qc-green);border-color:var(--qc-green)' : '') + '">' +
          esc(sl.timeSlot) + '<small>' + (sl.available ? 'Available' : esc(sl.reason)) + '</small></button>';
      }).join('');
    } catch (e) { box.innerHTML = '<div class="text-danger">' + esc(e.message) + '</div>'; }
  }
  document.getElementById('slotCourt').addEventListener('change', loadSlots);
  document.getElementById('slotDate').addEventListener('change', loadSlots);
  loadSlots();
}

if (PAGE === 'home') initHome();
if (PAGE === 'venues') initVenueList();
if (PAGE === 'details') initDetails();
