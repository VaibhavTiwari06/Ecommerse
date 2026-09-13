// TASK-FE-004 | REQ-SRC-001, REQ-SRC-002
// SearchPage — full-text search results + filter panel.
//
// Flow:
//   • ?q= in URL drives the search term (set by Navbar search form).
//   • Results from GET /api/books/search?q= when term is present (REQ-SRC-001).
//   • Filter panel (category, publisher, price, inStock) re-queries GET /api/books
//     with matching params (REQ-SRC-002).
//   • When both term and filters are active, filters are applied via GET /api/books
//     since the backend /search endpoint does not accept filter params.
//   • Changing filters or search resets to page 0.

import { useEffect, useState, useCallback, type FormEvent } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import BookGrid from '../components/BookGrid';
import type { Category, PagedResponse, BookSummary, Publisher } from '../types';

const PAGE_SIZE = 20;

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // ── URL-driven search term ─────────────────────────────────────────────
  const urlQuery = searchParams.get('q') ?? '';
  const [inputValue, setInputValue] = useState(urlQuery);

  // Keep input in sync when URL changes (e.g. Navbar search fires again)
  useEffect(() => {
    setInputValue(urlQuery);
  }, [urlQuery]);

  // ── Filter state ───────────────────────────────────────────────────────
  const [categories, setCategories] = useState<Category[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterPublisher, setFilterPublisher] = useState('');
  const [filterMinPrice, setFilterMinPrice] = useState('');
  const [filterMaxPrice, setFilterMaxPrice] = useState('');
  const [filterInStock, setFilterInStock] = useState(false);

  // ── Results state ──────────────────────────────────────────────────────
  const [books, setBooks] = useState<PagedResponse<BookSummary> | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);

  const hasFilters =
    !!filterCategory || !!filterPublisher || !!filterMinPrice || !!filterMaxPrice || filterInStock;

  // Load sidebar data once
  useEffect(() => {
    Promise.all([
      apiClient.get<Category[]>('/api/categories'),
      apiClient.get<Publisher[]>('/api/publishers'),
    ])
      .then(([cats, pubs]) => {
        setCategories(cats);
        setPublishers(pubs);
      })
      .catch(() => {/* non-fatal */});
  }, []);

  // ── Fetch results ──────────────────────────────────────────────────────
  const fetchResults = useCallback(() => {
    // Nothing to do if no search term and no filters
    if (!urlQuery.trim() && !hasFilters) {
      setBooks(null);
      setLoading(false);
      return;
    }

    setLoading(true);

    // When filters are active (with or without a term) → GET /api/books (paginated)
    // When only a term (no filters) → GET /api/books/search (plain array, wrapped below)
    if (hasFilters) {
      const params: Record<string, unknown> = { page, size: PAGE_SIZE };
      if (filterCategory) params.category = filterCategory;
      if (filterPublisher) params.publisher = filterPublisher;
      if (filterMinPrice) params.minPrice = filterMinPrice;
      if (filterMaxPrice) params.maxPrice = filterMaxPrice;
      if (filterInStock) params.inStock = true;
      apiClient.get<PagedResponse<BookSummary>>('/api/books', params)
        .then(setBooks)
        .catch(() => setBooks(null))
        .finally(() => setLoading(false));
    } else {
      // CR-005/BUG-004: /api/books/search returns a plain array, not PagedResponse.
      // Wrap it in a synthetic PagedResponse so BookGrid receives the expected shape.
      apiClient.get<BookSummary[]>('/api/books/search', {
        q: urlQuery.trim(),
        size: PAGE_SIZE,
      })
        .then((result) => {
          setBooks({
            content: result,
            page: 0,
            size: result.length,
            totalElements: result.length,
            totalPages: 1,
          });
        })
        .catch(() => setBooks(null))
        .finally(() => setLoading(false));
    }
  }, [urlQuery, hasFilters, filterCategory, filterPublisher, filterMinPrice, filterMaxPrice, filterInStock, page]);

  useEffect(() => {
    fetchResults();
  }, [fetchResults]);

  // ── Search bar submit (updates URL → triggers re-render) ───────────────
  function handleSearchSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = inputValue.trim();
    if (!trimmed) return;
    setPage(0);
    navigate(`/search?q=${encodeURIComponent(trimmed)}`);
  }

  // ── Filter change helpers (reset page) ────────────────────────────────
  function applyFilter(fn: () => void) {
    fn();
    setPage(0);
  }

  function clearAllFilters() {
    setFilterCategory('');
    setFilterPublisher('');
    setFilterMinPrice('');
    setFilterMaxPrice('');
    setFilterInStock(false);
    setPage(0);
  }

  // ── Derived heading ────────────────────────────────────────────────────
  function resultHeading() {
    if (!urlQuery && !hasFilters) return null;
    if (urlQuery && !hasFilters) return `Results for "${urlQuery}"`;
    if (!urlQuery && hasFilters) return 'Filtered results';
    return `Results for "${urlQuery}" (filtered)`;
  }

  const heading = resultHeading();

  return (
    <div className="page">
      {/* ── Inline search bar ── */}
      <form className="search-bar-inline" onSubmit={handleSearchSubmit}>
        <input
          type="search"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder="Search books, authors, categories, publishers, ISBN…"
          aria-label="Search"
          autoFocus
        />
        <button type="submit" className="btn-primary">Search</button>
      </form>

      <div className="catalogue-layout" style={{ marginTop: 24 }}>
        {/* ── Filter panel ── */}
        <aside className="sidebar">
          <div className="sidebar-filter-header">
            <span className="sidebar-heading" style={{ marginBottom: 0 }}>Filters</span>
            {hasFilters && (
              <button className="btn-link-muted" onClick={clearAllFilters}>
                Clear all
              </button>
            )}
          </div>

          {/* Category */}
          <section className="sidebar-section" style={{ marginTop: 16 }}>
            <h3 className="sidebar-heading">Category</h3>
            <ul className="sidebar-list">
              {categories.map((cat) => (
                <li key={cat.id}>
                  <button
                    className={`sidebar-item ${filterCategory === cat.name ? 'sidebar-item--active' : ''}`}
                    onClick={() =>
                      applyFilter(() =>
                        setFilterCategory(filterCategory === cat.name ? '' : cat.name)
                      )
                    }
                  >
                    {cat.name}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          {/* Publisher */}
          <section className="sidebar-section">
            <h3 className="sidebar-heading">Publisher</h3>
            <ul className="sidebar-list">
              {publishers.map((pub) => (
                <li key={pub.id}>
                  <button
                    className={`sidebar-item ${filterPublisher === pub.name ? 'sidebar-item--active' : ''}`}
                    onClick={() =>
                      applyFilter(() =>
                        setFilterPublisher(filterPublisher === pub.name ? '' : pub.name)
                      )
                    }
                  >
                    {pub.name}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          {/* Price range */}
          <section className="sidebar-section">
            <h3 className="sidebar-heading">Price (₹)</h3>
            <div className="filter-price-row">
              <input
                type="number"
                min={0}
                placeholder="Min"
                value={filterMinPrice}
                onChange={(e) => applyFilter(() => setFilterMinPrice(e.target.value))}
                className="filter-price-input"
              />
              <span className="filter-price-sep">–</span>
              <input
                type="number"
                min={0}
                placeholder="Max"
                value={filterMaxPrice}
                onChange={(e) => applyFilter(() => setFilterMaxPrice(e.target.value))}
                className="filter-price-input"
              />
            </div>
          </section>

          {/* In stock */}
          <section className="sidebar-section">
            <label className="filter-checkbox-label">
              <input
                type="checkbox"
                checked={filterInStock}
                onChange={(e) => applyFilter(() => setFilterInStock(e.target.checked))}
              />
              In stock only
            </label>
          </section>
        </aside>

        {/* ── Results ── */}
        <section className="catalogue-main">
          {heading && (
            <div className="catalogue-header">
              <h1 className="catalogue-title">{heading}</h1>
            </div>
          )}

          {!urlQuery.trim() && !hasFilters ? (
            <p className="empty-state">Enter a search term or apply a filter to see results.</p>
          ) : (
            <BookGrid data={books} loading={loading} onPageChange={setPage} />
          )}
        </section>
      </div>
    </div>
  );
}
