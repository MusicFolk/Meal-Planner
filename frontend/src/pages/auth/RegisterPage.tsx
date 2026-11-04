import { Alert, Button, Checkbox, FormControlLabel, FormHelperText, Link, Stack, TextField, Typography } from '@mui/material';
import { useState, type FormEvent } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import {
  hasErrors,
  mapServerErrors,
  PASSWORD_MESSAGE,
  PASSWORD_PATTERN,
  validateEmail,
  type Errors,
} from '../../lib/validation';
import { AuthShell } from './AuthShell';

/** Kliento registracija (UI-06, FR-28, FR-29). */
export function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: '', email: '', password: '', passwordRepeat: '' });
  const [consent, setConsent] = useState(false);
  const [errors, setErrors] = useState<Errors>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value });

  const validate = (): Errors => ({
    firstName: !form.firstName.trim()
      ? 'Įveskite vardą.'
      : form.firstName.length > 50
        ? 'Vardas negali būti ilgesnis nei 50 simbolių.'
        : undefined,
    email: validateEmail(form.email),
    password: PASSWORD_PATTERN.test(form.password) ? undefined : PASSWORD_MESSAGE,
    passwordRepeat: form.password === form.passwordRepeat ? undefined : 'Slaptažodžiai nesutampa.',
    healthDataConsent: consent ? undefined : 'Būtinas sutikimas tvarkyti sveikatos duomenis.',
  });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const fieldErrors = validate();
    setErrors(fieldErrors);
    if (hasErrors(fieldErrors)) return;
    setBusy(true);
    setError(null);
    try {
      const res = await api.post<{ message: string }>('/api/auth/register', {
        ...form,
        email: form.email.trim(),
        healthDataConsent: consent,
      });
      navigate('/prisijungti', { state: { notice: res.message } });
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(mapServerErrors(err.fieldErrors, { passwordRepeatMatching: 'passwordRepeat' }));
        setError(err.message);
      } else {
        setError('Įvyko klaida. Bandykite vėliau.');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell title="Registracija">
      <Stack component="form" spacing={2} onSubmit={submit} noValidate>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          label="Vardas"
          required
          value={form.firstName}
          onChange={set('firstName')}
          error={!!errors.firstName}
          helperText={errors.firstName ?? `${form.firstName.length}/50`}
          slotProps={{ htmlInput: { maxLength: 50 } }}
        />
        <TextField
          label="El. paštas"
          type="email"
          autoComplete="email"
          required
          value={form.email}
          onChange={set('email')}
          error={!!errors.email}
          helperText={errors.email}
          slotProps={{ htmlInput: { maxLength: 100 } }}
        />
        <TextField
          label="Slaptažodis"
          type="password"
          autoComplete="new-password"
          required
          value={form.password}
          onChange={set('password')}
          error={!!errors.password}
          helperText={errors.password ?? '8–64 simboliai, bent viena raidė ir vienas skaitmuo'}
          slotProps={{ htmlInput: { maxLength: 64 } }}
        />
        <TextField
          label="Pakartokite slaptažodį"
          type="password"
          autoComplete="new-password"
          required
          value={form.passwordRepeat}
          onChange={set('passwordRepeat')}
          error={!!errors.passwordRepeat}
          helperText={errors.passwordRepeat}
          slotProps={{ htmlInput: { maxLength: 64 } }}
        />
        <div>
          <FormControlLabel
            control={<Checkbox checked={consent} onChange={(e) => setConsent(e.target.checked)} />}
            label="Sutinku, kad mano sveikatos duomenys būtų tvarkomi mitybos planui sudaryti *"
          />
          {errors.healthDataConsent && <FormHelperText error>{errors.healthDataConsent}</FormHelperText>}
        </div>
        <Button type="submit" variant="contained" size="large" disabled={busy}>
          Registruotis
        </Button>
        <Typography variant="body2" sx={{ textAlign: 'center' }}>
          Jau turite paskyrą?{' '}
          <Link component={RouterLink} to="/prisijungti">
            Prisijungti
          </Link>
        </Typography>
      </Stack>
    </AuthShell>
  );
}
