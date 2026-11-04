import { Box, Paper, Stack, useMediaQuery, useTheme } from '@mui/material';
import type { ReactNode } from 'react';

/**
 * Pagrindinio lango veiksmo vieta (UI-18): telefone – ekrano apačioje,
 * kompiuteryje – formos dešinėje.
 */
export function PrimaryAction({ children }: { children: ReactNode }) {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  if (desktop) {
    return (
      <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 3 }}>
        {children}
      </Stack>
    );
  }
  return (
    <>
      <Box sx={{ height: 88 }} />
      <Paper
        square
        elevation={3}
        sx={{
          position: 'fixed',
          left: 0,
          right: 0,
          bottom: 'var(--bottom-nav-height, 0px)',
          p: 1.5,
          zIndex: (t) => t.zIndex.appBar,
        }}
      >
        <Stack direction="row" spacing={1.5} sx={{ '& > *': { flex: 1 } }}>
          {children}
        </Stack>
      </Paper>
    </>
  );
}
