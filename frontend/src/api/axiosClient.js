import axios from 'axios';

function createClient(baseURL = '') {
  const client = axios.create({
    baseURL,
    timeout: 20000,
    withCredentials: true, // For HTTP-only refresh token cookie
    headers: {
      'Content-Type': 'application/json',
    },
  });

  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => {
      const contentType = response.headers?.['content-type'] || '';
      if (contentType.includes('text/html') && typeof response.data === 'string') {
        return Promise.reject(
          new Error('Received HTML response instead of JSON. Check API route configuration.')
        );
      }
      return response.data;
    },
    async (error) => {
      const originalRequest = error.config;
      if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/auth/login') && !originalRequest.url.includes('/auth/refresh')) {
        originalRequest._retry = true;
        try {
          const res = await axios.post('/api/v1/auth/refresh', {}, { withCredentials: true });
          const newAccessToken = res.data?.accessToken;
          if (newAccessToken) {
            localStorage.setItem('accessToken', newAccessToken);
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            return client(originalRequest);
          }
        } catch (refreshErr) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('rms_user');
          // Optional redirect to /login
        }
      }
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Unexpected error';
      return Promise.reject(new Error(message));
    }
  );

  return client;
}

export const apiClient = createClient('');
export const jobClient = apiClient;
export const candidateClient = apiClient;
export const companyClient = apiClient;
