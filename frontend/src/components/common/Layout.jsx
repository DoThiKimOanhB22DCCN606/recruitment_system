import React, { useState, useEffect } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  Container,
  Button,
  Box,
  Stack,
  IconButton,
  Badge,
  Menu,
  MenuItem,
  Avatar,
  Divider,
} from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import WorkIcon from '@mui/icons-material/Work';
import NotificationsIcon from '@mui/icons-material/Notifications';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import PersonIcon from '@mui/icons-material/Person';
import { useAuth } from '../../context/AuthContext';
import { notificationApi } from '../../api/notificationApi';
import { candidateApi } from '../../api/candidateApi';
import { useQuery } from '@tanstack/react-query';

export default function Layout({ children }) {
  const { user, role, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const [anchorEl, setAnchorEl] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [candidateAvatarUrl, setCandidateAvatarUrl] = useState(null);
  const { data: candidateProfile } = useQuery({
    queryKey: ['candidate-profile'],
    queryFn: candidateApi.getProfile,
    enabled: role === 'CANDIDATE',
  });

  useEffect(() => {
    if (role !== 'CANDIDATE' || !candidateProfile?.avatarUrl) {
      setCandidateAvatarUrl(null);
      return;
    }
    let active = true;
    let url;
    candidateApi.getAvatar().then((blob) => {
      url = URL.createObjectURL(blob);
      if (active) setCandidateAvatarUrl(url);
      else URL.revokeObjectURL(url);
    }).catch(() => {});
    return () => { active = false; if (url) URL.revokeObjectURL(url); };
  }, [role, candidateProfile?.avatarUrl]);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      return undefined;
    }

    let active = true;
    const refreshUnreadCount = () => notificationApi.getUnreadCount()
      .then((res) => {
        if (active) setUnreadCount(Number(typeof res === 'number' ? res : res?.count) || 0);
      })
      .catch(() => {
        if (active) setUnreadCount(0);
      });

    refreshUnreadCount();
    const intervalId = window.setInterval(refreshUnreadCount, 30000);
    window.addEventListener('focus', refreshUnreadCount);
    window.addEventListener('notifications-updated', refreshUnreadCount);
    return () => {
      active = false;
      window.clearInterval(intervalId);
      window.removeEventListener('focus', refreshUnreadCount);
      window.removeEventListener('notifications-updated', refreshUnreadCount);
    };
  }, [isAuthenticated]);

  const handleMenuOpen = (e) => setAnchorEl(e.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
    navigate('/login');
  };

  const isCandidate = role === 'CANDIDATE';
  const isEmployer = role === 'HR_ADMIN' || role === 'RECRUITER';
  const candidateName = candidateProfile?.fullName?.trim();
  const emailPrefix = candidateProfile?.email?.split('@')[0];
  const displayName = isCandidate
    ? (candidateName && candidateName.toLowerCase() !== emailPrefix?.toLowerCase()
        ? candidateName : 'Ứng viên')
    : (user?.fullName || user?.email);

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f8fafc', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" color="default" elevation={1} sx={{ bgcolor: '#ffffff', borderBottom: '1px solid #e2e8f0' }}>
        <Toolbar sx={{ flexWrap: 'wrap', gap: 1, px: { xs: 2, md: 4 } }}>
          <Stack direction="row" alignItems="center" spacing={1} component={RouterLink} to={role === 'SYS_ADMIN' ? '/admin/tenants' : '/'} sx={{ textDecoration: 'none', color: 'primary.main', mr: 2 }}>
            <WorkIcon sx={{ fontSize: 28 }} />
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: -0.5 }}>
              RMS Portal
            </Typography>
          </Stack>

          {/* Navigation links based on role */}
          <Stack direction="row" spacing={1} sx={{ flexGrow: 1 }} flexWrap="wrap">
            {role !== 'SYS_ADMIN' && (
              <Button component={RouterLink} to="/" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                Việc làm
              </Button>
            )}

            {isEmployer && (
              <>
                <Button component={RouterLink} to="/employer/jobs" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Quản lý tin tuyển dụng
                </Button>
                <Button component={RouterLink} to="/employer/interviews" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Lịch phỏng vấn
                </Button>
                <Button component={RouterLink} to="/employer/offers" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Lời mời nhận việc (Offers)
                </Button>
                <Button component={RouterLink} to="/company/profile" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Doanh nghiệp
                </Button>
              </>
            )}

            {role === 'HR_ADMIN' && (
              <Button component={RouterLink} to="/admin/jobs/approval" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                Duyệt tin tuyển dụng
              </Button>
            )}

            {role === 'SYS_ADMIN' && (
              <>
                <Button component={RouterLink} to="/admin/tenants" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Quản lý tenant
                </Button>
                <Button component={RouterLink} to="/admin/audit-logs" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Audit log
                </Button>
              </>
            )}

            {role === 'HR_ADMIN' && (
              <Button component={RouterLink} to="/admin/recruiters" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                Quản lý Recruiter
              </Button>
            )}

            {isCandidate && (
              <>
                <Button component={RouterLink} to="/candidate/applications" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Hồ sơ đã nộp
                </Button>
                <Button component={RouterLink} to="/candidate/interviews" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Lịch phỏng vấn
                </Button>
                <Button component={RouterLink} to="/candidate/offers" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Lời mời nhận được
                </Button>
                <Button component={RouterLink} to="/candidate/profile" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  Hồ sơ cá nhân
                </Button>
                <Button component={RouterLink} to="/candidate/cvs" color="inherit" size="small" sx={{ fontWeight: 600 }}>
                  CV của tôi
                </Button>
              </>
            )}
          </Stack>

          {/* Right Action Icons & User Menu */}
          <Stack direction="row" spacing={1.5} alignItems="center">
            {isAuthenticated ? (
              <>
                <IconButton
                  component={RouterLink}
                  to="/notifications"
                  color="inherit"
                  title="Thông báo"
                  aria-label={`Thông báo, ${unreadCount} chưa đọc`}
                >
                  <Badge badgeContent={unreadCount} color="error">
                    <NotificationsIcon />
                  </Badge>
                </IconButton>

                <Button
                  onClick={handleMenuOpen}
                  startIcon={<Avatar src={candidateAvatarUrl || undefined} sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: '0.875rem' }}>{role === 'CANDIDATE' ? <PersonIcon sx={{ color: 'white', fontSize: 20 }} /> : (user?.fullName?.[0] || 'U')}</Avatar>}
                  color="inherit"
                  sx={{ textTransform: 'none', fontWeight: 600 }}
                >
                  {displayName}
                </Button>

                <Menu
                  anchorEl={anchorEl}
                  open={Boolean(anchorEl)}
                  onClose={handleMenuClose}
                  transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                  anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                >
                  <Box sx={{ px: 2, py: 1 }}>
                    <Typography variant="subtitle2" fontWeight={700}>
                      {displayName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {user?.email}
                    </Typography>
                  </Box>
                  <Divider />
                  {isCandidate && (
                    <MenuItem component={RouterLink} to="/candidate/profile" onClick={handleMenuClose}>
                      Hồ sơ cá nhân
                    </MenuItem>
                  )}
                  {isEmployer && (
                    <MenuItem component={RouterLink} to="/company/profile" onClick={handleMenuClose}>
                      Thông tin doanh nghiệp
                    </MenuItem>
                  )}
                  <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                    Đăng xuất
                  </MenuItem>
                </Menu>
              </>
            ) : (
              <>
                <Button component={RouterLink} to="/login" variant="outlined" size="small" sx={{ fontWeight: 600 }}>
                  Đăng nhập
                </Button>
                <Button component={RouterLink} to="/register" variant="contained" size="small" sx={{ fontWeight: 600 }}>
                  Đăng ký
                </Button>
              </>
            )}
          </Stack>
        </Toolbar>
      </AppBar>

      <Box component="main" sx={{ flexGrow: 1, py: 3 }}>
        {children}
      </Box>

      <Box component="footer" sx={{ py: 3, textAlign: 'center', bgcolor: '#ffffff', borderTop: '1px solid #e2e8f0', mt: 'auto' }}>
        <Typography variant="body2" color="text.secondary">
          © 2026 Recruitment Management System (RMS) • Authoritative Compliance v2.0
        </Typography>
      </Box>
    </Box>
  );
}
