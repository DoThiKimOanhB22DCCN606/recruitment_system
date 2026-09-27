import { apiClient } from './axiosClient';

export const authApi = {
  login: (credentials) => apiClient.post('/api/v1/auth/login', credentials),
  register: (data) => apiClient.post('/api/v1/auth/register', data),
  verifyEmail: (token) => apiClient.post('/api/v1/auth/verify-email', { token }),
  resendVerification: (email) => apiClient.post('/api/v1/auth/resend-verification', { email }),
  logout: () => apiClient.post('/api/v1/auth/logout'),
  refreshToken: () => apiClient.post('/api/v1/auth/refresh'),
  getMe: () => apiClient.get('/api/v1/auth/me'),
  forgotPassword: (data) => apiClient.post('/api/v1/auth/forgot-password', data),
  resetPassword: (data) => apiClient.post('/api/v1/auth/reset-password', data),
};
