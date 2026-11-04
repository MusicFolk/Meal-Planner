import { Alert, Button, Stack, TextField } from '@mui/material';
import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { Me } from '../../api/types';
import { homePath, useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { hasErrors, mapServerErrors, PASSWORD_MESSAGE, PASSWORD_PATTERN, type Errors } from '../../lib/validation';
import { AuthShell } from './AuthShell';

/** Dietologas, prisijungęs pirmą kartą, privalo pakeisti laikiną slaptažodį (FR-38). */
export function ChangePasswordPage() {
  const { user, setUser, loading } = useAuth();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', newPasswordRepeat: '' });
  const [errors, setErrors] = useState<Errors>({});
  const [error, setError] = useState<string | null>(null);

  if (!loading && !user) return <Navigate to="/prisijungti" replace />;

  const set = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const fieldErrors: Errors = {
      currentPassword: form.currentPassword ? undefined : 'Įveskite dabartinį slaptažodį.',
      newPassword: PASSWORD_PATTERN.test(form.newPassword) ? undefined : PASSWORD_MESSAGE,
      newPasswordRepeat: form.newPassword === form.newPasswordRepeat ? undefined : 'Slaptažodžiai nesutampa.',
    };
    setErrors(fieldErrors);
    if (hasErrors(fieldErrors)) return;
    try {
      const me = await api.post<Me>('/api/auth/change-password', form);
      setUser(me);
      showMessage('success', 'Slaptažodis pakeistas.');
      navigate(homePath(me), { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(mapServerErrors(err.fieldErrors, { newPasswordRepeatMatching: 'newPasswordRepeat' }));
        setError(err.message);
      }
    }
  };

  return (
    <AuthShell title="Slaptažodžio keitimas">
      <Stack component="form" spacing={2} onSubmit={submit} noValidate>
        {user?.mustChangePassword && (
          <Alert severity="info">Prisijungėte su laikinu slaptažodžiu. Prieš tęsdami, susikurkite naują.</Alert>
        )}
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          label="Dabartinis (laikinas) slaptažodis"
          type="password"
          required
          value={form.currentPassword}
          onChange={set('currentPassword')}
          error={!!errors.currentPassword}
          helperText={errors.currentPassword}
        />
        <TextField
          label="Naujas slaptažodis"
          type="password"
          autoComplete="new-password"
          required
          value={form.newPassword}
          onChange={set('newPassword')}
          error={!!errors.newPassword}
          helperText={errors.newPassword ?? '8–64 simboliai, bent viena raidė ir vienas skaitmuo'}
          slotProps={{ htmlInput: { maxLength: 64 } }}
        />
        <TextField
          label="Pakartokite naują slaptažodį"
          type="password"
          autoComplete="new-password"
          required
          value={form.newPasswordRepeat}
          onChange={set('newPasswordRepeat')}
          error={!!errors.newPasswordRepeat}
          helperText={errors.newPasswordRepeat}
          slotProps={{ htmlInput: { maxLength: 64 } }}
        />
        <Button type="submit" variant="contained" size="large">
          Pakeisti slaptažodį
        </Button>
      </Stack>
    </AuthShell>
  );
}
