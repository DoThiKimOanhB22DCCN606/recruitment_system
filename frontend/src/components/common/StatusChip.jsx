import React from 'react';
import { Chip } from '@mui/material';

const COLORS = {
  DRAFT: 'default',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'info',
  REJECTED: 'error',
  PUBLISHED: 'success',
  CLOSED: 'default',
};

const LABELS = {
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  PUBLISHED: 'Đã đăng',
  CLOSED: 'Đã đóng',
};

export default function StatusChip({ status }) {
  return <Chip size="small" color={COLORS[status] || 'default'} label={LABELS[status] || status} />;
}
