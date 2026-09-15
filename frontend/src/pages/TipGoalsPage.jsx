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
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Smart Tipping Goals</Typography>
        <Button variant="contained" color="primary" onClick={handleOpen}>
          Create Goal
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}>
          <CircularProgress />
        </Box>
      ) : goals.length === 0 ? (
        <Alert severity="info" sx={{ mt: 3, backgroundColor: 'rgba(2, 136, 209, 0.1)', color: 'white' }}>
          No goals found. Create your first goal to start tracking your progress!
        </Alert>
      ) : (
        <Grid container spacing={3}>
          {goals.map((goal) => (
            <Grid item xs={12} md={6} key={goal.id}>
              <Card sx={{ 
                background: 'rgba(255, 255, 255, 0.05)',
                backdropFilter: 'blur(10px)',
                border: '1px solid rgba(255, 255, 255, 0.1)'
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="h6" color="primary">
                      {GOAL_TYPES[goal.goalType]}
                    </Typography>
                    <Chip 
                      label={goal.status} 
                      color={
                        goal.status === 'COMPLETED' ? 'success' : 
                        goal.status === 'ACTIVE' ? 'primary' : 
                        goal.status === 'EXPIRED' ? 'warning' : 'default'
                      } 
                      size="small" 
                    />
                  </Box>
                  
                  <Typography variant="body2" color="textSecondary" gutterBottom>
                    {PERIODS[goal.period]} {goal.period === 'CUSTOM' && `(${goal.startDate} to ${goal.endDate})`}
                  </Typography>

                  <Box sx={{ my: 3 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body1">
                        {formatValue(goal.currentValue, goal.goalType, goal.currency)} / {formatValue(goal.targetValue, goal.goalType, goal.currency)}
                      </Typography>
                      <Typography variant="body1" fontWeight="bold">
                        {goal.progressPercentage}%
                      </Typography>
                    </Box>
                    <LinearProgress 
                      variant="determinate" 
                      value={Number(goal.progressPercentage)} 
                      color={goal.status === 'COMPLETED' ? 'success' : 'primary'}
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                    <Box>
                      {goal.status === 'ACTIVE' && (
                        <Typography variant="body2" color="textSecondary">
                          {formatValue(goal.remainingValue, goal.goalType, goal.currency)} remaining
                        </Typography>
                      )}
                      <Typography variant="caption" display="block" color="textSecondary" sx={{ mt: 1 }}>
                        Confidence: {goal.confidence} (Sample size: {goal.sampleSize})
                      </Typography>
                      {goal.restaurantName && (
                        <Typography variant="caption" display="block" color="textSecondary">
                          Restaurant: {goal.restaurantName}
                        </Typography>
                      )}
                    </Box>
                    
                    {goal.status === 'ACTIVE' && (
                      <Button size="small" color="error" onClick={() => handleCancel(goal.id)}>
                        Cancel
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
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth PaperProps={{
        sx: {
          background: '#121212',
          border: '1px solid rgba(255,255,255,0.1)',
        }
      }}>
        <DialogTitle>Create New Tip Goal</DialogTitle>
        <DialogContent>
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
        <DialogActions>
          <Button onClick={handleClose} color="inherit">Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">Create</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TipGoalsPage;
