import { Box, CircularProgress } from '@mui/material';

export function Loading() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }} role="status" aria-label="Įkeliama">
      <CircularProgress />
    </Box>
  );
}
