// Database Viewer Module for Smart City ANPR Admin Panel

// DOM Elements
const dateFilter = document.getElementById('dateFilter');
const plateFilter = document.getElementById('plateFilter');
const applyFilterBtn = document.getElementById('applyFilterBtn');
const resetFilterBtn = document.getElementById('resetFilterBtn');
const databaseTable = document.getElementById('databaseTable');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');
const paginationInfo = document.getElementById('paginationInfo');

// Pagination state
const pageSize = 10;
let currentPage = 1;
let totalPages = 1;
let lastDoc = null;
let firstDoc = null;
let activeFilters = {};

// Initialize Database Viewer
function initDatabaseViewer() {
    console.log('Database Viewer initialized');
    
    // Set today's date as default for date filter
    const dateFilter = document.getElementById('dateFilter');
    if (dateFilter) {
        const today = new Date().toISOString().split('T')[0];
        dateFilter.value = today;
    }
    
    // Add event listeners for filter buttons
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    const resetFilterBtn = document.getElementById('resetFilterBtn');
    
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', function() {
            // Get filter values
            const dateFilter = document.getElementById('dateFilter').value;
            const plateFilter = document.getElementById('plateFilter').value;
            
            // Apply filters
            console.log("Applying filters:", { date: dateFilter, plate: plateFilter });
            // TODO: Implement actual filtering
            applyFilters(dateFilter, plateFilter);
        });
    }
    
    if (resetFilterBtn) {
        resetFilterBtn.addEventListener('click', function() {
            // Reset filter inputs
            document.getElementById('dateFilter').value = '';
            document.getElementById('plateFilter').value = '';
            
            console.log("Filters reset");
            // Reload original data
            loadPlateData();
        });
    }
    
    // Initialize pagination
    const prevPageBtn = document.getElementById('prevPageBtn');
    const nextPageBtn = document.getElementById('nextPageBtn');
    
    if (prevPageBtn) {
        prevPageBtn.addEventListener('click', function() {
            console.log("Previous page");
            // TODO: Implement pagination
        });
    }
    
    if (nextPageBtn) {
        nextPageBtn.addEventListener('click', function() {
            console.log("Next page");
            // TODO: Implement pagination
        });
    }
    
    // Load initial data
    loadPlateData();
}

function applyFilters(dateStr, plateNumber) {
    // Implementation for filtering data
    console.log(`Filtering data by date: ${dateStr} and plate: ${plateNumber}`);
    
    // TODO: Implement filtering logic
    loadPlateData(dateStr, plateNumber);
}

async function loadPlateData(dateFilter = '', plateFilter = '') {
    const db = firebase.firestore();
    let plateDataRef = db.collection("Orders").orderBy("entryTime", "desc");
    let plateRecogRef = db.collection("PlateRecognition").orderBy("timestamp", "desc");
    
    // Apply filters if provided
    if (plateFilter) {
        plateDataRef = plateDataRef.where("licensePlate", "==", plateFilter);
        plateRecogRef = plateRecogRef.where("plate_number", "==", plateFilter);
    }
    
    // Date filtering would need to be implemented with additional logic
    // as it requires timestamp comparison
    
    // Set loading state
    const tableBody = document.getElementById('plateDataBody');
    tableBody.innerHTML = '<tr><td colspan="8" class="text-center">Loading data...</td></tr>';
    
    try {
        // Get data from both collections
        const [orderSnapshot, plateRecogSnapshot] = await Promise.all([
            plateDataRef.get(),
            plateRecogRef.get()
        ]);
        
        // Get all user information
        const userSnapshot = await db.collection('Users').get();
        const userMap = {};
        
        // Create mapping from user ID to user name
        userSnapshot.forEach(doc => {
            const userData = doc.data();
            userMap[doc.id] = userData.displayName || userData.name || userData.email || doc.id;
        });
        
        const orders = [];
        orderSnapshot.forEach(doc => {
            const data = doc.data();
            // Add document ID to the data
            data.id = doc.id;
            // Add source information
            data.source = "order";
            // Add user name if available
            if (data.userID && userMap[data.userID]) {
                data.userName = userMap[data.userID];
            } else {
                data.userName = data.userID ? 'Unknown' : 'System';
            }
            orders.push(data);
        });
        
        const plateRecogs = [];
        plateRecogSnapshot.forEach(doc => {
            const data = doc.data();
            // Add document ID to the data
            data.id = doc.id;
            // Add source information
            data.source = "plateRecognition";
            // Make field names consistent with orders
            if (data.plate_number) data.licensePlate = data.plate_number;
            if (data.timestamp) data.entryTime = data.timestamp;
            if (data.country_identifier) data.countryIdentifier = data.country_identifier;
            // Add user name if available
            if (data.userId) {
                if (userMap[data.userId]) {
                    data.userName = userMap[data.userId];
                    data.userID = data.userId; // Make userId consistent
                } else {
                    data.userName = 'Unknown';
                    data.userID = data.userId;
                }
            } else {
                data.userName = 'System';
            }
            // Keep the exit_after_seconds field
            data.exit_after_seconds = data.exit_after_seconds || 0;
            plateRecogs.push(data);
        });
        
        // Combine data from both collections
        let combinedData = [...orders, ...plateRecogs];
        
        // Apply date filter if provided
        if (dateFilter) {
            const filterDate = new Date(dateFilter);
            const startOfDay = new Date(filterDate);
            startOfDay.setHours(0, 0, 0, 0);
            const endOfDay = new Date(filterDate);
            endOfDay.setHours(23, 59, 59, 999);
            
            combinedData = combinedData.filter(item => {
                const timestamp = item.entryTime;
                if (!timestamp) return false;
                
                const itemDate = new Date(timestamp.seconds * 1000);
                return itemDate >= startOfDay && itemDate <= endOfDay;
            });
        }
        
        // Sort by entry time/timestamp (newest first)
        combinedData.sort((a, b) => {
            const timeA = a.entryTime ? a.entryTime.seconds : 0;
            const timeB = b.entryTime ? b.entryTime.seconds : 0;
            return timeB - timeA;
        });
        
        // Group by license plate to avoid duplicates
        const licensePlateGroups = {};
        
        combinedData.forEach(item => {
            const licensePlate = item.licensePlate;
            if (!licensePlate) return;
            
            // Convert timestamp to a string for grouping
            const entryTimeStr = item.entryTime ? 
                  new Date(item.entryTime.seconds * 1000).toISOString().split('T')[0] + 
                  'T' + new Date(item.entryTime.seconds * 1000).toTimeString().split(' ')[0].substring(0, 5) : 
                  '';
            
            // Create a key that combines license plate and timestamp (rounded to minutes)
            const key = `${licensePlate}-${entryTimeStr}`;
            
            // If this key doesn't exist yet or if we prefer Orders over PlateRecognition data
            if (!licensePlateGroups[key] || 
                (licensePlateGroups[key].source === "plateRecognition" && item.source === "order")) {
                licensePlateGroups[key] = item;
            } 
            // If this is an order with a user ID and the existing item doesn't have one, update
            else if (item.source === "order" && item.userID && (!licensePlateGroups[key].userID || licensePlateGroups[key].userID === "System")) {
                licensePlateGroups[key] = item;
            }
        });
        
        // Convert groups back to array
        const dedupedData = Object.values(licensePlateGroups);
        
        // Sort again to ensure proper order
        dedupedData.sort((a, b) => {
            const timeA = a.entryTime ? a.entryTime.seconds : 0;
            const timeB = b.entryTime ? b.entryTime.seconds : 0;
            return timeB - timeA;
        });
        
        if (dedupedData.length > 0) {
            // Render the deduplicated data
            renderPlateData(dedupedData);
        } else {
            tableBody.innerHTML = '<tr><td colspan="8" class="text-center">No data found</td></tr>';
        }
    } catch (error) {
        console.error("Error getting documents: ", error);
        tableBody.innerHTML = `<tr><td colspan="8" class="text-center">Error loading data: ${error.message}</td></tr>`;
    }
}

// Format timestamp for display
function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    
    const date = new Date(timestamp.seconds * 1000);
    return date.toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// Calculate exit time based on entry time and duration
function calculateExitTime(timestamp, exitAfterSeconds) {
    if (!timestamp) return 'N/A';
    
    // If exitAfterSeconds is not provided, return N/A
    if (!exitAfterSeconds) return 'N/A';
    
    const exitDate = new Date((timestamp.seconds + exitAfterSeconds) * 1000);
    return exitDate.toLocaleString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// Initialize when DOM is loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDatabaseViewer);
} else {
    initDatabaseViewer();
}

// Make functions available globally
window.initDatabaseViewer = initDatabaseViewer;
window.formatTimestamp = formatTimestamp;
window.calculateExitTime = calculateExitTime;

// Set up event listeners for database viewer elements
function setupDatabaseViewerListeners() {
    // Filter buttons
    applyFilterBtn.addEventListener('click', handleApplyFilter);
    resetFilterBtn.addEventListener('click', handleResetFilter);
    
    // Pagination buttons
    prevPageBtn.addEventListener('click', handlePrevPage);
    nextPageBtn.addEventListener('click', handleNextPage);
}

// Handle applying filters
function handleApplyFilter() {
    // Get filter values
    const date = dateFilter.value ? new Date(dateFilter.value) : null;
    const plate = plateFilter.value.trim();
    
    // Set active filters
    activeFilters = {};
    
    if (date) {
        // Set the start of the selected day
        const startDate = new Date(date);
        startDate.setHours(0, 0, 0, 0);
        
        // Set the end of the selected day
        const endDate = new Date(date);
        endDate.setHours(23, 59, 59, 999);
        
        activeFilters.dateRange = {
            start: startDate,
            end: endDate
        };
    }
    
    if (plate) {
        activeFilters.plate = plate;
    }
    
    // Reset pagination and load data with filters
    resetPagination();
    loadDatabaseData();
}

// Handle resetting filters
function handleResetFilter() {
    // Clear filter inputs
    dateFilter.value = new Date().toISOString().split('T')[0];
    plateFilter.value = '';
    
    // Clear active filters
    activeFilters = {};
    
    // Reset pagination and reload data
    resetPagination();
    loadDatabaseData();
}

// Handle previous page button
function handlePrevPage() {
    if (currentPage > 1) {
        currentPage--;
        loadDatabaseData(false, true);
    }
}

// Handle next page button
function handleNextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        loadDatabaseData(true);
    }
}

// Reset pagination state
function resetPagination() {
    currentPage = 1;
    totalPages = 1;
    lastDoc = null;
    firstDoc = null;
    updatePaginationUI();
}

// Update pagination UI
function updatePaginationUI() {
    paginationInfo.textContent = `Page ${currentPage} of ${totalPages || 1}`;
    prevPageBtn.disabled = currentPage <= 1;
    nextPageBtn.disabled = currentPage >= totalPages;
}

// Load database data from Firebase
async function loadDatabaseData(next = false, prev = false) {
    try {
        // Show loading state
        databaseTable.innerHTML = '<tr><td colspan="7">Loading data...</td></tr>';
        
        // Reference to Firestore database
        const db = window.firebaseApp.db;
        
        // Get total count for pagination
        await updateTotalPages(db);
        
        // Fetch data from both Orders and PlateRecognition collections
        const ordersPromise = fetchOrdersData(db, next, prev);
        const plateRecognitionPromise = fetchPlateRecognitionData(db, next, prev);
        
        // Wait for both queries to complete
        const [ordersData, plateRecognitionData] = await Promise.all([ordersPromise, plateRecognitionPromise]);
        
        // Combine the results
        const combinedData = [...ordersData, ...plateRecognitionData];
        
        // Sort by timestamp (newest first)
        combinedData.sort((a, b) => {
            const timeA = a.timestamp ? a.timestamp.seconds : 0;
            const timeB = b.timestamp ? b.timestamp.seconds : 0;
            return timeB - timeA;
        });
        
        // Clear the table
        databaseTable.innerHTML = '';
        
        // Check if we have results
        if (combinedData.length === 0) {
            databaseTable.innerHTML = '<tr><td colspan="7">No data found matching your criteria.</td></tr>';
            return;
        }
        
        // Display the results
        combinedData.forEach(item => {
            const row = document.createElement('tr');
            
            if (item.type === 'order') {
                // Format dates for order
                const entryTime = item.entryTime ? formatDate(item.entryTime) : 'N/A';
                const exitTime = item.exitTime ? formatDate(item.exitTime) : 'In Progress';
                const fee = item.fee ? `£${item.fee.toFixed(2)}` : '£0.00';
                
                row.innerHTML = `
                    <td>${item.userID || 'Anonymous'}</td>
                    <td>${item.licensePlate || 'Unknown'}</td>
                    <td>N/A</td>
                    <td>${entryTime}</td>
                    <td>${exitTime}</td>
                    <td>${fee}</td>
                    <td>
                        <button class="btn small-btn outline-btn view-btn" data-id="${item.id}" data-type="order">View</button>
                    </td>
                `;
            } else if (item.type === 'plateRecognition') {
                // Format data for plate recognition
                const entryTime = item.timestamp ? formatDate(item.timestamp) : 'N/A';
                const confidencePercent = item.confidence ? `${Math.round(item.confidence * 100)}%` : 'N/A';
                
                // Calculate exit time
                let exitTimeDisplay = 'N/A';
                if (item.timestamp && item.exit_after_seconds) {
                    const exitTime = new Date(item.timestamp.toDate().getTime() + (item.exit_after_seconds * 1000));
                    exitTimeDisplay = formatDate(exitTime);
                }
                
                // Format fee
                const feeDisplay = item.fee ? `£${item.fee.toFixed(2)}` : 'N/A';
                
                row.innerHTML = `
                    <td>System</td>
                    <td>${item.plate_number || 'Unknown'}</td>
                    <td>${item.country_identifier || 'UNKNOWN'}</td>
                    <td>${entryTime}</td>
                    <td>${exitTimeDisplay}</td>
                    <td>${feeDisplay}</td>
                    <td>
                        <button class="btn small-btn outline-btn view-btn" data-id="${item.id}" data-type="plate">View</button>
                    </td>
                `;
            }
            
            databaseTable.appendChild(row);
        });
        
        // Add event listeners to view buttons
        const viewButtons = document.querySelectorAll('.view-btn');
        viewButtons.forEach(button => {
            button.addEventListener('click', () => handleViewDetails(button.dataset.id, button.dataset.type));
        });
        
        // Update pagination UI
        updatePaginationUI();
        
    } catch (error) {
        console.error('Error loading database data:', error);
        databaseTable.innerHTML = '<tr><td colspan="7">Error loading data. Please try again.</td></tr>';
        
        // Show error message
        if (typeof showMessage === 'function') {
            showMessage('Error', 'Failed to load database data. Please try again later.');
        }
    }
}

// Fetch orders data from Firebase
async function fetchOrdersData(db, next, prev) {
    try {
        // Start with the Orders collection
        let query = db.collection('Orders');
        
        // Apply date filter if active
        if (activeFilters.dateRange) {
            query = query.where('entryTime', '>=', activeFilters.dateRange.start)
                         .where('entryTime', '<=', activeFilters.dateRange.end);
        }
        
        // Order by entry time
        query = query.orderBy('entryTime', 'desc');
        
        // Apply pagination
        if (next && lastDoc) {
            query = query.startAfter(lastDoc);
        } else if (prev && firstDoc) {
            query = query.endBefore(firstDoc).limitToLast(pageSize);
        }
        
        // Limit results to page size
        query = query.limit(pageSize);
        
        // Execute the query
        const snapshot = await query.get();
        
        if (snapshot.empty) {
            return [];
        }
        
        // Process results
        const results = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            
            // Filter by plate number if needed
            if (activeFilters.plate && data.licensePlate && 
                !data.licensePlate.toLowerCase().includes(activeFilters.plate.toLowerCase())) {
                return;
            }
            
            results.push({
                ...data,
                id: doc.id,
                type: 'order'
            });
        });
        
        return results;
    } catch (error) {
        console.error('Error fetching orders:', error);
        return [];
    }
}

// Fetch plate recognition data from Firebase
async function fetchPlateRecognitionData(db, next, prev) {
    try {
        // Start with the PlateRecognition collection
        let query = db.collection('PlateRecognition');
        
        // Apply date filter if active
        if (activeFilters.dateRange) {
            query = query.where('timestamp', '>=', activeFilters.dateRange.start)
                         .where('timestamp', '<=', activeFilters.dateRange.end);
        }
        
        // Order by timestamp
        query = query.orderBy('timestamp', 'desc');
        
        // Apply pagination
        if (next && lastDoc) {
            query = query.startAfter(lastDoc);
        } else if (prev && firstDoc) {
            query = query.endBefore(firstDoc).limitToLast(pageSize);
        }
        
        // Limit results to page size
        query = query.limit(pageSize);
        
        // Execute the query
        const snapshot = await query.get();
        
        if (snapshot.empty) {
            return [];
        }
        
        // Process results
        const results = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            
            // Filter by plate number if needed
            if (activeFilters.plate && data.plate_number && 
                !data.plate_number.toLowerCase().includes(activeFilters.plate.toLowerCase())) {
                return;
            }
            
            results.push({
                ...data,
                id: doc.id,
                type: 'plateRecognition'
            });
        });
        
        return results;
    } catch (error) {
        console.error('Error fetching plate recognition data:', error);
        return [];
    }
}

// Update total pages for pagination
async function updateTotalPages(db) {
    try {
        // Count documents in both collections
        let ordersCount = 0;
        let plateRecognitionCount = 0;
        
        // Count Orders documents
        let ordersQuery = db.collection('Orders');
        if (activeFilters.dateRange) {
            ordersQuery = ordersQuery.where('entryTime', '>=', activeFilters.dateRange.start)
                                   .where('entryTime', '<=', activeFilters.dateRange.end);
        }
        const ordersSnapshot = await ordersQuery.get();
        ordersCount = ordersSnapshot.size;
        
        // Count PlateRecognition documents
        let plateQuery = db.collection('PlateRecognition');
        if (activeFilters.dateRange) {
            plateQuery = plateQuery.where('timestamp', '>=', activeFilters.dateRange.start)
                                 .where('timestamp', '<=', activeFilters.dateRange.end);
        }
        const plateSnapshot = await plateQuery.get();
        plateRecognitionCount = plateSnapshot.size;
        
        // Calculate total pages
        const totalCount = ordersCount + plateRecognitionCount;
        totalPages = Math.ceil(totalCount / pageSize) || 1;
        
        // Update pagination UI
        updatePaginationUI();
        
    } catch (error) {
        console.error('Error counting documents:', error);
        totalPages = 1;
    }
}

// Handle view details button
function handleViewDetails(id, type) {
    let message = '';
    
    if (type === 'order') {
        message = `Viewing details for order ${id}. In a real implementation, this would show detailed information about the order.`;
    } else if (type === 'plate') {
        message = `Viewing details for plate ${id}. In a real implementation, this would show detailed information about the plate.`;
    }
    
    if (typeof showMessage === 'function') {
        showMessage('Info', message);
    }
}

// Render plate data to table
async function renderPlateData(data) {
    const tableBody = document.getElementById('plateDataBody');
    if (!tableBody) {
        console.error('plateDataBody element not found');
        return;
    }
    
    tableBody.innerHTML = '<tr><td colspan="8" class="text-center">Loading data...</td></tr>';
    
    if (data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="8" class="text-center">No data available</td></tr>';
        return;
    }
    
    // Get user name mapping from user IDs
    const db = firebase.firestore();
    const userMap = {};
    
    // Collect all user IDs that need to be queried
    const userIds = data.filter(doc => doc.userID).map(doc => doc.userID);
    
    if (userIds.length > 0) {
        try {
            // Get all user documents for the user IDs
            const usersSnapshot = await db.collection('Users').get();
            
            // Create a mapping from user ID to user name
            usersSnapshot.forEach(doc => {
                const userData = doc.data();
                userMap[doc.id] = userData.displayName || userData.name || userData.email || doc.id;
            });
        } catch (error) {
            console.error('Error fetching user data:', error);
        }
    }
    
    // Clear table, prepare to add data
    tableBody.innerHTML = '';
    
    data.forEach(doc => {
        const row = document.createElement('tr');
        
        // User name (instead of user ID)
        const userNameCell = document.createElement('td');
        if (doc.userName) {
            userNameCell.textContent = doc.userName;
        } else if (doc.userID && userMap[doc.userID]) {
            userNameCell.textContent = userMap[doc.userID];
        } else {
            userNameCell.textContent = doc.userID ? 'Unknown' : 'System';
        }
        row.appendChild(userNameCell);
        
        // License Plate
        const licensePlateCell = document.createElement('td');
        licensePlateCell.textContent = doc.licensePlate || doc.plate_number || 'Unknown';
        row.appendChild(licensePlateCell);
        
        // Country Identifier
        const countryCell = document.createElement('td');
        countryCell.textContent = doc.countryIdentifier || doc.country_identifier || 'GB';
        row.appendChild(countryCell);
        
        // Entry Time
        const entryTimeCell = document.createElement('td');
        entryTimeCell.textContent = formatTimestamp(doc.entryTime || doc.timestamp);
        row.appendChild(entryTimeCell);
        
        // Exit Time - Always calculate from entry time and exit_after_seconds if exitTime is not available
        const exitTimeCell = document.createElement('td');
        if (doc.exitTime) {
            exitTimeCell.textContent = formatTimestamp(doc.exitTime);
        } else if ((doc.entryTime || doc.timestamp) && doc.exit_after_seconds) {
            exitTimeCell.textContent = calculateExitTime(doc.entryTime || doc.timestamp, doc.exit_after_seconds);
            exitTimeCell.style.color = "#FF8C00"; // Dark orange for predicted exit time
            exitTimeCell.title = "Predicted exit time";
        } else {
            exitTimeCell.textContent = "Not available";
        }
        row.appendChild(exitTimeCell);
        
        // Fee
        const feeCell = document.createElement('td');
        feeCell.textContent = '£' + (doc.fee || 0).toFixed(2);
        row.appendChild(feeCell);
        
        // Payment Status
        const paymentStatusCell = document.createElement('td');
        if (doc.status === 'paid') {
            paymentStatusCell.textContent = 'Paid';
            paymentStatusCell.classList.add('text-success');
            paymentStatusCell.style.color = 'green';
            paymentStatusCell.style.fontWeight = 'bold';
        } else {
            paymentStatusCell.textContent = 'Unpaid';
            paymentStatusCell.classList.add('text-danger');
            paymentStatusCell.style.color = 'red';
        }
        row.appendChild(paymentStatusCell);
        
        // Actions
        const actionsCell = document.createElement('td');
        const viewButton = document.createElement('button');
        viewButton.textContent = 'View';
        viewButton.className = 'btn btn-sm btn-primary';
        viewButton.style.backgroundColor = '#4285F4';
        viewButton.style.color = '#ffffff';
        viewButton.style.border = 'none';
        viewButton.style.padding = '4px 8px';
        viewButton.style.borderRadius = '4px';
        viewButton.style.cursor = 'pointer';
        
        viewButton.addEventListener('click', () => {
            // Build details message including predicted exit time if necessary
            let exitTimeInfo;
            if (doc.exitTime) {
                exitTimeInfo = formatTimestamp(doc.exitTime);
            } else if ((doc.entryTime || doc.timestamp) && doc.exit_after_seconds) {
                exitTimeInfo = calculateExitTime(doc.entryTime || doc.timestamp, doc.exit_after_seconds) + " (Predicted)";
            } else {
                exitTimeInfo = "Not available";
            }
            
            // Get user name for display
            const userName = doc.userName || (doc.userID && userMap[doc.userID] ? userMap[doc.userID] : (doc.userID ? 'Unknown' : 'System'));
            
            // Show details in alert or modal
            alert('Details for ' + (doc.licensePlate || doc.plate_number) + '\n' +
                  'User: ' + userName + '\n' +
                  'Entry: ' + (formatTimestamp(doc.entryTime || doc.timestamp)) + '\n' +
                  'Exit: ' + exitTimeInfo + '\n' +
                  'Fee: £' + (doc.fee || 0).toFixed(2) + '\n' +
                  'Payment Status: ' + (doc.status === 'paid' ? 'Paid' : 'Unpaid'));
        });
        
        actionsCell.appendChild(viewButton);
        row.appendChild(actionsCell);
        
        tableBody.appendChild(row);
    });
}