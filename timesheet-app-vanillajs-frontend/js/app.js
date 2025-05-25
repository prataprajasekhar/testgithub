console.log("Timesheet app.js loaded.");

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const messageDiv = document.getElementById('message');
    const loginSection = document.getElementById('loginSection');
    const loggedInSection = document.getElementById('loggedInSection');
    const welcomeMessage = document.getElementById('welcomeMessage');
    const contentArea = document.getElementById('contentArea');
    const logoutButton = document.getElementById('logoutButton');

    // Role-specific controls
    const roleSpecificControlsDiv = document.getElementById('roleSpecificControls');
    const viewPendingApprovalsButton = document.getElementById('viewPendingApprovalsButton');
    const userManagementButton = document.getElementById('userManagementButton');

    // Timesheet form elements
    const createTimesheetFormSection = document.getElementById('createTimesheetFormSection');
    const createTimesheetForm = document.getElementById('createTimesheetForm');
    const cancelCreateTimesheetButton = document.getElementById('cancelCreateTimesheet');
    const createTimesheetMessageDiv = document.getElementById('createTimesheetMessage');


    function showCreateTimesheetForm() {
        if (createTimesheetFormSection) createTimesheetFormSection.style.display = 'block';
        if (createTimesheetForm) createTimesheetForm.reset(); 
        if (createTimesheetMessageDiv) {
            createTimesheetMessageDiv.textContent = ''; 
            createTimesheetMessageDiv.className = '';
        }
    }

    function hideCreateTimesheetForm() {
        if (createTimesheetFormSection) createTimesheetFormSection.style.display = 'none';
        if (createTimesheetMessageDiv) {
            createTimesheetMessageDiv.textContent = '';
            createTimesheetMessageDiv.className = '';
        }
    }

    async function approveTimesheet(timesheetId) {
        const authToken = localStorage.getItem('authToken');
        if (!authToken) {
            alert("Authentication token not found. Please log in again.");
            showLoginView();
            return;
        }
        try {
            const response = await fetch(`http://localhost:8080/api/manager/timesheets/${timesheetId}/approve`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                },
            });
            if (response.ok) {
                alert('Timesheet approved successfully!'); // Simple feedback
                fetchAndDisplayPendingTimesheets(); // Refresh the list
            } else {
                const errorData = await response.json();
                alert(`Error approving timesheet: ${errorData.message || response.statusText}`);
                if (response.status === 401 || response.status === 403) showLoginView();
            }
        } catch (error) {
            console.error('Approve timesheet error:', error);
            alert('An unexpected error occurred while approving the timesheet.');
        }
    }

    async function rejectTimesheet(timesheetId) {
        const authToken = localStorage.getItem('authToken');
        if (!authToken) {
            alert("Authentication token not found. Please log in again.");
            showLoginView();
            return;
        }
        try {
            const response = await fetch(`http://localhost:8080/api/manager/timesheets/${timesheetId}/reject`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                },
            });
            if (response.ok) {
                alert('Timesheet rejected successfully!'); // Simple feedback
                fetchAndDisplayPendingTimesheets(); // Refresh the list
            } else {
                const errorData = await response.json();
                alert(`Error rejecting timesheet: ${errorData.message || response.statusText}`);
                if (response.status === 401 || response.status === 403) showLoginView();
            }
        } catch (error) {
            console.error('Reject timesheet error:', error);
            alert('An unexpected error occurred while rejecting the timesheet.');
        }
    }
    
    // Event delegation for dynamically created approve/reject buttons
    if (contentArea) {
        contentArea.addEventListener('click', (event) => {
            const target = event.target;
            const timesheetId = target.dataset.timesheetId;

            if (target.classList.contains('approve-pending-button') && timesheetId) {
                approveTimesheet(timesheetId);
            } else if (target.classList.contains('reject-pending-button') && timesheetId) {
                rejectTimesheet(timesheetId);
            }
            // Can add delegation for edit/delete employee timesheets later if needed
        });
    }


    async function fetchAndDisplayPendingTimesheets() {
        if (!contentArea) {
            console.error("Content area not found for pending timesheets.");
            return;
        }
        contentArea.innerHTML = '<h2>Pending Timesheet Approvals</h2><p>Loading pending timesheets...</p>';
        hideCreateTimesheetForm(); // Ensure create form is hidden

        const authToken = localStorage.getItem('authToken');
        if (!authToken) {
            contentArea.innerHTML = '<p class="error">You are not logged in. No token found.</p>';
            showLoginView();
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/api/manager/timesheets/pending', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json',
                },
            });

            if (response.ok) {
                const timesheets = await response.json();
                contentArea.innerHTML = '<h2>Pending Timesheet Approvals</h2>'; // Clear loading message, keep title

                if (timesheets.length === 0) {
                    contentArea.innerHTML += '<p>No timesheets pending approval.</p>';
                    return;
                }

                const table = document.createElement('table');
                table.className = 'timesheets-table pending-approvals-table'; // Add specific class if needed
                const thead = table.createTHead();
                const headerRow = thead.insertRow();
                // Adjusted headers for manager view
                const headers = ['Employee', 'Date', 'Hours Worked', 'Description', 'Actions'];
                headers.forEach(headerText => {
                    const th = document.createElement('th');
                    th.textContent = headerText;
                    headerRow.appendChild(th);
                });

                const tbody = table.createTBody();
                timesheets.forEach(ts => {
                    const row = tbody.insertRow();
                    row.insertCell().textContent = ts.employeeName; // Use employeeName from DTO
                    row.insertCell().textContent = ts.date;
                    row.insertCell().textContent = ts.hoursWorked;
                    row.insertCell().textContent = ts.description || '';
                    
                    const actionsCell = row.insertCell();
                    const approveButton = document.createElement('button');
                    approveButton.textContent = 'Approve';
                    approveButton.className = 'action-button approve-pending-button';
                    approveButton.dataset.timesheetId = ts.id;
                    actionsCell.appendChild(approveButton);

                    const rejectButton = document.createElement('button');
                    rejectButton.textContent = 'Reject';
                    rejectButton.className = 'action-button reject-pending-button';
                    rejectButton.dataset.timesheetId = ts.id;
                    actionsCell.appendChild(rejectButton);
                });
                contentArea.appendChild(table);

            } else {
                const errorText = await response.text();
                contentArea.innerHTML = `<h2>Pending Timesheet Approvals</h2><p class="error">Error fetching pending timesheets: ${response.status} - ${errorText || response.statusText}</p>`;
                if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('username');
                    localStorage.removeItem('role');
                    showLoginView();
                }
            }
        } catch (error) {
            console.error('Error fetching pending timesheets:', error);
            contentArea.innerHTML = `<h2>Pending Timesheet Approvals</h2><p class="error">An unexpected error occurred while fetching pending timesheets.</p>`;
        }
    }


    async function fetchAndDisplayTimesheets() { // Employee's own timesheets
        if (!contentArea) {
            console.error("Content area not found.");
            return;
        }
        contentArea.innerHTML = '<h2>My Timesheets</h2><p>Loading timesheets...</p>'; 
        hideCreateTimesheetForm(); 

        const authToken = localStorage.getItem('authToken');
        if (!authToken) {
            contentArea.innerHTML = '<p>You are not logged in. No token found.</p>';
            showLoginView(); 
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/api/employee/timesheets', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json',
                },
            });

            if (response.ok) {
                const timesheets = await response.json();
                contentArea.innerHTML = '<h2>My Timesheets</h2>'; // Keep title, clear loading

                const createButton = document.createElement('button');
                createButton.id = 'showCreateTimesheetFormButton';
                createButton.textContent = 'Create New Timesheet';
                createButton.addEventListener('click', showCreateTimesheetForm); 
                contentArea.appendChild(createButton);
                
                const spacingDiv = document.createElement('div');
                spacingDiv.style.height = '20px';
                contentArea.appendChild(spacingDiv);

                if (timesheets.length === 0) {
                    const noTimesheetsP = document.createElement('p');
                    noTimesheetsP.textContent = 'No timesheets found.';
                    contentArea.appendChild(noTimesheetsP);
                    return;
                }

                const table = document.createElement('table');
                table.className = 'timesheets-table employee-timesheets-table'; 
                const thead = table.createTHead();
                const headerRow = thead.insertRow();
                const headers = ['Date', 'Hours Worked', 'Description', 'Status', 'Actions'];
                headers.forEach(headerText => {
                    const th = document.createElement('th');
                    th.textContent = headerText;
                    headerRow.appendChild(th);
                });

                const tbody = table.createTBody();
                timesheets.forEach(ts => {
                    const row = tbody.insertRow();
                    row.insertCell().textContent = ts.date;
                    row.insertCell().textContent = ts.hoursWorked;
                    row.insertCell().textContent = ts.description || ''; 
                    row.insertCell().textContent = ts.status;
                    
                    const actionsCell = row.insertCell();
                    if (ts.status === 'PENDING') {
                        const editButton = document.createElement('button');
                        editButton.textContent = 'Edit';
                        editButton.className = 'action-button edit-button';
                        editButton.dataset.timesheetId = ts.id; 
                        actionsCell.appendChild(editButton);

                        const deleteButton = document.createElement('button');
                        deleteButton.textContent = 'Delete';
                        deleteButton.className = 'action-button delete-button';
                        deleteButton.dataset.timesheetId = ts.id;
                        actionsCell.appendChild(deleteButton);
                    } else {
                        actionsCell.textContent = 'N/A';
                    }
                });
                contentArea.appendChild(table);

            } else {
                const errorText = await response.text();
                contentArea.innerHTML = `<h2>My Timesheets</h2><p class="error">Error fetching timesheets: ${response.status} - ${errorText || response.statusText}</p>`;
                if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('username');
                    localStorage.removeItem('role');
                    showLoginView();
                }
            }
        } catch (error) {
            console.error('Error fetching timesheets:', error);
            contentArea.innerHTML = `<h2>My Timesheets</h2><p class="error">An unexpected error occurred while fetching timesheets. Please check the console.</p>`;
        }
    }


    function showLoginView() {
        if (loginSection) loginSection.style.display = 'block';
        if (loggedInSection) loggedInSection.style.display = 'none';
        if (roleSpecificControlsDiv) roleSpecificControlsDiv.style.display = 'none'; 
        if (messageDiv) { 
            messageDiv.textContent = '';
            messageDiv.className = '';
        }
    }

    function showLoggedInView() {
        if (loginSection) loginSection.style.display = 'none';
        if (loggedInSection) loggedInSection.style.display = 'block';
        
        const username = localStorage.getItem('username');
        const role = localStorage.getItem('role');

        if (welcomeMessage && username && role) {
            welcomeMessage.textContent = `Welcome, ${username}! (Role: ${role})`;
        }

        if (roleSpecificControlsDiv && viewPendingApprovalsButton && userManagementButton) {
            roleSpecificControlsDiv.style.display = 'block'; 
            viewPendingApprovalsButton.style.display = 'none'; 
            userManagementButton.style.display = 'none';

            if (role === 'ROLE_MANAGER' || role === 'ROLE_SUPER_ADMIN') {
                viewPendingApprovalsButton.style.display = 'inline-block';
            }
            if (role === 'ROLE_SUPER_ADMIN') {
                userManagementButton.style.display = 'inline-block';
            }
        }
        
        // Default view for all logged-in users is their own timesheets
        fetchAndDisplayTimesheets(); 
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async (event) => {
            event.preventDefault(); 
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            messageDiv.textContent = ''; 
            messageDiv.className = ''; 
            if (!username || !password) {
                messageDiv.textContent = 'Please enter both username and password.';
                messageDiv.className = 'error';
                return;
            }
            try {
                const response = await fetch('http://localhost:8080/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password }),
                });
                const contentType = response.headers.get("content-type");
                let data;
                if (contentType && contentType.indexOf("application/json") !== -1) data = await response.json();
                else data = await response.text(); 
                if (response.ok) {
                    localStorage.setItem('authToken', data.jwtToken);
                    localStorage.setItem('username', data.username);
                    localStorage.setItem('role', data.role);
                    showLoggedInView();
                } else {
                    const errorMessage = (typeof data === 'object' && data.message) ? data.message : (typeof data === 'string' ? data : response.statusText);
                    messageDiv.textContent = 'Login failed: ' + errorMessage;
                    messageDiv.className = 'error';
                }
            } catch (error) {
                console.error('Login error:', error);
                messageDiv.textContent = 'An error occurred during login. Please check the console.';
                messageDiv.className = 'error';
            }
        });
    }

    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
            localStorage.removeItem('role');
            showLoginView();
            if (contentArea) contentArea.innerHTML = ''; 
            hideCreateTimesheetForm(); 
        });
    }

    if (cancelCreateTimesheetButton) {
        cancelCreateTimesheetButton.addEventListener('click', hideCreateTimesheetForm);
    }

    if (createTimesheetForm) {
        createTimesheetForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const date = document.getElementById('timesheetDate').value;
            const hoursWorked = document.getElementById('timesheetHours').value;
            const description = document.getElementById('timesheetDescription').value;
            const authToken = localStorage.getItem('authToken');

            if (createTimesheetMessageDiv) {
                createTimesheetMessageDiv.textContent = '';
                createTimesheetMessageDiv.className = '';
            }

            if (!date || !hoursWorked) {
                if (createTimesheetMessageDiv) {
                    createTimesheetMessageDiv.textContent = 'Date and Hours Worked are required.';
                    createTimesheetMessageDiv.className = 'error';
                }
                return;
            }

            try {
                const response = await fetch('http://localhost:8080/api/employee/timesheets', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${authToken}`,
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ date, hoursWorked: parseFloat(hoursWorked), description }),
                });

                if (response.status === 201) { 
                    hideCreateTimesheetForm();
                    fetchAndDisplayTimesheets(); // Refresh employee's own timesheet list
                    if (messageDiv && loginSection.style.display === 'none') { 
                        messageDiv.textContent = 'Timesheet created successfully!';
                        messageDiv.className = 'success';
                        setTimeout(() => { messageDiv.textContent = ''; messageDiv.className = ''; }, 3000);
                    }
                } else {
                    const errorData = await response.json(); 
                    if (createTimesheetMessageDiv) {
                        createTimesheetMessageDiv.textContent = `Error: ${errorData.message || response.statusText}`;
                        createTimesheetMessageDiv.className = 'error';
                    }
                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem('authToken');
                        localStorage.removeItem('username');
                        localStorage.removeItem('role');
                        showLoginView();
                    }
                }
            } catch (error) {
                console.error('Create timesheet error:', error);
                if (createTimesheetMessageDiv) {
                    createTimesheetMessageDiv.textContent = 'An unexpected error occurred. Please check the console.';
                    createTimesheetMessageDiv.className = 'error';
                }
            }
        });
    }

    // Updated event listener for role-specific buttons
    if (viewPendingApprovalsButton) {
        viewPendingApprovalsButton.addEventListener('click', () => {
            // alert('Manager: View Pending Approvals - Feature coming soon!'); // Remove alert
            contentArea.innerHTML = ''; // Clear content area first
            hideCreateTimesheetForm(); // Hide create form when viewing other content
            fetchAndDisplayPendingTimesheets(); // Call the new function
        });
    }

    if (userManagementButton) {
        userManagementButton.addEventListener('click', () => {
            alert('Admin: User Management - Feature coming soon!');
            contentArea.innerHTML = '<h2>User Management (Coming Soon)</h2>';
            hideCreateTimesheetForm(); 
        });
    }


    // Initial check for existing session
    if (localStorage.getItem('authToken')) {
        showLoggedInView();
    } else {
        showLoginView();
    }
});
