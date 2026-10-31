import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Container,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/authApi';

export default function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState(searchParams.get('token') || '');
  const [invite, setInvite] = useState(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(Boolean(searchParams.get('token')));
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const validateToken = async (value) => {
    const cleanToken = value.trim();
    if (!cleanToken) {
      setError('Vui lòng nhập mã lời mời.');
      return;
    }
    setChecking(true);
    setError('');
    try {
      const details = await authApi.getInvitation(cleanToken);
      setToken(cleanToken);
      setInvite(details);
    } catch (err) {
      setInvite(null);
      setError(err.message || 'Lời mời không hợp lệ hoặc đã hết hạn.');
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => {
    const urlToken = searchParams.get('token');
    if (urlToken) validateToken(urlToken);
  }, [searchParams]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (password !== confirmPassword) {
      setError('Hai mật khẩu không khớp.');
      return;
    }
    setLoading(true);
    try {
      await authApi.acceptInvitation(token, password);
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Không thể chấp nhận lời mời.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ py: 5 }}>
      <Paper elevation={3} sx={{ p: { xs: 3, sm: 4 }, borderRadius: 2 }}>
        {success ? (
          <Box sx={{ textAlign: 'center' }}>
            <CheckCircleOutlineIcon color="success" sx={{ fontSize: 60, mb: 1 }} />
            <Typography variant="h5" fontWeight={700} gutterBottom>Đã kích hoạt tài khoản</Typography>
            <Alert severity="success" sx={{ mb: 3 }}>Bạn có thể đăng nhập với email đã được mời và mật khẩu vừa tạo.</Alert>
            <Button component={RouterLink} to="/login" variant="contained" fullWidth>Đăng nhập</Button>
          </Box>
        ) : (
          <>
            <Typography variant="h5" fontWeight={700} gutterBottom>Chấp nhận lời mời Recruiter</Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
              Xác thực mã lời mời, sau đó tạo mật khẩu cho tài khoản của bạn.
            </Typography>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            {!invite ? (
              <Box component="form" onSubmit={(event) => { event.preventDefault(); validateToken(token); }}>
                <Stack spacing={2}>
                  <TextField
                    label="Mã lời mời"
                    value={token}
                    onChange={(event) => setToken(event.target.value)}
                    required
                    fullWidth
                    multiline
                  />
                  <Button type="submit" variant="contained" disabled={checking}>
                    {checking ? 'Đang xác thực...' : 'Xác thực lời mời'}
                  </Button>
                </Stack>
              </Box>
            ) : (
              <>
                <Alert severity="info" sx={{ mb: 3 }}>
                  Email: <strong>{invite.email}</strong><br />
                  Vai trò: <strong>{invite.role}</strong><br />
                  Tenant: <strong>{invite.tenantId}</strong>
                </Alert>
                <Box component="form" onSubmit={handleSubmit}>
                  <Stack spacing={2}>
                    <TextField label="Mật khẩu mới" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required fullWidth autoComplete="new-password" helperText="Ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt." />
                    <TextField label="Nhập lại mật khẩu" type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required fullWidth autoComplete="new-password" />
                    <Button type="submit" variant="contained" disabled={loading}>
                      {loading ? 'Đang kích hoạt...' : 'Tạo mật khẩu và kích hoạt'}
                    </Button>
                  </Stack>
                </Box>
              </>
            )}

            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Link component={RouterLink} to="/login" variant="body2">Quay lại đăng nhập</Link>
            </Box>
          </>
        )}
      </Paper>
    </Container>
  );
}
