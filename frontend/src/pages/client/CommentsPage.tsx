import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { ClientReview, OrderInfo } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { ReviewStatusProgress } from '../../components/ReviewStatusProgress';
import { useMessage } from '../../context/MessageContext';
import { formatDate } from '../../lib/format';

/** Dietologo komentarų langas (3.3 užduotis, FR-21). Komentarus mato tik plano savininkas. */
export function CommentsPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [state, setState] = useState<{ review?: ClientReview; orderInfo: OrderInfo } | null>(null);

  useEffect(() => {
    Promise.all([api.get<ClientReview | undefined>('/api/reviews/current'), api.get<OrderInfo>('/api/reviews/order-info')])
      .then(([review, orderInfo]) => setState({ review, orderInfo }))
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [showMessage]);

  if (!state) return <Loading />;
  const { review, orderInfo } = state;
  const general = review?.comments.find((c) => c.dishId === null);
  const dishComments = review?.comments.filter((c) => c.dishId !== null) ?? [];

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle title="Dietologo komentarai" back="/planas" />
      {!review ? (
        <Card>
          <CardContent>
            <Typography gutterBottom>Šiam planui dietologo peržiūra neužsakyta.</Typography>
            {orderInfo.canOrder && (
              <Button variant="contained" onClick={() => navigate('/planas/perziura')}>
                Užsakyti dietologo peržiūrą
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <Stack spacing={2}>
          <Card>
            <CardContent>
              <ReviewStatusProgress status={review.status} />
              {review.question && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  Jūsų klausimas: „{review.question}“
                </Typography>
              )}
            </CardContent>
          </Card>
          {review.status !== 'COMPLETED' ? (
            <Typography color="text.secondary">Komentarai bus matomi, kai dietologas baigs peržiūrą.</Typography>
          ) : (
            <>
              {general && (
                <CommentCard title="Bendras komentaras" text={general.text} author={review.dietitianName} date={general.createdAt} />
              )}
              {dishComments.map((c) => (
                <CommentCard
                  key={c.id}
                  title={c.dishName ?? ''}
                  text={c.text}
                  author={review.dietitianName}
                  date={c.createdAt}
                  onClick={() => navigate(`/planas/patiekalas/${c.dishId}`)}
                />
              ))}
            </>
          )}
        </Stack>
      )}
    </Box>
  );
}

function CommentCard({ title, text, author, date, onClick }: {
  title: string;
  text: string;
  author: string | null;
  date: string;
  onClick?: () => void;
}) {
  return (
    <Card>
      <CardContent>
        <Typography sx={{ fontWeight: 600 }}>{title}</Typography>
        <Typography sx={{ my: 1, whiteSpace: 'pre-wrap' }}>{text}</Typography>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            {author} · {formatDate(date)}
          </Typography>
          {onClick && (
            <Button size="small" onClick={onClick}>
              Receptas
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}
