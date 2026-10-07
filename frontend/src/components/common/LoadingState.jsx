import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

export function LoadingState({ label = 'Đang tải...' }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 6, justifyContent: 'center' }}>
      <CircularProgress size={22} />
      <Typography color="text.secondary">{label}</Typography>
    </Box>
  );
}

export function ErrorState({ message }) {
  return (
    <Box sx={{ py: 6, textAlign: 'center' }}>
      <Typography color="error">{message || 'Đã xảy ra lỗi. Vui lòng thử lại.'}</Typography>
    </Box>
  );
}
