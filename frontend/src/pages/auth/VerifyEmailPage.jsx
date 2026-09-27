import React, { useState, useEffect } from 'react';
import { Container, Paper, Typography, TextField, Button, Box, Alert, Stack, Link, Divider } from '@mui/material';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { authApi } from '../../api/authApi';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const tokenFromUrl = searchParams.get('token') || '';

  const [token, setToken] = useState(tokenFromUrl);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState(null);

  const [resendEmail, setResendEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState(null);
  const [resendError, setResendError] = useState(null);

  const handleVerify = async (tokenToVerify) => {
    const val = tokenToVerify || token;
    if (!val) {
      setError('Vui lòng nhập mã xác thực.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await authApi.verifyEmail(val.trim());
      setSuccess('Xác thực email thành công! Bạn có thể đăng nhập ngay.');
    } catch (err) {
      setError(err.message || 'Mã xác thực không hợp lệ hoặc đã hết hạn.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tokenFromUrl) {
      handleVerify(tokenFromUrl);
    }
  }, [tokenFromUrl]);

  const handleSubmit = (e) => {
    e.preventDefault();
    handleVerify(token);
  };

  const handleResend = async (e) => {
    e.preventDefault();
    if (!resendEmail) {
      setResendError('Vui lòng nhập email.');
      return;
    }
    setResendLoading(true);
    setResendError(null);
    setResendMessage(null);
    try {
      await authApi.resendVerification(resendEmail.trim());
      setResendMessage('Nếu email tồn tại trong hệ thống, mã xác thực mới đã được gửi đến bạn.');
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
          Nhập mã xác thực đã được gửi đến email của bạn để kích hoạt tài khoản.
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
                  label="Mã xác thực (Token)"
                  required
                  fullWidth
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="Nhập mã xác thực từ email..."
                  helperText="Mã xác thực có hiệu lực trong vòng 24 giờ"
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
                  label="Email nhận lại mã"
                  type="email"
                  size="small"
                  fullWidth
                  value={resendEmail}
                  onChange={(e) => setResendEmail(e.target.value)}
                  placeholder="example@rms.local"
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
