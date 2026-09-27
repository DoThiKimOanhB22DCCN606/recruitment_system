import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  TextField,
  RadioGroup,
  FormControlLabel,
  Radio,
  Box,
  Alert,
  CircularProgress,
  Stack,
  Divider,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { candidateApi } from '../../api/candidateApi';
import { atsApi } from '../../api/atsApi';

function formatToUuid(val, prefix = '00000000-0000-0000-0000-') {
  if (!val) return '00000000-0000-4000-8000-000000000001';
  const str = String(val);
  const uuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
  if (uuidRegex.test(str)) {
    return str;
  }
  return prefix + str.replace(/\D/g, '').slice(-12).padStart(12, '0');
}

export default function ApplyModal({ open, onClose, job, onSuccess }) {
  const [cvs, setCvs] = useState([]);
  const [selectedCvId, setSelectedCvId] = useState('');
  const [coverLetter, setCoverLetter] = useState('');
  const [loadingCvs, setLoadingCvs] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (open) {
      loadCvs();
      setSuccess(false);
      setError(null);
    }
  }, [open]);

  const loadCvs = async () => {
    setLoadingCvs(true);
    setError(null);
    try {
      const res = await candidateApi.getCvs();
      const list = Array.isArray(res) ? res : res?.content || [];
      setCvs(list);
      if (list.length > 0) {
        const def = list.find((c) => c.isDefault) || list[0];
        setSelectedCvId(String(def.id));
      } else {
        setSelectedCvId('');
      }
    } catch (err) {
      setError('Không thể tải danh sách CV. Vui lòng thử lại.');
    } finally {
      setLoadingCvs(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setError(null);
    try {
      const newCv = await candidateApi.uploadCv(file, true);
      await loadCvs();
      if (newCv?.id) {
        setSelectedCvId(String(newCv.id));
      }
    } catch (err) {
      setError(err.message || 'Tải lên CV thất bại. Hỗ trợ tệp PDF, DOC, DOCX dưới 5MB.');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedCvId) {
      setError('Vui lòng chọn hoặc tải lên một CV để ứng tuyển.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const formattedJobId = formatToUuid(job?.id, '00000000-0000-0000-0000-');
      const formattedCvId = formatToUuid(selectedCvId, '00000000-0000-0000-0001-');
      const idempotencyKey =
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : '50000000-0000-4000-8000-' + Date.now().toString(16).padStart(12, '0');

      await atsApi.apply(
        {
          jobId: formattedJobId,
          cvId: formattedCvId,
          coverLetter: coverLetter.trim() || undefined,
        },
        idempotencyKey
      );

      setSuccess(true);
      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      const msg = err.message || '';
      if (msg.includes('ERR_DUPLICATE_APPLICATION') || msg.includes('already applied')) {
        setError('Bạn đã nộp hồ sơ ứng tuyển vào vị trí này trước đó.');
      } else {
        setError(msg || 'Nộp hồ sơ thất bại. Vui lòng kiểm tra lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 700 }}>
        Ứng tuyển: {job?.title}
      </DialogTitle>
      <DialogContent dividers>
        {success ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CheckCircleIcon color="success" sx={{ fontSize: 64, mb: 2 }} />
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Nộp hồ sơ thành công!
            </Typography>
            <Typography color="text.secondary">
              Nhà tuyển dụng sẽ xem xét hồ sơ và liên hệ với bạn trong thời gian sớm nhất.
            </Typography>
          </Box>
        ) : (
          <Box component="form" onSubmit={handleSubmit}>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Typography variant="subtitle2" fontWeight={700} gutterBottom>
              Chọn CV ứng tuyển
            </Typography>

            {loadingCvs ? (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 2 }}>
                <CircularProgress size={20} />
                <Typography variant="body2" color="text.secondary">
                  Đang tải hồ sơ của bạn...
                </Typography>
              </Box>
            ) : cvs.length > 0 ? (
              <RadioGroup
                value={selectedCvId}
                onChange={(e) => setSelectedCvId(e.target.value)}
                sx={{ mb: 2 }}
              >
                {cvs.map((cv) => (
                  <FormControlLabel
                    key={cv.id}
                    value={String(cv.id)}
                    control={<Radio />}
                    label={
                      <Box>
                        <Typography variant="body2" fontWeight={600}>
                          {cv.fileName} {cv.isDefault && '(Mặc định)'}
                        </Typography>
                        {cv.fileSize && (
                          <Typography variant="caption" color="text.secondary">
                            {(cv.fileSize / 1024).toFixed(0)} KB
                          </Typography>
                        )}
                      </Box>
                    }
                  />
                ))}
              </RadioGroup>
            ) : (
              <Alert severity="info" sx={{ mb: 2 }}>
                Bạn chưa có CV nào trong hồ sơ. Vui lòng tải lên CV bên dưới.
              </Alert>
            )}

            <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
              <Button
                variant="outlined"
                component="label"
                startIcon={<CloudUploadIcon />}
                disabled={uploading}
                size="small"
              >
                {uploading ? 'Đang tải lên...' : 'Tải lên CV mới (.pdf, .docx)'}
                <input
                  type="file"
                  hidden
                  accept=".pdf,.doc,.docx"
                  onChange={handleFileUpload}
                />
              </Button>
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" fontWeight={700} gutterBottom>
              Thư giới thiệu (Không bắt buộc)
            </Typography>
            <TextField
              fullWidth
              multiline
              rows={4}
              placeholder="Chia sẻ lý do bạn phù hợp với vị trí này..."
              value={coverLetter}
              onChange={(e) => setCoverLetter(e.target.value)}
              inputProps={{ maxLength: 2000 }}
              helperText={`${coverLetter.length}/2000 ký tự`}
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        {success ? (
          <Button variant="contained" onClick={onClose}>
            Đóng
          </Button>
        ) : (
          <>
            <Button onClick={onClose} disabled={submitting}>
              Huỷ
            </Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={submitting || uploading || (!selectedCvId && cvs.length === 0)}
            >
              {submitting ? 'Đang gửi...' : 'Nộp hồ sơ'}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}
