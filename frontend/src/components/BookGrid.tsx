// TASK-FE-003 | REQ-CAT-001
// BookGrid — paginated grid of BookCards with prev/next controls.

import BookCard from './BookCard';
import type { BookSummary, PagedResponse } from '../types';

interface Props {
  data: PagedResponse<BookSummary> | null;
  loading: boolean;
  onPageChange: (page: number) => void;
}

export default function BookGrid({ data, loading, onPageChange }: Props) {
  if (loading) {
    return <div className="loading-spinner">Loading books…</div>;
  }

  if (!data || data.content.length === 0) {
    return <p className="empty-state">No books found.</p>;
  }

  return (
    <div className="book-grid-container">
      <div className="book-grid">
        {data.content.map((book) => (
          <BookCard key={book.id} book={book} />
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="pagination">
          <button
            className="btn-secondary"
            disabled={data.page === 0}
            onClick={() => onPageChange(data.page - 1)}
          >
            ← Prev
          </button>

          <span className="pagination-info">
            Page {data.page + 1} of {data.totalPages}
            <span className="pagination-total"> ({data.totalElements} books)</span>
          </span>

          <button
            className="btn-secondary"
            disabled={data.page >= data.totalPages - 1}
            onClick={() => onPageChange(data.page + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
