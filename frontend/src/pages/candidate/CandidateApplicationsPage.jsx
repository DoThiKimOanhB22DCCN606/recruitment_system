import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Paper,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  Chip,
  Stack,
  Box,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import CancelIcon from '@mui/icons-material/Cancel';
import { atsApi } from '../../api/atsApi';

export default function CandidateApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchApplications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await atsApi.getCandidateApplications();
      setApplications(Array.isArray(data) ? data : data?.content || []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách hồ sơ ứng tuyển.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const handleWithdraw = async (applicationId) => {
    const reason = prompt('Nhập lý do rút hồ sơ ứng tuyển:');
    if (reason === null) return;
    try {
      await atsApi.withdraw(applicationId, reason || 'Rút hồ sơ tự nguyện');
      fetchApplications();
    } catch (err) {
      alert(err.message || 'Không thể rút hồ sơ ứng tuyển.');
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <Chip label="Đang xử lý" color="primary" size="small" />;
      case 'HIRED':
        return <Chip label="Đã trúng tuyển" color="success" size="small" />;
      case 'REJECTED':
        return <Chip label="Từ chối" color="error" size="small" />;
      case 'WITHDRAWN':
        return <Chip label="Đã rút hồ sơ" size="small" />;
      default:
        return <Chip label={status || 'ACTIVE'} size="small" />;
    }
  };

  return (
    <Container maxWidth="lg">
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Hồ sơ ứng tuyển của tôi
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Theo dõi tiến độ xét duyệt và trạng thái phỏng vấn trực tiếp từ các nhà tuyển dụng
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : applications.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center', borderRadius: 2 }}>
          <Typography color="text.secondary" gutterBottom>
            Bạn chưa nộp hồ sơ ứng tuyển vào vị trí nào.
          </Typography>
          <Button component={RouterLink} to="/" variant="contained" sx={{ mt: 2 }}>
            Khám phá việc làm ngay
          </Button>
        </Paper>
      ) : (
        <Paper elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: '#f8fafc' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Vị trí tuyển dụng</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Ngày nộp</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Bước quy trình (Stage)</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Trạng thái</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="right">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {applications.map((app) => (
                <TableRow key={app.id} hover>
                  <TableCell>
                    <Typography variant="subtitle2" fontWeight={600}>
                      {app.jobTitle || `Công việc #${app.jobId}`}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {app.companyName || 'Công ty tuyển dụng'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {app.appliedAt
                      ? new Date(app.appliedAt).toLocaleDateString('vi-VN')
                      : 'Mới đây'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={app.stageName || 'Sourced'}
                      variant="outlined"
                      color="secondary"
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{getStatusChip(app.status)}</TableCell>
                  <TableCell align="right">
                    {app.status === 'ACTIVE' && (
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<CancelIcon />}
                        onClick={() => handleWithdraw(app.id)}
                      >
                        Rút hồ sơ
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
    </Container>
  );
}
