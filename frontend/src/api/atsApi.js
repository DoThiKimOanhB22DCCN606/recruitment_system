import { apiClient } from './axiosClient';

const toAtsJobId = (jobId) => {
  const value = String(jobId);
  return /^\d+$/.test(value)
    ? `00000000-0000-0000-0000-${value.padStart(12, '0')}`
    : value;
};

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
  getBoard: (jobId) => apiClient.get(`/api/v1/employer/jobs/${toAtsJobId(jobId)}/ats`),
  getApplicationCv: (applicationId) => apiClient.get(`/api/v1/applications/${applicationId}/cv`, { responseType: 'blob' }),
  moveStage: (applicationId, data) => apiClient.patch(`/api/v1/applications/${applicationId}/stage`, data),
  withdraw: (applicationId, reason) => apiClient.post(`/api/v1/applications/${applicationId}/withdraw`, { reason }),
  getCandidateApplications: (page = 0, size = 20) => apiClient.get(`/api/v1/candidate/applications?page=${page}&size=${size}`),
  getApplicationHistory: (applicationId) => apiClient.get(`/api/v1/applications/${applicationId}/stage-history`),
};
