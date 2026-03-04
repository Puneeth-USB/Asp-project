document.addEventListener('DOMContentLoaded', function() {
    localStorage.removeItem('jwt');
});
// Password toggle
document.getElementById('togglePassword').addEventListener('click', function(){
    const pw = document.getElementById('password');
    const btn = this;
    if(pw.type === 'password'){ pw.type = 'text'; btn.innerText = 'Hide'; }
    else{ pw.type = 'password'; btn.innerText = 'Show'; }
});

async function postJSON(url, payload){

    const res = await fetch(url, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    const text = await res.text();
    return {ok: res.ok, status: res.status, text};
}

const loginForm = document.getElementById('loginForm');
const loginBtn = document.getElementById('loginBtn');
const btnText = document.getElementById('btnText');
const errorEl = document.getElementById('errorMsg');
const successEl = document.getElementById('successMsg');

loginForm.addEventListener('submit', async function(e){
    e.preventDefault();
    errorEl.textContent=''; successEl.textContent='';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    if(!username || !password){ errorEl.textContent = 'Please enter username and password'; return; }

    // show loading
    loginBtn.disabled = true; btnText.style.display='none';
    const loader = document.createElement('span'); loader.className='loader'; loader.id='loaderEl';
    loginBtn.appendChild(loader);

    try{
        // Step 1: Generate token
        const {ok, text: tokenRaw} = await postJSON('http://localhost:1997/auth/token', {userName: username, password});
        const token = tokenRaw ? tokenRaw.trim() : '';
        if(ok && token){
            // Step 2: Validate token with backend (send raw JWT, no encoding)
            const validateRes = await fetch(`http://localhost:1997/auth/validate?token=${token}`);
            const validateText = await validateRes.text();
            if(validateRes.ok && validateText.includes('Token is valid')){
                localStorage.setItem('jwt', token);
                
                // Get token from localStorage and check role
                const storedToken = localStorage.getItem('jwt');
                let role = null;
                
                if (storedToken) {
                    try {
                        const payload = storedToken.split('.')[1];
                        const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
                        role = decoded.role;
                        console.log('Decoded JWT payload from localStorage:', decoded);
                        console.log('Extracted role:', role);
                    } catch (err) {
                        console.error('JWT decoding error:', err);
                        errorEl.textContent = 'Error processing token. Please try again.';
                        return;
                    }
                }
                
                successEl.textContent = 'Login successful — redirecting...';
                setTimeout(()=>{
                    if(role === 'Maintenance'){
                        console.log('Redirecting to maintenance dashboard');
                        window.location.href = '/MaintenanceStaff/maintenance.html?t=' + Date.now();
                    } else {
                        console.log('Redirecting to landing page');
                        window.location.href = '/landingpage/landing.html?t=' + Date.now();
                    }
                }, 700);
            } else {
                errorEl.textContent = 'Token validation failed. Please login again.';
            }
        } else {
            errorEl.textContent = token || 'Invalid credentials';
        }
    }catch(err){
        errorEl.textContent = 'Network error — please try again';
    }finally{
        const loaderNode = document.getElementById('loaderEl'); if(loaderNode) loaderNode.remove();
        btnText.style.display='inline'; loginBtn.disabled = false;
    }
});

// Forgot password / reset flow (use the visible reset card in HTML)
const forgotLink = document.getElementById('forgotLink');
const resetCard = document.getElementById('resetCard');
const loginCard = document.querySelector('.form-card[role="main"]');

if (forgotLink) {
    forgotLink.addEventListener('click', function (e) {
        e.preventDefault();
        openResetCard();
    });
}

function openResetCard() {
    if (loginCard) loginCard.style.display = 'none';
    if (resetCard) {
        resetCard.style.display = 'block';
        const emailInput = document.getElementById('resetEmail');
        if (emailInput) emailInput.focus();
        // ensure sections are in initial state
        document.getElementById('otpSection').style.display = 'none';
        document.getElementById('newPwSection').style.display = 'none';
        document.getElementById('resetMsg').textContent = '';
    }
}

// OTP timer state
let otpTimerInterval = null;
function formatTime(sec) {
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
}
function startOtpTimer(seconds = 180) {
    stopOtpTimer();
    const otpTimerEl = document.getElementById('otpTimer');
    const msgEl = document.getElementById('resetMsg');
    let remaining = seconds;
    if (otpTimerEl) otpTimerEl.textContent = formatTime(remaining);
    otpTimerInterval = setInterval(() => {
        remaining -= 1;
        if (otpTimerEl) otpTimerEl.textContent = formatTime(remaining);
        if (remaining <= 0) {
            // expired
            stopOtpTimer();
            if (otpTimerEl) otpTimerEl.textContent = '00:00';
            if (msgEl) msgEl.innerHTML = '<div class="message error">OTP expired — please resend.</div>';
            // revert to email input so user can resend
            const otpSection = document.getElementById('otpSection'); if (otpSection) otpSection.style.display = 'none';
            const emailSection = document.getElementById('emailSection'); if (emailSection) emailSection.style.display = 'block';
            const emailActions = document.getElementById('emailActions'); if (emailActions) emailActions.style.display = 'block';
            const resetEmail = document.getElementById('resetEmail'); if (resetEmail) resetEmail.focus();
        }
    }, 1000);
}
function stopOtpTimer() {
    if (otpTimerInterval) {
        clearInterval(otpTimerInterval);
        otpTimerInterval = null;
    }
}

// ensure timer is cleared when closing the card
const originalCloseResetCard = closeResetCard;
function closeResetCard() {
    stopOtpTimer();
    // reset timer display
    const otpTimerEl = document.getElementById('otpTimer'); if (otpTimerEl) otpTimerEl.textContent = '03:00';
    originalCloseResetCard();
}

function closeResetCard() {
    if (resetCard) resetCard.style.display = 'none';
    if (loginCard) loginCard.style.display = 'block';
    // clear fields
    const ids = ['resetEmail','resetOtp','newPassword','confirmPassword'];
    ids.forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    const msg = document.getElementById('resetMsg'); if (msg) msg.textContent = '';
}

// wire buttons on the static card
const sendOtpBtn = document.getElementById('sendOtpBtn');
const resetCancelBtnTop = document.getElementById('resetCancelBtnTop');
const otpCancelBtn = document.getElementById('otpCancelBtn');
const newPwCancelBtn = document.getElementById('newPwCancelBtn');

if (resetCancelBtnTop) resetCancelBtnTop.addEventListener('click', (e)=>{ e.preventDefault(); closeResetCard(); });
if (otpCancelBtn) otpCancelBtn.addEventListener('click', (e)=>{ e.preventDefault(); closeResetCard(); });
if (newPwCancelBtn) newPwCancelBtn.addEventListener('click', (e)=>{ e.preventDefault(); closeResetCard(); });

if (sendOtpBtn) {
    sendOtpBtn.addEventListener('click', async function (e) {
        e.preventDefault();
        const email = (document.getElementById('resetEmail') || {}).value || '';
        const msgEl = document.getElementById('resetMsg');
        if (!email) { if (msgEl) msgEl.textContent = 'Please enter your email.'; return; }
        if (msgEl) msgEl.innerHTML = '<span class="loader"></span> Sending OTP...';

        try {
            const res = await fetch('http://localhost:3010/notify/generate/' + encodeURIComponent(email), { method: 'POST' });
            const text = await res.text();
            if (!res.ok) {
                if (msgEl) msgEl.innerHTML = '<div class="message error">' + escapeHtml(text || 'Failed to send OTP') + '</div>';
                return;
            }
            // On success: hide the send/cancel (emailActions) and email input, show only OTP input
            if (msgEl) msgEl.innerHTML = '<div class="message success">OTP sent. Enter it below.</div>';
            const emailActions = document.getElementById('emailActions'); if (emailActions) emailActions.style.display = 'none';
            const emailSection = document.getElementById('emailSection'); if (emailSection) emailSection.style.display = 'none';
            const otpSection = document.getElementById('otpSection'); if (otpSection) otpSection.style.display = 'block';
            const otpInput = document.getElementById('resetOtp'); if (otpInput) otpInput.focus();
            // start countdown (3 minutes)
            startOtpTimer(180);
        } catch (err) {
            console.error(err);
            if (msgEl) msgEl.innerHTML = '<div class="message error">Network error — could not send OTP.</div>';
        }
    });
}

// verify OTP
const verifyOtpBtnEl = document.getElementById('verifyOtpBtn');
if (verifyOtpBtnEl) {
    verifyOtpBtnEl.addEventListener('click', async function (e) {
        e.preventDefault();
        const otp = (document.getElementById('resetOtp') || {}).value || '';
        const email = (document.getElementById('resetEmail') || {}).value || '';
        const msgEl = document.getElementById('resetMsg'); if (msgEl) msgEl.textContent = '';
        if (!otp) { if (msgEl) msgEl.textContent = 'Please enter the OTP.'; return; }
        if (msgEl) msgEl.innerHTML = '<span class="loader"></span> Verifying...';

        try {
            const body = new URLSearchParams(); body.append('otp', otp);
            const res = await fetch('http://localhost:3010/notify/validate/' + encodeURIComponent(email), {
                method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString()
            });
            const text = await res.text();
            if (res.ok) {
                if (msgEl) msgEl.innerHTML = '<div class="message success">' + escapeHtml(text || 'OTP validated') + '</div>';
                // Hide OTP and email sections, show only new-password fields
                const otpSection = document.getElementById('otpSection'); if (otpSection) otpSection.style.display = 'none';
                const emailSection = document.getElementById('emailSection'); if (emailSection) emailSection.style.display = 'none';
                const emailActions = document.getElementById('emailActions'); if (emailActions) emailActions.style.display = 'none';
                // stop timer when validated
                stopOtpTimer();
                document.getElementById('newPwSection').style.display = 'block';
                const pwInput = document.getElementById('newPassword'); if (pwInput) pwInput.focus();
            } else {
                if (msgEl) msgEl.innerHTML = '<div class="message error">' + escapeHtml(text || 'OTP validation failed') + '</div>';
            }
        } catch (err) {
            console.error(err);
            if (msgEl) msgEl.innerHTML = '<div class="message error">Network error while validating OTP.</div>';
        }
    });
}

// submit new password
const submitNewPwBtnEl = document.getElementById('submitNewPwBtn');
if (submitNewPwBtnEl) {
    submitNewPwBtnEl.addEventListener('click', async function (e) {
        e.preventDefault();
        const newPw = (document.getElementById('newPassword') || {}).value || '';
        const confirm = (document.getElementById('confirmPassword') || {}).value || '';
        const email = (document.getElementById('resetEmail') || {}).value || '';
        const msgEl = document.getElementById('resetMsg'); if (msgEl) msgEl.textContent = '';
        if (!newPw || !confirm) { if (msgEl) msgEl.textContent = 'Please enter and confirm your new password.'; return; }
        if (newPw !== confirm) { if (msgEl) msgEl.textContent = 'Passwords do not match.'; return; }
        if (msgEl) msgEl.innerHTML = '<span class="loader"></span> Updating password...';

        try {
            const { ok, text } = await postJSON('http://localhost:1997/auth/password', { email: email, password: newPw });
            if (ok) {
                if (msgEl) msgEl.innerHTML = '<div class="message success">' + escapeHtml(text || 'Password updated successfully') + '</div>';
                setTimeout(() => { window.location.href = '/login.html'; }, 1200);
            } else {
                if (msgEl) msgEl.innerHTML = '<div class="message error">' + escapeHtml(text || 'Failed to update password') + '</div>';
            }
        } catch (err) {
            console.error(err);
            if (msgEl) msgEl.innerHTML = '<div class="message error">Network error while updating password.</div>';
        }
    });
}

// small helper to escape user-provided strings when inserted as HTML
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// enable Enter key to progress through reset flow
const resetEmailInput = document.getElementById('resetEmail');
const resetOtpInput = document.getElementById('resetOtp');
const newPasswordInput = document.getElementById('newPassword');
const confirmPasswordInput = document.getElementById('confirmPassword');
const submitBtn = document.getElementById('submitNewPwBtn');

if (resetEmailInput) {
    resetEmailInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (sendOtpBtn) sendOtpBtn.click();
        }
    });
}
if (resetOtpInput) {
    resetOtpInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (verifyOtpBtnEl) verifyOtpBtnEl.click();
        }
    });
}
if (newPasswordInput) {
    newPasswordInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (submitBtn) submitBtn.click();
        }
    });
}
if (confirmPasswordInput) {
    confirmPasswordInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (submitBtn) submitBtn.click();
        }
    });
}