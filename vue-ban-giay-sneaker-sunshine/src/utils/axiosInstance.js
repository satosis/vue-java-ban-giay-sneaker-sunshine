// src/utils/axiosInstance.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api', // URL gốc backend của bạn
  timeout: 10000,
});

// Interceptor để tự động đính kèm token trong header Authorization
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token'); // Lấy token từ localStorage

    if (token) {
      // Nếu token chưa có 'Bearer ' thì thêm prefix
      config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;
