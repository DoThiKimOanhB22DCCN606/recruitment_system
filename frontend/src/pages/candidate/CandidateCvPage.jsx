import React, { useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Container, Typography, Card, IconButton, Button, Chip, Stack,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DeleteIcon from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import DescriptionIcon from '@mui/icons-material/Description';
import { candidateApi } from '../../api/candidateApi';
import { LoadingState, ErrorState } from '../../components/common/LoadingState';

/**
 * CV management UI. Uploads go straight to the Candidate Service, which
 * stores the file in MinIO (bucket: candidate-cvs) and returns a short-lived
 * presigned download URL for each CV.
 */
export default function CandidateCvPage() {
  const fileInputRef = useRef(null);
  const queryClient = useQueryClient();

  const { data: cvs, isLoading, isError } = useQuery({
    queryKey: ['candidate-cvs'],
    queryFn: candidateApi.listCvs,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['candidate-cvs'] });

  const uploadMutation = useMutation({
    mutationFn: (file) => candidateApi.uploadCv(file),
    onSuccess: invalidate,
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => candidateApi.deleteCv(id),
    onSuccess: invalidate,
  });

  const defaultMutation = useMutation({
    mutationFn: (id) => candidateApi.setDefaultCv(id),
    onSuccess: invalidate,
  });

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) uploadMutation.mutate(file);
    e.target.value = '';
  };

  if (isLoading) return <LoadingState />;
  if (isError) return <ErrorState />;

  return (
    <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 }, pb: 3 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} sx={{ mb: 2.5 }}>
          <Box>
            <Typography variant="h5" fontWeight={700}>CV của tôi</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Quản lý CV đã tải lên và chọn CV mặc định để ứng tuyển.</Typography>
          </Box>
          <Button variant="contained" startIcon={<UploadFileIcon />} disabled={uploadMutation.isPending} onClick={() => fileInputRef.current?.click()}>
            Tải lên CV
          </Button>
          <input ref={fileInputRef} type="file" accept=".pdf,.doc,.docx" hidden onChange={handleFileChange} />
        </Stack>

        {cvs.length === 0 ? (
          <Card sx={{ p: { xs: 3, sm: 6 }, borderRadius: 3, textAlign: 'center' }}>
            <DescriptionIcon sx={{ fontSize: 44, color: 'text.disabled', mb: 1 }} />
            <Typography fontWeight={600}>Bạn chưa tải lên CV nào</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Tải CV lên để sử dụng khi ứng tuyển.</Typography>
          </Card>
        ) : (
          <Stack spacing={2}>
            {cvs.map((cv) => (
              <Card key={cv.id} sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3, border: '1px solid', borderColor: 'divider' }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'flex-start', sm: 'center' }} justifyContent="space-between">
                  <Stack direction="row" spacing={2} alignItems="center" sx={{ minWidth: 0, flexGrow: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 48, height: 48, flexShrink: 0, borderRadius: 2, bgcolor: 'action.hover', color: 'primary.main' }}>
                      <DescriptionIcon />
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                      <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
                        <Typography fontWeight={700} sx={{ overflowWrap: 'anywhere' }}>{cv.fileName}</Typography>
                        {cv.isDefault && <Chip size="small" color="warning" label="Mặc định" />}
                      </Stack>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                        {(cv.fileSize / 1024).toFixed(0)} KB · Tải lên lúc {new Date(cv.uploadedAt).toLocaleString('vi-VN')}
                      </Typography>
                    </Box>
                  </Stack>
                  <Stack direction="row" spacing={0.5} alignItems="center" sx={{ flexShrink: 0, alignSelf: { xs: 'flex-end', sm: 'center' } }}>
                    <IconButton aria-label={`Mở CV ${cv.fileName}`} title="Mở CV" component="a" href={cv.downloadUrl} target="_blank" rel="noreferrer">
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                    <IconButton aria-label={`Đặt ${cv.fileName} làm mặc định`} title="Đặt làm mặc định" disabled={defaultMutation.isPending || cv.isDefault} onClick={() => defaultMutation.mutate(cv.id)}>
                      {cv.isDefault ? <StarIcon color="warning" fontSize="small" /> : <StarBorderIcon fontSize="small" />}
                    </IconButton>
                    <IconButton aria-label={`Xoá CV ${cv.fileName}`} title="Xoá" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate(cv.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Stack>
              </Card>
            ))}
          </Stack>
        )}
    </Container>
  );
}
