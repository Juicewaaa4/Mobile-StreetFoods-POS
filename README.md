# Zoey's Street Foods POS Application

A simple mobile Point of Sale (POS) application for Zoey's Street Foods business using Kotlin with Jetpack Compose.

## Features

### Authentication
- Simple login system with role-based access
- Two roles: Admin and Cashier
- Demo credentials provided

### Cashier Features
- View product list of street foods
- Add items to cart with quantity adjustment
- Calculate total price
- Accept cash input and calculate change
- Complete transactions

### Admin Features
- Product Management (Add, Edit, Delete products)
- Toggle product availability
- View transaction history
- See sales statistics

## Design

### Color Palette
- Primary Green: #2E7D32
- Secondary Green: #66BB6A
- Background: #F5F5F5
- Surface/Card: #FFFFFF
- Text Primary: #212121
- Text Secondary: #757575

### Design Guidelines
- Minimalist and uncluttered UI
- White cards on light background
- Rounded corners (12dp–16dp)
- Primary buttons in green with white text
- Light green highlights for selected items
- Flat design without gradients

## Demo Credentials

### Admin
- Username: `admin`
- Password: `admin123`

### Cashier
- Username: `cashier`
- Password: `cashier123`

## Default Products

The app comes pre-loaded with common street food items:
- Fishball - ₱30.00
- Kwek-Kwek - ₱40.00
- Squidball - ₱35.00
- Chicken Balls - ₱45.00
- Hotdog on Stick - ₱50.00
- Banana Cue - ₱25.00
- Camote Cue - ₱25.00
- Saging na Saba - ₱20.00

## 🚀 New Features (v2.0)
- **Cloud Sync:** Fully migrated from Room database to Firebase Firestore for real-time cloud synchronization.
- **Firebase Auth:** Secure login for Admin and Cashier roles (Email/Password).
- **Profit Tracking:** Added `Cost/Puhunan` tracking for products. Automatically computes net profit/kita per item and total profit in the Analytics Dashboard.
- **Modern UI:** Upgraded to Material 3 components with a clean, dynamic aesthetic.

## 🛠 Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Firebase Firestore (Cloud NoSQL)
- **Authentication:** Firebase Auth
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & Flow

## 📋 Features
### 👑 Admin Dashboard
- **Product Management:** Add, edit, and delete products. Set Selling Price and **Puhunan (Cost)** to track profit. Toggle availability (in-stock/out-of-stock).
- **Analytics:** View total revenue, total cost, net profit, transactions, average order value, and best-selling products with date filtering (Today, This Week, This Month).
- **Transaction History:** View all past sales.

### 👩‍💼 Cashier Dashboard
- **Point of Sale (POS):** Tap products to add to cart, adjust quantities, and calculate totals instantly.
- **Payment Processing:** Enter cash amount and automatically compute change.
- **Role-based Access:** Cashiers can only access POS, Payment, and view their own dashboard.

## 🔐 Demo Credentials
> [!IMPORTANT]
> To test the app, use the following demo accounts (ensure they are registered in your Firebase Authentication console):
- **Admin:** `admin@test.com` | Password: `admin123`
- **Cashier:** `cashier@test.com` | Password: `cashier123`

## Project Structure

```
app/src/main/java/com/streetfood/pos/
├── data/
│   ├── dao/           # Data Access Objects
│   ├── database/      # Room database setup
│   └── models/        # Data models (Product, Transaction, User)
├── navigation/        # Navigation setup
├── ui/
│   ├── screens/       # Compose screens
│   └── theme/         # Theme and colors
├── viewmodel/         # ViewModels for business logic
└── MainActivity.kt    # Main activity
```

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run the application
4. Use demo credentials to login to Zoey's Street Foods POS

## Usage

### For Cashiers
1. Login with cashier credentials
2. Select "Point of Sale" from home screen
3. Add products to cart by tapping on them
4. Adjust quantities using +/- buttons
5. Click "Proceed to Payment" when ready
6. Enter cash received amount
7. Complete transaction

### For Admins
1. Login with admin credentials
2. From home screen, access:
   - **Point of Sale**: Same functionality as cashier
   - **Product Management**: Add/edit/delete products
   - **Transaction History**: View all sales records

## Database

The app uses Room Database for local storage:
- Products table: Stores product information
- Transactions table: Stores sales records
- Cart items table: Stores transaction details

## Notes

- This is a demo application with basic functionality
- Data is stored locally on the device
- Authentication is simplified for demo purposes
- Can be extended with additional features like:
  - Cloud synchronization
  - Advanced reporting
  - Inventory management
  - Customer management
  - Receipt printing
