import { Box, Card, CardContent, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { Logo } from '../../components/Logo';

/** Prisijungimo ir registracijos langų rėmas: kompiuteryje forma pateikiama dešinėje lango pusėje. */
export function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 480px' } }}>
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'center',
          p: 8,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
        }}
      >
        <Typography variant="h3" component="p" sx={{ fontWeight: 700, mb: 2 }}>
          Meal Planner
        </Typography>
        <Typography variant="h6" component="p" sx={{ opacity: 0.9, maxWidth: 520 }}>
          Individualus savaitės mitybos planas su receptais ir produktų sąrašu – be vizito į kliniką.
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}>
        <Card sx={{ width: '100%', maxWidth: 420 }} variant="elevation" elevation={0}>
          <CardContent>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mb: 3 }}>
              <Logo size={40} />
              <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
                {title}
              </Typography>
            </Stack>
            {children}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
