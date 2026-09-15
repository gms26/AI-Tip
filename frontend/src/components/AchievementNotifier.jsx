import React, { useState, useEffect } from 'react';
import { Snackbar, Alert, Box, Typography } from '@mui/material';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';

const AchievementNotifier = () => {
  const [queue, setQueue] = useState([]);
  const [open, setOpen] = useState(false);
  const [current, setCurrent] = useState(null);

  useEffect(() => {
    const handleAchievementUnlock = (e) => {
      const achievements = e.detail; // Expecting array of unlocked achievements
      if (achievements && achievements.length > 0) {
        setQueue(prev => [...prev, ...achievements]);
      }
    };

    window.addEventListener('achievements-unlocked', handleAchievementUnlock);
    return () => window.removeEventListener('achievements-unlocked', handleAchievementUnlock);
  }, []);

  useEffect(() => {
    if (queue.length > 0 && !current) {
      setCurrent(queue[0]);
      setQueue(prev => prev.slice(1));
      setOpen(true);
    } else if (queue.length > 0 && current && !open) {
      setCurrent(queue[0]);
      setQueue(prev => prev.slice(1));
      setOpen(true);
    }
  }, [queue, current, open]);

  const handleClose = (event, reason) => {
    if (reason === 'clickaway') return;
    setOpen(false);
  };

  const handleExited = () => {
    setCurrent(null);
  };

  return (
    <Snackbar
      open={open}
      autoHideDuration={6000}
      onClose={handleClose}
      TransitionProps={{ onExited: handleExited }}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert 
        onClose={handleClose} 
        icon={<EmojiEventsIcon sx={{ color: '#ffffff' }} />}
        sx={{ 
          background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.95) 0%, rgba(214, 61, 25, 0.95) 100%)',
          color: '#fff',
          backdropFilter: 'blur(10px)',
          borderRadius: 2,
          border: '1px solid rgba(255,255,255,0.2)'
        }}
      >
        <Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 800 }}>
            🎉 Achievement Unlocked!
          </Typography>
          {current && (
            <Typography variant="body2" sx={{ mt: 0.5 }}>
              <strong>{current.name}</strong><br />
              {current.description}
            </Typography>
          )}
        </Box>
      </Alert>
    </Snackbar>
  );
};

export default AchievementNotifier;
