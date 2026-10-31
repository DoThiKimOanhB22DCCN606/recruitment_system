import { apiClient } from './axiosClient';

export const notificationApi = {
  getNotifications: () => apiClient.get('/api/v1/notifications'),
  markAsRead: (id) => apiClient.patch(`/api/v1/notifications/${id}/read`),
  markAllAsRead: () => apiClient.patch('/api/v1/notifications/read-all'),
  getUnreadCount: () => apiClient.get('/api/v1/notifications/unread-count'),
};
