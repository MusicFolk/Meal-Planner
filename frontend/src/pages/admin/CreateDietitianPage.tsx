import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { User } from '../../api/types';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { useMessage } from '../../context/MessageContext';
import { hasErrors, validateEmail, type Errors } from '../../lib/validation';

/** Dietologo paskyros kūrimas; laikinas slaptažodis parodomas vieną kartą (FR-38). */
export function CreateDietitianPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '' });
  const [errors, setErrors] = useState<Errors>({});
  const [created, setCreated] = useState<{ user: User; temporaryPassword: string } | null>(null);

  const set = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const fieldErrors: Errors = {
      firstName: !form.firstName.trim() ? 'Įveskite vardą.' : form.firstName.length > 50 ? 'Vardas negali būti ilgesnis nei 50 simbolių.' : undefined,
      lastName: !form.lastName.trim() ? 'Įveskite pavardę.' : form.lastName.length > 50 ? 'Pavardė negali būti ilgesnė nei 50 simbolių.' : undefined,
      email: validateEmail(form.email),
    };
    setErrors(fieldErrors);
    if (hasErrors(fieldErrors)) return;
    try {
      setCreated(await api.post('/api/admin/dietitians', { ...form, email: form.email.trim() }));
      showMessage('success', 'Dietologo paskyra sėkmingai sukurta.');
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        showMessage('error', err.message);
      }
    }
  };

  return (
    <Box component="form" onSubmit={submit} noValidate sx={{ maxWidth: 640 }}>
      <PageTitle title="Nauja dietologo paskyra" back="/admin" />
      <Card>
        <CardContent>
          <Stack spacing={2}>
            <TextField label="Vardas" required value={form.firstName} onChange={set('firstName')} error={!!errors.firstName} helperText={errors.firstName ?? `${form.firstName.length}/50`} slotProps={{ htmlInput: { maxLength: 50 } }} />
            <TextField label="Pavardė" required value={form.lastName} onChange={set('lastName')} error={!!errors.lastName} helperText={errors.lastName ?? `${form.lastName.length}/50`} slotProps={{ htmlInput: { maxLength: 50 } }} />
            <TextField label="El. paštas" type="email" required value={form.email} onChange={set('email')} error={!!errors.email} helperText={errors.email} slotProps={{ htmlInput: { maxLength: 100 } }} />
          </Stack>
        </CardContent>
      </Card>
      <PrimaryAction>
        <Button type="submit" variant="contained" size="large">
          Sukurti paskyrą
        </Button>
      </PrimaryAction>

      <Dialog open={created !== null}>
        <DialogTitle>Laikinas slaptažodis</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Perduokite dietologui ({created?.user.email}) šį slaptažodį. Jis rodomas tik vieną kartą – prisijungęs pirmą
            kartą dietologas privalės jį pakeisti.
          </DialogContentText>
          <Alert severity="warning" icon={false}>
            <Typography sx={{ fontFamily: 'monospace', fontSize: '1.4rem', letterSpacing: 2 }}>{created?.temporaryPassword}</Typography>
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={() => navigate('/admin')}>
            Supratau
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
