import React, { useState } from 'react';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Alert,
  Stack,
  Link,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Checkbox,
} from '@mui/material';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useAuth } from '../../context/AuthContext';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [privacyAccepted, setPrivacyAccepted] = useState(true);
  const [role, setRole] = useState('CANDIDATE'); // CANDIDATE or HR_ADMIN
  const [error, setError] = useState(null);
  const [registeredEmail, setRegisteredEmail] = useState(null);
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!privacyAccepted) {
      setError('Bạn cần đồng ý với Chính sách bảo mật để tiếp tục.');
      return;
    }
    if (role === 'HR_ADMIN' && !companyName.trim()) {
      setError('Vui lòng nhập tên công ty/doanh nghiệp.');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await register({
        email: email.trim(),
        password,
        fullName: fullName.trim(),
        role,
        companyName: role === 'HR_ADMIN' ? companyName.trim() : undefined,
        privacyAccepted: Boolean(privacyAccepted),
      });
      setRegisteredEmail(email.trim());
    } catch (err) {
      setError(err.message || 'Đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ py: 6 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" align="center" fontWeight={700} gutterBottom>
          Tạo tài khoản mới
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Tham gia hệ thống tuyển dụng hiện đại
        </Typography>

        {registeredEmail ? (
          <Box sx={{ textAlign: 'center', py: 2 }}>
            <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64, mb: 2 }} />
            <Alert severity="success" sx={{ mb: 3, textAlign: 'left' }}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                Đăng ký tài khoản thành công!
              </Typography>
              Mã xác thực đã được gửi đến hòm thư <b>{registeredEmail}</b>. Vui lòng kích hoạt tài khoản trước khi đăng nhập.
            </Alert>
            <Stack spacing={2}>
              <Button
                variant="contained"
                size="large"
                component={RouterLink}
                to="/verify-email"
                fullWidth
                sx={{ py: 1.2, fontWeight: 600 }}
              >
                Nhập mã xác thực email ngay
              </Button>
              <Button
                variant="outlined"
                component={RouterLink}
                to="/login"
                fullWidth
              >
                Về trang Đăng nhập
              </Button>
            </Stack>
          </Box>
        ) : (
          <>
            {error && (
              <Alert severity="error" sx={{ mb: 3 }}>
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit}>
              <Stack spacing={2.5}>
                <TextField
                  label="Họ và tên"
                  required
                  fullWidth
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Nguyễn Văn A"
                />
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
                  helperText="Tối thiểu 8 ký tự, bao gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&)"
                />

                <FormControl component="fieldset">
                  <FormLabel component="legend" sx={{ fontWeight: 600, fontSize: '0.9rem' }}>
                    Loại tài khoản
                  </FormLabel>
                  <RadioGroup
                    row
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                  >
                    <FormControlLabel
                      value="CANDIDATE"
                      control={<Radio />}
                      label="Ứng viên"
                    />
                    <FormControlLabel
                      value="HR_ADMIN"
                      control={<Radio />}
                      label="Quản lý HR (HR Admin)"
                    />
                  </RadioGroup>
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                    * Nhà tuyển dụng (Recruiter) tham gia qua thư mời từ Quản lý HR
                  </Typography>
                </FormControl>

                {role === 'HR_ADMIN' && (
                  <TextField
                    label="Tên công ty / Doanh nghiệp"
                    required
                    fullWidth
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    placeholder="Ví dụ: Công ty TNHH Giải Pháp Phần Mềm"
                    helperText="Doanh nghiệp của bạn sẽ được khởi tạo trong hệ thống"
                  />
                )}

                <FormControlLabel
                  control={
                    <Checkbox
                      checked={privacyAccepted}
                      onChange={(e) => setPrivacyAccepted(e.target.checked)}
                      color="primary"
                    />
                  }
                  label={
                    <Typography variant="body2">
                      Tôi đồng ý với <b>Chính sách bảo mật</b> và Điều khoản sử dụng của RMS
                    </Typography>
                  }
                />

                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  fullWidth
                  disabled={loading}
                  sx={{ mt: 1, py: 1.2, fontWeight: 600 }}
                >
                  {loading ? 'Đang tạo tài khoản...' : 'Đăng ký'}
                </Button>
              </Stack>
            </Box>

            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Link component={RouterLink} to="/login" variant="body2">
                Đã có tài khoản? Đăng nhập ngay
              </Link>
            </Box>
          </>
        )}
      </Paper>
    </Container>
  );
}
