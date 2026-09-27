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
  Divider,
  IconButton,
} from '@mui/material';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { notificationApi } from '../../api/notificationApi';

export default function NotificationCenterPage() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await notificationApi.getNotifications();
      setNotifications(Array.isArray(res) ? res : res?.content || []);
    } catch (e) {
      // Fallback or mock notifications
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      fetchNotifications();
    } catch (e) {
      // ignore
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      fetchNotifications();
    } catch (e) {
      // ignore
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
