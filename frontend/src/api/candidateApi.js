import { apiClient } from './axiosClient';

export const candidateApi = {
  getProfile: () => apiClient.get('/api/v1/candidate/profile'),
  updateProfile: (payload) => apiClient.put('/api/v1/candidate/profile', payload),
  setOpenToWork: (payload) => apiClient.put('/api/v1/candidate/open-to-work', payload),
  listCvs: () => apiClient.get('/api/v1/candidate/cvs'),
  getCvs: () => apiClient.get('/api/v1/candidate/cvs'),
  uploadCv: (file, setAsDefault = false) => {
    const form = new FormData();
    form.append('file', file);
    form.append('setAsDefault', setAsDefault);
    return apiClient.post('/api/v1/candidate/cv', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  deleteCv: (id) => apiClient.delete(`/api/v1/candidate/cv/${id}`),
  setDefaultCv: (id) => apiClient.put(`/api/v1/candidate/cv/${id}/default`),
};
