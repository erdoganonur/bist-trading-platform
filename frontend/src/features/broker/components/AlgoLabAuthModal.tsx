import { Modal, Form, Input, Button, Steps, Alert, Space, Typography } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons';
import { useAlgoLabStore } from '@app/store';
import { useAlgoLabAuth } from '../hooks/useAlgoLabAuth';
import type { AlgoLabLoginRequest, AlgoLabOTPRequest } from '@services/api/broker.service';

const { Text } = Typography;

interface AlgoLabAuthModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * AlgoLab 2-Step Authentication Modal
 * Step 1: Username/Password (sends SMS OTP)
 * Step 2: OTP Code Verification
 */
export const AlgoLabAuthModal = ({ open, onClose, onSuccess }: AlgoLabAuthModalProps) => {
  const { isAuthenticating, error } = useAlgoLabStore();
  const { login, verifyOTP, reset, step, username } = useAlgoLabAuth();

  const [loginForm] = Form.useForm();
  const [otpForm] = Form.useForm();

  const handleLoginSubmit = async (values: AlgoLabLoginRequest) => {
    await login(values);
  };

  const handleOTPSubmit = async (values: AlgoLabOTPRequest) => {
    const success = await verifyOTP(values);
    if (success) {
      handleClose();
      onSuccess?.();
    }
  };

  const handleClose = () => {
    loginForm.resetFields();
    otpForm.resetFields();
    reset();
    onClose();
  };

  const handleBackToLogin = () => {
    otpForm.resetFields();
    reset();
  };

  return (
    <Modal
      title="AlgoLab Broker Girişi"
      open={open}
      onCancel={handleClose}
      footer={null}
      width={500}
      destroyOnClose
    >
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* Steps Indicator */}
        <Steps
          current={step - 1}
          items={[
            { title: 'Giriş', icon: <UserOutlined /> },
            { title: 'OTP Doğrulama', icon: <SafetyOutlined /> },
          ]}
        />

        {/* Error Alert */}
        {error && (
          <Alert
            message="Hata"
            description={error}
            type="error"
            closable
            showIcon
          />
        )}

        {/* Step 1: Login Form */}
        {step === 1 && (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
              AlgoLab kullanıcı adı ve şifrenizi girin. SMS ile doğrulama kodu gönderilecektir.
            </Text>

            <Form
              form={loginForm}
              name="algolab-login"
              onFinish={handleLoginSubmit}
              layout="vertical"
              autoComplete="off"
            >
              <Form.Item
                name="username"
                label="Kullanıcı Adı"
                rules={[
                  { required: true, message: 'Kullanıcı adı gereklidir' },
                  { min: 3, message: 'En az 3 karakter olmalıdır' },
                ]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="AlgoLab kullanıcı adı"
                  size="large"
                  autoFocus
                />
              </Form.Item>

              <Form.Item
                name="password"
                label="Şifre"
                rules={[
                  { required: true, message: 'Şifre gereklidir' },
                  { min: 4, message: 'En az 4 karakter olmalıdır' },
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="AlgoLab şifresi"
                  size="large"
                />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  size="large"
                  loading={isAuthenticating}
                >
                  SMS Kodu Gönder
                </Button>
              </Form.Item>
            </Form>

            <Alert
              message="Bilgi"
              description="AlgoLab hesabınıza SMS ile tek kullanımlık kod gönderilecektir. Lütfen telefonunuzun yanında olduğundan emin olun."
              type="info"
              showIcon
            />
          </div>
        )}

        {/* Step 2: OTP Form */}
        {step === 2 && (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
              <strong>{username}</strong> kullanıcısına SMS ile gönderilen 6 haneli kodu girin.
            </Text>

            <Form
              form={otpForm}
              name="algolab-otp"
              onFinish={handleOTPSubmit}
              layout="vertical"
              autoComplete="off"
            >
              <Form.Item
                name="otpCode"
                label="SMS Doğrulama Kodu"
                rules={[
                  { required: true, message: 'OTP kodu gereklidir' },
                  { len: 6, message: '6 haneli kod giriniz' },
                  { pattern: /^\d+$/, message: 'Sadece rakam giriniz' },
                ]}
              >
                <Input
                  prefix={<SafetyOutlined />}
                  placeholder="000000"
                  size="large"
                  maxLength={6}
                  autoFocus
                  style={{ fontSize: '20px', letterSpacing: '8px', textAlign: 'center' }}
                />
              </Form.Item>

              <Form.Item>
                <Space direction="vertical" style={{ width: '100%' }} size="middle">
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    loading={isAuthenticating}
                  >
                    Doğrula ve Giriş Yap
                  </Button>

                  <Button
                    type="link"
                    block
                    onClick={handleBackToLogin}
                    disabled={isAuthenticating}
                  >
                    Geri Dön
                  </Button>
                </Space>
              </Form.Item>
            </Form>

            <Alert
              message="SMS Almadınız mı?"
              description="Kod genellikle 1-2 dakika içinde gelir. Gelmediği takdirde 'Geri Dön' butonuna basarak tekrar deneyebilirsiniz."
              type="warning"
              showIcon
            />
          </div>
        )}
      </Space>
    </Modal>
  );
};
