import { apiClient } from './axiosClient';

export const notificationApi = {
  getNotifications: (page = 0, size = 20) => apiClient.get(`/api/v1/notifications?page=${page}&size=${size}`),
  markAsRead: (id) => apiClient.put(`/api/v1/notifications/${id}/read`),
  markAllAsRead: () => apiClient.put('/api/v1/notifications/read-all'),
  getUnreadCount: () => apiClient.get('/api/v1/notifications/unread-count'),
};
