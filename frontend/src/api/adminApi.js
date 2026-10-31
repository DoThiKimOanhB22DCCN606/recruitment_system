import { apiClient } from './axiosClient';

const adminBase = '/api/v1/admin';

export const adminApi = {
  getCompanyRegistrations: () => apiClient.get(`${adminBase}/company-registrations`),
  approveCompanyRegistration: (id) => apiClient.post(`${adminBase}/company-registrations/${id}/approve`),
  rejectCompanyRegistration: (id) => apiClient.post(`${adminBase}/company-registrations/${id}/reject`),
  getTenants: () => apiClient.get(`${adminBase}/tenants`),
  createTenant: (tenant) => apiClient.post(`${adminBase}/tenants`, tenant),
  updateTenant: (id, tenant) => apiClient.put(`${adminBase}/tenants/${id}`, tenant),
  deleteTenant: (id) => apiClient.delete(`${adminBase}/tenants/${id}`),
  suspendTenant: (id) => apiClient.patch(`${adminBase}/tenants/${id}/suspend`),
  activateTenant: (id) => apiClient.patch(`${adminBase}/tenants/${id}/activate`),
  createHrAdmin: (account) => apiClient.post(`${adminBase}/users/hr-admins`, account),
  getAuditLogs: (params) => apiClient.get(`${adminBase}/audit-logs`, { params }),
  exportAuditLogsCsv: (params) => apiClient.get(`${adminBase}/audit-logs/export`, {
    params: { ...params, format: 'csv' },
    responseType: 'blob',
  }),
};
