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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Rating,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import RateReviewIcon from '@mui/icons-material/RateReview';
import CancelIcon from '@mui/icons-material/Cancel';
import { interviewApi } from '../../api/interviewApi';
import { useAuth } from '../../context/AuthContext';

export default function InterviewListPage() {
  const { hasRole, user } = useAuth();
  const isEmployer = hasRole(['HR_ADMIN', 'RECRUITER']);

  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Feedback dialog state
  const [openFeedback, setOpenFeedback] = useState(false);
  const [selectedInterview, setSelectedInterview] = useState(null);
  const [rating, setRating] = useState(4);
  const [feedbackNotes, setFeedbackNotes] = useState('');
  const [recommendation, setRecommendation] = useState('PROCEED');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  const fetchInterviews = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = isEmployer
        ? await interviewApi.getInterviews()
        : await interviewApi.getMyInterviews();
      setInterviews(Array.isArray(data) ? data : data?.content || []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách phỏng vấn.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInterviews();
  }, [isEmployer]);

  const handleOpenFeedback = (interview) => {
    setSelectedInterview(interview);
    setRating(4);
    setFeedbackNotes('');
    setRecommendation('PROCEED');
    setOpenFeedback(true);
  };

  const handleSubmitFeedback = async () => {
    if (!selectedInterview) return;
    setSubmittingFeedback(true);
    try {
      await interviewApi.submitFeedback(selectedInterview.id, {
        score: rating,
        feedback: feedbackNotes,
        recommendation,
      });
      setOpenFeedback(false);
      fetchInterviews();
    } catch (err) {
      alert(err.message || 'Không thể gửi đánh giá phỏng vấn.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleCancelInterview = async (interviewId) => {
    const reason = prompt('Nhập lý do hủy buổi phỏng vấn:');
    if (reason === null) return;
    try {
      await interviewApi.cancel(interviewId, reason || 'Cancelled by recruiter');
      fetchInterviews();
    } catch (err) {
      alert(err.message || 'Không thể hủy phỏng vấn.');
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'SCHEDULED':
        return <Chip label="Đã lên lịch" color="primary" size="small" />;
      case 'COMPLETED':
        return <Chip label="Đã hoàn thành" color="success" size="small" />;
      case 'CANCELLED':
        return <Chip label="Đã hủy" color="error" size="small" />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  return (
    <Container maxWidth="lg">
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <div>
          <Typography variant="h5" fontWeight={700}>
            {isEmployer ? 'Quản lý Lịch phỏng vấn' : 'Lịch phỏng vấn của tôi'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {isEmployer ? 'Theo dõi và đánh giá các buổi phỏng vấn ứng viên' : 'Lịch phỏng vấn của bạn'}
          </Typography>
        </div>
        {isEmployer && (
          <Button
            component={RouterLink}
            to="/employer/interviews/schedule"
            variant="contained"
            startIcon={<AddIcon />}
          >
            Lên lịch phỏng vấn mới
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
      ) : interviews.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center', borderRadius: 2 }}>
          <Typography color="text.secondary">Chưa có lịch phỏng vấn nào.</Typography>
        </Paper>
      ) : (
        <Paper elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: '#f8fafc' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>{isEmployer ? 'Ứng viên / Vị trí' : 'Lịch của tôi'}</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Hình thức</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Thời gian</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Thời lượng</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Địa điểm / link</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Trạng thái</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="right">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {interviews.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Typography variant="subtitle2" fontWeight={600}>
                      {isEmployer ? (item.candidateName || `Ứng viên #${item.candidateId}`) : 'Lịch phỏng vấn của bạn'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Hồ sơ #{item.applicationId}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={item.interviewType || 'TECHNICAL'} variant="outlined" size="small" />
                  </TableCell>
                  <TableCell>
                    {(item.scheduledAt || (item.scheduledDate && item.startTime))
                      ? new Date(item.scheduledAt || `${item.scheduledDate}T${item.startTime}`).toLocaleString('vi-VN', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })
                      : 'Chưa đặt'}
                  </TableCell>
                  <TableCell>{item.durationMinutes || 60} phút</TableCell>
                  <TableCell>{item.locationOrUrl || item.location || 'Chưa có địa điểm'}</TableCell>
                  <TableCell>{getStatusChip(item.status)}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      {(item.meetingUrl || /^https?:\/\//i.test(item.locationOrUrl || '')) && (
                        <Button
                          size="small"
                          variant="outlined"
                          href={item.meetingUrl || item.locationOrUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          Vào phòng họp
                        </Button>
                      )}
                      {isEmployer && item.status === 'SCHEDULED' && (
                        <>
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            startIcon={<RateReviewIcon />}
                            onClick={() => handleOpenFeedback(item)}
                          >
                            Đánh giá
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            startIcon={<CancelIcon />}
                            onClick={() => handleCancelInterview(item.id)}
                          >
                            Hủy
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

      {/* Submit Feedback Dialog */}
      <Dialog open={openFeedback} onClose={() => setOpenFeedback(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Đánh giá phỏng vấn</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Điểm đánh giá:
              </Typography>
              <Rating
                value={rating}
                onChange={(e, val) => setRating(val || 1)}
                max={5}
                size="large"
              />
            </Box>
            <TextField
              label="Kết luận khuyến nghị"
              select
              fullWidth
              value={recommendation}
              onChange={(e) => setRecommendation(e.target.value)}
            >
              <MenuItem value="PROCEED">Đạt yêu cầu - Đề xuất chuyển tiếp (Proceed)</MenuItem>
              <MenuItem value="OFFER">Đạt yêu cầu - Đề xuất gửi Offer (Offer)</MenuItem>
              <MenuItem value="REJECT">Không đạt yêu cầu (Reject)</MenuItem>
              <MenuItem value="SECOND_OPINION">Cần phỏng vấn thêm (Second Round)</MenuItem>
            </TextField>
            <TextField
              label="Nhận xét chi tiết"
              multiline
              rows={4}
              fullWidth
              required
              value={feedbackNotes}
              onChange={(e) => setFeedbackNotes(e.target.value)}
              placeholder="Nhập đánh giá về chuyên môn, thái độ, sự phù hợp văn hóa..."
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenFeedback(false)} disabled={submittingFeedback}>
            Hủy
          </Button>
          <Button
            onClick={handleSubmitFeedback}
            variant="contained"
            disabled={submittingFeedback || !feedbackNotes}
          >
            {submittingFeedback ? 'Đang lưu...' : 'Lưu đánh giá'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
