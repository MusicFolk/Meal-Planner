import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { Comment, DietitianReviewDetail, Meal, Recipe } from '../../api/types';
import { DishCard } from '../../components/DishCard';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { PrimaryAction } from '../../components/PrimaryAction';
import { QuestionnaireSummary } from '../../components/QuestionnaireSummary';
import { RecipeView } from '../../components/RecipeView';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { useMessage } from '../../context/MessageContext';
import { formatDate, formatDateTime, formatInt, WEEKDAYS } from '../../lib/format';

const MAX_COMMENT = 2000;
const COMMENT_ERROR = 'Komentaras negali būti tuščias arba ilgesnis nei 2000 simbolių.';

/**
 * Peržiūros langas: kairėje – kliento anketa be kontaktinių duomenų ir klausimas, dešinėje – planas
 * su galimybe komentuoti atskirus patiekalus (DS-2, FR-35–FR-37).
 */
export function ReviewDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [review, setReview] = useState<DietitianReviewDetail | null>(null);
  const [day, setDay] = useState(1);
  const [general, setGeneral] = useState('');
  const [generalError, setGeneralError] = useState<string | undefined>();
  const [editor, setEditor] = useState<{ meal: Meal; comment?: Comment } | null>(null);
  const [recipe, setRecipe] = useState<Recipe | null>(null);

  useEffect(() => {
    api
      .post<DietitianReviewDetail>(`/api/dietitian/reviews/${id}/open`)
      .then((r) => {
        setReview(r);
        setGeneral(r.comments.find((c) => c.dishId === null)?.text ?? '');
      })
      .catch((e: ApiError) => {
        showMessage('error', e.message);
        navigate('/dietologas', { replace: true });
      });
  }, [id, navigate, showMessage]);

  if (!review) return <Loading />;

  const editable = review.status === 'IN_REVIEW';
  const planDay = review.plan.days.find((d) => d.dayNumber === day) ?? review.plan.days[0];
  const dishComments = (dishId: number) => review.comments.filter((c) => c.dishId === dishId);

  const saveComment = async (text: string) => {
    if (!editor) return;
    const saved = editor.comment
      ? await api.put<Comment>(`/api/dietitian/reviews/${id}/comments/${editor.comment.id}`, { text })
      : await api.post<Comment>(`/api/dietitian/reviews/${id}/comments`, { dishId: editor.meal.dish.id, text });
    setReview({
      ...review,
      comments: editor.comment
        ? review.comments.map((c) => (c.id === saved.id ? saved : c))
        : [...review.comments, saved],
    });
    setEditor(null);
  };

  const deleteComment = async (comment: Comment) => {
    try {
      await api.delete(`/api/dietitian/reviews/${id}/comments/${comment.id}`);
      setReview({ ...review, comments: review.comments.filter((c) => c.id !== comment.id) });
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Nepavyko ištrinti komentaro.');
    }
  };

  const openRecipe = async (meal: Meal) => {
    try {
      setRecipe(await api.get<Recipe>(`/api/dietitian/reviews/${id}/dishes/${meal.dish.id}`));
    } catch (e) {
      showMessage('error', e instanceof ApiError ? e.message : 'Nepavyko atidaryti recepto.');
    }
  };

  const submit = async () => {
    if (!general.trim() || general.length > MAX_COMMENT) {
      setGeneralError(COMMENT_ERROR);
      showMessage('error', COMMENT_ERROR);
      return;
    }
    try {
      const done = await api.post<DietitianReviewDetail>(`/api/dietitian/reviews/${id}/submit`, {
        generalComment: general,
      });
      setReview(done);
      showMessage('success', 'Peržiūra baigta. Komentarai pateikti klientui.');
    } catch (e) {
      if (e instanceof ApiError) {
        setGeneralError(e.fieldErrors.generalComment);
        showMessage('error', e.message);
      }
    }
  };

  return (
    <>
      <PageTitle
        title={`Peržiūros užklausa #${review.id}`}
        subtitle={`Pateikta ${formatDateTime(review.submittedAt)}`}
        back="/dietologas"
        action={<ReviewStatusChip status={review.status} />}
      />
      {review.status === 'COMPLETED' && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Peržiūra pateikta {review.reviewedAt ? formatDate(review.reviewedAt) : ''}. Komentarų redaguoti nebegalima.
        </Alert>
      )}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '360px minmax(0, 1fr)' }, gap: 3, alignItems: 'start' }}>
        <Stack spacing={2}>
          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Kliento klausimas
              </Typography>
              <Typography sx={{ whiteSpace: 'pre-wrap' }} color={review.question ? 'text.primary' : 'text.secondary'}>
                {review.question ?? 'Klientas klausimo nepateikė.'}
              </Typography>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Kliento anketa
              </Typography>
              {review.questionnaire ? (
                <QuestionnaireSummary questionnaire={review.questionnaire} />
              ) : (
                <Typography color="text.secondary">Anketa nepateikta.</Typography>
              )}
            </CardContent>
          </Card>
        </Stack>

        <Stack spacing={2}>
          <Card>
            <Tabs value={day} onChange={(_, v: number) => setDay(v)} variant="scrollable" allowScrollButtonsMobile>
              {review.plan.days.map((d) => (
                <Tab key={d.dayNumber} value={d.dayNumber} label={`${WEEKDAYS[d.dayNumber - 1].short} · ${formatInt(d.totalCalories)} kcal`} />
              ))}
            </Tabs>
          </Card>
          <Typography color="text.secondary">
            {WEEKDAYS[planDay.dayNumber - 1].full}: {formatInt(planDay.totalCalories)} / {formatInt(review.plan.dailyCalories ?? 0)} kcal
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
            {planDay.meals.map((meal) => (
              <DishCard
                key={meal.type}
                meal={meal}
                aiGenerated={review.plan.aiGenerated}
                onClick={() => openRecipe(meal)}
                actions={
                  <Stack spacing={1}>
                    {dishComments(meal.dish.id).map((c) => (
                      <Box key={c.id} sx={{ bgcolor: 'action.hover', borderRadius: 1, p: 1, display: 'flex', gap: 1 }}>
                        <Typography variant="body2" sx={{ flexGrow: 1, whiteSpace: 'pre-wrap' }}>
                          {c.text}
                        </Typography>
                        {editable && (
                          <Stack direction="row">
                            <IconButton size="small" aria-label="Redaguoti komentarą" onClick={() => setEditor({ meal, comment: c })}>
                              <EditOutlinedIcon fontSize="small" />
                            </IconButton>
                            <IconButton size="small" aria-label="Ištrinti komentarą" onClick={() => deleteComment(c)}>
                              <DeleteOutlineIcon fontSize="small" />
                            </IconButton>
                          </Stack>
                        )}
                      </Box>
                    ))}
                    {editable && (
                      <Button size="small" variant="outlined" onClick={() => setEditor({ meal })}>
                        Komentuoti patiekalą
                      </Button>
                    )}
                  </Stack>
                }
              />
            ))}
          </Box>

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Bendras komentaras planui *
              </Typography>
              <TextField
                fullWidth
                multiline
                minRows={4}
                value={general}
                disabled={!editable}
                onChange={(e) => {
                  setGeneral(e.target.value);
                  setGeneralError(undefined);
                }}
                error={!!generalError || general.length > MAX_COMMENT}
                helperText={generalError ?? `${general.length}/${MAX_COMMENT}`}
                slotProps={{ htmlInput: { 'aria-label': 'Bendras komentaras planui' } }}
              />
              {editable && (
                <PrimaryAction>
                  <Button variant="contained" size="large" onClick={submit}>
                    Pateikti peržiūrą
                  </Button>
                </PrimaryAction>
              )}
            </CardContent>
          </Card>
        </Stack>
      </Box>

      <CommentDialog
        key={editor?.comment?.id ?? editor?.meal.dish.id ?? 0}
        open={editor !== null}
        title={editor?.meal.dish.name ?? ''}
        initial={editor?.comment?.text ?? ''}
        onClose={() => setEditor(null)}
        onSave={saveComment}
      />

      <Dialog open={recipe !== null} onClose={() => setRecipe(null)} fullWidth maxWidth="md">
        <DialogTitle>{recipe?.name}</DialogTitle>
        <DialogContent>{recipe && <RecipeView recipe={recipe} />}</DialogContent>
        <DialogActions>
          <Button onClick={() => setRecipe(null)}>Uždaryti</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

function CommentDialog({ open, title, initial, onClose, onSave }: {
  open: boolean;
  title: string;
  initial: string;
  onClose: () => void;
  onSave: (text: string) => Promise<void>;
}) {
  const [text, setText] = useState(initial);
  const [error, setError] = useState<string | undefined>();

  const save = async () => {
    if (!text.trim() || text.length > MAX_COMMENT) {
      setError(COMMENT_ERROR);
      return;
    }
    try {
      await onSave(text);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : COMMENT_ERROR);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Komentaras patiekalui: {title}</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          multiline
          minRows={4}
          sx={{ mt: 1 }}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setError(undefined);
          }}
          error={!!error || text.length > MAX_COMMENT}
          helperText={error ?? `${text.length}/${MAX_COMMENT}`}
          slotProps={{ htmlInput: { 'aria-label': 'Komentaras' } }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Atšaukti</Button>
        <Button variant="contained" onClick={save}>
          Išsaugoti
        </Button>
      </DialogActions>
    </Dialog>
  );
}
