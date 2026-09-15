import { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Typography, Grid, Chip, Alert,
  CircularProgress, TextField, MenuItem, Slider, LinearProgress
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  Receipt as ReceiptIcon,
  Percent as PercentIcon,
  Shield as ShieldIcon,
  AccountBalanceWallet as BudgetIcon,
  Warning as WarningIcon
} from '@mui/icons-material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { motion } from 'framer-motion';
import tipForecastApi from '../api/tipForecastApi';

const TipForecastPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currency, setCurrency] = useState('USD');
  const [period, setPeriod] = useState('NEXT_30_DAYS');
  const [lookbackDays, setLookbackDays] = useState(90);
  const [monthlyBudget, setMonthlyBudget] = useState('');

  const currencies = ['USD', 'EUR', 'GBP', 'INR', 'JPY', 'CAD', 'AUD'];
  const periods = [
    { value: 'CURRENT_MONTH', label: 'Current Month' },
    { value: 'NEXT_30_DAYS', label: 'Next 30 Days' },
    { value: 'NEXT_3_MONTHS', label: 'Next 3 Months' }
  ];

  const fetchForecast = async () => {
    setLoading(true);
    try {
      const params = { currency, period, lookbackDays };
      if (monthlyBudget && parseFloat(monthlyBudget) > 0) {
        params.monthlyBudget = parseFloat(monthlyBudget);
      }
      const res = await tipForecastApi.getForecast(params);
      setData(res);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch forecast', err);
      setError('Could not load forecast data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timeout = setTimeout(fetchForecast, 300);
    return () => clearTimeout(timeout);
  }, [currency, period, lookbackDays, monthlyBudget]);

  const getConfidenceColor = (conf) => {
    switch (conf) {
      case 'HIGH': return '#10B981';
      case 'MEDIUM': return '#F59E0B';
      case 'LOW': return '#F59E0B';
      default: return '#94A3B8';
    }
  };

  const getBudgetStatusColor = (status) => {
    switch (status) {
      case 'UNDER_BUDGET': return '#10B981';
      case 'APPROACHING_LIMIT': return '#F59E0B';
      case 'LIMIT_REACHED': return '#F59E0B';
      case 'OVER_BUDGET': return '#FF6B6B';
      default: return '#94A3B8';
    }
  };

  const getBudgetStatusLabel = (status) => {
    switch (status) {
      case 'UNDER_BUDGET': return 'Under Budget';
      case 'APPROACHING_LIMIT': return 'Approaching Limit';
      case 'LIMIT_REACHED': return 'Limit Reached';
      case 'OVER_BUDGET': return 'Over Budget';
      default: return status;
    }
  };

  const containerVariants = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.1 } }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 24 } }
  };

  const chartData = data && data.historicalTipCount > 0 ? [
    {
      name: 'Historical Avg',
      amount: parseFloat(data.historicalAverageTipAmount) || 0,
      type: 'historical'
    },
    {
      name: 'Projected Total',
      amount: parseFloat(data.estimatedMonthlyTipAmount) || 0,
      type: 'projection'
    }
  ] : [];

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', px: { xs: 2, md: 4 }, py: { xs: 4, md: 6 }, width: '100%' }}>
      {/* Header */}
      <Box component={motion.div} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <TrendingUpIcon sx={{ color: '#10B981', fontSize: 32 }} />
          <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700 }}>
            Smart Tip Forecast
          </Typography>
        </Box>
        <Typography variant="body1" sx={{ color: '#94A3B8' }}>
          Projected future tipping activity based on your recent history.
        </Typography>
      </Box>

      {/* Controls */}
      <Card component={motion.div} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} sx={{ mb: 4 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={3}>
              <TextField
                select fullWidth size="small" label="Currency" value={currency}
                onChange={(e) => setCurrency(e.target.value)}
              >
                {currencies.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={3}>
              <TextField
                select fullWidth size="small" label="Forecast Period" value={period}
                onChange={(e) => setPeriod(e.target.value)}
              >
                {periods.map(p => <MenuItem key={p.value} value={p.value}>{p.label}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={3}>
              <TextField
                fullWidth size="small" label="Monthly Budget (optional)" type="number"
                value={monthlyBudget} onChange={(e) => setMonthlyBudget(e.target.value)}
                placeholder="e.g. 500"
                slotProps={{ htmlInput: { min: 0 } }}
              />
            </Grid>
            <Grid item xs={12} sm={3}>
              <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                Lookback: {lookbackDays} days
              </Typography>
              <Slider
                value={lookbackDays} onChange={(_, v) => setLookbackDays(v)}
                min={7} max={365} step={1}
                sx={{ color: '#10B981' }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {loading && !data ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
          <CircularProgress sx={{ color: '#10B981' }} />
        </Box>
      ) : error ? (
        <Alert severity="error">{error}</Alert>
      ) : data && data.historicalTipCount === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <TrendingUpIcon sx={{ fontSize: 64, color: '#94A3B8', mb: 2 }} />
            <Typography variant="h6" sx={{ color: '#FFFFFF', mb: 1 }}>No Forecasting History Available</Typography>
            <Typography variant="body2" sx={{ color: '#94A3B8' }}>
              Start recording tips in {data.currency} to see spending forecasts.
            </Typography>
            <Chip label={`Confidence: ${data.confidence}`} size="small"
              sx={{ mt: 2, backgroundColor: 'rgba(255,109,0,0.1)', color: '#F59E0B' }} />
          </CardContent>
        </Card>
      ) : data ? (
        <Box component={motion.div} variants={containerVariants} initial="hidden" animate="show">
          <Grid container spacing={3}>
            {/* Overview Cards */}
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <ReceiptIcon sx={{ color: '#10B981', fontSize: 28, mb: 1 }} />
                  <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>
                    Estimated Tips
                  </Typography>
                  <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 0.5 }}>
                    {data.estimatedTipCount}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                    in {data.forecastDays} days
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <BudgetIcon sx={{ color: '#fd5b38', fontSize: 28, mb: 1 }} />
                  <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>
                    Estimated Spending
                  </Typography>
                  <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 0.5 }}>
                    {data.currency} {Number(data.estimatedMonthlyTipAmount).toFixed(2)}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                    projected total
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <PercentIcon sx={{ color: '#38bdf8', fontSize: 28, mb: 1 }} />
                  <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>
                    Historical Median
                  </Typography>
                  <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 0.5 }}>
                    {Number(data.historicalMedianPercentage).toFixed(1)}%
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                    tip percentage
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <ShieldIcon sx={{ color: getConfidenceColor(data.confidence), fontSize: 28, mb: 1 }} />
                  <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>
                    Confidence
                  </Typography>
                  <Typography variant="h4" sx={{
                    color: getConfidenceColor(data.confidence), fontWeight: 700, mt: 0.5
                  }}>
                    {data.confidence}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                    {data.historicalTipCount} tips analysed
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            {/* Chart */}
            <Grid item xs={12} md={8}>
              <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" sx={{ color: '#FFFFFF', mb: 3 }}>
                    Historical vs Projected
                  </Typography>
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                      <XAxis dataKey="name" tick={{ fill: '#94A3B8', fontSize: 12 }} />
                      <YAxis tick={{ fill: '#94A3B8', fontSize: 12 }}
                        tickFormatter={(v) => `${data.currency} ${v}`} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#1A1F30', border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: 8, color: '#FFFFFF'
                        }}
                        formatter={(v) => [`${data.currency} ${Number(v).toFixed(2)}`, 'Amount']}
                      />
                      <Bar dataKey="amount" radius={[6, 6, 0, 0]}>
                        {chartData.map((entry, index) => (
                          <Cell key={index}
                            fill={entry.type === 'historical' ? '#fd5b38' : '#ff8566'}
                            opacity={entry.type === 'projection' ? 0.75 : 1} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                  <Box sx={{ display: 'flex', gap: 3, mt: 2, justifyContent: 'center' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: '#fd5b38' }} />
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Historical</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: '#ff8566', opacity: 0.75 }} />
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Projected</Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Historical Stats */}
            <Grid item xs={12} md={4}>
              <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" sx={{ color: '#FFFFFF', mb: 3 }}>Historical Stats</Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                    <Box>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Average Tip Amount</Typography>
                      <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                        {data.currency} {Number(data.historicalAverageTipAmount).toFixed(2)}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Average Tip %</Typography>
                      <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                        {Number(data.historicalAveragePercentage).toFixed(1)}%
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Lookback Window</Typography>
                      <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                        {data.lookbackDays} days
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Tips in Window</Typography>
                      <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                        {data.historicalTipCount}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Budget Projection Card (conditional) */}
            {data.budgetStatus && (
              <Grid item xs={12}>
                <Card component={motion.div} variants={itemVariants}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                      <BudgetIcon sx={{ color: '#FFFFFF' }} />
                      <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Budget Projection</Typography>
                      <Chip
                        label={getBudgetStatusLabel(data.budgetStatus)}
                        size="small"
                        sx={{
                          ml: 'auto',
                          backgroundColor: `${getBudgetStatusColor(data.budgetStatus)}20`,
                          color: getBudgetStatusColor(data.budgetStatus),
                          border: `1px solid ${getBudgetStatusColor(data.budgetStatus)}40`,
                          fontWeight: 'bold'
                        }}
                      />
                    </Box>
                    <Grid container spacing={3}>
                      <Grid item xs={6} md={3}>
                        <Box sx={{ p: 2, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>Monthly Budget</Typography>
                          <Typography variant="h5" sx={{ color: '#FFFFFF', mt: 1 }}>
                            {data.currency} {Number(data.monthlyBudget).toFixed(2)}
                          </Typography>
                        </Box>
                      </Grid>
                      <Grid item xs={6} md={3}>
                        <Box sx={{ p: 2, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>Projected Tips</Typography>
                          <Typography variant="h5" sx={{ color: '#FFFFFF', mt: 1 }}>
                            {data.currency} {Number(data.estimatedMonthlyTipAmount).toFixed(2)}
                          </Typography>
                        </Box>
                      </Grid>
                      <Grid item xs={6} md={3}>
                        <Box sx={{ p: 2, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>Projected Usage</Typography>
                          <Typography variant="h5" sx={{
                            color: getBudgetStatusColor(data.budgetStatus), mt: 1
                          }}>
                            {Number(data.projectedBudgetUsage).toFixed(1)}%
                          </Typography>
                        </Box>
                      </Grid>
                      <Grid item xs={6} md={3}>
                        <Box sx={{ p: 2, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>Estimated Tips</Typography>
                          <Typography variant="h5" sx={{ color: '#FFFFFF', mt: 1 }}>
                            {data.estimatedTipCount}
                          </Typography>
                        </Box>
                      </Grid>
                    </Grid>
                    <Box sx={{ mt: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" sx={{ color: '#94A3B8' }}>Budget Usage</Typography>
                        <Typography variant="body2" sx={{
                          color: getBudgetStatusColor(data.budgetStatus)
                        }}>
                          {Number(data.projectedBudgetUsage).toFixed(1)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(Number(data.projectedBudgetUsage), 100)}
                        sx={{
                          height: 8, borderRadius: 4,
                          backgroundColor: 'rgba(255,255,255,0.08)',
                          '& .MuiLinearProgress-bar': {
                            borderRadius: 4,
                            backgroundColor: getBudgetStatusColor(data.budgetStatus)
                          }
                        }}
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Message Card */}
            <Grid item xs={12}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent>
                  <Typography variant="body1" sx={{ color: '#94A3B8', fontStyle: 'italic' }}>
                    {data.message}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Box>
      ) : null}
    </Box>
  );
};

export default TipForecastPage;
