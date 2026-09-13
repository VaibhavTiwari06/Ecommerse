// TASK-FE-001 | REQ-FE-001
// CR-005/REQ-NEW-003 — AppShell updated with footer.

import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function AppShell() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-content">
        <Suspense fallback={<div className="loading-spinner">Loading…</div>}>
          <Outlet />
        </Suspense>
      </main>
      <footer className="app-footer">
        <p>© 2026 E-Bookstore. Built with Spring Boot &amp; React.</p>
      </footer>
    </div>
  );
}
