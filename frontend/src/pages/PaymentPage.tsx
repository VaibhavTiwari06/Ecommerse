// TASK-FE-008 | REQ-PAY-001, REQ-PAY-002
// PaymentPage — card type selector + simulated card form + confirm payment.
//
// Receives { addressId, giftPointsToRedeem, grandTotal } from CheckoutPage
// via router state. If state is missing the user is redirected to /checkout.
//
// On success → navigates to /confirmation with the full PaymentResponse.

import { useState, type FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import apiClient from '../api/apiClient';
import type { PaymentResponse } from '../types';
import type { CheckoutState } from './CheckoutPage';

type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD';

export default function PaymentPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // Guard: must arrive from CheckoutPage with state
  const checkoutState = location.state as CheckoutState | null;
  if (!checkoutState?.addressId) {
    // Redirect on next render — cannot navigate during render directly
    // so we use a side-effect-free redirect pattern via null render + navigate
    navigate('/checkout', { replace: true });
    return null;
  }

  const { addressId, giftPointsToRedeem, grandTotal } = checkoutState;

  // ── Form state ────────────────────────────────────────────────────────────
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CREDIT_CARD');
  const [cardNumber, setCardNumber] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [expiry, setExpiry] = useState('');    // MM/YY
  const [cvv, setCvv] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // ── Client-side card validation (simulated — REQ-PAY-001) ─────────────────
  function validate(): string | null {
    const digits = cardNumber.replace(/\s/g, '');
    if (!/^\d{16}$/.test(digits)) return 'Card number must be 16 digits.';
    if (!cardHolder.trim()) return 'Card holder name is required.';
    if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry)) return 'Expiry must be MM/YY.';
    if (!/^\d{3,4}$/.test(cvv)) return 'CVV must be 3 or 4 digits.';
    return null;
  }

  // ── Format card number with spaces every 4 digits ─────────────────────────
  function formatCardNumber(value: string) {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    return digits.replace(/(.{4})/g, '$1 ').trim();
  }

  // ── Format expiry MM/YY ───────────────────────────────────────────────────
  function formatExpiry(value: string) {
    const digits = value.replace(/\D/g, '').slice(0, 4);
    if (digits.length >= 3) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
    return digits;
  }

  // ── Submit ────────────────────────────────────────────────────────────────
  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post<PaymentResponse>('/api/payment/initiate', {
        addressId,
        paymentMethod,
        giftPointsToRedeem,
      });

      // REQ-PAY-003 AC3: cart is cleared server-side; refresh client cart
      // navigate to confirmation, passing the full response as state
      navigate('/confirmation', { state: response, replace: true });
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Payment failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <h1 className="checkout-title">Payment</h1>

      <div className="payment-layout">
        {/* ── Payment form ── */}
        <div className="payment-main">
          <form className="checkout-section" onSubmit={handleSubmit} noValidate>

            {/* Payment method — REQ-PAY-002 */}
            <h2 className="checkout-section-title">Payment method</h2>
            <div className="payment-method-group">
              {(['CREDIT_CARD', 'DEBIT_CARD'] as PaymentMethod[]).map((m) => (
                <label key={m} className={`payment-method-option ${paymentMethod === m ? 'payment-method-option--active' : ''}`}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={m}
                    checked={paymentMethod === m}
                    onChange={() => setPaymentMethod(m)}
                  />
                  {m === 'CREDIT_CARD' ? '💳 Credit card' : '🏦 Debit card'}
                </label>
              ))}
            </div>

            {/* Card details */}
            <h2 className="checkout-section-title" style={{ marginTop: 24 }}>Card details</h2>

            <div className="form-group" style={{ marginLeft: 16, marginRight: 16 }}>
              <label htmlFor="cardNumber">Card number</label>
              <input
                id="cardNumber"
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
                placeholder="1234 5678 9012 3456"
                inputMode="numeric"
                autoComplete="cc-number"
                required
              />
            </div>

            <div className="form-group" style={{ marginLeft: 16, marginRight: 16 }} >
              <label htmlFor="cardHolder">Card holder name</label>
              <input
                id="cardHolder"
                type="text"
                value={cardHolder}
                onChange={(e) => setCardHolder(e.target.value)}
                placeholder="Name as on card"
                autoComplete="cc-name"
                required
              />
            </div>

            <div className="payment-card-row">
              <div className="form-group">
                <label htmlFor="expiry">Expiry (MM/YY)</label>
                <input
                  id="expiry"
                  type="text"
                  value={expiry}
                  onChange={(e) => setExpiry(formatExpiry(e.target.value))}
                  placeholder="MM/YY"
                  inputMode="numeric"
                  autoComplete="cc-exp"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="cvv">CVV</label>
                <input
                  id="cvv"
                  type="password"
                  value={cvv}
                  onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, 4))}
                  placeholder="•••"
                  inputMode="numeric"
                  autoComplete="cc-csc"
                  required
                />
              </div>
            </div>

            {error && <p className="form-error" role="alert">{error}</p>}

            <button
              type="submit"
              className="btn-primary btn-full"
              style={{ marginTop: 8 }}
              disabled={loading}
            >
              {loading ? 'Processing…' : `Pay ₹${grandTotal.toFixed(2)}`}
            </button>
          </form>
        </div>

        {/* ── Order summary sidebar ── */}
        <aside className="cart-summary">
          <h2 className="cart-summary-title">Order total</h2>
          <div className="cart-summary-row cart-summary-total">
            <span>Amount to pay</span>
            <span>₹{grandTotal.toFixed(2)}</span>
          </div>
          {giftPointsToRedeem > 0 && (
            <p className="checkout-hint" style={{ marginTop: 10 }}>
              Includes ₹{(giftPointsToRedeem * 2).toFixed(2)} gift point discount
            </p>
          )}
          <p className="checkout-hint" style={{ marginTop: 8 }}>
            🔒 This is a simulated payment. No real transaction will occur.
          </p>
        </aside>
      </div>
    </div>
  );
}
