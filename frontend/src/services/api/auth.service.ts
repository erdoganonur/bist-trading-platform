import { axiosInstance } from './axios.config';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  fullName: string;
  phoneNumber: string;
  tcKimlikNo?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  authorities: string[];
}

export const authService = {
  login: (data: LoginRequest) =>
    axiosInstance.post<AuthResponse>('/api/v1/auth/login', data),

  register: (data: RegisterRequest) =>
    axiosInstance.post<AuthResponse>('/api/v1/auth/register', data),

  logout: () =>
    axiosInstance.post('/api/v1/auth/logout', {
      accessToken: localStorage.getItem('access_token'),
      refreshToken: localStorage.getItem('refresh_token'),
    }),

  refreshToken: (refreshToken: string) =>
    axiosInstance.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken }),

  validateToken: () =>
    axiosInstance.get('/api/v1/auth/validate'),
};
