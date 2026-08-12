/**
 * App Component — Root Router Configuration
 *
 * PURPOSE:
 * Defines all application routes and wraps protected routes
 * with the ProtectedRoute component.
 *
 * ROUTING STRATEGY:
 * - / → redirects to /dashboard (authenticated) or /login (not)
 * - /login → public, redirects to dashboard if already authenticated
 * - /register → public, redirects to dashboard if already authenticated
 * - /dashboard → protected, requires valid JWT
 * - * → 404 catch-all
 *
 * WHY redirect / to /dashboard?
 * The dashboard is the main authenticated view. Users shouldn't
 * see a blank root page.
 *
 * WHY redirect authenticated users away from login/register?
 * No reason to show login to someone already logged in.
 * Prevents confusion.
 */
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import TipCalculatorPage from './pages/TipCalculatorPage';
import NotFoundPage from './pages/NotFoundPage';
import { Box, CircularProgress } from '@mui/material';

/**
 * Wrapper for public-only routes (login, register).
 * Redirects to dashboard if user is already authenticated.
 */
const PublicRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
          background: 'linear-gradient(135deg, #0A0E1A 0%, #121829 100%)',
        }}
      >
        <CircularProgress size={48} sx={{ color: '#6C63FF' }} />
      </Box>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

const App = () => {
  return (
    <Routes>
      {/* Root redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* Public routes — redirect if already authenticated */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        }
      />

      {/* Protected routes — require authentication */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/tips"
        element={
          <ProtectedRoute>
            <TipCalculatorPage />
          </ProtectedRoute>
        }
      />

      {/* 404 catch-all */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default App;
