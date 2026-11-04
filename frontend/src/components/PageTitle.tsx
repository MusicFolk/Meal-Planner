import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { IconButton, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';

export function PageTitle({ title, back, subtitle, action }: {
  title: string;
  back?: string;
  subtitle?: ReactNode;
  action?: ReactNode;
}) {
  const navigate = useNavigate();
  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
      {back && (
        <IconButton onClick={() => navigate(back)} aria-label="Atgal" edge="start">
          <ArrowBackIcon />
        </IconButton>
      )}
      <Stack sx={{ flexGrow: 1, minWidth: 0 }}>
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
        <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
          {title}
        </Typography>
      </Stack>
      {action}
    </Stack>
  );
}
