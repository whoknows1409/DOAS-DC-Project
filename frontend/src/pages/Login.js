import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Divider } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined, ShoppingOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/users/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: values.username,
          password: values.password,
        }),
      });

      if (response.ok) {
        const user = await response.json();
        localStorage.setItem('user', JSON.stringify(user));
        
        // Set role based on username (admin gets ADMIN role, others get USER role)
        const userRole = user.username === 'admin' ? 'ADMIN' : 'USER';
        localStorage.setItem('userRole', userRole);
        
        // Force page reload to update App state
        window.location.href = userRole === 'ADMIN' ? '/admin' : '/';
      } else {
        setError('Invalid username or password');
      }
    } catch (err) {
      setError('Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = () => {
    navigate('/register');
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #023e8a 0%, #0077b6 50%, #00b4d8 100%)',
      padding: '20px'
    }}>
      <Card 
        style={{ 
          width: 450, 
          boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
          borderRadius: '16px',
          border: 'none'
        }}
        bodyStyle={{ padding: '40px' }}
      >
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{
            width: '80px',
            height: '80px',
            margin: '0 auto 16px',
            background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 16px rgba(0, 119, 182, 0.4)'
          }}>
            <ShoppingOutlined style={{ fontSize: '40px', color: 'white' }} />
          </div>
          <Title level={2} style={{ 
            marginBottom: '8px',
            background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            fontWeight: 'bold'
          }}>
            Welcome Back
          </Title>
          <Text type="secondary" style={{ fontSize: '15px' }}>
            Sign in to continue to DOAS
          </Text>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            style={{ marginBottom: '24px', borderRadius: '8px' }}
            showIcon
          />
        )}

        <Form
          name="login"
          onFinish={onFinish}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="username"
            label={<span style={{ fontWeight: 500 }}>Username</span>}
            rules={[
              { required: true, message: 'Please input your username!' },
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#999' }} />}
              placeholder="Enter your username"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item
            name="password"
            label={<span style={{ fontWeight: 500 }}>Password</span>}
            rules={[
              { required: true, message: 'Please input your password!' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#999' }} />}
              placeholder="Enter your password"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item style={{ marginTop: '32px' }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              icon={<LoginOutlined />}
              style={{ 
                height: '50px', 
                fontSize: '16px',
                borderRadius: '8px',
                fontWeight: 500,
                background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
                border: 'none',
                boxShadow: '0 4px 12px rgba(0, 119, 182, 0.4)'
              }}
            >
              Sign In
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '24px 0' }}>or</Divider>

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">Don't have an account? </Text>
          <Button 
            type="link" 
            onClick={handleRegister} 
            style={{ 
              padding: 0,
              fontWeight: 500,
              fontSize: '15px'
            }}
          >
            Create account
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default Login;
