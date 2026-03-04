document.getElementById('togglePassword').addEventListener('click', function(){
    const pw = document.getElementById('password');
    const btn = this;
    if(pw.type === 'password'){ pw.type = 'text'; btn.innerText = 'Hide'; }
    else{ pw.type = 'password'; btn.innerText = 'Show'; }
});

async function postJSON(url, payload){
    const backendUrl = 'http://localhost:1997' + url;
    const res = await fetch(backendUrl, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)});
    const text = await res.text();
    return {ok: res.ok, status: res.status, text};
}

const form = document.getElementById('registerForm');
const btn = document.getElementById('registerBtn');
const btnText = document.getElementById('btnText');
const err = document.getElementById('registerError');
const ok = document.getElementById('registerSuccess');

// Show/hide expertise dropdown based on role
const roleSelect = document.getElementById('role');
const expertiseRow = document.getElementById('expertiseRow');
roleSelect.addEventListener('change', function() {
    if (roleSelect.value === 'Maintenance') {
        expertiseRow.style.display = '';
    } else {
        expertiseRow.style.display = 'none';
    }
});

form.addEventListener('submit', async function(e){
    e.preventDefault(); err.textContent=''; ok.textContent='';
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const role = document.getElementById('role').value;
    const expertise = document.getElementById('expertise').value;
    if(!username || !email || !password || !role){ err.textContent = 'Please fill all fields'; return; }

    btn.disabled = true; btnText.style.display='none';
    const loader = document.createElement('span'); loader.className='loader'; loader.id='loaderEl'; btn.appendChild(loader);

    try{
        const {ok: success, text} = await postJSON('/auth/register', {userName: username, password, email, role});
        if(success){
            ok.textContent = text || 'User registered';
            // If Maintenance, create MaintenanceStaff in request controller
            if(role === 'Maintenance'){
                // POST to /requests/add/staff
                await fetch('http://localhost:1997/requests/add/staff', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: username,
                        expertises: [expertise]
                    })
                });
            }
            setTimeout(()=> window.location.href='login.html',1500);
        }
        else{ err.textContent = text || 'Registration failed'; }
    }catch(e){ err.textContent = 'Network error'; }
    finally{ const l=document.getElementById('loaderEl'); if(l) l.remove(); btnText.style.display='inline'; btn.disabled=false; }
});