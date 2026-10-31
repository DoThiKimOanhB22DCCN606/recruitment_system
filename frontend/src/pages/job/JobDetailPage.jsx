import React, { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, Chip, Stack, Divider, Button, Tooltip, Alert } from '@mui/material';
import { jobApi } from '../../api/jobApi';
import { useAuth } from '../../context/AuthContext';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import StatusChip from '../../components/common/StatusChip';
import ApplyModal from '../../components/job/ApplyModal';
import { formatMoney } from '../../utils/money';
import JobCompanyHeader from '../../components/job/JobCompanyHeader';

export default function JobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAuthenticated, role } = useAuth();
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [applied, setApplied] = useState(false);

  const { data: job, isLoading, isError } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobApi.getById(id),
  });

  if (isLoading) return <LoadingState label="Đang tải chi tiết việc làm..." />;
  if (isError || !job) return <ErrorState />;

  const isEmployer = role === 'HR_ADMIN' || role === 'RECRUITER' || role === 'SYS_ADMIN';
  const isCandidate = role === 'CANDIDATE';
  const isClosed = job.status === 'CLOSED';

  const handleApplyClick = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }
    if (isClosed) return;
    setApplyModalOpen(true);
  };

  return (
    <Card>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <JobCompanyHeader job={job} logoSize={72}>
            <Typography variant="h4" fontWeight={700}>{job.title}</Typography>
          </JobCompanyHeader>
          <StatusChip status={job.status} />
        </Stack>

        {isClosed && <Alert severity="info" sx={{ mt: 2 }}>Vị trí này đã ngừng tuyển. Các hồ sơ đã nộp vẫn được giữ nguyên.</Alert>}

        <Stack direction="row" spacing={1} sx={{ my: 2, flexWrap: 'wrap' }}>
          <Chip label={job.employmentType} />
          {job.experienceLevel && <Chip label={job.experienceLevel} variant="outlined" />}
          {job.workType && (
            <Chip
              label={job.workType}
              color={job.workType === 'REMOTE' ? 'secondary' : job.workType === 'HYBRID' ? 'primary' : 'default'}
            />
          )}
          {job.salaryMin && job.salaryMax && (
            <Chip label={`${formatMoney(job.salaryMin)} - ${formatMoney(job.salaryMax)} ${job.salaryCurrency}`} />
          )}
          {(job.skills || []).map((s) => <Chip key={s} label={s} variant="outlined" />)}
        </Stack>

        <Divider sx={{ my: 2 }} />

        <Typography variant="h6" gutterBottom>Mô tả công việc</Typography>
        <Typography whiteSpace="pre-line" paragraph>{job.description}</Typography>

        {job.requirements && (
          <>
            <Typography variant="h6" gutterBottom>Yêu cầu</Typography>
            <Typography whiteSpace="pre-line" paragraph>{job.requirements}</Typography>
          </>
        )}

        {job.benefits && (
          <>
            <Typography variant="h6" gutterBottom>Quyền lợi</Typography>
            <Typography whiteSpace="pre-line" paragraph>{job.benefits}</Typography>
          </>
        )}

        {isClosed ? (
          <Button variant="outlined" size="large" disabled sx={{ mt: 2 }}>
            Vị trí này đã ngừng tuyển
          </Button>
        ) : isEmployer ? (
          <Tooltip title="Tài khoản Nhà tuyển dụng không thể nộp đơn ứng tuyển">
            <span>
              <Button variant="contained" size="large" disabled sx={{ mt: 2 }}>
                Ứng tuyển ngay
              </Button>
            </span>
          </Tooltip>
        ) : isCandidate ? (
          <Button
            variant="contained"
            size="large"
            color={applied ? 'success' : 'primary'}
            sx={{ mt: 2, fontWeight: 700 }}
            onClick={handleApplyClick}
            disabled={applied}
          >
            {applied ? 'Đã nộp đơn ứng tuyển' : 'Ứng tuyển ngay'}
          </Button>
        ) : (
          <Tooltip title="Chỉ tài khoản Candidate mới được nộp hồ sơ">
            <span>
              <Button variant="contained" size="large" disabled sx={{ mt: 2 }}>
                Chỉ Candidate được ứng tuyển
              </Button>
            </span>
          </Tooltip>
        )}

        <ApplyModal
          open={applyModalOpen}
          onClose={() => setApplyModalOpen(false)}
          job={job}
          onSuccess={() => {
            setApplied(true);
          }}
        />
      </CardContent>
    </Card>
  );
}
