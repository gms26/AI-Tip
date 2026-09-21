import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Box,
  TextField,
  Button,
  Typography,
  Link,
  Snackbar,
  Alert,
  InputAdornment,
  IconButton,
  CircularProgress,
  Grid,
} from '@mui/material';
import {
  Email as EmailIcon,
  Lock as LockIcon,
  Visibility,
  VisibilityOff,
  AutoAwesome as SparkleIcon,
  ArrowBack as ArrowBackIcon,
  Lightbulb as LightbulbIcon,
} from '@mui/icons-material';
import { motion } from 'framer-motion';
import PtLogo from '../components/PtLogo';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success',
  });

  const validate = () => {
    const newErrors = {};
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^\S+@\S+\.\S+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    if (!formData.password) {
      newErrors.password = 'Password is required';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    try {
      await login(formData);
      setSnackbar({
        open: true,
        message: 'Login successful! Redirecting...',
        severity: 'success',
      });
      setTimeout(() => navigate('/dashboard'), 800);
    } catch (error) {
      const message =
        error.response?.data?.message ||
        'Login failed. Please check your credentials.';
      setSnackbar({ open: true, message, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        py: 8,
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

      <Button
        component={RouterLink}
        to="/"
        startIcon={<ArrowBackIcon />}
        sx={{
          position: 'absolute',
          top: 32,
          left: 32,
          color: '#9AA0A6',
          zIndex: 10,
          fontWeight: 600,
          backgroundColor: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(10px)',
          '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.1)', color: '#fff' },
        }}
      >
        Back to Home
      </Button>

      {/* Glassmorphic Card */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, type: 'spring' }}
        style={{ zIndex: 1, width: '100%', maxWidth: '450px', margin: '0 24px' }}
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
          }}
        >
          <PtLogo size={64} showText={false} sx={{ mb: 2 }} />
          <Typography
            variant="h4"
            sx={{
              fontWeight: 900,
              mb: 1,
              fontFamily: "'Outfit', 'Inter', sans-serif",
              color: '#fff',
            }}
          >
            Welcome Back
          </Typography>
          <Typography variant="body1" sx={{ color: '#9AA0A6', mb: 4, fontWeight: 400, textAlign: 'center' }}>
            Sign in to Possible Tip
          </Typography>

          <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }} noValidate>
            <TextField
              fullWidth
              id="login-email"
              name="email"
              label="Email Address"
              type="email"
              value={formData.email}
              onChange={handleChange}
              error={!!errors.email}
              helperText={errors.email}
              autoComplete="email"
              autoFocus
              sx={{
                mb: 3,
                '& .MuiOutlinedInput-root': {
                  backgroundColor: 'rgba(0, 0, 0, 0.2)',
                  borderRadius: 3,
                  '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.1)' },
                  '&:hover fieldset': { borderColor: 'rgba(255, 255, 255, 0.2)' },
                  '&.Mui-focused fieldset': { borderColor: '#fd5b38' },
                },
                '& .MuiInputLabel-root': { color: '#9AA0A6' },
                '& .MuiInputBase-input': { color: '#E8EAED', fontWeight: 500 },
              }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <EmailIcon sx={{ color: '#9AA0A6', fontSize: 20 }} />
                    </InputAdornment>
                  ),
                }
              }}
            />

            <TextField
              fullWidth
              id="login-password"
              name="password"
              label="Password"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
              onChange={handleChange}
              error={!!errors.password}
              helperText={errors.password}
              autoComplete="current-password"
              sx={{
                mb: 4,
                '& .MuiOutlinedInput-root': {
                  backgroundColor: 'rgba(0, 0, 0, 0.2)',
                  borderRadius: 3,
                  '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.1)' },
                  '&:hover fieldset': { borderColor: 'rgba(255, 255, 255, 0.2)' },
                  '&.Mui-focused fieldset': { borderColor: '#fd5b38' },
                },
                '& .MuiInputLabel-root': { color: '#9AA0A6' },
                '& .MuiInputBase-input': { color: '#E8EAED', fontWeight: 500 },
              }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockIcon sx={{ color: '#9AA0A6', fontSize: 20 }} />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                        size="small"
                        sx={{ color: '#666' }}
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }
              }}
            />

            <Button
              id="login-submit"
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{
                py: 1.8,
                borderRadius: 3,
                textTransform: 'none',
                fontSize: '1.1rem',
                fontWeight: 700,
                fontFamily: "'Outfit', 'Inter', sans-serif",
                background: 'linear-gradient(135deg, #fd5b38 0%, #ff8a65 100%)',
                boxShadow: '0 8px 20px rgba(253, 91, 56, 0.3)',
                mb: 3,
                '&:hover': {
                  background: 'linear-gradient(135deg, #e04826 0%, #fd5b38 100%)',
                  boxShadow: '0 12px 24px rgba(253, 91, 56, 0.4)',
                },
              }}
            >
              {loading ? <CircularProgress size={24} sx={{ color: '#fff' }} /> : 'Sign In'}
            </Button>

            <Typography variant="body2" sx={{ textAlign: 'center', color: '#9AA0A6', fontWeight: 500 }}>
              Don&apos;t have an account?{' '}
              <Link
                component={RouterLink}
                to="/register"
                sx={{
                  color: '#fd5b38',
                  fontWeight: 700,
                  textDecoration: 'none',
                  '&:hover': { textDecoration: 'underline' },
                }}
              >
                Create one
              </Link>
            </Typography>
          </Box>
        </Box>
      </motion.div>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%', borderRadius: 2 }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default LoginPage;
