import React from 'react';
import { AppBar, Toolbar, Typography, Container, Button, Box, Stack } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import WorkIcon from '@mui/icons-material/Work';

const navLinks = [
  { to: '/', label: 'Tìm việc' },
  { to: '/employer/jobs', label: 'Quản lý tin (NTD)' },
  { to: '/admin/jobs/approval', label: 'Duyệt tin (Admin)' },
  { to: '/candidate/profile', label: 'Hồ sơ ứng viên' },
  { to: '/candidate/cvs', label: 'CV của tôi' },
  { to: '/company/profile', label: 'Hồ sơ doanh nghiệp' },
];

export default function Layout({ children }) {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f5f7fa' }}>
      <AppBar position="static" color="primary" elevation={0}>
        <Toolbar sx={{ flexWrap: 'wrap', gap: 1 }}>
          <WorkIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            Recruitment System
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {navLinks.map((l) => (
              <Button key={l.to} component={RouterLink} to={l.to} color="inherit" size="small">
                {l.label}
              </Button>
            ))}
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        {children}
      </Container>
    </Box>
  );
}
