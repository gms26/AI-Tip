import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  LinearProgress,
  Chip,
  Alert,
  CircularProgress
} from '@mui/material';
import { goalApi } from '../api/goalApi';
import TrackChangesIcon from '@mui/icons-material/TrackChanges';
import AddIcon from '@mui/icons-material/Add';

const GOAL_TYPES = {
  TOTAL_TIP_AMOUNT: 'Total Tip Amount',
  TIP_COUNT: 'Tip Count',
  AVERAGE_TIP_PERCENTAGE: 'Average Tip Percentage',
  MEDIAN_TIP_PERCENTAGE: 'Median Tip Percentage',
  RESTAURANT_EXPLORATION: 'Restaurant Exploration',
  SERVICE_QUALITY: 'Service Quality'
};

const PERIODS = {
  CURRENT_MONTH: 'Current Month',
  CURRENT_YEAR: 'Current Year',
  CUSTOM: 'Custom',
  ALL_TIME: 'All Time'
};

const SERVICE_QUALITIES = ['POOR', 'AVERAGE', 'GOOD', 'EXCELLENT'];

const TipGoalsPage = () => {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [open, setOpen] = useState(false);
  const [formData, setFormData] = useState({
    goalType: 'TOTAL_TIP_AMOUNT',
    targetValue: '',
    currency: 'USD',
    period: 'CURRENT_MONTH',
    startDate: '',
    endDate: '',
    restaurantName: '',
    serviceQuality: 'GOOD'
  });
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    fetchGoals();
  }, []);

  const fetchGoals = async () => {
    try {
      setLoading(true);
      const data = await goalApi.getGoals();
      setGoals(data);
      setError('');
    } catch (err) {
      setError('Failed to fetch goals. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpen = () => setOpen(true);
  const handleClose = () => {
    setOpen(false);
    setSubmitError('');
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError('');
    try {
      const payload = { ...formData };
      
      // Clean up irrelevant fields based on goalType/period
      if (payload.period !== 'CUSTOM') {
        payload.startDate = null;
        payload.endDate = null;
      }
      if (payload.goalType !== 'TOTAL_TIP_AMOUNT' && payload.goalType !== 'AVERAGE_TIP_PERCENTAGE' && payload.goalType !== 'MEDIAN_TIP_PERCENTAGE') {
        payload.currency = null;
      }
      if (payload.goalType !== 'RESTAURANT_EXPLORATION') {
        // If not strictly exploration, optionally they can still scope by restaurant, but let's just pass it
      }
      if (payload.goalType !== 'SERVICE_QUALITY') {
        payload.serviceQuality = null;
      }

      await goalApi.createGoal(payload);
      handleClose();
      fetchGoals();
    } catch (err) {
      if (err.response?.status === 409) {
        setSubmitError('An active goal with this exact configuration already exists.');
      } else {
        setSubmitError(err.response?.data?.message || 'Failed to create goal.');
      }
    }
  };

  const handleCancel = async (id) => {
    try {
      await goalApi.cancelGoal(id);
      fetchGoals();
    } catch (err) {
      setError('Failed to cancel goal.');
    }
  };

  const formatValue = (val, type, currency) => {
    if (type === 'TOTAL_TIP_AMOUNT') return `${currency || ''} ${Number(val).toFixed(2)}`;
    if (type === 'AVERAGE_TIP_PERCENTAGE' || type === 'MEDIAN_TIP_PERCENTAGE') return `${Number(val).toFixed(2)}%`;
    return val;
  };

  return (
    <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 }, width: '100%', maxWidth: 1300, mx: 'auto' }}>
      {/* ═══ Page Header ═══ */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, mb: 4, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.8,
              background: 'linear-gradient(135deg, #fd5b38 0%, #ff8b70 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 18px rgba(253, 91, 56, 0.3)',
            }}
          >
            <TrackChangesIcon sx={{ fontSize: 24, color: '#FFFFFF' }} />
          </Box>
          <Box>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 800, letterSpacing: '0.12em', color: '#fd5b38', textTransform: 'uppercase' }}>
              TARGET OBJECTIVES
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#F8FAFC', letterSpacing: '-0.025em' }}>
              Smart Tipping Goals
            </Typography>
          </Box>
        </Box>
        <Button
          variant="contained"
          onClick={handleOpen}
          startIcon={<AddIcon />}
          sx={{
            borderRadius: '30px',
            px: 3.5,
            py: 1.2,
            fontWeight: 700,
            background: 'linear-gradient(135deg, #fd5b38 0%, #e04826 100%)',
            color: '#ffffff',
            boxShadow: '0 4px 18px rgba(253, 91, 56, 0.35)',
            '&:hover': {
              background: '#e04826',
              boxShadow: '0 8px 28px rgba(253, 91, 56, 0.5)',
            }
          }}
        >
          Create Goal
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
          <CircularProgress sx={{ color: '#fd5b38' }} />
        </Box>
      ) : goals.length === 0 ? (
        <Card sx={{ 
          background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))',
          backdropFilter: 'blur(16px)',
          border: '1px solid rgba(253, 91, 56, 0.25)',
          borderRadius: 3.5,
          p: 4,
          textAlign: 'center'
        }}>
          <TrackChangesIcon sx={{ fontSize: 48, color: '#fd5b38', mb: 2, opacity: 0.8 }} />
          <Typography variant="h6" sx={{ color: '#F8FAFC', fontWeight: 700, mb: 1 }}>
            No Active Tipping Goals
          </Typography>
          <Typography variant="body2" sx={{ color: '#94A3B8', mb: 3, maxWidth: 500, mx: 'auto' }}>
            Set a tip budget target, percentage benchmark, or restaurant exploration milestone to keep your spending intentional.
          </Typography>
          <Button
            variant="contained"
            onClick={handleOpen}
            startIcon={<AddIcon />}
            sx={{
              borderRadius: '30px',
              px: 3.5,
              py: 1,
              fontWeight: 700,
              background: 'linear-gradient(135deg, #fd5b38 0%, #e04826 100%)',
              color: '#ffffff'
            }}
          >
            Create Your First Goal
          </Button>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {goals.map((goal) => (
            <Grid item xs={12} md={6} key={goal.id}>
              <Card sx={{ 
                background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))',
                backdropFilter: 'blur(16px)',
                border: '1px solid rgba(253, 91, 56, 0.25)',
                borderRadius: 3.5,
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.35)'
              }}>
                <CardContent sx={{ p: { xs: 2.5, md: 3 } }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                    <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 700, fontSize: '1.15rem' }}>
                      {GOAL_TYPES[goal.goalType]}
                    </Typography>
                    <Chip 
                      label={goal.status} 
                      sx={{
                        fontWeight: 800,
                        fontSize: '0.72rem',
                        height: 24,
                        borderRadius: '12px',
                        background: goal.status === 'COMPLETED' ? 'rgba(0, 230, 118, 0.15)' :
                          goal.status === 'ACTIVE' ? 'rgba(253, 91, 56, 0.15)' :
                          goal.status === 'EXPIRED' ? 'rgba(255, 215, 64, 0.15)' : 'rgba(255, 255, 255, 0.08)',
                        color: goal.status === 'COMPLETED' ? '#00e676' :
                          goal.status === 'ACTIVE' ? '#fd5b38' :
                          goal.status === 'EXPIRED' ? '#ffd740' : '#c6c8c9',
                        border: `1px solid ${
                          goal.status === 'COMPLETED' ? 'rgba(0, 230, 118, 0.3)' :
                          goal.status === 'ACTIVE' ? 'rgba(253, 91, 56, 0.3)' :
                          goal.status === 'EXPIRED' ? 'rgba(255, 215, 64, 0.3)' : 'rgba(255, 255, 255, 0.1)'
                        }`
                      }}
                      size="small" 
                    />
                  </Box>
                  
                  <Typography variant="body2" sx={{ color: '#94A3B8', mb: 2 }}>
                    {PERIODS[goal.period]} {goal.period === 'CUSTOM' && `(${goal.startDate} to ${goal.endDate})`}
                  </Typography>

                  <Box sx={{ my: 2.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2" sx={{ color: '#E8EAED', fontWeight: 600 }}>
                        {formatValue(goal.currentValue, goal.goalType, goal.currency)} / {formatValue(goal.targetValue, goal.goalType, goal.currency)}
                      </Typography>
                      <Typography variant="body2" sx={{ color: '#fd5b38', fontWeight: 800 }}>
                        {goal.progressPercentage}%
                      </Typography>
                    </Box>
                    <LinearProgress 
                      variant="determinate" 
                      value={Math.min(100, Number(goal.progressPercentage))} 
                      sx={{
                        height: 10,
                        borderRadius: 5,
                        backgroundColor: 'rgba(255, 255, 255, 0.08)',
                        '& .MuiLinearProgress-bar': {
                          background: goal.status === 'COMPLETED' 
                            ? 'linear-gradient(90deg, #00e676, #69f0ae)'
                            : 'linear-gradient(90deg, #fd5b38, #ff8a65)',
                          borderRadius: 5
                        }
                      }}
                    />
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', pt: 1 }}>
                    <Box>
                      {goal.status === 'ACTIVE' && (
                        <Typography variant="body2" sx={{ color: '#94A3B8', fontWeight: 500 }}>
                          {formatValue(goal.remainingValue, goal.goalType, goal.currency)} remaining
                        </Typography>
                      )}
                      <Typography variant="caption" display="block" sx={{ color: '#64748B', mt: 0.5 }}>
                        Confidence: {goal.confidence} (Sample: {goal.sampleSize})
                      </Typography>
                      {goal.restaurantName && (
                        <Typography variant="caption" display="block" sx={{ color: '#64748B' }}>
                          Restaurant: {goal.restaurantName}
                        </Typography>
                      )}
                    </Box>
                    
                    {goal.status === 'ACTIVE' && (
                      <Button 
                        size="small" 
                        onClick={() => handleCancel(goal.id)}
                        sx={{ 
                          color: '#fb7185',
                          fontWeight: 600,
                          fontSize: '0.82rem',
                          textTransform: 'none',
                          '&:hover': { color: '#ff4d6d', backgroundColor: 'rgba(251, 113, 133, 0.08)' }
                        }}
                      >
                        Cancel Goal
                      </Button>
                    )}
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Create Goal Dialog */}
      <Dialog 
        open={open} 
        onClose={handleClose} 
        maxWidth="sm" 
        fullWidth 
        PaperProps={{
          sx: {
            background: 'rgba(15, 18, 30, 0.98)',
            backdropFilter: 'blur(24px)',
            border: '1px solid rgba(253, 91, 56, 0.25)',
            borderRadius: 3.5,
            p: 1.5,
            boxShadow: '0 24px 64px rgba(0, 0, 0, 0.85)'
          }
        }}
      >
        <DialogTitle sx={{ color: '#FFFFFF', fontWeight: 800, fontSize: '1.25rem', px: 3, pt: 2 }}>
          Create New Tip Goal
        </DialogTitle>
        <DialogContent sx={{ px: 3 }}>
          {submitError && <Alert severity="error" sx={{ mb: 2, mt: 1 }}>{submitError}</Alert>}
          <Box component="form" sx={{ mt: 1 }}>
            <TextField
              select
              fullWidth
              label="Goal Type"
              name="goalType"
              value={formData.goalType}
              onChange={handleChange}
              margin="normal"
            >
              {Object.entries(GOAL_TYPES).map(([key, label]) => (
                <MenuItem key={key} value={key}>{label}</MenuItem>
              ))}
            </TextField>

            <TextField
              fullWidth
              label="Target Value"
              name="targetValue"
              type="number"
              value={formData.targetValue}
              onChange={handleChange}
              margin="normal"
              required
              inputProps={{ min: "0.01", step: "0.01" }}
            />

            {(formData.goalType === 'TOTAL_TIP_AMOUNT' || 
              formData.goalType === 'AVERAGE_TIP_PERCENTAGE' || 
              formData.goalType === 'MEDIAN_TIP_PERCENTAGE') && (
              <TextField
                fullWidth
                label="Currency"
                name="currency"
                value={formData.currency}
                onChange={handleChange}
                margin="normal"
                required={formData.goalType === 'TOTAL_TIP_AMOUNT'}
              />
            )}

            <TextField
              select
              fullWidth
              label="Period"
              name="period"
              value={formData.period}
              onChange={handleChange}
              margin="normal"
            >
              {Object.entries(PERIODS).map(([key, label]) => (
                <MenuItem key={key} value={key}>{label}</MenuItem>
              ))}
            </TextField>

            {formData.period === 'CUSTOM' && (
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  fullWidth
                  label="Start Date"
                  name="startDate"
                  type="date"
                  value={formData.startDate}
                  onChange={handleChange}
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                  required
                />
                <TextField
                  fullWidth
                  label="End Date"
                  name="endDate"
                  type="date"
                  value={formData.endDate}
                  onChange={handleChange}
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                  required
                />
              </Box>
            )}

            <TextField
              fullWidth
              label="Restaurant (Optional)"
              name="restaurantName"
              value={formData.restaurantName}
              onChange={handleChange}
              margin="normal"
            />

            {formData.goalType === 'SERVICE_QUALITY' && (
              <TextField
                select
                fullWidth
                label="Service Quality"
                name="serviceQuality"
                value={formData.serviceQuality}
                onChange={handleChange}
                margin="normal"
                required
              >
                {SERVICE_QUALITIES.map((q) => (
                  <MenuItem key={q} value={q}>{q}</MenuItem>
                ))}
              </TextField>
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleClose} sx={{ color: '#94A3B8', fontWeight: 600 }}>Cancel</Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained" 
            sx={{
              borderRadius: '24px',
              px: 3.5,
              py: 1,
              fontWeight: 700,
              background: 'linear-gradient(135deg, #fd5b38 0%, #e04826 100%)',
              color: '#ffffff'
            }}
          >
            Create Goal
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TipGoalsPage;
