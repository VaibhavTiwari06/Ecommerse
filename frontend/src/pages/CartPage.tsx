// TASK-FE-006 | REQ-CRT-001, REQ-CRT-002, REQ-CRT-003, REQ-CRT-004, REQ-REC-002
// CR-005/REQ-NEW-001 — CartPage is now accessible to guests.
// CartPage — line items, quantity controls, totals, recommendations strip.
//
// Routing logic (from CartContext):
//   • Authenticated  → server cart via API (GET /api/cart, PUT, DELETE)
//   • Guest          → localStorage cart via CartContext guest helpers
//
// /cart is no longer wrapped in ProtectedRoute.
// /checkout remains protected — guests see a "Sign in to checkout" prompt.

import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import RecommendationStrip from '../components/RecommendationStrip';

export default function CartPage() {
  const { isAuthenticated } = useAuth();
  const {
    serverCart,
    guestItems,
    removeItem,
    updateQty,
    refreshServerCart,
  } = useCart();
  const navigate = useNavigate();

  // Refresh server cart on mount to get latest totals
  useEffect(() => {
    if (isAuthenticated) {
      refreshServerCart();
    }
  }, [isAuthenticated, refreshServerCart]);

  // ── Derived display data ─────────────────────────────────────────────────
  const items = isAuthenticated
    ? (serverCart?.items ?? [])
    : guestItems;

  const isEmpty = items.length === 0;

  // Totals — server cart gives exact values; guest cart approximated locally
  const subtotal = isAuthenticated
    ? (serverCart?.subtotal ?? 0)
    : guestItems.reduce((s, i) => s + i.price * i.quantity, 0);

  const deliveryCharge = isAuthenticated
    ? (serverCart?.deliveryCharge ?? 40)
    : 40;

  const grandTotal = isAuthenticated
    ? (serverCart?.grandTotal ?? 0)
    : subtotal + deliveryCharge;

  // ── Handlers ─────────────────────────────────────────────────────────────
  async function handleQtyChange(bookId: number, newQty: number) {
    if (newQty < 1) {
      await removeItem(bookId);
    } else {
      await updateQty(bookId, newQty);
    }
  }

  // ── Empty state ───────────────────────────────────────────────────────────
  if (isEmpty) {
    return (
      <div className="page">
        <div className="cart-empty">
          <h1>Your cart is empty</h1>
          <p>Browse our catalogue and add books you'd like to buy.</p>
          <Link to="/" className="btn-primary">Browse books</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="cart-title">Shopping cart</h1>

      <div className="cart-layout">
        {/* ── Line items ── */}
        <div className="cart-items">
          {items.map((item) => {
            // Both ApiCartItem and GuestCartItem have bookId, title, coverImageUrl, price, quantity
            const bookId = item.bookId;
            const itemTotal =
              'itemTotal' in item ? (item as { itemTotal: number }).itemTotal : item.price * item.quantity;

            return (
              <div key={bookId} className="cart-row">
                <Link to={`/books/${bookId}`} className="cart-row-cover">
                  <img
                    src={item.coverImageUrl}
                    alt={item.title}
                    onError={(e) => {
                      (e.currentTarget as HTMLImageElement).src =
                        'https://covers.openlibrary.org/b/id/0-M.jpg';
                    }}
                  />
                </Link>

                <div className="cart-row-info">
                  <Link to={`/books/${bookId}`} className="cart-row-title">
                    {item.title}
                  </Link>
                  <p className="cart-row-price">₹{item.price.toFixed(2)} each</p>
                </div>

                <div className="cart-row-qty">
                  <button
                    className="qty-btn"
                    onClick={() => handleQtyChange(bookId, item.quantity - 1)}
                    aria-label="Decrease quantity"
                  >
                    −
                  </button>
                  <span className="qty-value">{item.quantity}</span>
                  <button
                    className="qty-btn"
                    onClick={() => handleQtyChange(bookId, item.quantity + 1)}
                    aria-label="Increase quantity"
                  >
                    +
                  </button>
                </div>

                <p className="cart-row-total">₹{itemTotal.toFixed(2)}</p>

                <button
                  className="cart-row-remove"
                  onClick={() => removeItem(bookId)}
                  aria-label={`Remove ${item.title}`}
                >
                  ✕
                </button>
              </div>
            );
          })}
        </div>

        {/* ── Order summary ── */}
        <aside className="cart-summary">
          <h2 className="cart-summary-title">Order summary</h2>

          <div className="cart-summary-row">
            <span>Subtotal</span>
            <span>₹{subtotal.toFixed(2)}</span>
          </div>
          <div className="cart-summary-row">
            <span>Delivery</span>
            <span>₹{deliveryCharge.toFixed(2)}</span>
          </div>
          <div className="cart-summary-row cart-summary-total">
            <span>Total</span>
            <span>₹{grandTotal.toFixed(2)}</span>
          </div>

          {isAuthenticated ? (
            <button
              className="btn-primary btn-full"
              style={{ marginTop: 20 }}
              onClick={() => navigate('/checkout')}
            >
              Proceed to checkout
            </button>
          ) : (
            <Link
              to="/login"
              state={{ from: { pathname: '/cart' } }}
              className="btn-primary btn-full"
              style={{ marginTop: 20, display: 'block', textAlign: 'center', textDecoration: 'none' }}
            >
              Sign in to checkout
            </Link>
          )}

          <Link to="/" className="cart-continue-link">
            ← Continue shopping
          </Link>
        </aside>
      </div>

      {/* REQ-REC-002 — recommendations with limit=4 on cart page */}
      <RecommendationStrip limit={4} />
    </div>
  );
}
