// TASK-FE-001 | REQ-FE-001
// Application router — all routes declared here.

import { createBrowserRouter } from 'react-router-dom';
import AppShell from '../components/AppShell';
import ProtectedRoute from '../components/ProtectedRoute';

// Pages — lazy imports to keep initial bundle small
import { lazy } from 'react';

const HomePage         = lazy(() => import('../pages/HomePage'));
const LoginPage        = lazy(() => import('../pages/LoginPage'));
const RegisterPage     = lazy(() => import('../pages/RegisterPage'));
const SearchPage       = lazy(() => import('../pages/SearchPage'));
const BookDetailPage   = lazy(() => import('../pages/BookDetailPage'));
const CartPage         = lazy(() => import('../pages/CartPage'));
const CheckoutPage     = lazy(() => import('../pages/CheckoutPage'));
const PaymentPage      = lazy(() => import('../pages/PaymentPage'));
const ConfirmationPage = lazy(() => import('../pages/ConfirmationPage'));
const OrderHistoryPage = lazy(() => import('../pages/OrderHistoryPage'));
const ProfilePage      = lazy(() => import('../pages/ProfilePage'));

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true,          element: <HomePage /> },
      { path: 'login',        element: <LoginPage /> },
      { path: 'register',     element: <RegisterPage /> },
      { path: 'search',       element: <SearchPage /> },
      { path: 'books/:id',    element: <BookDetailPage /> },
      // CR-005/REQ-NEW-001: /cart is open to guests — they see their localStorage cart.
      // Checkout remains protected. ProtectedRoute is on /checkout, not /cart.
      { path: 'cart',         element: <CartPage /> },
      {
        path: 'checkout',
        element: <ProtectedRoute><CheckoutPage /></ProtectedRoute>,
      },
      {
        path: 'payment',
        element: <ProtectedRoute><PaymentPage /></ProtectedRoute>,
      },
      {
        path: 'confirmation',
        element: <ProtectedRoute><ConfirmationPage /></ProtectedRoute>,
      },
      {
        path: 'orders',
        element: <ProtectedRoute><OrderHistoryPage /></ProtectedRoute>,
      },
      {
        path: 'profile',
        element: <ProtectedRoute><ProfilePage /></ProtectedRoute>,
      },
    ],
  },
]);

export default router;
