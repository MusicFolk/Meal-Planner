import { CssBaseline, ThemeProvider, createTheme, useMediaQuery } from '@mui/material';
import { useMemo, type ReactNode } from 'react';
import { useAuth } from './AuthContext';

/**
 * Material Design tema (UI-14). Pagrindinė spalva žalia (UI-18).
 * Tema parenkama pagal kliento nustatymą arba įrenginį (UI-20).
 */
export function AppThemeProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
  const preference = user?.theme ?? 'SYSTEM';
  const mode = preference === 'DARK' || (preference === 'SYSTEM' && prefersDark) ? 'dark' : 'light';

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: { main: mode === 'dark' ? '#66bb6a' : '#2e7d32' },
          secondary: { main: '#455a64' },
          background: mode === 'light' ? { default: '#f6f7f6', paper: '#ffffff' } : {},
        },
        shape: { borderRadius: 12 },
        typography: {
          fontFamily: '"Roboto", "Segoe UI", system-ui, sans-serif',
          button: { textTransform: 'none', fontWeight: 600 },
        },
        components: {
          MuiCard: { defaultProps: { variant: 'outlined' } },
          MuiButton: { defaultProps: { disableElevation: true } },
        },
      }),
    [mode],
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
