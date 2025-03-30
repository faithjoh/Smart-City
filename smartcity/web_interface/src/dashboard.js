// Dashboard Module for Smart City ANPR Admin Panel

// DOM Elements
const totalVehiclesEl = document.getElementById('totalVehicles');
const activeParkingEl = document.getElementById('activeParking');
const revenueTodayEl = document.getElementById('revenueToday');
const goToPlateReaderBtn = document.getElementById('goToPlateReaderBtn');
const goToDatabaseBtn = document.getElementById('goToDatabaseBtn');

// Initialize Dashboard
function initDashboard() {
    console.log('Dashboard initialized');
    
    // Load the dashboard data
    loadDashboardData();
    
    // Set up event listeners
    setupDashboardListeners();
}

// Load dashboard data from Firebase
async function loadDashboardData() {
    try {
        // Get the current date at midnight for daily stats
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        // Reference to Firestore database
        const db = window.firebaseApp.db;
        
        // Get total vehicles (total unique license plates)
        const platesSnapshot = await db.collection('PlateRecognition')
            .get();
            
        // Use a Set to count unique plates
        const uniquePlates = new Set();
        platesSnapshot.forEach(doc => {
            const data = doc.data();
            if (data.plate_number) {
                uniquePlates.add(data.plate_number);
            }
        });
        
        // Update the total vehicles display
        totalVehiclesEl.textContent = uniquePlates.size;
        
        // Get active parking (orders with entry time but no exit time)
        const activeOrdersSnapshot = await db.collection('Orders')
            .where('exitTime', '==', null)
            .get();
            
        // Update active parking display
        activeParkingEl.textContent = activeOrdersSnapshot.size;
        
        // Get today's revenue
        let revenueSnapshot;
        try {
            // First try using new query conditions
            revenueSnapshot = await db.collection('Orders')
                .where('status', '==', 'paid')
                .where('paymentDate', '>=', today)
                .get();
                
            // If results are empty, try using exitTime as fallback
            if (revenueSnapshot.empty) {
                console.log('No orders found with status=paid and paymentDate>=today, falling back to exitTime');
                revenueSnapshot = await db.collection('Orders')
                    .where('status', '==', 'paid')
                    .get();
            }
        } catch (error) {
            // If query fails (possibly field doesn't exist), fall back to original query method
            console.error('Error with paid orders query:', error);
            console.log('Falling back to default query');
            revenueSnapshot = await db.collection('Orders')
                .where('exitTime', '>=', today)
                .get();
        }
        
        // Calculate total revenue for today
        let revenue = 0;
        const processedOrderIds = new Set(); // Track orders already processed

        // Check current date
        const currentDate = new Date();
        const currentDateString = currentDate.toISOString().split('T')[0];
        console.log(`Current date for revenue calculation: ${currentDateString}`);

        revenueSnapshot.forEach(doc => {
            const data = doc.data();
            const orderId = doc.id;
            
            // Check if the order's payment date or exit time is today
            let includeInRevenue = false;
            let reason = '';
            
            // Check payment date
            if (data.paymentDate && data.paymentDate.toDate) {
                const paymentDate = data.paymentDate.toDate();
                const paymentDateString = paymentDate.toISOString().split('T')[0];
                if (paymentDateString === currentDateString) {
                    includeInRevenue = true;
                    reason = 'paymentDate matches today';
                }
            }
            
            // If no payment date but exit time exists, check exit time
            if (!includeInRevenue && data.exitTime && data.exitTime.toDate) {
                const exitTime = data.exitTime.toDate();
                const exitTimeString = exitTime.toISOString().split('T')[0];
                if (exitTimeString === currentDateString) {
                    includeInRevenue = true;
                    reason = 'exitTime matches today';
                }
            }
            
            // Check order status
            const isPaid = data.status === 'paid';
            
            // Only include paid orders from today in revenue
            if (includeInRevenue && isPaid && !processedOrderIds.has(orderId)) {
                revenue += (data.fee || 0);
                processedOrderIds.add(orderId);
                console.log(`Adding order ${orderId} with fee ${data.fee} to revenue (${reason})`);
            } else if (isPaid && data.fee) {
                console.log(`Skipping paid order ${orderId} with fee ${data.fee}: not from today`);
            }
        });
        
        // Update revenue display (format as currency)
        revenueTodayEl.textContent = `£${revenue.toFixed(2)}`;
        console.log(`Total revenue: £${revenue.toFixed(2)} from ${processedOrderIds.size} orders`);
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        
        // Set default values in case of error
        totalVehiclesEl.textContent = '0';
        activeParkingEl.textContent = '0';
        revenueTodayEl.textContent = '£0.00';
        
        // Show error message
        if (typeof showMessage === 'function') {
            showMessage('Error', 'Failed to load dashboard data. Please try again later.');
        }
    }
}

// Set up event listeners for dashboard elements
function setupDashboardListeners() {
    // Quick action buttons
    goToPlateReaderBtn.addEventListener('click', () => {
        // Show plate reader section
        showSection('plateReader');
    });
    
    goToDatabaseBtn.addEventListener('click', () => {
        // Show database section
        showSection('database');
    });
}

// Export the init function
window.initDashboard = initDashboard; 