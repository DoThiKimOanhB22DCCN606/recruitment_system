import React from 'react';
import { Alert, Snackbar } from '@mui/material';

export default function AppSnackbar({ open, severity = 'success', message, onClose }) {
  return (
    <Snackbar
      open={open}
      autoHideDuration={4500}
      onClose={onClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
    >
      <Alert
        severity={severity}
        variant="filled"
        onClose={onClose}
        sx={{ width: '100%' }}
      >
        {message}
      </Alert>
    </Snackbar>
  );
}
