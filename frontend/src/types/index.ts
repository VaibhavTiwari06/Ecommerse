// Shared TypeScript interfaces — derived from API design v1.0

// ── Catalogue ────────────────────────────────────────────────────────────────

export interface BookSummary {
  id: number;
  title: string;
  authors: string;
  coverImageUrl: string;
  price: number;
  category: string;
  publisher: string;
  inStock: boolean;
}

export interface BookDetail extends BookSummary {
  isbn: string;
  description: string;
  publishedDate: string | null;
  pageCount: number | null;
  language: string;
  stockQuantity: number;
  tentativeDeliveryDate: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Category {
  id: number;
  name: string;
}

export interface Publisher {
  id: number;
  name: string;
}

// ── Cart ─────────────────────────────────────────────────────────────────────

export interface CartItem {
  id: number;
  bookId: number;
  title: string;
  coverImageUrl: string;
  price: number;
  quantity: number;
  itemTotal: number;
}

export interface CartResponse {
  items: CartItem[];
  subtotal: number;
  deliveryCharge: number;
  grandTotal: number;
}

// ── Orders ───────────────────────────────────────────────────────────────────

export interface OrderSummary {
  id: number;
  createdAt: string;
  status: string;
  itemCount: number;
  grandTotal: number;
}

export interface OrderItemDetail {
  bookId: number;
  titleSnapshot: string;
  priceSnapshot: number;
  quantity: number;
  itemTotal: number;
}

export interface OrderDetail {
  id: number;
  createdAt: string;
  status: string;
  items: OrderItemDetail[];
  subtotal: number;
  deliveryCharge: number;
  giftPointsRedeemed: number;
  giftPointDiscount: number;
  grandTotal: number;
  tentativeDeliveryDate: string;
  deliveryAddress: AddressItem;
}

// ── Addresses ────────────────────────────────────────────────────────────────

export interface AddressItem {
  id: number;
  label: string | null;
  street: string;
  city: string;
  state: string;
  pincode: string;
  isDefault: boolean;
}

// ── Checkout ─────────────────────────────────────────────────────────────────

export interface CheckoutSummary {
  items: CartItem[];
  subtotal: number;
  deliveryCharge: number;
  grandTotal: number;
  giftPointBalance: number;
  maxGiftPointDiscount: number;
  tentativeDeliveryDate: string;
  savedAddresses: AddressItem[];
}

// ── Payment ──────────────────────────────────────────────────────────────────

export interface PaymentResponse {
  orderId: number;
  paymentReference: string;
  paymentMethod: string;
  items: CartItem[];
  subtotal: number;
  deliveryCharge: number;
  giftPointsRedeemed: number;
  giftPointDiscount: number;
  grandTotal: number;
  giftPointsEarned: number;
  tentativeDeliveryDate: string;
  message: string;
}

// ── User ─────────────────────────────────────────────────────────────────────

export interface UserProfile {
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  giftPointBalance: number;
}

export interface GiftPointBalance {
  balance: number;
}
