import React, { useEffect } from 'react';
import { useForm, Controller, useFieldArray } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, Grid, TextField, Button, Divider,
  IconButton, Stack, FormControlLabel, Switch,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { candidateApi } from '../../api/candidateApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';

export default function CandidateProfilePage() {
  const queryClient = useQueryClient();
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['candidate-profile'],
    queryFn: candidateApi.getProfile,
  });

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      fullName: '', email: '', phone: '', headline: '', summary: '', address: '',
      educations: [], experiences: [], skills: [],
    },
  });

  useEffect(() => {
    if (profile) reset(profile);
  }, [profile, reset]);

  const educations = useFieldArray({ control, name: 'educations' });
  const experiences = useFieldArray({ control, name: 'experiences' });
  const skills = useFieldArray({ control, name: 'skills' });

  const saveMutation = useMutation({
    mutationFn: (payload) => candidateApi.updateProfile(payload),
    onSuccess: (data) => queryClient.setQueryData(['candidate-profile'], data),
  });

  const openToWorkMutation = useMutation({
    mutationFn: (openToWork) => candidateApi.setOpenToWork({ openToWork, durationDays: 30 }),
    onSuccess: (data) => queryClient.setQueryData(['candidate-profile'], data),
  });

  if (isLoading) return <LoadingState />;
  if (isError) return <ErrorState />;

  return (
    <Card>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5" fontWeight={700}>Hồ sơ ứng viên</Typography>
          <FormControlLabel
            control={
              <Switch
                checked={profile.openToWork}
                onChange={(e) => openToWorkMutation.mutate(e.target.checked)}
              />
            }
            label="Open To Work"
          />
        </Stack>

        <Box component="form" onSubmit={handleSubmit((v) => saveMutation.mutate(v))} sx={{ mt: 2 }}>
          <Typography variant="h6" gutterBottom>Thông tin cá nhân</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Controller name="fullName" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Họ và tên" required />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="email" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Email" required />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="phone" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Số điện thoại" />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="headline" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Chức danh mong muốn" />} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="address" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Địa chỉ" />} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="summary" control={control} render={({ field }) =>
                <TextField {...field} fullWidth multiline minRows={3} label="Giới thiệu bản thân" />} />
            </Grid>
          </Grid>

          <Divider sx={{ my: 3 }} />
          <SectionHeader title="Học vấn" onAdd={() => educations.append({ schoolName: '', degree: '', fieldOfStudy: '' })} />
          {educations.fields.map((f, idx) => (
            <Grid container spacing={2} key={f.id} sx={{ mb: 1 }} alignItems="center">
              <Grid item xs={12} md={4}>
                <Controller name={`educations.${idx}.schoolName`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Trường" size="small" />} />
              </Grid>
              <Grid item xs={12} md={3}>
                <Controller name={`educations.${idx}.degree`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Bằng cấp" size="small" />} />
              </Grid>
              <Grid item xs={12} md={4}>
                <Controller name={`educations.${idx}.fieldOfStudy`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Chuyên ngành" size="small" />} />
              </Grid>
              <Grid item xs={12} md={1}>
                <IconButton onClick={() => educations.remove(idx)}><DeleteIcon fontSize="small" /></IconButton>
              </Grid>
            </Grid>
          ))}

          <Divider sx={{ my: 3 }} />
          <SectionHeader title="Kinh nghiệm" onAdd={() => experiences.append({ companyName: '', jobTitle: '' })} />
          {experiences.fields.map((f, idx) => (
            <Grid container spacing={2} key={f.id} sx={{ mb: 1 }} alignItems="center">
              <Grid item xs={12} md={5}>
                <Controller name={`experiences.${idx}.companyName`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Công ty" size="small" />} />
              </Grid>
              <Grid item xs={12} md={5}>
                <Controller name={`experiences.${idx}.jobTitle`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Vị trí" size="small" />} />
              </Grid>
              <Grid item xs={12} md={1}>
                <IconButton onClick={() => experiences.remove(idx)}><DeleteIcon fontSize="small" /></IconButton>
              </Grid>
            </Grid>
          ))}

          <Divider sx={{ my: 3 }} />
          <SectionHeader title="Kỹ năng" onAdd={() => skills.append({ skillName: '', proficiency: 'INTERMEDIATE' })} />
          {skills.fields.map((f, idx) => (
            <Grid container spacing={2} key={f.id} sx={{ mb: 1 }} alignItems="center">
              <Grid item xs={12} md={6}>
                <Controller name={`skills.${idx}.skillName`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Kỹ năng" size="small" />} />
              </Grid>
              <Grid item xs={12} md={5}>
                <Controller name={`skills.${idx}.proficiency`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Trình độ" size="small" />} />
              </Grid>
              <Grid item xs={12} md={1}>
                <IconButton onClick={() => skills.remove(idx)}><DeleteIcon fontSize="small" /></IconButton>
              </Grid>
            </Grid>
          ))}

          <Button type="submit" variant="contained" size="large" sx={{ mt: 3 }}>
            Lưu hồ sơ
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

function SectionHeader({ title, onAdd }) {
  return (
    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
      <Typography variant="h6">{title}</Typography>
      <IconButton color="primary" onClick={onAdd}><AddIcon /></IconButton>
    </Stack>
  );
}
