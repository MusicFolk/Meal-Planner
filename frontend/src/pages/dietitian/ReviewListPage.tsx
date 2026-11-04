import {
  Button,
  Card,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { DietitianReviewSummary, ReviewStatus } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { ReviewStatusChip } from '../../components/ReviewStatusChip';
import { useMessage } from '../../context/MessageContext';
import { formatDateTime } from '../../lib/format';
import { reviewStatusLabels } from '../../lib/labels';

/** Apmokėtų peržiūros užklausų sąrašas su filtru pagal būseną, seniausios viršuje (DS-1, FR-34). */
export function ReviewListPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  const [filter, setFilter] = useState<ReviewStatus | 'ALL'>('ALL');
  const [items, setItems] = useState<DietitianReviewSummary[] | null>(null);

  useEffect(() => {
    const query = filter === 'ALL' ? '' : `?status=${filter}`;
    api
      .get<DietitianReviewSummary[]>(`/api/dietitian/reviews${query}`)
      .then(setItems)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [filter, showMessage]);

  return (
    <>
      <PageTitle title="Peržiūros užklausos" />
      <ToggleButtonGroup
        exclusive
        size="small"
        value={filter}
        onChange={(_, v: ReviewStatus | 'ALL' | null) => v && setFilter(v)}
        sx={{ mb: 2 }}
        aria-label="Filtras pagal būseną"
      >
        <ToggleButton value="ALL">Visos</ToggleButton>
        {(Object.keys(reviewStatusLabels) as ReviewStatus[]).map((s) => (
          <ToggleButton key={s} value={s}>
            {reviewStatusLabels[s]}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>
      {items === null ? (
        <Loading />
      ) : items.length === 0 ? (
        <Typography color="text.secondary">Užklausų nėra.</Typography>
      ) : (
        <TableContainer component={Card}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Pateikta</TableCell>
                <TableCell>Kliento klausimas</TableCell>
                <TableCell>Būsena</TableCell>
                <TableCell align="right" />
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDateTime(r.submittedAt)}</TableCell>
                  <TableCell>{r.question ?? <Typography color="text.secondary">Be klausimo</Typography>}</TableCell>
                  <TableCell>
                    <ReviewStatusChip status={r.status} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
                      <Button variant={r.status === 'PENDING' ? 'contained' : 'outlined'} onClick={() => navigate(`/dietologas/uzklausos/${r.id}`)}>
                        {r.status === 'PENDING' ? 'Pradėti peržiūrą' : 'Atidaryti'}
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
