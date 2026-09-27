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
import AddIcon from '@mui/icons-material/Add';
import SendIcon from '@mui/icons-material/Send';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { offerApi } from '../../api/offerApi';
import { useAuth } from '../../context/AuthContext';

export default function OfferListPage() {
  const { hasRole } = useAuth();
  const isEmployer = hasRole(['HR_ADMIN', 'RECRUITER']);

  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOffers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await offerApi.getOffers();
      setOffers(Array.isArray(data) ? data : data?.content || []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách Offer.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOffers();
  }, []);

  const handleSendOffer = async (offerId) => {
    if (!window.confirm('Bạn có chắc chắn muốn gửi thư mời này tới ứng viên?')) return;
    try {
      await offerApi.sendOffer(offerId);
      fetchOffers();
    } catch (err) {
      alert(err.message || 'Không thể gửi Offer.');
    }
  };

  const handleAcceptOffer = async (offerId) => {
    if (!window.confirm('Bạn có đồng ý nhận lời mời làm việc này?')) return;
    try {
      await offerApi.acceptOffer(offerId);
      alert('Chúc mừng bạn! Bạn đã đồng ý nhận việc thành công.');
      fetchOffers();
    } catch (err) {
      alert(err.message || 'Không thể chấp nhận Offer.');
    }
  };

  const handleDeclineOffer = async (offerId) => {
    if (!window.confirm('Bạn có chắc chắn muốn từ chối lời mời làm việc này?')) return;
    try {
      await offerApi.declineOffer(offerId);
      fetchOffers();
    } catch (err) {
      alert(err.message || 'Không thể từ chối Offer.');
    }
  };

  const handleWithdrawOffer = async (offerId) => {
    if (!window.confirm('Bạn có chắc chắn muốn thu hồi Offer này?')) return;
    try {
      await offerApi.withdrawOffer(offerId);
      fetchOffers();
    } catch (err) {
      alert(err.message || 'Không thể thu hồi Offer.');
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
      case 'WITHDRAWN':
        return <Chip label="Đã thu hồi" size="small" />;
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
                <TableCell sx={{ fontWeight: 700 }}>Mã Offer / Ứng viên</TableCell>
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
                      {item.candidateName || `Ứng viên #${item.candidateId}`}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Hồ sơ #{item.applicationId}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700} color="primary.main">
                      {item.proposedSalary ? Number(item.proposedSalary).toLocaleString('vi-VN') : '0'} {item.currency || 'VND'}
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
                          onClick={() => handleSendOffer(item.id)}
                        >
                          Gửi ứng viên
                        </Button>
                      )}
                      {isEmployer && (item.status === 'DRAFT' || item.status === 'SENT') && (
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          onClick={() => handleWithdrawOffer(item.id)}
                        >
                          Thu hồi
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
    </Container>
  );
}
