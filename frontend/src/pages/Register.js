import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Divider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, UserAddOutlined, ShoppingOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;

const Register = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/users/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: values.username,
          email: values.email,
          password: values.password,
        }),
      });

      if (response.ok) {
        setSuccess(true);
        setTimeout(() => {
          navigate('/login');
        }, 2000);
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Registration failed');
      }
    } catch (err) {
      setError('Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = () => {
    navigate('/login');
  };

  if (success) {
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
            textAlign: 'center',
            borderRadius: '16px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
          }}
          bodyStyle={{ padding: '60px 40px' }}
        >
          <CheckCircleOutlined style={{ fontSize: '80px', color: '#52c41a', marginBottom: '24px' }} />
          <Title level={2} style={{ color: '#52c41a', marginBottom: '16px' }}>
            Registration Successful!
          </Title>
          <Text style={{ fontSize: '16px', color: '#666' }}>
            Redirecting to login page...
          </Text>
        </Card>
      </div>
    );
  }

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
            <UserAddOutlined style={{ fontSize: '40px', color: 'white' }} />
          </div>
          <Title level={2} style={{ 
            marginBottom: '8px',
            background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            fontWeight: 'bold'
          }}>
            Create Account
          </Title>
          <Text type="secondary" style={{ fontSize: '15px' }}>
            Join DOAS today
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
          name="register"
          onFinish={onFinish}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="username"
            label={<span style={{ fontWeight: 500 }}>Username</span>}
            rules={[
              { required: true, message: 'Please input your username!' },
              { min: 3, message: 'Username must be at least 3 characters!' },
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#999' }} />}
              placeholder="Choose a username"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item
            name="email"
            label={<span style={{ fontWeight: 500 }}>Email</span>}
            rules={[
              { required: true, message: 'Please input your email!' },
              { type: 'email', message: 'Please enter a valid email!' },
            ]}
          >
            <Input
              prefix={<MailOutlined style={{ color: '#999' }} />}
              placeholder="Enter your email"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item
            name="password"
            label={<span style={{ fontWeight: 500 }}>Password</span>}
            rules={[
              { required: true, message: 'Please input your password!' },
              { min: 6, message: 'Password must be at least 6 characters!' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#999' }} />}
              placeholder="Create a password"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label={<span style={{ fontWeight: 500 }}>Confirm Password</span>}
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm your password!' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Passwords do not match!'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#999' }} />}
              placeholder="Confirm your password"
              style={{ borderRadius: '8px', height: '48px' }}
            />
          </Form.Item>

          <Form.Item style={{ marginTop: '32px' }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              icon={<UserAddOutlined />}
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
              Create Account
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '24px 0' }}>or</Divider>

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">Already have an account? </Text>
          <Button 
            type="link" 
            onClick={handleLogin} 
            style={{ 
              padding: 0,
              fontWeight: 500,
              fontSize: '15px'
            }}
          >
            Sign in here
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default Register;
