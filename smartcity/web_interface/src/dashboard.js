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
            // 首先尝试使用新的查询条件
            revenueSnapshot = await db.collection('Orders')
                .where('status', '==', 'paid')
                .where('paymentDate', '>=', today)
                .get();
                
            // 如果结果为空，尝试使用退出时间作为备选
            if (revenueSnapshot.empty) {
                console.log('No orders found with status=paid and paymentDate>=today, falling back to exitTime');
                revenueSnapshot = await db.collection('Orders')
                    .where('status', '==', 'paid')
                    .get();
            }
        } catch (error) {
            // 如果查询失败（可能是字段不存在），退回到原来的查询方式
            console.error('Error with paid orders query:', error);
            console.log('Falling back to default query');
            revenueSnapshot = await db.collection('Orders')
                .where('exitTime', '>=', today)
                .get();
        }
        
        // Calculate total revenue for today
        let revenue = 0;
        const processedOrderIds = new Set(); // Track orders already processed

        // 检查当前日期
        const currentDate = new Date();
        const currentDateString = currentDate.toISOString().split('T')[0];
        console.log(`Current date for revenue calculation: ${currentDateString}`);

        revenueSnapshot.forEach(doc => {
            const data = doc.data();
            const orderId = doc.id;
            
            // 检查订单的支付日期或退出时间是否为今天
            let includeInRevenue = false;
            let reason = '';
            
            // 检查支付日期
            if (data.paymentDate && data.paymentDate.toDate) {
                const paymentDate = data.paymentDate.toDate();
                const paymentDateString = paymentDate.toISOString().split('T')[0];
                if (paymentDateString === currentDateString) {
                    includeInRevenue = true;
                    reason = 'paymentDate matches today';
                }
            }
            
            // 如果没有支付日期但有退出时间，检查退出时间
            if (!includeInRevenue && data.exitTime && data.exitTime.toDate) {
                const exitTime = data.exitTime.toDate();
                const exitTimeString = exitTime.toISOString().split('T')[0];
                if (exitTimeString === currentDateString) {
                    includeInRevenue = true;
                    reason = 'exitTime matches today';
                }
            }
            
            // 检查订单状态
            const isPaid = data.status === 'paid';
            
            // 只有已支付且日期匹配今天的订单才计入收入
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