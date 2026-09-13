// TASK-FE-003 | REQ-REC-002
// RecommendationStrip — horizontal scroll strip of recommended books.
// Only rendered for authenticated users with order history (non-empty list).

import { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import BookCard from './BookCard';
import type { BookSummary } from '../types';

interface Props {
  limit: number; // 8 on HomePage, 4 on CartPage
}

export default function RecommendationStrip({ limit }: Props) {
  const { isAuthenticated } = useAuth();
  const [books, setBooks] = useState<BookSummary[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return;

    apiClient
      .get<BookSummary[]>('/api/recommendations', { limit })
      .then((res) => setBooks(res))
      .catch(() => {/* silently suppress — non-critical */})
      .finally(() => setLoaded(true));
  }, [isAuthenticated, limit]);

  // REQ-REC-002 AC3: section hidden if not auth or empty list
  if (!isAuthenticated || (loaded && books.length === 0)) return null;
  if (!loaded) return null;

  return (
    <section className="rec-strip">
      <h2 className="rec-strip-title">Recommended for you</h2>
      <div className="rec-strip-scroll">
        {books.map((book) => (
          <div className="rec-strip-item" key={book.id}>
            <BookCard book={book} />
          </div>
        ))}
      </div>
    </section>
  );
}
