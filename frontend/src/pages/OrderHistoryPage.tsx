// TASK-FE-010 | REQ-ORD-002, REQ-ORD-003, REQ-ORD-004
// OrderHistoryPage — list of past orders, expandable detail, Buy Again, Cancel.
//
// Layout:
//   • Order list (summary cards) — GET /api/orders
//   • Clicking an order expands inline detail — GET /api/orders/{id}
//   • Buy Again  — POST /api/orders/{id}/buy-again  (REQ-ORD-003)
//   • Cancel     — POST /api/orders/{id}/cancel     (REQ-ORD-004)

import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { useCart } from '../context/CartContext';
import type { OrderSummary, OrderDetail } from '../types';

// Statuses that permit cancellation (REQ-ORD-004)
const CANCELLABLE = new Set(['CONFIRMED', 'PROCESSING']);

// Human-readable status labels
const STATUS_LABEL: Record<string, string> = {
  CONFIRMED:        'Confirmed',
  PROCESSING:       'Processing',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED:        'Delivered',
  CANCELLED:        'Cancelled',
};

const STATUS_BADGE: Record<string, string> = {
  CONFIRMED:        'badge-warning',
  PROCESSING:       'badge-warning',
  OUT_FOR_DELIVERY: 'badge-info',
  DELIVERED:        'badge-success',
  CANCELLED:        'badge-muted',
};

export default function OrderHistoryPage() {
  const { refreshServerCart } = useCart();

  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);

  // Expanded order detail (one at a time)
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [detailCache, setDetailCache] = useState<Record<number, OrderDetail>>({});
  const [detailLoading, setDetailLoading] = useState(false);

  // Per-order action feedback
  const [actionState, setActionState] = useState<Record<number, { loading: boolean; message: string; error: string }>>({});

  // ── Load order list ───────────────────────────────────────────────────────
  const fetchOrders = useCallback(() => {
    setLoading(true);
    apiClient
      .get<OrderSummary[]>('/api/orders')
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  // ── Expand / collapse order detail ────────────────────────────────────────
  async function toggleDetail(id: number) {
    if (expandedId === id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(id);
    if (detailCache[id]) return; // already loaded

    setDetailLoading(true);
    try {
      const detail = await apiClient.get<OrderDetail>(`/api/orders/${id}`);
      setDetailCache((prev) => ({ ...prev, [id]: detail }));
    } catch {
      // leave detail empty — summary row still shown
    } finally {
      setDetailLoading(false);
    }
  }

  // ── Buy Again (REQ-ORD-003) ───────────────────────────────────────────────
  async function handleBuyAgain(orderId: number) {
    setActionState((prev) => ({
      ...prev,
      [orderId]: { loading: true, message: '', error: '' },
    }));
    try {
      const res = await apiClient.post<{
        addedItems: { bookId: number; title: string; quantity: number }[];
        skippedItems: { bookId: number; title: string; reason: string }[];
      }>(`/api/orders/${orderId}/buy-again`);

      await refreshServerCart();

      const added = res.addedItems.length;
      const skipped = res.skippedItems.length;
      const msg = skipped > 0
        ? `${added} item(s) added to cart. ${skipped} out-of-stock item(s) skipped.`
        : `${added} item(s) added to your cart.`;

      setActionState((prev) => ({
        ...prev,
        [orderId]: { loading: false, message: msg, error: '' },
      }));
    } catch (err: unknown) {
      setActionState((prev) => ({
        ...prev,
        [orderId]: {
          loading: false,
          message: '',
          error: err instanceof Error ? err.message : 'Failed to add items.',
        },
      }));
    }
  }

  // ── Cancel order (REQ-ORD-004) ────────────────────────────────────────────
  async function handleCancel(orderId: number) {
    if (!window.confirm('Cancel this order? Redeemed gift points will be forfeited.')) return;

    setActionState((prev) => ({
      ...prev,
      [orderId]: { loading: true, message: '', error: '' },
    }));
    try {
      await apiClient.post<{ orderId: number; status: string; message: string }>(
        `/api/orders/${orderId}/cancel`
      );

      // Update summary list status locally
      setOrders((prev) =>
        prev.map((o) => (o.id === orderId ? { ...o, status: 'CANCELLED' } : o))
      );
      // Invalidate detail cache for this order
      setDetailCache((prev) => {
        const next = { ...prev };
        delete next[orderId];
        return next;
      });

      setActionState((prev) => ({
        ...prev,
        [orderId]: { loading: false, message: 'Order cancelled.', error: '' },
      }));
    } catch (err: unknown) {
      setActionState((prev) => ({
        ...prev,
        [orderId]: {
          loading: false,
          message: '',
          error: err instanceof Error ? err.message : 'Failed to cancel order.',
        },
      }));
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────
  if (loading) return <div className="loading-spinner">Loading orders…</div>;

  return (
    <div className="page">
      <h1 className="cart-title">Order history</h1>

      {orders.length === 0 ? (
        <div className="cart-empty">
          <h2>No orders yet</h2>
          <p>When you place an order, it will appear here.</p>
          <Link to="/" className="btn-primary">Browse books</Link>
        </div>
      ) : (
        <div className="order-list">
          {orders.map((order) => {
            const action = actionState[order.id];
            const detail = detailCache[order.id];
            const isExpanded = expandedId === order.id;
            const canCancel = CANCELLABLE.has(order.status);

            return (
              <div key={order.id} className="order-card">
                {/* ── Summary row ── */}
                <div className="order-card-header">
                  <div className="order-card-meta">
                    <span className="order-card-id">Order #{order.id}</span>
                    <span className="order-card-date">
                      {new Date(order.createdAt).toLocaleDateString('en-IN', {
                        day: 'numeric', month: 'short', year: 'numeric',
                      })}
                    </span>
                  </div>

                  <div className="order-card-right">
                    <span className={`badge ${STATUS_BADGE[order.status] ?? 'badge-muted'}`}>
                      {STATUS_LABEL[order.status] ?? order.status}
                    </span>
                    <span className="order-card-total">₹{order.grandTotal.toFixed(2)}</span>
                    <span className="order-card-items">{order.itemCount} item(s)</span>
                  </div>
                </div>

                {/* ── Action feedback ── */}
                {action?.message && (
                  <p className="order-action-msg">{action.message}</p>
                )}
                {action?.error && (
                  <p className="form-error" style={{ padding: '0 20px 12px' }}>{action.error}</p>
                )}

                {/* ── Action buttons ── */}
                <div className="order-card-actions" style ={{ marginTop: 8}}>
                  <button
                    className="btn-secondary"
                    onClick={() => toggleDetail(order.id)}
                  >
                    {isExpanded ? 'Hide details ▲' : 'View details ▼'}
                  </button>

                  <button
                    className="btn-secondary"
                    onClick={() => handleBuyAgain(order.id)}
                    disabled={action?.loading}
                  >
                    {action?.loading ? 'Adding…' : '🔁 Buy again'}
                  </button>

                  {canCancel && (
                    <button
                      className="btn-danger"
                      onClick={() => handleCancel(order.id)}
                      disabled={action?.loading}
                    >
                      Cancel order
                    </button>
                  )}
                </div>

                {/* ── Expanded detail ── */}
                {isExpanded && (
                  <div className="order-detail-panel">
                    {detailLoading && !detail ? (
                      <p className="empty-state" style={{ padding: '16px 0' }}>Loading…</p>
                    ) : detail ? (
                      <>
                        {/* Items */}
                        <div className="order-detail-items">
                          {detail.items.map((item) => (
                            <div key={item.bookId} className="checkout-item-row">
                              <Link to={`/books/${item.bookId}`} className="checkout-item-title" style={{ textDecoration: 'none', color: 'inherit' }}>
                                {item.titleSnapshot}
                              </Link>
                              <span className="checkout-item-qty">×{item.quantity}</span>
                              <span className="checkout-item-total">
                                ₹{item.itemTotal.toFixed(2)}
                              </span>
                            </div>
                          ))}
                        </div>

                        {/* Totals */}
                        <div className="order-detail-totals">
                          <div className="cart-summary-row">
                            <span>Subtotal</span>
                            <span>₹{detail.subtotal.toFixed(2)}</span>
                          </div>
                          {detail.giftPointsRedeemed > 0 && (
                            <div className="cart-summary-row" style={{ color: 'var(--color-success)' }}>
                              <span>Gift discount ({detail.giftPointsRedeemed} pts)</span>
                              <span>−₹{detail.giftPointDiscount.toFixed(2)}</span>
                            </div>
                          )}
                          <div className="cart-summary-row">
                            <span>Delivery</span>
                            <span>₹{detail.deliveryCharge.toFixed(2)}</span>
                          </div>
                          <div className="cart-summary-row cart-summary-total">
                            <span>Grand total</span>
                            <span>₹{detail.grandTotal.toFixed(2)}</span>
                          </div>
                        </div>

                        {/* Delivery info */}
                        <div className="order-detail-delivery">
                          <span className="checkout-delivery-label">Delivery address</span>
                          <span>
                            {detail.deliveryAddress.street}, {detail.deliveryAddress.city},{' '}
                            {detail.deliveryAddress.state} — {detail.deliveryAddress.pincode}
                          </span>
                          <span className="checkout-delivery-label" style={{ marginTop: 8 }}>
                            Estimated delivery
                          </span>
                          <span>
                            {new Date(detail.tentativeDeliveryDate).toLocaleDateString('en-IN', {
                              day: 'numeric', month: 'long', year: 'numeric',
                            })}
                          </span>
                        </div>
                      </>
                    ) : (
                      <p className="empty-state" style={{ padding: '16px 0' }}>
                        Could not load order details.
                      </p>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
