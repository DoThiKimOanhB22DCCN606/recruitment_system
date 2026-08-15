import { jobClient } from './axiosClient';

export const jobApi = {
  list: (params) => jobClient.get('/jobs', { params }),
  search: (params) => jobClient.get('/jobs/search', { params }),
  getById: (id) => jobClient.get(`/jobs/${id}`),
  create: (payload) => jobClient.post('/jobs', payload),
  update: (id, payload) => jobClient.put(`/jobs/${id}`, payload),
  remove: (id) => jobClient.delete(`/jobs/${id}`),
  changeStatus: (id, payload) => jobClient.post(`/jobs/${id}/status`, payload),
  statusHistory: (id) => jobClient.get(`/jobs/${id}/status-history`),
};
