import { Box, Card, CardContent, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import type { Recipe } from '../api/types';
import { formatGrams, formatInt } from '../lib/format';

/** Recepto lapas: maistinė vertė, ingredientai gramais ir sunumeruoti žingsniai (UI-02, FR-18). */
export function RecipeView({ recipe }: { recipe: Recipe }) {
  const nutrition = [
    ['Energinė vertė', `${formatInt(recipe.nutrition.kcal)} kcal`],
    ['Baltymai', `${formatInt(recipe.nutrition.proteinG)} g`],
    ['Riebalai', `${formatInt(recipe.nutrition.fatG)} g`],
    ['Angliavandeniai', `${formatInt(recipe.nutrition.carbsG)} g`],
  ];
  return (
    <Stack spacing={2}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(4, 1fr)' }, gap: 1 }}>
        {nutrition.map(([label, value]) => (
          <Card key={label}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">
                {label}
              </Typography>
              <Typography sx={{ fontWeight: 700 }}>{value}</Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
      <Card>
        <CardContent>
          <Typography variant="h6" component="h2" gutterBottom>
            Ingredientai ({formatGrams(recipe.portionGrams)})
          </Typography>
          <List dense disablePadding>
            {recipe.ingredients.map((i) => (
              <ListItem key={i.productId} disableGutters secondaryAction={<Typography>{formatGrams(i.grams)}</Typography>}>
                <ListItemText primary={i.name} />
              </ListItem>
            ))}
          </List>
        </CardContent>
      </Card>
      <Card>
        <CardContent>
          <Typography variant="h6" component="h2" gutterBottom>
            Gaminimo žingsniai
          </Typography>
          <Box component="ol" sx={{ pl: 3, m: 0, '& li': { mb: 1 } }}>
            {recipe.steps.map((s) => (
              <li key={s.number}>
                <Typography>{s.description}</Typography>
              </li>
            ))}
          </Box>
        </CardContent>
      </Card>
    </Stack>
  );
}
