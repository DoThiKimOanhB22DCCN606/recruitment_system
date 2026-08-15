import React, { useEffect, useRef } from 'react';
import { useForm, Controller, useFieldArray } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, Grid, TextField, Button, Avatar,
  Stack, Divider, IconButton,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { companyApi } from '../../api/companyApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';

/**
 * Company profile page. Logo upload posts directly to the Company Service,
 * which stores the image in MinIO (bucket: company-logos) and returns a
 * presigned URL used as the <Avatar> source.
 */
export default function CompanyProfilePage() {
  const fileInputRef = useRef(null);
  const queryClient = useQueryClient();

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['company-profile'],
    queryFn: companyApi.getProfile,
  });

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      name: '', description: '', industry: '', companySize: '', website: '',
      email: '', phone: '', locations: [], socialLinks: [],
    },
  });

  useEffect(() => {
    if (profile) reset(profile);
  }, [profile, reset]);

  const locations = useFieldArray({ control, name: 'locations' });
  const socialLinks = useFieldArray({ control, name: 'socialLinks' });

  const invalidate = (data) => queryClient.setQueryData(['company-profile'], data);

  const saveMutation = useMutation({
    mutationFn: (payload) => companyApi.updateProfile(payload),
    onSuccess: invalidate,
  });

  const logoMutation = useMutation({
    mutationFn: (file) => companyApi.uploadLogo(file),
    onSuccess: invalidate,
  });

  const handleLogoChange = (e) => {
    const file = e.target.files?.[0];
    if (file) logoMutation.mutate(file);
    e.target.value = '';
  };

  if (isLoading) return <LoadingState />;
  if (isError) return <ErrorState />;

  return (
    <Card>
      <CardContent>
        <Stack direction="row" spacing={3} alignItems="center" sx={{ mb: 3 }}>
          <Avatar src={profile.logoUrl} sx={{ width: 80, height: 80 }} variant="rounded" />
          <Box>
            <Typography variant="h5" fontWeight={700}>{profile.name || 'Hồ sơ doanh nghiệp'}</Typography>
            <Button size="small" startIcon={<UploadFileIcon />} onClick={() => fileInputRef.current.click()} sx={{ mt: 1 }}>
              Cập nhật logo
            </Button>
            <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleLogoChange} />
          </Box>
        </Stack>

        <Box component="form" onSubmit={handleSubmit((v) => saveMutation.mutate(v))}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Controller name="name" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Tên công ty" required />} />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller name="industry" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Lĩnh vực hoạt động" />} />
            </Grid>
            <Grid item xs={12} md={4}>
              <Controller name="companySize" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Quy mô công ty" />} />
            </Grid>
            <Grid item xs={12} md={4}>
              <Controller name="website" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Website" />} />
            </Grid>
            <Grid item xs={12} md={4}>
              <Controller name="email" control={control} render={({ field }) =>
                <TextField {...field} fullWidth label="Email liên hệ" />} />
            </Grid>
            <Grid item xs={12}>
              <Controller name="description" control={control} render={({ field }) =>
                <TextField {...field} fullWidth multiline minRows={4} label="Mô tả doanh nghiệp" />} />
            </Grid>
          </Grid>

          <Divider sx={{ my: 3 }} />
          <SectionHeader title="Địa chỉ" onAdd={() => locations.append({ address: '', city: '', country: 'Vietnam' })} />
          {locations.fields.map((f, idx) => (
            <Grid container spacing={2} key={f.id} sx={{ mb: 1 }} alignItems="center">
              <Grid item xs={12} md={6}>
                <Controller name={`locations.${idx}.address`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Địa chỉ" size="small" />} />
              </Grid>
              <Grid item xs={12} md={4}>
                <Controller name={`locations.${idx}.city`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Thành phố" size="small" />} />
              </Grid>
              <Grid item xs={12} md={1}>
                <IconButton onClick={() => locations.remove(idx)}><DeleteIcon fontSize="small" /></IconButton>
              </Grid>
            </Grid>
          ))}

          <Divider sx={{ my: 3 }} />
          <SectionHeader title="Mạng xã hội" onAdd={() => socialLinks.append({ platform: 'LINKEDIN', url: '' })} />
          {socialLinks.fields.map((f, idx) => (
            <Grid container spacing={2} key={f.id} sx={{ mb: 1 }} alignItems="center">
              <Grid item xs={12} md={4}>
                <Controller name={`socialLinks.${idx}.platform`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="Nền tảng" size="small" />} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Controller name={`socialLinks.${idx}.url`} control={control}
                  render={({ field }) => <TextField {...field} fullWidth label="URL" size="small" />} />
              </Grid>
              <Grid item xs={12} md={1}>
                <IconButton onClick={() => socialLinks.remove(idx)}><DeleteIcon fontSize="small" /></IconButton>
              </Grid>
            </Grid>
          ))}

          <Button type="submit" variant="contained" size="large" sx={{ mt: 3 }}>
            Lưu hồ sơ doanh nghiệp
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
