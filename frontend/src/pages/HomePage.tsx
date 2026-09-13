// TASK-FE-003 | REQ-CAT-001, REQ-CAT-002, REQ-CAT-003, REQ-REC-002
// CR-005/REQ-NEW-003 — Hero section + category chips added to home page.

import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import BookGrid from '../components/BookGrid';
import CategorySidebar from '../components/CategorySidebar';
import RecommendationStrip from '../components/RecommendationStrip';
import type { Category, PagedResponse, BookSummary, Publisher } from '../types';

const PAGE_SIZE = 20;

export default function HomePage() {
  const [books, setBooks] = useState<PagedResponse<BookSummary> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Active filters
  const [activeCategory, setActiveCategory] = useState('');
  const [activePublisher, setActivePublisher] = useState('');
  const [page, setPage] = useState(0);

  // Load sidebar data once on mount
  useEffect(() => {
    Promise.all([
      apiClient.get<Category[]>('/api/categories'),
      apiClient.get<Publisher[]>('/api/publishers'),
    ]).then(([cats, pubs]) => {
      setCategories(cats);
      setPublishers(pubs);
    }).catch(() => {/* non-fatal — sidebar stays empty */});
  }, []);

  // Reload books whenever filters or page change
  const fetchBooks = useCallback(() => {
    setLoading(true);
    const params: Record<string, unknown> = { page, size: PAGE_SIZE };
    if (activeCategory) params.category = activeCategory;
    if (activePublisher) params.publisher = activePublisher;

    apiClient
      .get<PagedResponse<BookSummary>>('/api/books', params)
      .then(setBooks)
      .catch(() => setBooks(null))
      .finally(() => setLoading(false));
  }, [activeCategory, activePublisher, page]);

  useEffect(() => {
    fetchBooks();
  }, [fetchBooks]);

  // Reset to page 0 when filters change
  function handleCategoryChange(name: string) {
    setActiveCategory(name);
    setActivePublisher('');
    setPage(0);
  }

  function handlePublisherChange(name: string) {
    setActivePublisher(name);
    setActiveCategory('');
    setPage(0);
  }

  function clearFilters() {
    setActiveCategory('');
    setActivePublisher('');
    setPage(0);
  }

  const hasFilter = !!(activeCategory || activePublisher);

  return (
    <div className="home-page">

      {/* ── Hero Section ── */}
      {!hasFilter && (
        <section className="hero">
          <div className="hero-content">
            <p className="hero-eyebrow">Welcome to E-Bookstore</p>
            <h1 className="hero-headline">Discover Your Next<br />Great Read</h1>
            <p className="hero-sub">
              113 books across 8 categories — Fiction, Science, History &amp; more.
              Free delivery on all orders.
            </p>
            <div className="hero-actions">
              <button
                className="hero-cta"
                onClick={() => document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth' })}
              >
                Browse All Books
              </button>
              <button
                className="hero-cta-secondary"
                onClick={() => navigate('/search')}
              >
                Search Catalogue
              </button>
            </div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="hero-book-stack">
              <div className="hb hb1" />
              <div className="hb hb2" />
              <div className="hb hb3" />
              <div className="hb hb4" />
              <div className="hb hb5" />
            </div>
          </div>
        </section>
      )}

      {/* ── Recommendation Strip ── */}
      <div className="page">
        <RecommendationStrip limit={8} />

        {/* ── Category Chips ── */}
        {categories.length > 0 && (
          <div className="category-chips-row">
            <button
              className={`category-chip ${!activeCategory && !activePublisher ? 'category-chip--active' : ''}`}
              onClick={clearFilters}
            >
              All
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                className={`category-chip ${activeCategory === cat.name ? 'category-chip--active' : ''}`}
                onClick={() => handleCategoryChange(cat.name)}
              >
                {cat.name}
              </button>
            ))}
          </div>
        )}

        {/* ── Catalogue ── */}
        <div id="catalogue" className="catalogue-layout">
          <CategorySidebar
            categories={categories}
            publishers={publishers}
            activeCategory={activeCategory}
            activePublisher={activePublisher}
            onCategoryChange={handleCategoryChange}
            onPublisherChange={handlePublisherChange}
          />

          <section className="catalogue-main">
            <div className="catalogue-header">
              <h2 className="catalogue-title">
                {activeCategory || activePublisher
                  ? (activeCategory || activePublisher)
                  : 'All Books'}
              </h2>

              {hasFilter && (
                <button className="btn-link-muted" onClick={clearFilters}>
                  ✕ Clear filter
                </button>
              )}
            </div>

            <BookGrid
              data={books}
              loading={loading}
              onPageChange={setPage}
            />
          </section>
        </div>
      </div>
    </div>
  );
}
