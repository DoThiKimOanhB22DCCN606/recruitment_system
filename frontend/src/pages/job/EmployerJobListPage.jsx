import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Button, Table, TableHead, TableRow, TableCell, TableBody,
  IconButton, Stack,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import StatusChip from '../../components/common/StatusChip';

export default function EmployerJobListPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['employer-jobs'],
    queryFn: () => jobApi.list({}),
  });

  const removeMutation = useMutation({
    mutationFn: (id) => jobApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employer-jobs'] }),
  });

  const submitMutation = useMutation({
    mutationFn: (id) => jobApi.changeStatus(id, { targetStatus: 'PENDING_APPROVAL', note: 'Submitted for approval' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employer-jobs'] }),
  });

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
          {data.content.map((job) => (
            <TableRow key={job.id} hover>
              <TableCell>{job.title}</TableCell>
              <TableCell>{job.location}</TableCell>
              <TableCell><StatusChip status={job.status} /></TableCell>
              <TableCell align="right">
                {job.status === 'DRAFT' && (
                  <IconButton title="Gửi duyệt" onClick={() => submitMutation.mutate(job.id)}>
                    <SendIcon fontSize="small" />
                  </IconButton>
                )}
                <IconButton component={RouterLink} to={`/employer/jobs/${job.id}/edit`} title="Chỉnh sửa">
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton title="Xoá" onClick={() => removeMutation.mutate(job.id)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}
