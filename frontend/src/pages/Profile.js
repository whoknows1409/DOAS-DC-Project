import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Avatar, Typography, Space, message, Divider, Row, Col, Statistic, Upload } from 'antd';
import { 
  UserOutlined, 
  MailOutlined, 
  EditOutlined, 
  SaveOutlined, 
  LockOutlined,
  ShoppingOutlined,
  DollarOutlined,
  TrophyOutlined,
  CameraOutlined
} from '@ant-design/icons';
import { auctionAPI } from '../services/api';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;

function Profile() {
  const [user, setUser] = useState(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [stats, setStats] = useState({ auctions: 0, bids: 0, wonAuctions: 0 });
  const [form] = Form.useForm();
  const navigate = useNavigate();

  useEffect(() => {
    loadUserData();
    loadUserStats();
  }, []);

  const loadUserData = () => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      const userData = JSON.parse(savedUser);
      setUser(userData);
      form.setFieldsValue({
        username: userData.username,
        email: userData.email
      });
    } else {
      navigate('/login');
    }
  };

  const loadUserStats = async () => {
    try {
      const savedUser = localStorage.getItem('user');
      if (savedUser) {
        const userData = JSON.parse(savedUser);
        
        // Fetch user's auctions
        const auctionsResponse = await fetch(`/api/users/${userData.id}/auctions`);
        const auctions = auctionsResponse.ok ? await auctionsResponse.json() : [];
        
        // Fetch user's bids
        const bidsResponse = await fetch(`/api/users/${userData.id}/bids`);
        const bids = bidsResponse.ok ? await bidsResponse.json() : [];
        
        // Fetch user's won auctions
        const wonResponse = await fetch(`/api/users/${userData.id}/won-auctions`);
        const wonAuctions = wonResponse.ok ? await wonResponse.json() : [];
        
        setStats({
          auctions: auctions.length,
          bids: bids.length,
          wonAuctions: wonAuctions.length
        });
      }
    } catch (error) {
      console.error('Failed to load user stats:', error);
    }
  };

  const handleEdit = () => {
    setEditing(true);
  };

  const handleCancel = () => {
    setEditing(false);
    form.setFieldsValue({
      username: user.username,
      email: user.email
    });
  };

  const handleProfileImageUpload = async (file) => {
    setUploading(true);
    try {
      // Upload the image
      const uploadResult = await auctionAPI.uploadFile(file);
      
      // Update the user's profile image URL
      const updatedUser = await auctionAPI.updateProfileImage(user.id, uploadResult.fileUrl);
      
      // Update local storage and state
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      
      message.success('Profile image updated successfully!');
      
      // Force page reload to update avatar in header
      setTimeout(() => {
        window.location.reload();
      }, 500);
      
      return false; // Prevent default upload behavior
    } catch (error) {
      console.error('Failed to upload profile image:', error);
      message.error('Failed to upload profile image. Please try again.');
      return false;
    } finally {
      setUploading(false);
    }
  };

  const handleSave = async (values) => {
    setLoading(true);
    try {
      const response = await fetch(`/api/users/${user.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: values.username,
          email: values.email,
        }),
      });

      if (response.ok) {
        const updatedUser = await response.json();
        localStorage.setItem('user', JSON.stringify(updatedUser));
        setUser(updatedUser);
        setEditing(false);
        message.success('Profile updated successfully!');
      } else {
        message.error('Failed to update profile');
      }
    } catch (error) {
      console.error('Failed to update profile:', error);
      message.error('Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Text>Loading...</Text>
      </div>
    );
  }

  return (
    <div>
      <Title level={2}>My Profile</Title>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card>
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <div style={{ position: 'relative', display: 'inline-block', marginBottom: '16px' }}>
                <div className="avatar-gradient-border">
                  <Avatar 
                    size={120}
                    src={user.profileImageUrl}
                    icon={!user.profileImageUrl && <UserOutlined />}
                    style={{ 
                      background: user.profileImageUrl ? 'transparent' : 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
                    }}
                  />
                </div>
                <Upload
                  beforeUpload={handleProfileImageUpload}
                  maxCount={1}
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  showUploadList={false}
                >
                  <Button
                    shape="circle"
                    icon={<CameraOutlined />}
                    loading={uploading}
                    style={{
                      position: 'absolute',
                      bottom: 0,
                      right: 0,
                      background: '#0077b6',
                      color: 'white',
                      border: '2px solid white',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
                    }}
                    title="Upload profile picture"
                  />
                </Upload>
              </div>
              <Title level={3} style={{ marginBottom: '8px' }}>{user.username}</Title>
              <Text type="secondary">{user.email}</Text>
              <Divider />
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <Text type="secondary">Member since</Text>
                <Text strong>{new Date(user.createdAt).toLocaleDateString('en-US', { 
                  year: 'numeric', 
                  month: 'long', 
                  day: 'numeric' 
                })}</Text>
              </Space>
            </div>
          </Card>

          <Card style={{ marginTop: '16px' }} title="Activity Statistics">
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <div className="stat-card">
                  <div className="stat-card-icon">
                    <ShoppingOutlined />
                  </div>
                  <Statistic 
                    title="Auctions Created" 
                    value={stats.auctions}
                    valueStyle={{ color: '#0077b6', fontWeight: 'bold' }}
                  />
                </div>
              </Col>
              <Col span={24}>
                <div className="stat-card">
                  <div className="stat-card-icon">
                    <DollarOutlined />
                  </div>
                  <Statistic 
                    title="Total Bids Placed" 
                    value={stats.bids}
                    valueStyle={{ color: '#52c41a', fontWeight: 'bold' }}
                  />
                </div>
              </Col>
              <Col span={24}>
                <div className="stat-card">
                  <div className="stat-card-icon">
                    <TrophyOutlined />
                  </div>
                  <Statistic 
                    title="Auctions Won" 
                    value={stats.wonAuctions}
                    valueStyle={{ color: '#faad14', fontWeight: 'bold' }}
                  />
                </div>
              </Col>
            </Row>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card 
            title="Profile Information"
            extra={
              !editing ? (
                <Button 
                  type="primary" 
                  icon={<EditOutlined />}
                  onClick={handleEdit}
                >
                  Edit Profile
                </Button>
              ) : null
            }
          >
            <Form
              form={form}
              layout="vertical"
              onFinish={handleSave}
              disabled={!editing}
            >
              <Form.Item
                name="username"
                label="Username"
                rules={[
                  { required: true, message: 'Please input your username!' },
                  { min: 3, message: 'Username must be at least 3 characters!' }
                ]}
              >
                <Input 
                  prefix={<UserOutlined />}
                  placeholder="Username"
                  size="large"
                />
              </Form.Item>

              <Form.Item
                name="email"
                label="Email"
                rules={[
                  { required: true, message: 'Please input your email!' },
                  { type: 'email', message: 'Please enter a valid email!' }
                ]}
              >
                <Input 
                  prefix={<MailOutlined />}
                  placeholder="Email"
                  size="large"
                />
              </Form.Item>

              {editing && (
                <Form.Item>
                  <Space>
                    <Button 
                      type="primary" 
                      htmlType="submit" 
                      icon={<SaveOutlined />}
                      loading={loading}
                    >
                      Save Changes
                    </Button>
                    <Button onClick={handleCancel}>
                      Cancel
                    </Button>
                  </Space>
                </Form.Item>
              )}
            </Form>

            <Divider />

            <Title level={4}>Account Information</Title>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <div>
                <Text type="secondary">User ID</Text>
                <br />
                <Text code>{user.id}</Text>
              </div>
              <div>
                <Text type="secondary">Account Created</Text>
                <br />
                <Text>{new Date(user.createdAt).toLocaleString()}</Text>
              </div>
              {user.updatedAt && (
                <div>
                  <Text type="secondary">Last Updated</Text>
                  <br />
                  <Text>{new Date(user.updatedAt).toLocaleString()}</Text>
                </div>
              )}
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default Profile;

