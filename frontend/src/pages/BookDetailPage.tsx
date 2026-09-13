// TASK-FE-005 | REQ-CAT-004, REQ-CAT-005, REQ-CAT-006
// BookDetailPage — full book info + availability + related books + Add to Cart.

import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import BookCard from '../components/BookCard';
import { useCart } from '../context/CartContext';
import type { BookDetail, BookSummary } from '../types';

export default function BookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart } = useCart();

  const [book, setBook] = useState<BookDetail | null>(null);
  const [related, setRelated] = useState<BookSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [addedToCart, setAddedToCart] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setNotFound(false);
    setAddedToCart(false);

    // Fetch detail and related in parallel
    Promise.all([
      apiClient.get<BookDetail>(`/api/books/${id}`),
      apiClient.get<BookSummary[]>(`/api/books/${id}/related`),
    ])
      .then(([bookData, relatedData]) => {
        setBook(bookData);
        setRelated(relatedData);
      })
      .catch((err: Error) => {
        if (err.message.includes('404') || err.message.toLowerCase().includes('not found')) {
          setNotFound(true);
        }
        // other errors — leave book null, page shows generic error
      })
      .finally(() => setLoading(false));
  }, [id]);

  async function handleAddToCart() {
    if (!book) return;
    await addToCart({
      bookId: book.id,
      title: book.title,
      coverImageUrl: book.coverImageUrl,
      price: book.price,
    });
    setAddedToCart(true);
    // Reset feedback after 2 s
    setTimeout(() => setAddedToCart(false), 2000);
  }

  // ── Loading ──────────────────────────────────────────────────────────────
  if (loading) {
    return <div className="loading-spinner">Loading…</div>;
  }

  // ── 404 ──────────────────────────────────────────────────────────────────
  if (notFound) {
    return (
      <div className="page">
        <div className="detail-not-found">
          <h1>Book not found</h1>
          <p>The book you're looking for doesn't exist or has been removed.</p>
          <button className="btn-primary" onClick={() => navigate('/')}>
            Back to catalogue
          </button>
        </div>
      </div>
    );
  }

  // ── Error (non-404) ──────────────────────────────────────────────────────
  if (!book) {
    return (
      <div className="page">
        <p className="empty-state">Failed to load book. Please try again.</p>
      </div>
    );
  }

  return (
    <div className="page">
      {/* ── Main detail layout ── */}
      <div className="detail-layout">

        {/* Cover */}
        <div className="detail-cover">
          <img
            src={book.coverImageUrl}
            alt={book.title}
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).src =
                'https://covers.openlibrary.org/b/id/0-M.jpg';
            }}
          />
        </div>

        {/* Info */}
        <div className="detail-info">
          <p className="detail-category">{book.category}</p>
          <h1 className="detail-title">{book.title}</h1>
          <p className="detail-authors">by {book.authors}</p>
          <p className="detail-price">₹{book.price.toFixed(2)}</p>

          {/* Availability — REQ-CAT-006 */}
          <div className="detail-availability">
            {book.inStock ? (
              <>
                <span className="badge badge-success">In Stock</span>
                {book.tentativeDeliveryDate && (
                  <span className="detail-delivery">
                    Estimated delivery by{' '}
                    <strong>
                      {new Date(book.tentativeDeliveryDate).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'long',
                        year: 'numeric',
                      })}
                    </strong>
                  </span>
                )}
              </>
            ) : (
              <span className="badge badge-muted">Out of Stock</span>
            )}
          </div>

          {/* Add to Cart */}
          {book.inStock && (
            <button
              className={`btn-primary detail-cart-btn ${addedToCart ? 'btn-added' : ''}`}
              onClick={handleAddToCart}
              disabled={addedToCart}
            >
              {addedToCart ? '✓ Added to cart' : 'Add to cart'}
            </button>
          )}

          {/* Metadata table — REQ-CAT-004 */}
          <table className="detail-meta-table">
            <tbody>
              <tr>
                <th>Publisher</th>
                <td>{book.publisher}</td>
              </tr>
              {book.publishedDate && (
                <tr>
                  <th>Published</th>
                  <td>{book.publishedDate}</td>
                </tr>
              )}
              {book.pageCount && (
                <tr>
                  <th>Pages</th>
                  <td>{book.pageCount}</td>
                </tr>
              )}
              <tr>
                <th>Language</th>
                <td>{book.language.toUpperCase()}</td>
              </tr>
              <tr>
                <th>ISBN</th>
                <td>{book.isbn}</td>
              </tr>
              {book.stockQuantity > 0 && (
                <tr>
                  <th>Stock</th>
                  <td>{book.stockQuantity} available</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Description */}
      <div className="detail-description">
        <h2>About this book</h2>
        <p>{book.description}</p>
      </div>

      {/* Related books — REQ-CAT-005 */}
      {related.length > 0 && (
        <section className="detail-related">
          <h2 className="detail-related-title">Related books</h2>
          <div className="detail-related-grid">
            {related.map((r) => (
              <BookCard key={r.id} book={r} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
