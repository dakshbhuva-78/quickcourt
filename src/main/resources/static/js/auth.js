/* Login and registration forms. */

function showFormError(msg) {
  const box = document.getElementById('formError');
  box.textContent = msg;
  box.classList.remove('d-none');
}

function safeNext(next) { // only allow our own pages (prevents open-redirect tricks)
  return next && /^[\w-]+\.html(\?[\w=&%.-]*)?$/.test(next) ? next : null;
}

const loginForm = document.getElementById('loginForm');
if (loginForm) {
  if (Auth.token && Auth.user) location.href = Auth.user.dashboard || 'index.html';
  loginForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    if (!email || !password) { showFormError('Please enter your email and password'); return; }
    const btn = document.getElementById('loginBtn'); btn.disabled = true;
    try {
      const resp = await api('/auth/login', { method: 'POST', body: { email: email, password: password } });
      Auth.save(resp);
      location.href = safeNext(qs('next')) || resp.dashboard || 'index.html';
    } catch (err) {
      showFormError(err.message);
      btn.disabled = false;
    }
  });
}

const registerForm = document.getElementById('registerForm');
if (registerForm) {
  const wanted = qs('role');
  if (wanted === 'VENUE_OWNER' || wanted === 'PLAYER') document.getElementById('role').value = wanted;

  registerForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirm = document.getElementById('confirm').value;
    const role = document.getElementById('role').value;

    if (name.length < 2) return showFormError('Please enter your full name');
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) return showFormError('Please enter a valid email');
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(password))
      return showFormError('Password needs 8+ characters with upper-case, lower-case and a digit');
    if (password !== confirm) return showFormError('Passwords do not match');

    const btn = document.getElementById('regBtn'); btn.disabled = true;
    try {
      await api('/auth/register', { method: 'POST', body: { name: name, email: email, password: password, role: role } });
      toast('Account created! Please log in.');
      setTimeout(function () { location.href = 'login.html'; }, 900);
    } catch (err) {
      showFormError(err.message);
      btn.disabled = false;
    }
  });
}
