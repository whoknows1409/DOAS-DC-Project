import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Typography, 
  Statistic, 
  Table, 
  Tag, 
  Button, 
  Space,
  Progress,
  Tabs,
  List,
  message,
  Modal,
  Descriptions
} from 'antd';
import { 
  CloudServerOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  ThunderboltOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  UserOutlined,
  ShoppingCartOutlined,
  DollarOutlined,
  FireOutlined,
  StopOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { auctionAPI } from '../services/api';
import { useNavigate } from 'react-router-dom';
import moment from 'moment';

const { Title, Text } = Typography;
const { TabPane } = Tabs;
const { confirm } = Modal;

function EnhancedAdminDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [systemStatus, setSystemStatus] = useState(null);
  const [auctions, setAuctions] = useState([]);
  const [users, setUsers] = useState([]);
  const [recentBids, setRecentBids] = useState([]);
  const [activeTab, setActiveTab] = useState('1');

  useEffect(() => {
    loadDashboardData();
    const interval = setInterval(loadDashboardData, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, []);

  const loadDashboardData = async () => {
    try {
      // Load system status
      const statusResponse = await fetch('/api/admin/status');
      if (statusResponse.ok) {
        const status = await statusResponse.json();
        setSystemStatus(status);
      }

      // Load all auctions
      const auctionsResponse = await fetch('/api/auctions');
      if (auctionsResponse.ok) {
        const auctionsData = await auctionsResponse.json();
        setAuctions(auctionsData);

        // Load recent bids from all auctions
        const bidsPromises = auctionsData.slice(0, 5).map(auction =>
          fetch(`/api/auctions/${auction.id}/bids`)
            .then(res => res.ok ? res.json() : [])
            .catch(() => [])
        );
        const bidsArrays = await Promise.all(bidsPromises);
        const allBids = bidsArrays.flat();
        setRecentBids(allBids.sort((a, b) => 
          new Date(b.timestamp) - new Date(a.timestamp)
        ).slice(0, 10));
      }

      // Load all users
      const usersResponse = await fetch('/api/users');
      if (usersResponse.ok) {
        const usersData = await usersResponse.json();
        setUsers(usersData);
      }

      setLoading(false);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      setLoading(false);
    }
  };

  const handleEndAuction = (auctionId, auctionTitle) => {
    confirm({
      title: 'End Auction Early?',
      icon: <ExclamationCircleOutlined />,
      content: `Are you sure you want to end "${auctionTitle}" before its scheduled end time?`,
      okText: 'Yes, End Auction',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          const response = await fetch(`/api/admin/auctions/${auctionId}/end`, {
            method: 'POST',
          });
          
          if (response.ok) {
            message.success('Auction ended successfully');
            loadDashboardData();
          } else {
            message.error('Failed to end auction');
          }
        } catch (error) {
          console.error('Error ending auction:', error);
          message.error('Failed to end auction');
        }
      },
    });
  };

  const handleTriggerElection = async () => {
    try {
      const response = await fetch('/api/admin/election/trigger', {
        method: 'POST',
      });
      
      if (response.ok) {
        message.success('Coordinator election triggered');
        setTimeout(loadDashboardData, 2000);
      } else {
        message.error('Failed to trigger election');
      }
    } catch (error) {
      console.error('Error triggering election:', error);
      message.error('Failed to trigger election');
    }
  };

  const serverColumns = [
    {
      title: 'Server ID',
      dataIndex: 'serverId',
      key: 'serverId',
      render: (id, record) => {
        const isCoordinator = record.isCoordinator !== undefined ? record.isCoordinator : record.coordinator;
        return (
          <Space>
            <CloudServerOutlined />
            <Text strong>Server {id}</Text>
            {isCoordinator && <Tag color="gold">COORDINATOR</Tag>}
          </Space>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'isHealthy',
      key: 'status',
      render: (healthy, record) => {
        // Check both isHealthy and healthy properties (backend serialization inconsistency)
        const isHealthy = record.isHealthy !== undefined ? record.isHealthy : record.healthy;
        return (
          <Tag color={isHealthy ? 'green' : 'red'} icon={isHealthy ? <CheckCircleOutlined /> : <StopOutlined />}>
            {isHealthy ? 'HEALTHY' : 'OFFLINE'}
          </Tag>
        );
      },
    },
    {
      title: 'Logical Clock',
      dataIndex: 'logicalClock',
      key: 'logicalClock',
      render: (clock) => <Tag color="blue">{clock}</Tag>,
    },
    {
      title: 'Uptime',
      dataIndex: 'uptime',
      key: 'uptime',
      render: (uptime) => {
        const hours = Math.floor(uptime / 3600000);
        const minutes = Math.floor((uptime % 3600000) / 60000);
        return <Text>{hours}h {minutes}m</Text>;
      },
    },
    {
      title: 'Active Connections',
      dataIndex: 'activeConnections',
      key: 'activeConnections',
      render: (connections) => <Tag>{connections}</Tag>,
    },
  ];

  const auctionColumns = [
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      render: (title, record) => (
        <Space direction="vertical" size="small">
          <Text strong>{title}</Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>{record.description}</Text>
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const colors = { 'ACTIVE': 'green', 'ENDED': 'red', 'PENDING': 'orange' };
        return <Tag color={colors[status]}>{status}</Tag>;
      },
    },
    {
      title: 'Current Price',
      dataIndex: 'currentPrice',
      key: 'currentPrice',
      render: (price) => <Text strong style={{ color: '#1890ff' }}>${price}</Text>,
    },
    {
      title: 'Time Remaining',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (endTime, record) => {
        if (record.status === 'ENDED') return <Tag color="red">Ended</Tag>;
        
        const now = moment();
        const end = moment(endTime);
        const duration = moment.duration(end.diff(now));
        
        if (duration.asMilliseconds() <= 0) {
          return <Tag color="red">Expired</Tag>;
        }
        
        const hours = Math.floor(duration.asHours());
        const minutes = duration.minutes();
        return <Tag color="blue">{hours}h {minutes}m</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button 
            type="link" 
            size="small"
            onClick={() => navigate(`/auctions/${record.id}`)}
          >
            View
          </Button>
          {record.status === 'ACTIVE' && (
            <Button 
              type="link" 
              danger 
              size="small"
              icon={<StopOutlined />}
              onClick={() => handleEndAuction(record.id, record.title)}
            >
              End Now
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const getHealthPercentage = () => {
    if (!systemStatus?.servers) return 0;
    const healthy = systemStatus.servers.filter(s => s.isHealthy !== undefined ? s.isHealthy : s.healthy).length;
    return (healthy / systemStatus.servers.length) * 100;
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={2}>
          <CloudServerOutlined style={{ marginRight: '12px' }} />
          Admin Dashboard
        </Title>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadDashboardData}
            loading={loading}
          >
            Refresh
          </Button>
          <Button 
            icon={<PlayCircleOutlined />} 
            onClick={handleTriggerElection}
            type="primary"
          >
            Trigger Election
          </Button>
        </Space>
      </div>

      {/* System Health Overview */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Auctions"
              value={auctions.length}
              prefix={<ShoppingCartOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Active Auctions"
              value={auctions.filter(a => a.status === 'ACTIVE').length}
              prefix={<FireOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Users"
              value={users.length}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Bids"
              value={recentBids.length}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      {/* System Health */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="System Health">
            <Row gutter={16}>
              <Col span={12}>
                <div style={{ textAlign: 'center' }}>
                  <Progress
                    type="circle"
                    percent={getHealthPercentage()}
                    format={percent => `${percent.toFixed(0)}%`}
                    status={getHealthPercentage() === 100 ? 'success' : 'exception'}
                  />
                  <div style={{ marginTop: 16 }}>
                    <Text strong>Server Health</Text>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                {systemStatus && (
                  <Descriptions column={1} size="small">
                    <Descriptions.Item label="Coordinator">
                      Server {systemStatus.coordinatorId || 'N/A'}
                    </Descriptions.Item>
                    <Descriptions.Item label="Total Servers">
                      {systemStatus.servers?.length || 0}
                    </Descriptions.Item>
                    <Descriptions.Item label="Healthy Servers">
                      <Tag color="green">
                        {systemStatus.servers?.filter(s => s.isHealthy !== undefined ? s.isHealthy : s.healthy).length || 0}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Offline Servers">
                      <Tag color="red">
                        {systemStatus.servers?.filter(s => !(s.isHealthy !== undefined ? s.isHealthy : s.healthy)).length || 0}
                      </Tag>
                    </Descriptions.Item>
                  </Descriptions>
                )}
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Detailed Views */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="Server Status" key="1">
            <Table
              columns={serverColumns}
              dataSource={systemStatus?.servers || []}
              loading={loading}
              rowKey="serverId"
              pagination={false}
            />
          </TabPane>

          <TabPane tab="Auction Management" key="2">
            <Table
              columns={auctionColumns}
              dataSource={auctions}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 10 }}
            />
          </TabPane>

          <TabPane tab="Recent Bids" key="3">
            <List
              loading={loading}
              dataSource={recentBids}
              locale={{ emptyText: 'No recent bids' }}
              renderItem={bid => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text strong>Bid: ${bid.amount}</Text>
                        <Tag color="blue">Server {bid.serverId}</Tag>
                        <Tag>Clock: {bid.logicalTimestamp}</Tag>
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Space>
                          <Text type="secondary">Auction:</Text>
                          <Text code>{bid.auctionId}</Text>
                        </Space>
                        <Space>
                          <Text type="secondary">Bidder:</Text>
                          <Text code>{bid.bidderId}</Text>
                        </Space>
                        <Text type="secondary">
                          {moment(bid.timestamp).format('MMM DD, YYYY HH:mm:ss')}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </TabPane>

          <TabPane tab="Users" key="4">
            <Table
              dataSource={users}
              loading={loading}
              rowKey="id"
              columns={[
                {
                  title: 'Username',
                  dataIndex: 'username',
                  key: 'username',
                  render: (username) => (
                    <Space>
                      <UserOutlined />
                      <Text strong>{username}</Text>
                    </Space>
                  ),
                },
                {
                  title: 'Email',
                  dataIndex: 'email',
                  key: 'email',
                },
                {
                  title: 'Joined',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  render: (date) => moment(date).format('MMM DD, YYYY'),
                },
              ]}
              pagination={{ pageSize: 10 }}
            />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
}

export default EnhancedAdminDashboard;

