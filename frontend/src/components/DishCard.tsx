import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import { Box, Card, CardActionArea, CardContent, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import type { Meal } from '../api/types';
import { formatGrams, formatKcal } from '../lib/format';
import { mealTypeLabels } from '../lib/labels';
import { AiBadge } from './AiBadge';

interface Props {
  meal: Meal;
  aiGenerated: boolean;
  commentCount?: number;
  onClick?: () => void;
  actions?: ReactNode;
}

/** Patiekalo kortelė: valgymo tipas, pavadinimas, porcija ir kaloringumas (UI-02, NFR-49). */
export function DishCard({ meal, aiGenerated, commentCount = 0, onClick, actions }: Props) {
  const content = (
    <CardContent>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}>
        <Typography variant="overline" color="text.secondary">
          {mealTypeLabels[meal.type]}
        </Typography>
        <Typography sx={{ fontWeight: 700 }}>{formatKcal(meal.dish.nutrition.kcal)}</Typography>
      </Stack>
      <Typography variant="h6" component="h3" sx={{ fontSize: '1.05rem', lineHeight: 1.3, mb: 1 }}>
        {meal.dish.name}
      </Typography>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          Porcija {formatGrams(meal.dish.portionGrams)}
        </Typography>
        {aiGenerated && <AiBadge />}
      </Stack>
      {commentCount > 0 && (
        <Stack direction="row" spacing={1} sx={{ mt: 1, alignItems: 'center' }}>
          <ChatBubbleOutlineIcon fontSize="small" color="action" />
          <Typography variant="body2">
            {commentCount} dietologo {commentCount === 1 ? 'komentaras' : 'komentarai'}
          </Typography>
        </Stack>
      )}
    </CardContent>
  );
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {onClick ? (
        <CardActionArea onClick={onClick} sx={{ flexGrow: 1, alignItems: 'stretch' }}>
          {content}
        </CardActionArea>
      ) : (
        <Box sx={{ flexGrow: 1 }}>{content}</Box>
      )}
      {actions && <Box sx={{ px: 2, pb: 2 }}>{actions}</Box>}
    </Card>
  );
}
