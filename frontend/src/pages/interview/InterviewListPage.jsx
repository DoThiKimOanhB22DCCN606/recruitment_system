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
import { atsApi } from '../../api/atsApi';
import { jobApi } from '../../api/jobApi';
import { companyApi } from '../../api/companyApi';
import { useAuth } from '../../context/AuthContext';
import AppSnackbar from '../../components/common/AppSnackbar';

export default function InterviewListPage() {
  const { hasRole, user } = useAuth();
  const isEmployer = hasRole(['HR_ADMIN', 'RECRUITER']);

  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedSlots, setSelectedSlots] = useState({});
  const [toast, setToast] = useState({ open: false, severity: 'success', message: '' });
  const closeToast = () => setToast((current) => ({ ...current, open: false }));
  const showToast = (message, severity = 'success') => setToast({ open: true, severity, message });

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
                || application?.fullName,
              candidateEmail: application?.candidateEmail
                || application?.candidate?.email,
            };
          } catch {
            return item;
          }
        }));
        setInterviews(enriched);
      } else {
        const applicationIds = [...new Set(items.map((item) => item.applicationId).filter(Boolean))];
        const applicationEntries = await Promise.all(applicationIds.map(async (applicationId) => {
          try {
            const response = await atsApi.getApplication(applicationId);
            return [applicationId, response?.data || response?.application || response];
          } catch {
            return [applicationId, null];
          }
        }));
        const applicationsById = new Map(applicationEntries);
        const jobIds = [...new Set(applicationEntries
          .map(([, application]) => application?.jobId)
          .filter(Boolean))];
        const jobEntries = await Promise.all(jobIds.map(async (jobId) => {
          try {
            return [jobId, await jobApi.getJobById(jobId)];
          } catch {
            return [jobId, null];
          }
        }));
        const jobsById = new Map(jobEntries);
        const tenantIds = [...new Set(applicationEntries
          .map(([, application]) => application?.tenantId)
          .filter(Boolean))];
        const brandEntries = await Promise.all(tenantIds.map(async (tenantId) => {
          try {
            return [tenantId, await companyApi.getBrandByTenant(tenantId)];
          } catch {
            return [tenantId, null];
          }
        }));
        const brandsByTenantId = new Map(brandEntries);
        setInterviews(items.map((item) => {
          const application = applicationsById.get(item.applicationId);
          const job = jobsById.get(application?.jobId);
          const brand = brandsByTenantId.get(application?.tenantId);
          return {
            ...item,
            jobTitle: job?.title || application?.jobTitle || item.jobTitle,
            companyName: brand?.name || job?.companyName || application?.companyName || item.companyName,
          };
        }));
      }
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
      showToast('Đã lưu đánh giá phỏng vấn.');
      fetchInterviews();
    } catch (err) {
      showToast(err.message || 'Không thể gửi đánh giá phỏng vấn.', 'error');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleCancelInterview = async (interviewId) => {
    const reason = prompt('Nhập lý do hủy buổi phỏng vấn:');
    if (reason === null) return;
    try {
      await interviewApi.cancel(interviewId, reason || 'Cancelled by recruiter');
      showToast('Đã hủy lịch phỏng vấn.');
      fetchInterviews();
    } catch (err) {
      showToast(err.message || 'Không thể hủy phỏng vấn.', 'error');
    }
  };

  const handleSelectTimeSlot = async (interview) => {
    const slotIndex = selectedSlots[interview.id];
    if (slotIndex === undefined || slotIndex === '') return;
    try {
      await interviewApi.selectTimeSlot(interview.id, Number(slotIndex));
      showToast('Bạn đã xác nhận khung giờ phỏng vấn.');
      fetchInterviews();
    } catch (err) {
      showToast(err.message || 'Không thể chọn khung giờ phỏng vấn.', 'error');
      if (err.status === 409) fetchInterviews();
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'SCHEDULED':
        return <Chip label="Đã lên lịch" color="primary" size="small" />;
      case 'AWAITING_CANDIDATE_SELECTION':
        return <Chip label="Chờ bạn chọn khung giờ" color="warning" size="small" />;
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
                <TableCell sx={{ fontWeight: 700 }}>{isEmployer ? 'Ứng viên / Vị trí' : 'Vị trí ứng tuyển / Công ty'}</TableCell>
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
                      {isEmployer
                        ? (item.candidateName || item.candidateEmail || 'Chưa có tên ứng viên')
                        : (item.jobTitle || 'Vị trí ứng tuyển')}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {isEmployer
                        ? (item.candidateName && item.candidateEmail ? item.candidateEmail : 'Hồ sơ ứng tuyển')
                        : (item.companyName || 'Chưa có thông tin công ty')}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={item.interviewType || 'TECHNICAL'} variant="outlined" size="small" />
                  </TableCell>
                  <TableCell>
                    {item.status === 'AWAITING_CANDIDATE_SELECTION'
                      ? (
                        <Stack spacing={0.5}>
                          <Typography variant="caption" color="text.secondary">
                            {isEmployer ? 'Các khung giờ đã gửi' : 'Chọn một khung giờ bên phải'}
                          </Typography>
                          {item.timeSlots?.map((slot, index) => (
                            <Typography key={`${slot.date}-${slot.startTime}-${index}`} variant="body2">
                              {new Date(`${slot.date}T00:00:00`).toLocaleDateString('vi-VN')}
                              {' · '}{String(slot.startTime).slice(0, 5)}–{String(slot.endTime).slice(0, 5)}
                            </Typography>
                          ))}
                        </Stack>
                      )
                      : (item.scheduledAt || (item.scheduledDate && item.startTime))
                      ? new Date(item.scheduledAt || `${item.scheduledDate}T${item.startTime}`).toLocaleString('vi-VN', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })
                      : 'Chưa đặt'}
                  </TableCell>
                  <TableCell>{item.status === 'AWAITING_CANDIDATE_SELECTION'
                    ? 'Theo khung giờ'
                    : `${item.durationMinutes || 60} phút`}</TableCell>
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
                      {!isEmployer && item.status === 'AWAITING_CANDIDATE_SELECTION' && (
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                          <TextField
                            select
                            size="small"
                            label="Chọn khung giờ"
                            value={selectedSlots[item.id] ?? ''}
                            onChange={(event) => setSelectedSlots((current) => ({
                              ...current,
                              [item.id]: event.target.value,
                            }))}
                            sx={{ minWidth: 250 }}
                          >
                            {item.timeSlots?.map((slot, index) => (
                              <MenuItem key={`${slot.date}-${slot.startTime}-${index}`} value={index}>
                                {new Date(`${slot.date}T00:00:00`).toLocaleDateString('vi-VN')}
                                {' · '}{String(slot.startTime).slice(0, 5)}–{String(slot.endTime).slice(0, 5)}
                              </MenuItem>
                            ))}
                          </TextField>
                          <Button
                            size="small"
                            variant="contained"
                            disabled={selectedSlots[item.id] === undefined || selectedSlots[item.id] === ''}
                            onClick={() => handleSelectTimeSlot(item)}
                          >
                            Xác nhận
                          </Button>
                        </Stack>
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

      <AppSnackbar
        open={toast.open}
        severity={toast.severity}
        message={toast.message}
        onClose={closeToast}
      />
    </Container>
  );
}
