import { Form, Input, Button, Card, Typography, Alert, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { useAuth } from '@hooks/useAuth';
import type { LoginRequest } from '@services/api/auth.service';

const { Title, Text } = Typography;

export const LoginPage = () => {
  const { login, isLoading, isError, error } = useAuth();
  const [form] = Form.useForm();

  const onFinish = (values: LoginRequest) => {
    login(values);
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    }}>
      <Card
        style={{
          width: 400,
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
        }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Title level={2} style={{ marginBottom: 0 }}>
              BIST Trading Platform
            </Title>
            <Text type="secondary">Sign in to your account</Text>
          </div>

          {isError && (
            <Alert
              message="Login Failed"
              description={error?.message || 'Invalid username or password'}
              type="error"
              closable
            />
          )}

          <Form
            form={form}
            name="login"
            onFinish={onFinish}
            autoComplete="off"
            layout="vertical"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: 'Please input your username!' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="Username"
                size="large"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: 'Please input your password!' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Password"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                block
                size="large"
                loading={isLoading}
              >
                Sign In
              </Button>
            </Form.Item>
          </Form>

          <div style={{ textAlign: 'center' }}>
            <Text type="secondary">
              Don't have an account?{' '}
              <Link to="/register">Sign up</Link>
            </Text>
          </div>
        </Space>
      </Card>
    </div>
  );
};
