import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import { Alert, Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { ClientReview, MealPlan, ShoppingList } from '../../api/types';
import { LinkCard } from '../../components/LinkCard';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { ReviewStatusProgress } from '../../components/ReviewStatusProgress';
import { WeekPlan } from '../../components/WeekPlan';
import { useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { formatDateTime } from '../../lib/format';

/** Plano iš istorijos peržiūra su receptais ir produktų sąrašu; planą galima vėl pažymėti aktualiu (FR-22). */
export function HistoryPlanPage() {
  const { planId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [plan, setPlan] = useState<MealPlan | null>(null);
  const [review, setReview] = useState<ClientReview | undefined>();
  const [shopping, setShopping] = useState<ShoppingList | undefined>();
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get<MealPlan>(`/api/meal-plans/${planId}`),
      api.get<ClientReview[]>('/api/reviews'),
      api.get<ShoppingList>(`/api/meal-plans/${planId}/shopping-list`),
    ])
      .then(([p, reviews, list]) => {
        setPlan(p);
        setReview(reviews.find((r) => r.planId === p.id));
        setShopping(list);
      })
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [planId, showMessage]);

  const activate = async () => {
    setBusy(true);
    try {
      await api.put(`/api/meal-plans/${planId}/active`);
      showMessage('success', 'Planas pažymėtas aktualiu.');
      navigate('/planas');
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Nepavyko pažymėti plano aktualiu.');
      setBusy(false);
    }
  };

  if (!plan) return <Loading />;

  const back = `/planas/istorija/${plan.id}`;
  const completedComments = review?.status === 'COMPLETED' ? review.comments : [];

  return (
    <Box>
      <PageTitle
        title={plan.active ? 'Aktualus planas' : 'Ankstesnis planas'}
        subtitle={`Sukurtas ${formatDateTime(plan.createdAt)}`}
        back="/planas/istorija"
      />
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) 320px' },
          gap: 3,
          alignItems: 'start',
        }}
      >
        <WeekPlan
          plan={plan}
          firstDayOfWeek={user?.firstDayOfWeek ?? 'MONDAY'}
          commentCount={(dishId) => completedComments.filter((c) => c.dishId === dishId).length}
          onDishClick={(dishId) => navigate(`/planas/patiekalas/${dishId}`, { state: { back } })}
        />

        <Stack spacing={2}>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            <LinkCard
              icon={<ShoppingCartOutlinedIcon />}
              title="Produktų sąrašas"
              subtitle={shopping ? `${shopping.purchasedItems} iš ${shopping.totalItems} nupirkta` : ''}
              onClick={() => navigate(`/planas/istorija/${plan.id}/produktai`)}
            />
            <Card>
              <CardContent>
                <ChatBubbleOutlineIcon />
                <Typography sx={{ fontWeight: 600, mt: 1 }}>Komentarai</Typography>
                <Typography variant="body2" color="text.secondary">
                  {completedComments.length > 0 ? `${completedComments.length} dietologo komentarai` : 'Komentarų nėra'}
                </Typography>
              </CardContent>
            </Card>
          </Box>

          {review && (
            <Card>
              <CardContent>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography sx={{ fontWeight: 600 }}>Dietologo peržiūra</Typography>
                  <ReviewStatusChip status={review.status} />
                </Stack>
                <ReviewStatusProgress status={review.status} />
              </CardContent>
            </Card>
          )}

          {plan.active ? (
            <Alert severity="info">Šis planas yra aktualus.</Alert>
          ) : (
            <PrimaryAction>
              <Button variant="contained" size="large" onClick={activate} disabled={busy}>
                Pažymėti aktualiu
              </Button>
            </PrimaryAction>
          )}
        </Stack>
      </Box>
    </Box>
  );
}
