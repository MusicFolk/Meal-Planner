import { Alert, Box, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { ClientReview, PlanSummary } from '../../api/types';
import { AiBadge, DemoBadge } from '../../components/AiBadge';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { useMessage } from '../../context/MessageContext';
import { formatDateTime, formatInt } from '../../lib/format';

/** Planų istorija: visi kliento planai, naujausi viršuje (NI-19, FR-22). */
export function PlanHistoryPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [plans, setPlans] = useState<PlanSummary[] | null>(null);
  const [reviews, setReviews] = useState<ClientReview[]>([]);

  useEffect(() => {
    Promise.all([api.get<PlanSummary[]>('/api/meal-plans'), api.get<ClientReview[]>('/api/reviews')])
      .then(([p, r]) => {
        setPlans(p);
        setReviews(r);
      })
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  if (!plans) return <Loading />;

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle title="Planų istorija" subtitle="Naujausi planai viršuje" back="/planas" />
      {plans.length === 0 ? (
        <Alert severity="info">Mitybos planų dar nėra.</Alert>
      ) : (
        <Stack spacing={2}>
          {plans.map((plan) => {
            const review = reviews.find((r) => r.planId === plan.id);
            return (
              <Card key={plan.id}>
                <CardActionArea onClick={() => navigate(`/planas/istorija/${plan.id}`)}>
                  <CardContent>
                    <Stack
                      direction="row"
                      sx={{ justifyContent: 'space-between', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}
                    >
                      <Typography sx={{ fontWeight: 600 }}>{formatDateTime(plan.createdAt)}</Typography>
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        {plan.active && <Chip label="Aktualus" color="success" size="small" />}
                        {plan.aiGenerated ? <AiBadge /> : <DemoBadge />}
                      </Stack>
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Vidutiniškai {formatInt(plan.averageCalories)}
                      {plan.dailyCalories ? ` / ${formatInt(plan.dailyCalories)}` : ''} kcal per dieną
                      {plan.maxDeviationPercent != null &&
                        ` · didžiausias nuokrypis ${plan.maxDeviationPercent > 0 ? '+' : ''}${String(
                          plan.maxDeviationPercent,
                        ).replace('.', ',')} %`}
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {plan.firstDayDishes.join(' · ')}
                    </Typography>
                    {review && (
                      <Box sx={{ mt: 1 }}>
                        <ReviewStatusChip status={review.status} />
                      </Box>
                    )}
                  </CardContent>
                </CardActionArea>
              </Card>
            );
          })}
        </Stack>
      )}
    </Box>
  );
}
