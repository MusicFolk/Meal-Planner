import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material';
import { useState, type FormEvent } from 'react';
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router';
import { ApiError } from '../../api/client';
import { homePath, useAuth } from '../../context/AuthContext';
import { validateEmail, type Errors } from '../../lib/validation';
import { AuthShell } from './AuthShell';

/** Prisijungimo forma, bendra visiems vaidmenims (UI-05, FR-30, FR-31). */
export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const notice = (location.state as { notice?: string } | null)?.notice;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Errors>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homePath(user)} replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const fieldErrors: Errors = {
      email: validateEmail(email),
      password: password ? undefined : 'Įveskite slaptažodį.',
    };
    setErrors(fieldErrors);
    if (fieldErrors.email || fieldErrors.password) return;
    setBusy(true);
    setError(null);
    try {
      const me = await login(email.trim(), password);
      navigate(homePath(me), { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Įvyko klaida. Bandykite vėliau.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Prisijungimas">
      <Stack component="form" spacing={2} onSubmit={submit} noValidate>
        {notice && <Alert severity="success">{notice}</Alert>}
        {error && (
          <Alert severity={error.includes('užrakintas') ? 'warning' : 'error'} role="alert">
            {error}
          </Alert>
        )}
        <TextField
          label="El. paštas"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={!!errors.email}
          helperText={errors.email}
          slotProps={{ htmlInput: { maxLength: 100 } }}
        />
        <TextField
          label="Slaptažodis"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={!!errors.password}
          helperText={errors.password}
          slotProps={{ htmlInput: { maxLength: 64 } }}
        />
        <Button type="submit" variant="contained" size="large" disabled={busy}>
          Prisijungti
        </Button>
        <Typography variant="body2" sx={{ textAlign: 'center' }}>
          Neturite paskyros?{' '}
          <Link component={RouterLink} to="/registruotis">
            Registruotis
          </Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
