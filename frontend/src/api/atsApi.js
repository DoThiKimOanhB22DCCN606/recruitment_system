import { apiClient } from './axiosClient';

export const atsApi = {
  apply: (data, idempotencyKey) => {
    const key =
      idempotencyKey ||
      (typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : '00000000-0000-4000-8000-' + Date.now().toString(16).padStart(12, '0'));
    return apiClient.post('/api/v1/applications', data, {
      headers: { 'Idempotency-Key': key },
    });
  },
  getBoard: (jobId) => apiClient.get(`/api/v1/jobs/${jobId}/board`),
  getJobStages: (jobId) => apiClient.get(`/api/v1/jobs/${jobId}/stages`),
  createStage: (jobId, data) => apiClient.post(`/api/v1/jobs/${jobId}/stages`, data),
  moveStage: (applicationId, data) => apiClient.patch(`/api/v1/applications/${applicationId}/stage`, data),
  withdraw: (applicationId, reason) => apiClient.post(`/api/v1/applications/${applicationId}/withdraw`, { reason }),
  getCandidateApplications: (page = 0, size = 20) => apiClient.get(`/api/v1/applications/my?page=${page}&size=${size}`),
  getApplicationHistory: (applicationId) => apiClient.get(`/api/v1/applications/${applicationId}/history`),
};
