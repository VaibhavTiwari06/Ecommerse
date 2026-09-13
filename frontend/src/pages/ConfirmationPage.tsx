// TASK-FE-009 | REQ-PAY-005
// ConfirmationPage — purchase confirmation screen shown after successful payment.
//
// Receives the full PaymentResponse as router state from PaymentPage.
// If state is missing (direct navigation), redirects to /.
//
// Required content (REQ-PAY-005):
//   ✓ Confirmation message
//   ✓ Order ID
//   ✓ Items purchased
//   ✓ Total paid (₹)
//   ✓ Tentative delivery date
//   ✓ Gift points earned

import { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import type { PaymentResponse } from '../types';

export default function ConfirmationPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { refreshServerCart } = useCart();

  const response = location.state as PaymentResponse | null;

  // REQ-PAY-003 AC3: server clears cart on payment; sync client state
  useEffect(() => {
    if (response) {
      refreshServerCart();
    }
  }, [response, refreshServerCart]);

  // Guard: must arrive from PaymentPage
  if (!response?.orderId) {
    navigate('/', { replace: true });
    return null;
  }

  const {
    orderId,
    paymentReference,
    items,
    subtotal,
    deliveryCharge,
    giftPointsRedeemed,
    giftPointDiscount,
    grandTotal,
    giftPointsEarned,
    tentativeDeliveryDate,
    message,
  } = response;

  return (
    <div className="page">
      <div className="confirm-header">
        <div className="confirm-icon">✓</div>
        <h1 className="confirm-title">Order confirmed!</h1>
        <p className="confirm-message">{message}</p>
      </div>

      <div className="confirm-layout">

        {/* ── Order details ── */}
        <div className="confirm-main">

          {/* Meta */}
          <section className="checkout-section">
            <h2 className="checkout-section-title">Order details</h2>
            <table className="detail-meta-table" style={{ marginLeft: 16, marginRight: 16 }}>
              <tbody>
                <tr>
                  <th>Order ID</th>
                  <td>#{orderId}</td>
                </tr>
                <tr>
                  <th>Reference</th>
                  <td style={{ fontSize: 12, wordBreak: 'break-all' }}>{paymentReference}</td>
                </tr>
                <tr>
                  <th>Delivery by</th>
                  <td>
                    <strong>
                      {new Date(tentativeDeliveryDate).toLocaleDateString('en-IN', {
                        day: 'numeric', month: 'long', year: 'numeric',
                      })}
                    </strong>
                  </td>
                </tr>
              </tbody>
            </table>
          </section>

          {/* Items purchased */}
          <section className="checkout-section">
            <h2 className="checkout-section-title">Items ordered</h2>
            <div className="checkout-items">
              {items.map((item) => (
                <div key={item.bookId} className="checkout-item-row">
                  <img
                    src={item.coverImageUrl}
                    alt={item.title}
                    className="checkout-item-cover"
                    onError={(e) => {
                      (e.currentTarget as HTMLImageElement).src =
                        'https://covers.openlibrary.org/b/id/0-M.jpg';
                    }}
                  />
                  <span className="checkout-item-title">{item.title}</span>
                  <span className="checkout-item-qty">×{item.quantity}</span>
                  <span className="checkout-item-total">
                    ₹{item.itemTotal.toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* ── Payment summary sidebar ── */}
        <aside className="cart-summary">
          <h2 className="cart-summary-title">Payment summary</h2>

          <div className="cart-summary-row">
            <span>Subtotal</span>
            <span>₹{subtotal.toFixed(2)}</span>
          </div>

          {giftPointsRedeemed > 0 && (
            <div className="cart-summary-row" style={{ color: 'var(--color-success)' }}>
              <span>Gift discount ({giftPointsRedeemed} pts)</span>
              <span>−₹{giftPointDiscount.toFixed(2)}</span>
            </div>
          )}

          <div className="cart-summary-row">
            <span>Delivery</span>
            <span>₹{deliveryCharge.toFixed(2)}</span>
          </div>

          <div className="cart-summary-row cart-summary-total">
            <span>Total paid</span>
            <span>₹{grandTotal.toFixed(2)}</span>
          </div>

          {/* Gift points earned — REQ-GFT-001 */}
          {giftPointsEarned > 0 && (
            <div className="confirm-points-earned">
              <span className="badge badge-success">+{giftPointsEarned} gift points earned</span>
              <p className="confirm-points-note">
                Worth ₹{(giftPointsEarned * 2).toFixed(2)} off your next order.
              </p>
            </div>
          )}

          <div className="confirm-actions">
            <Link to="/orders" className="btn-primary btn-full" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', padding: '9px 18px' }}>
              View my orders
            </Link>
            <Link to="/" className="cart-continue-link">
              Continue shopping
            </Link>
          </div>
        </aside>
      </div>
    </div>
  );
}
