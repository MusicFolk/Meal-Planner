import CheckIcon from '@mui/icons-material/Check';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  FormControl,
  FormControlLabel,
  FormHelperText,
  InputAdornment,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type {
  ActivityLevel,
  Allergen,
  Gender,
  Goal,
  Product,
  Questionnaire,
  QuestionnaireSaveResponse,
} from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { useClearMessage, useMessage } from '../../context/MessageContext';
import { formatDate, formatDecimal, formatFactor } from '../../lib/format';
import { activityLabels, genderLabels, goalLabels } from '../../lib/labels';
import { hasErrors, type Errors } from '../../lib/validation';

const MAX_DISLIKED = 20;

interface FormState {
  heightCm: string;
  weightKg: string;
  age: string;
  gender: Gender | null;
  activityLevel: ActivityLevel | null;
  goal: Goal | null;
  chronicDisease: boolean | null;
  allergenIds: number[];
  dislikedProducts: Product[];
}

const EMPTY: FormState = {
  heightCm: '',
  weightKg: '',
  age: '',
  gender: null,
  activityLevel: null,
  goal: null,
  chronicDisease: null,
  allergenIds: [],
  dislikedProducts: [],
};

/** Anketa su laukų ribomis pagal 17 lentelę (UI-07, FR-01–FR-03, UI-22). */
export function QuestionnairePage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const clearMessage = useClearMessage();
  const [form, setForm] = useState<FormState>(EMPTY);
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [allergens, setAllergens] = useState<Allergen[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [errors, setErrors] = useState<Errors>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get<Questionnaire | undefined>('/api/questionnaire'),
      api.get<Allergen[]>('/api/catalog/allergens'),
      api.get<Product[]>('/api/catalog/products'),
    ])
      .then(([q, a, p]) => {
        setAllergens(a);
        setProducts(p);
        if (q) {
          setUpdatedAt(q.updatedAt);
          setForm({
            heightCm: String(q.heightCm),
            weightKg: formatDecimal(q.weightKg),
            age: String(q.age),
            gender: q.gender,
            activityLevel: q.activityLevel,
            goal: q.goal,
            chronicDisease: q.chronicDisease,
            allergenIds: q.allergens.map((x) => x.id),
            dislikedProducts: q.dislikedProducts,
          });
        }
      })
      .catch((e: ApiError) => showMessage('error', e.message))
      .finally(() => setLoading(false));
  }, [showMessage]);

  const validate = (): Errors => {
    const height = Number(form.heightCm);
    const weight = Number(form.weightKg.replace(',', '.'));
    const age = Number(form.age);
    return {
      heightCm:
        !/^\d+$/.test(form.heightCm) || height < 100 || height > 250 ? 'Ūgis turi būti nuo 100 iki 250 cm.' : undefined,
      weightKg: !/^\d{1,3}([.,]\d)?$/.test(form.weightKg.trim())
        ? form.weightKg.trim() === ''
          ? 'Svoris turi būti nuo 30 iki 300 kg.'
          : 'Svoris nurodomas 0,1 kg tikslumu.'
        : weight < 30 || weight > 300
          ? 'Svoris turi būti nuo 30 iki 300 kg.'
          : undefined,
      age: !/^\d+$/.test(form.age) || age < 18 || age > 100 ? 'Amžius turi būti nuo 18 iki 100 metų.' : undefined,
      gender: form.gender ? undefined : 'Pasirinkite lytį.',
      activityLevel: form.activityLevel ? undefined : 'Pasirinkite fizinio aktyvumo lygį.',
      goal: form.goal ? undefined : 'Pasirinkite tikslą.',
      chronicDisease: form.chronicDisease === null ? 'Nurodykite, ar sergate lėtine liga.' : undefined,
      dislikedProductIds:
        form.dislikedProducts.length > MAX_DISLIKED
          ? 'Galima nurodyti ne daugiau kaip 20 nemėgstamų produktų.'
          : undefined,
    };
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    clearMessage();
    const fieldErrors = validate();
    setErrors(fieldErrors);
    if (hasErrors(fieldErrors)) {
      showMessage('error', 'Patikrinkite pažymėtus laukus.');
      return;
    }
    setBusy(true);
    try {
      const res = await api.put<QuestionnaireSaveResponse>('/api/questionnaire', {
        heightCm: Number(form.heightCm),
        weightKg: Number(form.weightKg.replace(',', '.')),
        age: Number(form.age),
        gender: form.gender,
        activityLevel: form.activityLevel,
        goal: form.goal,
        chronicDisease: form.chronicDisease,
        allergenIds: form.allergenIds,
        dislikedProductIds: form.dislikedProducts.map((p) => p.id),
      });
      navigate('/anketa/rodikliai', { state: res });
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        showMessage('error', err.message);
      }
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Loading />;

  const toggleAllergen = (id: number) =>
    setForm((f) => ({
      ...f,
      allergenIds: f.allergenIds.includes(id) ? f.allergenIds.filter((x) => x !== id) : [...f.allergenIds, id],
    }));

  return (
    <Box component="form" onSubmit={submit} noValidate sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle
        title="Anketa"
        subtitle={updatedAt ? `Paskutinis pakeitimas ${formatDate(updatedAt)}` : undefined}
      />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Laukai, pažymėti *, privalomi.
      </Typography>

      <Stack spacing={2}>
        <Section title="Biometriniai duomenys">
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
            <TextField
              label="Ūgis"
              required
              inputMode="numeric"
              value={form.heightCm}
              onChange={(e) => setForm({ ...form, heightCm: e.target.value })}
              error={!!errors.heightCm}
              helperText={errors.heightCm ?? '100–250 cm'}
              slotProps={{ input: { endAdornment: <InputAdornment position="end">cm</InputAdornment> } }}
            />
            <TextField
              label="Svoris"
              required
              inputMode="decimal"
              value={form.weightKg}
              onChange={(e) => setForm({ ...form, weightKg: e.target.value })}
              error={!!errors.weightKg}
              helperText={errors.weightKg ?? '30–300 kg, 0,1 kg tikslumu'}
              slotProps={{ input: { endAdornment: <InputAdornment position="end">kg</InputAdornment> } }}
            />
            <TextField
              label="Amžius"
              required
              inputMode="numeric"
              value={form.age}
              onChange={(e) => setForm({ ...form, age: e.target.value })}
              error={!!errors.age}
              helperText={errors.age ?? '18–100 m.'}
              slotProps={{ input: { endAdornment: <InputAdornment position="end">m.</InputAdornment> } }}
            />
          </Box>
          <FormControl error={!!errors.gender} sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom id="gender-label">
              Lytis *
            </Typography>
            <ToggleButtonGroup
              exclusive
              aria-labelledby="gender-label"
              value={form.gender}
              onChange={(_, v: Gender | null) => v && setForm({ ...form, gender: v })}
            >
              {(Object.keys(genderLabels) as Gender[]).map((g) => (
                <ToggleButton key={g} value={g} sx={{ px: 4 }}>
                  {genderLabels[g]}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
            {errors.gender && <FormHelperText>{errors.gender}</FormHelperText>}
          </FormControl>
        </Section>

        <Section title="Fizinio aktyvumo lygis *">
          <FormControl error={!!errors.activityLevel} fullWidth>
            <RadioGroup
              value={form.activityLevel ?? ''}
              onChange={(e) => setForm({ ...form, activityLevel: e.target.value as ActivityLevel })}
            >
              <Stack spacing={1}>
                {(Object.keys(activityLabels) as ActivityLevel[]).map((level) => {
                  const a = activityLabels[level];
                  const selected = form.activityLevel === level;
                  return (
                    <Box
                      key={level}
                      sx={{
                        border: 1,
                        borderColor: selected ? 'primary.main' : 'divider',
                        bgcolor: selected ? 'action.selected' : undefined,
                        borderRadius: 2,
                        px: 1.5,
                        py: 0.5,
                      }}
                    >
                      <FormControlLabel
                        value={level}
                        control={<Radio />}
                        sx={{ width: '100%' }}
                        label={
                          <Box>
                            <Typography>{a.name}</Typography>
                            <Typography variant="body2" color="text.secondary">
                              {a.hint} · {formatFactor(a.factor)}
                            </Typography>
                          </Box>
                        }
                      />
                    </Box>
                  );
                })}
              </Stack>
            </RadioGroup>
            {errors.activityLevel && <FormHelperText>{errors.activityLevel}</FormHelperText>}
          </FormControl>
        </Section>

        <Section title="Tikslas *">
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
            {(Object.keys(goalLabels) as Goal[]).map((g) => (
              <Chip
                key={g}
                label={goalLabels[g]}
                icon={form.goal === g ? <CheckIcon /> : undefined}
                color={form.goal === g ? 'primary' : 'default'}
                variant={form.goal === g ? 'filled' : 'outlined'}
                onClick={() => setForm({ ...form, goal: g })}
                aria-pressed={form.goal === g}
              />
            ))}
          </Stack>
          {errors.goal && <FormHelperText error>{errors.goal}</FormHelperText>}
        </Section>

        <Section title="Mitybos apribojimai">
          <Typography variant="subtitle2" gutterBottom>
            Alergenai
          </Typography>
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, mb: 2 }}>
            {allergens.map((a) => {
              const selected = form.allergenIds.includes(a.id);
              return (
                <Chip
                  key={a.id}
                  label={a.name}
                  icon={selected ? <CheckIcon /> : undefined}
                  color={selected ? 'primary' : 'default'}
                  variant={selected ? 'filled' : 'outlined'}
                  onClick={() => toggleAllergen(a.id)}
                  aria-pressed={selected}
                />
              );
            })}
          </Stack>
          <Autocomplete
            multiple
            options={products}
            value={form.dislikedProducts}
            onChange={(_, value) => value.length <= MAX_DISLIKED && setForm({ ...form, dislikedProducts: value })}
            getOptionLabel={(p) => p.name}
            isOptionEqualToValue={(a, b) => a.id === b.id}
            noOptionsText="Produktų nerasta"
            renderInput={(params) => (
              <TextField
                {...params}
                label="Nemėgstami produktai"
                placeholder="Ieškoti produkto…"
                error={!!errors.dislikedProductIds}
                helperText={errors.dislikedProductIds ?? `${form.dislikedProducts.length}/${MAX_DISLIKED}`}
              />
            )}
          />
        </Section>

        <Section title="Ar sergate lėtine liga? *">
          <ToggleButtonGroup
            exclusive
            fullWidth
            value={form.chronicDisease}
            onChange={(_, v: boolean | null) => v !== null && setForm({ ...form, chronicDisease: v })}
          >
            <ToggleButton value={true}>Taip</ToggleButton>
            <ToggleButton value={false}>Ne</ToggleButton>
          </ToggleButtonGroup>
          {errors.chronicDisease && <FormHelperText error>{errors.chronicDisease}</FormHelperText>}
          {form.chronicDisease && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Nurodėte lėtinę ligą. Rekomenduojame kreiptis į dietologą. Sudarę planą galėsite užsakyti jo
              peržiūrą.
            </Alert>
          )}
        </Section>
      </Stack>

      <PrimaryAction>
        <Button type="submit" variant="contained" size="large" disabled={busy}>
          Išsaugoti anketą
        </Button>
      </PrimaryAction>
    </Box>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
          {title}
        </Typography>
        {children}
      </CardContent>
    </Card>
  );
}
