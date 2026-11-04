import { Step, StepLabel, Stepper } from '@mui/material';
import type { ReviewStatus } from '../api/types';
import { reviewStatusLabels } from '../lib/labels';

const ORDER: ReviewStatus[] = ['PENDING', 'IN_REVIEW', 'COMPLETED'];

/** Peržiūros būsenos eiga „Laukia“ → „Peržiūrima“ → „Atlikta“ (UI-04, NFR-49). */
export function ReviewStatusProgress({ status }: { status: ReviewStatus }) {
  const active = ORDER.indexOf(status);
  return (
    <Stepper activeStep={status === 'COMPLETED' ? ORDER.length : active + 1} alternativeLabel>
      {ORDER.map((s) => (
        <Step key={s} completed={ORDER.indexOf(s) <= active}>
          <StepLabel>{reviewStatusLabels[s]}</StepLabel>
        </Step>
      ))}
    </Stepper>
  );
}
