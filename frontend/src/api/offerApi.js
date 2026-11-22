import { apiClient } from './axiosClient';

export const offerApi = {
  createDraft: (data) => {
    const key =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : 'offer-' + Date.now().toString(16) + '-' + Math.random().toString(16).slice(2);
    return apiClient.post('/api/v1/offers', data, {
      headers: { 'Idempotency-Key': key },
    });
  },
  getOffers: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return apiClient.get(`/api/v1/offers${qs ? '?' + qs : ''}`);
  },
  getOfferById: (id) => apiClient.get(`/api/v1/offers/${id}`),
  update: (id, data) => apiClient.put(`/api/v1/offers/${id}`, data),
  sendOffer: (id) => apiClient.post(`/api/v1/offers/${id}/send`),
  acceptOffer: (id) => apiClient.post(`/api/v1/offers/${id}/accept`),
  declineOffer: (id) => apiClient.post(`/api/v1/offers/${id}/decline`),
};
