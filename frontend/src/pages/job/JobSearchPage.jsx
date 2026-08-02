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

export default function JobSearchPage() {
  const [filters, setFilters] = useState({
    keyword: '', location: '', employmentType: '', remote: '', page: 0, size: 10,
  });

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['job-search', filters],
    queryFn: () =>
      jobApi.search({
        ...filters,
        remote: filters.remote === '' ? undefined : filters.remote === 'true',
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
              fullWidth label="Từ khoá (tiêu đề, kỹ năng, công ty...)"
              value={filters.keyword} onChange={update('keyword')}
            />
          </Grid>
          <Grid item xs={6} md={3}>
            <TextField fullWidth label="Địa điểm" value={filters.location} onChange={update('location')} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth label="Loại hình" value={filters.employmentType} onChange={update('employmentType')}>
              <MenuItem value="">Tất cả</MenuItem>
              {EMPLOYMENT_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth label="Remote" value={filters.remote} onChange={update('remote')}>
              <MenuItem value="">Tất cả</MenuItem>
              <MenuItem value="true">Remote</MenuItem>
              <MenuItem value="false">Tại văn phòng</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6} md={1}>
            <Button fullWidth variant="contained" sx={{ height: '100%' }} onClick={() => refetch()}>
              Tìm
            </Button>
          </Grid>
        </Grid>
      </Card>

      {isLoading && <LoadingState label="Đang tìm kiếm việc làm..." />}
      {isError && <ErrorState />}

      {data && (
        <>
          <Stack spacing={2}>
            {data.content.map((job) => (
              <Card key={job.id} component={RouterLink} to={`/jobs/${job.id}`}
                    sx={{ textDecoration: 'none', p: 2, '&:hover': { boxShadow: 4 } }}>
                <CardContent sx={{ p: '8px !important' }}>
                  <Typography variant="h6" color="text.primary">{job.title}</Typography>
                  <Typography color="text.secondary">{job.companyName} · {job.location}</Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
                    {job.remote && <Chip size="small" label="Remote" color="secondary" />}
                    <Chip size="small" label={job.employmentType} variant="outlined" />
                    {job.salaryMin && job.salaryMax && (
                      <Chip size="small" label={`${job.salaryMin} - ${job.salaryMax} ${job.salaryCurrency}`} />
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
