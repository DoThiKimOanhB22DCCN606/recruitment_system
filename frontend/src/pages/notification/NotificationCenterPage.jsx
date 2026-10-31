import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Button,
  Stack,
  Box,
  CircularProgress,
  Alert,
  Divider,
  IconButton,
} from '@mui/material';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { notificationApi } from '../../api/notificationApi';
import { Link as RouterLink } from 'react-router-dom';

export default function NotificationCenterPage() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchNotifications = async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const res = await notificationApi.getNotifications();
      setNotifications(Array.isArray(res) ? res : res?.content || []);
      setError('');
    } catch (e) {
      setError(e.message || 'Không thể tải thông báo. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications(true);
    const intervalId = window.setInterval(() => fetchNotifications(), 30000);
    return () => window.clearInterval(intervalId);
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      window.dispatchEvent(new Event('notifications-updated'));
      fetchNotifications();
    } catch (e) {
      setError(e.message || 'Không thể đánh dấu đã đọc tất cả.');
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      window.dispatchEvent(new Event('notifications-updated'));
      fetchNotifications();
    } catch (e) {
      setError(e.message || 'Không thể đánh dấu thông báo đã đọc.');
    }
  };

  return (
    <Container maxWidth="md">
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>
          Trung tâm thông báo
        </Typography>
        {notifications.length > 0 && (
          <Button
            variant="outlined"
            startIcon={<DoneAllIcon />}
            onClick={handleMarkAllRead}
          >
            Đánh dấu đã đọc tất cả
          </Button>
        )}
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center', borderRadius: 2 }}>
          <NotificationsActiveIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
          <Typography color="text.secondary">Bạn không có thông báo mới nào.</Typography>
        </Paper>
      ) : (
        <Paper elevation={2} sx={{ borderRadius: 2 }}>
          <List disablePadding>
            {notifications.map((item, idx) => (
              <React.Fragment key={item.id || idx}>
                <ListItem
                  sx={{
                    bgcolor: item.read ? 'inherit' : '#f0f9ff',
                    px: 3,
                    py: 2,
                  }}
                  secondaryAction={
                    !item.read && (
                      <IconButton
                        edge="end"
                        title="Đánh dấu đã đọc"
                        onClick={() => handleMarkRead(item.id)}
                      >
                        <CheckCircleIcon color="primary" />
                      </IconButton>
                    )
                  }
                >
                  <ListItemIcon>
                    <NotificationsActiveIcon color={item.read ? 'disabled' : 'primary'} />
                  </ListItemIcon>
                  <ListItemText
                    secondaryTypographyProps={{ component: 'div' }}
                    primary={
                      <Typography variant="subtitle2" fontWeight={item.read ? 400 : 700}>
                        {item.title}
                      </Typography>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" color="text.secondary" sx={{ my: 0.5 }}>
                          {item.message}
                        </Typography>
                        {item.entityType === 'COMPANY_REGISTRATION' && (
                          <Button component={RouterLink} to="/admin/tenants" size="small" sx={{ mt: 0.5 }}>
                            Xem đăng ký doanh nghiệp
                          </Button>
                        )}
                        <Typography variant="caption" color="text.secondary">
                          {item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : 'Vừa xong'}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
                {idx < notifications.length - 1 && <Divider />}
              </React.Fragment>
            ))}
          </List>
        </Paper>
      )}
    </Container>
  );
}
