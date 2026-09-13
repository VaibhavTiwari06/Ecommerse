// TASK-FE-002 | REQ-USR-004, CR-001, TASK-AUTH-001
// RegisterPage — collects fullName, email, phoneNumber, password.
// On success: stores auth, merges guest cart, redirects to home.

import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import apiClient, { setToken } from '../api/apiClient';
import { useAuth, type AuthUser } from '../context/AuthContext';
import { useCartMerge } from '../hooks/useCartMerge';

interface RegisterResponse {
  token: string;
  userId: number;
  fullName: string;
}

export default function RegisterPage() {
  const { login } = useAuth();
  const { mergeCart } = useCartMerge();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Client-side validation mirrors server rules (REQ-USR-004 / TASK-AUTH-002)
  function validate(): string | null {
    if (!fullName.trim()) return 'Full name is required.';
    if (!email.trim()) return 'Email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Enter a valid email address.';
    if (!phoneNumber.trim()) return 'Phone number is required.';
    if (!/^\d{10}$/.test(phoneNumber)) return 'Phone number must be exactly 10 digits.';
    if (!password) return 'Password is required.';
    if (password.length < 8) return 'Password must be at least 8 characters.';
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);

    try {
      const res = await apiClient.post<RegisterResponse>('/api/auth/register', {
        fullName,
        email,
        phoneNumber,
        password,
      });

      // Set token synchronously so mergeCart API call has auth. TASK-AUTH-001
      setToken(res.token);

      try {
        await mergeCart();
      } catch {
        // merge failure is non-fatal
      }

      const authUser: AuthUser = {
        userId: res.userId,
        fullName: res.fullName,
        token: res.token,
      };

      login(authUser);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Create account</h1>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="fullName">Full name</label>
            <input
              id="fullName"
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Vaibhav Tiwari"
              required
              autoComplete="name"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email address</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="phoneNumber">Phone number</label>
            <input
              id="phoneNumber"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value.replace(/\D/g, '').slice(0, 10))}
              placeholder="10-digit mobile number"
              required
              autoComplete="tel"
              inputMode="numeric"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Minimum 8 characters"
              required
              autoComplete="new-password"
            />
          </div>

          {error && <p className="form-error" role="alert">{error}</p>}

          <button type="submit" className="btn-primary btn-full" disabled={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account?{' '}
          <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
