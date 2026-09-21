/**
 * 404 Not Found Page
 *
 * PURPOSE:
 * Catch-all page for undefined routes. Provides a clear message
 * and navigation back to the app.
 *
 * WHY a custom 404?
 * - Browser default is ugly and confusing
 * - Keeps users within the app
 * - Provides a clear action (go home / go back)
 */
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';
import {
  SentimentDissatisfied as SadIcon,
  Home as HomeIcon,
} from '@mui/icons-material';

const NotFoundPage = () => {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #0A0E1A 0%, #121829 100%)',
        px: 2,
        textAlign: 'center',
      }}
    >
      <SadIcon
        sx={{
          fontSize: 80,
          color: '#fd5b38',
          mb: 3,
          opacity: 0.7,
        }}
      />

      <Typography
        variant="h1"
        sx={{
          fontWeight: 800,
          fontSize: { xs: '4rem', sm: '6rem' },
          background: 'linear-gradient(135deg, #fd5b38, #ff8a65)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          lineHeight: 1,
          mb: 2,
        }}
      >
        404
      </Typography>

      <Typography
        variant="h5"
        sx={{ fontWeight: 600, color: '#E8EAED', mb: 1 }}
      >
        Page Not Found
      </Typography>

      <Typography
        variant="body1"
        sx={{ color: '#9AA0A6', mb: 4, maxWidth: 400 }}
      >
        The page you&apos;re looking for doesn&apos;t exist or has been moved.
      </Typography>

      <Button
        variant="contained"
        startIcon={<HomeIcon />}
        onClick={() => navigate('/dashboard')}
        size="large"
        sx={{ px: 4 }}
      >
        Go to Dashboard
      </Button>
    </Box>
  );
};

export default NotFoundPage;
