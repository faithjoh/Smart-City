# Smart City with ANPR Parking System

This project is a smart city solution that integrates an Automatic Number Plate Recognition (ANPR) parking system. The solution consists of two main interfaces:

1. **User Interface (Android App)**  
2. **Admin Interface (Web Management Interface with ANPR functionality)**

All comments and interfaces are tailored for English users, so the function descriptions in this document are in English for ease of use and maintenance by English users.

---

## Table of Contents
- [Overview](#overview)
- [Feature Introduction](#feature-introduction)
  - [1. User Interface (Android App)](#1-user-interface-android-app)
  - [2. Admin Interface (Web Management Interface)](#2-admin-interface-web-management-interface)
  - [3. ANPR Program](#3-anpr-program)
- [Technologies Used](#technologies-used)
- [Firebase Data Structure (Recommended)](#firebase-data-structure-recommended)
- [Installation and Usage](#installation-and-usage)
- [Usage Instructions](#usage-instructions)
- [License](#license)

---

## Overview
This is a solution that simulates a smart parking scenario, utilizing ANPR (Automatic Number Plate Recognition) technology to simplify parking management processes. It includes:

- A user-facing **Android application** (registration, parking fee payment, map location, reviews, etc.)
- An admin-facing **Web management interface** (monitoring vehicles, reading license plate information, and managing parking fees)
- An **ANPR program** based on **OCR** technology (recognizing and processing license plate numbers and country identifiers)

---

## Feature Introduction

### 1. User Interface (Android App)
The **User Interface** is an Android application that provides functions such as registration, login, viewing nearby parking lots, paying parking fees, and viewing/submitting parking reviews.

1.1 **User Registration and Login**  
   - Users complete registration through **Email/Password**, **ID Name**, and **License Plate Number** (default plate format is UK license plate).  
   - User account information is stored using [Firebase Authentication](https://firebase.google.com/docs/auth).  
   - Password recovery function (forgot password) is completed through Firebase.  

1.2 **Map and Parking Lot Locations**  
   - View the distribution of approximately 20 parking lots in the UK region on the map. These must be real parking lot information.  
   - Default map location is set to **London, UK**.  
   - Support for **Postcode Search** to display search results on the map.  
   - Parking lots are displayed using map Markers with a "P" label.  

1.3 **Payment**  
   - The application displays the user's **Entry Time** and **Exit Time** (both retrieved from Firebase).  
   - Prompts the calculated **Parking Fee** (also retrieved from Firebase).  
   - Integrates **Stripe API** payment functionality for demonstration (demo only, no real payments).  

1.4 **Parking Details and Reviews**  
   - After clicking on a parking lot Marker, a secondary menu/dialog appears, containing:  
     - Parking lot name, address, rating, price, total spaces, opening hours, etc.  
     - Navigation button (can redirect to third-party navigation applications like Google Maps).  
     - User review section: view existing comments or add new feedback.  
     - Close button in the top right corner to close the menu.

1.5 **User Center**  
   - Click on the **User Icon** in the map interface to enter the User Center, which includes:  
     - **User Information** (username, email, license plate number) which can be modified.  
       - Email modification requires additional Firebase verification.  
       - Other information modifications are also stored in Firebase.  
     - **Current Order** (license plate number, start time, etc.) and a "Pay Now" button to proceed with payment.  
     - Log out functionality.
     - Option to add multiple license plate information.

1.6 **Payment History**
   - This section allows users to view the complete history of parking activities associated with their registered license plates.
   - Features include:
     - Chronological list of all parking sessions (sorted by entry time, newest first)
     - Display of essential information for each record: license plate, entry time, exit time, fee amount, and payment status
     - Clear visual distinction between paid and unpaid records
     - "Pay Now" button for unpaid records that redirects to the payment processing screen
     - Automatic refresh of payment status when returning to the screen
   - The payment history is retrieved from Firebase Firestore using composite indexing for efficient queries
   - Implemented with a RecyclerView for smooth scrolling through potentially large lists of records

### 2. Admin Interface (Web Management Interface)
The **Admin Interface** provides functions for administrator login, license plate information processing, and database data viewing.

2.1. **Admin Login and Recovery**  
   - Administrators log in with **Email** + **Password**.  
   - Email password recovery function is provided.  

2.2 **Main Interface**  
   - Displays an overview of the ANPR process and parking fees.  
   - Access to the license plate reading page or database viewing page.  

2.3 **License Plate Reading/Upload Interface**  
   - Administrators can upload images from local sources or get images from predefined folders for license plate recognition.  
   - The interface displays the image-to-character (OCR) process.  
   - Main elements: **Upload Photo**, **Start Reading**, **Processing Status**, **Result Display**.  
   - Reading results show recognized license plate information and country identifier, with options to return to the main interface or go to the information summary page. 
   — License plates not uploaded by registered users will not be entered into the database, with a prompt: "Not registered, entry not allowed"

2.4 **Database Viewing**  
   - Displays license plate information of registered users
   - Retrieves and displays the following information from Firebase:  
     - **User ID**  
     - **License Plate Number**  
     - **Country Identifier** (such as "GB" for Great Britain)
     - **Entry Time**  
     - **Exit Time**  
     - **Parking Fee**  
     - **Payment Status**
     

### 3. ANPR Program
An OCR program based on Python for automatic license plate recognition:
1. **Recognize license plates** from uploaded files or local directories  
2. The admin interface can click "Start Reading" to read photos from specified folders or user uploads  
3. The system extracts **license plate numbers** and **country identifiers** (such as "GB" for Great Britain), and stores the results in Firebase  
4. Supports recognizing "GB" markings and EU flag icons in the blue area to determine the country of origin of the license plate



### 5. HOME
5.1. **Welcome/Greeting + User Overview**

Provides a friendly and intuitive welcome interface in the form of "Welcome back, {username}".

5.2. The Home screen can display **the nearest parking lot to you**; clicking will jump to the details page of this closest parking lot

5.3. **Message Center/Notifications**

Place a message prompt or notification center entry in Home to display new system notifications, payment reminders, activity notifications, etc.
Notifications include unpaid order reminders, parking entry/exit reminders, and payment success reminders

If there are new messages, display a red dot or number prompt to remind users to check.
Users can click to view notification content

5.4. **Beginner Guide/Tutorial Entry**

For new users, if App functionality explanations are needed, a prominent "Beginner Tutorial" entry can be made in Home.

For existing users, this can be optionally collapsed or hidden.

5.5. **Modular Layout**

The entire Home is recommended to use a "card layout" or "sectioned layout", making different functional blocks clearly layered and easy to understand and click.


---

## Technologies Used
- **Main Programming Languages**: Python, Kotlin, HTML/JavaScript (Web Admin Interface)
- **Frameworks & APIs**:
  - **Firebase** (Authentication, Realtime Database or Firestore)
  - **Google Maps API** (Map and Location Functions)
  - **Stripe API** (for Payment Demonstration)
  - **OCR/Computer Vision** (Python for ANPR)
- **Android** (User Mobile Application)
- **Web** (Admin Interface)

---

## Firebase Data Structure (Recommended)

Below is the suggested Firebase data structure, which can be adjusted according to requirements:

```
Firebase Root
├── Users
│   └── {UserID}
│       ├── email: string
│       ├── password: string (hashed)
│       ├── name: string
│       ├── licensePlate: string
│       └── ...
├── ParkingSpots
│   └── {SpotID}
│       ├── name: string
│       ├── address: string
│       ├── rating: float
│       ├── totalSlots: number
│       ├── openHours: string
│       └── ...
├── Orders
│   └── {OrderID}
│       ├── userID: string
│       ├── licensePlate: string
│       ├── entryTime: timestamp
│       ├── exitTime: timestamp
│       ├── fee: float
│       └── ...
├── PlateRecognition
│   └── {RecognitionID}
│       ├── plate_number: string
│       ├── country_identifier: string
│       ├── confidence: float
│       ├── timestamp: timestamp
│       ├── exit_after_seconds: number
│       ├── fee: float
│       └── ...
├── Reviews
│   └── {SpotID}
│       └── {ReviewID}
│           ├── userID: string
│           ├── rating: float
│           ├── comment: string
│           └── ...
└── ...
```

## Latest Achievements

We have successfully implemented several key features and improvements to enhance the Smart City ANPR system:

1. **UK License Plate Recognition** - The system accurately recognizes standard UK format license plates (e.g., "AA70 PYY")
2. **Country Identifier Recognition** - The system can recognize "GB" markings and EU flag icons on license plates
3. **Database Integration** - Recognition results are automatically stored in the Firebase database, including license plate numbers, country identifiers, timestamps, and calculated fees
4. **Complete Admin Interface** - The web interface allows real-time viewing of all recognized license plates and their country identifier information
5. **Payment History Feature** - Added functionality to view all parking records associated with user license plates, with clear distinction between paid and unpaid records
6. **Multiple License Plate Support** - Users can now register and manage multiple license plates from the User Center
7. **Improved Order Management** - The system now displays both entry and exit times in the Current Order section
8. **Enhanced Error Handling** - Implemented robust error handling for Firebase queries to improve application stability:
   - Replaced Tasks.whenAllSuccess with sequential processing of queries
   - Added proper error recovery for failed license plate queries
   - Implemented graceful fallback mechanisms for partial query failures
9. **User Experience Improvements** - Removed duplicate texts in dialogs and improved UI consistency
10. **Documentation Enhancement** - Updated documentation to be fully English-compatible for international users
11. **Development Workflow Optimization** - Fixed compilation errors and warnings to ensure smooth development process
12. **Project Structure Optimization** - Removed unnecessary ParkingSystem directory to streamline the project organization

The random exit time generation (1-20 seconds) and fee calculation (£5 per second) functions have also been completed, providing simulated real-world scenario data for testing.

## Development Process and Status

### Android App Development Process

The Android application development followed a structured approach with several phases:

1. **Planning and Requirement Analysis**
   - Gathered requirements for user registration, map integration, payment processing, and parking management
   - Created wireframes for key screens: login/registration, map view, user center, and payment screens
   - Defined data models and Firebase integration points

2. **UI/UX Design Implementation**
   - Implemented Material Design 3 principles for a modern, consistent user interface
   - Created responsive layouts that adapt to different screen sizes
   - Designed intuitive navigation flow between application screens

3. **Core Functionality Development**
   - Implemented Firebase Authentication for user management
   - Integrated Google Maps API for parking lot visualization and search
   - Created custom markers and info windows for parking lots
   - Developed Firebase Firestore queries for efficient data retrieval

4. **Payment and License Plate Management**
   - Implemented Stripe SDK integration for payment processing
   - Developed the multi-license plate management system
   - Created the payment history view with status indicators

5. **Testing and Optimization**
   - Conducted unit tests for key functionality
   - Performed UI testing across different device sizes
   - Optimized Firebase queries for better performance
   - Fixed navigation and data refresh issues

### Current Completion Status

The Android application is now feature-complete with all planned functionality implemented:

✅ **Authentication System** - Complete with email/password login, registration, and password recovery  
✅ **Map Interface** - Fully functional with real UK parking locations and search capability  
✅ **User Center** - Complete profile management with license plate modification support  
✅ **Payment Processing** - Integrated Stripe payment flow with transaction history  
✅ **Multi-License Plate Support** - Users can manage multiple vehicles  
✅ **Order Management** - Entry/exit time tracking and fee calculation working correctly  
✅ **Parking Details** - Information and reviews system for parking locations  
✅ **Notification System** - Payment and parking status notifications  

### Technical Implementation Highlights

- **Architecture:** MVVM (Model-View-ViewModel) pattern for clean separation of concerns
- **UI Components:** Custom composable components for consistent UI across the application
- **Data Management:** Repository pattern for data access with LiveData for reactive UI updates
- **Background Processing:** Kotlin Coroutines for asynchronous operations
- **Error Handling:** Comprehensive exception handling with user-friendly error messages
- **Performance Optimizations:**
  - LazyColumn with paging for efficient large list rendering
  - Bitmap caching for map markers to reduce memory usage
  - Query optimization with Firebase indexing
  - Background data prefetching for common user actions

All features have been tested on multiple devices and API levels to ensure compatibility and performance across different Android versions.
