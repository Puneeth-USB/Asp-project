// Load unassigned requests and for each request fetch staff matching the request.scope
async function loadUnassignedRequests() {
  try {
    const response = await fetch('http://localhost:1997/requests/unassigned');
    const data = await response.json();
    const tbody = document.getElementById('unassigned-requests-body');
    tbody.innerHTML = '';

    if (!Array.isArray(data) || data.length === 0) {
      const tr = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 6;
      td.textContent = 'No unassigned requests.';
      tr.appendChild(td);
      tbody.appendChild(tr);
      return;
    }

    data.forEach(request => {
      const tr = document.createElement('tr');

      const tdId = document.createElement('td'); tdId.textContent = request.id;
      const tdBookedBy = document.createElement('td'); tdBookedBy.textContent = request.bookedBy || '';
      const tdRoom = document.createElement('td'); tdRoom.textContent = request.roomId || '';
      const tdScope = document.createElement('td'); tdScope.textContent = request.scope || '';
      const tdDesc = document.createElement('td'); tdDesc.textContent = request.description || '';

      const tdAction = document.createElement('td');
      const staffContainer = document.createElement('div');
      staffContainer.id = `staff-list-${request.id}`;
      staffContainer.className = 'staff-list';
      staffContainer.textContent = 'Loading staff...';
      tdAction.appendChild(staffContainer);

      tr.appendChild(tdId);
      tr.appendChild(tdBookedBy);
      tr.appendChild(tdRoom);
      tr.appendChild(tdScope);
      tr.appendChild(tdDesc);
      tr.appendChild(tdAction);

      tbody.appendChild(tr);

      // Fetch staff for this request's expertise/scope
      let expertise = request.scope || 'GENERAL_ROOM';
      // If facility scope is FULL_ROOM, use GENERAL_ROOM when querying staff
      if (expertise === 'FULL_ROOM') expertise = 'GENERAL_ROOM';
      loadStaffForRequest(request.id, expertise);
    });
  } catch (err) {
    alert('Error loading unassigned requests.');
    console.error('Error loading unassigned requests:', err);
  }
}

// Fetch staff by expertise and populate the staff list for a specific request row
async function loadStaffForRequest(requestId, expertise) {
  const container = document.getElementById(`staff-list-${requestId}`);
  if (!container) return;
  container.innerHTML = 'Loading staff...';

  try {
    const res = await fetch(`http://localhost:1997/requests/staff/${encodeURIComponent(expertise)}`);
    if (!res.ok) {
      container.textContent = 'Failed to load staff';
      return;
    }
    const staffList = await res.json();
    container.innerHTML = '';

    if (!Array.isArray(staffList) || staffList.length === 0) {
      container.textContent = 'No staff available for this expertise';
      return;
    }

    // Create select dropdown and assign button
    const select = document.createElement('select');
    select.id = `staff-select-${requestId}`;
    select.className = 'staff-select';
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Select staff';
    placeholder.disabled = true;
    placeholder.selected = true;
    select.appendChild(placeholder);

    staffList.forEach(staff => {
      const opt = document.createElement('option');
      opt.value = staff.id;
      opt.textContent = `${staff.name} (${staff.id})${staff.expertises ? ' — ' + (Array.isArray(staff.expertises) ? staff.expertises.join(', ') : staff.expertises) : ''}`;
      select.appendChild(opt);
    });

    const assignBtn = document.createElement('button');
    assignBtn.className = 'assign-btn';
    assignBtn.textContent = 'Assign';
    assignBtn.disabled = true;

    // Enable button when a staff is selected
    select.addEventListener('change', () => {
      assignBtn.disabled = !select.value;
    });

    assignBtn.addEventListener('click', () => {
      const staffId = select.value;
      if (!staffId) return alert('Please select a staff member to assign.');
      assignStaff(requestId, staffId, assignBtn);
    });

    const wrapper = document.createElement('div');
    wrapper.className = 'staff-dropdown-wrapper';
    wrapper.appendChild(select);
    wrapper.appendChild(assignBtn);

    container.appendChild(wrapper);
  } catch (err) {
    console.error('Error fetching staff for expertise', expertise, err);
    container.textContent = 'Error loading staff';
  }
}

// Assign a staff member to a request using the existing PUT endpoint
async function assignStaff(requestId, staffId, buttonNode) {
  if (!confirm(`Assign staff ID ${staffId} to request ${requestId}?`)) return;

  try {
    if (buttonNode) buttonNode.disabled = true;
    const response = await fetch(`http://localhost:1997/requests/${requestId}/assign/${staffId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' }
    });

    if (response.ok) {
      alert('Staff assigned successfully!');
      // Remove the request row from the table immediately
      const staffContainer = document.getElementById(`staff-list-${requestId}`);
      if (staffContainer) {
        const tr = staffContainer.closest('tr');
        if (tr) tr.remove();
      } else {
        // Fallback: refresh the entire list
        loadUnassignedRequests();
      }
    } else {
      const txt = await response.text().catch(() => null);
      alert(txt || 'Failed to assign staff.');
      if (buttonNode) buttonNode.disabled = false;
    }
  } catch (err) {
    console.error('Error assigning staff:', err);
    alert('Error assigning staff.');
    if (buttonNode) buttonNode.disabled = false;
  }
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
  loadUnassignedRequests();
});
