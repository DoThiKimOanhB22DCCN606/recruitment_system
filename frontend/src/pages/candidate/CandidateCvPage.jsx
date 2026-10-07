import React, { useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, List, ListItem, ListItemText,
  IconButton, Button, Chip, Stack,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DeleteIcon from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
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
    <Card>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h5" fontWeight={700}>CV của tôi</Typography>
          <Button variant="contained" startIcon={<UploadFileIcon />} onClick={() => fileInputRef.current.click()}>
            Tải lên CV
          </Button>
          <input ref={fileInputRef} type="file" accept=".pdf,.doc,.docx" hidden onChange={handleFileChange} />
        </Stack>

        <List>
          {cvs.map((cv) => (
            <ListItem key={cv.id} divider
              secondaryAction={
                <Stack direction="row" spacing={0.5}>
                  <IconButton title="Tải xuống" component="a" href={cv.downloadUrl} target="_blank" rel="noreferrer">
                    <DownloadIcon fontSize="small" />
                  </IconButton>
                  <IconButton title="Đặt làm mặc định" onClick={() => defaultMutation.mutate(cv.id)}>
                    {cv.isDefault ? <StarIcon color="warning" fontSize="small" /> : <StarBorderIcon fontSize="small" />}
                  </IconButton>
                  <IconButton title="Xoá" onClick={() => deleteMutation.mutate(cv.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              }>
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} alignItems="center">
                    <span>{cv.fileName}</span>
                    {cv.isDefault && <Chip size="small" color="warning" label="Mặc định" />}
                  </Stack>
                }
                secondary={`${(cv.fileSize / 1024).toFixed(0)} KB · Tải lên lúc ${new Date(cv.uploadedAt).toLocaleString('vi-VN')}`}
              />
            </ListItem>
          ))}
          {cvs.length === 0 && (
            <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
              Bạn chưa tải lên CV nào.
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
}
