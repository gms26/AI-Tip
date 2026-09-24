import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppShell from './components/AppShell';

// Public Pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import LandingPage from './pages/LandingPage';
import NotFoundPage from './pages/NotFoundPage';

// Protected Pages
import DashboardPage from './pages/DashboardPage';
import TipCalculatorPage from './pages/TipCalculatorPage';
import TipHistoryPage from './pages/TipHistoryPage';
import TipCoachPage from './pages/TipCoachPage';
import RecommendationsPage from './pages/RecommendationsPage';
import TipScenarioPage from './pages/TipScenarioPage';
import TipEvolutionPage from './pages/TipEvolutionPage';
import TipAnalyticsPage from './pages/TipAnalyticsPage';
import TipForecastPage from './pages/TipForecastPage';
import TipGoalsPage from './pages/TipGoalsPage';
import TipInsightsPage from './pages/TipInsightsPage';
import AchievementsPage from './pages/AchievementsPage';
import TipPoolPage from './pages/TipPoolPage';
import TipProfilePage from './pages/TipProfilePage';
import TipDataQualityPage from './pages/TipDataQualityPage';
import { Box, CircularProgress } from '@mui/material';
import RenderSleepNotifier from './components/RenderSleepNotifier';

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
        <CircularProgress size={48} sx={{ color: '#fd5b38' }} />
      </Box>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

// Wrapper that combines ProtectedRoute and AppShell
const ProtectedShell = ({ children }) => {
  return (
    <ProtectedRoute>
      <AppShell>{children}</AppShell>
    </ProtectedRoute>
  );
};

const App = () => {
  return (
    <>
      <Routes>
        <Route path="/" element={<LandingPage />} />

        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

        <Route path="/dashboard" element={<ProtectedShell><DashboardPage /></ProtectedShell>} />
        <Route path="/tips" element={<ProtectedShell><TipCalculatorPage /></ProtectedShell>} />
        <Route path="/tip-history" element={<ProtectedShell><TipHistoryPage /></ProtectedShell>} />
        <Route path="/coach" element={<ProtectedShell><TipCoachPage /></ProtectedShell>} />
        <Route path="/recommendations" element={<ProtectedShell><RecommendationsPage /></ProtectedShell>} />
        <Route path="/tip-scenarios" element={<ProtectedShell><TipScenarioPage /></ProtectedShell>} />
        <Route path="/tip-evolution" element={<ProtectedShell><TipEvolutionPage /></ProtectedShell>} />
        <Route path="/analytics" element={<ProtectedShell><TipAnalyticsPage /></ProtectedShell>} />
        <Route path="/tip-forecast" element={<ProtectedShell><TipForecastPage /></ProtectedShell>} />
        <Route path="/tip-goals" element={<ProtectedShell><TipGoalsPage /></ProtectedShell>} />
        <Route path="/tip-insights" element={<ProtectedShell><TipInsightsPage /></ProtectedShell>} />
        <Route path="/achievements" element={<ProtectedShell><AchievementsPage /></ProtectedShell>} />
        <Route path="/tip-pools" element={<ProtectedShell><TipPoolPage /></ProtectedShell>} />
        <Route path="/tip-profile" element={<ProtectedShell><TipProfilePage /></ProtectedShell>} />
        <Route path="/data-quality" element={<ProtectedShell><TipDataQualityPage /></ProtectedShell>} />

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <RenderSleepNotifier />
    </>
  );
};

export default App;
