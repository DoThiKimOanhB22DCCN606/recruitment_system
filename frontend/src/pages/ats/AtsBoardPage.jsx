import React, { useState, useEffect } from 'react';
import {
  Container,
  Box,
  Typography,
  Paper,
  Stack,
  Card,
  CardContent,
  Button,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  CircularProgress,
  Alert,
  Tooltip,
} from '@mui/material';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import CloseIcon from '@mui/icons-material/Close';
import DescriptionIcon from '@mui/icons-material/Description';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DownloadIcon from '@mui/icons-material/Download';
import EventIcon from '@mui/icons-material/Event';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { atsApi } from '../../api/atsApi';
import { jobApi } from '../../api/jobApi';
import AppSnackbar from '../../components/common/AppSnackbar';

export default function AtsBoardPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();

  const [job, setJob] = useState(null);
  const [boardData, setBoardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [visibleCountMap, setVisibleCountMap] = useState({});

  const getVisibleLimit = (stageId) => visibleCountMap[stageId] || 10;

  const handleLoadMore = (stageId) => {
    setVisibleCountMap((prev) => ({
      ...prev,
      [stageId]: (prev[stageId] || 10) + 10,
    }));
  };

  // Transition Dialog state
  const [openTransition, setOpenTransition] = useState(false);
  const [selectedApp, setSelectedApp] = useState(null);
  const [targetStageId, setTargetStageId] = useState('');
  const [transitionReason, setTransitionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loadingCvId, setLoadingCvId] = useState(null);
  const [toast, setToast] = useState({ open: false, severity: 'success', message: '' });
  const closeToast = () => setToast((current) => ({ ...current, open: false }));
  const showToast = (message, severity = 'success') => setToast({ open: true, severity, message });

  const handleOpenCv = async (app) => {
    const isPdf = app.cvFileName?.toLowerCase().endsWith('.pdf');
    const pdfTab = isPdf ? window.open('about:blank', '_blank') : null;
    if (pdfTab) {
      pdfTab.document.title = app.cvFileName || 'CV ứng viên';
      pdfTab.document.body.innerHTML = '<p style="font: 16px sans-serif; padding: 24px">Đang tải CV…</p>';
    }
    setLoadingCvId(app.id);
    try {
      const blob = await atsApi.getApplicationCv(app.id);
      const url = URL.createObjectURL(blob);
      if (isPdf) {
        if (pdfTab && !pdfTab.closed) {
          pdfTab.location.href = url;
        } else {
          URL.revokeObjectURL(url);
          throw new Error('Trình duyệt đã chặn tab PDF mới. Hãy cho phép mở cửa sổ bật lên rồi thử lại.');
        }
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      } else {
        const link = document.createElement('a');
        link.href = url;
        link.download = app.cvFileName || 'candidate-cv';
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      if (pdfTab && !pdfTab.closed) pdfTab.close();
      showToast(err.message || 'Không thể mở CV ứng viên.', 'error');
    } finally {
      setLoadingCvId(null);
    }
  };

  const fetchBoard = async () => {
    setLoading(true);
    setError(null);
    try {
      const [jobRes, columns] = await Promise.all([
        jobApi.getEmployerById(jobId).catch(() => null),
        atsApi.getBoard(jobId),
      ]);
      setJob(jobRes);
      const stageColumns = Array.isArray(columns) ? columns : [];
      setBoardData({
        stages: stageColumns.map(({ id, name, sequenceOrder, terminal, total, hasMore }) => ({
          id, name, sequenceOrder, terminal, total, hasMore,
        })),
        applications: stageColumns.flatMap((stage) => stage.applications || []),
      });
    } catch (err) {
      setError(err.message || 'Không thể tải bảng Kanban ATS.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBoard();
  }, [jobId]);

  const handleOpenMove = (app, defaultTarget = '') => {
    setSelectedApp(app);
    setTargetStageId(defaultTarget);
    setTransitionReason('');
    setOpenTransition(true);
  };

  const handleMoveSubmit = async () => {
    if (!selectedApp || !targetStageId) return;
    const targetStage = (boardData?.stages || []).find((stage) => stage.id === targetStageId);
    const isRejectTarget = targetStage?.name?.toLowerCase().includes('reject');
    if (isRejectTarget && !transitionReason.trim()) {
      showToast('Vui lòng nhập lý do từ chối cụ thể để gửi cho ứng viên.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await atsApi.moveStage(selectedApp.id, {
        newStageId: targetStageId,
        reason: transitionReason.trim() || null,
      });
      setOpenTransition(false);
      fetchBoard();
    } catch (err) {
      showToast(err.message || 'Không thể chuyển trạng thái ứng viên.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  const stages = boardData?.stages || [
    { id: '1', name: 'Sourced', sequenceOrder: 1 },
    { id: '2', name: 'Screening', sequenceOrder: 2 },
    { id: '3', name: 'Interviewing', sequenceOrder: 3 },
    { id: '4', name: 'Offered', sequenceOrder: 4 },
    { id: '5', name: 'Hired', sequenceOrder: 5, terminal: true },
    { id: '6', name: 'Rejected', sequenceOrder: 6, terminal: true },
  ];

  const applications = boardData?.applications || [];
  const hasApplications = applications.length > 0 || stages.some((stage) => Number(stage.total || 0) > 0);
  const selectedTargetStage = stages.find((stage) => stage.id === targetStageId);
  const rejectionReasonRequired = selectedTargetStage?.name?.toLowerCase().includes('reject') || false;

  return (
    <Box sx={{ width: '100%', px: 2 }}>
      {/* Header */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Box>
          <Button
            component={RouterLink}
            to="/employer/jobs"
            startIcon={<ArrowBackIcon />}
            sx={{ mb: 1 }}
          >
            Danh sách tin tuyển dụng
          </Button>
          <Typography variant="h5" fontWeight={700}>
            Hồ sơ ứng tuyển: {job?.title || `Tin tuyển dụng #${jobId}`}
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={fetchBoard}
        >
          Làm mới
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {!error && boardData && !hasApplications && (
        <Paper variant="outlined" sx={{ p: 5, textAlign: 'center', borderRadius: 2 }}>
          <Typography color="text.secondary">Chưa có ứng viên ứng tuyển</Typography>
        </Paper>
      )}

      {/* Kanban Swimlanes */}
      {hasApplications && <Box
        sx={{
          display: 'flex',
          gap: 2,
          overflowX: 'auto',
          pb: 3,
          minHeight: '70vh',
          alignItems: 'flex-start',
        }}
      >
        {stages.map((stage, idx) => {
          const stageApps = applications.filter(
            (a) => a.currentStageId === stage.id || a.stageId === stage.id || a.stageName?.toLowerCase() === stage.name.toLowerCase()
          );

          const limit = getVisibleLimit(stage.id || stage.name);
          const displayedApps = stageApps.slice(0, limit);
          const hasMore = stageApps.length > limit;

          const isTerminalHired = stage.name.toLowerCase().includes('hired');
          const isTerminalRejected = stage.name.toLowerCase().includes('rejected');
          const nextStage = stages[idx + 1];

          return (
            <Paper
              key={stage.id || stage.name}
              elevation={2}
              sx={{
                width: 320,
                minWidth: 320,
                bgcolor: '#f8fafc',
                borderRadius: 2,
                p: 2,
                display: 'flex',
                flexDirection: 'column',
                borderTop: `4px solid ${
                  isTerminalHired
                    ? '#10b981'
                    : isTerminalRejected
                    ? '#ef4444'
                    : stage.sequenceOrder === 1
                    ? '#3b82f6'
                    : '#6366f1'
                }`,
              }}
            >
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="subtitle1" fontWeight={700}>
                  {stage.name}
                </Typography>
                <Chip
                  label={stageApps.length}
                  size="small"
                  color={stageApps.length > 0 ? 'primary' : 'default'}
                  sx={{ fontWeight: 700 }}
                />
              </Stack>

              {/* Application Cards */}
              <Stack spacing={1.5} sx={{ minHeight: 100 }}>
                {stageApps.length === 0 ? (
                  <Box
                    sx={{
                      p: 3,
                      border: '2px dashed #cbd5e1',
                      borderRadius: 2,
                      textAlign: 'center',
                    }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      Chưa có ứng viên
                    </Typography>
                  </Box>
                ) : (
                  displayedApps.map((app) => {
                    const isWithdrawn = app.status === 'WITHDRAWN';
                    return (
                      <Card
                        key={app.id}
                        elevation={1}
                        sx={{
                          borderRadius: 2,
                          opacity: isWithdrawn ? 0.7 : 1,
                          backgroundColor: isWithdrawn ? '#f8fafc' : '#ffffff',
                          border: isWithdrawn ? '1px dashed #ef4444' : '1px solid #e2e8f0'
                        }}
                      >
                        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                            <Typography variant="subtitle2" fontWeight={700}>
                              {app.candidateName || app.candidateEmail || `Ứng viên #${app.candidateId}`}
                            </Typography>
                            {isWithdrawn && (
                              <Chip label="Đã rút hồ sơ" color="error" size="small" sx={{ fontSize: '0.65rem', height: 20 }} />
                            )}
                          </Stack>
                          <Typography variant="caption" color="text.secondary" display="block">
                            Ứng tuyển: {app.appliedAt ? new Date(app.appliedAt).toLocaleDateString('vi-VN') : 'Mới'}
                          </Typography>

                          {app.candidateEmail && (
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                              {app.candidateEmail}
                            </Typography>
                          )}

                          {app.cvFileName && (
                            <Stack direction="row" spacing={0.5} alignItems="center" sx={{ mt: 1 }}>
                              <DescriptionIcon fontSize="small" color="action" />
                              <Typography variant="caption" color="text.secondary">CV:</Typography>
                              <Button
                                size="small"
                                startIcon={app.cvFileName.toLowerCase().endsWith('.pdf')
                                  ? <PictureAsPdfIcon />
                                  : <DownloadIcon />}
                                onClick={() => handleOpenCv(app)}
                                disabled={loadingCvId === app.id}
                                sx={{ textTransform: 'none', minWidth: 0 }}
                              >
                                {loadingCvId === app.id
                                  ? 'Đang mở CV…'
                                  : app.cvFileName}
                              </Button>
                            </Stack>
                          )}

                          {app.coverLetter && (
                            <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
                              {app.coverLetter}
                            </Typography>
                          )}

                          {/* Action buttons */}
                          {!isWithdrawn && (
                            <Stack direction="row" spacing={1} sx={{ mt: 1.5 }} flexWrap="wrap">
                              {nextStage && !stage.terminal && (
                                <Button
                                  size="small"
                                  variant="contained"
                                  color="primary"
                                  endIcon={<ArrowForwardIcon />}
                                  onClick={() => handleOpenMove(app, nextStage.id)}
                                  sx={{ textTransform: 'none', fontSize: '0.75rem', py: 0.3 }}
                                >
                                  Chuyển tiếp
                                </Button>
                              )}

                              {stage.name.toLowerCase().includes('interview') && (
                                <Tooltip title="Lên lịch phỏng vấn">
                                  <IconButton
                                    size="small"
                                    color="primary"
                                    onClick={() => navigate(`/employer/interviews/schedule?applicationId=${app.id}&candidateId=${app.candidateId}`)}
                                  >
                                    <EventIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}

                              {stage.name.toLowerCase().includes('offer') && (
                                <Tooltip title="Tạo thư mời nhận việc">
                                  <IconButton
                                    size="small"
                                    color="secondary"
                                    onClick={() => navigate(`/employer/offers/new?applicationId=${app.id}&candidateId=${app.candidateId}`)}
                                  >
                                    <LocalOfferIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}

                              {!stage.terminal && (
                                <Button
                                  size="small"
                                  variant="outlined"
                                  color="error"
                                  onClick={() => {
                                    const rejStage = stages.find((s) => s.name.toLowerCase().includes('reject'));
                                    handleOpenMove(app, rejStage?.id || '');
                                  }}
                                  sx={{ textTransform: 'none', fontSize: '0.75rem', py: 0.3 }}
                                >
                                  Từ chối
                                </Button>
                              )}
                            </Stack>
                          )}
                        </CardContent>
                      </Card>
                    );
                  })
                )}
                {hasMore && (
                  <Button
                    size="small"
                    variant="text"
                    onClick={() => handleLoadMore(stage.id || stage.name)}
                    sx={{ mt: 1, textTransform: 'none', width: '100%' }}
                  >
                    Tải thêm ({stageApps.length - limit} ứng viên)
                  </Button>
                )}
              </Stack>
            </Paper>
          );
        })}
      </Box>}

      {/* Move Stage Modal */}
      <Dialog open={openTransition} onClose={() => setOpenTransition(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Chuyển bước quy trình tuyển dụng</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {(() => {
              const currentStage = stages.find((s) => s.id === selectedApp?.stageId || s.id === selectedApp?.currentStageId);
              const targetStage = stages.find((s) => s.id === targetStageId);
              const isBackward = currentStage && targetStage && targetStage.sequenceOrder < currentStage.sequenceOrder;
              if (isBackward) {
                return (
                  <Alert severity="warning">
                    Cảnh báo: Bạn đang chuyển ứng viên lùi về bước trước đó trong quy trình ({currentStage.name} → {targetStage.name}).
                  </Alert>
                );
              }
              return null;
            })()}
            <TextField
              select
              label="Bước mục tiêu"
              fullWidth
              required
              value={targetStageId}
              onChange={(e) => setTargetStageId(e.target.value)}
            >
              {stages.map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {s.name} {s.terminal ? '(Kết thúc)' : ''}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label={rejectionReasonRequired
                ? 'Lý do từ chối (ứng viên sẽ nhận được)'
                : 'Ghi chú gửi ứng viên (không bắt buộc)'}
              multiline
              rows={3}
              fullWidth
              required={rejectionReasonRequired}
              value={transitionReason}
              onChange={(e) => setTransitionReason(e.target.value)}
              placeholder={rejectionReasonRequired
                ? 'Nhập lý do cụ thể, dễ hiểu cho ứng viên...'
                : 'Nhập ghi chú nếu muốn ứng viên biết thêm thông tin...'}
              helperText={rejectionReasonRequired
                ? 'Lý do này sẽ được gửi trong thông báo từ chối.'
                : 'Ghi chú đã nhập sẽ xuất hiện trong thông báo cho ứng viên.'}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenTransition(false)} disabled={submitting}>
            Hủy
          </Button>
          <Button
            onClick={handleMoveSubmit}
            variant="contained"
            disabled={submitting || !targetStageId || (rejectionReasonRequired && !transitionReason.trim())}
          >
            {submitting ? 'Đang cập nhật...' : 'Xác nhận chuyển'}
          </Button>
        </DialogActions>
      </Dialog>
      <AppSnackbar
        open={toast.open}
        severity={toast.severity}
        message={toast.message}
        onClose={closeToast}
      />
    </Box>
  );
}
