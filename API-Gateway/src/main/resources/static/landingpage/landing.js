// Function to navigate to a specific URL
function goTo(url) {
    window.location.href = url;
}

// Function to handle logout functionality
function logout() {
    localStorage.removeItem('jwt');
    window.location.href = '/login.html';
}

// Utility to decode JWT and get payload
function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

// Show/hide cards based on role
function showCardsByRole() {
    const token = localStorage.getItem('jwt');
    const payload = token ? parseJwt(token) : null;
    const role = payload && payload.role ? payload.role.toLowerCase() : '';
    const cards = document.querySelectorAll('.feature-card');
    cards.forEach((card, idx) => {
        if (role === 'staff') {
            card.style.display = '';
        } else if (role === 'student') {
            card.style.display = (idx === 1 || idx === 2) ? '' : 'none';
        } else if (role === 'professor') {
            card.style.display = (idx === 0 || idx === 1 || idx === 2) ? '' : 'none';
        } else {
            card.style.display = 'none'; 
        }
    });
}

document.addEventListener('DOMContentLoaded', showCardsByRole);