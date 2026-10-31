import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Container,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import GroupAddOutlinedIcon from '@mui/icons-material/GroupAddOutlined';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

export default function RecruiterInvitePage() {
  const { tenantId } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      if (password !== confirmPassword) {
        setError('Hai mật khẩu không khớp.');
        return;
      }
      if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(password)) {
        setError('Mật khẩu cần ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt (@$!%*#?&).');
        return;
      }
      const created = await authApi.createRecruiter(email.trim(), password);
      setSuccess(`Đã tạo tài khoản ${created.email} với role ${created.role}, trạng thái ${created.status}, thuộc tenant ${created.tenantId}. Tài khoản có thể đăng nhập ngay.`);
      setEmail('');
      setPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(err.message || 'Không thể tạo tài khoản Recruiter.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md">
      <Paper elevation={2} sx={{ p: { xs: 3, sm: 4 }, borderRadius: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <GroupAddOutlinedIcon color="primary" fontSize="large" />
          <Typography variant="h5" fontWeight={700}>Tạo tài khoản Recruiter</Typography>
        </Stack>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          Tạo tài khoản Recruiter hoạt động ngay. Hệ thống tự gán cùng tenant với HR_ADMIN đang đăng nhập; tenant không nhập thủ công.
        </Typography>

        {tenantId && (
          <Alert severity="info" sx={{ mb: 3 }}>
            Tenant hiện tại: <strong>{tenantId}</strong>
          </Alert>
        )}
        {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2}>
            <TextField
              label="Email Recruiter"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              fullWidth
              autoComplete="email"
              placeholder="recruiter@example.com"
            />
            <TextField
              label="Mật khẩu"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              fullWidth
              autoComplete="new-password"
              helperText="Ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt (@$!%*#?&)."
            />
            <TextField
              label="Nhập lại mật khẩu"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              fullWidth
              autoComplete="new-password"
            />
            <TextField label="Vai trò" value="RECRUITER" fullWidth disabled />
            <Button type="submit" variant="contained" disabled={loading || !tenantId} sx={{ alignSelf: 'flex-start' }}>
              {loading ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}
            </Button>
          </Stack>
        </Box>

        <Alert severity="warning" sx={{ mt: 3 }}>
          Tài khoản sẽ hoạt động ngay sau khi tạo. Hãy chuyển thông tin đăng nhập cho Recruiter qua kênh an toàn.
        </Alert>
      </Paper>
    </Container>
  );
}
