# Foodie Express - Frontend

A modern, responsive food delivery web application built with React, TypeScript, and Tailwind CSS.

## 🚀 Features

### Public Features
- **Home Page**: Attractive landing page with restaurant highlights and features
- **Browse Restaurants**: View restaurants with filtering and search
- **Restaurant Details**: View menus and add items to cart
- **Customer Registration**: Sign up with full validation
- **Customer Login**: Secure authentication with session tokens

### Customer Features (Protected)
- **Shopping Cart**: Add, remove, update quantities, and clear cart
- **Checkout**: Delivery address form with order summary
- **Order History**: View past orders with status tracking
- **Order Details**: Detailed view of individual orders
- **Bills/Invoices**: View and download order invoices
- **Customer Profile**: View and edit personal information

### Admin Features (Protected)
- **Admin Dashboard**: Statistics overview with quick actions
- **Restaurant Management**: Add, edit, and delete restaurants
- **Food Item Management**: Manage menu items with availability toggle
- **Order Management**: View and update order statuses
- **Admin Authentication**: Separate login for admin users

## 🛠️ Tech Stack

### Frontend
- **React 18** with TypeScript
- **Vite** - Fast build tool
- **React Router** - Client-side routing
- **Axios** - HTTP client with interceptors
- **React Hook Form** - Form validation
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - UI component library
- **Lucide React** - Icon library
- **Sonner** - Toast notifications

### Backend Integration
- **Session-based authentication** with `X-Session-Token` header
- **Protected routes** for both customers and admins
- **Auto-redirect** on 401 unauthorized responses
- **Centralized API layer** with error handling

## 📁 Project Structure

```
src/
├── api/                    # API service files
├── components/            # Reusable UI components
│   ├── ui/               # shadcn/ui components
│   ├── Navbar.tsx
│   ├── Footer.tsx
│   ├── ProtectedRoute.tsx
│   └── AdminRoute.tsx
├── contexts/              # React contexts (AuthContext)
├── layouts/               # Layout components
├── pages/                 # Page components
│   ├── public/           # Home, Login, Register
│   ├── customer/         # Cart, Orders, Profile, etc.
│   └── admin/            # Admin dashboard and management
├── types/                 # TypeScript interfaces
├── lib/                   # Utility functions
└── App.tsx               # Main application component
```

## 🚦 Getting Started

### Prerequisites
- Node.js 18+ and npm
- Backend server running on http://localhost:8080

### Installation
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure environment variables (optional, defaults to localhost:8080):
   ```bash
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

### Development
Start the development server:
```bash
npm run dev
```

The app will be available at http://localhost:5173

### Building for Production
```bash
npm run build
```

### Linting
```bash
npm run lint
```

## 🔐 Authentication Flow

### Customer Authentication
1. User registers with username, password, email, and profile information
2. Backend creates customer record and returns session token
3. Frontend stores token in localStorage and adds to all subsequent requests via Axios interceptor
4. Protected routes check authentication via AuthContext

### Admin Authentication
1. Admin logs in with separate credentials (default: admin/admin123)
2. Backend validates admin role and returns session token
3. Frontend restricts admin routes to users with ADMIN role
4. Admin and customer sessions are kept separate

## 📱 Responsive Design

The application is fully responsive across:
- **Mobile**: 320px+
- **Tablet**: 768px+
- **Desktop**: 1024px+
- **Large screens**: 1440px+

## 🎨 UI Components

- **Navbar**: Responsive with mobile menu
- **Restaurant Cards**: Hover effects with gradient backgrounds
- **Food Item Cards**: Add to cart functionality with availability status
- **Cart**: Interactive quantity controls with real-time updates
- **Order Status**: Visual timeline with color-coded status badges
- **Admin Dashboard**: Professional layout with statistics cards

## 🔄 State Management

- **AuthContext**: Manages authentication state, user information, and session tokens
- **React State**: Component-level state for forms and UI interactions
- **Local Storage**: Persists session tokens across page refreshes

## 📊 API Integration

The frontend integrates with the following backend endpoints:

### Authentication
- `POST /api/auth/register` - Customer registration
- `POST /api/auth/login` - Login for customers/admins
- `POST /api/auth/logout` - Session invalidation
- `POST /api/auth/admin/register` - Admin registration

### Restaurants
- `GET /api/restaurants` - List all restaurants
- `GET /api/restaurants/{id}` - Get restaurant details
- `GET /api/restaurants/{id}/items` - Get restaurant menu items

### Customer
- `GET /api/customers/me` - Get customer profile
- `PUT /api/customers/me` - Update customer profile

### Cart
- `GET /api/cart` - Get cart contents
- `POST /api/cart/add` - Add item to cart
- `PUT /api/cart/items/{id}` - Update item quantity
- `DELETE /api/cart/items/{id}` - Remove item from cart
- `DELETE /api/cart/clear` - Clear cart

### Orders
- `POST /api/orders/place` - Place order from cart
- `GET /api/orders` - Get customer orders
- `GET /api/orders/{id}` - Get order details
- `PATCH /api/orders/{id}/cancel` - Cancel order

### Bills
- `GET /api/bills/order/{orderId}` - Get bill for order
- `GET /api/bills/my` - Get customer bills
- `GET /api/bills/{id}` - Get bill details
- `POST /api/bills/{id}/pay` - Mark bill as paid

### Admin (Requires Admin Role)
- `POST /api/admin/restaurants` - Add restaurant
- `PUT /api/admin/restaurants/{id}` - Update restaurant
- `DELETE /api/admin/restaurants/{id}` - Delete restaurant
- `POST /api/admin/restaurants/{id}/items` - Add food item
- `PUT /api/admin/food-items/{id}` - Update food item
- `DELETE /api/admin/food-items/{id}` - Delete food item
- `PATCH /api/admin/orders/{id}/status` - Update order status

## 🧪 Testing Credentials

### Default Admin Account
- **Username**: `admin`
- **Password**: `admin123`

### Demo Data
The backend seed data includes:
- **Spice Garden** (Indian cuisine)
- **Tokyo Bites** (Japanese cuisine)
- **Burger Barn** (American cuisine)

Each restaurant has sample menu items with realistic pricing.

## 🐛 Error Handling

- **Form validation**: Client-side validation with React Hook Form
- **API errors**: Centralized error handling with toast notifications
- **401 unauthorized**: Auto-redirect to login page
- **404 not found**: Proper error messages with navigation options
- **500 server errors**: User-friendly error messages with retry options

## 🎯 Key Features Implemented

### Real-time Updates
- Cart updates immediately reflect changes
- Order status updates instantly
- Form submissions show loading states

### User Experience
- Loading skeletons for API calls
- Empty states for no data
- Success/error toast notifications
- Smooth transitions and hover effects

### Security
- Session token validation
- Protected routes
- Admin role verification
- No exposed sensitive data in frontend

## 📈 Future Enhancements

1. **Real-time order tracking** with websockets
2. **Advanced search** with price range and rating filters
3. **Payment integration** with multiple payment methods
4. **Reviews and ratings** system
5. **Push notifications** for order updates
6. **Multi-language support**
7. **Dark mode toggle**

## 📄 License

This project is part of the Foodie Express full-stack application.

---

**Happy ordering! 🍽️**