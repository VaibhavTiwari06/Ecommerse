// TASK-FE-001 | REQ-FE-001
// CartContext — routes cart operations to localStorage (guest) or API (authenticated).

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import apiClient from '../api/apiClient';
import { useAuth } from './AuthContext';

// ── Types ────────────────────────────────────────────────────────────────────

export interface GuestCartItem {
  bookId: number;
  title: string;
  coverImageUrl: string;
  price: number;
  quantity: number;
}

export interface ApiCartItem {
  id: number;
  bookId: number;
  title: string;
  coverImageUrl: string;
  price: number;
  quantity: number;
  itemTotal: number;
}

export interface CartSummary {
  items: ApiCartItem[];
  subtotal: number;
  deliveryCharge: number;
  grandTotal: number;
}

interface CartContextValue {
  // Unified item count for nav badge
  itemCount: number;
  // Guest items (only meaningful when not authenticated)
  guestItems: GuestCartItem[];
  // Server cart (only meaningful when authenticated)
  serverCart: CartSummary | null;
  addToCart: (item: Omit<GuestCartItem, 'quantity'>) => Promise<void>;
  removeItem: (bookId: number) => Promise<void>;
  updateQty: (bookId: number, quantity: number) => Promise<void>;
  clearGuestCart: () => void;
  refreshServerCart: () => Promise<void>;
}

// ── Constants ────────────────────────────────────────────────────────────────

const GUEST_CART_KEY = 'ebookstore_guest_cart';

function readGuestCart(): GuestCartItem[] {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY);
    return raw ? (JSON.parse(raw) as GuestCartItem[]) : [];
  } catch {
    return [];
  }
}

function writeGuestCart(items: GuestCartItem[]): void {
  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items));
}

// ── Context ──────────────────────────────────────────────────────────────────

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [guestItems, setGuestItemsState] = useState<GuestCartItem[]>(readGuestCart);
  const [serverCart, setServerCart] = useState<CartSummary | null>(null);

  // Persist guest cart on every change
  function setGuestItems(items: GuestCartItem[]) {
    setGuestItemsState(items);
    writeGuestCart(items);
  }

  const refreshServerCart = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const cart = await apiClient.get<CartSummary>('/api/cart');
      setServerCart(cart);
    } catch {
      // leave stale data rather than crashing
    }
  }, [isAuthenticated]);

  // Load server cart on auth change
  useEffect(() => {
    if (isAuthenticated) {
      refreshServerCart();
    } else {
      setServerCart(null);
    }
  }, [isAuthenticated, refreshServerCart]);

  // ── addToCart ──────────────────────────────────────────────────────────────
  async function addToCart(item: Omit<GuestCartItem, 'quantity'>) {
    if (isAuthenticated) {
      await apiClient.post('/api/cart/items', { bookId: item.bookId, quantity: 1 });
      await refreshServerCart();
    } else {
      const existing = guestItems.find((i) => i.bookId === item.bookId);
      if (existing) {
        setGuestItems(
          guestItems.map((i) =>
            i.bookId === item.bookId ? { ...i, quantity: i.quantity + 1 } : i
          )
        );
      } else {
        setGuestItems([...guestItems, { ...item, quantity: 1 }]);
      }
    }
  }

  // ── removeItem ─────────────────────────────────────────────────────────────
  async function removeItem(bookId: number) {
    if (isAuthenticated) {
      const cartItemId = serverCart?.items.find((i) => i.bookId === bookId)?.id;
      if (cartItemId !== undefined) {
        await apiClient.delete(`/api/cart/items/${cartItemId}`);
        await refreshServerCart();
      }
    } else {
      setGuestItems(guestItems.filter((i) => i.bookId !== bookId));
    }
  }

  // ── updateQty ─────────────────────────────────────────────────────────────
  async function updateQty(bookId: number, quantity: number) {
    if (quantity < 1) {
      return removeItem(bookId);
    }
    if (isAuthenticated) {
      const cartItemId = serverCart?.items.find((i) => i.bookId === bookId)?.id;
      if (cartItemId !== undefined) {
        await apiClient.put(`/api/cart/items/${cartItemId}`, { quantity });
        await refreshServerCart();
      }
    } else {
      setGuestItems(
        guestItems.map((i) => (i.bookId === bookId ? { ...i, quantity } : i))
      );
    }
  }

  // ── clearGuestCart ────────────────────────────────────────────────────────
  function clearGuestCart() {
    setGuestItems([]);
  }

  const itemCount = isAuthenticated
    ? (serverCart?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0)
    : guestItems.reduce((sum, i) => sum + i.quantity, 0);

  return (
    <CartContext.Provider
      value={{
        itemCount,
        guestItems,
        serverCart,
        addToCart,
        removeItem,
        updateQty,
        clearGuestCart,
        refreshServerCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used inside <CartProvider>');
  return ctx;
}
