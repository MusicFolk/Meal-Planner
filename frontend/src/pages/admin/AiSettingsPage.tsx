import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { Box, Button, Card, CardContent, IconButton, InputAdornment, Stack, TextField, Tooltip, Typography } from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { api, ApiError } from '../../api/client';
import type { AiConfiguration } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { useMessage } from '../../context/MessageContext';
import { formatDateTime } from '../../lib/format';
import { hasErrors, type Errors } from '../../lib/validation';

const MODEL_HINT =
  'Palaikomi tik Google Gemini modeliai. Įrašykite modelio pavadinimą tiksliai taip, kaip jis nurodytas ' +
  'Google AI Studio, pvz., gemini-3.5-flash-lite.';

/** DI parametrai: DI modelis ir dienos generavimų limitas vienam klientui (NI-20, FR-41, UI-13). */
export function AiSettingsPage() {
  const showMessage = useMessage();
  const [config, setConfig] = useState<AiConfiguration | null>(null);
  const [form, setForm] = useState({ model: '', dailyGenerationLimit: '' });
  const [errors, setErrors] = useState<Errors>({});
  const [busy, setBusy] = useState(false);

  const apply = (c: AiConfiguration) => {
    setConfig(c);
    setForm({ model: c.model, dailyGenerationLimit: String(c.dailyGenerationLimit) });
  };

  useEffect(() => {
    api
      .get<AiConfiguration>('/api/admin/ai-configuration')
      .then(apply)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const model = form.model.trim();
    const limit = Number(form.dailyGenerationLimit);
    const fieldErrors: Errors = {
      model: !model
        ? 'Įveskite DI modelio pavadinimą.'
        : !/^[A-Za-z0-9._-]+$/.test(model)
          ? 'Modelio pavadinime gali būti tik raidės, skaitmenys, taškas ir brūkšnelis.'
          : undefined,
      dailyGenerationLimit:
        !Number.isInteger(limit) || limit < 1 || limit > 20 ? 'Limitas turi būti nuo 1 iki 20.' : undefined,
    };
    setErrors(fieldErrors);
    if (hasErrors(fieldErrors)) return;
    setBusy(true);
    try {
      apply(await api.put<AiConfiguration>('/api/admin/ai-configuration', { model, dailyGenerationLimit: limit }));
      showMessage('success', 'DI modelis ir generavimų limitas išsaugoti.');
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        showMessage('error', err.message);
      }
    } finally {
      setBusy(false);
    }
  };

  if (!config) return <Loading />;

  return (
    <Box component="form" onSubmit={submit} noValidate sx={{ maxWidth: 640 }}>
      <PageTitle title="DI parametrai" />
      <Card>
        <CardContent>
          <Stack spacing={2}>
            <TextField
              label="DI modelis"
              required
              value={form.model}
              onChange={(e) => setForm({ ...form, model: e.target.value })}
              error={!!errors.model}
              helperText={errors.model ?? 'Google Gemini modelio pavadinimas'}
              slotProps={{
                htmlInput: { maxLength: 100 },
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <Tooltip title={MODEL_HINT}>
                        <IconButton edge="end" aria-label={MODEL_HINT}>
                          <InfoOutlinedIcon />
                        </IconButton>
                      </Tooltip>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <TextField
              label="Dienos generavimų limitas"
              type="number"
              required
              value={form.dailyGenerationLimit}
              onChange={(e) => setForm({ ...form, dailyGenerationLimit: e.target.value })}
              error={!!errors.dailyGenerationLimit}
              helperText={errors.dailyGenerationLimit ?? 'Vienam klientui per dieną, nuo 1 iki 20'}
              slotProps={{ htmlInput: { min: 1, max: 20, step: 1 } }}
            />
            <Typography variant="body2" color="text.secondary">
              {config.changedAt
                ? `Paskutinį kartą keitė ${config.changedBy ?? 'ištrintas administratorius'}, ${formatDateTime(config.changedAt)}.`
                : 'Parametrai dar nekeisti – naudojamas aplinkos kintamuoju GEMINI_MODEL nurodytas modelis.'}{' '}
              Nauji parametrai taikomi nuo kito plano generavimo.
            </Typography>
          </Stack>
        </CardContent>
      </Card>
      <PrimaryAction>
        <Button type="submit" variant="contained" size="large" disabled={busy}>
          Išsaugoti
        </Button>
      </PrimaryAction>
    </Box>
  );
}
