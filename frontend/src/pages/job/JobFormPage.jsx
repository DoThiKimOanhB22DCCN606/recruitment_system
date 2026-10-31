import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Box, Grid, TextField, MenuItem, Button, Typography, Card, CardContent, Alert } from '@mui/material';
import { jobApi } from '../../api/jobApi';
import { formatMoney, formatMoneyInput, parseMoneyInput } from '../../utils/money';

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'];
const EXPERIENCE_LEVELS = ['INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD'];
const WORK_TYPES = [
  { value: 'ONSITE', label: 'Tại văn phòng (Onsite)' },
  { value: 'HYBRID', label: 'Linh hoạt (Hybrid)' },
  { value: 'REMOTE', label: 'Từ xa (Remote)' },
];

export default function JobFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: existingJob } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobApi.getEmployerById(id),
    enabled: isEdit,
  });

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      title: '', description: '', requirements: '', benefits: '', location: '',
      salaryMin: '', salaryMax: '', salaryCurrency: 'VND',
      employmentType: 'FULL_TIME', experienceLevel: 'MID', workType: 'ONSITE',
      vacancies: 1, skills: '', tags: '', expiresAt: '',
    },
  });

  useEffect(() => {
    if (existingJob) {
      reset({
        ...existingJob,
        salaryMin: formatMoney(existingJob.salaryMin),
        salaryMax: formatMoney(existingJob.salaryMax),
        workType: existingJob.workType || 'ONSITE',
        expiresAt: existingJob.expiresAt?.slice(0, 16) || '',
        skills: (existingJob.skills || []).join(', '),
        tags: (existingJob.tags || []).join(', '),
      });
    }
  }, [existingJob, reset]);

  const saveMutation = useMutation({
    mutationFn: (payload) => (isEdit ? jobApi.update(id, payload) : jobApi.create(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employer-jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job-search'] });
      queryClient.invalidateQueries({ queryKey: ['job', id] });
      navigate('/employer/jobs');
    },
  });

  const onSubmit = (values) => {
    const payload = {
      ...values,
      salaryMin: parseMoneyInput(values.salaryMin),
      salaryMax: parseMoneyInput(values.salaryMax),
      vacancies: Number(values.vacancies),
      expiresAt: values.expiresAt ? `${values.expiresAt}:00` : null,
      skills: values.skills ? values.skills.split(',').map((s) => s.trim()).filter(Boolean) : [],
      tags: values.tags ? values.tags.split(',').map((s) => s.trim()).filter(Boolean) : [],
    };
    saveMutation.mutate(payload);
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          {isEdit ? 'Chỉnh sửa tin tuyển dụng' : 'Tạo tin tuyển dụng mới'}
        </Typography>

        <Box component="form" onSubmit={handleSubmit(onSubmit)}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Controller name="title" control={control} rules={{ required: true }}
                render={({ field }) => <TextField {...field} fullWidth label="Tiêu đề tin tuyển dụng" required />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="expiresAt" control={control}
                rules={{ validate: (value) => !value || new Date(value).getTime() > Date.now() || 'Hạn tuyển phải sau thời điểm hiện tại' }}
                render={({ field, fieldState }) => (
                  <TextField {...field} fullWidth type="datetime-local"
                    label={isEdit ? 'Hạn tuyển (có thể thay đổi)' : 'Hạn tuyển'}
                    InputLabelProps={{ shrink: true }} error={!!fieldState.error}
                    helperText={fieldState.error?.message || 'Giờ Việt Nam. Để trống: tự đóng sau 30 ngày kể từ khi đăng tin'} />
                )} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="description" control={control} rules={{ required: true }}
                render={({ field }) => <TextField {...field} fullWidth multiline minRows={4} label="Mô tả công việc" required />} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="requirements" control={control}
                render={({ field }) => <TextField {...field} fullWidth multiline minRows={3} label="Yêu cầu" />} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="benefits" control={control}
                render={({ field }) => <TextField {...field} fullWidth multiline minRows={3} label="Quyền lợi" />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="location" control={control} rules={{ required: true }}
                render={({ field }) => <TextField {...field} fullWidth label="Địa điểm" required />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="salaryMin" control={control}
                render={({ field }) => <TextField {...field} fullWidth type="text" inputMode="decimal" label="Lương tối thiểu" onChange={(event) => field.onChange(formatMoneyInput(event.target.value))} />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="salaryMax" control={control}
                render={({ field }) => <TextField {...field} fullWidth type="text" inputMode="decimal" label="Lương tối đa" onChange={(event) => field.onChange(formatMoneyInput(event.target.value))} />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="employmentType" control={control}
                render={({ field }) => (
                  <TextField {...field} select fullWidth label="Loại hình">
                    {EMPLOYMENT_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                  </TextField>
                )} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="experienceLevel" control={control}
                render={({ field }) => (
                  <TextField {...field} select fullWidth label="Kinh nghiệm">
                    {EXPERIENCE_LEVELS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                  </TextField>
                )} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="vacancies" control={control}
                render={({ field }) => <TextField {...field} fullWidth type="number" label="Số lượng tuyển" />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="workType" control={control}
                render={({ field }) => (
                  <TextField {...field} select fullWidth label="Hình thức làm việc">
                    {WORK_TYPES.map((w) => <MenuItem key={w.value} value={w.value}>{w.label}</MenuItem>)}
                  </TextField>
                )} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="skills" control={control}
                render={({ field }) => <TextField {...field} fullWidth label="Kỹ năng (cách nhau bởi dấu phẩy)" />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="tags" control={control}
                render={({ field }) => <TextField {...field} fullWidth label="Tags (cách nhau bởi dấu phẩy)" />} />
            </Grid>
          </Grid>

          {saveMutation.isError && <Alert severity="error" sx={{ mt: 2 }}>Không thể lưu tin tuyển dụng. Vui lòng kiểm tra hạn tuyển và thử lại.</Alert>}
          <Button type="submit" variant="contained" size="large" sx={{ mt: 3 }} disabled={saveMutation.isPending}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo tin (Nháp)'}
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}
