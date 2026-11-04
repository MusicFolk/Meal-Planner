import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { OrderInfo, PaymentInitiated, PaymentMethod } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PaymentMethodPicker } from '../../components/PaymentMethodPicker';
import { PrimaryAction } from '../../components/PrimaryAction';
import { useMessage } from '../../context/MessageContext';
import { formatMoney } from '../../lib/format';

const MAX_QUESTION = 500;

/** Peržiūros užsakymas su apmokėjimu per banko sąsają (UI-10, FR-23, FR-24). */
export function ReviewOrderPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [info, setInfo] = useState<OrderInfo | null>(null);
  const [question, setQuestion] = useState('');
  const [method, setMethod] = useState<PaymentMethod>('BANK_LINK');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get<OrderInfo>('/api/reviews/order-info').then(setInfo).catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  if (!info) return <Loading />;

  const submit = async () => {
    if (question.length > MAX_QUESTION) return;
    setBusy(true);
    try {
      const res = await api.post<PaymentInitiated>('/api/reviews/order', { method, question });
      navigate(res.redirectUrl);
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Įvyko klaida. Bandykite vėliau.');
      setBusy(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 640, mx: 'auto' }}>
      <PageTitle title="Dietologo peržiūra" back="/planas" />
      <Stack spacing={2}>
        {!info.canOrder && <Alert severity="warning">{info.reason}</Alert>}
        <Card>
          <CardContent>
            <Typography color="text.secondary">Peržiūros kaina</Typography>
            <Typography variant="h4" component="p" sx={{ fontWeight: 700 }}>
              {formatMoney(info.price)}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Klinikos dietologas peržiūrės jūsų aktualų mitybos planą ir anketą (be kontaktinių duomenų) ir pateiks
              komentarus. Vykti į kliniką nereikia.
            </Typography>
          </CardContent>
        </Card>
        <TextField
          label="Klausimas dietologui"
          multiline
          minRows={3}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          error={question.length > MAX_QUESTION}
          helperText={
            question.length > MAX_QUESTION
              ? 'Klausimas negali būti ilgesnis nei 500 simbolių.'
              : `Neprivalomas · ${question.length}/${MAX_QUESTION}`
          }
        />
        <PaymentMethodPicker value={method} onChange={setMethod} />
      </Stack>
      <PrimaryAction>
        <Button variant="contained" size="large" disabled={!info.canOrder || busy} onClick={submit}>
          Apmokėti {formatMoney(info.price)}
        </Button>
      </PrimaryAction>
    </Box>
  );
}
