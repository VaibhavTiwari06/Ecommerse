// TASK-FE-007 | REQ-CHK-001, REQ-CHK-002, REQ-CHK-003, REQ-GFT-002
// CheckoutPage — address selection, order summary, gift point redemption, delivery date.
//
// Data flow:
//   1. Load GET /api/checkout/summary on mount.
//   2. User selects a saved address (or adds a new one inline).
//   3. User optionally enters gift points to redeem (0 → balance).
//   4. Live totals recalculated client-side from summary + redemption input.
//   5. "Continue to payment" → navigate to /payment with { addressId, giftPointsToRedeem }.

import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import type { CheckoutSummary, AddressItem } from '../types';

// Shape passed to PaymentPage via router state
export interface CheckoutState {
  addressId: number;
  giftPointsToRedeem: number;
  grandTotal: number;
}

export default function CheckoutPage() {
  const navigate = useNavigate();

  const [summary, setSummary] = useState<CheckoutSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // ── Address ───────────────────────────────────────────────────────────────
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);

  // Inline new-address form
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [addrLabel, setAddrLabel] = useState('');
  const [addrStreet, setAddrStreet] = useState('');
  const [addrCity, setAddrCity] = useState('');
  const [addrState, setAddrState] = useState('');
  const [addrPincode, setAddrPincode] = useState('');
  const [addrSaving, setAddrSaving] = useState(false);
  const [addrError, setAddrError] = useState('');

  // ── Gift points ───────────────────────────────────────────────────────────
  const [giftPointsInput, setGiftPointsInput] = useState('');
  const [giftPointsError, setGiftPointsError] = useState('');

  // ── Load summary ──────────────────────────────────────────────────────────
  useEffect(() => {
    apiClient
      .get<CheckoutSummary>('/api/checkout/summary')
      .then((s) => {
        setSummary(s);
        // Auto-select default address if present
        const def = s.savedAddresses.find((a) => a.isDefault) ?? s.savedAddresses[0] ?? null;
        if (def) setSelectedAddressId(def.id);
      })
      .catch(() => setError('Failed to load checkout. Please go back and try again.'))
      .finally(() => setLoading(false));
  }, []);

  // ── Derived totals (REQ-GFT-002 AC4) ──────────────────────────────────────
  const pointsToRedeem = Math.max(0, parseInt(giftPointsInput || '0', 10) || 0);
  const giftPointDiscount = pointsToRedeem * 2;                      // 1 pt = ₹2
  const subtotal = summary?.subtotal ?? 0;
  const deliveryCharge = summary?.deliveryCharge ?? 40;
  // Grand total cannot be negative — minimum is ₹40 (delivery only) REQ-GFT-002 AC5
  const grandTotal = Math.max(deliveryCharge, subtotal - giftPointDiscount + deliveryCharge);

  // ── Gift point validation ─────────────────────────────────────────────────
  function validateGiftPoints(raw: string) {
    const val = parseInt(raw || '0', 10);
    if (isNaN(val) || val < 0) {
      setGiftPointsError('Enter a valid number of points.');
      return;
    }
    if (summary && val > summary.giftPointBalance) {
      setGiftPointsError(`You only have ${summary.giftPointBalance} points.`);
      return;
    }
    setGiftPointsError('');
  }

  // ── Save new address inline (REQ-CHK-001 AC3) ─────────────────────────────
  async function handleSaveAddress(e: FormEvent) {
    e.preventDefault();
    setAddrError('');

    if (!addrStreet.trim() || !addrCity.trim() || !addrState.trim() || !addrPincode.trim()) {
      setAddrError('Street, city, state and pincode are required.');
      return;
    }
    if (!/^\d{6}$/.test(addrPincode)) {
      setAddrError('Pincode must be exactly 6 digits.');
      return;
    }

    setAddrSaving(true);
    try {
      const saved = await apiClient.post<AddressItem>('/api/user/addresses', {
        label: addrLabel.trim() || null,
        street: addrStreet.trim(),
        city: addrCity.trim(),
        state: addrState.trim(),
        pincode: addrPincode.trim(),
        isDefault: false,
      });

      // Add saved address to summary list and select it
      setSummary((prev) =>
        prev ? { ...prev, savedAddresses: [...prev.savedAddresses, saved] } : prev
      );
      setSelectedAddressId(saved.id);
      setShowAddressForm(false);

      // Reset form
      setAddrLabel(''); setAddrStreet(''); setAddrCity('');
      setAddrState(''); setAddrPincode('');
    } catch (err: unknown) {
      setAddrError(err instanceof Error ? err.message : 'Failed to save address.');
    } finally {
      setAddrSaving(false);
    }
  }

  // ── Continue to payment ───────────────────────────────────────────────────
  function handleContinue() {
    if (!selectedAddressId) {
      setError('Please select a delivery address.');
      return;
    }
    if (giftPointsError) return;

    const state: CheckoutState = {
      addressId: selectedAddressId,
      giftPointsToRedeem: pointsToRedeem,
      grandTotal,
    };
    navigate('/payment', { state });
  }

  // ── Loading / error ───────────────────────────────────────────────────────
  if (loading) return <div className="loading-spinner">Loading checkout…</div>;

  if (error && !summary) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
      </div>
    );
  }

  if (!summary) return null;

  const { items, savedAddresses, giftPointBalance, tentativeDeliveryDate } = summary;

  return (
    <div className="page">
      <h1 className="checkout-title">Checkout</h1>

      <div className="checkout-layout">

        {/* ── Left column ── */}
        <div className="checkout-main">

          {/* Delivery address — REQ-CHK-001 */}
          <section className="checkout-section">
            <h2 className="checkout-section-title">Delivery address</h2>

            {savedAddresses.length === 0 && !showAddressForm && (
              <p className="checkout-hint">No saved addresses. Add one below.</p>
            )}

            <div className="address-list">
              {savedAddresses.map((addr) => (
                <label key={addr.id} className="address-option">
                  <input
                    type="radio"
                    name="address"
                    value={addr.id}
                    checked={selectedAddressId === addr.id}
                    onChange={() => setSelectedAddressId(addr.id)}
                  />
                  <div className="address-option-body">
                    {addr.label && <span className="address-label">{addr.label}</span>}
                    <span>{addr.street}, {addr.city}</span>
                    <span>{addr.state} — {addr.pincode}</span>
                  </div>
                </label>
              ))}
            </div>

            {/* Toggle inline address form */}
            {!showAddressForm && (
              <button
                className="btn-secondary checkout-add-addr"
                onClick={() => setShowAddressForm(true)}
              >
                + Add new address
              </button>
            )}

            {/* Inline address form — REQ-CHK-001 AC3 */}
            {showAddressForm && (
              <form className="addr-form" onSubmit={handleSaveAddress} noValidate>
                <h3 className="addr-form-title">New address</h3>

                <div className="form-group">
                  <label htmlFor="addrLabel">Label (optional)</label>
                  <input id="addrLabel" type="text" value={addrLabel}
                    onChange={(e) => setAddrLabel(e.target.value)} placeholder="Home / Office" />
                </div>
                <div className="form-group">
                  <label htmlFor="addrStreet">Street *</label>
                  <input id="addrStreet" type="text" value={addrStreet}
                    onChange={(e) => setAddrStreet(e.target.value)} required />
                </div>
                <div className="addr-form-row">
                  <div className="form-group">
                    <label htmlFor="addrCity">City *</label>
                    <input id="addrCity" type="text" value={addrCity}
                      onChange={(e) => setAddrCity(e.target.value)} required />
                  </div>
                  <div className="form-group">
                    <label htmlFor="addrState">State *</label>
                    <input id="addrState" type="text" value={addrState}
                      onChange={(e) => setAddrState(e.target.value)} required />
                  </div>
                  <div className="form-group">
                    <label htmlFor="addrPincode">Pincode *</label>
                    <input id="addrPincode" type="text" value={addrPincode}
                      onChange={(e) => setAddrPincode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      inputMode="numeric" required />
                  </div>
                </div>

                {addrError && <p className="form-error">{addrError}</p>}

                <div className="addr-form-actions">
                  <button type="submit" className="btn-primary" disabled={addrSaving}>
                    {addrSaving ? 'Saving…' : 'Save address'}
                  </button>
                  <button type="button" className="btn-secondary"
                    onClick={() => { setShowAddressForm(false); setAddrError(''); }}>
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </section>

          {/* Gift points — REQ-GFT-002 */}
          {giftPointBalance > 0 && (
            <section className="checkout-section">
              <h2 className="checkout-section-title">Gift points</h2>
              <p className="checkout-hint">
                You have <strong>{giftPointBalance} points</strong> (worth ₹{giftPointBalance * 2}).
                Each point gives ₹2 off.
              </p>
              <div className="gift-point-row">
                <div className="form-group" style={{ flex: 1, maxWidth: 200 }}>
                  <label htmlFor="giftPoints">Points to redeem</label>
                  <input
                    id="giftPoints"
                    type="number"
                    min={0}
                    max={giftPointBalance}
                    value={giftPointsInput}
                    onChange={(e) => {
                      setGiftPointsInput(e.target.value);
                      validateGiftPoints(e.target.value);
                    }}
                    placeholder={`0 – ${giftPointBalance}`}
                    inputMode="numeric"
                  />
                </div>
                {giftPointsError && <p className="form-error">{giftPointsError}</p>}
                {pointsToRedeem > 0 && !giftPointsError && (
                  <p className="gift-point-saving">
                    Saving ₹{giftPointDiscount.toFixed(2)}
                  </p>
                )}
              </div>
            </section>
          )}

          {/* Cart items summary */}
          <section className="checkout-section">
            <h2 className="checkout-section-title">Items ({items.length})</h2>
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

        {/* ── Right column — order summary ── */}
        <aside className="cart-summary">
          <h2 className="cart-summary-title">Order summary</h2>

          <div className="cart-summary-row">
            <span>Subtotal</span>
            <span>₹{subtotal.toFixed(2)}</span>
          </div>

          {pointsToRedeem > 0 && !giftPointsError && (
            <div className="cart-summary-row" style={{ color: 'var(--color-success)' }}>
              <span>Gift point discount</span>
              <span>−₹{giftPointDiscount.toFixed(2)}</span>
            </div>
          )}

          <div className="cart-summary-row">
            <span>Delivery</span>
            <span>₹{deliveryCharge.toFixed(2)}</span>
          </div>

          <div className="cart-summary-row cart-summary-total">
            <span>Total</span>
            <span>₹{grandTotal.toFixed(2)}</span>
          </div>

          {/* Delivery date — REQ-CHK-003 */}
          <div className="checkout-delivery-date">
            <span className="checkout-delivery-label">Estimated delivery</span>
            <span className="checkout-delivery-value">
              {new Date(tentativeDeliveryDate).toLocaleDateString('en-IN', {
                day: 'numeric', month: 'long', year: 'numeric',
              })}
            </span>
          </div>

          {error && <p className="form-error" style={{ marginTop: 8 }}>{error}</p>}

          <button
            className="btn-primary btn-full"
            style={{ marginTop: 20 }}
            onClick={handleContinue}
            disabled={!selectedAddressId || !!giftPointsError}
          >
            Continue to payment
          </button>
        </aside>
      </div>
    </div>
  );
}
