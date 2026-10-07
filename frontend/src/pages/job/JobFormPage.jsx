import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box, Grid, TextField, MenuItem, Button, Typography, Card, CardContent,
  FormControlLabel, Switch,
} from '@mui/material';
import { jobApi } from '../../api/jobApi';

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'];
const EXPERIENCE_LEVELS = ['INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD'];

export default function JobFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: existingJob } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobApi.getById(id),
    enabled: isEdit,
  });

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      title: '', description: '', requirements: '', benefits: '', location: '',
      salaryMin: '', salaryMax: '', salaryCurrency: 'VND',
      employmentType: 'FULL_TIME', experienceLevel: 'MID', remote: false,
      vacancies: 1, skills: '', tags: '', expiryDays: 30,
    },
  });

  useEffect(() => {
    if (existingJob) {
      reset({
        ...existingJob,
        skills: (existingJob.skills || []).join(', '),
        tags: (existingJob.tags || []).join(', '),
      });
    }
  }, [existingJob, reset]);

  const saveMutation = useMutation({
    mutationFn: (payload) => (isEdit ? jobApi.update(id, payload) : jobApi.create(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employer-jobs'] });
      navigate('/employer/jobs');
    },
  });

  const onSubmit = (values) => {
    const payload = {
      ...values,
      salaryMin: values.salaryMin ? Number(values.salaryMin) : null,
      salaryMax: values.salaryMax ? Number(values.salaryMax) : null,
      vacancies: Number(values.vacancies),
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
                render={({ field }) => <TextField {...field} fullWidth type="number" label="Lương tối thiểu" />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <Controller name="salaryMax" control={control}
                render={({ field }) => <TextField {...field} fullWidth type="number" label="Lương tối đa" />} />
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
              <Controller name="remote" control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={<Switch checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                    label="Remote"
                  />
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

          <Button type="submit" variant="contained" size="large" sx={{ mt: 3 }}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo tin (Nháp)'}
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}
