import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { Loading } from './components/Loading';
import { RequireRole } from './components/RequireRole';
import { homePath, useAuth } from './context/AuthContext';
import { ClientLayout } from './layouts/ClientLayout';
import { StaffLayout } from './layouts/StaffLayout';
import { CreateDietitianPage } from './pages/admin/CreateDietitianPage';
import { UsersPage } from './pages/admin/UsersPage';
import { ChangePasswordPage } from './pages/auth/ChangePasswordPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { AccountPage } from './pages/client/AccountPage';
import { BankPage } from './pages/client/BankPage';
import { CommentsPage } from './pages/client/CommentsPage';
import { MetricsPage } from './pages/client/MetricsPage';
import { PlanPage } from './pages/client/PlanPage';
import { QuestionnairePage } from './pages/client/QuestionnairePage';
import { RecipePage } from './pages/client/RecipePage';
import { ReviewOrderPage } from './pages/client/ReviewOrderPage';
import { ShoppingListPage } from './pages/client/ShoppingListPage';
import { ReviewDetailPage } from './pages/dietitian/ReviewDetailPage';
import { ReviewListPage } from './pages/dietitian/ReviewListPage';

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return <Navigate to={user ? homePath(user) : '/prisijungti'} replace />;
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/prisijungti" element={<LoginPage />} />
        <Route path="/registruotis" element={<RegisterPage />} />
        <Route path="/keisti-slaptazodi" element={<ChangePasswordPage />} />

        <Route
          element={
            <RequireRole role="CLIENT">
              <ClientLayout />
            </RequireRole>
          }
        >
          <Route path="/planas" element={<PlanPage />} />
          <Route path="/planas/patiekalas/:dishId" element={<RecipePage />} />
          <Route path="/planas/komentarai" element={<CommentsPage />} />
          <Route path="/planas/perziura" element={<ReviewOrderPage />} />
          <Route path="/produktai" element={<ShoppingListPage />} />
          <Route path="/anketa" element={<QuestionnairePage />} />
          <Route path="/anketa/rodikliai" element={<MetricsPage />} />
          <Route path="/paskyra" element={<AccountPage />} />
        </Route>
        <Route
          path="/bankas/:reference"
          element={
            <RequireRole role="CLIENT">
              <BankPage />
            </RequireRole>
          }
        />

        <Route
          element={
            <RequireRole role="DIETITIAN">
              <StaffLayout sections={[{ path: '/dietologas', label: 'Peržiūros užklausos' }]} />
            </RequireRole>
          }
        >
          <Route path="/dietologas" element={<ReviewListPage />} />
          <Route path="/dietologas/uzklausos/:id" element={<ReviewDetailPage />} />
        </Route>

        <Route
          element={
            <RequireRole role="ADMIN">
              <StaffLayout
                sections={[
                  { path: '/admin', label: 'Naudotojai' },
                  { path: '/admin/dietologai/naujas', label: 'Nauja dietologo paskyra' },
                ]}
              />
            </RequireRole>
          }
        >
          <Route path="/admin" element={<UsersPage />} />
          <Route path="/admin/dietologai/naujas" element={<CreateDietitianPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
