import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import DownloadIcon from '@mui/icons-material/Download';
import LogoutIcon from '@mui/icons-material/Logout';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  List,
  ListItem,
  ListItemText,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError, download } from '../../api/client';
import type { ClientReview, FirstDayOfWeek, PaymentInitiated, PaymentMethod, Settings, Subscription, Theme } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PaymentMethodPicker } from '../../components/PaymentMethodPicker';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { formatDate, formatMoney } from '../../lib/format';
import { subscriptionStatusLabels } from '../../lib/labels';

/** Paskyros nustatymai: prenumerata, peržiūros, sąsajos nustatymai, duomenų atsisiuntimas ir ištrynimas (UI-11). */
export function AccountPage() {
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [reviews, setReviews] = useState<ClientReview[]>([]);
  const [payOpen, setPayOpen] = useState(false);
  const [method, setMethod] = useState<PaymentMethod>('CARD');
  const [deleteOpen, setDeleteOpen] = useState(false);

  const load = useCallback(() => {
    Promise.all([api.get<Subscription>('/api/subscription'), api.get<ClientReview[]>('/api/reviews')])
      .then(([s, r]) => {
        setSubscription(s);
        setReviews(r);
      })
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  useEffect(load, [load]);

  if (!subscription || !user) return <Loading />;

  const saveSettings = async (settings: Settings) => {
    try {
      const saved = await api.put<Settings>('/api/account/settings', settings);
      setUser({ ...user, ...saved });
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Nepavyko išsaugoti nustatymų.');
    }
  };

  const pay = async () => {
    try {
      const res = await api.post<PaymentInitiated>('/api/subscription/pay', { method });
      navigate(res.redirectUrl);
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Įvyko klaida. Bandykite vėliau.');
    }
  };

  const deleteAccount = async () => {
    try {
      await api.delete('/api/account');
      setUser(null);
      navigate('/prisijungti', { replace: true, state: { notice: 'Jūsų paskyra ir asmens duomenys ištrinti.' } });
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Paskyros ištrinti nepavyko.');
    }
  };

  const statusColor = subscription.status === 'ACTIVE' ? 'success' : subscription.status === 'TRIAL' ? 'info' : 'warning';

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle title="Paskyra" subtitle={`${user.firstName} · ${user.email}`} />
      <Stack spacing={2}>
        <Card>
          <CardContent>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="h6" component="h2">
                Prenumerata
              </Typography>
              <Chip color={statusColor} label={subscriptionStatusLabels[subscription.status]} />
            </Stack>
            <Typography color="text.secondary">
              {subscription.status === 'EXPIRED' ? 'Pasibaigė' : 'Galioja iki'} {formatDate(subscription.validUntil)}
            </Typography>
            {subscription.status === 'EXPIRED' && (
              <Typography variant="body2" sx={{ mt: 1 }}>
                Prenumerata nebegalioja. Norėdami generuoti planą, atnaujinkite prenumeratą. Ankstesni planai lieka matomi.
              </Typography>
            )}
            {subscription.canPay && (
              <Button variant="contained" sx={{ mt: 2 }} onClick={() => setPayOpen(true)}>
                Apmokėti prenumeratą – {formatMoney(subscription.price)} / 30 d.
              </Button>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" component="h2">
              Dietologo peržiūros
            </Typography>
            {reviews.length === 0 ? (
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                Peržiūrų neužsakyta.
              </Typography>
            ) : (
              <List dense>
                {reviews.map((r) => (
                  <ListItem key={r.id} disableGutters secondaryAction={<ReviewStatusChip status={r.status} />}>
                    <ListItemText primary={`Užsakyta ${formatDate(r.submittedAt)}`} secondary={r.question ?? 'Be klausimo'} />
                  </ListItem>
                ))}
              </List>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom>
              Sąsajos nustatymai
            </Typography>
            <Typography variant="subtitle2" gutterBottom id="theme-label">
              Tema
            </Typography>
            <ToggleButtonGroup
              exclusive
              aria-labelledby="theme-label"
              value={user.theme}
              onChange={(_, theme: Theme | null) => theme && saveSettings({ theme, firstDayOfWeek: user.firstDayOfWeek })}
              sx={{ mb: 2, flexWrap: 'wrap' }}
            >
              <ToggleButton value="LIGHT">Šviesi</ToggleButton>
              <ToggleButton value="DARK">Tamsi</ToggleButton>
              <ToggleButton value="SYSTEM">Pagal įrenginį</ToggleButton>
            </ToggleButtonGroup>
            <Typography variant="subtitle2" gutterBottom id="first-day-label">
              Savaitės pirmoji diena
            </Typography>
            <ToggleButtonGroup
              exclusive
              aria-labelledby="first-day-label"
              value={user.firstDayOfWeek}
              onChange={(_, day: FirstDayOfWeek | null) => day && saveSettings({ theme: user.theme, firstDayOfWeek: day })}
            >
              <ToggleButton value="MONDAY">Pirmadienis</ToggleButton>
              <ToggleButton value="SUNDAY">Sekmadienis</ToggleButton>
            </ToggleButtonGroup>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom>
              Mano duomenys
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Galite atsisiųsti visus savo duomenis (anketą, planus, komentarus, mokėjimus) JSON formatu arba visam laikui
              ištrinti paskyrą.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={() =>
                  download('/api/account/export', `mealplanner_duomenys_${formatDate(new Date())}.json`).catch((e: Error) =>
                    showMessage('error', e.message),
                  )
                }
              >
                Atsisiųsti duomenis
              </Button>
              <Button variant="outlined" color="error" startIcon={<DeleteOutlineIcon />} onClick={() => setDeleteOpen(true)}>
                Ištrinti paskyrą
              </Button>
            </Stack>
          </CardContent>
        </Card>

        <Button
          variant="text"
          startIcon={<LogoutIcon />}
          onClick={async () => {
            await logout();
            navigate('/prisijungti');
          }}
        >
          Atsijungti
        </Button>
      </Stack>

      <Dialog open={payOpen} onClose={() => setPayOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Prenumeratos apmokėjimas</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Prenumerata {formatMoney(subscription.price)} už 30 dienų. Automatiškai nepratęsiama.
          </DialogContentText>
          <PaymentMethodPicker value={method} onChange={setMethod} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPayOpen(false)}>Atšaukti</Button>
          <Button variant="contained" onClick={pay}>
            Apmokėti
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={deleteOpen} onClose={() => setDeleteOpen(false)}>
        <DialogTitle>Paskyros ištrynimas</DialogTitle>
        <DialogContent>
          <DialogContentText>Ar tikrai norite ištrinti paskyrą? Veiksmo atšaukti negalima.</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteOpen(false)} autoFocus>
            Atšaukti
          </Button>
          <Button variant="contained" color="error" onClick={deleteAccount}>
            Ištrinti
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
