import { Box, Chip, Divider, Stack, Typography } from '@mui/material';
import type { Questionnaire } from '../api/types';
import { formatDecimal, formatFactor, formatInt } from '../lib/format';
import { activityLabels, bmiCategoryLabels, genderLabels, goalLabels } from '../lib/labels';

/** Anketos santrauka be kontaktinių duomenų – naudojama dietologo darbo vietoje (FR-35, NFR-49). */
export function QuestionnaireSummary({ questionnaire: q }: { questionnaire: Questionnaire }) {
  const rows: [string, string][] = [
    ['Ūgis', `${q.heightCm} cm`],
    ['Svoris', `${formatDecimal(q.weightKg)} kg`],
    ['Amžius', `${q.age} m.`],
    ['Lytis', genderLabels[q.gender]],
    ['Aktyvumo lygis', `${activityLabels[q.activityLevel].name} (${formatFactor(activityLabels[q.activityLevel].factor)})`],
    ['Tikslas', goalLabels[q.goal]],
    ['Lėtinė liga', q.chronicDisease ? 'Taip' : 'Ne'],
    ['KMI', `${formatDecimal(q.metrics.bmi)} – ${bmiCategoryLabels[q.metrics.bmiCategory]}`],
    ['Paros poreikis', `${formatInt(q.metrics.dailyCalories)} kcal`],
  ];
  return (
    <Stack spacing={1}>
      {rows.map(([label, value]) => (
        <Stack key={label} direction="row" sx={{ justifyContent: 'space-between', gap: 2 }}>
          <Typography color="text.secondary">{label}</Typography>
          <Typography sx={{ fontWeight: 500, textAlign: 'right' }}>{value}</Typography>
        </Stack>
      ))}
      <Divider sx={{ my: 1 }} />
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Alergenai
        </Typography>
        <ChipList items={q.allergens.map((a) => a.name)} />
      </Box>
      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Nemėgstami produktai
        </Typography>
        <ChipList items={q.dislikedProducts.map((p) => p.name)} />
      </Box>
    </Stack>
  );
}

function ChipList({ items }: { items: string[] }) {
  if (items.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        Nenurodyta
      </Typography>
    );
  }
  return (
    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
      {items.map((i) => (
        <Chip key={i} label={i} size="small" />
      ))}
    </Stack>
  );
}
