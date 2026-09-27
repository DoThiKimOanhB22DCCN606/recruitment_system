import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('rms_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        try {
          const profile = await authApi.getMe();
          if (profile) {
            setUser(profile);
            localStorage.setItem('rms_user', JSON.stringify(profile));
          }
        } catch (err) {
          // Token expired or invalid
          localStorage.removeItem('accessToken');
          localStorage.removeItem('rms_user');
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    if (res.accessToken) {
      localStorage.setItem('accessToken', res.accessToken);
    }
    const u = res.user || {};
    const rawRole = u.role || res.role || '';
    const userData = {
      userId: u.id || res.userId,
      email: u.email || res.email,
      role: rawRole.replace(/^ROLE_/, ''),
      tenantId: u.tenantId || res.tenantId,
      fullName: u.fullName || res.fullName || (u.email || res.email)?.split('@')[0],
    };
    setUser(userData);
    localStorage.setItem('rms_user', JSON.stringify(userData));
    return userData;
  };

  const register = async (data) => {
    const res = await authApi.register(data);
    return res;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (e) {
      // ignore
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('rms_user');
      setUser(null);
    }
  };

  const hasRole = (roles) => {
    if (!user || !user.role) return false;
    const userRole = user.role.replace(/^ROLE_/, '');
    if (Array.isArray(roles)) {
      return roles.some((r) => r.replace(/^ROLE_/, '') === userRole);
    }
    return roles.replace(/^ROLE_/, '') === userRole;
  };

  const value = {
    user,
    role: user?.role?.replace(/^ROLE_/, '') || null,
    tenantId: user?.tenantId || null,
    isAuthenticated: !!user,
    loading,
    login,
    register,
    logout,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
