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

  const [formData, setFormData] = useState({
    applicationId: applicationId,
    interviewType: 'TECHNICAL',
    meetingUrl: '',
    interviewerEmails: '',
    location: 'Google Meet / Online',
  });
  const [timeSlots, setTimeSlots] = useState([{ date: '', startTime: '08:00', endTime: '10:00' }]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSlotChange = (index, field, value) => {
    setTimeSlots((current) => current.map((slot, slotIndex) => (
      slotIndex === index ? { ...slot, [field]: value } : slot
    )));
  };

  const addTimeSlot = () => {
    setTimeSlots((current) => [...current, { date: '', startTime: '14:00', endTime: '16:00' }]);
  };

  const removeTimeSlot = (index) => {
    setTimeSlots((current) => current.filter((_, slotIndex) => slotIndex !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (timeSlots.length === 0) {
        throw new Error('Hãy thêm ít nhất một khung giờ để ứng viên lựa chọn.');
      }
      for (const slot of timeSlots) {
        if (!slot.date || !slot.startTime || !slot.endTime) {
          throw new Error('Hãy điền ngày, giờ bắt đầu và giờ kết thúc cho từng khung giờ.');
        }
        if (slot.endTime <= slot.startTime) {
          throw new Error('Giờ kết thúc phải sau giờ bắt đầu trong mỗi khung.');
        }
        if (new Date(`${slot.date}T${slot.startTime}`).getTime() <= Date.now()) {
          throw new Error('Các khung giờ phải nằm trong tương lai.');
        }
      }
      await interviewApi.schedule({
        applicationId: formData.applicationId,
        interviewType: formData.interviewType,
        durationMinutes: 60,
        timeSlots,
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
          Thêm một hoặc nhiều khung giờ. Ứng viên sẽ chọn một khung; thời lượng buổi phỏng vấn bằng khoảng thời gian đã chọn.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <TextField
              label="Mã hồ sơ ứng tuyển (Application ID)"
              name="applicationId"
              required
              fullWidth
              value={formData.applicationId}
              onChange={handleChange}
            />

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

            <Box>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Các khung giờ ứng viên có thể chọn
              </Typography>
              <Stack spacing={2}>
                {timeSlots.map((slot, index) => (
                  <Paper key={index} variant="outlined" sx={{ p: 2 }}>
                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
                      <TextField
                        label={`Ngày - Khung ${index + 1}`}
                        type="date"
                        required
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                        value={slot.date}
                        onChange={(event) => handleSlotChange(index, 'date', event.target.value)}
                      />
                      <TextField
                        label="Từ"
                        type="time"
                        required
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                        value={slot.startTime}
                        onChange={(event) => handleSlotChange(index, 'startTime', event.target.value)}
                      />
                      <TextField
                        label="Đến"
                        type="time"
                        required
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                        value={slot.endTime}
                        onChange={(event) => handleSlotChange(index, 'endTime', event.target.value)}
                      />
                      <Button
                        color="error"
                        disabled={timeSlots.length === 1}
                        onClick={() => removeTimeSlot(index)}
                      >
                        Xóa
                      </Button>
                    </Stack>
                  </Paper>
                ))}
                <Button
                  variant="outlined"
                  onClick={addTimeSlot}
                  disabled={timeSlots.length >= 10}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  + Thêm khung giờ
                </Button>
              </Stack>
            </Box>

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
