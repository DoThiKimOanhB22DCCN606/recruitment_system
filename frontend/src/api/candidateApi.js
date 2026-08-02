import { candidateClient } from './axiosClient';

export const candidateApi = {
  getProfile: () => candidateClient.get('/candidate/profile'),
  updateProfile: (payload) => candidateClient.put('/candidate/profile', payload),
  setOpenToWork: (payload) => candidateClient.put('/candidate/open-to-work', payload),
  listCvs: () => candidateClient.get('/candidate/cvs'),
  uploadCv: (file, setAsDefault = false) => {
    const form = new FormData();
    form.append('file', file);
    form.append('setAsDefault', setAsDefault);
    return candidateClient.post('/candidate/cv', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  deleteCv: (id) => candidateClient.delete(`/candidate/cv/${id}`),
  setDefaultCv: (id) => candidateClient.put(`/candidate/cv/${id}/default`),
};
