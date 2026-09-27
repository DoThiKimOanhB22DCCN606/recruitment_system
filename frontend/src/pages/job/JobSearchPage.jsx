import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Grid, TextField, MenuItem, Card, CardContent, Typography,
  Pagination, Chip, Stack, Button,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'];
const WORK_TYPES = [
  { value: 'ONSITE', label: 'Tại văn phòng' },
  { value: 'HYBRID', label: 'Linh hoạt' },
  { value: 'REMOTE', label: 'Từ xa' },
];

export default function JobSearchPage() {
  const [filters, setFilters] = useState({
    keyword: '', location: '', employmentType: '', workType: '', skill: '', sortBy: 'publishedAt', sortOrder: 'desc', page: 0, size: 10,
  });

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['job-search', filters],
    queryFn: () =>
      jobApi.search({
        ...filters,
        workType: filters.workType || undefined,
        skill: filters.skill || undefined,
        sortBy: filters.sortBy || undefined,
        sortOrder: filters.sortOrder || undefined,
      }),
  });

  const update = (field) => (e) =>
    setFilters((f) => ({ ...f, [field]: e.target.value, page: 0 }));

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Tìm kiếm việc làm
      </Typography>

      <Card sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth label="Từ khoá (tiêu đề, công ty...)"
              value={filters.keyword} onChange={update('keyword')}
            />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth label="Kỹ năng" value={filters.skill} onChange={update('skill')} placeholder="Java, React..." />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth label="Địa điểm" value={filters.location} onChange={update('location')} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth label="Loại hình" value={filters.employmentType} onChange={update('employmentType')}>
              <MenuItem value="">Tất cả</MenuItem>
              {EMPLOYMENT_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth label="Hình thức" value={filters.workType} onChange={update('workType')}>
              <MenuItem value="">Tất cả</MenuItem>
              {WORK_TYPES.map((w) => <MenuItem key={w.value} value={w.value}>{w.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth label="Sắp xếp" value={filters.sortBy} onChange={update('sortBy')}>
              <MenuItem value="publishedAt">Mới nhất</MenuItem>
              <MenuItem value="salary">Mức lương</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <Button fullWidth variant="contained" sx={{ height: '100%' }} onClick={() => refetch()}>
              Tìm kiếm
            </Button>
          </Grid>
        </Grid>
      </Card>

      {isLoading && <LoadingState label="Đang tìm kiếm việc làm..." />}
      {isError && <ErrorState />}
      {data && !Array.isArray(data.content) && (
        <ErrorState message="Không thể tải dữ liệu việc làm. Định dạng phản hồi không hợp lệ." />
      )}

      {data && Array.isArray(data.content) && (
        <>
          <Stack spacing={2}>
            {data.content.map((job) => (
              <Card key={job.id} component={RouterLink} to={`/jobs/${job.id}`}
                    sx={{ textDecoration: 'none', p: 2, '&:hover': { boxShadow: 4 } }}>
                <CardContent sx={{ p: '8px !important' }}>
                  <Typography variant="h6" color="text.primary">{job.title}</Typography>
                  <Typography color="text.secondary">{job.companyName} · {job.location}</Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
                    {job.workType && (
                      <Chip
                        size="small"
                        label={job.workType}
                        color={job.workType === 'REMOTE' ? 'secondary' : job.workType === 'HYBRID' ? 'primary' : 'default'}
                      />
                    )}
                    <Chip size="small" label={job.employmentType} variant="outlined" />
                    {job.salaryMin && job.salaryMax && (
                      <Chip label={`${job.salaryMin} - ${job.salaryMax} ${job.salaryCurrency}`} />
                    )}
                    {(job.skills || []).slice(0, 4).map((s) => (
                      <Chip key={s} size="small" label={s} variant="outlined" />
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            ))}
            {data.content.length === 0 && (
              <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
                Không tìm thấy việc làm phù hợp.
              </Typography>
            )}
          </Stack>

          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
            <Pagination
              count={data.totalPages || 1}
              page={filters.page + 1}
              onChange={(_, p) => setFilters((f) => ({ ...f, page: p - 1 }))}
            />
          </Box>
        </>
      )}
    </Box>
  );
}
