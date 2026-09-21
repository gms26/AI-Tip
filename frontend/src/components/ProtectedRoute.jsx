/**
 * Protected Route Component
 *
 * PURPOSE:
 * Guards routes that require authentication. If the user is not
 * authenticated, they are redirected to the login page.
 *
 * WHY a separate component?
 * - Reusable — wrap any route that needs protection
 * - Declarative — security is visible in the router config
 * - Centralized — auth check logic lives in one place
 *
 * WHY show loading state?
 * On initial page load, the AuthContext is validating the stored
 * token. During this time, we don't know if the user is authenticated.
 * Showing a loading spinner prevents a flash of the login page
 * before redirecting to the dashboard.
 */
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Box, CircularProgress } from '@mui/material';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  // Show loading spinner while auth state is being determined
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
        <CircularProgress
          size={48}
          sx={{ color: '#fd5b38' }}
        />
      </Box>
    );
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    /**
     * WHY pass state.from?
     * After login, we can redirect the user back to the page
     * they originally wanted to visit. Better UX than always
     * landing on the dashboard.
     */
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

export default ProtectedRoute;
