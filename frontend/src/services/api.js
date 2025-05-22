import axios from 'axios';

const API_URL = '/api';

// 토큰 가져오기
const getToken = () => {
  return localStorage.getItem('token');
};

// axios 인스턴스 생성
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터
api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 인증 관련 API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/users/register', userData),
};

// 사용자 관련 API
export const userAPI = {
  getProfile: (userId) => api.get(`/users/${userId}`),
  updateProfile: (userId, userData) => api.put(`/users/${userId}`, userData),
};

// 레시피 관련 API
export const recipeAPI = {
  requestRecipe: (recipeRequest) => api.post('/recipes', recipeRequest),
  getHistory: (userId) => api.get(`/recipes/user/${userId}`),
};

// 프롬프트 관련 API
export const promptAPI = {
  generatePrompt: (promptRequest) => api.post('/prompt/generate', promptRequest),
};