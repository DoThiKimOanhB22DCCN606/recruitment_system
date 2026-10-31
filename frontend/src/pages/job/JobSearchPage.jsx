import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Container, Grid, TextField, MenuItem, Card, Typography,
  Pagination, Chip, Stack, Button,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { jobApi } from '../../api/jobApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';
import { formatMoney } from '../../utils/money';
import JobCompanyHeader from '../../components/job/JobCompanyHeader';

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'];
const WORK_TYPES = [
  { value: 'ONSITE', label: 'Tại văn phòng' },
  { value: 'HYBRID', label: 'Linh hoạt' },
  { value: 'REMOTE', label: 'Từ xa' },
];
const EMPLOYMENT_LABELS = {
  FULL_TIME: 'Toàn thời gian',
  PART_TIME: 'Bán thời gian',
  CONTRACT: 'Hợp đồng',
  INTERNSHIP: 'Thực tập',
};

export default function JobSearchPage() {
  const [filters, setFilters] = useState({
    keyword: '', location: '', employmentType: '', workType: '', skill: '', sortBy: 'publishedAt', sortOrder: 'desc', page: 0, size: 10,
  });
  const [searchFilters, setSearchFilters] = useState(filters);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['job-search', searchFilters],
    queryFn: () =>
      jobApi.search({
        ...searchFilters,
        workType: searchFilters.workType || undefined,
        skill: searchFilters.skill || undefined,
        sortBy: searchFilters.sortBy || undefined,
        sortOrder: searchFilters.sortOrder || undefined,
      }),
  });

  const update = (field) => (e) =>
    setFilters((f) => ({ ...f, [field]: e.target.value }));

  const submitSearch = (event) => {
    event.preventDefault();
    const nextFilters = { ...filters, page: 0 };
    if (JSON.stringify(nextFilters) === JSON.stringify(searchFilters)) {
      refetch();
    } else {
      setSearchFilters(nextFilters);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 }, pb: 3 }}>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 2.5 }}>
        Tìm kiếm việc làm
      </Typography>

      <Card component="form" onSubmit={submitSearch} sx={{ p: { xs: 2, sm: 3 }, mb: 3, borderRadius: 3 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={4}>
            <TextField
              fullWidth label="Từ khoá (tiêu đề, công ty...)"
              value={filters.keyword} onChange={update('keyword')}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <TextField fullWidth label="Kỹ năng" value={filters.skill} onChange={update('skill')} placeholder="Java, React..." />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <TextField fullWidth label="Địa điểm" value={filters.location} onChange={update('location')} />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <TextField select fullWidth label="Loại hình" value={filters.employmentType} onChange={update('employmentType')}>
              <MenuItem value="">Tất cả</MenuItem>
              {EMPLOYMENT_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <TextField select fullWidth label="Hình thức" value={filters.workType} onChange={update('workType')}>
              <MenuItem value="">Tất cả</MenuItem>
              {WORK_TYPES.map((w) => <MenuItem key={w.value} value={w.value}>{w.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <TextField select fullWidth label="Sắp xếp" value={filters.sortBy} onChange={update('sortBy')}>
              <MenuItem value="publishedAt">Mới nhất</MenuItem>
              <MenuItem value="salary">Mức lương</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Button fullWidth variant="contained" type="submit" sx={{ minHeight: 56, borderRadius: 2 }}>
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
                    sx={{ display: 'block', textDecoration: 'none', p: { xs: 2, sm: 3 }, borderRadius: 3, border: '1px solid', borderColor: 'divider', transition: 'box-shadow 0.2s, border-color 0.2s', '&:hover': { boxShadow: 4, borderColor: 'primary.light' } }}>
                <JobCompanyHeader job={job}>
                  <Typography variant="h6" color="text.primary" fontWeight={700}>{job.title}</Typography>
                </JobCompanyHeader>
                {job.status === 'CLOSED' && (
                  <Chip size="small" color="default" label="Vị trí này đã ngừng tuyển" sx={{ mt: 1.5 }} />
                )}

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', sm: '145px 160px minmax(0, 1fr)' }, gap: { xs: 2, sm: 3 }, mt: 2.5, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Hình thức</Typography>
                    <Typography variant="body2" fontWeight={600} color="text.primary">
                      {WORK_TYPES.find((type) => type.value === job.workType)?.label || job.workType || 'Chưa cập nhật'}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Loại hình</Typography>
                    <Typography variant="body2" fontWeight={600} color="text.primary">
                      {EMPLOYMENT_LABELS[job.employmentType] || job.employmentType || 'Chưa cập nhật'}
                    </Typography>
                  </Box>
                  <Box sx={{ gridColumn: { xs: '1 / -1', sm: 'auto' } }}>
                    <Typography variant="caption" color="text.secondary">Mức lương</Typography>
                    <Typography variant="body2" fontWeight={700} color="primary.main">
                      {job.salaryMin != null && job.salaryMax != null
                        ? `${formatMoney(job.salaryMin)} - ${formatMoney(job.salaryMax)} ${job.salaryCurrency || ''}`
                        : 'Thoả thuận'}
                    </Typography>
                  </Box>
                </Box>
                {(job.skills || []).length > 0 && (
                  <Stack direction="row" useFlexGap flexWrap="wrap" spacing={1} sx={{ mt: 2 }}>
                    {job.skills.slice(0, 4).map((skill) => (
                      <Chip key={skill} size="small" label={skill} variant="outlined" />
                    ))}
                  </Stack>
                )}
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
              page={searchFilters.page + 1}
              onChange={(_, p) => setSearchFilters((f) => ({ ...f, page: p - 1 }))}
            />
          </Box>
        </>
      )}
    </Container>
  );
}
