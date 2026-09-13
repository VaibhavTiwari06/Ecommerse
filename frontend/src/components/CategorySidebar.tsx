// TASK-FE-003 | REQ-CAT-002, REQ-CAT-003
// CategorySidebar — lists categories and publishers for filtering.
// Active filter is highlighted. Clicking the active filter clears it.

import type { Category, Publisher } from '../types';

interface Props {
  categories: Category[];
  publishers: Publisher[];
  activeCategory: string;
  activePublisher: string;
  onCategoryChange: (name: string) => void;
  onPublisherChange: (name: string) => void;
}

export default function CategorySidebar({
  categories,
  publishers,
  activeCategory,
  activePublisher,
  onCategoryChange,
  onPublisherChange,
}: Props) {
  return (
    <aside className="sidebar">
      <section className="sidebar-section">
        <h3 className="sidebar-heading">Categories</h3>
        <ul className="sidebar-list">
          {categories.map((cat) => (
            <li key={cat.id}>
              <button
                className={`sidebar-item ${activeCategory === cat.name ? 'sidebar-item--active' : ''}`}
                onClick={() =>
                  onCategoryChange(activeCategory === cat.name ? '' : cat.name)
                }
              >
                {cat.name}
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className="sidebar-section">
        <h3 className="sidebar-heading">Publishers</h3>
        <ul className="sidebar-list">
          {publishers.map((pub) => (
            <li key={pub.id}>
              <button
                className={`sidebar-item ${activePublisher === pub.name ? 'sidebar-item--active' : ''}`}
                onClick={() =>
                  onPublisherChange(activePublisher === pub.name ? '' : pub.name)
                }
              >
                {pub.name}
              </button>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  );
}
