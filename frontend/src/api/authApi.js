import { apiClient } from './axiosClient';

export const authApi = {
  login: (credentials) => apiClient.post('/api/v1/auth/login', credentials),
  register: (data) => apiClient.post('/api/v1/auth/register', data),
  verifyEmail: (email, otp) => apiClient.post('/api/v1/auth/verify-email', { email, otp }),
  resendVerification: (email) => apiClient.post('/api/v1/auth/resend-verification', { email }),
  logout: () => apiClient.post('/api/v1/auth/logout'),
  refreshToken: () => apiClient.post('/api/v1/auth/refresh'),
  getMe: () => apiClient.get('/api/v1/users/me'),
  inviteRecruiter: (email) => apiClient.post('/api/v1/users/invite', { email, role: 'RECRUITER' }),
  createRecruiter: (email, password) => apiClient.post('/api/v1/users/recruiters', { email, password }),
  getInvitation: (token) => apiClient.get('/api/v1/invitations/accept', { params: { token } }),
  acceptInvitation: (token, password) => apiClient.post('/api/v1/invitations/accept', { token, password }),
  forgotPassword: (data) => apiClient.post('/api/v1/auth/forgot-password', data),
  resetPassword: (data) => apiClient.post('/api/v1/auth/reset-password', data),
};
