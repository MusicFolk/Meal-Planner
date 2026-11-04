import RefreshIcon from '@mui/icons-material/Refresh';
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { Questionnaire, QuestionnaireSaveResponse } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { useMessage } from '../../context/MessageContext';
import { formatDate, formatDecimal, formatFactor, formatInt } from '../../lib/format';
import { activityLabels, bmiCategoryLabels, genderLabels, goalLabels } from '../../lib/labels';

const BMI_SCALE = [
  { label: '< 18,5', category: 'UNDERWEIGHT' },
  { label: '18,5–24,9', category: 'NORMAL' },
  { label: '25–29,9', category: 'OVERWEIGHT' },
  { label: '≥ 30', category: 'OBESE' },
];

/** KMI su kategorija ir paros kalorijų poreikis su skaičiavimo paaiškinimu (FR-04–FR-07, NI-04). */
export function MetricsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const saved = location.state as QuestionnaireSaveResponse | null;
  const [q, setQ] = useState<Questionnaire | null>(saved?.questionnaire ?? null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!saved) {
      api
        .get<Questionnaire | undefined>('/api/questionnaire')
        .then((res) => (res ? setQ(res) : navigate('/anketa', { replace: true })))
        .catch((e: ApiError) => showMessage('error', e.message));
    }
  }, [saved, navigate, showMessage]);

  if (!q) return <Loading />;

  const e = q.metrics.explanation;
  const bmrFormula = `10 × ${formatDecimal(q.weightKg)} + 6,25 × ${q.heightCm} − 5 × ${q.age} ${
    q.gender === 'MALE' ? '+ 5' : '− 161'
  }`;

  const regenerate = async () => {
    setBusy(true);
    try {
      await api.post('/api/meal-plans/generate');
      showMessage('success', 'Savaitės mitybos planas sėkmingai sugeneruotas.');
      navigate('/planas');
    } catch (err) {
      showMessage('warning', err instanceof ApiError ? err.message : 'Plano sudaryti nepavyko.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle title="Jūsų rodikliai" back="/anketa" />
      <Stack spacing={2}>
        {saved?.firstTime && (
          <Alert severity="success">Anketa išsaugota. Apskaičiuotas KMI ir paros kalorijų poreikis.</Alert>
        )}
        {saved && !saved.firstTime && (
          <>
            <Alert severity="success">Anketa išsaugota {formatDate(q.updatedAt)}.</Alert>
            <Alert severity="info">Duomenys atnaujinti. Galite perkurti mitybos planą.</Alert>
          </>
        )}
        {q.chronicDisease && (
          <Alert severity="warning">Nurodėte lėtinę ligą. Rekomenduojame kreiptis į dietologą.</Alert>
        )}
        {e.safeLimitApplied && (
          <Alert severity="warning">
            Apskaičiuotas poreikis ({formatInt(e.calculatedCalories)} kcal) mažesnis už saugią ribą, todėl planui
            naudojama {formatInt(e.safeMinimum)} kcal.
          </Alert>
        )}

        <Card>
          <CardContent>
            <Typography color="text.secondary">Kūno masės indeksas (KMI)</Typography>
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center', my: 1 }}>
              <Typography variant="h3" component="p" sx={{ fontWeight: 700 }}>
                {formatDecimal(q.metrics.bmi)}
              </Typography>
              <Chip color={q.metrics.bmiCategory === 'NORMAL' ? 'success' : 'warning'} label={bmiCategoryLabels[q.metrics.bmiCategory]} />
            </Stack>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 0.5 }}>
              {BMI_SCALE.map((s) => (
                <Box key={s.category}>
                  <Box
                    sx={{
                      height: 8,
                      borderRadius: 4,
                      bgcolor: s.category === q.metrics.bmiCategory ? 'text.primary' : 'action.disabledBackground',
                    }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {s.label}
                  </Typography>
                </Box>
              ))}
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              KMI = svoris (kg) / ūgis (m)² = {formatDecimal(q.weightKg)} / {formatDecimal(q.heightCm / 100).replace(/,0$/, '')}²
            </Typography>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography color="text.secondary">Paros kalorijų poreikis</Typography>
            <Typography variant="h3" component="p" sx={{ fontWeight: 700, my: 1 }}>
              {formatInt(q.metrics.dailyCalories)} kcal
            </Typography>
            <Typography sx={{ fontWeight: 600, mb: 1 }}>Kaip apskaičiuota (Mifflin–St Jeor)</Typography>
            <ExplanationRow
              title="BMR (bazinė medžiagų apykaita)"
              detail={`${bmrFormula} (${genderLabels[q.gender].toLowerCase()})`}
              value={`${formatInt(e.bmr)} kcal`}
            />
            <Divider />
            <ExplanationRow
              title="Aktyvumo koeficientas"
              detail={`${activityLabels[q.activityLevel].name} × ${formatFactor(e.activityFactor)}`}
              value={`${formatInt(e.afterActivity)} kcal`}
            />
            <Divider />
            <ExplanationRow
              title="Tikslo korekcija"
              detail={`${goalLabels[q.goal]} × ${formatFactor(e.goalFactor)}`}
              value={`${formatInt(e.calculatedCalories)} kcal`}
            />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Saugi riba: ne mažiau kaip {formatInt(e.safeMinimum)} kcal ({q.gender === 'MALE' ? 'vyrams' : 'moterims'}).
            </Typography>
          </CardContent>
        </Card>
      </Stack>

      <PrimaryAction>
        {saved && !saved.firstTime ? (
          <>
            <Button variant="outlined" size="large" onClick={() => navigate('/planas')}>
              Vėliau
            </Button>
            <Button variant="contained" size="large" startIcon={<RefreshIcon />} onClick={regenerate} disabled={busy}>
              Perkurti planą
            </Button>
          </>
        ) : (
          <Button variant="contained" size="large" onClick={() => navigate('/planas')}>
            Į mitybos planą
          </Button>
        )}
      </PrimaryAction>
    </Box>
  );
}

function ExplanationRow({ title, detail, value }: { title: string; detail: string; value: string }) {
  return (
    <Stack direction="row" sx={{ justifyContent: 'space-between', py: 1, gap: 2 }}>
      <Box>
        <Typography sx={{ fontWeight: 500 }}>{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {detail}
        </Typography>
      </Box>
      <Typography sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>{value}</Typography>
    </Stack>
  );
}
