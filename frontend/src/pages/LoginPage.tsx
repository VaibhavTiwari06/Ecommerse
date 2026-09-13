// TASK-FE-002 | REQ-USR-005, CR-001
// LoginPage — accepts email or phone number + password.
// On success: stores auth, merges guest cart, redirects to intended page.

import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import apiClient, { setToken } from '../api/apiClient';
import { useAuth, type AuthUser } from '../context/AuthContext';
import { useCartMerge } from '../hooks/useCartMerge';

interface LoginResponse {
  token: string;
  userId: number;
  fullName: string;
}

export default function LoginPage() {
  const { login } = useAuth();
  const { mergeCart } = useCartMerge();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: Location })?.from?.pathname ?? '/';

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await apiClient.post<LoginResponse>('/api/auth/login', {
        identifier,
        password,
      });

      // CR-005/BUG-001 — correct order to avoid merge race condition:
      // 1. Set token synchronously so the merge API call has auth.
      // 2. Merge guest cart BEFORE flipping isAuthenticated — no race
      //    with CartContext's refreshServerCart useEffect.
      // 3. login() flips auth state — CartContext refreshes already-merged cart.
      setToken(res.token);

      // REQ-USR-003 — merge guest cart before login() flips isAuthenticated
      try {
        await mergeCart();
      } catch {
        // merge failure is non-fatal — user is already logged in
      }

      const authUser: AuthUser = {
        userId: res.userId,
        fullName: res.fullName,
        token: res.token,
      };
      login(authUser);

      navigate(from, { replace: true });
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Sign in</h1>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="identifier">Email or phone number</label>
            <input
              id="identifier"
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="email@example.com or 9876543210"
              required
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Your password"
              required
              autoComplete="current-password"
            />
          </div>

          {error && <p className="form-error" role="alert">{error}</p>}

          <button type="submit" className="btn-primary btn-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="auth-footer">
          Don't have an account?{' '}
          <Link to="/register">Create one</Link>
        </p>
      </div>
    </div>
  );
}
