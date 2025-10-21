import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, List, Button, Tag, Typography, Statistic, Row, Col, Space, message, Spin, Modal, Form, Input, InputNumber, DatePicker, Upload, Image } from 'antd';
import { PlusOutlined, ClockCircleOutlined, FireOutlined, ReloadOutlined, UploadOutlined, PictureOutlined } from '@ant-design/icons';
import { auctionAPI } from '../services/api';
import WebSocketService from '../services/websocket';
import moment from 'moment';
import CountdownTimer from '../components/CountdownTimer';
import EmptyState from '../components/EmptyState';
import AuctionSkeleton from '../components/AuctionSkeleton';
import StatusBadge from '../components/StatusBadge';

const { Title, Text } = Typography;

function AuctionList() {
  const navigate = useNavigate();
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [serverStatus, setServerStatus] = useState({});
  const [logicalTimestamp, setLogicalTimestamp] = useState(0);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [creating, setCreating] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [uploadedImageUrl, setUploadedImageUrl] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    loadAuctions();
    loadServerStatus();
    connectWebSocket();
    
    // Refresh data periodically
    const interval = setInterval(() => {
      loadAuctions();
      loadServerStatus();
    }, 10000);

    return () => {
      clearInterval(interval);
      WebSocketService.disconnect();
    };
  }, []);

  const loadAuctions = async () => {
    try {
      const data = await auctionAPI.getActiveAuctions();
      // Filter out ended auctions - only show ACTIVE status
      const activeAuctions = data.filter(auction => 
        auction.status === 'ACTIVE' && 
        moment(auction.endTime).isAfter(moment())
      );
      setAuctions(activeAuctions);
      setLoading(false);
    } catch (error) {
      console.error('Failed to load auctions:', error);
      message.error('Failed to load auctions. Please try again.');
      setLoading(false);
    }
  };

  const loadServerStatus = async () => {
    try {
      const status = await auctionAPI.getServerStatus();
      setServerStatus(status);
      setLogicalTimestamp(status.logicalTimestamp || 0);
    } catch (error) {
      console.error('Failed to load server status:', error);
    }
  };

  const connectWebSocket = async () => {
    try {
      await WebSocketService.connect();
      
      // Subscribe to auction updates
      WebSocketService.subscribeToAllAuctions((update) => {
        console.log('Received auction update:', update);
        if (update.type === 'BID_UPDATE' || update.type === 'AUCTION_UPDATE') {
          loadAuctions(); // Refresh auctions list
          setLogicalTimestamp(update.logicalTimestamp);
        }
      });

      // Subscribe to server status updates
      WebSocketService.subscribeToServerStatus((status) => {
        console.log('Received server status:', status);
        setServerStatus(status);
        setLogicalTimestamp(status.logicalClock);
      });

    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
    }
  };

  const getTimeRemaining = (endTime) => {
    const now = moment();
    const end = moment(endTime);
    const duration = moment.duration(end.diff(now));
    
    if (duration.asMilliseconds() <= 0) {
      return <Tag color="red">Ended</Tag>;
    }
    
    const days = Math.floor(duration.asDays());
    const hours = duration.hours();
    const minutes = duration.minutes();
    
    if (days > 0) {
      return <Tag color="green">{days}d {hours}h left</Tag>;
    } else if (hours > 0) {
      return <Tag color="orange">{hours}h {minutes}m left</Tag>;
    } else {
      return <Tag color="red">{minutes}m left</Tag>;
    }
  };

  const handleCreateAuction = () => {
    setCreateModalVisible(true);
    setUploadedImageUrl(null);
    form.resetFields();
  };

  const handleImageUpload = async (file) => {
    setUploading(true);
    try {
      const result = await auctionAPI.uploadFile(file);
      setUploadedImageUrl(result.fileUrl);
      message.success('Image uploaded successfully!');
      return false; // Prevent default upload behavior
    } catch (error) {
      console.error('Failed to upload image:', error);
      message.error('Failed to upload image. Please try again.');
      return false;
    } finally {
      setUploading(false);
    }
  };

  const handleCreateSubmit = async (values) => {
    setCreating(true);
    try {
      // Get current user from localStorage
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user) {
        message.error('Please login to create an auction');
        return;
      }

      const auctionData = {
        title: values.title,
        description: values.description,
        startingPrice: values.startingPrice,
        endTime: values.endTime.format('YYYY-MM-DDTHH:mm:ss'),
        sellerId: user.id,
        imageUrl: uploadedImageUrl
      };

      await auctionAPI.createAuction(auctionData);
      message.success('Auction created successfully!');
      setCreateModalVisible(false);
      setUploadedImageUrl(null);
      loadAuctions(); // Refresh the auction list
    } catch (error) {
      console.error('Failed to create auction:', error);
      message.error('Failed to create auction. Please try again.');
    } finally {
      setCreating(false);
    }
  };

  const handleCreateCancel = () => {
    setCreateModalVisible(false);
    setUploadedImageUrl(null);
    form.resetFields();
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await loadAuctions();
      await loadServerStatus();
      message.success('Data refreshed successfully!');
    } catch (error) {
      message.error('Failed to refresh data');
    } finally {
      setRefreshing(false);
    }
  };

  if (loading) {
    return (
      <div>
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col span={24}>
            <Title level={2}>Active Auctions</Title>
          </Col>
        </Row>
        <AuctionSkeleton count={6} />
      </div>
    );
  }

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Active Auctions"
              value={auctions.length}
              prefix={<FireOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Logical Clock"
              value={logicalTimestamp}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Server Status"
              value={serverStatus.serverId ? `Server ${serverStatus.serverId}` : "Connecting..."}
              valueStyle={{ color: serverStatus.serverId ? '#3f8600' : '#cf1322' }}
              suffix={serverStatus.isCoordinator ? <Tag color="gold">Coordinator</Tag> : null}
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Button 
              type="primary" 
              icon={<PlusOutlined />}
              onClick={handleCreateAuction}
              block
            >
              Create Auction
            </Button>
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Button 
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={refreshing}
              block
            >
              Refresh
            </Button>
          </Card>
        </Col>
      </Row>

      <Card>
        <Title level={3}>Active Auctions</Title>
        {auctions.length === 0 ? (
          <EmptyState
            type="auctions"
            onAction={handleCreateAuction}
            actionText="Create Your First Auction"
          />
        ) : (
        <List
          grid={{ gutter: 16, column: 3 }}
          dataSource={auctions}
          renderItem={(auction) => (
            <List.Item>
              <Card
                hoverable
                size="small"
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Text strong>{auction.title}</Text>
                    <StatusBadge status={auction.status} />
                  </div>
                }
                extra={
                  <Button 
                    type="link" 
                    onClick={() => navigate(`/auctions/${auction.id}`)}
                  >
                    View Details
                  </Button>
                }
                cover={
                  auction.imageUrl ? (
                    <div style={{ height: '200px', overflow: 'hidden', background: '#f0f0f0' }}>
                      <Image
                        alt={auction.title}
                        src={auction.imageUrl}
                        style={{ width: '100%', height: '200px', objectFit: 'cover' }}
                        fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%3E%3Crect fill='%23f0f0f0' width='100' height='100'/%3E%3Ctext fill='%23999' x='50%25' y='50%25' text-anchor='middle' dy='.3em' font-family='sans-serif' font-size='14'%3ENo Image%3C/text%3E%3C/svg%3E"
                      />
                    </div>
                  ) : (
                    <div style={{ 
                      height: '200px', 
                      background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <PictureOutlined style={{ fontSize: '48px', color: 'rgba(255,255,255,0.5)' }} />
                    </div>
                  )
                }
              >
                <div style={{ marginBottom: 8 }}>
                  <Text type="secondary">{auction.description}</Text>
                </div>
                
                <Row gutter={8}>
                  <Col span={12}>
                    <Statistic
                      title="Current Bid"
                      value={auction.currentPrice}
                      precision={2}
                      prefix="$"
                      valueStyle={{ fontSize: '16px' }}
                    />
                  </Col>
                  <Col span={12}>
                    <Statistic
                      title="Starting Price"
                      value={auction.startingPrice}
                      precision={2}
                      prefix="$"
                      valueStyle={{ fontSize: '14px', color: '#8c8c8c' }}
                    />
                  </Col>
                </Row>
                
                <div style={{ marginTop: 16, padding: '12px', background: 'rgba(0, 119, 182, 0.05)', borderRadius: '8px' }}>
                  <CountdownTimer endTime={auction.endTime} size="small" />
                </div>
                
                <div style={{ marginTop: 12 }}>
                  <Text type="secondary" style={{ fontSize: '11px' }}>
                    Ends: {moment(auction.endTime).format('MMM DD, YYYY HH:mm')}
                  </Text>
                </div>
              </Card>
            </List.Item>
          )}
        />
        )}
      </Card>

      <Modal
        title="Create New Auction"
        open={createModalVisible}
        onCancel={handleCreateCancel}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateSubmit}
        >
          <Form.Item
            name="title"
            label="Auction Title"
            rules={[
              { required: true, message: 'Please input the auction title!' },
              { min: 3, message: 'Title must be at least 3 characters!' }
            ]}
          >
            <Input placeholder="Enter auction title" />
          </Form.Item>

          <Form.Item
            name="description"
            label="Description"
            rules={[
              { required: true, message: 'Please input the auction description!' },
              { min: 10, message: 'Description must be at least 10 characters!' }
            ]}
          >
            <Input.TextArea 
              rows={4} 
              placeholder="Describe your auction item" 
            />
          </Form.Item>

          <Form.Item
            name="startingPrice"
            label="Starting Price ($)"
            rules={[
              { required: true, message: 'Please input the starting price!' },
              { type: 'number', min: 0.01, message: 'Price must be greater than 0!' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="0.00"
              min={0.01}
              step={0.01}
              precision={2}
            />
          </Form.Item>

          <Form.Item
            name="endTime"
            label="End Time"
            rules={[
              { required: true, message: 'Please select the auction end time!' }
            ]}
          >
            <DatePicker
              showTime
              style={{ width: '100%' }}
              placeholder="Select end date and time"
              disabledDate={(current) => current && current < moment().startOf('day')}
              disabledTime={(current) => {
                if (current && current.isSame(moment(), 'day')) {
                  return {
                    disabledHours: () => {
                      const hours = [];
                      for (let i = 0; i < moment().hour(); i++) {
                        hours.push(i);
                      }
                      return hours;
                    },
                    disabledMinutes: (selectedHour) => {
                      if (selectedHour === moment().hour()) {
                        const minutes = [];
                        for (let i = 0; i <= moment().minute(); i++) {
                          minutes.push(i);
                        }
                        return minutes;
                      }
                      return [];
                    }
                  };
                }
                return {};
              }}
            />
          </Form.Item>

          <Form.Item
            label="Auction Image"
            extra="Upload an image for your auction item (JPEG, PNG, GIF, WEBP, max 10MB)"
          >
            <Upload
              beforeUpload={handleImageUpload}
              maxCount={1}
              accept="image/jpeg,image/png,image/gif,image/webp"
              showUploadList={false}
            >
              <Button icon={<UploadOutlined />} loading={uploading} block>
                {uploading ? 'Uploading...' : 'Select Image'}
              </Button>
            </Upload>
            {uploadedImageUrl && (
              <div style={{ marginTop: 16, textAlign: 'center' }}>
                <Image
                  src={uploadedImageUrl}
                  alt="Auction preview"
                  style={{ maxWidth: '100%', maxHeight: '300px', borderRadius: '8px' }}
                />
                <div style={{ marginTop: 8 }}>
                  <Button 
                    size="small" 
                    danger 
                    onClick={() => setUploadedImageUrl(null)}
                  >
                    Remove Image
                  </Button>
                </div>
              </div>
            )}
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={handleCreateCancel}>
                Cancel
              </Button>
              <Button type="primary" htmlType="submit" loading={creating}>
                Create Auction
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default AuctionList;