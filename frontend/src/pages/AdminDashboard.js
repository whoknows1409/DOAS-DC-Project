import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Table, 
  Tag, 
  Button, 
  Space, 
  Typography, 
  Progress,
  Alert,
  Select,
  message
} from 'antd';
import { 
  CloudServerOutlined, 
  ClockCircleOutlined, 
  SyncOutlined, 
  ThunderboltOutlined,
  ReloadOutlined,
  PlayCircleOutlined
} from '@ant-design/icons';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { adminAPI } from '../services/api';

const { Title, Text } = Typography;
const { Option } = Select;

function AdminDashboard() {
  const [systemStatus, setSystemStatus] = useState(null);
  const [clockSync, setClockSync] = useState(null);
  const [replicationLag, setReplicationLag] = useState(null);
  const [loadDistribution, setLoadDistribution] = useState(null);
  const [electionStatus, setElectionStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshInterval, setRefreshInterval] = useState(5000);

  useEffect(() => {
    loadAllData();
    
    const interval = setInterval(loadAllData, refreshInterval);
    return () => clearInterval(interval);
  }, [refreshInterval]);

  const loadAllData = async () => {
    try {
      const [
        statusData,
        clockData,
        replicationData,
        loadData,
        electionData
      ] = await Promise.all([
        adminAPI.getSystemStatus(),
        adminAPI.getClockSyncStatus(),
        adminAPI.getReplicationLag(),
        adminAPI.getLoadDistribution(),
        adminAPI.getElectionStatus()
      ]);

      setSystemStatus(statusData);
      setClockSync(clockData);
      setReplicationLag(replicationData);
      setLoadDistribution(loadData);
      setElectionStatus(electionData);
      setLoading(false);
    } catch (error) {
      console.error('Failed to load admin data:', error);
      message.error('Failed to load admin dashboard data');
      setLoading(false);
    }
  };

  const handleTriggerElection = async () => {
    try {
      await adminAPI.triggerElection();
      message.success('Election triggered successfully');
      setTimeout(loadAllData, 2000);
    } catch (error) {
      console.error('Failed to trigger election:', error);
      message.error('Failed to trigger election');
    }
  };

  const handleClockSync = async () => {
    try {
      await adminAPI.triggerClockSync();
      message.success('Clock synchronization triggered');
      setTimeout(loadAllData, 2000);
    } catch (error) {
      console.error('Failed to trigger clock sync:', error);
      message.error('Failed to trigger clock sync');
    }
  };

  const getServerColumns = () => [
    {
      title: 'Server ID',
      dataIndex: 'serverId',
      key: 'serverId',
      render: (id) => <Tag color="blue">Server {id}</Tag>
    },
    {
      title: 'Status',
      dataIndex: 'isHealthy',
      key: 'isHealthy',
      render: (healthy, record) => {
        // Check both isHealthy and healthy properties (backend serialization)
        const isHealthy = record.isHealthy !== undefined ? record.isHealthy : record.healthy;
        return (
          <Tag color={isHealthy ? 'green' : 'red'}>
            {isHealthy ? 'Healthy' : 'Unhealthy'}
          </Tag>
        );
      }
    },
    {
      title: 'Coordinator',
      dataIndex: 'isCoordinator',
      key: 'isCoordinator',
      render: (isCoord, record) => {
        // Check both isCoordinator and coordinator properties
        const isCoordinator = record.isCoordinator !== undefined ? record.isCoordinator : record.coordinator;
        return isCoordinator ? <Tag color="gold">Coordinator</Tag> : '-';
      }
    },
    {
      title: 'Logical Clock',
      dataIndex: 'logicalClock',
      key: 'logicalClock',
      render: (clock) => <Text code>{clock}</Text>
    },
    {
      title: 'Active Connections',
      dataIndex: 'activeConnections',
      key: 'activeConnections'
    },
    {
      title: 'Uptime',
      dataIndex: 'uptime',
      key: 'uptime',
      render: (uptime) => `${Math.floor(uptime / 1000)}s`
    }
  ];

  const getClockDriftData = () => {
    if (!clockSync?.clockDrifts) return [];
    
    return Object.entries(clockSync.clockDrifts).map(([serverId, drift]) => ({
      server: `Server ${serverId}`,
      drift: drift
    }));
  };

  const getReplicationLagData = () => {
    if (!replicationLag?.peerLag) return [];
    
    return Object.entries(replicationLag.peerLag).map(([serverId, lag]) => ({
      server: `Server ${serverId}`,
      lag: lag >= 0 ? lag : 0
    }));
  };

  const getLoadBalanceData = () => {
    if (!loadDistribution?.serverLoad) return [];
    
    return Object.entries(loadDistribution.serverLoad).map(([serverId, load]) => ({
      server: `Server ${serverId}`,
      connections: load.activeConnections || 0,
      healthy: load.isHealthy ? 1 : 0
    }));
  };

  if (loading) {
    return <div>Loading admin dashboard...</div>;
  }

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Current Coordinator"
              value={systemStatus?.coordinatorId || 'N/A'}
              prefix={<CloudServerOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Local Logical Clock"
              value={systemStatus?.logicalClock || 0}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Active Auctions"
              value={systemStatus?.activeAuctions || 0}
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text>Actions</Text>
              <Space>
                <Button 
                  icon={<PlayCircleOutlined />} 
                  onClick={handleTriggerElection}
                  size="small"
                >
                  Election
                </Button>
                <Button 
                  icon={<SyncOutlined />} 
                  onClick={handleClockSync}
                  size="small"
                >
                  Sync
                </Button>
                <Button 
                  icon={<ReloadOutlined />} 
                  onClick={loadAllData}
                  size="small"
                >
                  Refresh
                </Button>
              </Space>
            </Space>
          </Card>
        </Col>
      </Row>

      {/* Election Status */}
      {electionStatus && (
        <Alert
          message={`Election Status: ${electionStatus.electionInProgress ? 'In Progress' : 'Stable'}`}
          description={`Current Coordinator: Server ${electionStatus.currentCoordinator} | This Server: ${electionStatus.isCoordinator ? 'Coordinator' : 'Participant'}`}
          type={electionStatus.electionInProgress ? 'warning' : 'success'}
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Server Status Table */}
      <Card title="Server Cluster Status" style={{ marginBottom: 16 }}>
        <Table
          columns={getServerColumns()}
          dataSource={systemStatus?.servers || []}
          rowKey="serverId"
          pagination={false}
          size="small"
        />
      </Card>

      <Row gutter={[16, 16]}>
        {/* Clock Synchronization */}
        <Col span={12}>
          <Card title="Clock Synchronization" className="clock-sync">
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title="Local Clock"
                  value={clockSync?.localClock || 0}
                  valueStyle={{ fontSize: '16px' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="Sync Status"
                  value={clockSync?.syncStatus || 'Unknown'}
                  valueStyle={{ 
                    fontSize: '14px',
                    color: clockSync?.syncStatus === 'SYNCHRONIZED' ? '#3f8600' : '#cf1322'
                  }}
                />
              </Col>
            </Row>
            
            {getClockDriftData().length > 0 && (
              <div style={{ marginTop: 16 }}>
                <Title level={5}>Clock Drift</Title>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={getClockDriftData()}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="server" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="drift" fill="#1890ff" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>
        </Col>

        {/* Replication Lag */}
        <Col span={12}>
          <Card title="Replication Lag Metrics">
            <Row gutter={16}>
              <Col span={8}>
                <Statistic
                  title="Average Lag"
                  value={replicationLag?.averageLag || 0}
                  suffix="ms"
                  valueStyle={{ fontSize: '16px' }}
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="Max Lag"
                  value={replicationLag?.maxLag || 0}
                  suffix="ms"
                  valueStyle={{ fontSize: '16px', color: '#cf1322' }}
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="Healthy Nodes"
                  value={Object.values(replicationLag?.peerLag || {}).filter(lag => lag >= 0).length}
                  suffix={`/${Object.keys(replicationLag?.peerLag || {}).length}`}
                  valueStyle={{ fontSize: '16px', color: '#3f8600' }}
                />
              </Col>
            </Row>
            
            {getReplicationLagData().length > 0 && (
              <div style={{ marginTop: 16 }}>
                <Title level={5}>Replication Lag by Server</Title>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={getReplicationLagData()}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="server" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="lag" fill="#52c41a" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* Load Distribution */}
      <Card title="Load Distribution" className="load-balancing" style={{ marginTop: 16 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Statistic
              title="Total Connections"
              value={loadDistribution?.totalConnections || 0}
              valueStyle={{ fontSize: '18px' }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="Load Balance Score"
              value={loadDistribution?.loadBalanceScore || 0}
              precision={2}
              suffix="/ 1.0"
              valueStyle={{ 
                fontSize: '18px',
                color: (loadDistribution?.loadBalanceScore || 0) > 0.8 ? '#3f8600' : '#cf1322'
              }}
            />
          </Col>
          <Col span={8}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text>Refresh Interval</Text>
              <Select
                value={refreshInterval}
                onChange={setRefreshInterval}
                style={{ width: '100%' }}
                size="small"
              >
                <Option value={1000}>1s</Option>
                <Option value={5000}>5s</Option>
                <Option value={10000}>10s</Option>
                <Option value={30000}>30s</Option>
              </Select>
            </Space>
          </Col>
        </Row>
        
        {getLoadBalanceData().length > 0 && (
          <div style={{ marginTop: 16 }}>
            <Title level={5}>Connection Distribution</Title>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={getLoadBalanceData()}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="server" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="connections" fill="#1890ff" name="Connections" />
                <Bar dataKey="healthy" fill="#52c41a" name="Healthy" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
    </div>
  );
}

export default AdminDashboard;