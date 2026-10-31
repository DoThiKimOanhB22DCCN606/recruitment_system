import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Button, Table, TableHead, TableRow, TableCell, TableBody,
  IconButton, Stack, Alert,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import PublishIcon from '@mui/icons-material/Publish';
import LockIcon from '@mui/icons-material/Lock';
import BlockIcon from '@mui/icons-material/Block';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import StatusChip from '../../components/common/StatusChip';
import { useAuth } from '../../context/AuthContext';

export default function EmployerJobListPage() {
  const queryClient = useQueryClient();
  const { role } = useAuth();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['employer-jobs'],
    queryFn: () => jobApi.listForEmployer({}),
  });

  const removeMutation = useMutation({
    mutationFn: (id) => jobApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employer-jobs'] }),
  });

  const submitMutation = useMutation({
    mutationFn: (id) => jobApi.changeStatus(id, { targetStatus: 'PENDING_APPROVAL', note: 'Submitted for approval' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employer-jobs'] }),
  });

  const publishMutation = useMutation({
    mutationFn: (id) => jobApi.changeStatus(id, { targetStatus: 'PUBLISHED', note: 'Published by HR_ADMIN' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employer-jobs'] }),
  });

  const closeMutation = useMutation({
    mutationFn: (id) => jobApi.changeStatus(id, { targetStatus: 'CLOSED', note: 'Dừng tuyển bởi HR_ADMIN' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employer-jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job-search'] });
      queryClient.invalidateQueries({ queryKey: ['job'] });
    },
  });

  const closeJob = (job) => {
    if (window.confirm(`Dừng tuyển vị trí "${job.title}"? Các hồ sơ đã nộp vẫn được giữ nguyên.`)) {
      closeMutation.mutate(job.id);
    }
  };

  if (isLoading) return <LoadingState />;
  if (isError) return <ErrorState />;

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" fontWeight={700}>Quản lý tin tuyển dụng</Typography>
        <Button variant="contained" startIcon={<AddIcon />} component={RouterLink} to="/employer/jobs/new">
          Tạo tin mới
        </Button>
      </Stack>

      {closeMutation.isError && <Alert severity="error" sx={{ mb: 2 }}>Không thể dừng tuyển. Vui lòng thử lại.</Alert>}

      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Tiêu đề</TableCell>
            <TableCell>Địa điểm</TableCell>
            <TableCell>Trạng thái</TableCell>
            <TableCell align="right">Thao tác</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Array.isArray(data?.content) && data.content.map((job) => (
            <TableRow key={job.id} hover>
              <TableCell>{job.title}</TableCell>
              <TableCell>{job.location}</TableCell>
              <TableCell><StatusChip status={job.status} /></TableCell>
              <TableCell align="right">
                <Button
                  size="small"
                  variant="outlined"
                  component={RouterLink}
                  to={`/employer/jobs/${job.id}/ats`}
                  sx={{ mr: 1, textTransform: 'none' }}
                >
                  Quy trình ATS
                </Button>
                {job.status === 'DRAFT' && role === 'HR_ADMIN' && (
                  <IconButton title="Publish" onClick={() => publishMutation.mutate(job.id)}>
                    <PublishIcon fontSize="small" />
                  </IconButton>
                )}
                {job.status === 'DRAFT' && role === 'RECRUITER' && (
                  <IconButton title="Gửi duyệt" onClick={() => submitMutation.mutate(job.id)}>
                    <SendIcon fontSize="small" />
                  </IconButton>
                )}
                {job.status === 'PUBLISHED' && role === 'HR_ADMIN' && (
                  <Button size="small" color="warning" startIcon={<BlockIcon />}
                    disabled={closeMutation.isPending} onClick={() => closeJob(job)}
                    sx={{ mr: 1, textTransform: 'none' }}>
                    Dừng tuyển
                  </Button>
                )}
                {job.status === 'PENDING_APPROVAL' || job.status === 'CLOSED' ? (
                  <IconButton disabled title={job.status === 'CLOSED' ? 'Tin đã ngừng tuyển' : 'Đang chờ HR_ADMIN duyệt'}>
                    <LockIcon fontSize="small" />
                  </IconButton>
                ) : (
                  <IconButton component={RouterLink} to={`/employer/jobs/${job.id}/edit`} title="Chỉnh sửa">
                    <EditIcon fontSize="small" />
                  </IconButton>
                )}
                {job.status === 'DRAFT' && (
                  <IconButton title="Xoá" onClick={() => removeMutation.mutate(job.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}
