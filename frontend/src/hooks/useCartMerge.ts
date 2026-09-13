// TASK-FE-002 | REQ-USR-003
// useCartMerge — called immediately after successful login.
// Reads localStorage guest cart, POSTs to /api/cart/merge, then clears localStorage.

import apiClient from '../api/apiClient';
import { useCart } from '../context/CartContext';

const GUEST_CART_KEY = 'ebookstore_guest_cart';

interface MergeItem {
  bookId: number;
  quantity: number;
}

export function useCartMerge() {
  const { clearGuestCart, refreshServerCart } = useCart();

  async function mergeCart(): Promise<void> {
    const raw = localStorage.getItem(GUEST_CART_KEY);
    if (!raw) return;

    let items: MergeItem[] = [];
    try {
      items = JSON.parse(raw) as MergeItem[];
    } catch {
      return;
    }

    if (items.length === 0) return;

    await apiClient.post('/api/cart/merge', { items });
    clearGuestCart();
    await refreshServerCart();
  }

  return { mergeCart };
}
