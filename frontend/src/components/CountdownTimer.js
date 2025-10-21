import React, { useState, useEffect } from 'react';
import { Statistic } from 'antd';
import { ClockCircleOutlined, FireOutlined } from '@ant-design/icons';
import moment from 'moment';

const CountdownTimer = ({ endTime, size = 'small' }) => {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

  function calculateTimeLeft() {
    const now = moment();
    const end = moment(endTime);
    const diff = end.diff(now);

    if (diff <= 0) {
      return { ended: true, days: 0, hours: 0, minutes: 0, seconds: 0 };
    }

    const duration = moment.duration(diff);
    return {
      ended: false,
      days: Math.floor(duration.asDays()),
      hours: duration.hours(),
      minutes: duration.minutes(),
      seconds: duration.seconds(),
      totalSeconds: duration.asSeconds()
    };
  }

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(calculateTimeLeft());
    }, 1000);

    return () => clearInterval(timer);
  }, [endTime]);

  if (timeLeft.ended) {
    return (
      <div style={{ 
        color: '#ff4d4f', 
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        gap: '4px'
      }}>
        <FireOutlined />
        <span>Auction Ended</span>
      </div>
    );
  }

  // Determine urgency level
  const isUrgent = timeLeft.totalSeconds < 3600; // Less than 1 hour
  const isCritical = timeLeft.totalSeconds < 300; // Less than 5 minutes

  const urgencyColor = isCritical ? '#ff4d4f' : isUrgent ? '#faad14' : '#52c41a';

  if (size === 'large') {
    return (
      <div style={{
        display: 'flex',
        gap: '16px',
        justifyContent: 'center',
        padding: '16px',
        background: 'linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(82, 196, 160, 0.1))',
        borderRadius: '12px',
        border: `2px solid ${urgencyColor}20`,
      }}>
        {timeLeft.days > 0 && (
          <TimeUnit value={timeLeft.days} label="Days" color={urgencyColor} animate={isCritical} />
        )}
        <TimeUnit value={timeLeft.hours} label="Hours" color={urgencyColor} animate={isCritical} />
        <TimeUnit value={timeLeft.minutes} label="Minutes" color={urgencyColor} animate={isCritical} />
        <TimeUnit value={timeLeft.seconds} label="Seconds" color={urgencyColor} animate={true} />
      </div>
    );
  }

  // Compact format for cards
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: '6px',
      color: urgencyColor,
      fontWeight: '600',
      animation: isCritical ? 'pulse 1.5s infinite' : 'none'
    }}>
      <ClockCircleOutlined style={{ fontSize: '16px' }} />
      <span style={{ fontSize: '14px' }}>
        {timeLeft.days > 0 && `${timeLeft.days}d `}
        {String(timeLeft.hours).padStart(2, '0')}:
        {String(timeLeft.minutes).padStart(2, '0')}:
        {String(timeLeft.seconds).padStart(2, '0')}
      </span>
    </div>
  );
};

const TimeUnit = ({ value, label, color, animate }) => (
  <div style={{
    textAlign: 'center',
    minWidth: '60px',
    animation: animate ? 'countdownPulse 1s infinite' : 'none'
  }}>
    <div style={{
      fontSize: '28px',
      fontWeight: 'bold',
      color: color,
      lineHeight: '1',
      marginBottom: '4px',
      fontFamily: 'monospace'
    }}>
      {String(value).padStart(2, '0')}
    </div>
    <div style={{
      fontSize: '12px',
      color: '#888',
      textTransform: 'uppercase',
      letterSpacing: '1px'
    }}>
      {label}
    </div>
  </div>
);

export default CountdownTimer;


