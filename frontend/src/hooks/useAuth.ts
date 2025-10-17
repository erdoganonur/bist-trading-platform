import { useMutation } from '@tanstack/react-query';
import { authService } from '@services/api/auth.service';
import type { LoginRequest, RegisterRequest } from '@services/api/auth.service';
import { useAuthStore } from '@/app/store';
import { useNavigate } from 'react-router-dom';
import { wsService } from '@services/websocket/websocket.service';

export const useAuth = () => {
  const { setAuth, clearAuth, isAuthenticated, user } = useAuthStore();
  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authService.login(data),
    onSuccess: (response) => {
      const data = response.data;

      // Store tokens
      localStorage.setItem('access_token', data.accessToken);
      localStorage.setItem('refresh_token', data.refreshToken);

      // Update auth state
      setAuth({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        userId: data.userId,
        username: data.username,
        authorities: data.authorities,
      });

      // Connect WebSocket
      wsService.connect(data.accessToken).catch(err => {
        console.error('[useAuth] WebSocket connection failed:', err);
      });

      // Navigate to dashboard
      navigate('/dashboard');
    },
    onError: (error) => {
      console.error('[useAuth] Login failed:', error);
    },
  });

  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) => authService.register(data),
    onSuccess: (response) => {
      const data = response.data;

      // Store tokens
      localStorage.setItem('access_token', data.accessToken);
      localStorage.setItem('refresh_token', data.refreshToken);

      // Update auth state
      setAuth({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        userId: data.userId,
        username: data.username,
        authorities: data.authorities,
      });

      // Connect WebSocket
      wsService.connect(data.accessToken).catch(err => {
        console.error('[useAuth] WebSocket connection failed:', err);
      });

      // Navigate to dashboard
      navigate('/dashboard');
    },
    onError: (error) => {
      console.error('[useAuth] Registration failed:', error);
    },
  });

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('[useAuth] Logout API call failed:', error);
    } finally {
      // Disconnect WebSocket
      wsService.disconnect();

      // Clear local storage
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');

      // Clear auth state
      clearAuth();

      // Navigate to login
      navigate('/login');
    }
  };

  return {
    login: loginMutation.mutate,
    register: registerMutation.mutate,
    logout,
    isLoading: loginMutation.isPending || registerMutation.isPending,
    isError: loginMutation.isError || registerMutation.isError,
    error: loginMutation.error || registerMutation.error,
    isAuthenticated,
    user,
  };
};
