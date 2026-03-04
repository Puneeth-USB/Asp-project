document.addEventListener('DOMContentLoaded', function() {
  // Get staffName from JWT token in localStorage
  let staffName = null;
  const token = localStorage.getItem('jwt');
  if (token) {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      staffName = decoded.name;
    } catch (err) {
      staffName = '';
    }
  }

  // Fetch in-progress requests (left panel)
  fetch(`http://localhost:1997/requests/staff/${encodeURIComponent(staffName)}/in-progress`)
    .then(res => res.json())
    .then(requests => {
      const tbody = document.getElementById('assigned-requests-body');
      tbody.innerHTML = '';
      if (!Array.isArray(requests) || requests.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8">No assigned requests</td></tr>';
        return;
      }
      requests.forEach(req => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${req.id}</td>
          <td>${req.bookedBy ?? ''}</td>
          <td>${req.roomId ?? ''}</td>
          <td>${req.scope ?? ''}</td>
          <td>${req.description ?? ''}</td>
          <td>${req.status ?? ''}</td>
          <td>${req.createdAt ? new Date(req.createdAt).toLocaleDateString() : ''}</td>
          <td><button class="mark-complete" data-id="${req.id}">Mark Complete</button></td>
        `;
        tbody.appendChild(tr);
      });
    })
    .catch(() => {
      const tbody = document.getElementById('assigned-requests-body');
      tbody.innerHTML = '<tr><td colspan="8">Failed to load requests</td></tr>';
    });

  // Fetch completed requests (right panel)
  fetch(`http://localhost:1997/requests/staff/${encodeURIComponent(staffName)}/completed`)
    .then(res => res.json())
    .then(requests => {
      const tbody = document.getElementById('completed-requests-body');
      tbody.innerHTML = '';
      if (!Array.isArray(requests) || requests.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">No completed requests</td></tr>';
        return;
      }
      requests.forEach(req => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${req.id}</td>
          <td>${req.roomId ?? ''}</td>
          <td>${req.status ?? ''}</td>
          <td>${req.completedAt ? new Date(req.completedAt).toLocaleDateString() : ''}</td>
        `;
        tbody.appendChild(tr);
      });
    })
    .catch(() => {
      const tbody = document.getElementById('completed-requests-body');
      tbody.innerHTML = '<tr><td colspan="4">Failed to load completed requests</td></tr>';
    });

  // Mark Complete button handler only
  document.addEventListener('click', function(e) {
    if (e.target.classList.contains('mark-complete')) {
      const requestId = e.target.getAttribute('data-id');
      fetch(`http://localhost:1997/requests/${requestId}/complete`, {
        method: 'PUT'
      })
      .then(res => {
        if (res.ok) {
          alert('Request marked as completed!');
          location.reload();
        } else {
          alert('Failed to mark request as completed.');
        }
      })
      .catch(() => {
        alert('Network error. Please try again.');
      });
    }
  });
});