// Main JavaScript File for Smart City ANPR Admin Panel

// DOM Elements
const navItems = document.querySelectorAll('.nav-item');
const contentSections = document.querySelectorAll('.content-section');

// Initialize the application
function initApp() {
    console.log('Smart City ANPR Admin Panel initialized');
    
    // Set up navigation
    setupNavigation();
}

// Set up navigation between sections
function setupNavigation() {
    // Add click event to navigation items
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const targetSection = item.dataset.target;
            showSection(targetSection);
        });
    });
}

// Show a specific section and hide others
function showSection(sectionId) {
    // Hide all sections
    contentSections.forEach(section => {
        section.classList.add('hidden');
    });
    
    // Remove active class from all nav items
    navItems.forEach(item => {
        item.classList.remove('active');
    });
    
    // Show the target section
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.remove('hidden');
    }
    
    // Add active class to the clicked nav item
    const activeNavItem = document.querySelector(`.nav-item[data-target="${sectionId}"]`);
    if (activeNavItem) {
        activeNavItem.classList.add('active');
    }
    
    // Initialize section-specific functionality
    initializeSection(sectionId);
}

// Initialize section-specific functionality
function initializeSection(sectionId) {
    switch (sectionId) {
        case 'dashboard':
            if (typeof window.initDashboard === 'function') {
                window.initDashboard();
            }
            break;
        case 'plateReader':
            if (typeof window.initPlateReader === 'function') {
                window.initPlateReader();
            }
            break;
        case 'database':
            if (typeof window.initDatabaseViewer === 'function') {
                window.initDatabaseViewer();
            }
            break;
    }
}

// Make showSection available globally
window.showSection = showSection;

// Initialize on DOM load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initApp);
} else {
    initApp();
} 