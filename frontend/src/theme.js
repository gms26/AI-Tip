/**
 * Custom Material UI Theme — Premium Dark Mode
 *
 * PURPOSE:
 * Defines the visual identity of the AI Tip Assistant.
 * Uses a modern dark theme with vibrant accent colors,
 * glassmorphism effects, and professional typography.
 *
 * WHY A CUSTOM THEME?
 * - MUI's default theme looks generic and unprofessional
 * - Custom palette creates a premium, cohesive feel
 * - Typography with Inter font improves readability
 * - Consistent design tokens reduce ad-hoc styling
 *
 * WHY DARK MODE?
 * - Modern, premium aesthetic
 * - Easier on the eyes for extended use
 * - Makes accent colors pop more
 */
import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#6C63FF',      // Vibrant indigo — modern and premium
      light: '#8B83FF',
      dark: '#4A42D4',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#00D9FF',       // Cyan accent — eye-catching CTAs
      light: '#33E1FF',
      dark: '#00A8CC',
      contrastText: '#000000',
    },
    background: {
      default: '#0A0E1A',   // Deep navy — rich dark background
      paper: '#121829',     // Slightly lighter for cards/surfaces
    },
    error: {
      main: '#FF5252',
      light: '#FF8A80',
    },
    success: {
      main: '#00E676',
      light: '#69F0AE',
    },
    warning: {
      main: '#FFD740',
    },
    text: {
      primary: '#E8EAED',
      secondary: '#9AA0A6',
    },
    divider: 'rgba(255, 255, 255, 0.08)',
  },

  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 800,
      letterSpacing: '-0.02em',
    },
    h2: {
      fontWeight: 700,
      letterSpacing: '-0.01em',
    },
    h3: {
      fontWeight: 700,
    },
    h4: {
      fontWeight: 600,
    },
    h5: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 600,
    },
    button: {
      fontWeight: 600,
      textTransform: 'none', // No uppercase — more modern
      letterSpacing: '0.02em',
    },
  },

  shape: {
    borderRadius: 12, // Rounded corners — modern look
  },

  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          padding: '10px 24px',
          fontSize: '0.95rem',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-1px)',
            boxShadow: '0 4px 20px rgba(108, 99, 255, 0.3)',
          },
        },
        contained: {
          background: 'linear-gradient(135deg, #6C63FF 0%, #4A42D4 100%)',
          '&:hover': {
            background: 'linear-gradient(135deg, #8B83FF 0%, #6C63FF 100%)',
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 10,
            transition: 'all 0.3s ease',
            '&:hover': {
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#6C63FF',
              },
            },
            '&.Mui-focused': {
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#6C63FF',
                boxShadow: '0 0 0 3px rgba(108, 99, 255, 0.15)',
              },
            },
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none', // Remove MUI's default gradient on Paper
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          background: 'rgba(18, 24, 41, 0.7)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.06)',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 10,
        },
      },
    },
  },
});

export default theme;
