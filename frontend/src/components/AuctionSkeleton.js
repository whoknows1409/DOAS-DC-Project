import React from 'react';
import { Card, Row, Col } from 'antd';

const AuctionSkeleton = ({ count = 3 }) => {
  return (
    <Row gutter={[16, 16]}>
      {Array.from({ length: count }).map((_, index) => (
        <Col xs={24} sm={12} lg={8} key={index}>
          <Card
            style={{
              borderRadius: '12px',
              overflow: 'hidden',
              animation: 'fadeIn 0.5s ease',
              animationDelay: `${index * 0.1}s`,
              animationFillMode: 'both'
            }}
          >
            {/* Image skeleton */}
            <div
              className="skeleton-shimmer"
              style={{
                width: '100%',
                height: '200px',
                borderRadius: '8px',
                marginBottom: '16px'
              }}
            />

            {/* Title skeleton */}
            <div
              className="skeleton-shimmer"
              style={{
                width: '80%',
                height: '24px',
                borderRadius: '4px',
                marginBottom: '12px'
              }}
            />

            {/* Description skeleton */}
            <div
              className="skeleton-shimmer"
              style={{
                width: '100%',
                height: '16px',
                borderRadius: '4px',
                marginBottom: '8px'
              }}
            />
            <div
              className="skeleton-shimmer"
              style={{
                width: '60%',
                height: '16px',
                borderRadius: '4px',
                marginBottom: '16px'
              }}
            />

            {/* Price and timer row */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
              <div
                className="skeleton-shimmer"
                style={{
                  width: '40%',
                  height: '32px',
                  borderRadius: '6px'
                }}
              />
              <div
                className="skeleton-shimmer"
                style={{
                  width: '40%',
                  height: '32px',
                  borderRadius: '6px'
                }}
              />
            </div>

            {/* Button skeleton */}
            <div
              className="skeleton-shimmer"
              style={{
                width: '100%',
                height: '40px',
                borderRadius: '8px'
              }}
            />
          </Card>
        </Col>
      ))}
    </Row>
  );
};

export default AuctionSkeleton;


