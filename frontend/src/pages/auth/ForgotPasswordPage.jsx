import React, { useState } from 'react';
import { Container, Paper, Typography, TextField, Button, Box, Alert, Stack, Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { authApi } from '../../api/authApi';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);
    try {
      await authApi.forgotPassword({ email });
      setMessage('Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hòm thư của bạn.');
    } catch (err) {
      setError(err.message || 'Không thể xử lý yêu cầu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ py: 6 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" align="center" fontWeight={700} gutterBottom>
          Quên mật khẩu
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Nhập địa chỉ email đăng ký để nhận liên kết khôi phục mật khẩu.
        </Typography>

        {message && <Alert severity="success" sx={{ mb: 3 }}>{message}</Alert>}
        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <TextField
              label="Email đã đăng ký"
              type="email"
              required
              fullWidth
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              fullWidth
              disabled={loading}
              sx={{ py: 1.2, fontWeight: 600 }}
            >
              {loading ? 'Đang gửi yêu cầu...' : 'Gửi liên kết đặt lại mật khẩu'}
            </Button>
          </Stack>
        </Box>

        <Box sx={{ mt: 3, textAlign: 'center' }}>
          <Link component={RouterLink} to="/login" variant="body2">
            Quay lại Đăng nhập
          </Link>
        </Box>
      </Paper>
    </Container>
  );
}
