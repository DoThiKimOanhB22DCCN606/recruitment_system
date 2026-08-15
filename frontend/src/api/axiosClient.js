import axios from 'axios';

function createClient(baseURL) {
  const client = axios.create({ baseURL, timeout: 15000 });

  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response.data,
    (error) => {
      const message =
        error.response?.data?.message || error.message || 'Unexpected error';
      return Promise.reject(new Error(message));
    }
  );

  return client;
}

// baseURL points to nginx location prefix which proxies to each backend service.
// Paths in each api file should be relative to the backend controller root:
//   jobClient:       /api/jobs       → job-service:8081       (controller: /jobs)
//   candidateClient: /api/candidate  → candidate-service:8082 (controller: /candidate)
//   companyClient:   /api/company    → company-service:8083   (controller: /company)
export const jobClient       = createClient('/api');
export const candidateClient = createClient('/api');
export const companyClient   = createClient('/api');
