// TASK-FE-001 | REQ-FE-001
// AuthContext — stores authenticated user info and token, exposes login/logout.

import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from 'react';
import { setToken, clearToken } from '../api/apiClient';

export interface AuthUser {
  userId: number;
  fullName: string;
  token: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (user: AuthUser) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const USER_KEY = 'ebookstore_user';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    try {
      const stored = localStorage.getItem(USER_KEY);
      return stored ? (JSON.parse(stored) as AuthUser) : null;
    } catch {
      return null;
    }
  });

  // Keep token in sync whenever user changes
  useEffect(() => {
    if (user) {
      setToken(user.token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
      clearToken();
      localStorage.removeItem(USER_KEY);
    }
  }, [user]);

  function login(authUser: AuthUser) {
    // Synchronously set token so it is available in localStorage
    // before any subsequent API calls (e.g. cart merge). CR-005/BUG-001
    setToken(authUser.token);
    setUser(authUser);
  }

  function logout() {
    // Synchronously clear all user data from localStorage before
    // re-render so no race condition can expose stale data. CR-005/REQ-NEW-002
    clearToken();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: user !== null, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
