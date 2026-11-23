import { Card, CardActionArea, CardContent, Typography } from '@mui/material';
import type { ReactNode } from 'react';

/** Nuorodos kortelė plano languose (produktų sąrašas, komentarai, planų istorija). */
export function LinkCard({ icon, title, subtitle, onClick }: {
  icon: ReactNode;
  title: string;
  subtitle: string;
  onClick: () => void;
}) {
  return (
    <Card>
      <CardActionArea onClick={onClick} sx={{ height: '100%' }}>
        <CardContent>
          {icon}
          <Typography sx={{ fontWeight: 600, mt: 1 }}>{title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}
