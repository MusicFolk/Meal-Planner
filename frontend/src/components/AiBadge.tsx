import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import { Chip } from '@mui/material';

/** Vienoda geltona žyma „Sugeneruota DI“ visose darbo vietose (UI-19). */
export function AiBadge({ size = 'small' }: { size?: 'small' | 'medium' }) {
  return (
    <Chip
      icon={<AutoAwesomeIcon sx={{ color: '#3e2e00 !important' }} />}
      label="Sugeneruota DI"
      size={size}
      sx={{ bgcolor: '#ffd54f', color: '#3e2e00', fontWeight: 600 }}
    />
  );
}

/** 1 sprinto planai imami iš demonstracinių duomenų, todėl jie žymimi atskirai. */
export function DemoBadge() {
  return <Chip label="Demonstracinis planas" size="small" variant="outlined" />;
}
