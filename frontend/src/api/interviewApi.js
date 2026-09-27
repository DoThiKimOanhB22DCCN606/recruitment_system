import { apiClient } from './axiosClient';

export const interviewApi = {
  schedule: (data) => apiClient.post('/api/v1/interviews', data),
  getInterviews: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return apiClient.get(`/api/v1/interviews${qs ? '?' + qs : ''}`);
  },
  getInterviewById: (id) => apiClient.get(`/api/v1/interviews/${id}`),
  update: (id, data) => apiClient.put(`/api/v1/interviews/${id}`, data),
  cancel: (id, reason) => apiClient.post(`/api/v1/interviews/${id}/cancel`, { reason }),
  submitFeedback: (id, feedback) => apiClient.post(`/api/v1/interviews/${id}/feedback`, feedback),
  getFeedbacks: (id) => apiClient.get(`/api/v1/interviews/${id}/feedback`),
};
