import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, Dropdown, Avatar, Switch } from 'antd';
import {
  DashboardOutlined,
  ShoppingCartOutlined,
  BarChartOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  BulbOutlined,
  BulbFilled
} from '@ant-design/icons';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';
import AuctionList from './pages/AuctionList';
import AdminDashboard from './pages/AdminDashboard';
import EnhancedAdminDashboard from './pages/EnhancedAdminDashboard';
import UserDashboard from './pages/UserDashboard';
import AuctionDetail from './pages/AuctionDetail';
import Profile from './pages/Profile';
import Login from './pages/Login';
import Register from './pages/Register';
import ServerHealthFooter from './components/ServerHealthFooter';
import './App.css';

const { Header, Sider, Content } = Layout;

function AppContent() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDarkMode, toggleTheme } = useTheme();
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const [user, setUser] = useState(null);
  const [userRole, setUserRole] = useState(null);

  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    const savedRole = localStorage.getItem('userRole');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
      setUserRole(savedRole);
    }
  }, []);

  useEffect(() => {
    // Update selected key based on current path
    const path = location.pathname;
    if (path === '/' || path === '/dashboard') {
      setSelectedKey('dashboard');
    } else if (path.startsWith('/auctions')) {
      setSelectedKey('auctions');
    } else if (path === '/admin') {
      setSelectedKey('admin');
    }
  }, [location]);

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('userRole');
    setUser(null);
    setUserRole(null);
  };

  const handleMenuClick = ({ key }) => {
    setSelectedKey(key);
    if (key === 'dashboard') {
      navigate('/dashboard');
    } else if (key === 'auctions') {
      navigate('/auctions');
    } else if (key === 'admin') {
      navigate('/admin');
    }
  };

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: userRole === 'ADMIN' ? 'System Dashboard' : 'My Dashboard',
    },
    {
      key: 'auctions',
      icon: <ShoppingCartOutlined />,
      label: 'Auctions',
    },
  ];

  if (userRole === 'ADMIN') {
    menuItems.push({
      key: 'admin',
      icon: <BarChartOutlined />,
      label: 'Admin Panel',
    });
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      onClick: handleLogout,
    },
  ];

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

      return (
        <Layout style={{ minHeight: '100vh', background: isDarkMode ? '#010226' : '#f4fcfe', paddingBottom: '40px' }}>
      <Header style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        background: isDarkMode ? '#010226' : '#023e8a',
        boxShadow: isDarkMode ? '0 4px 6px rgba(0,0,0,0.5)' : '0 2px 4px rgba(0,0,0,0.2)',
        borderBottom: isDarkMode ? '1px solid #012451' : '1px solid #035cd1'
      }}>
        <div style={{ color: 'white', fontSize: '18px', fontWeight: 'bold' }}>
          DOAS - Distributed Online Auction System
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {isDarkMode ? 
              <BulbFilled style={{ color: '#48cae4', fontSize: '18px' }} /> : 
              <BulbOutlined style={{ color: '#caf0f8', fontSize: '18px' }} />
            }
            <Switch 
              checked={isDarkMode}
              onChange={toggleTheme}
              checkedChildren="Dark"
              unCheckedChildren="Light"
              style={{
                background: isDarkMode ? '#0077b6' : 'rgba(255, 255, 255, 0.3)'
              }}
            />
          </div>
          <span style={{ color: 'white' }}>
            Welcome, {user.username} ({userRole})
          </span>
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
          >
            <Avatar 
              src={user.profileImageUrl} 
              icon={!user.profileImageUrl && <UserOutlined />}
              style={{ 
                cursor: 'pointer',
                background: user.profileImageUrl ? 'transparent' : undefined
              }} 
            />
          </Dropdown>
        </div>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: isDarkMode ? '#011836' : '#ffffff' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            theme={isDarkMode ? 'dark' : 'light'}
            style={{ 
              height: '100%', 
              borderRight: isDarkMode ? '1px solid #012451' : '1px solid #d2f3f9',
              background: isDarkMode ? '#011836' : '#ffffff'
            }}
            items={menuItems}
            onClick={handleMenuClick}
          />
        </Sider>
        <Layout style={{ padding: '0 24px 24px', background: isDarkMode ? '#010226' : '#f4fcfe' }}>
          <Content
            style={{
              background: isDarkMode ? '#011836' : '#ffffff',
              padding: 24,
              margin: 0,
              minHeight: 280,
              borderRadius: '12px',
              marginTop: '24px',
              boxShadow: isDarkMode 
                ? '0 4px 6px rgba(0,0,0,0.5)' 
                : '0 2px 4px rgba(3,4,94,0.08)'
            }}
          >
            <Routes>
              <Route path="/" element={userRole === 'ADMIN' ? <EnhancedAdminDashboard /> : <UserDashboard />} />
              <Route path="/dashboard" element={userRole === 'ADMIN' ? <EnhancedAdminDashboard /> : <UserDashboard />} />
              <Route path="/auctions" element={<AuctionList />} />
              <Route path="/auctions/:id" element={<AuctionDetail />} />
              <Route path="/admin" element={userRole === 'ADMIN' ? <AdminDashboard /> : <Navigate to="/" replace />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/login" element={<Navigate to="/" replace />} />
              <Route path="/register" element={<Navigate to="/" replace />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
      <ServerHealthFooter />
    </Layout>
  );
}

function App() {
  return (
    <ThemeProvider>
      <Router>
        <AppContent />
      </Router>
    </ThemeProvider>
  );
}

export default App;