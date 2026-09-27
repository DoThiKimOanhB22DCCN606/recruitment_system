import React, { useState } from 'react';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Stack,
  Alert,
  MenuItem,
} from '@mui/material';
import { useNavigate, useSearchParams, Link as RouterLink } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { offerApi } from '../../api/offerApi';

export default function OfferCreatePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const applicationId = searchParams.get('applicationId') || '';
  const candidateId = searchParams.get('candidateId') || '';

  const [formData, setFormData] = useState({
    applicationId: applicationId,
    candidateId: candidateId,
    proposedSalary: '',
    currency: 'VND',
    startDate: '',
    expiryDate: '',
    notes: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await offerApi.createDraft({
        applicationId: formData.applicationId,
        candidateId: formData.candidateId,
        proposedSalary: parseFloat(formData.proposedSalary),
        currency: formData.currency,
        startDate: formData.startDate,
        expiryDate: formData.expiryDate || null,
        notes: formData.notes,
      });
      navigate('/employer/offers');
    } catch (err) {
      setError(err.message || 'Không thể tạo bản nháp Offer.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md">
      <Button
        component={RouterLink}
        to="/employer/offers"
        startIcon={<ArrowBackIcon />}
        sx={{ mb: 2 }}
      >
        Quay lại danh sách Offer
      </Button>

      <Paper elevation={2} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          Tạo bản nháp Thư mời nhận việc (Draft Offer)
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Ứng viên phải đang ở bước Offered trước khi tạo Offer.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Mã hồ sơ ứng tuyển (Application ID)"
                name="applicationId"
                required
                fullWidth
                value={formData.applicationId}
                onChange={handleChange}
              />
              <TextField
                label="Mã ứng viên (Candidate ID)"
                name="candidateId"
                required
                fullWidth
                value={formData.candidateId}
                onChange={handleChange}
              />
            </Stack>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Mức lương đề xuất (Gross/Net)"
                name="proposedSalary"
                type="number"
                required
                fullWidth
                value={formData.proposedSalary}
                onChange={handleChange}
                placeholder="25000000"
              />
              <TextField
                select
                label="Loại tiền tệ"
                name="currency"
                required
                sx={{ minWidth: 120 }}
                value={formData.currency}
                onChange={handleChange}
              >
                <MenuItem value="VND">VND</MenuItem>
                <MenuItem value="USD">USD</MenuItem>
                <MenuItem value="EUR">EUR</MenuItem>
              </TextField>
            </Stack>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Ngày bắt đầu làm việc (Start Date)"
                name="startDate"
                type="date"
                required
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={formData.startDate}
                onChange={handleChange}
              />

              <TextField
                label="Hạn phản hồi lời mời (Expiry Date)"
                name="expiryDate"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                helperText="Mặc định 7 ngày kể từ lúc tạo nếu để trống. Phải trước ngày bắt đầu làm việc."
                value={formData.expiryDate}
                onChange={handleChange}
              />
            </Stack>

            <TextField
              label="Ghi chú / Quyền lợi đính kèm"
              name="notes"
              multiline
              rows={4}
              fullWidth
              value={formData.notes}
              onChange={handleChange}
              placeholder="Ví dụ: Thưởng hiệu suất hàng quý, bảo hiểm sức khỏe cao cấp..."
            />

            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 2, py: 1.2, fontWeight: 700 }}
            >
              {loading ? 'Đang tạo...' : 'Tạo bản nháp Offer'}
            </Button>
          </Stack>
        </Box>
      </Paper>
    </Container>
  );
}
