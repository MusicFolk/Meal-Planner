import {
  Button,
  Card,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { Role, User } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { formatDate } from '../../lib/format';
import { roleLabels } from '../../lib/labels';

type StatusFilter = 'ACTIVE' | 'BLOCKED' | '';

/** Naudotojų sąrašas su filtrais, blokavimas ir atblokavimas (AS-1, FR-39, FR-40). */
export function UsersPage() {
  const { user: me } = useAuth();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [role, setRole] = useState<Role | ''>('');
  const [status, setStatus] = useState<StatusFilter>('');
  const [users, setUsers] = useState<User[] | null>(null);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (role) params.set('role', role);
    if (status) params.set('status', status);
    api
      .get<User[]>(`/api/admin/users?${params}`)
      .then(setUsers)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [role, status, showMessage]);

  useEffect(load, [load]);

  const setBlocked = async (user: User, blocked: boolean) => {
    try {
      await api.post(`/api/admin/users/${user.id}/${blocked ? 'block' : 'unblock'}`);
      showMessage('success', blocked ? 'Naudotojas užblokuotas.' : 'Naudotojas atblokuotas.');
      load();
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Veiksmo atlikti nepavyko.');
    }
  };

  return (
    <>
      <PageTitle
        title="Naudotojai"
        action={
          <Button variant="contained" onClick={() => navigate('/admin/dietologai/naujas')}>
            Sukurti dietologo paskyrą
          </Button>
        }
      />
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="role-filter">Vaidmuo</InputLabel>
          <Select labelId="role-filter" label="Vaidmuo" value={role} onChange={(e) => setRole(e.target.value as Role | '')}>
            <MenuItem value="">Visi</MenuItem>
            {(Object.keys(roleLabels) as Role[]).map((r) => (
              <MenuItem key={r} value={r}>
                {roleLabels[r]}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="status-filter">Būsena</InputLabel>
          <Select labelId="status-filter" label="Būsena" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
            <MenuItem value="">Visos</MenuItem>
            <MenuItem value="ACTIVE">Aktyvi</MenuItem>
            <MenuItem value="BLOCKED">Užblokuota</MenuItem>
          </Select>
        </FormControl>
      </Stack>
      {users === null ? (
        <Loading />
      ) : users.length === 0 ? (
        <Typography color="text.secondary">Naudotojų nerasta.</Typography>
      ) : (
        <TableContainer component={Card}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Vardas, pavardė</TableCell>
                <TableCell>El. paštas</TableCell>
                <TableCell>Vaidmuo</TableCell>
                <TableCell>Būsena</TableCell>
                <TableCell>Sukurta</TableCell>
                <TableCell align="right">Veiksmai</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>
                    {u.firstName} {u.lastName}
                  </TableCell>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>{roleLabels[u.role]}</TableCell>
                  <TableCell>
                    <Chip size="small" color={u.blocked ? 'error' : 'success'} variant="outlined" label={u.blocked ? 'Užblokuota' : 'Aktyvi'} />
                  </TableCell>
                  <TableCell>{formatDate(u.createdAt)}</TableCell>
                  <TableCell align="right">
                    {u.blocked ? (
                      <Button size="small" onClick={() => setBlocked(u, false)}>
                        Atblokuoti
                      </Button>
                    ) : (
                      <Button size="small" color="error" disabled={u.id === me?.id} onClick={() => setBlocked(u, true)}>
                        Blokuoti
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
