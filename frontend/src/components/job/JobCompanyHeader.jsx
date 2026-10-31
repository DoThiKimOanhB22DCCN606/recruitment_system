import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Avatar, Box, Stack, Typography } from '@mui/material';
import { companyApi } from '../../api/companyApi';

export default function JobCompanyHeader({ job, children, logoSize = 56 }) {
  const { data: brand } = useQuery({
    queryKey: ['company-brand', job.tenantId],
    queryFn: () => companyApi.getBrandByTenant(job.tenantId),
    enabled: Boolean(job.tenantId),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  const name = brand?.name || job.companyName || 'Doanh nghiệp tuyển dụng';
  const logoUrl = brand?.hasLogo
    ? `/api/v1/companies/tenant/${job.tenantId}/logo?v=${brand.logoVersion || ''}`
    : undefined;

  return (
    <Stack direction="row" spacing={2} alignItems="center">
      <Avatar src={logoUrl} alt={`Logo ${name}`} variant="rounded"
        sx={{ width: logoSize, height: logoSize, bgcolor: 'primary.light', flexShrink: 0 }}>
        {name.charAt(0).toUpperCase()}
      </Avatar>
      <Box sx={{ minWidth: 0 }}>
        {children}
        <Typography color="text.secondary" sx={{ mt: 0.5 }} noWrap>{name}</Typography>
      </Box>
    </Stack>
  );
}
