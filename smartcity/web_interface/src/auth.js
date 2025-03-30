// Authentication Module for Smart City ANPR Admin Panel

// Shorthand for document queries
const $ = document.querySelector.bind(document);
const $$ = document.querySelectorAll.bind(document);

// Elements
const loginContainer = $('#loginContainer');
const dashboardContainer = $('#dashboardContainer');
const loginBtn = $('#loginBtn');
const logoutBtn = $('#logoutBtn');
const emailInput = $('#email');
const passwordInput = $('#password');
const userEmailDisplay = $('#userEmail');
const forgotPasswordLink = $('#forgotPasswordLink');
const passwordResetModal = $('#passwordResetModal');
const resetEmailInput = $('#resetEmail');
const sendResetLinkBtn = $('#sendResetLinkBtn');
const closePasswordResetModal = $('#closePasswordResetModal');
const messageModal = $('#messageModal');
const modalTitle = $('#modalTitle');
const modalMessage = $('#modalMessage');
const modalOkBtn = $('#modalOkBtn');
const closeModal = $('#closeModal');

// Auth state observer
function initAuth() {
    // Check if user is already logged in
    firebase.auth().onAuthStateChanged(user => {
        if (user) {
            // User is signed in
            showDashboard(user);
        } else {
            // User is signed out
            showLogin();
        }
    });

    // Login form submission
    loginBtn.addEventListener('click', handleLogin);

    // Handle Enter key press in login form
    passwordInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        }
    });

    // Logout button
    logoutBtn.addEventListener('click', handleLogout);

    // Forgot password link
    forgotPasswordLink.addEventListener('click', showPasswordResetModal);

    // Password reset form
    sendResetLinkBtn.addEventListener('click', handlePasswordReset);
    closePasswordResetModal.addEventListener('click', hidePasswordResetModal);

    // Message modal
    modalOkBtn.addEventListener('click', hideMessageModal);
    closeModal.addEventListener('click', hideMessageModal);
}

// Handle login form submission
async function handleLogin() {
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email || !password) {
        showMessage('Error', 'Please enter both email and password.');
        return;
    }

    try {
        // Disable the login button to prevent multiple submissions
        loginBtn.disabled = true;
        loginBtn.textContent = 'Logging in...';

        // Sign in with Firebase Auth
        await firebase.auth().signInWithEmailAndPassword(email, password);
        
        // Login successful - Auth state observer will handle the UI update
        passwordInput.value = '';
    } catch (error) {
        console.error('Login error:', error);
        
        let errorMessage = 'Failed to log in. Please check your credentials.';
        if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
            errorMessage = 'Invalid email or password. Please try again.';
        } else if (error.code === 'auth/too-many-requests') {
            errorMessage = 'Too many failed login attempts. Please try again later.';
        }
        
        showMessage('Login Failed', errorMessage);
        
        // Re-enable the login button
        loginBtn.disabled = false;
        loginBtn.textContent = 'Login';
    }
}

// Handle logout
async function handleLogout() {
    try {
        await firebase.auth().signOut();
        // Auth state observer will handle UI update
    } catch (error) {
        console.error('Logout error:', error);
        showMessage('Error', 'Failed to log out. Please try again.');
    }
}

// Handle password reset
async function handlePasswordReset() {
    const email = resetEmailInput.value.trim();
    
    if (!email) {
        showMessage('Error', 'Please enter your email address.');
        return;
    }
    
    try {
        sendResetLinkBtn.disabled = true;
        sendResetLinkBtn.textContent = 'Sending...';
        
        await firebase.auth().sendPasswordResetEmail(email);
        
        hidePasswordResetModal();
        showMessage('Success', 'Password reset link has been sent to your email.');
        resetEmailInput.value = '';
    } catch (error) {
        console.error('Password reset error:', error);
        
        let errorMessage = 'Failed to send password reset link.';
        if (error.code === 'auth/user-not-found') {
            errorMessage = 'No user found with this email address.';
        }
        
        showMessage('Error', errorMessage);
    } finally {
        sendResetLinkBtn.disabled = false;
        sendResetLinkBtn.textContent = 'Send Reset Link';
    }
}

// UI Helpers
function showDashboard(user) {
    loginContainer.classList.add('hidden');
    dashboardContainer.classList.remove('hidden');
    userEmailDisplay.textContent = user.email;
    
    // Re-enable login button for next login
    loginBtn.disabled = false;
    loginBtn.textContent = 'Login';
    
    // Initialize dashboard
    if (typeof initDashboard === 'function') {
        initDashboard();
    }
}

function showLogin() {
    dashboardContainer.classList.add('hidden');
    loginContainer.classList.remove('hidden');
    
    // Clear any inputs
    emailInput.value = '';
    passwordInput.value = '';
    
    // Re-enable login button
    loginBtn.disabled = false;
    loginBtn.textContent = 'Login';
}

function showPasswordResetModal() {
    passwordResetModal.classList.remove('hidden');
}

function hidePasswordResetModal() {
    passwordResetModal.classList.add('hidden');
    resetEmailInput.value = '';
}

function showMessage(title, message) {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    messageModal.classList.remove('hidden');
}

function hideMessageModal() {
    messageModal.classList.add('hidden');
}

// Check if the DOM is fully loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAuth);
} else {
    initAuth();
} 