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
      main: '#fd5b38',      // Vibrant orange — matching internal UI
      light: '#ff8a65',
      dark: '#e04826',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#ffffff',       // White secondary for neutral look
      light: '#f5f5f5',
      dark: '#e0e0e0',
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
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 800,
      letterSpacing: '-0.02em',
      fontSize: '2rem',
    },
    h2: {
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.01em',
      fontSize: '1.75rem',
    },
    h3: {
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 700,
      fontSize: '1.5rem',
    },
    h4: {
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 600,
      fontSize: '1.25rem',
    },
    h5: {
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 600,
      fontSize: '1.1rem',
    },
    h6: {
      fontFamily: '"Outfit", sans-serif',
      fontWeight: 600,
      fontSize: '1rem',
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
          padding: '6px 16px',
          fontSize: '0.85rem',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-1px)',
            boxShadow: '0 4px 20px rgba(253, 91, 56, 0.3)',
          },
        },
        contained: {
          background: 'linear-gradient(135deg, #fd5b38 0%, #e04826 100%)',
          '&:hover': {
            background: 'linear-gradient(135deg, #ff8a65 0%, #fd5b38 100%)',
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
                borderColor: '#fd5b38',
              },
            },
            '&.Mui-focused': {
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#fd5b38',
                boxShadow: '0 0 0 3px rgba(253, 91, 56, 0.15)',
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
