import React from 'react';
import { Empty, Button } from 'antd';
import { 
  ShoppingOutlined, 
  InboxOutlined, 
  TrophyOutlined,
  FileSearchOutlined,
  PlusCircleOutlined 
} from '@ant-design/icons';

const EmptyState = ({ type = 'auctions', onAction, actionText, description }) => {
  const configs = {
    auctions: {
      icon: <ShoppingOutlined style={{ fontSize: '80px', color: '#0077b6' }} />,
      title: 'No Active Auctions',
      defaultDescription: 'There are no active auctions at the moment. Check back soon or create your own auction!',
      actionIcon: <PlusCircleOutlined />,
      defaultActionText: 'Create Auction'
    },
    bids: {
      icon: <InboxOutlined style={{ fontSize: '80px', color: '#48cae4' }} />,
      title: 'No Bids Yet',
      defaultDescription: 'Be the first to place a bid on this auction!',
      actionIcon: null,
      defaultActionText: null
    },
    wonAuctions: {
      icon: <TrophyOutlined style={{ fontSize: '80px', color: '#ffd700' }} />,
      title: 'No Won Auctions',
      defaultDescription: "You haven't won any auctions yet. Keep bidding to win your first auction!",
      actionIcon: <ShoppingOutlined />,
      defaultActionText: 'Browse Auctions'
    },
    search: {
      icon: <FileSearchOutlined style={{ fontSize: '80px', color: '#666' }} />,
      title: 'No Results Found',
      defaultDescription: 'Try adjusting your search criteria or filters.',
      actionIcon: null,
      defaultActionText: null
    },
    generic: {
      icon: <InboxOutlined style={{ fontSize: '80px', color: '#999' }} />,
      title: 'No Data',
      defaultDescription: 'There is no data to display at this time.',
      actionIcon: null,
      defaultActionText: null
    }
  };

  const config = configs[type] || configs.generic;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '60px 20px',
      minHeight: '400px',
      background: 'linear-gradient(135deg, rgba(2, 62, 138, 0.03), rgba(0, 180, 216, 0.03))',
      borderRadius: '16px',
      border: '2px dashed rgba(0, 119, 182, 0.2)',
    }}>
      <div style={{
        marginBottom: '24px',
        animation: 'float 3s ease-in-out infinite'
      }}>
        {config.icon}
      </div>
      <div style={{
        textAlign: 'center',
        maxWidth: '500px'
      }}>
        <h3 style={{
          fontSize: '24px',
          fontWeight: '600',
          color: '#023e8a',
          marginBottom: '12px'
        }}>
          {config.title}
        </h3>
        <p style={{
          fontSize: '15px',
          color: '#666',
          marginBottom: '24px',
          lineHeight: '1.6'
        }}>
          {description || config.defaultDescription}
        </p>
        {onAction && (
          <Button
            type="primary"
            size="large"
            icon={config.actionIcon}
            onClick={onAction}
            style={{
              background: 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
              border: 'none',
              boxShadow: '0 4px 12px rgba(0, 119, 182, 0.3)',
              height: '44px',
              borderRadius: '8px',
              fontWeight: '600'
            }}
          >
            {actionText || config.defaultActionText}
          </Button>
        )}
      </div>
    </div>
  );
};

export default EmptyState;


