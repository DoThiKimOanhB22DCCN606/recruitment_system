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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import SendIcon from '@mui/icons-material/Send';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { offerApi } from '../../api/offerApi';
import { atsApi } from '../../api/atsApi';
import { useAuth } from '../../context/AuthContext';
import { formatMoney } from '../../utils/money';
import AppSnackbar from '../../components/common/AppSnackbar';

export default function OfferListPage() {
  const { hasRole } = useAuth();
  const isEmployer = hasRole(['HR_ADMIN', 'RECRUITER']);

  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState({ open: false, severity: 'success', message: '' });
  const [sendDialogOffer, setSendDialogOffer] = useState(null);
  const [sendingOffer, setSendingOffer] = useState(false);
  const closeToast = () => setToast((current) => ({ ...current, open: false }));
  const showToast = (message, severity = 'success') => setToast({ open: true, severity, message });

  const fetchOffers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await offerApi.getOffers();
      const items = Array.isArray(data) ? data : data?.content || [];
      if (isEmployer) {
        const enriched = await Promise.all(items.map(async (item) => {
          try {
            const response = await atsApi.getApplication(item.applicationId);
            const application = response?.data || response?.application || response;
            return {
              ...item,
              candidateName: application?.candidateName
                || application?.candidateFullName
                || application?.candidate?.fullName
                || application?.fullName
                || item.candidateName,
              candidateEmail: application?.candidateEmail
                || application?.candidate?.email
                || item.candidateEmail,
            };
          } catch {
            return item;
          }
        }));
        setOffers(enriched);
      } else {
        setOffers(items);
      }
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách Offer.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOffers();
  }, []);

  const handleSendOffer = async () => {
    if (!sendDialogOffer) return;
    setSendingOffer(true);
    try {
      await offerApi.sendOffer(sendDialogOffer.id);
      setSendDialogOffer(null);
      showToast(`Đã gửi thư mời đến ${sendDialogOffer.candidateEmail || 'ứng viên'}.`);
      await fetchOffers();
    } catch (err) {
      showToast(err.message || 'Không thể gửi lời mời nhận việc.', 'error');
    } finally {
      setSendingOffer(false);
    }
  };

  const handleAcceptOffer = async (offerId) => {
    if (!window.confirm('Bạn có đồng ý nhận lời mời làm việc này?')) return;
    try {
      await offerApi.acceptOffer(offerId);
      showToast('Bạn đã đồng ý nhận lời mời làm việc thành công.');
      fetchOffers();
    } catch (err) {
      showToast(err.message || 'Không thể chấp nhận lời mời làm việc.', 'error');
    }
  };

  const handleDeclineOffer = async (offerId) => {
    if (!window.confirm('Bạn có chắc chắn muốn từ chối lời mời làm việc này?')) return;
    try {
      await offerApi.declineOffer(offerId);
      fetchOffers();
    } catch (err) {
      showToast(err.message || 'Không thể từ chối lời mời làm việc.', 'error');
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'DRAFT':
        return <Chip label="Bản nháp" size="small" />;
      case 'SENT':
        return <Chip label="Đã gửi" color="primary" size="small" />;
      case 'ACCEPTED':
        return <Chip label="Đã chấp nhận" color="success" size="small" />;
      case 'DECLINED':
        return <Chip label="Đã từ chối" color="error" size="small" />;
      case 'EXPIRED':
        return <Chip label="Đã hết hạn" color="warning" size="small" />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  return (
    <Container maxWidth="lg">
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <div>
          <Typography variant="h5" fontWeight={700}>
            Quản lý Lời mời nhận việc (Offers)
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {isEmployer ? 'Theo dõi, phê duyệt và gửi Offer cho ứng viên' : 'Danh sách thư mời nhận việc của bạn'}
          </Typography>
        </div>
        {isEmployer && (
          <Button
            component={RouterLink}
            to="/employer/offers/new"
            variant="contained"
            startIcon={<AddIcon />}
          >
            Tạo Offer mới
          </Button>
        )}
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : offers.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center', borderRadius: 2 }}>
          <Typography color="text.secondary">Chưa có lời mời nhận việc nào.</Typography>
        </Paper>
      ) : (
        <Paper elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: '#f8fafc' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Ứng viên</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Mức lương đề xuất</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Ngày bắt đầu</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Hạn phản hồi</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Trạng thái</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="right">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {offers.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Typography variant="subtitle2" fontWeight={600}>
                      {isEmployer ? (item.candidateName || 'Chưa có tên ứng viên') : 'Lời mời của bạn'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {isEmployer ? (item.candidateEmail || 'Hồ sơ ứng tuyển') : 'Hồ sơ ứng tuyển của bạn'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700} color="primary.main">
                      {formatMoney(item.proposedSalary ?? 0)} {item.currency || 'VND'}
                    </Typography>
                  </TableCell>
                  <TableCell>{item.startDate || 'Thoả thuận'}</TableCell>
                  <TableCell>{item.expiryDate || '7 ngày'}</TableCell>
                  <TableCell>{getStatusChip(item.status)}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      {isEmployer && item.status === 'DRAFT' && (
                        <Button
                          size="small"
                          variant="contained"
                          color="primary"
                          startIcon={<SendIcon />}
                          onClick={() => setSendDialogOffer(item)}
                        >
                          Gửi ứng viên
                        </Button>
                      )}
                      {!isEmployer && item.status === 'SENT' && (
                        <>
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            startIcon={<CheckCircleIcon />}
                            onClick={() => handleAcceptOffer(item.id)}
                          >
                            Đồng ý
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            startIcon={<CancelIcon />}
                            onClick={() => handleDeclineOffer(item.id)}
                          >
                            Từ chối
                          </Button>
                        </>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
      <Dialog
        open={Boolean(sendDialogOffer)}
        onClose={() => !sendingOffer && setSendDialogOffer(null)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Xác nhận gửi thư mời nhận việc</DialogTitle>
        <DialogContent>
          <Typography>
            Hệ thống sẽ gửi email offer đến ứng viên
            {sendDialogOffer?.candidateName ? ` ${sendDialogOffer.candidateName}` : ''}
            {sendDialogOffer?.candidateEmail ? ` (${sendDialogOffer.candidateEmail})` : ''}.
            Sau khi gửi thành công, trạng thái offer sẽ chuyển thành “Đã gửi”.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setSendDialogOffer(null)} disabled={sendingOffer}>Hủy</Button>
          <Button onClick={handleSendOffer} variant="contained" startIcon={<SendIcon />} disabled={sendingOffer}>
            {sendingOffer ? 'Đang gửi email…' : 'Gửi email'}
          </Button>
        </DialogActions>
      </Dialog>
      <AppSnackbar
        open={toast.open}
        severity={toast.severity}
        message={toast.message}
        onClose={closeToast}
      />
    </Container>
  );
}
