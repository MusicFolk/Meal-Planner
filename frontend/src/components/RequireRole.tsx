import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import type { Role } from '../api/types';
import { homePath, useAuth } from '../context/AuthContext';
import { Loading } from './Loading';

/** Neprisijungęs naudotojas nukreipiamas į prisijungimo langą (FR-33), kito vaidmens – į savo pagrindinį langą (FR-32). */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/prisijungti" replace state={{ from: location.pathname }} />;
  if (user.role !== role) return <Navigate to={homePath(user)} replace />;
  if (user.mustChangePassword && location.pathname !== '/keisti-slaptazodi') {
    return <Navigate to="/keisti-slaptazodi" replace />;
  }
  return <>{children}</>;
}
