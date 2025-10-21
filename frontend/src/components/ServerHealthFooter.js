import React, { useState, useEffect } from 'react';
import { auctionAPI } from '../services/api';
import { CloudServerOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';

const ServerHealthFooter = () => {
  const [serverStatus, setServerStatus] = useState({
    serverHealthy: false,
    logicalClock: 0,
    serverId: null,
    isCoordinator: false,
    loading: true
  });

  useEffect(() => {
    checkServerHealth();
    const interval = setInterval(checkServerHealth, 5000);
    return () => clearInterval(interval);
  }, []);

  const checkServerHealth = async () => {
    try {
      const status = await auctionAPI.getServerStatus();
      setServerStatus({
        serverHealthy: status.serverHealthy || false,
        logicalClock: status.logicalClock || 0,
        serverId: status.serverId || '?',
        isCoordinator: status.isCoordinator || false,
        loading: false
      });
    } catch (error) {
      setServerStatus(prev => ({
        ...prev,
        serverHealthy: false,
        loading: false
      }));
    }
  };

  const getHealthDotClass = () => {
    if (serverStatus.loading) return 'server-health-dot connecting';
    return serverStatus.serverHealthy ? 'server-health-dot healthy' : 'server-health-dot unhealthy';
  };

  const getHealthIcon = () => {
    if (serverStatus.loading) return <LoadingOutlined spin />;
    return serverStatus.serverHealthy ? 
      <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
      <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
  };

  const getHealthText = () => {
    if (serverStatus.loading) return 'Connecting...';
    return serverStatus.serverHealthy ? 'Connected' : 'Disconnected';
  };

  return (
    <div style={{
      position: 'fixed',
      bottom: 0,
      left: 0,
      right: 0,
      height: '40px',
      background: 'linear-gradient(90deg, rgba(2, 62, 138, 0.95), rgba(0, 119, 182, 0.95))',
      backdropFilter: 'blur(10px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      color: 'white',
      fontSize: '13px',
      boxShadow: '0 -2px 10px rgba(0, 0, 0, 0.1)',
      zIndex: 999,
      fontFamily: 'monospace'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span className={getHealthDotClass()} />
          {getHealthIcon()}
          <span style={{ fontWeight: '600' }}>{getHealthText()}</span>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <CloudServerOutlined />
          <span>Server {serverStatus.serverId}</span>
          {serverStatus.isCoordinator && (
            <span style={{
              background: 'rgba(255, 215, 0, 0.3)',
              padding: '2px 8px',
              borderRadius: '10px',
              fontSize: '11px',
              fontWeight: 'bold',
              color: '#ffd700'
            }}>
              COORDINATOR
            </span>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div>
          <span style={{ opacity: 0.8 }}>Logical Clock:</span>{' '}
          <span style={{ fontWeight: 'bold', color: '#48cae4' }}>{serverStatus.logicalClock}</span>
        </div>
        
        <div style={{ opacity: 0.6, fontSize: '11px' }}>
          DOAS v1.0
        </div>
      </div>
    </div>
  );
};

export default ServerHealthFooter;


