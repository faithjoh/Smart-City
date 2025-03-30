// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyB7KmELhy13G7ThjqR1LyCL4aBmPEljVIg",
    authDomain: "parking-3e319.firebaseapp.com",
    projectId: "parking-3e319",
    storageBucket: "parking-3e319.firebasestorage.app",
    messagingSenderId: "1019635069018",
    appId: "1:1019635069018:web:73eca60da52adb8665e863",
    measurementId: "G-8ZK7N71MJ5"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Create auth and firestore references
const auth = firebase.auth();
const db = firebase.firestore();

// Export for use in other files
window.firebaseApp = {
    auth,
    db
};

// Log initialization
console.log("Firebase initialized successfully"); 