import React, { useState } from 'react';
import { Container, Paper, Typography, TextField, Button, Box, Alert, Stack, Link } from '@mui/material';
import { useNavigate, Link as RouterLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const u = await login(email, password);
      if (u.role === 'CANDIDATE') {
        navigate(from === '/' ? '/candidate/profile' : from, { replace: true });
      } else if (u.role === 'HR_ADMIN' || u.role === 'RECRUITER' || u.role === 'SYS_ADMIN') {
        navigate(from === '/' ? '/employer/jobs' : from, { replace: true });
      } else {
        navigate(from, { replace: true });
      }
    } catch (err) {
      setError(err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ py: 6 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" align="center" fontWeight={700} gutterBottom>
          Đăng nhập RMS
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Hệ thống Quản lý Tuyển dụng & Đánh giá ứng viên
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <TextField
              label="Email"
              type="email"
              required
              fullWidth
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@rms.local"
            />
            <TextField
              label="Mật khẩu"
              type="password"
              required
              fullWidth
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              fullWidth
              disabled={loading}
              sx={{ mt: 1, py: 1.2, fontWeight: 600 }}
            >
              {loading ? 'Đang xác thực...' : 'Đăng nhập'}
            </Button>
          </Stack>
        </Box>

        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3 }}>
          <Link component={RouterLink} to="/forgot-password" variant="body2">
            Quên mật khẩu?
          </Link>
          <Link component={RouterLink} to="/register" variant="body2">
            Chưa có tài khoản? Đăng ký ngay
          </Link>
        </Stack>
      </Paper>
    </Container>
  );
}
