import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, IconButton, Paper, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { adminApi } from '../../api/adminApi';

const emptyForm = { name: '', contactEmail: '', taxCode: '', status: 'ACTIVE' };
const emptyAdminForm = { email: '', password: '', confirmPassword: '' };
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

export default function TenantManagementPage() {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTenant, setEditingTenant] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState('');
  const [adminDialogTenant, setAdminDialogTenant] = useState(null);
  const [adminForm, setAdminForm] = useState(emptyAdminForm);
  const [adminError, setAdminError] = useState('');
  const [createdAdmin, setCreatedAdmin] = useState(null);
  const [registrationMessage, setRegistrationMessage] = useState('');

  const { data: tenants = [], isLoading, isError } = useQuery({
    queryKey: ['admin-tenants'],
    queryFn: adminApi.getTenants,
  });

  const { data: registrations = [], isError: registrationsError } = useQuery({
    queryKey: ['admin-company-registrations'],
    queryFn: adminApi.getCompanyRegistrations,
  });

  const registrationMutation = useMutation({
    mutationFn: ({ id, action }) => action === 'approve'
      ? adminApi.approveCompanyRegistration(id)
      : adminApi.rejectCompanyRegistration(id),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin-company-registrations'] });
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      setRegistrationMessage(variables.action === 'approve' ? 'Đã duyệt và kích hoạt doanh nghiệp.' : 'Đã từ chối đăng ký doanh nghiệp.');
    },
    onError: (err) => setRegistrationMessage(err.message || 'Không thể xử lý đăng ký doanh nghiệp.'),
  });

  const saveMutation = useMutation({
    mutationFn: () => editingTenant
      ? adminApi.updateTenant(editingTenant.id, form)
      : adminApi.createTenant(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      setDialogOpen(false);
      setError('');
    },
    onError: (err) => setError(err.message || 'Không thể lưu tenant.'),
  });

  const actionMutation = useMutation({
    mutationFn: ({ action, tenant }) => {
      if (action === 'delete') return adminApi.deleteTenant(tenant.id);
      if (action === 'suspend') return adminApi.suspendTenant(tenant.id);
      return adminApi.activateTenant(tenant.id);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-tenants'] }),
    onError: (err) => setError(err.message || 'Thao tác tenant thất bại.'),
  });

  const createAdminMutation = useMutation({
    mutationFn: () => adminApi.createHrAdmin({
      email: adminForm.email.trim(),
      password: adminForm.password,
      tenantId: adminDialogTenant.id,
    }),
    onSuccess: (account) => {
      setCreatedAdmin(account);
      setAdminForm(emptyAdminForm);
      setAdminError('');
    },
    onError: (err) => setAdminError(err.message || 'Không thể tạo tài khoản HR_ADMIN.'),
  });

  const openCreate = () => {
    setEditingTenant(null);
    setForm(emptyForm);
    setError('');
    setDialogOpen(true);
  };

  const openEdit = (tenant) => {
    setEditingTenant(tenant);
    setForm({ name: tenant.name || '', contactEmail: tenant.contactEmail || '', taxCode: tenant.taxCode || '', status: tenant.status || 'ACTIVE' });
    setError('');
    setDialogOpen(true);
  };

  const save = () => {
    if (!form.name.trim()) {
      setError('Tên tenant không được để trống.');
      return;
    }
    saveMutation.mutate();
  };

  const handleDelete = (tenant) => {
    if (window.confirm(`Xóa tenant “${tenant.name}”? Thao tác này có thể bị từ chối nếu tenant còn dữ liệu liên quan.`)) {
      actionMutation.mutate({ action: 'delete', tenant });
    }
  };

  const openAdminDialog = (tenant) => {
    setAdminDialogTenant(tenant);
    setAdminForm(emptyAdminForm);
    setAdminError('');
    setCreatedAdmin(null);
  };

  const closeAdminDialog = () => {
    if (createAdminMutation.isPending) return;
    setAdminDialogTenant(null);
    setCreatedAdmin(null);
    setAdminError('');
  };

  const submitAdmin = (event) => {
    event.preventDefault();
    setAdminError('');
    setCreatedAdmin(null);
    if (adminForm.password !== adminForm.confirmPassword) {
      setAdminError('Mật khẩu xác nhận không khớp.');
      return;
    }
    if (!passwordPattern.test(adminForm.password)) {
      setAdminError('Mật khẩu cần ít nhất 8 ký tự, gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&).');
      return;
    }
    createAdminMutation.mutate();
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', px: 2 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Quản lý tenant</Typography>
          <Typography color="text.secondary">Tạo tenant, cập nhật thông tin và quản lý trạng thái hoạt động.</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>Tạo tenant</Button>
      </Stack>

      {error && !dialogOpen && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {registrationMessage && <Alert severity="info" sx={{ mb: 2 }} onClose={() => setRegistrationMessage('')}>{registrationMessage}</Alert>}
      <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>Doanh nghiệp chờ duyệt</Typography>
      {registrationsError && <Alert severity="error" sx={{ mb: 2 }}>Không tải được đăng ký doanh nghiệp.</Alert>}
      <TableContainer component={Paper} variant="outlined" sx={{ mb: 4 }}>
        <Table size="small">
          <TableHead><TableRow>
            <TableCell>Doanh nghiệp</TableCell><TableCell>Người đăng ký</TableCell>
            <TableCell>Email liên hệ</TableCell><TableCell>Mã số thuế</TableCell>
            <TableCell>Ngày đăng ký</TableCell><TableCell align="right">Thao tác</TableCell>
          </TableRow></TableHead>
          <TableBody>
            {registrations.map((registration) => (
              <TableRow key={registration.id}>
                <TableCell>{registration.companyName}</TableCell>
                <TableCell>{registration.fullName}<br />{registration.email}</TableCell>
                <TableCell>{registration.contactEmail}</TableCell>
                <TableCell>{registration.taxCode}</TableCell>
                <TableCell>{registration.createdAt ? new Date(registration.createdAt).toLocaleString('vi-VN') : '—'}</TableCell>
                <TableCell align="right">
                  <Stack direction="row" justifyContent="flex-end" spacing={1}>
                    <Button size="small" variant="contained" disabled={registrationMutation.isPending}
                      onClick={() => registrationMutation.mutate({ id: registration.id, action: 'approve' })}>Duyệt</Button>
                    <Button size="small" color="error" disabled={registrationMutation.isPending}
                      onClick={() => registrationMutation.mutate({ id: registration.id, action: 'reject' })}>Từ chối</Button>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
            {registrations.length === 0 && <TableRow><TableCell colSpan={6} align="center">Không có doanh nghiệp chờ duyệt.</TableCell></TableRow>}
          </TableBody>
        </Table>
      </TableContainer>
      <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>Tenant đang quản lý</Typography>
      {isError && <Alert severity="error" sx={{ mb: 2 }}>Không tải được danh sách tenant. Kiểm tra quyền SYS_ADMIN và kết nối IAM.</Alert>}
      {isLoading ? <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}><CircularProgress /></Box> : (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Tên tenant</TableCell>
                <TableCell>Email liên hệ</TableCell>
                <TableCell>Mã số thuế</TableCell>
                <TableCell>Tenant ID</TableCell>
                <TableCell>Trạng thái</TableCell>
                <TableCell>Ngày tạo</TableCell>
                <TableCell align="right">Thao tác</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tenants.map((tenant) => (
                <TableRow key={tenant.id} hover>
                  <TableCell sx={{ fontWeight: 600 }}>{tenant.name}</TableCell>
                  <TableCell>{tenant.contactEmail || '—'}</TableCell>
                  <TableCell>{tenant.taxCode || '—'}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>{tenant.id}</TableCell>
                  <TableCell><Chip size="small" label={tenant.status} color={tenant.status === 'ACTIVE' ? 'success' : 'default'} /></TableCell>
                  <TableCell>{tenant.createdAt ? new Date(tenant.createdAt).toLocaleString('vi-VN') : '—'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title={tenant.status === 'ACTIVE' ? 'Thêm tài khoản HR_ADMIN' : 'Chỉ tenant ACTIVE mới thêm được HR_ADMIN'}>
                      <span>
                        <IconButton aria-label={`Thêm HR_ADMIN cho ${tenant.name}`} color="primary" disabled={tenant.status !== 'ACTIVE'} onClick={() => openAdminDialog(tenant)}>
                          <AddIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="Sửa"><IconButton aria-label="Sửa tenant" onClick={() => openEdit(tenant)}><EditIcon /></IconButton></Tooltip>
                    {tenant.status === 'ACTIVE' ? (
                      <Tooltip title="Tạm ngưng"><IconButton aria-label="Tạm ngưng tenant" color="warning" disabled={actionMutation.isPending} onClick={() => actionMutation.mutate({ action: 'suspend', tenant })}><BlockIcon /></IconButton></Tooltip>
                    ) : (
                      <Tooltip title="Kích hoạt"><IconButton aria-label="Kích hoạt tenant" color="success" disabled={actionMutation.isPending} onClick={() => actionMutation.mutate({ action: 'activate', tenant })}><CheckCircleOutlineIcon /></IconButton></Tooltip>
                    )}
                    <Tooltip title="Xóa"><IconButton aria-label="Xóa tenant" color="error" disabled={actionMutation.isPending} onClick={() => handleDelete(tenant)}><DeleteOutlineIcon /></IconButton></Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {tenants.length === 0 && <TableRow><TableCell colSpan={7} align="center">Chưa có tenant nào.</TableCell></TableRow>}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => !saveMutation.isPending && setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingTenant ? 'Cập nhật tenant' : 'Tạo tenant'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField autoFocus required label="Tên tenant" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Email liên hệ" type="email" value={form.contactEmail} onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} />
            <TextField label="Mã số thuế" value={form.taxCode} onChange={(e) => setForm({ ...form, taxCode: e.target.value })} />
            {editingTenant && <TextField select SelectProps={{ native: true }} label="Trạng thái" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <option value="ACTIVE">ACTIVE</option><option value="SUSPENDED">SUSPENDED</option>
            </TextField>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} disabled={saveMutation.isPending}>Hủy</Button>
          <Button variant="contained" onClick={save} disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Đang lưu…' : 'Lưu'}</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(adminDialogTenant)} onClose={closeAdminDialog} fullWidth maxWidth="sm">
        <DialogTitle>Thêm tài khoản HR_ADMIN</DialogTitle>
        <DialogContent>
          {createdAdmin ? (
            <Alert severity="success" sx={{ mt: 1 }}>
              Đã tạo <b>{createdAdmin.email}</b> với quyền <b>{createdAdmin.role}</b> trong tenant <b>{adminDialogTenant?.name}</b>. Tài khoản đã hoạt động và có thể đăng nhập bằng mật khẩu vừa tạo.
            </Alert>
          ) : (
            <Stack component="form" id="create-tenant-admin-form" spacing={2} sx={{ pt: 1 }} onSubmit={submitAdmin}>
              {adminError && <Alert severity="error">{adminError}</Alert>}
              <TextField label="Tenant" value={adminDialogTenant?.name || ''} fullWidth disabled />
              <TextField label="Email HR_ADMIN" type="email" autoComplete="email" required fullWidth value={adminForm.email} onChange={(event) => setAdminForm({ ...adminForm, email: event.target.value })} />
              <TextField label="Mật khẩu ban đầu" type="password" autoComplete="new-password" required fullWidth value={adminForm.password} onChange={(event) => setAdminForm({ ...adminForm, password: event.target.value })} helperText="Tối thiểu 8 ký tự, gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&)." />
              <TextField label="Nhập lại mật khẩu" type="password" autoComplete="new-password" required fullWidth value={adminForm.confirmPassword} onChange={(event) => setAdminForm({ ...adminForm, confirmPassword: event.target.value })} />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          {createdAdmin ? (
            <Button onClick={closeAdminDialog}>Đóng</Button>
          ) : (
            <>
              <Button onClick={closeAdminDialog} disabled={createAdminMutation.isPending}>Hủy</Button>
              <Button type="submit" form="create-tenant-admin-form" variant="contained" disabled={createAdminMutation.isPending}>
                {createAdminMutation.isPending ? 'Đang tạo…' : 'Tạo tài khoản'}
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}
