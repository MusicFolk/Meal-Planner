import LogoutIcon from '@mui/icons-material/Logout';
import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material';
import { Outlet, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext';
import { roleLabels } from '../lib/labels';
import { Logo } from '../components/Logo';

/** Dietologo ir administratoriaus darbo vietos: tik viršutinė juosta su mygtuku „Atsijungti“. */
export function StaffLayout({ sections }: { sections: { path: string; label: string }[] }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <Box sx={{ minHeight: '100vh' }}>
      <AppBar position="sticky" color="inherit" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Logo />
          <Typography variant="h6" component="div" sx={{ fontWeight: 700 }}>
            Meal Planner
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexGrow: 1 }}>
            {sections.map((s) => (
              <Button key={s.path} color="inherit" onClick={() => navigate(s.path)}>
                {s.label}
              </Button>
            ))}
          </Box>
          {user && (
            <Typography variant="body2" color="text.secondary" sx={{ display: { xs: 'none', sm: 'block' } }}>
              {user.firstName} {user.lastName} · {roleLabels[user.role]}
            </Typography>
          )}
          <Button
            variant="outlined"
            startIcon={<LogoutIcon />}
            onClick={async () => {
              await logout();
              navigate('/prisijungti');
            }}
          >
            Atsijungti
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
