import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // /api/jobs/* → job-service:8081/jobs/*
      '/api/jobs': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api\/jobs/, '/jobs'),
      },
      // /api/candidate/* → candidate-service:8082/candidate/*
      '/api/candidate': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api\/candidate/, '/candidate'),
      },
      // /api/company/* → company-service:8083/company/*
      '/api/company': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api\/company/, '/company'),
      },
    },
  },
});
