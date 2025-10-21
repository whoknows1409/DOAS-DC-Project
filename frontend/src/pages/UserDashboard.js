import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Typography, Statistic, List, Tag, Button, Tabs, Space, message } from 'antd';
import { 
  TrophyOutlined, 
  DollarOutlined, 
  FireOutlined, 
  ClockCircleOutlined,
  ShoppingCartOutlined,
  CheckCircleOutlined,
  UserOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { auctionAPI } from '../services/api';
import moment from 'moment';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

function UserDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [myAuctions, setMyAuctions] = useState([]);
  const [myBids, setMyBids] = useState([]);
  const [wonAuctions, setWonAuctions] = useState([]);
  const [activeTab, setActiveTab] = useState('1');

  useEffect(() => {
    loadUserData();
  }, []);

  const loadUserData = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user) return;

      // Load user's auctions
      const userAuctionsResponse = await fetch(`/api/auctions/seller/${user.id}`);
      if (userAuctionsResponse.ok) {
        const userAuctions = await userAuctionsResponse.json();
        setMyAuctions(userAuctions);
      }

      // Load user's bids
      const userBidsResponse = await fetch(`/api/users/${user.id}/bids`);
      if (userBidsResponse.ok) {
        const userBids = await userBidsResponse.json();
        setMyBids(userBids);
      }

      // Load won auctions using dedicated endpoint
      const wonAuctionsResponse = await fetch(`/api/users/${user.id}/won-auctions`);
      if (wonAuctionsResponse.ok) {
        const wonAuctions = await wonAuctionsResponse.json();
        setWonAuctions(wonAuctions);
      }

      setLoading(false);
    } catch (error) {
      console.error('Failed to load user data:', error);
      message.error('Failed to load dashboard data');
      setLoading(false);
    }
  };

  const getStatusTag = (status) => {
    const statusColors = {
      'ACTIVE': 'green',
      'ENDED': 'red',
      'PENDING': 'orange',
    };
    return <Tag color={statusColors[status] || 'default'}>{status}</Tag>;
  };

  const getTimeRemaining = (endTime) => {
    const now = moment();
    const end = moment(endTime);
    const duration = moment.duration(end.diff(now));

    if (duration.asMilliseconds() <= 0) {
      return <Tag color="red">Ended</Tag>;
    }

    const hours = Math.floor(duration.asHours());
    const minutes = duration.minutes();

    return (
      <Tag icon={<ClockCircleOutlined />} color="blue">
        {hours}h {minutes}m left
      </Tag>
    );
  };

  const totalBidAmount = myBids.reduce((sum, bid) => sum + parseFloat(bid.amount || 0), 0);
  const activeBids = myBids.filter(bid => {
    // This is simplified - in production you'd check if auction is still active
    return true;
  }).length;

  return (
    <div>
      <Title level={2}>
        <UserOutlined style={{ marginRight: '12px' }} />
        My Dashboard
      </Title>

      {/* Statistics Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="My Auctions"
              value={myAuctions.length}
              prefix={<ShoppingCartOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Bids Placed"
              value={myBids.length}
              prefix={<FireOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Auctions Won"
              value={wonAuctions.length}
              prefix={<TrophyOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Bid Amount"
              value={totalBidAmount.toFixed(2)}
              prefix={<DollarOutlined />}
              precision={2}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Tabs for different views */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="My Auctions" key="1">
            <List
              loading={loading}
              dataSource={myAuctions}
              locale={{ emptyText: 'You haven\'t created any auctions yet' }}
              renderItem={auction => (
                <List.Item
                  actions={[
                    <Button 
                      type="link" 
                      onClick={() => navigate(`/auctions/${auction.id}`)}
                    >
                      View Details
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text strong>{auction.title}</Text>
                        {getStatusTag(auction.status)}
                        {auction.status === 'ACTIVE' && getTimeRemaining(auction.endTime)}
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Text>{auction.description}</Text>
                        <Space>
                          <Text type="secondary">Starting Price:</Text>
                          <Text strong>${auction.startingPrice}</Text>
                          <Text type="secondary">Current Price:</Text>
                          <Text strong style={{ color: '#1890ff' }}>${auction.currentPrice}</Text>
                        </Space>
                        <Text type="secondary">
                          Created: {moment(auction.createdAt).format('MMM DD, YYYY HH:mm')}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </TabPane>

          <TabPane tab="My Bids" key="2">
            <List
              loading={loading}
              dataSource={myBids}
              locale={{ emptyText: 'You haven\'t placed any bids yet' }}
              renderItem={bid => (
                <List.Item
                  actions={[
                    <Button 
                      type="link" 
                      onClick={() => navigate(`/auctions/${bid.auctionId}`)}
                    >
                      View Auction
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text strong>Bid Amount: ${bid.amount}</Text>
                        <Tag color="blue">Server {bid.serverId}</Tag>
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Space>
                          <Text type="secondary">Auction ID:</Text>
                          <Text code>{bid.auctionId}</Text>
                        </Space>
                        <Space>
                          <Text type="secondary">Placed:</Text>
                          <Text>{moment(bid.timestamp).format('MMM DD, YYYY HH:mm:ss')}</Text>
                        </Space>
                        <Space>
                          <Text type="secondary">Logical Clock:</Text>
                          <Tag>{bid.logicalTimestamp}</Tag>
                        </Space>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </TabPane>

          <TabPane tab="Won Auctions" key="3">
            <List
              loading={loading}
              dataSource={wonAuctions}
              locale={{ emptyText: 'You haven\'t won any auctions yet' }}
              renderItem={auction => (
                <List.Item
                  actions={[
                    <Button 
                      type="primary" 
                      icon={<CheckCircleOutlined />}
                      onClick={() => navigate(`/auctions/${auction.id}`)}
                    >
                      View Details
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    avatar={<TrophyOutlined style={{ fontSize: '32px', color: '#faad14' }} />}
                    title={
                      <Space>
                        <Text strong>{auction.title}</Text>
                        <Tag color="gold">WON</Tag>
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Text>{auction.description}</Text>
                        <Space>
                          <Text type="secondary">Winning Bid:</Text>
                          <Text strong style={{ color: '#52c41a', fontSize: '16px' }}>
                            ${auction.winningBid?.amount}
                          </Text>
                        </Space>
                        <Text type="secondary">
                          Won on: {moment(auction.endTime).format('MMM DD, YYYY HH:mm')}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
}

export default UserDashboard;

