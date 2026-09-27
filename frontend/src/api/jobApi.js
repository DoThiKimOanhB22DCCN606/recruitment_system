import { apiClient } from './axiosClient';

export const jobApi = {
  list: (params) => apiClient.get('/api/v1/jobs', { params }),
  search: (params) => apiClient.get('/api/v1/jobs/search', { params }),
  getById: (id) => apiClient.get(`/api/v1/jobs/${id}`),
  getJobById: (id) => apiClient.get(`/api/v1/jobs/${id}`),
  create: (payload) => apiClient.post('/api/v1/jobs', payload),
  update: (id, payload) => apiClient.put(`/api/v1/jobs/${id}`, payload),
  remove: (id) => apiClient.delete(`/api/v1/jobs/${id}`),
  changeStatus: (id, payload) => apiClient.post(`/api/v1/jobs/${id}/status`, payload),
  statusHistory: (id) => apiClient.get(`/api/v1/jobs/${id}/status-history`),
};
