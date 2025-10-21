import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Card, 
  Button, 
  InputNumber, 
  List, 
  Typography, 
  Tag, 
  Space, 
  Statistic, 
  Row, 
  Col, 
  message,
  Descriptions,
  Avatar,
  Image
} from 'antd';
import { UserOutlined, ClockCircleOutlined, DollarOutlined, PictureOutlined } from '@ant-design/icons';
import { auctionAPI } from '../services/api';
import WebSocketService from '../services/websocket';
import moment from 'moment';
import CountdownTimer from '../components/CountdownTimer';
import StatusBadge from '../components/StatusBadge';

const { Title, Text } = Typography;

function AuctionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [auction, setAuction] = useState(null);
  const [bids, setBids] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bidAmount, setBidAmount] = useState(0);
  const [placingBid, setPlacingBid] = useState(false);
  const [logicalTimestamp, setLogicalTimestamp] = useState(0);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (id) {
      loadAuction();
      loadBids();
      connectWebSocket();

      // Refresh auction data every 5 seconds
      const interval = setInterval(() => {
        loadAuction();
        loadBids();
      }, 5000);

      return () => {
        clearInterval(interval);
        WebSocketService.unsubscribeFromAuction(id);
      };
    }
  }, [id]);

  const loadAuction = async () => {
    try {
      const data = await auctionAPI.getAuction(id);
      if (data) {
        setAuction(data);
        setBidAmount(data.currentPrice + 1); // Minimum bid is current price + 1
        setLoading(false);
      } else {
        message.error('Auction not found');
        setLoading(false);
      }
    } catch (error) {
      console.error('Failed to load auction:', error);
      message.error('Failed to load auction');
      setLoading(false);
    }
  };

  const loadBids = async () => {
    try {
      const data = await auctionAPI.getBids(id);
      setBids(data);
    } catch (error) {
      console.error('Failed to load bids:', error);
    }
  };

  const connectWebSocket = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user) {
        console.log('No user found, skipping WebSocket connection');
        return;
      }

      await WebSocketService.connect();
      setConnected(true);
      
      // Subscribe to auction updates for real-time notifications
      WebSocketService.subscribeToAuction(id, (update) => {
        console.log('Received auction update:', update);
        
        if (update.type === 'BID_UPDATE') {
          setBids(prev => [update.bid, ...prev]);
          setAuction(prev => ({...prev, currentPrice: update.bid.amount}));
          setLogicalTimestamp(update.logicalTimestamp);
          message.info(`New bid placed: $${update.bid.amount}`);
        } else if (update.type === 'AUCTION_UPDATE') {
          setAuction(update.auction);
          setLogicalTimestamp(update.logicalTimestamp);
        }
      });

      // Subscribe to personal bid responses
      WebSocketService.subscribeToAuctionUpdates(id, user.id, (response) => {
        console.log('Bid response:', response);
        if (response.type === 'BID_PLACED') {
          setLogicalTimestamp(response.logicalTimestamp);
        } else if (response.type === 'BID_ERROR') {
          message.error(response.error);
        }
      });

    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
      setConnected(false);
    }
  };

  const isAuctionEnded = () => {
    if (auction.status !== 'ACTIVE') return true;
    return moment().isAfter(moment(auction.endTime));
  };

  const handlePlaceBid = async () => {
    // Check if auction has ended
    if (isAuctionEnded()) {
      message.error('This auction has ended. No more bids can be placed.');
      return;
    }

    if (bidAmount <= auction.currentPrice) {
      message.error('Bid amount must be higher than current price');
      return;
    }

    setPlacingBid(true);
    
    try {
      // Get current user from localStorage
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user) {
        message.error('Please login to place a bid');
        setPlacingBid(false);
        return;
      }

      // Always use REST API for reliability
      const bidData = {
        bidderId: user.id,
        amount: bidAmount
      };
      
      const response = await auctionAPI.placeBid(id, bidData);
      
      if (response) {
        setBids(prev => [response, ...prev]);
        setAuction(prev => ({...prev, currentPrice: bidAmount}));
        setLogicalTimestamp(response.logicalTimestamp || logicalTimestamp + 1);
        setBidAmount(bidAmount + 1); // Set next minimum bid
        message.success('Bid placed successfully!');
      }
      setPlacingBid(false);
    } catch (error) {
      console.error('Failed to place bid:', error);
      message.error(error.response?.data?.message || 'Failed to place bid. Please try again.');
      setPlacingBid(false);
    }
  };

  // Removed old getTimeRemaining function - now using CountdownTimer component

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!auction) {
    return <div>Auction not found</div>;
  }

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={16}>
          <Card>
            <Space direction="vertical" style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={2}>{auction.title}</Title>
                <StatusBadge status={auction.status} />
              </div>
              
              {/* Countdown Timer - Large version for detail page */}
              <div style={{ marginBottom: '16px' }}>
                <CountdownTimer endTime={auction.endTime} size="large" />
              </div>

              {auction.imageUrl ? (
                <div style={{ 
                  marginBottom: '16px', 
                  textAlign: 'center',
                  background: '#f0f0f0',
                  borderRadius: '8px',
                  overflow: 'hidden'
                }}>
                  <Image
                    src={auction.imageUrl}
                    alt={auction.title}
                    style={{ maxWidth: '100%', maxHeight: '500px', objectFit: 'contain' }}
                    fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%3E%3Crect fill='%23f0f0f0' width='100' height='100'/%3E%3Ctext fill='%23999' x='50%25' y='50%25' text-anchor='middle' dy='.3em' font-family='sans-serif' font-size='14'%3EImage Not Available%3C/text%3E%3C/svg%3E"
                  />
                </div>
              ) : (
                <div style={{ 
                  marginBottom: '16px',
                  height: '300px', 
                  background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
                  borderRadius: '8px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <PictureOutlined style={{ fontSize: '64px', color: 'rgba(255,255,255,0.5)' }} />
                </div>
              )}
              
              <Text type="secondary">{auction.description}</Text>
              
              <Descriptions column={2}>
                <Descriptions.Item label="Starting Price">
                  ${auction.startingPrice}
                </Descriptions.Item>
                <Descriptions.Item label="Current Price">
                  <Text strong style={{ fontSize: '18px', color: '#1890ff' }}>
                    ${auction.currentPrice}
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="End Time">
                  {moment(auction.endTime).format('MMM DD, YYYY HH:mm')}
                </Descriptions.Item>
                <Descriptions.Item label="Status">
                  <Tag color={auction.status === 'ACTIVE' ? 'green' : 'red'}>
                    {auction.status}
                  </Tag>
                </Descriptions.Item>
                {auction.status === 'ENDED' && auction.winnerName && (
                  <Descriptions.Item label="Winner" span={2}>
                    <Space>
                      <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#52c41a' }} />
                      <Text strong style={{ fontSize: '16px', color: '#52c41a' }}>
                        {auction.winnerName}
                      </Text>
                      <Tag color="gold">WINNER</Tag>
                    </Space>
                  </Descriptions.Item>
                )}
              </Descriptions>

              <div style={{ background: '#f0f2f5', padding: '16px', borderRadius: '6px' }}>
                <Space>
                  <ClockCircleOutlined />
                  <Text>Logical Timestamp: </Text>
                  <Tag color="blue">{logicalTimestamp}</Tag>
                  <Text type="secondary">({connected ? 'Connected' : 'Disconnected'})</Text>
                </Space>
              </div>
            </Space>
          </Card>

          <Card title="Place Bid" style={{ marginTop: 16 }}>
            <Row gutter={16} align="middle">
              <Col span={8}>
                <InputNumber
                  style={{ width: '100%' }}
                  min={auction.currentPrice + 1}
                  step={1}
                  value={bidAmount}
                  onChange={setBidAmount}
                  prefix={<DollarOutlined />}
                  size="large"
                />
              </Col>
              <Col span={8}>
                <Button
                  type="primary"
                  size="large"
                  onClick={handlePlaceBid}
                  loading={placingBid}
                  disabled={isAuctionEnded()}
                  block
                >
                  {isAuctionEnded() ? 'Auction Ended' : 'Place Bid'}
                </Button>
              </Col>
              <Col span={8}>
                <Text type="secondary">
                  Minimum bid: ${auction.currentPrice + 1}
                </Text>
              </Col>
            </Row>
          </Card>
        </Col>

        <Col span={8}>
          <Card title="Bid History" className="bid-history">
            <List
              dataSource={bids}
              renderItem={(bid, index) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <Avatar icon={<UserOutlined />} style={{ 
                        backgroundColor: index === 0 ? '#52c41a' : '#1890ff' 
                      }} />
                    }
                    title={
                      <Space>
                        <Text strong>${bid.amount}</Text>
                        <Tag size="small">Server {bid.serverId}</Tag>
                        {index === 0 && <Tag color="gold">Highest</Tag>}
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Text strong style={{ color: '#1890ff' }}>
                          {bid.bidderName || 'Anonymous'}
                        </Text>
                        <Text type="secondary">
                          {moment(bid.timestamp).format('HH:mm:ss')}
                        </Text>
                        <Text type="secondary" style={{ fontSize: '11px' }}>
                          Logical: {bid.logicalTimestamp}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default AuctionDetail;