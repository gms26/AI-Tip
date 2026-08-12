/**
 * Register Page
 *
 * PURPOSE:
 * Creates new user accounts. Matches the premium design language
 * of the login page with additional fields and confirm password.
 *
 * FEATURES:
 * - Name, email, password, confirm password fields
 * - Real-time client-side validation
 * - Password strength indicator
 * - Loading state with spinner
 * - Snackbar notifications
 * - Redirects to dashboard on success
 *
 * WHY confirm password on frontend only?
 * The backend doesn't need a confirmPassword field.
 * It's purely a UX safeguard against typos.
 */
import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Link,
  Snackbar,
  Alert,
  InputAdornment,
  IconButton,
  CircularProgress,
  LinearProgress,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Lock as LockIcon,
  Visibility,
  VisibilityOff,
  AutoAwesome as SparkleIcon,
} from '@mui/icons-material';

/**
 * Calculates password strength score (0–100).
 * WHY? Visual feedback encourages stronger passwords.
 */
const getPasswordStrength = (password) => {
  let score = 0;
  if (password.length >= 8) score += 25;
  if (password.length >= 12) score += 15;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 20;
  if (/\d/.test(password)) score += 20;
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score += 20;
  return Math.min(score, 100);
};

const getStrengthColor = (score) => {
  if (score < 30) return '#FF5252';
  if (score < 60) return '#FFD740';
  if (score < 80) return '#00D9FF';
  return '#00E676';
};

const getStrengthLabel = (score) => {
  if (score < 30) return 'Weak';
  if (score < 60) return 'Fair';
  if (score < 80) return 'Good';
  return 'Strong';
};

const RegisterPage = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success',
  });

  const passwordStrength = getPasswordStrength(formData.password);

  const validate = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    } else if (formData.name.trim().length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
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
      // Only send name, email, password — not confirmPassword
      await register({
        name: formData.name,
        email: formData.email,
        password: formData.password,
      });
      setSnackbar({
        open: true,
        message: 'Account created successfully! Redirecting...',
        severity: 'success',
      });
      setTimeout(() => navigate('/dashboard'), 800);
    } catch (error) {
      const data = error.response?.data;
      let message = 'Registration failed. Please try again.';

      if (data?.errors) {
        // Field-level validation errors from backend
        setErrors(data.errors);
        message = data.message || message;
      } else if (data?.message) {
        message = data.message;
      }

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
        background: 'linear-gradient(135deg, #0A0E1A 0%, #1a1040 50%, #0A0E1A 100%)',
        position: 'relative',
        overflow: 'hidden',
        px: 2,
        py: 4,
      }}
    >
      {/* Animated background orbs */}
      <Box
        sx={{
          position: 'absolute',
          width: 350,
          height: 350,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(0, 217, 255, 0.12) 0%, transparent 70%)',
          top: -80,
          left: -80,
          animation: 'float 12s ease-in-out infinite',
          '@keyframes float': {
            '0%, 100%': { transform: 'translateY(0) scale(1)' },
            '50%': { transform: 'translateY(-30px) scale(1.1)' },
          },
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          width: 400,
          height: 400,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(108, 99, 255, 0.12) 0%, transparent 70%)',
          bottom: -100,
          right: -100,
          animation: 'float 15s ease-in-out infinite reverse',
        }}
      />

      <Card
        sx={{
          maxWidth: 440,
          width: '100%',
          background: 'rgba(18, 24, 41, 0.75)',
          backdropFilter: 'blur(24px)',
          border: '1px solid rgba(108, 99, 255, 0.15)',
          borderRadius: 3,
          boxShadow: '0 24px 80px rgba(0, 0, 0, 0.5), 0 0 40px rgba(108, 99, 255, 0.08)',
          position: 'relative',
          zIndex: 1,
        }}
      >
        <CardContent sx={{ p: { xs: 3, sm: 4.5 } }}>
          {/* Header */}
          <Box sx={{ textAlign: 'center', mb: 3.5 }}>
            <Box
              sx={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 56,
                height: 56,
                borderRadius: 2.5,
                background: 'linear-gradient(135deg, #00D9FF 0%, #0088AA 100%)',
                mb: 2,
                boxShadow: '0 8px 24px rgba(0, 217, 255, 0.25)',
              }}
            >
              <SparkleIcon sx={{ fontSize: 28, color: '#fff' }} />
            </Box>
            <Typography variant="h4" sx={{ fontWeight: 700, color: '#E8EAED', mb: 0.5 }}>
              Create Account
            </Typography>
            <Typography variant="body2" sx={{ color: '#9AA0A6' }}>
              Join AI Tip Assistant today
            </Typography>
          </Box>

          {/* Form */}
          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              fullWidth
              id="register-name"
              name="name"
              label="Full Name"
              value={formData.name}
              onChange={handleChange}
              error={!!errors.name}
              helperText={errors.name}
              autoComplete="name"
              autoFocus
              sx={{ mb: 2 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon sx={{ color: '#00D9FF', fontSize: 20 }} />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              fullWidth
              id="register-email"
              name="email"
              label="Email Address"
              type="email"
              value={formData.email}
              onChange={handleChange}
              error={!!errors.email}
              helperText={errors.email}
              autoComplete="email"
              sx={{ mb: 2 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon sx={{ color: '#00D9FF', fontSize: 20 }} />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              fullWidth
              id="register-password"
              name="password"
              label="Password"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
              onChange={handleChange}
              error={!!errors.password}
              helperText={errors.password || 'Minimum 8 characters'}
              autoComplete="new-password"
              sx={{ mb: 0.5 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon sx={{ color: '#00D9FF', fontSize: 20 }} />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                      size="small"
                      sx={{ color: '#9AA0A6' }}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            {/* Password Strength Indicator */}
            {formData.password && (
              <Box sx={{ mb: 2, px: 0.5 }}>
                <LinearProgress
                  variant="determinate"
                  value={passwordStrength}
                  sx={{
                    height: 4,
                    borderRadius: 2,
                    backgroundColor: 'rgba(255, 255, 255, 0.08)',
                    '& .MuiLinearProgress-bar': {
                      backgroundColor: getStrengthColor(passwordStrength),
                      borderRadius: 2,
                      transition: 'all 0.4s ease',
                    },
                  }}
                />
                <Typography
                  variant="caption"
                  sx={{
                    color: getStrengthColor(passwordStrength),
                    mt: 0.5,
                    display: 'block',
                    fontWeight: 500,
                  }}
                >
                  Password strength: {getStrengthLabel(passwordStrength)}
                </Typography>
              </Box>
            )}

            <TextField
              fullWidth
              id="register-confirm-password"
              name="confirmPassword"
              label="Confirm Password"
              type={showConfirmPassword ? 'text' : 'password'}
              value={formData.confirmPassword}
              onChange={handleChange}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword}
              autoComplete="new-password"
              sx={{ mb: 3 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon sx={{ color: '#00D9FF', fontSize: 20 }} />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      edge="end"
                      size="small"
                      sx={{ color: '#9AA0A6' }}
                    >
                      {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Button
              id="register-submit"
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{
                py: 1.5,
                fontSize: '1rem',
                fontWeight: 600,
                mb: 3,
                background: 'linear-gradient(135deg, #00D9FF 0%, #0088AA 100%)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #33E1FF 0%, #00D9FF 100%)',
                  boxShadow: '0 4px 20px rgba(0, 217, 255, 0.3)',
                },
              }}
            >
              {loading ? (
                <CircularProgress size={24} sx={{ color: '#fff' }} />
              ) : (
                'Create Account'
              )}
            </Button>

            <Typography
              variant="body2"
              sx={{ textAlign: 'center', color: '#9AA0A6' }}
            >
              Already have an account?{' '}
              <Link
                component={RouterLink}
                to="/login"
                sx={{
                  color: '#00D9FF',
                  fontWeight: 600,
                  textDecoration: 'none',
                  '&:hover': {
                    textDecoration: 'underline',
                    color: '#33E1FF',
                  },
                }}
              >
                Sign in
              </Link>
            </Typography>
          </Box>
        </CardContent>
      </Card>

      {/* Snackbar */}
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

export default RegisterPage;
