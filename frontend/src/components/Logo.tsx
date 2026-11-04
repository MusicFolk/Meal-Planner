import { Box } from '@mui/material';

export function Logo({ size = 32 }: { size?: number }) {
  return <Box component="img" src="/favicon.svg" alt="" sx={{ width: size, height: size, display: 'block' }} />;
}
