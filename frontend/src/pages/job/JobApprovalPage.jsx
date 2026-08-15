import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Table, TableHead, TableRow, TableCell, TableBody, Button, Stack,
} from '@mui/material';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import StatusChip from '../../components/common/StatusChip';

/**
 * Admin view: lists jobs pending approval and lets the admin approve/reject.
 * In production this would call GET /jobs?status=PENDING_APPROVAL; here we
 * filter client-side over the general listing endpoint for simplicity.
 */
export default function JobApprovalPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['jobs-all-for-approval'],
    queryFn: () => jobApi.list({}),
  });

  const decide = useMutation({
    mutationFn: ({ id, targetStatus, note }) => jobApi.changeStatus(id, { targetStatus, note }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['jobs-all-for-approval'] }),
  });

  if (isLoading) return <LoadingState />;
  if (isError) return <ErrorState />;

  const pending = data.content.filter((j) => j.status === 'PENDING_APPROVAL');

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Duyệt tin tuyển dụng
      </Typography>

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
          {pending.map((job) => (
            <TableRow key={job.id} hover>
              <TableCell>{job.title}</TableCell>
              <TableCell>{job.location}</TableCell>
              <TableCell><StatusChip status={job.status} /></TableCell>
              <TableCell align="right">
                <Stack direction="row" spacing={1} justifyContent="flex-end">
                  <Button size="small" variant="contained" color="success"
                    onClick={() => decide.mutate({ id: job.id, targetStatus: 'APPROVED', note: 'Approved' })}>
                    Duyệt
                  </Button>
                  <Button size="small" variant="outlined" color="error"
                    onClick={() => decide.mutate({ id: job.id, targetStatus: 'REJECTED', note: 'Rejected' })}>
                    Từ chối
                  </Button>
                </Stack>
              </TableCell>
            </TableRow>
          ))}
          {pending.length === 0 && (
            <TableRow>
              <TableCell colSpan={4} align="center">Không có tin nào đang chờ duyệt.</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Box>
  );
}
