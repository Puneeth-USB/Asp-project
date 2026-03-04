// ======================= CONFIG =======================

// Request Service base URL (from @RequestMapping("/requests"))
const BASE_URL = "http://localhost:1997/requests";

// ======================= JWT HELPERS ==================

// Get the user's display name used as `bookedBy` and for /by-name/{name}
function getUserNameFromJWT() {
  const token = localStorage.getItem("jwt");
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));

    // From your sample payload:
    // {
    //   "role": "Professor",
    //   "name": "Sam Ramanujan",
    //   "id": 4,
    //   "email": "sam@gmail.com",
    //   ...
    // }
    return payload.name || null;
  } catch (e) {
    console.error("Failed to decode JWT:", e);
    return null;
  }
}

// Helper to decode JWT and get normalized role
function getRoleFromJWT() {
  const token = localStorage.getItem("jwt");
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const roleRaw = payload.role;
    return roleRaw ? roleRaw.toString().toLowerCase() : null;
  } catch (e) {
    return null;
  }
}

// Decide which roles count as "staff" for showing extra columns, etc.
function isStaffRole(role) {
  if (!role) return false;
  const staffRoles = ["staff", "maintenance", "admin", "manager", "warden"];
  return staffRoles.includes(role);
}

// ======================= STATE ========================

// We cache requests so we can build a full DTO when updating description
let cachedRequests = [];

// ======================= API CALLS ====================

// Fetch requests for the logged-in user by name or all requests for staff
async function fetchRequestsForCurrentUser() {
  const role = getRoleFromJWT();
  if (role === 'staff') {
    // Staff: fetch all requests
    try {
      const url = `${BASE_URL}`;
      const response = await fetch(url);
      if (!response.ok) {
        console.error("API error while fetching all requests:", response.status, response.statusText);
        return [];
      }
      const data = await response.json();
      console.log("Fetched all requests (staff):", data);
      return data;
    } catch (err) {
      console.error("Fetch error:", err);
      return [];
    }
  } else {
    // Non-staff: fetch only their own requests
    const name = getUserNameFromJWT();
    if (!name) {
      console.error("No user name found in JWT; cannot fetch requests.");
      return [];
    }
    try {
      const url = `${BASE_URL}/by-name/${encodeURIComponent(name)}`;
      const response = await fetch(url);
      if (!response.ok) {
        console.error("API error while fetching requests:", response.status, response.statusText);
        return [];
      }
      const data = await response.json();
      console.log("Fetched requests:", data);
      return data;
    } catch (err) {
      console.error("Fetch error:", err);
      return [];
    }
  }
}

// ======================= HELPERS ======================

// Nice display for FacilityScope enum, e.g. "ELECTRICAL_OUTAGE" -> "Electrical outage"
function formatScope(scope) {
  if (!scope) return "";
  return scope
    .toString()
    .split("_")
    .map(part => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

// ======================= UI POPULATION ================

async function populateRequests() {
  const role = getRoleFromJWT();
  const staffView = isStaffRole(role);

  // Right now, backend only exposes /by-name/{name},
  // so we fetch "my requests" for all users, including staff.
  const requests = await fetchRequestsForCurrentUser();
  cachedRequests = requests || [];

  // Left panel table
  const tableBody = document.getElementById("request-table-body");
  tableBody.innerHTML = "";

  // If staff, show requester name column by adding header dynamically
  if (staffView) {
    const tableHeadRow = document.querySelector("#request-table thead tr");
    if (
      tableHeadRow &&
      !Array.from(tableHeadRow.children).find(th => th.textContent === "Requester Name")
    ) {
      const th = document.createElement("th");
      th.textContent = "Requester Name";
      tableHeadRow.appendChild(th);
    }
  }

  // Fill table rows
  cachedRequests.forEach(req => {
    const tr = document.createElement("tr");

    // MaintenanceRequest is assumed to have:
    // id, roomId, description, status, assignedStaffId, bookedBy, scope, priority, createdDate, etc.
    let rowHtml = `
      <td>${req.id}</td>
      <td>${req.roomId ?? ""}</td>
      <td>${formatScope(req.scope)}</td>
      <td>${req.description ?? ""}</td>
      <td>${req.status ?? ""}</td>
      <td>${req.assignedStaffId != null ? req.assignedStaffId : "None"}</td>
      <td>${req.createdAt ? new Date(req.createdAt).toLocaleDateString() : ""}</td>
    `;

    if (staffView) {
      rowHtml += `<td>${req.bookedBy ?? ""}</td>`;
    }

    tr.innerHTML = rowHtml;
    tableBody.appendChild(tr);
  });

  // Right panel dropdown – list request IDs
  const dropdown = document.getElementById("request-dropdown");
  dropdown.innerHTML = "";
  cachedRequests.forEach(req => {
    const option = document.createElement("option");
    option.value = req.id;
    option.textContent = req.id;
    dropdown.appendChild(option);
  });
}

// ======================= UPDATE ISSUE =================

function setupUpdateHandler() {
  document.getElementById("update-issue-btn").onclick = async () => {
    const selectedId = document.getElementById("request-dropdown").value;
    const newIssue = document.getElementById("issue-input").value.trim();
    const statusDiv = document.getElementById("update-status");

    if (!selectedId) {
      statusDiv.textContent = "Please select a request.";
      return;
    }

    if (!newIssue) {
      statusDiv.textContent = "Please enter a new issue description.";
      return;
    }

    // Find the original request so we can build a full MaintenanceRequestDTO
    const original = cachedRequests.find(r => String(r.id) === String(selectedId));
    if (!original) {
      statusDiv.textContent = "Could not find the selected request in memory.";
      return;
    }

    // Build DTO expected by controller's @PutMapping("/{id}")
    // record MaintenanceRequestDTO(
    //   Long roomId,
    //   String bookedBy,
    //   FacilityScope scope,
    //   PriorityLevel priority,
    //   String description
    // )
    const dto = {
      roomId: original.roomId,
      bookedBy: original.bookedBy,
      scope: original.scope,
      priority: original.priority,
      description: newIssue
    };

    try {
      const url = `${BASE_URL}/${encodeURIComponent(selectedId)}`;
      const response = await fetch(url, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(dto)
      });

      if (response.ok) {
        statusDiv.textContent = "Issue updated successfully.";
        alert("Request updated successfully!");
        // Reload to show updated description
        window.location.reload();
      } else {
        console.error("Update failed with status:", response.status);
        statusDiv.textContent = "Failed to update issue.";
      }
    } catch (err) {
      console.error("Error updating issue:", err);
      statusDiv.textContent = "Error updating issue.";
    }
  };
}

// ======================= INIT =========================

window.onload = () => {
  populateRequests();
  setupUpdateHandler();
};
