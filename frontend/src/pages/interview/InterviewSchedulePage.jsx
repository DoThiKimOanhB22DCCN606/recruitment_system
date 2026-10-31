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
import { interviewApi } from '../../api/interviewApi';

export default function InterviewSchedulePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const applicationId = searchParams.get('applicationId') || '';
  const candidateId = searchParams.get('candidateId') || '';

  const [formData, setFormData] = useState({
    applicationId: applicationId,
    candidateId: candidateId,
    interviewType: 'TECHNICAL',
    scheduledAt: '',
    durationMinutes: 60,
    meetingUrl: '',
    interviewerEmails: '',
    location: 'Google Meet / Online',
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
      await interviewApi.schedule({
        applicationId: formData.applicationId,
        candidateId: formData.candidateId,
        interviewType: formData.interviewType,
        scheduledAt: formData.scheduledAt ? new Date(formData.scheduledAt).toISOString() : new Date().toISOString(),
        durationMinutes: parseInt(formData.durationMinutes, 10),
        meetingUrl: formData.meetingUrl,
        interviewerEmails: formData.interviewerEmails ? formData.interviewerEmails.split(',').map(s => s.trim()) : [],
        location: formData.location,
      });
      navigate('/employer/interviews');
    } catch (err) {
      setError(err.message || 'Không thể lên lịch phỏng vấn.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md">
      <Button
        component={RouterLink}
        to="/employer/interviews"
        startIcon={<ArrowBackIcon />}
        sx={{ mb: 2 }}
      >
        Quay lại danh sách phỏng vấn
      </Button>

      <Paper elevation={2} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          Lên lịch phỏng vấn ứng viên
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Thông tin phỏng vấn sẽ được gửi email thông báo tới ứng viên và hội đồng tuyển dụng.
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
                select
                label="Vòng / Hình thức phỏng vấn"
                name="interviewType"
                required
                fullWidth
                value={formData.interviewType}
                onChange={handleChange}
              >
                <MenuItem value="HR">Phỏng vấn sơ vấn HR (HR Screening)</MenuItem>
                <MenuItem value="TECHNICAL">Phỏng vấn chuyên môn (Technical Interview)</MenuItem>
                <MenuItem value="BEHAVIORAL">Phỏng vấn hành vi / Quản lý (Behavioral)</MenuItem>
                <MenuItem value="FINAL">Phỏng vấn Giám đốc / Vòng cuối (Final Round)</MenuItem>
              </TextField>

              <TextField
                label="Thời lượng (phút)"
                name="durationMinutes"
                type="number"
                required
                fullWidth
                value={formData.durationMinutes}
                onChange={handleChange}
              />
            </Stack>

            <TextField
              label="Thời gian phỏng vấn"
              name="scheduledAt"
              type="datetime-local"
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={formData.scheduledAt}
              onChange={handleChange}
            />

            <TextField
              label="Đường dẫn phòng họp online (Google Meet / Zoom)"
              name="meetingUrl"
              placeholder="https://meet.google.com/xyz-abcd-efg"
              fullWidth
              value={formData.meetingUrl}
              onChange={handleChange}
            />

            <TextField
              label="Email người phỏng vấn (phân cách bằng dấu phẩy)"
              name="interviewerEmails"
              placeholder="interviewer1@company.com, techlead@company.com"
              fullWidth
              value={formData.interviewerEmails}
              onChange={handleChange}
            />

            <TextField
              label="Địa điểm / Ghi chú địa điểm"
              name="location"
              fullWidth
              value={formData.location}
              onChange={handleChange}
            />

            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 2, py: 1.2, fontWeight: 700 }}
            >
              {loading ? 'Đang tạo lịch...' : 'Xác nhận tạo lịch phỏng vấn'}
            </Button>
          </Stack>
        </Box>
      </Paper>
    </Container>
  );
}
