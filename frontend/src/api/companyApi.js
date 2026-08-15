import { companyClient } from './axiosClient';

export const companyApi = {
  getProfile: () => companyClient.get('/company'),
  getById: (id) => companyClient.get(`/company/${id}`),
  updateProfile: (payload) => companyClient.put('/company', payload),
  uploadLogo: (file) => {
    const form = new FormData();
    form.append('file', file);
    return companyClient.post('/company/logo', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  deleteLogo: () => companyClient.delete('/company/logo'),
};
