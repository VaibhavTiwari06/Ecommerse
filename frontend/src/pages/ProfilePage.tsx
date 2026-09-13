// TASK-FE-011 | REQ-USR-006, REQ-GFT-003
// ProfilePage — user info, gift point balance, saved delivery addresses.

import { useEffect, useState, type FormEvent } from 'react';
import apiClient from '../api/apiClient';
import type { UserProfile, AddressItem } from '../types';

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('');
}

export default function ProfilePage() {
  const [profile, setProfile]   = useState<UserProfile | null>(null);
  const [addresses, setAddresses] = useState<AddressItem[]>([]);
  const [loading, setLoading]   = useState(true);

  const [showForm,    setShowForm]    = useState(false);
  const [addrLabel,   setAddrLabel]   = useState('');
  const [addrStreet,  setAddrStreet]  = useState('');
  const [addrCity,    setAddrCity]    = useState('');
  const [addrState,   setAddrState]   = useState('');
  const [addrPincode, setAddrPincode] = useState('');
  const [addrDefault, setAddrDefault] = useState(false);
  const [addrSaving,  setAddrSaving]  = useState(false);
  const [addrError,   setAddrError]   = useState('');

  useEffect(() => {
    Promise.all([
      apiClient.get<UserProfile>('/api/user/profile'),
      apiClient.get<AddressItem[]>('/api/user/addresses'),
    ])
      .then(([p, a]) => { setProfile(p); setAddresses(a); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  async function handleSaveAddress(e: FormEvent) {
    e.preventDefault();
    setAddrError('');

    if (!addrStreet.trim() || !addrCity.trim() || !addrState.trim() || !addrPincode.trim()) {
      setAddrError('Street, city, state and pincode are required.');
      return;
    }
    if (!/^\d{6}$/.test(addrPincode)) {
      setAddrError('Pincode must be exactly 6 digits.');
      return;
    }

    setAddrSaving(true);
    try {
      const saved = await apiClient.post<AddressItem>('/api/user/addresses', {
        label:     addrLabel.trim() || null,
        street:    addrStreet.trim(),
        city:      addrCity.trim(),
        state:     addrState.trim(),
        pincode:   addrPincode.trim(),
        isDefault: addrDefault,
      });

      setAddresses((prev) => [...prev, saved]);
      setShowForm(false);
      setAddrLabel(''); setAddrStreet(''); setAddrCity('');
      setAddrState(''); setAddrPincode(''); setAddrDefault(false);
    } catch (err: unknown) {
      setAddrError(err instanceof Error ? err.message : 'Failed to save address.');
    } finally {
      setAddrSaving(false);
    }
  }

  if (loading) return <div className="loading-spinner">Loading profile…</div>;

  if (!profile) {
    return (
      <div className="page">
        <p className="empty-state">Failed to load profile. Please try again.</p>
      </div>
    );
  }

  return (
    <div className="page">

      {/* ── Profile hero ─────────────────────────────────────────────────── */}
      <div className="profile-hero">
        <div className="profile-avatar">
          <span className="profile-avatar-text">{getInitials(profile.fullName)}</span>
        </div>
        <div className="profile-hero-info">
          <h1 className="profile-user-name">{profile.fullName}</h1>
          <div className="profile-user-meta">
            <span className="profile-user-meta-item">
              <span className="profile-meta-icon" aria-hidden="true">✉</span>
              {profile.email}
            </span>
            <span className="profile-user-meta-sep" aria-hidden="true">·</span>
            <span className="profile-user-meta-item">
              <span className="profile-meta-icon" aria-hidden="true">✆</span>
              {profile.phoneNumber}
            </span>
          </div>
        </div>
      </div>

      <div className="profile-layout">

        {/* ── Left column ──────────────────────────────────────────────── */}
        <div className="profile-main">

          {/* Account information */}
          <section className="profile-section">
            <div className="profile-section-header">
              <h2 className="profile-section-title">Account information</h2>
            </div>
            <div className="profile-info-list">
              <div className="profile-info-row">
                <span className="profile-info-label">Full name</span>
                <span className="profile-info-value">{profile.fullName}</span>
              </div>
              <div className="profile-info-row">
                <span className="profile-info-label">Email address</span>
                <span className="profile-info-value">{profile.email}</span>
              </div>
              <div className="profile-info-row profile-info-row--last">
                <span className="profile-info-label">Phone number</span>
                <span className="profile-info-value">{profile.phoneNumber}</span>
              </div>
            </div>
          </section>

          {/* Delivery addresses */}
          <section className="profile-section">
            <div className="profile-section-header">
              <h2 className="profile-section-title">Delivery addresses</h2>
              {!showForm && (
                <button
                  className="btn-secondary profile-add-addr-btn"
                  onClick={() => setShowForm(true)}
                >
                  + Add address
                </button>
              )}
            </div>

            {addresses.length === 0 && !showForm && (
              <div className="profile-addr-empty">
                <div className="profile-addr-empty-icon">📍</div>
                <p className="profile-addr-empty-title">No saved addresses</p>
                <p className="profile-addr-empty-sub">Add one to speed up checkout.</p>
              </div>
            )}

            {addresses.length > 0 && (
              <div className="profile-addr-grid">
                {addresses.map((addr) => (
                  <div
                    key={addr.id}
                    className={`profile-addr-card${addr.isDefault ? ' profile-addr-card--default' : ''}`}
                  >
                    <div className="profile-addr-card-top">
                      <span className={`profile-addr-label${!addr.label ? ' profile-addr-label--generic' : ''}`}>
                        {addr.label || 'Address'}
                      </span>
                      {addr.isDefault && (
                        <span className="badge badge-success">Default</span>
                      )}
                    </div>
                    <p className="profile-addr-street">{addr.street}</p>
                    <p className="profile-addr-city">{addr.city}, {addr.state}</p>
                    <p className="profile-addr-pincode">{addr.pincode}</p>
                  </div>
                ))}
              </div>
            )}

            {/* Add address form */}
            {showForm && (
              <form className="addr-form" onSubmit={handleSaveAddress} noValidate>
                <h3 className="addr-form-title">New delivery address</h3>

                <div className="form-group">
                  <label htmlFor="pAddrLabel">
                    Label <span className="profile-form-optional">(optional)</span>
                  </label>
                  <input
                    id="pAddrLabel" type="text" value={addrLabel}
                    onChange={(e) => setAddrLabel(e.target.value)}
                    placeholder="e.g. Home, Office"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="pAddrStreet">Street address</label>
                  <input
                    id="pAddrStreet" type="text" value={addrStreet}
                    onChange={(e) => setAddrStreet(e.target.value)}
                    placeholder="Flat / building, street name"
                    required
                  />
                </div>

                <div className="addr-form-row">
                  <div className="form-group">
                    <label htmlFor="pAddrCity">City</label>
                    <input
                      id="pAddrCity" type="text" value={addrCity}
                      onChange={(e) => setAddrCity(e.target.value)} required
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="pAddrState">State</label>
                    <input
                      id="pAddrState" type="text" value={addrState}
                      onChange={(e) => setAddrState(e.target.value)} required
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="pAddrPincode">Pincode</label>
                    <input
                      id="pAddrPincode" type="text" value={addrPincode}
                      onChange={(e) => setAddrPincode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      inputMode="numeric" placeholder="6 digits" required
                    />
                  </div>
                </div>

                <label className="filter-checkbox-label">
                  <input
                    type="checkbox" checked={addrDefault}
                    onChange={(e) => setAddrDefault(e.target.checked)}
                  />
                  Set as default delivery address
                </label>

                {addrError && <p className="form-error">{addrError}</p>}

                <div className="addr-form-actions">
                  <button type="submit" className="btn-primary" disabled={addrSaving}>
                    {addrSaving ? 'Saving…' : 'Save address'}
                  </button>
                  <button
                    type="button" className="btn-secondary"
                    onClick={() => { setShowForm(false); setAddrError(''); }}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </section>
        </div>

        {/* ── Right column — gift points (unchanged) ────────────────────── */}
        <aside className="profile-sidebar">
          <div className="profile-gift-card">
            <h2 className="profile-gift-title">Gift points</h2>

            <div className="profile-gift-balance">
              {profile.giftPointBalance}
            </div>
            <p className="profile-gift-label">points available</p>

            <div className="profile-gift-value">
              Worth <strong>₹{(profile.giftPointBalance * 2).toFixed(2)}</strong> off your next order
            </div>

            <div className="profile-gift-rules">
              <p>• Earn 1 point for every ₹50 spent</p>
              <p>• 1 point = ₹2 discount at checkout</p>
              <p>• Points never expire</p>
            </div>
          </div>
        </aside>

      </div>
    </div>
  );
}
