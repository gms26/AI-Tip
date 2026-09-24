import React, { useState, useEffect } from 'react';
import { Snackbar, Alert, Typography, Box, CircularProgress } from '@mui/material';

const RenderSleepNotifier = () => {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handleSlowRequest = () => setOpen(true);
    const handleRequestCompleted = () => setOpen(false);

    window.addEventListener('api-slow-request', handleSlowRequest);
    window.addEventListener('api-request-completed', handleRequestCompleted);

    return () => {
      window.removeEventListener('api-slow-request', handleSlowRequest);
      window.removeEventListener('api-request-completed', handleRequestCompleted);
    };
  }, []);

  return (
    <Snackbar
      open={open}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      sx={{ bottom: { xs: 24, sm: 24 }, zIndex: 9999 }}
    >
      <Alert 
        severity="info" 
        icon={<CircularProgress size={24} sx={{ color: '#fd5b38' }} />}
        sx={{ 
          backgroundColor: 'rgba(21, 22, 29, 0.95)',
          color: '#ffffff',
          border: '1px solid rgba(253, 91, 56, 0.3)',
          backdropFilter: 'blur(10px)',
          borderRadius: '12px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
          alignItems: 'center',
          '& .MuiAlert-icon': {
            alignItems: 'center',
            mr: 2
          }
        }}
      >
        <Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 800, color: '#fd5b38', mb: 0.5 }}>
            Server is Waking Up...
          </Typography>
          <Typography variant="body2" sx={{ color: '#c6c8c9', fontSize: '0.85rem', lineHeight: 1.4 }}>
            The backend is hosted on a free Render tier and is spinning up from sleep. This usually takes about 30 to 50 seconds. Please hang tight!
          </Typography>
        </Box>
      </Alert>
    </Snackbar>
  );
};

export default RenderSleepNotifier;
