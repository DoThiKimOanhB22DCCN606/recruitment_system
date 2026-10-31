import React, { useEffect, useState } from 'react';
import { useForm, Controller, useFieldArray } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Container, Card, CardContent, Typography, Grid, TextField, Button,
  IconButton, Stack, FormControlLabel, Switch,
  Avatar, Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonIcon from '@mui/icons-material/Person';
import { candidateApi } from '../../api/candidateApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';

export default function CandidateProfilePage() {
  const queryClient = useQueryClient();
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [avatarError, setAvatarError] = useState('');
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

  useEffect(() => {
    if (!profile?.avatarUrl) { setAvatarUrl(null); return; }
    let active = true;
    let url;
    candidateApi.getAvatar().then((blob) => {
      url = URL.createObjectURL(blob);
      if (active) setAvatarUrl(url);
      else URL.revokeObjectURL(url);
    }).catch(() => { if (active) setAvatarError('Không tải được ảnh đại diện.'); });
    return () => { active = false; if (url) URL.revokeObjectURL(url); };
  }, [profile?.avatarUrl]);

  const uploadAvatarMutation = useMutation({
    mutationFn: candidateApi.uploadAvatar,
    onSuccess: (data) => {
      setAvatarError('');
      queryClient.setQueryData(['candidate-profile'], data);
    },
    onError: (error) => setAvatarError(error.message || 'Không tải được ảnh đại diện.'),
  });

  const deleteAvatarMutation = useMutation({
    mutationFn: candidateApi.deleteAvatar,
    onSuccess: (data) => {
      setAvatarError('');
      setAvatarUrl(null);
      queryClient.setQueryData(['candidate-profile'], data);
    },
    onError: (error) => setAvatarError(error.message || 'Không xóa được ảnh đại diện.'),
  });

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
    <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 }, pb: 3 }}>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 2.5 }}>Hồ sơ cá nhân</Typography>
      <Card sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: { xs: 2, sm: 3 }, '&:last-child': { pb: { xs: 2, sm: 3 } } }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5} alignItems={{ xs: 'flex-start', sm: 'center' }}>
          <Box sx={{ position: 'relative', width: 72, height: 72, flexShrink: 0 }}>
            <Avatar src={avatarUrl || undefined} sx={{ width: 72, height: 72, bgcolor: 'primary.main' }}>
              <PersonIcon sx={{ color: 'white', fontSize: 42 }} />
            </Avatar>
            {profile.avatarUrl && (
              <IconButton
                aria-label="Xóa ảnh đại diện"
                title="Xóa ảnh đại diện"
                size="small"
                disabled={deleteAvatarMutation.isPending || uploadAvatarMutation.isPending}
                onClick={() => deleteAvatarMutation.mutate()}
                sx={{ position: 'absolute', right: -5, bottom: -5, bgcolor: 'background.paper', boxShadow: 2,
                  '&:hover': { bgcolor: 'grey.100' } }}
              >
                <DeleteIcon fontSize="small" color="error" />
              </IconButton>
            )}
          </Box>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography variant="h6" fontWeight={700} noWrap>{profile.fullName || 'Ứng viên'}</Typography>
            <Typography variant="body2" color="text.secondary" noWrap sx={{ mb: 1.5 }}>{profile.email}</Typography>
            <Button component="label" variant="outlined" size="small" disabled={uploadAvatarMutation.isPending}>
              {profile.avatarUrl ? 'Thay ảnh đại diện' : 'Tải ảnh đại diện'}
              <input hidden type="file" accept="image/png,image/jpeg" onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) uploadAvatarMutation.mutate(file);
                event.target.value = '';
              }} />
            </Button>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>PNG hoặc JPEG, tối đa 2 MB</Typography>
          </Box>
          <FormControlLabel
            sx={{ m: 0, alignSelf: { xs: 'flex-start', sm: 'center' } }}
            control={
              <Switch
                checked={profile.openToWork}
                onChange={(e) => openToWorkMutation.mutate(e.target.checked)}
              />
            }
            label="Sẵn sàng tìm việc"
          />
          </Stack>
          {avatarError && <Alert severity="error" sx={{ mt: 2 }}>{avatarError}</Alert>}
        </CardContent>
      </Card>

      <Box component="form" onSubmit={handleSubmit((v) => saveMutation.mutate(v))}>
        <Card sx={{ mb: 2.5, borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
          <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>Thông tin cá nhân</Typography>
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

          </CardContent>
        </Card>
        <Card sx={{ mb: 2.5, borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
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

          </CardContent>
        </Card>
        <Card sx={{ mb: 2.5, borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
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

          </CardContent>
        </Card>
        <Card sx={{ mb: 2.5, borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
          <SectionHeader title="Kỹ năng" onAdd={() => skills.append({ skillName: '', proficiency: '' })} />
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

          </CardContent>
        </Card>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button type="submit" variant="contained" size="large" disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Đang lưu...' : 'Lưu hồ sơ'}
          </Button>
        </Box>
      </Box>
    </Container>
  );
}

function SectionHeader({ title, onAdd }) {
  return (
    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
      <Typography variant="h6" fontWeight={700}>{title}</Typography>
      <Button size="small" variant="outlined" startIcon={<AddIcon />} onClick={onAdd}>Thêm</Button>
    </Stack>
  );
}
