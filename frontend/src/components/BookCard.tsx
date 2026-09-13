// TASK-FE-003 | REQ-CAT-001, REQ-CAT-006
// BookCard — single book tile used in grids and strips throughout the app.

import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import type { BookSummary } from '../types';

interface Props {
  book: BookSummary;
}

export default function BookCard({ book }: Props) {
  const { addToCart } = useCart();

  function handleAddToCart(e: React.MouseEvent) {
    e.preventDefault(); // don't navigate when clicking the button inside the link
    addToCart({
      bookId: book.id,
      title: book.title,
      coverImageUrl: book.coverImageUrl,
      price: book.price,
    });
  }

  return (
    <Link to={`/books/${book.id}`} className="book-card">
      <div className="book-card-cover">
        <img
          src={book.coverImageUrl}
          alt={book.title}
          loading="lazy"
          onError={(e) => {
            (e.currentTarget as HTMLImageElement).src =
              'https://covers.openlibrary.org/b/id/0-M.jpg';
          }}
        />
      </div>

      <div className="book-card-body">
        <p className="book-card-title">{book.title}</p>
        <p className="book-card-authors">{book.authors}</p>
        <p className="book-card-price">₹{book.price.toFixed(2)}</p>

        <div className="book-card-footer">
          {book.inStock ? (
            <span className="badge badge-success">In Stock</span>
          ) : (
            <span className="badge badge-muted">Out of Stock</span>
          )}

          {book.inStock && (
            <button
              className="btn-add-cart"
              onClick={handleAddToCart}
              aria-label={`Add ${book.title} to cart`}
            >
              + Cart
            </button>
          )}
        </div>
      </div>
    </Link>
  );
}
