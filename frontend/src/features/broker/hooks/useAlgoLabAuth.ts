import { useState } from 'react';
import { message } from 'antd';
import { useAlgoLabStore } from '@app/store';
import { brokerService } from '@services/api/broker.service';
import type { AlgoLabLoginRequest, AlgoLabOTPRequest } from '@services/api/broker.service';

/**
 * Custom hook for AlgoLab authentication
 * Handles 2-step OTP authentication flow
 */
export const useAlgoLabAuth = () => {
  const {
    setAuthenticating,
    setAuthenticated,
    setError,
    clearAlgoLabAuth
  } = useAlgoLabStore();

  const [step, setStep] = useState<1 | 2>(1);
  const [username, setUsername] = useState<string>('');

  /**
   * Step 1: Send login request (triggers SMS OTP)
   */
  const login = async (data: AlgoLabLoginRequest) => {
    try {
      setAuthenticating(true);
      setError(null);

      const response = await brokerService.algoLabLogin(data);

      if (response.data.success && response.data.smsSent) {
        message.success('SMS kodu gönderildi! Lütfen telefonunuzu kontrol edin.');
        setUsername(data.username);
        setStep(2);
      } else {
        throw new Error(response.data.message || 'Giriş başarısız');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'AlgoLab girişi başarısız';
      setError(errorMessage);
      message.error(errorMessage);
    } finally {
      setAuthenticating(false);
    }
  };

  /**
   * Step 2: Verify OTP code
   */
  const verifyOTP = async (data: AlgoLabOTPRequest) => {
    try {
      setAuthenticating(true);
      setError(null);

      const response = await brokerService.algoLabVerifyOTP(data);

      if (response.data.success && response.data.authenticated) {
        message.success('AlgoLab girişi başarılı!');

        setAuthenticated({
          username,
          sessionExpiresAt: response.data.sessionExpiresAt,
        });

        return true;
      } else {
        throw new Error(response.data.message || 'OTP doğrulama başarısız');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'OTP doğrulama başarısız';
      setError(errorMessage);
      message.error(errorMessage);
      return false;
    } finally {
      setAuthenticating(false);
    }
  };

  /**
   * Logout from AlgoLab
   */
  const logout = async () => {
    try {
      await brokerService.algoLabLogout();
      clearAlgoLabAuth();
      message.success('AlgoLab çıkışı yapıldı');
      setStep(1);
      setUsername('');
    } catch (error: any) {
      message.error('Çıkış sırasında hata oluştu');
    }
  };

  /**
   * Reset to step 1
   */
  const reset = () => {
    setStep(1);
    setUsername('');
    setError(null);
  };

  return {
    login,
    verifyOTP,
    logout,
    reset,
    step,
    setStep,
    username,
  };
};
