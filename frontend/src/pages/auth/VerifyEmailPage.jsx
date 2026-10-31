import React, { useState } from 'react';
import { Container, Paper, Typography, TextField, Button, Box, Alert, Stack, Link, Divider } from '@mui/material';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { authApi } from '../../api/authApi';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') || '');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState(null);

  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState(null);
  const [resendError, setResendError] = useState(null);

  const handleVerify = async () => {
    if (!email.trim() || !/^\d{6}$/.test(otp)) {
      setError('Vui lòng nhập email đăng ký và OTP gồm đúng 6 chữ số.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await authApi.verifyEmail(email.trim(), otp);
      setSuccess('Xác thực email thành công! Nếu đăng ký doanh nghiệp, tài khoản sẽ đăng nhập được sau khi SYS_ADMIN duyệt. Tài khoản ứng viên có thể đăng nhập ngay.');
    } catch (err) {
      setError(err.message || 'Mã xác thực không hợp lệ hoặc đã hết hạn.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleVerify();
  };

  const handleResend = async (e) => {
    e.preventDefault();
    if (!email.trim()) {
      setResendError('Vui lòng nhập email.');
      return;
    }
    setResendLoading(true);
    setResendError(null);
    setResendMessage(null);
    try {
      await authApi.resendVerification(email.trim());
      setResendMessage('Nếu tài khoản đang chờ xác minh, OTP mới đã được gửi đến email này. OTP cũ không còn hiệu lực.');
    } catch (err) {
      setResendError(err.message || 'Không thể gửi lại mã xác thực.');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ py: 6 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" align="center" fontWeight={700} gutterBottom>
          Xác thực tài khoản Email
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Nhập OTP 6 chữ số đã gửi đến email đăng ký. Mã có hiệu lực trong 10 phút.
        </Typography>

        {success ? (
          <Box sx={{ textAlign: 'center', py: 2 }}>
            <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64, mb: 2 }} />
            <Alert severity="success" sx={{ mb: 3 }}>
              {success}
            </Alert>
            <Button
              variant="contained"
              size="large"
              component={RouterLink}
              to="/login"
              fullWidth
              sx={{ py: 1.2, fontWeight: 600 }}
            >
              Đăng nhập ngay
            </Button>
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
                  label="Email đăng ký"
                  type="email"
                  required
                  fullWidth
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="example@email.com"
                />
                <TextField
                  label="Mã OTP"
                  required
                  fullWidth
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  placeholder="Nhập OTP 6 chữ số"
                  inputProps={{ inputMode: 'numeric', maxLength: 6, pattern: '[0-9]{6}' }}
                  helperText="Không chia sẻ OTP với người khác. Mã có hiệu lực 10 phút."
                />
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  fullWidth
                  disabled={loading}
                  sx={{ py: 1.2, fontWeight: 600 }}
                >
                  {loading ? 'Đang xác thực...' : 'Kích hoạt tài khoản'}
                </Button>
              </Stack>
            </Box>

            <Divider sx={{ my: 4 }}>hoặc gửi lại mã</Divider>

            {resendMessage && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {resendMessage}
              </Alert>
            )}
            {resendError && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {resendError}
              </Alert>
            )}

            <Box component="form" onSubmit={handleResend}>
              <Stack spacing={2}>
                <TextField
                  label="Email đăng ký"
                  type="email"
                  size="small"
                  fullWidth
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="example@email.com"
                />
                <Button
                  type="submit"
                  variant="outlined"
                  size="medium"
                  fullWidth
                  disabled={resendLoading}
                >
                  {resendLoading ? 'Đang gửi...' : 'Gửi lại mã xác thực'}
                </Button>
              </Stack>
            </Box>
          </>
        )}

        <Box sx={{ mt: 3, textAlign: 'center' }}>
          <Link component={RouterLink} to="/login" variant="body2">
            Quay lại Đăng nhập
          </Link>
        </Box>
      </Paper>
    </Container>
  );
}
