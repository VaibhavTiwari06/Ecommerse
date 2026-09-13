// TASK-FE-001 | REQ-FE-001
// CR-005/REQ-NEW-003 — Navbar redesigned with warm dark theme.

import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useState } from 'react';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
    }
  }

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/">
          <span className="navbar-brand-icon">📚</span>
          <span className="navbar-brand-text">E-Bookstore</span>
        </Link>
      </div>

      <form className="navbar-search" onSubmit={handleSearch}>
        <input
          type="search"
          placeholder="Search books, authors, categories…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          aria-label="Search"
        />
        <button type="submit" aria-label="Search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
        </button>
      </form>

      <div className="navbar-actions">
        <Link to="/cart" className="cart-link" aria-label="Cart">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/>
            <path d="M16 10a4 4 0 0 1-8 0"/>
          </svg>
          {itemCount > 0 && <span className="cart-badge">{itemCount}</span>}
        </Link>

        {isAuthenticated ? (
          <div className="user-menu">
            <span className="user-name">{user!.fullName.split(' ')[0]}</span>
            <Link to="/orders">Orders</Link>
            <Link to="/profile">Profile</Link>
            <button onClick={handleLogout} className="btn-link">Logout</button>
          </div>
        ) : (
          <div className="auth-links">
            <Link to="/login" className="nav-login-btn">Login</Link>
            <Link to="/register" className="nav-register-btn">Register</Link>
          </div>
        )}
      </div>
    </nav>
  );
}
