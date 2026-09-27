import { apiClient } from './axiosClient';

export const offerApi = {
  createDraft: (data) => apiClient.post('/api/v1/offers', data),
  getOffers: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return apiClient.get(`/api/v1/offers${qs ? '?' + qs : ''}`);
  },
  getOfferById: (id) => apiClient.get(`/api/v1/offers/${id}`),
  update: (id, data) => apiClient.put(`/api/v1/offers/${id}`, data),
  sendOffer: (id) => apiClient.post(`/api/v1/offers/${id}/send`),
  acceptOffer: (id) => apiClient.post(`/api/v1/offers/${id}/accept`),
  declineOffer: (id) => apiClient.post(`/api/v1/offers/${id}/decline`),
  withdrawOffer: (id) => apiClient.post(`/api/v1/offers/${id}/withdraw`),
};
