import { Chip } from '@mui/material';
import type { ReviewStatus } from '../api/types';
import { reviewStatusLabels } from '../lib/labels';

const COLORS: Record<ReviewStatus, 'warning' | 'info' | 'success'> = {
  PENDING: 'warning',
  IN_REVIEW: 'info',
  COMPLETED: 'success',
};

/** Užklausos būsenos žyma (NFR-49). */
export function ReviewStatusChip({ status }: { status: ReviewStatus }) {
  return <Chip size="small" color={COLORS[status]} label={reviewStatusLabels[status]} variant="outlined" />;
}
