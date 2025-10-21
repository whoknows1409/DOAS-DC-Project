import React from 'react';
import { CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';

const StatusBadge = ({ status }) => {
  const configs = {
    ACTIVE: {
      icon: <CheckCircleOutlined />,
      className: 'status-badge status-badge-active',
      text: 'Active'
    },
    ENDED: {
      icon: <CloseCircleOutlined />,
      className: 'status-badge status-badge-ended',
      text: 'Ended'
    },
    PENDING: {
      icon: <ClockCircleOutlined />,
      className: 'status-badge status-badge-pending',
      text: 'Pending'
    }
  };

  const config = configs[status] || configs.PENDING;

  return (
    <span className={config.className}>
      {config.icon}
      {config.text}
    </span>
  );
};

export default StatusBadge;


