/* Booking page, simulated payment pop-up and "My bookings" page. */

const BK_PAGE = document.body.dataset.page;

/* =============== PAYMENT POP-UP (shared by both pages) =============== */
function openPaymentModal(booking, onFinished) {
  let old = document.getElementById('payModal');
  if (old) old.remove();
  document.body.insertAdjacentHTML('beforeend',
    '<div class="modal fade" id="payModal" tabindex="-1" data-bs-backdrop="static"><div class="modal-dialog modal-dialog-centered"><div class="modal-content">' +
    '<div class="modal-header"><h5 class="modal-title">Complete payment</h5></div><div class="modal-body" id="payBody"></div></div></div></div>');
  const modalEl = document.getElementById('payModal');
  const modal = new bootstrap.Modal(modalEl);

  function showForm(errorMsg) {
    document.getElementById('payBody').innerHTML =
      '<div class="p-3 rounded-3 mb-3" style="background:var(--qc-gray)"><div class="fw-semibold">' + esc(booking.venue.name) + ' · ' + esc(booking.court.courtName) + '</div>' +
      '<div class="small text-muted">' + esc(fmtDate(booking.bookingDate)) + ' · ' + esc(booking.timeSlot) + '</div>' +
      '<div class="price fs-4 mt-1">' + money(booking.amount) + '</div></div>' +
      (errorMsg ? '<div class="alert alert-danger py-2">' + esc(errorMsg) + '</div>' : '') +
      '<div class="alert alert-info py-2 small">Simulation only. Do not enter real card details.<br>' +
      'To test a failure: UPI id starting with <b>fail</b> (fail@upi) or a card number ending in <b>0000</b>.</div>' +
      '<div class="mb-3"><div class="form-check form-check-inline"><input class="form-check-input" type="radio" name="pm" id="pmUPI" value="UPI" checked><label class="form-check-label" for="pmUPI">UPI</label></div>' +
      '<div class="form-check form-check-inline"><input class="form-check-input" type="radio" name="pm" id="pmCARD" value="CARD"><label class="form-check-label" for="pmCARD">Card</label></div>' +
      '<div class="form-check form-check-inline"><input class="form-check-input" type="radio" name="pm" id="pmCASH" value="CASH"><label class="form-check-label" for="pmCASH">Cash at venue</label></div></div>' +
      '<div id="pmUPIbox" class="mb-3"><label class="form-label" for="upiId">UPI ID</label><input id="upiId" class="form-control" placeholder="yourname@bank" value="demo@upi"></div>' +
      '<div id="pmCARDbox" class="mb-3 d-none"><label class="form-label" for="cardNo">Card number (16 digits)</label><input id="cardNo" class="form-control" inputmode="numeric" maxlength="19" placeholder="4111 1111 1111 1111"></div>' +
      '<div id="pmCASHbox" class="mb-3 d-none small text-muted">Your booking is confirmed now. Pay the amount at the venue.</div>' +
      '<div class="d-flex justify-content-between"><button class="btn btn-outline-secondary" id="payLater">Pay later</button><button class="btn btn-qc" id="payNow">Pay ' + money(booking.amount) + '</button></div>';

    modalEl.querySelectorAll('input[name=pm]').forEach(function (r) {
      r.addEventListener('change', function () {
        document.getElementById('pmUPIbox').classList.toggle('d-none', r.value !== 'UPI' || !r.checked);
        document.getElementById('pmCARDbox').classList.toggle('d-none', r.value !== 'CARD' || !r.checked);
        document.getElementById('pmCASHbox').classList.toggle('d-none', r.value !== 'CASH' || !r.checked);
      });
    });
    document.getElementById('payLater').addEventListener('click', function () {
      modal.hide(); toast('Booking saved as PENDING. Pay from My Bookings to confirm.', 'warning');
      onFinished();
    });
    document.getElementById('payNow').addEventListener('click', submitPayment);
  }

  async function submitPayment() {
    const method = modalEl.querySelector('input[name=pm]:checked').value;
    const btn = document.getElementById('payNow'); btn.disabled = true; btn.textContent = 'Processing…';
    try {
      const payment = await api('/payments', { method: 'POST', body: {
        bookingId: booking.id, paymentMethod: method,
        upiId: document.getElementById('upiId').value,
        cardNumber: document.getElementById('cardNo').value
      } });
      if (payment.paymentStatus === 'FAILED') {
        showForm('Payment failed (simulated decline). Reference ' + payment.transactionReference + '. Please try again.');
        return;
      }
      document.getElementById('payBody').innerHTML =
        '<div class="text-center py-3"><div style="font-size:3rem">✅</div><h4>Booking confirmed!</h4>' +
        '<p class="text-muted mb-1">' + esc(booking.venue.name) + ' · ' + esc(booking.court.courtName) + '</p>' +
        '<p class="text-muted mb-1">' + esc(fmtDate(booking.bookingDate)) + ' · ' + esc(booking.timeSlot) + '</p>' +
        '<p class="mb-1">Payment: <b>' + esc(payment.paymentMethod) + '</b> ' + statusBadge(payment.paymentStatus) + '</p>' +
        '<p class="small text-muted">Reference: ' + esc(payment.transactionReference) + '</p>' +
        '<button class="btn btn-qc" id="payDone">View my bookings</button></div>';
      document.getElementById('payDone').addEventListener('click', function () { modal.hide(); onFinished(); });
    } catch (e) {
      showForm(e.message);
    }
  }

  showForm('');
  modal.show();
}

/* =============== BOOKING PAGE =============== */
async function initBookingPage() {
  if (!requireRole(['PLAYER'])) return;
  const venueId = qs('venueId');
  if (!venueId) { location.href = 'venues.html'; return; }

  let venue, courts;
  try {
    venue = await api('/venues/' + encodeURIComponent(venueId));
    courts = await api('/venues/' + encodeURIComponent(venueId) + '/courts');
  } catch (e) { toast(e.message, 'error'); setTimeout(function () { location.href = 'venues.html'; }, 1200); return; }

  const state = { court: null, date: todayStr(0), slot: null };
  document.getElementById('bkTitle').textContent = 'Book: ' + venue.name;
  document.getElementById('bkSub').textContent = '📍 ' + venue.location + '  ·  ' + money(venue.pricePerHour) + ' per hour';
  document.getElementById('smVenue').textContent = venue.name;

  const dateInput = document.getElementById('bkDate');
  dateInput.min = todayStr(0); dateInput.max = todayStr(90); dateInput.value = state.date;

  const courtList = document.getElementById('courtList');
  courtList.innerHTML = courts.length ? courts.map(function (c) {
    return '<div class="col-sm-6 col-md-4"><div class="court-option" data-id="' + c.id + '" tabindex="0" role="button">' + esc(c.courtName) + '</div></div>';
  }).join('') : '<div class="text-muted">This venue has no courts yet.</div>';

  function updateSummary() {
    document.getElementById('smCourt').textContent = state.court ? state.court.courtName : '-';
    document.getElementById('smDate').textContent = state.date ? fmtDate(state.date) : '-';
    document.getElementById('smSlot').textContent = state.slot || '-';
    document.getElementById('smTotal').textContent = state.slot ? money(venue.pricePerHour) : '₹0';
    document.getElementById('confirmBtn').disabled = !(state.court && state.date && state.slot);
  }

  async function loadSlots() {
    state.slot = null; updateSummary();
    const box = document.getElementById('slotBox');
    if (!state.court) { box.innerHTML = '<div class="text-muted">Select a court first.</div>'; return; }
    box.innerHTML = '<div class="text-muted">Loading slots…</div>';
    try {
      const slots = await api('/venues/' + venue.id + '/slots?courtId=' + state.court.id + '&date=' + state.date);
      box.innerHTML = slots.map(function (s) {
        return '<button type="button" class="slot-btn" data-slot="' + esc(s.timeSlot) + '"' + (s.available ? '' : ' disabled') + '>' +
          esc(s.timeSlot) + '<small>' + (s.available ? 'Available' : esc(s.reason)) + '</small></button>';
      }).join('') || '<div class="text-muted">No slots for this date.</div>';
      box.querySelectorAll('.slot-btn:not([disabled])').forEach(function (b) {
        b.addEventListener('click', function () {
          box.querySelectorAll('.slot-btn').forEach(function (x) { x.classList.remove('selected'); });
          b.classList.add('selected'); state.slot = b.dataset.slot; updateSummary();
        });
      });
    } catch (e) { box.innerHTML = '<div class="text-danger">' + esc(e.message) + '</div>'; }
  }

  courtList.querySelectorAll('.court-option').forEach(function (el) {
    const pick = function () {
      courtList.querySelectorAll('.court-option').forEach(function (x) { x.classList.remove('selected'); });
      el.classList.add('selected');
      state.court = courts.find(function (c) { return String(c.id) === el.dataset.id; });
      loadSlots();
    };
    el.addEventListener('click', pick);
    el.addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); pick(); } });
  });
  dateInput.addEventListener('change', function () {
    if (!dateInput.value || dateInput.value < todayStr(0)) { toast('Please choose today or a future date', 'error'); dateInput.value = todayStr(0); }
    state.date = dateInput.value; loadSlots();
  });

  document.getElementById('confirmBtn').addEventListener('click', async function () {
    const btn = this; btn.disabled = true;
    try {
      const booking = await api('/bookings', { method: 'POST', body: {
        venueId: venue.id, courtId: state.court.id, bookingDate: state.date, timeSlot: state.slot } });
      openPaymentModal(booking, function () { location.href = 'my-bookings.html'; });
    } catch (e) {
      toast(e.message, 'error');
      btn.disabled = false;
      loadSlots(); // refresh: someone may have booked the slot meanwhile
    }
  });
  updateSummary();
}

/* =============== MY BOOKINGS PAGE =============== */
function isFuture(b) {
  return new Date(b.bookingDate + 'T' + b.timeSlot.substring(0, 5) + ':00') > new Date();
}

async function loadMyBookings() {
  const box = document.getElementById('bookingList');
  const status = document.getElementById('statusFilter').value;
  let list;
  try { list = await api('/bookings/my' + (status ? '?status=' + status : '')); }
  catch (e) { box.innerHTML = '<div class="empty">' + esc(e.message) + '</div>'; return; }

  if (!list.length) {
    box.innerHTML = '<div class="empty">No bookings yet. <a href="venues.html">Find a venue</a> and book your first game!</div>';
    return;
  }
  box.innerHTML = list.map(function (b) {
    const s = sportMeta(b.venue.sportType);
    const canCancel = (b.bookingStatus === 'PENDING' || b.bookingStatus === 'CONFIRMED') && isFuture(b);
    return '<div class="col-md-6"><div class="card venue-card h-100"><div class="card-body">' +
      '<div class="d-flex justify-content-between align-items-start"><div><h5 class="mb-0">' + esc(b.venue.name) + '</h5>' +
      '<div class="text-muted small">' + s.emoji + ' ' + esc(s.label) + ' · ' + esc(b.court.courtName) + '</div></div>' + statusBadge(b.bookingStatus) + '</div>' +
      '<div class="mt-3">📅 ' + esc(fmtDate(b.bookingDate)) + '<br>🕒 ' + esc(b.timeSlot) + '</div>' +
      '<div class="d-flex justify-content-between align-items-center mt-3"><span class="price">' + money(b.amount) + '</span><div class="d-flex gap-2">' +
      (b.bookingStatus === 'PENDING' && isFuture(b) ? '<button class="btn btn-qc btn-sm" data-pay="' + b.id + '">Pay now</button>' : '') +
      (canCancel ? '<button class="btn btn-outline-danger btn-sm" data-cancel="' + b.id + '">Cancel</button>' : '') +
      '</div></div></div></div></div>';
  }).join('');

  box.querySelectorAll('[data-cancel]').forEach(function (btn) {
    btn.addEventListener('click', async function () {
      if (!confirm('Cancel this booking?')) return;
      try { await api('/bookings/' + btn.dataset.cancel + '/cancel', { method: 'PUT' }); toast('Booking cancelled'); loadMyBookings(); }
      catch (e) { toast(e.message, 'error'); }
    });
  });
  box.querySelectorAll('[data-pay]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      const booking = list.find(function (x) { return String(x.id) === btn.dataset.pay; });
      openPaymentModal(booking, loadMyBookings);
    });
  });
}

function initMyBookings() {
  if (!requireRole(['PLAYER'])) return;
  document.getElementById('statusFilter').addEventListener('change', loadMyBookings);
  document.getElementById('exportBtn').addEventListener('click', function () { downloadFile('/bookings/export', 'my_bookings.csv'); });
  loadMyBookings();
}

if (BK_PAGE === 'booking') initBookingPage();
if (BK_PAGE === 'mybookings') initMyBookings();
