import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import {
  Alert,
  Box,
  Button,
  ButtonBase,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type {
  ClientReview,
  GenerationStatus,
  MealPlan,
  OrderInfo,
  ShoppingList,
  Subscription,
} from '../../api/types';
import { AiBadge, DemoBadge } from '../../components/AiBadge';
import { DishCard } from '../../components/DishCard';
import { Loading } from '../../components/Loading';
import { PrimaryAction } from '../../components/PrimaryAction';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { ReviewStatusProgress } from '../../components/ReviewStatusProgress';
import { useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { formatDate, formatDayHeading, formatInt, formatWeekRange, planCalendar } from '../../lib/format';

interface PlanData {
  plan: MealPlan | undefined;
  status: GenerationStatus;
  review: ClientReview | undefined;
  orderInfo: OrderInfo;
  shopping: ShoppingList | undefined;
  subscription: Subscription;
}

async function fetchPlanData(): Promise<PlanData> {
  const [plan, status, review, orderInfo, subscription] = await Promise.all([
    api.get<MealPlan | undefined>('/api/meal-plans/active'),
    api.get<GenerationStatus>('/api/meal-plans/generation-status'),
    api.get<ClientReview | undefined>('/api/reviews/current'),
    api.get<OrderInfo>('/api/reviews/order-info'),
    api.get<Subscription>('/api/subscription'),
  ]);
  const shopping = plan ? await api.get<ShoppingList>(`/api/meal-plans/${plan.id}/shopping-list`) : undefined;
  return { plan, status, review, orderInfo, shopping, subscription };
}

/** Savaitės plano langas – savaitės planuoklis (UI-01, FR-17, FR-27). */
export function PlanPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [data, setData] = useState<PlanData | null>(null);
  const [generating, setGenerating] = useState(false);
  const [selectedDay, setSelectedDay] = useState<number | null>(null);

  useEffect(() => {
    fetchPlanData()
      .then(setData)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  // Generavimo metu mygtukas neaktyvus, antras generavimas nepradedamas (FR-09)
  const generate = async () => {
    setGenerating(true);
    try {
      await api.post<MealPlan>('/api/meal-plans/generate');
      setData(await fetchPlanData());
      setSelectedDay(null);
      showMessage('success', 'Savaitės mitybos planas sėkmingai sugeneruotas.');
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        showMessage('warning', err.message);
      } else {
        showMessage('error', 'Plano sugeneruoti nepavyko. Ankstesnis planas nepakeistas.');
      }
    } finally {
      setGenerating(false);
    }
  };

  if (!data) return <Loading />;

  if (generating) return <Generating />;

  const { plan, status, review, orderInfo, shopping, subscription } = data;

  if (!plan) {
    return (
      <EmptyPlan status={status} subscription={subscription} onGenerate={generate} onQuestionnaire={() => navigate('/anketa')} />
    );
  }

  const calendar = planCalendar(plan.createdAt, user?.firstDayOfWeek ?? 'MONDAY');
  const todayNumber = ((new Date().getDay() + 6) % 7) + 1;
  const inThisWeek = calendar.some((d) => formatDate(d.date) === formatDate(new Date()));
  const dayNumber = selectedDay ?? (inThisWeek ? todayNumber : calendar[0].dayNumber);
  const day = plan.days.find((d) => d.dayNumber === dayNumber) ?? plan.days[0];
  const calendarDay = calendar.find((c) => c.dayNumber === day.dayNumber)!;
  const target = plan.dailyCalories ?? day.totalCalories;
  const deviation = target ? ((day.totalCalories - target) / target) * 100 : 0;
  const completedComments = review?.status === 'COMPLETED' ? review.comments : [];
  const commentCount = (dishId: number) => completedComments.filter((c) => c.dishId === dishId).length;

  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) 320px' },
        gap: 3,
        alignItems: 'start',
      }}
    >
      <Stack spacing={2}>
        <Box>
          <Typography variant="body2" color="text.secondary">
            {formatWeekRange(calendar[0].date, calendar[6].date)}
          </Typography>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap' }}>
            <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
              Savaitės planas
            </Typography>
            {plan.aiGenerated ? <AiBadge size="medium" /> : <DemoBadge />}
          </Stack>
        </Box>

        <Box
          role="tablist"
          aria-label="Savaitės dienos"
          sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: { xs: 0.5, sm: 1 } }}
        >
          {calendar.map((c) => {
            const selected = c.dayNumber === day.dayNumber;
            const planDay = plan.days.find((d) => d.dayNumber === c.dayNumber);
            return (
              <ButtonBase
                key={c.dayNumber}
                role="tab"
                aria-selected={selected}
                aria-label={formatDayHeading(c)}
                onClick={() => setSelectedDay(c.dayNumber)}
                sx={{
                  flexDirection: 'column',
                  py: 1,
                  borderRadius: 2,
                  border: 1,
                  borderColor: selected ? 'text.primary' : 'divider',
                  bgcolor: selected ? 'text.primary' : 'background.paper',
                  color: selected ? 'background.paper' : 'text.primary',
                }}
              >
                <Typography variant="caption">{c.short}</Typography>
                <Typography sx={{ fontWeight: 700, fontSize: '1.15rem' }}>{c.date.getDate()}</Typography>
                {planDay && (
                  <Typography variant="caption" sx={{ display: { xs: 'none', md: 'block' }, opacity: 0.8 }}>
                    {formatInt(planDay.totalCalories)} kcal
                  </Typography>
                )}
              </ButtonBase>
            );
          })}
        </Box>

        <Card>
          <CardContent>
            <Stack direction="row" sx={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
              <Typography sx={{ fontWeight: 600 }}>{formatDayHeading(calendarDay)}</Typography>
              <Typography sx={{ fontWeight: 600 }}>
                {formatInt(day.totalCalories)} / {formatInt(target)} kcal
              </Typography>
            </Stack>
            <LinearProgress
              variant="determinate"
              value={Math.min(100, (day.totalCalories / target) * 100)}
              sx={{ my: 1.5, height: 8, borderRadius: 4 }}
              aria-label="Dienos kaloringumas"
            />
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <CheckCircleOutlineIcon color={Math.abs(deviation) <= 10 ? 'success' : 'warning'} fontSize="small" />
              <Typography variant="body2">
                {Math.abs(deviation) <= 10 ? 'Atitinka paros poreikį' : 'Nukrypsta nuo paros poreikio'} (
                {deviation >= 0 ? '+' : ''}
                {deviation.toFixed(1).replace('.', ',')} %)
              </Typography>
            </Stack>
          </CardContent>
        </Card>

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
          {day.meals.map((meal) => (
            <DishCard
              key={meal.type}
              meal={meal}
              aiGenerated={plan.aiGenerated}
              commentCount={commentCount(meal.dish.id)}
              onClick={() => navigate(`/planas/patiekalas/${meal.dish.id}`)}
            />
          ))}
        </Box>
      </Stack>

      <Stack spacing={2}>
        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
          <LinkCard
            icon={<ShoppingCartOutlinedIcon />}
            title="Produktų sąrašas"
            subtitle={shopping ? `${shopping.purchasedItems} iš ${shopping.totalItems} nupirkta` : ''}
            onClick={() => navigate('/produktai')}
          />
          <LinkCard
            icon={<ChatBubbleOutlineIcon />}
            title="Komentarai"
            subtitle={completedComments.length > 0 ? `${completedComments.length} dietologo komentarai` : 'Komentarų nėra'}
            onClick={() => navigate('/planas/komentarai')}
          />
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

        <Box>
          <Button
            fullWidth
            variant="outlined"
            size="large"
            startIcon={<RefreshIcon />}
            onClick={generate}
            disabled={!status.canGenerate}
          >
            Perkurti planą
          </Button>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 1 }}>
            {status.canGenerate ? `Šiandien liko generavimų: ${status.remainingToday}` : status.reason}
          </Typography>
        </Box>

        {orderInfo.canOrder && (
          <PrimaryAction>
            <Button variant="contained" size="large" onClick={() => navigate('/planas/perziura')}>
              Užsakyti dietologo peržiūrą
            </Button>
          </PrimaryAction>
        )}
      </Stack>
    </Box>
  );
}

function LinkCard({ icon, title, subtitle, onClick }: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  onClick: () => void;
}) {
  return (
    <Card>
      <CardActionArea onClick={onClick} sx={{ height: '100%' }}>
        <CardContent>
          {icon}
          <Typography sx={{ fontWeight: 600, mt: 1 }}>{title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

function EmptyPlan({ status, subscription, onGenerate, onQuestionnaire }: {
  status: GenerationStatus;
  subscription: Subscription;
  onGenerate: () => void;
  onQuestionnaire: () => void;
}) {
  return (
    <Box sx={{ maxWidth: 560, mx: 'auto', textAlign: 'center', pt: 4 }}>
      <Box
        sx={{
          width: 120,
          height: 120,
          borderRadius: '50%',
          bgcolor: 'action.hover',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mx: 'auto',
          mb: 3,
        }}
      >
        <CalendarMonthOutlinedIcon sx={{ fontSize: 56 }} color="action" />
      </Box>
      <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
        Dar neturite mitybos plano
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1, mb: 3 }}>
        Planas bus sudarytas pagal jūsų anketą: 7 dienos po 3 valgymus.
      </Typography>
      {status.canGenerate ? (
        <Alert severity="info" sx={{ textAlign: 'left' }}>
          Šiandien planą galite generuoti dar {status.remainingToday} kartus.{' '}
          {subscription.status === 'TRIAL' && `Bandomasis laikotarpis galioja iki ${formatDate(subscription.validUntil)}.`}
        </Alert>
      ) : (
        <Alert severity="warning" sx={{ textAlign: 'left' }}>
          {status.reason}
        </Alert>
      )}
      <PrimaryAction>
        {status.questionnaireFilled ? (
          <Button
            variant="contained"
            size="large"
            startIcon={<AutoAwesomeIcon />}
            onClick={onGenerate}
            disabled={!status.canGenerate}
          >
            Generuoti planą
          </Button>
        ) : (
          <Button variant="contained" size="large" onClick={onQuestionnaire}>
            Užpildyti anketą
          </Button>
        )}
      </PrimaryAction>
    </Box>
  );
}

function Generating() {
  return (
    <Box sx={{ maxWidth: 560, mx: 'auto', textAlign: 'center', pt: 6 }} role="status">
      <CircularProgress size={64} />
      <Typography variant="h5" component="h1" sx={{ fontWeight: 700, mt: 3 }}>
        Sudaromas savaitės planas
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1 }}>
        Tai gali užtrukti iki 60 s. Neuždarykite lango.
      </Typography>
      <PrimaryAction>
        <Button variant="contained" size="large" disabled>
          Generuojama…
        </Button>
      </PrimaryAction>
    </Box>
  );
}
