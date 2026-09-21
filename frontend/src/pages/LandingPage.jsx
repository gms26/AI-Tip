import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import PtLogo from '../components/PtLogo';

const LandingPage = () => {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        py: 4,
      }}
    >
      {/* Animated Dark Gradient Background */}
      <motion.div
        animate={{
          background: [
            'linear-gradient(135deg, #0A0E1A 0%, #15161d 100%)',
            'linear-gradient(135deg, #15161d 0%, #1a1c23 100%)',
            'linear-gradient(135deg, #0A0E1A 0%, #11131a 100%)',
            'linear-gradient(135deg, #0A0E1A 0%, #15161d 100%)',
          ],
        }}
        transition={{ duration: 15, repeat: Infinity, ease: 'linear' }}
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 0,
        }}
      />

      {/* Glassmorphic Card */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, type: 'spring' }}
        style={{ zIndex: 1, width: '100%', maxWidth: '500px', margin: '0 24px' }}
      >
        <Box
          sx={{
            background: 'rgba(10, 14, 26, 0.4)',
            backdropFilter: 'blur(20px)',
            borderRadius: '32px',
            boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.3)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            p: { xs: 4, sm: 6 },
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            textAlign: 'center',
          }}
        >
          <PtLogo size={72} showText={false} sx={{ mb: 3 }} />
          <Typography
            variant="h3"
            sx={{
              fontWeight: 900,
              mb: 2,
              fontFamily: "'Outfit', 'Inter', sans-serif",
              color: '#fff',
            }}
          >
            Possible Tip
          </Typography>
          <Typography variant="body1" sx={{ color: '#9AA0A6', mb: 5, fontWeight: 400, lineHeight: 1.6 }}>
            Intelligent gratuity assistant, precise calculations, and personalized insights for your dining experiences.
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, width: '100%' }}>
            <Button
              fullWidth
              variant="contained"
              size="large"
              onClick={() => navigate('/login')}
              sx={{
                py: 1.8,
                borderRadius: 3,
                textTransform: 'none',
                fontSize: '1.1rem',
                fontWeight: 700,
                background: 'linear-gradient(135deg, #fd5b38 0%, #ff8a65 100%)',
                boxShadow: '0 8px 20px rgba(253, 91, 56, 0.3)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #e04826 0%, #fd5b38 100%)',
                  boxShadow: '0 12px 24px rgba(253, 91, 56, 0.4)',
                },
              }}
            >
              Sign In
            </Button>
            <Button
              fullWidth
              variant="outlined"
              size="large"
              onClick={() => navigate('/register')}
              sx={{
                py: 1.8,
                borderRadius: 3,
                textTransform: 'none',
                fontSize: '1.1rem',
                fontWeight: 700,
                color: '#fff',
                borderColor: 'rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  borderColor: '#fff',
                  background: 'rgba(255, 255, 255, 0.05)',
                },
              }}
            >
              Create Account
            </Button>
          </Box>
        </Box>
      </motion.div>
    </Box>
  );
};

export default LandingPage;
