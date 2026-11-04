import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutlineOutlined';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import {
  AppBar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Container,
  Paper,
  Tab,
  Tabs,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { Logo } from '../components/Logo';
import { NotificationBell } from '../components/NotificationBell';

const SECTIONS = [
  { path: '/planas', label: 'Planas', icon: <CalendarMonthOutlinedIcon /> },
  { path: '/produktai', label: 'Produktai', icon: <ShoppingCartOutlinedIcon /> },
  { path: '/anketa', label: 'Anketa', icon: <AssignmentOutlinedIcon /> },
  { path: '/paskyra', label: 'Paskyra', icon: <PersonOutlineIcon /> },
];

/** Kliento sąsaja: telefone – apatinė juosta su keturiais skirsniais, kompiuteryje – viršutinė juosta. */
export function ClientLayout() {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  const location = useLocation();
  const navigate = useNavigate();
  const current = SECTIONS.findIndex((s) => location.pathname.startsWith(s.path));

  return (
    <Box sx={{ minHeight: '100vh', '--bottom-nav-height': desktop ? '0px' : '64px' }}>
      <AppBar position="sticky" color="inherit" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar sx={{ gap: 1.5 }}>
          <Logo />
          <Typography variant="h6" component="div" sx={{ fontWeight: 700, mr: 3 }}>
            Meal Planner
          </Typography>
          {desktop && (
            <Tabs value={current === -1 ? false : current} sx={{ flexGrow: 1 }}>
              {SECTIONS.map((s) => (
                <Tab key={s.path} label={s.label} icon={s.icon} iconPosition="start" onClick={() => navigate(s.path)} />
              ))}
            </Tabs>
          )}
          <Box sx={{ flexGrow: desktop ? 0 : 1 }} />
          <NotificationBell />
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 3, pb: desktop ? 4 : 10 }}>
        <Outlet />
      </Container>
      {!desktop && (
        <Paper
          elevation={3}
          sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: (t) => t.zIndex.appBar + 1 }}
        >
          <BottomNavigation showLabels value={current} onChange={(_, i: number) => navigate(SECTIONS[i].path)}>
            {SECTIONS.map((s) => (
              <BottomNavigationAction key={s.path} label={s.label} icon={s.icon} />
            ))}
          </BottomNavigation>
        </Paper>
      )}
    </Box>
  );
}
