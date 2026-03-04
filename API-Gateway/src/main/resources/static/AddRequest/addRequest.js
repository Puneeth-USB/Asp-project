document.addEventListener('DOMContentLoaded', function() {
    // ===== JWT HELPERS =====
    function getTokenPayload() {
        const token = localStorage.getItem('jwt');
        if (!token || token.split('.').length !== 3) {
            console.warn('No valid JWT token found in localStorage under key "jwt".');
            return null;
        }
        try {
            const base64 = token.split('.')[1]
                .replace(/-/g, '+')
                .replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            return JSON.parse(jsonPayload);
        } catch (e) {
            console.error('Error decoding token payload:', e);
            return null;
        }
    }

    function extractUsername(payload) {
        if (!payload) return null;
        // Try common fields; adjust if your token uses a custom claim
        return payload.username || payload.name || payload.sub || payload.email || null;
    }

    const payload = getTokenPayload();
    const username = extractUsername(payload);

    const bookedByInput = document.getElementById('bookedBy');
    if (username) {
        bookedByInput.value = username;
    } else {
        bookedByInput.value = 'Unknown user';
    }

    // ===== ROOMS DROPDOWN (rooms booked by user) =====
    const roomSelect = document.getElementById('roomSelect');

    async function loadUserRooms() {
        roomSelect.innerHTML = '';
        const placeholder = document.createElement('option');
        placeholder.disabled = true;
        placeholder.selected = true;
        placeholder.textContent = username
            ? 'Select a room you have booked'
            : 'Login required to see your rooms';
        placeholder.value = '';
        roomSelect.appendChild(placeholder);

        if (!username) {
            roomSelect.disabled = true;
            return;
        }

        let roomsEndpoint;
        if (payload && (payload.role === 'staff' || payload.role === 'Staff')) {
            // Staff can see all rooms
            roomsEndpoint = 'http://localhost:1997/api/rooms/available';
        } else {
            // Regular user: only rooms booked by them
            roomsEndpoint = `http://localhost:1997/api/rooms/booked-by?name=${encodeURIComponent(username)}`;
        }

        try {
            const res = await fetch(roomsEndpoint);
            if (!res.ok) {
                console.error('Failed to fetch rooms, status:', res.status);
                return;
            }

            const rooms = await res.json();

            // Clear placeholder and repopulate
            roomSelect.innerHTML = '';
            if (!rooms || rooms.length === 0) {
                const noOpt = document.createElement('option');
                noOpt.disabled = true;
                noOpt.selected = true;
                noOpt.textContent = 'No rooms found for your account';
                noOpt.value = '';
                roomSelect.appendChild(noOpt);
                roomSelect.disabled = true;
                return;
            }

            rooms.forEach(room => {
                // Try multiple possible property names
                const id =
                    room.id ||
                    room.roomId ||
                    room.RoomId ||
                    room.RoomID ||
                    room.roomNumber ||
                    room.RoomNumber;
                const number =
                    room.roomNumber ||
                    room.RoomNumber ||
                    id;

                const opt = document.createElement('option');
                opt.value = id;
                opt.textContent = number;
                roomSelect.appendChild(opt);
            });
        } catch (err) {
            console.error('Error fetching rooms:', err);
        }
    }

    loadUserRooms();

    // ===== PANEL TOGGLING (same interaction, just simplified) =====
    const leftPanel = document.getElementById('leftPanel');
    const rightPanel = document.getElementById('rightPanel');
    const divider = document.getElementById('divider');
    const showServiceFormBtn = document.getElementById('showServiceFormBtn');
    const goBackBtn = document.getElementById('goBackBtn');

    showServiceFormBtn.addEventListener('click', function() {
        leftPanel.style.display = 'none';
        rightPanel.style.display = 'flex';
        divider.style.display = 'block';
        showServiceFormBtn.style.display = 'none';
    });

    goBackBtn.addEventListener('click', function() {
        leftPanel.style.display = 'flex';
        rightPanel.style.display = 'none';
        divider.style.display = 'none';
        showServiceFormBtn.style.display = 'block';
    });

    // ===== AI TROUBLESHOOTING FORM (unchanged) =====
    const aiForm = document.getElementById('aiForm');
    const aiQuestion = document.getElementById('aiQuestion');
    const aiResponse = document.getElementById('aiResponse');

    aiForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        aiResponse.textContent = 'Loading...';
        try {
            const res = await fetch(
                `http://localhost:1997/api/ai/generate?message=${encodeURIComponent(aiQuestion.value)}`
            );
            const data = await res.json();
            aiResponse.innerHTML =
                `<div style="background:#f1f5f9;padding:12px;border-radius:8px;border:1px solid #2563eb;color:#0f172a;font-size:16px;white-space:pre-line;">` +
                `${data.generation || 'No response from AI.'}</div>`;
        } catch (err) {
            console.error(err);
            aiResponse.textContent = 'Error contacting AI service.';
        }
    });

    // ===== MAINTENANCE REQUEST FORM =====
    const serviceForm = document.getElementById('serviceForm');
    const scopeSelect = document.getElementById('scope');
    const prioritySelect = document.getElementById('priority');
    const description = document.getElementById('description');
    const serviceResponse = document.getElementById('serviceResponse');

    serviceForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        if (!username) {
            serviceResponse.textContent = 'Unable to submit: user not identified from token.';
            return;
        }

        if (!roomSelect.value) {
            serviceResponse.textContent = 'Please select a room.';
            return;
        }

        const dto = {
            roomId: Number(roomSelect.value),         // MaintenanceRequestDTO.roomId (Long)
            bookedBy: username,                       // MaintenanceRequestDTO.bookedBy
            scope: scopeSelect.value,                 // FacilityScope enum
            priority: prioritySelect.value,           // PriorityLevel enum
            description: description.value.trim()     // Description text
        };

        serviceResponse.textContent = 'Submitting...';

        try {
            const res = await fetch('http://localhost:1997/requests', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dto)
            });

            const data = await res.json().catch(() => null);

            if (res.ok) {
                const roomLabel = roomSelect.options[roomSelect.selectedIndex]?.textContent || roomSelect.value;
                serviceResponse.textContent = `Maintenance request submitted for Room ${roomLabel}.`;
                serviceForm.reset();
                bookedByInput.value = username; // reset will clear this, so set it back
                loadUserRooms(); // reload in case bookings changed
            } else {
                serviceResponse.textContent =
                    'Error submitting request: ' +
                    (typeof data === 'string'
                        ? data
                        : JSON.stringify(data || { status: res.status }));
            }
        } catch (err) {
            console.error(err);
            serviceResponse.textContent = 'Error contacting maintenance service.';
        }
    });
});
