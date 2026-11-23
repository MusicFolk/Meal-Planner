import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import HistoryIcon from '@mui/icons-material/History';
import RefreshIcon from '@mui/icons-material/Refresh';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type {
  ClientReview,
  GenerationProgress,
  GenerationStatus,
  MealPlan,
  MealType,
  OrderInfo,
  ShoppingList,
  Subscription,
} from '../../api/types';
import { LinkCard } from '../../components/LinkCard';
import { Loading } from '../../components/Loading';
import { PrimaryAction } from '../../components/PrimaryAction';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { ReviewStatusProgress } from '../../components/ReviewStatusProgress';
import { WeekPlan } from '../../components/WeekPlan';
import { useAuth } from '../../context/AuthContext';
import { useMessage } from '../../context/MessageContext';
import { formatDate } from '../../lib/format';
import { mealTypeLabels } from '../../lib/labels';

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
  const location = useLocation();
  const showMessage = useMessage();
  const [data, setData] = useState<PlanData | null>(null);
  const [generating, setGenerating] = useState(false);
  const [progress, setProgress] = useState<GenerationProgress>();

  useEffect(() => {
    fetchPlanData()
      .then(setData)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  // Kol vyksta generavimas, kas sekundę gaunama kiekvieno valgymo tipo eiga (FR-09)
  useEffect(() => {
    if (!generating) return;
    const timer = window.setInterval(() => {
      api
        .get<GenerationProgress | undefined>('/api/meal-plans/generations/current')
        .then((p) => setProgress(p?.status === 'IN_PROGRESS' ? p : undefined))
        .catch(() => undefined);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [generating]);

  // Generavimo metu mygtukas neaktyvus, antras generavimas nepradedamas (FR-09)
  const generate = async () => {
    setProgress(undefined);
    setGenerating(true);
    try {
      await api.post<MealPlan>('/api/meal-plans/generate');
      setData(await fetchPlanData());
      showMessage('success', 'Savaitės mitybos planas sėkmingai sugeneruotas.');
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        showMessage('warning', err.message);
      } else {
        showMessage('error', 'Plano sugeneruoti nepavyko. Ankstesnis planas nepakeistas.');
      }
      // Atnaujinamas likusių generavimų skaičius; nesėkmingas generavimas į limitą neįskaičiuojamas (FR-14)
      fetchPlanData().then(setData).catch(() => undefined);
    } finally {
      setGenerating(false);
    }
  };

  // „Perkurti planą“ iš rodiklių lango: generavimas pradedamas čia vieną kartą (StrictMode kviečia efektą du kartus)
  const autoStarted = useRef(false);
  useEffect(() => {
    if (autoStarted.current || !(location.state as { generate?: boolean } | null)?.generate) return;
    autoStarted.current = true;
    navigate(location.pathname, { replace: true, state: null });
    generate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (generating) return <Generating progress={progress} />;

  if (!data) return <Loading />;

  const { plan, status, review, orderInfo, shopping, subscription } = data;

  if (!plan) {
    return (
      <EmptyPlan status={status} subscription={subscription} onGenerate={generate} onQuestionnaire={() => navigate('/anketa')} />
    );
  }

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
      <WeekPlan
        key={plan.id}
        plan={plan}
        firstDayOfWeek={user?.firstDayOfWeek ?? 'MONDAY'}
        commentCount={commentCount}
        onDishClick={(dishId) => navigate(`/planas/patiekalas/${dishId}`)}
      />

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
          <Box sx={{ gridColumn: '1 / -1' }}>
            <LinkCard
              icon={<HistoryIcon />}
              title="Planų istorija"
              subtitle="Ankstesni planai ir jų receptai"
              onClick={() => navigate('/planas/istorija')}
            />
          </Box>
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

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER'];

/** Generavimo langas: sukimosi indikatorius ir kiekvieno valgymo tipo eiga (FR-09, 8 pav. b). */
function Generating({ progress }: { progress: GenerationProgress | undefined }) {
  return (
    <Box sx={{ maxWidth: 560, mx: 'auto', textAlign: 'center', pt: 6 }} role="status">
      <CircularProgress size={64} />
      <Typography variant="h5" component="h1" sx={{ fontWeight: 700, mt: 3 }}>
        Sudaromas savaitės planas
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1 }}>
        Tai gali užtrukti iki 60 s. Neuždarykite lango.
      </Typography>
      <Stack spacing={2} sx={{ mt: 4, textAlign: 'left' }}>
        {MEAL_TYPES.map((type) => {
          const meal = progress?.meals.find((m) => m.type === type);
          const done = meal?.done ?? false;
          const attempt = (meal?.attempts ?? 0) + 1;
          return (
            <Box key={type}>
              <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
                <Typography sx={{ fontWeight: 600 }}>{mealTypeLabels[type]}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {done ? 'Paruošta' : attempt > 1 ? `Kartojama (${attempt} bandymas iš 3)` : 'Generuojama…'}
                </Typography>
              </Stack>
              <LinearProgress
                variant={done ? 'determinate' : 'indeterminate'}
                value={100}
                color={done ? 'success' : attempt > 1 ? 'warning' : 'primary'}
                sx={{ height: 8, borderRadius: 4 }}
                aria-label={`${mealTypeLabels[type]}: ${done ? 'paruošta' : 'generuojama'}`}
              />
            </Box>
          );
        })}
      </Stack>
      <PrimaryAction>
        <Button variant="contained" size="large" disabled>
          Generuojama…
        </Button>
      </PrimaryAction>
    </Box>
  );
}
