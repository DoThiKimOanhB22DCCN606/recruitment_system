import React from 'react';
import { Box, Paper, Typography, Button, Stack, Alert } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import HomeIcon from '@mui/icons-material/Home';

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '80vh',
            p: 3,
          }}
        >
          <Paper
            elevation={3}
            sx={{
              p: 4,
              maxWidth: 550,
              width: '100%',
              textAlign: 'center',
              borderRadius: 2,
            }}
          >
            <Typography variant="h5" fontWeight={700} color="error" gutterBottom>
              Đã xảy ra lỗi không mong muốn
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Hệ thống gặp sự cố khi hiển thị trang này. Vui lòng tải lại trang hoặc quay về trang chủ.
            </Typography>

            {this.state.error?.message && (
              <Alert severity="warning" sx={{ mb: 3, textAlign: 'left', wordBreak: 'break-word' }}>
                Chi tiết: {this.state.error.message}
              </Alert>
            )}

            <Stack direction="row" spacing={2} justifyContent="center">
              <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={this.handleReload}
              >
                Tải lại trang
              </Button>
              <Button
                variant="outlined"
                startIcon={<HomeIcon />}
                onClick={this.handleGoHome}
              >
                Về trang chủ
              </Button>
            </Stack>
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
