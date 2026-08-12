/**
 * Application Entry Point
 *
 * PURPOSE:
 * Mounts the React app and wraps it with all required providers.
 *
 * PROVIDER ORDER (outermost → innermost):
 * 1. StrictMode — development-only checks for common bugs
 * 2. ThemeProvider — MUI theme (colors, typography, components)
 * 3. CssBaseline — normalizes CSS across browsers
 * 4. BrowserRouter — enables client-side routing
 * 5. AuthProvider — provides auth state to all components
 * 6. App — the actual application routes and pages
 *
 * WHY this order?
 * - Theme must wrap everything that uses MUI components
 * - BrowserRouter must wrap anything that uses React Router hooks
 * - AuthProvider must wrap anything that uses useAuth()
 * - App is the innermost because it consumes all providers
 */
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import theme from './theme';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>
);
