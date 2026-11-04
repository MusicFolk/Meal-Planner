import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import { Alert, Box, Button, Card, CardContent, Divider, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { Payment } from '../../api/types';
import { Loading } from '../../components/Loading';
import { useMessage } from '../../context/MessageContext';
import { formatMoney } from '../../lib/format';
import { paymentMethodLabels } from '../../lib/labels';

/**
 * Banko mokėjimų sąsajos imitacija (NFR-09): atskiras langas su veiksmais „Apmokėti“ ir „Atmesti“,
 * kuris grąžina mokėjimo rezultatą programų serveriui per REST API.
 */
export function BankPage() {
  const { reference } = useParams();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [payment, setPayment] = useState<Payment | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get<Payment>(`/api/payments/${reference}`).then(setPayment).catch((e: ApiError) => showMessage('error', e.message));
  }, [reference, showMessage]);

  if (!payment) return <Loading />;

  const back = payment.purpose === 'REVIEW' ? '/planas' : '/paskyra';

  const decide = async (approved: boolean) => {
    setBusy(true);
    try {
      const result = await api.post<Payment>(`/api/bank/payments/${reference}/confirm`, { approved });
      if (result.status === 'PAID') {
        showMessage(
          'success',
          payment.purpose === 'REVIEW' ? 'Peržiūros užklausa sukurta. Būsena – „Laukia“.' : 'Prenumerata aktyvuota 30 dienų.',
        );
      } else {
        showMessage(
          'error',
          payment.purpose === 'REVIEW' ? 'Mokėjimas nepavyko, peržiūra neužsakyta.' : 'Mokėjimas nepavyko. Prenumerata neaktyvuota.',
        );
      }
      navigate(back, { replace: true });
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Įvyko klaida. Bandykite vėliau.');
      setBusy(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#e8eaf6', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}>
      <Card sx={{ width: '100%', maxWidth: 440 }} variant="elevation" elevation={2}>
        <Box sx={{ bgcolor: '#283593', color: '#fff', p: 2, display: 'flex', gap: 1.5, alignItems: 'center' }}>
          <AccountBalanceIcon />
          <Typography sx={{ fontWeight: 700 }}>Banko mokėjimų sąsaja</Typography>
        </Box>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Prototipe naudojama banko sąsajos imitacija – tikri pinigai nenurašomi.
          </Alert>
          {payment.status !== 'PENDING' ? (
            <Alert severity="warning">Šis mokėjimas jau apdorotas.</Alert>
          ) : (
            <Stack spacing={1.5}>
              <Row label="Gavėjas" value="Meal Planner" />
              <Row label="Paskirtis" value={payment.purpose === 'REVIEW' ? 'Dietologo plano peržiūra' : 'Prenumerata 30 dienų'} />
              <Row label="Mokėjimo būdas" value={paymentMethodLabels[payment.method]} />
              <Divider />
              <Row label="Suma" value={formatMoney(payment.amount)} bold />
            </Stack>
          )}
          <Stack direction="row" spacing={1.5} sx={{ mt: 3 }}>
            {payment.status === 'PENDING' ? (
              <>
                <Button fullWidth variant="outlined" color="error" disabled={busy} onClick={() => decide(false)}>
                  Atmesti
                </Button>
                <Button fullWidth variant="contained" disabled={busy} onClick={() => decide(true)} sx={{ bgcolor: '#283593' }}>
                  Apmokėti
                </Button>
              </>
            ) : (
              <Button fullWidth variant="contained" onClick={() => navigate(back)}>
                Grįžti
              </Button>
            )}
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
      <Typography color="text.secondary">{label}</Typography>
      <Typography sx={{ fontWeight: bold ? 700 : 500 }}>{value}</Typography>
    </Stack>
  );
}
