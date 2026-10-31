import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert, Box, Button, Paper, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import SearchIcon from '@mui/icons-material/Search';
import { adminApi } from '../../api/adminApi';

const toInstant = (value) => value ? new Date(value).toISOString() : undefined;

export default function AuditLogPage() {
  const [draft, setDraft] = useState({ tenantId: '', actorId: '', entityId: '', actionType: '', from: '', to: '' });
  const [filters, setFilters] = useState({});
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [exportError, setExportError] = useState('');

  const params = {
    ...filters,
    from: toInstant(filters.from),
    to: toInstant(filters.to),
    page,
    size,
  };
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === undefined) delete params[key];
  });

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['admin-audit-logs', params],
    queryFn: () => adminApi.getAuditLogs(params),
  });

  const applyFilters = (event) => {
    event.preventDefault();
    setPage(0);
    setFilters({ ...draft });
  };

  const exportCsv = async () => {
    setExportError('');
    try {
      const exportParams = { ...filters, from: toInstant(filters.from), to: toInstant(filters.to) };
      Object.keys(exportParams).forEach((key) => {
        if (!exportParams[key]) delete exportParams[key];
      });
      const blob = await adminApi.exportAuditLogsCsv(exportParams);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'audit_logs.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setExportError(err.message || 'Không thể xuất audit log.');
    }
  };

  const logs = data?.content || [];

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto', px: 2 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Audit log</Typography>
          <Typography color="text.secondary">Tra cứu hoạt động hệ thống và xuất kết quả ra CSV.</Typography>
        </Box>
        <Button variant="outlined" startIcon={<DownloadIcon />} onClick={exportCsv}>Xuất CSV</Button>
      </Stack>

      <Paper component="form" variant="outlined" onSubmit={applyFilters} sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" flexWrap="wrap" gap={2}>
          <TextField label="Tenant ID" size="small" value={draft.tenantId} onChange={(e) => setDraft({ ...draft, tenantId: e.target.value })} />
          <TextField label="Actor ID" size="small" value={draft.actorId} onChange={(e) => setDraft({ ...draft, actorId: e.target.value })} />
          <TextField label="Entity ID" size="small" value={draft.entityId} onChange={(e) => setDraft({ ...draft, entityId: e.target.value })} />
          <TextField label="Action type" size="small" value={draft.actionType} onChange={(e) => setDraft({ ...draft, actionType: e.target.value })} />
          <TextField label="Từ thời điểm" type="datetime-local" size="small" InputLabelProps={{ shrink: true }} value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
          <TextField label="Đến thời điểm" type="datetime-local" size="small" InputLabelProps={{ shrink: true }} value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
          <Button type="submit" variant="contained" startIcon={<SearchIcon />}>Lọc</Button>
        </Stack>
      </Paper>

      {isError && <Alert severity="error" sx={{ mb: 2 }}>{error?.message || 'Không tải được audit log. Kiểm tra quyền SYS_ADMIN và kết nối IAM.'}</Alert>}
      {exportError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setExportError('')}>{exportError}</Alert>}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead><TableRow>
            <TableCell>Thời gian</TableCell><TableCell>Hành động</TableCell><TableCell>Entity</TableCell>
            <TableCell>Entity ID</TableCell><TableCell>Tenant ID</TableCell><TableCell>Actor ID</TableCell><TableCell>IP</TableCell>
          </TableRow></TableHead>
          <TableBody>
            {isLoading && <TableRow><TableCell colSpan={7} align="center">Đang tải…</TableCell></TableRow>}
            {!isLoading && logs.map((log) => (
              <TableRow key={log.id} hover>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '—'}</TableCell>
                <TableCell>{log.actionType}</TableCell><TableCell>{log.entityType}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>{log.entityId}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>{log.tenantId || '—'}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>{log.actorId || '—'}</TableCell>
                <TableCell>{log.ipAddress || '—'}</TableCell>
              </TableRow>
            ))}
            {!isLoading && !isError && logs.length === 0 && <TableRow><TableCell colSpan={7} align="center">Không có bản ghi phù hợp.</TableCell></TableRow>}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={data?.totalElements || 0}
          page={page}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          rowsPerPage={size}
          onRowsPerPageChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}
          rowsPerPageOptions={[10, 20, 50]}
          labelRowsPerPage="Số dòng mỗi trang"
        />
      </TableContainer>
    </Box>
  );
}
