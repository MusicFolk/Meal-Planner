import { Alert, AlertTitle, Box, Stack } from '@mui/material';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { ClientReview, Recipe } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { useMessage } from '../../context/MessageContext';
import { formatDate } from '../../lib/format';
import { mealTypeLabels } from '../../lib/labels';
import { RecipeView } from '../../components/RecipeView';

/** Patiekalo recepto langas (3.1 užduotis, FR-18) su dietologo komentaru patiekalui. */
export function RecipePage() {
  const { dishId } = useParams();
  const showMessage = useMessage();
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [review, setReview] = useState<ClientReview | undefined>();

  useEffect(() => {
    Promise.all([
      api.get<Recipe>(`/api/meal-plans/dishes/${dishId}`),
      api.get<ClientReview | undefined>('/api/reviews/current'),
    ])
      .then(([r, rev]) => {
        setRecipe(r);
        setReview(rev);
      })
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [dishId, showMessage]);

  if (!recipe) return <Loading />;

  const comments =
    review?.status === 'COMPLETED' && review.planId === recipe.planId
      ? review.comments.filter((c) => c.dishId === recipe.dishId)
      : [];

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle title={recipe.name} subtitle={mealTypeLabels[recipe.mealType]} back="/planas" />
      <Stack spacing={2}>
        <RecipeView recipe={recipe} />
        {comments.map((c) => (
          <Alert key={c.id} severity="info" icon={false}>
            <AlertTitle>
              Dietologo komentaras · {review?.dietitianName} · {formatDate(c.createdAt)}
            </AlertTitle>
            {c.text}
          </Alert>
        ))}
      </Stack>
    </Box>
  );
}
