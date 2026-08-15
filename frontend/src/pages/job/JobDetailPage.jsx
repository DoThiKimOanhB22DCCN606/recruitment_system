import React from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, Chip, Stack, Divider, Button } from '@mui/material';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import StatusChip from '../../components/common/StatusChip';

export default function JobDetailPage() {
  const { id } = useParams();
  const { data: job, isLoading, isError } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobApi.getById(id),
  });

  if (isLoading) return <LoadingState label="Đang tải chi tiết việc làm..." />;
  if (isError || !job) return <ErrorState />;

  return (
    <Card>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="h4" fontWeight={700}>{job.title}</Typography>
            <Typography variant="subtitle1" color="text.secondary">
              {job.companyName} · {job.location}
            </Typography>
          </Box>
          <StatusChip status={job.status} />
        </Stack>

        <Stack direction="row" spacing={1} sx={{ my: 2, flexWrap: 'wrap' }}>
          <Chip label={job.employmentType} />
          {job.experienceLevel && <Chip label={job.experienceLevel} variant="outlined" />}
          {job.remote && <Chip label="Remote" color="secondary" />}
          {job.salaryMin && job.salaryMax && (
            <Chip label={`${job.salaryMin} - ${job.salaryMax} ${job.salaryCurrency}`} />
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

        <Button variant="contained" size="large" sx={{ mt: 2 }}>
          Ứng tuyển ngay
        </Button>
      </CardContent>
    </Card>
  );
}
