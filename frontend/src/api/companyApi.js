import { apiClient } from './axiosClient';

export const companyApi = {
  getProfile: () => apiClient.get('/api/v1/company'),
  getById: (id) => apiClient.get(`/api/v1/company/${id}`),
  getBrandByTenant: (tenantId) => apiClient.get(`/api/v1/companies/tenant/${tenantId}/branding`),
  updateProfile: (payload) => apiClient.put('/api/v1/company', payload),
  uploadLogo: (file) => {
    const form = new FormData();
    form.append('file', file);
    return apiClient.post('/api/v1/company/logo', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getLogo: () => apiClient.get('/api/v1/company/logo', { responseType: 'blob' }),
  deleteLogo: () => apiClient.delete('/api/v1/company/logo'),
};
