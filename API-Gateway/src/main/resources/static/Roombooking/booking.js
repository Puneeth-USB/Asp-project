// Base URL for your RoomController (it has @RequestMapping("/api"))
const API_BASE = 'http://localhost:1997/api/rooms';

// ---------------- INIT ----------------
document.addEventListener('DOMContentLoaded', () => {
  const bookBtn = document.getElementById('book-room-btn');
  if (bookBtn) {
    bookBtn.addEventListener('click', handleBooking);
  }

  fetchMyBookedRooms();
  fetchMyPendingRequests();
});

// ---------------- JWT helper ----------------
// This ONLY decodes the JWT to get the username.
// It does NOT encrypt or hash anything.
function getUsernameFromToken() {
  const token = localStorage.getItem('jwt');
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // Use plain text username from token
    return payload.name || payload.username || payload.sub || null;
  } catch (e) {
    console.error('Invalid JWT token', e);
    return null;
  }
}

// ---------------- Book a room ----------------
async function handleBooking() {
  const bookingMessage = document.getElementById('booking-status');

  const capacityInput = document.getElementById('capacity-input');
  const projectorSelect = document.getElementById('projector-select');
  const avSelect = document.getElementById('av-select');
  const dateInput = document.getElementById('date-input');
  const slotSelect = document.getElementById('slot-select');

  const username = getUsernameFromToken();

  const capacity = parseInt(capacityInput.value, 10);
  const needsProjector = projectorSelect.value === 'yes';
  const needsAvSystem = avSelect.value === 'yes';
  const date = dateInput.value;   // yyyy-MM-dd
  const slot = slotSelect.value;  // MORNING / AFTERNOON / EVENING

  if (!username) {
    bookingMessage.textContent = 'You must be logged in to book a room.';
    bookingMessage.style.color = 'red';
    return;
  }

  if (!capacity || !date || !slot) {
    bookingMessage.textContent = 'Please select capacity, date and slot.';
    bookingMessage.style.color = 'red';
    return;
  }

  // Username is sent as plain text here
  const requestBody = {
    date,
    slot,
    capacity,
    needsProjector,
    needsAvSystem,
    bookedBy: username
  };

  try {
    const response = await fetch(`${API_BASE}/bookings`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });

    const data = await response.json().catch(() => null);

    if (response.ok) {
      // Expected shape:
      // {
      //   bookingId: 3,
      //   roomId: 106,
      //   status: "CONFIRMED",
      //   message: "Room allocated with optimal capacity."
      // }
      const parts = [];

      if (data && data.message) {
        parts.push(data.message);
      } else {
        parts.push('Booking request submitted.');
      }

      if (data && data.roomId != null) {
        parts.push(`Room number: ${data.roomId}`);
      }

      if (data && data.status) {
        parts.push(`(${data.status})`);
      }

      bookingMessage.textContent = parts.join(' ');
      bookingMessage.style.color = 'green';

      // Refresh UI
      fetchMyBookedRooms();
      fetchMyPendingRequests();
    } else {
      bookingMessage.textContent =
        (data && data.message) || 'Booking failed.';
      bookingMessage.style.color = 'red';
    }
  } catch (error) {
    console.error('Error booking room:', error);
    bookingMessage.textContent = 'Error booking room.';
    bookingMessage.style.color = 'red';
  }
}

// ---------------- My booked rooms (left panel) ----------------
async function fetchMyBookedRooms() {
  const username = getUsernameFromToken();
  const tableBody = document.getElementById('myRoomsTableBody');
  if (!tableBody) return;

  tableBody.innerHTML = '';

  if (!username) {
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = 4;
    td.textContent = 'Please log in to see your bookings.';
    tr.appendChild(td);
    tableBody.appendChild(tr);
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE}/booked-by?name=${encodeURIComponent(username)}`
    );

    if (!response.ok) {
      throw new Error('Failed to load booked rooms');
    }

    // Backend JSON:
    // [
    //   {
    //     id: 106,
    //     capacity: 10,
    //     hasProjector: true,
    //     hasAvSystem: true,
    //     status: "AVAILABLE",
    //     bookings: [
    //       { id: 1, date: "2025-12-01", slot: "MORNING", bookedBy: "Sam ..." },
    //       { id: 3, date: "2025-12-02", slot: "MORNING", bookedBy: "Nikhil", status: "CANCELLED" }
    //     ]
    //   },
    //   ...
    // ]
    const rooms = await response.json();

    const myBookings = [];

    rooms.forEach((room) => {
      const roomNumber = room.id;
      const roomCapacity = room.capacity;
      const roomBookings = room.bookings || [];

      roomBookings.forEach((booking) => {
        // Only show this user's non-cancelled bookings
        if (
          booking.bookedBy === username &&
          booking.status !== 'CANCELLED'
        ) {
          myBookings.push({
            bookingId: booking.id,
            roomNumber: roomNumber,
            date: booking.date,
            slot: booking.slot,
            capacity: booking.requiredCapacity ?? roomCapacity,
            needsProjector: booking.needsProjector,
            needsAvSystem: booking.needsAvSystem,
            // include server flag indicating if booking has already been extended once
            extendOnce: booking.extendOnce ?? booking.extendedOnce ?? false
          });
        }
      });
    });

    if (myBookings.length === 0) {
      const tr = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 4;
      td.textContent = 'No rooms currently booked.';
      tr.appendChild(td);
      tableBody.appendChild(tr);
      return;
    }

    myBookings.forEach((b) => {
      const trMain = document.createElement('tr');
      trMain.classList.add('booking-main-row');

      const tdRoom = document.createElement('td');
      tdRoom.textContent =
        b.roomNumber != null ? b.roomNumber : '-';

      const tdDate = document.createElement('td');
      tdDate.textContent = b.date || '-';

      const tdSlot = document.createElement('td');
      tdSlot.textContent = b.slot || '-';

      const tdAction = document.createElement('td');
      if (b.bookingId != null) {
        const releaseBtn = document.createElement('button');
        releaseBtn.classList.add('release-btn');
        releaseBtn.textContent = 'Release Room';
        releaseBtn.addEventListener('click', () => releaseBooking(b.bookingId));
        tdAction.appendChild(releaseBtn);

        // Extend button: calls the PUT /bookings/{bookingId}/extend endpoint
        const extendBtn = document.createElement('button');
        // Use the same visual class as the release button so both look identical
        extendBtn.classList.add('release-btn', 'extend-btn');
        extendBtn.textContent = 'Extend Once';
        extendBtn.addEventListener('click', () => extendBooking(b.bookingId));
        // disable if server indicates it was already extended once
        if (b.extendOnce) {
          extendBtn.disabled = true;
          extendBtn.title = 'Already extended once';
        }
        tdAction.appendChild(extendBtn);
      } else {
        tdAction.textContent = '-';
      }

      trMain.appendChild(tdRoom);
      trMain.appendChild(tdDate);
      trMain.appendChild(tdSlot);
      trMain.appendChild(tdAction);

      const trDetails = document.createElement('tr');
      trDetails.classList.add('booking-details-row');

      const tdDetails = document.createElement('td');
      tdDetails.colSpan = 4;
      tdDetails.classList.add('booking-details-cell');

      const projectorText = b.needsProjector ? 'Yes' : 'No';
      const avText = b.needsAvSystem ? 'Yes' : 'No';

      tdDetails.textContent = `Capacity: ${
        b.capacity != null ? b.capacity : '-'
      } | Projector: ${projectorText} | AV Conference: ${avText}`;

      trDetails.appendChild(tdDetails);

      tableBody.appendChild(trMain);
      tableBody.appendChild(trDetails);
    });
  } catch (error) {
    console.error('Error fetching booked rooms:', error);
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = 4;
    td.textContent = 'Error loading booked rooms.';
    tr.appendChild(td);
    tableBody.appendChild(tr);
  }
}

// ---------------- Release booking ----------------
async function releaseBooking(bookingId) {
  const bookingMessage = document.getElementById('booking-status');

  try {
    const response = await fetch(
      `${API_BASE}/bookings/${bookingId}/release`,
      {
        method: 'POST'
      }
    );

    const data = await response.json().catch(() => null);

    if (response.ok) {
      bookingMessage.textContent =
        (data && data.message) || 'Room released successfully.';
      bookingMessage.style.color = 'green';

      fetchMyBookedRooms();
      fetchMyPendingRequests();
    } else {
      bookingMessage.textContent =
        (data && data.message) || 'Failed to release room.';
      bookingMessage.style.color = 'red';
    }
  } catch (error) {
    console.error('Error releasing room:', error);
    bookingMessage.textContent = 'Error releasing room.';
    bookingMessage.style.color = 'red';
  }
}

// New: Extend booking by one hour
async function extendBooking(bookingId) {
  const bookingMessage = document.getElementById('booking-status');

  try {
    const response = await fetch(
      `${API_BASE}/bookings/${bookingId}/extend`,
      {
        method: 'PUT'
      }
    );

    const data = await response.json().catch(() => null);

    if (response.ok) {
      bookingMessage.textContent =
        (data && data.message) || 'Booking extended by one hour.';
      bookingMessage.style.color = 'green';

      fetchMyBookedRooms();
      fetchMyPendingRequests();
    } else {
      bookingMessage.textContent =
        (data && data.message) || 'Failed to extend booking.';
      bookingMessage.style.color = 'red';
    }
  } catch (error) {
    console.error('Error extending booking:', error);
    bookingMessage.textContent = 'Error extending booking.';
    bookingMessage.style.color = 'red';
  }
}

// ---------------- Pending requests (right panel) ----------------
async function fetchMyPendingRequests() {
  const username = getUsernameFromToken();
  const container = document.getElementById('pending-requests-container');
  const listEl = document.getElementById('pending-requests-list');

  if (!container || !listEl) return;

  listEl.innerHTML = '';

  if (!username) {
    container.style.display = 'none';
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE}/pending-requests?name=${encodeURIComponent(username)}`
    );

    if (!response.ok) {
      throw new Error('Failed to load pending requests');
    }

    const pending = await response.json();

    if (!pending || pending.length === 0) {
      container.style.display = 'none';
      return;
    }

    container.style.display = 'block';

    pending.forEach((req) => {
      const card = document.createElement('div');
      card.classList.add('pending-request-card');

      const date = req.date ?? '';
      const slot = req.slot ?? '';
      const capacity =
        req.requiredCapacity ?? req.capacity ?? null;
      const needsProjector =
        req.needsProjector ?? false;
      const needsAv =
        req.needsAvSystem ?? false;

      const projectorText = needsProjector ? 'Yes' : 'No';
      const avText = needsAv ? 'Yes' : 'No';

      card.innerHTML = `
        <div class="pending-main-line">
          <span class="pending-date-slot">${date || '-'} | ${slot || '-'}</span>
        </div>
        <div class="pending-details-line">
          Capacity: ${capacity != null ? capacity : '-'} |
          Projector: ${projectorText} |
          AV Conference: ${avText}
        </div>
      `;

      listEl.appendChild(card);
    });
  } catch (error) {
    console.error('Error fetching pending requests:', error);
    container.style.display = 'none';
  }
}

