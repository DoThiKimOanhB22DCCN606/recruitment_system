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
import { jobApi } from '../../api/jobApi';
import { companyApi } from '../../api/companyApi';
import AppSnackbar from '../../components/common/AppSnackbar';

export default function CandidateApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState({ open: false, severity: 'success', message: '' });
  const closeToast = () => setToast((current) => ({ ...current, open: false }));
  const showToast = (message, severity = 'success') => setToast({ open: true, severity, message });

  const fetchApplications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await atsApi.getCandidateApplications();
      const items = Array.isArray(data) ? data : data?.content || [];
      const jobIds = [...new Set(items.map((application) => application.jobId).filter(Boolean))];
      const jobEntries = await Promise.all(jobIds.map(async (jobId) => {
        try {
          return [jobId, await jobApi.getJobById(jobId)];
        } catch {
          return [jobId, null];
        }
      }));
      const jobsById = new Map(jobEntries);
      const tenantIds = [...new Set(items.map((application) => application.tenantId).filter(Boolean))];
      const brandEntries = await Promise.all(tenantIds.map(async (tenantId) => {
        try {
          return [tenantId, await companyApi.getBrandByTenant(tenantId)];
        } catch {
          return [tenantId, null];
        }
      }));
      const brandsByTenantId = new Map(brandEntries);
      setApplications(items.map((application) => {
        const job = jobsById.get(application.jobId);
        const brand = brandsByTenantId.get(application.tenantId);
        return {
          ...application,
          jobTitle: job?.title || application.jobTitle,
          companyName: brand?.name || job?.companyName || application.companyName,
        };
      }));
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
      showToast('Bạn đã rút hồ sơ ứng tuyển.');
      fetchApplications();
    } catch (err) {
      showToast(err.message || 'Không thể rút hồ sơ ứng tuyển.', 'error');
    }
  };

  const getStatusChip = (application) => {
    if (application.status === 'HIRED') return <Chip label="Đã trúng tuyển" color="success" size="small" />;
    if (application.status === 'REJECTED') return <Chip label="Đã từ chối" color="error" size="small" />;
    if (application.status === 'WITHDRAWN') return <Chip label="Đã rút hồ sơ" size="small" />;

    const stage = (application.currentStageName || application.stageName || '').trim().toLowerCase();
    if (['new', 'sourced', 'mới ứng tuyển', 'đã nộp'].includes(stage)) {
      return <Chip label="Đã nộp" color="primary" size="small" />;
    }
    if (['screening', 'sàng lọc'].includes(stage)) {
      return <Chip label="Đang xem xét hồ sơ" color="info" size="small" />;
    }
    if (['interviewing', 'interview', 'phỏng vấn'].includes(stage)) {
      return <Chip label="Đang phỏng vấn" color="warning" size="small" />;
    }
    if (['offered', 'offer', 'mời nhận việc'].includes(stage)) {
      return <Chip label="Đang chờ phản hồi lời mời" color="secondary" size="small" />;
    }
    return <Chip label="Đang xem xét hồ sơ" color="info" size="small" />;
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
                <TableCell sx={{ fontWeight: 700 }}>Vị trí ứng tuyển / Công ty</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Ngày nộp</TableCell>
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
                    {app.companyName || 'Chưa có thông tin công ty'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {app.appliedAt
                      ? new Date(app.appliedAt).toLocaleDateString('vi-VN')
                      : 'Mới đây'}
                  </TableCell>
                  <TableCell>{getStatusChip(app)}</TableCell>
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
      <AppSnackbar
        open={toast.open}
        severity={toast.severity}
        message={toast.message}
        onClose={closeToast}
      />
    </Container>
  );
}
