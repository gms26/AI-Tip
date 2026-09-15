import { useState } from 'react';
import {
  Box, Card, CardContent, Typography, Grid, Chip, Alert,
  CircularProgress, TextField, MenuItem, Button, IconButton,
  LinearProgress
} from '@mui/material';
import {
  CompareArrows as CompareIcon,
  Add as AddIcon,
  Remove as RemoveIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Receipt as ReceiptIcon,
  AccountBalanceWallet as BudgetIcon,
  History as HistoryIcon
} from '@mui/icons-material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell, ReferenceLine } from 'recharts';
import { motion } from 'framer-motion';
import tipScenarioApi from '../api/tipScenarioApi';

const TipScenarioPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currency, setCurrency] = useState('USD');
  const [billAmount, setBillAmount] = useState('');
  const [scenarioPercentages, setScenarioPercentages] = useState(['10', '15', '20']);
  const [monthlyBudget, setMonthlyBudget] = useState('');

  const currencies = ['USD', 'EUR', 'GBP', 'INR', 'JPY', 'CAD', 'AUD'];

  const addScenario = () => {
    if (scenarioPercentages.length < 5) {
      setScenarioPercentages([...scenarioPercentages, '']);
    }
  };

  const removeScenario = (index) => {
    if (scenarioPercentages.length > 1) {
      setScenarioPercentages(scenarioPercentages.filter((_, i) => i !== index));
    }
  };

  const updateScenario = (index, value) => {
    const updated = [...scenarioPercentages];
    updated[index] = value;
    setScenarioPercentages(updated);
  };

  const handleCalculate = async () => {
    setLoading(true);
    setError(null);
    try {
      const validPercentages = scenarioPercentages
        .filter(p => p !== '' && !isNaN(parseFloat(p)))
        .map(p => parseFloat(p));

      if (!billAmount || parseFloat(billAmount) <= 0) {
        setError('Bill amount must be greater than 0.');
        setLoading(false);
        return;
      }
      if (validPercentages.length === 0) {
        setError('Add at least one scenario percentage.');
        setLoading(false);
        return;
      }

      const payload = {
        currency,
        billAmount: parseFloat(billAmount),
        scenarioPercentages: validPercentages
      };
      if (monthlyBudget && parseFloat(monthlyBudget) > 0) {
        payload.monthlyBudget = parseFloat(monthlyBudget);
      }

      const res = await tipScenarioApi.calculateScenarios(payload);
      setData(res);
    } catch (err) {
      console.error('Failed to calculate scenarios', err);
      setError(err.response?.data?.message || 'Could not calculate scenarios.');
    } finally {
      setLoading(false);
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

  const getScenarioColor = (diff) => {
    if (diff === null || diff === undefined) return '#38bdf8';
    if (diff < -2) return '#10B981';   // conservative
    if (diff > 2) return '#fd5b38';    // generous
    return '#38bdf8';                   // historical-like
  };

  const getScenarioLabel = (diff) => {
    if (diff === null || diff === undefined) return 'Scenario';
    if (diff < -2) return 'Conservative';
    if (diff > 2) return 'More Generous';
    return 'Historical-like';
  };

  const containerVariants = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.08 } }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 24 } }
  };

  const chartData = data ? data.scenarioResults.map(r => ({
    name: `${r.tipPercentage}%`,
    tipAmount: parseFloat(r.tipAmount) || 0,
    percentage: parseFloat(r.tipPercentage) || 0,
    diff: r.differenceFromHistorical
  })) : [];

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', px: { xs: 2, md: 4 }, py: { xs: 4, md: 6 }, width: '100%' }}>
      {/* Header */}
      <Box component={motion.div} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <CompareIcon sx={{ color: '#38bdf8', fontSize: 32 }} />
          <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700 }}>
            What-If Tip Scenarios
          </Typography>
        </Box>
        <Typography variant="body1" sx={{ color: '#94A3B8' }}>
          Compare hypothetical tip percentages against your historical behaviour. All results are estimates — not actual transactions.
        </Typography>
      </Box>

      {/* Input Controls */}
      <Card component={motion.div} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} sx={{ mb: 4 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="flex-start">
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
                fullWidth size="small" label="Bill Amount" type="number"
                value={billAmount} onChange={(e) => setBillAmount(e.target.value)}
                placeholder="e.g. 1000"
                slotProps={{ htmlInput: { min: 0.01, step: 0.01 } }}
              />
            </Grid>
            <Grid item xs={12} sm={3}>
              <TextField
                fullWidth size="small" label="Monthly Budget (optional)" type="number"
                value={monthlyBudget} onChange={(e) => setMonthlyBudget(e.target.value)}
                placeholder="e.g. 5000"
                slotProps={{ htmlInput: { min: 0 } }}
              />
            </Grid>
            <Grid item xs={12} sm={3}>
              <Button
                fullWidth variant="contained" onClick={handleCalculate}
                disabled={loading}
                sx={{ height: 40, background: 'linear-gradient(135deg, #fd5b38 0%, #d63d19 100%)', color: '#fff', fontWeight: 600, '&:hover': { background: 'linear-gradient(135deg, #ff6b4a 0%, #e0441d 100%)' } }}
              >
                {loading ? <CircularProgress size={20} sx={{ color: '#fff' }} /> : 'Compare Scenarios'}
              </Button>
            </Grid>
          </Grid>

          {/* Scenario Percentage Inputs */}
          <Box sx={{ mt: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                Tip Percentages to Compare ({scenarioPercentages.length}/5)
              </Typography>
              {scenarioPercentages.length < 5 && (
                <IconButton size="small" onClick={addScenario}
                  sx={{ color: '#10B981', border: '1px solid rgba(0,230,118,0.3)' }}>
                  <AddIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {scenarioPercentages.map((pct, i) => (
                <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <TextField
                    size="small" type="number" value={pct}
                    onChange={(e) => updateScenario(i, e.target.value)}
                    placeholder="%"
                    sx={{ width: 80 }}
                    slotProps={{ htmlInput: { min: 0, max: 100, step: 0.1 },
                      input: { endAdornment: <Typography variant="caption" sx={{ color: '#94A3B8' }}>%</Typography> } }}
                  />
                  {scenarioPercentages.length > 1 && (
                    <IconButton size="small" onClick={() => removeScenario(i)}
                      sx={{ color: '#F59E0B', p: 0.5 }}>
                      <RemoveIcon fontSize="small" />
                    </IconButton>
                  )}
                </Box>
              ))}
            </Box>
          </Box>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {data && (
        <Box component={motion.div} variants={containerVariants} initial="hidden" animate="show">
          <Grid container spacing={3}>

            {/* Historical Stats Panel */}
            {data.historicalTipCount > 0 && (
              <Grid item xs={12} md={4}>
                <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                      <HistoryIcon sx={{ color: '#38bdf8' }} />
                      <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Your History</Typography>
                      <Chip label="Fact" size="small"
                        sx={{ ml: 'auto', backgroundColor: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8', fontWeight: 'bold' }} />
                    </Box>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                      <Box>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Median Tip %</Typography>
                        <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                          {Number(data.historicalMedianTipPercentage).toFixed(1)}%
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Average Tip %</Typography>
                        <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                          {Number(data.historicalAverageTipPercentage).toFixed(1)}%
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Average Tip Amount</Typography>
                        <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                          {data.currency} {Number(data.historicalAverageTipAmount).toFixed(2)}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Tips Analysed (90 days)</Typography>
                        <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                          {data.historicalTipCount}
                        </Typography>
                      </Box>
                      {data.historicalMonthlyTipAmount && (
                        <Box>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>Est. Monthly Spending</Typography>
                          <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                            {data.currency} {Number(data.historicalMonthlyTipAmount).toFixed(2)}
                          </Typography>
                        </Box>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Chart */}
            <Grid item xs={12} md={data.historicalTipCount > 0 ? 8 : 12}>
              <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" sx={{ color: '#FFFFFF', mb: 3 }}>
                    Tip Amount Comparison
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
                        formatter={(v) => [`${data.currency} ${Number(v).toFixed(2)}`, 'Tip Amount']}
                      />
                      {data.historicalMedianTipPercentage && (
                        <ReferenceLine
                          x={`${data.historicalMedianTipPercentage}%`}
                          stroke="#38bdf8" strokeDasharray="5 5" strokeWidth={2}
                          label={{ value: 'Median', fill: '#38bdf8', fontSize: 11, position: 'top' }}
                        />
                      )}
                      <Bar dataKey="tipAmount" radius={[6, 6, 0, 0]}>
                        {chartData.map((entry, index) => (
                          <Cell key={index} fill={getScenarioColor(entry.diff)} opacity={0.85} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                  <Box sx={{ display: 'flex', gap: 3, mt: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: '#10B981' }} />
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Conservative</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: '#38bdf8' }} />
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>Historical-like</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 12, height: 12, borderRadius: 1, bgcolor: '#fd5b38' }} />
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>More Generous</Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Scenario Comparison Cards */}
            {data.scenarioResults.map((r, i) => (
              <Grid item xs={12} sm={6} md={4} key={i}>
                <Card component={motion.div} variants={itemVariants}
                  sx={{ height: '100%', borderTop: `3px solid ${getScenarioColor(r.differenceFromHistorical)}` }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                      <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 700 }}>
                        {Number(r.tipPercentage)}%
                      </Typography>
                      <Chip label={r.differenceFromHistorical !== null ? getScenarioLabel(r.differenceFromHistorical) : 'Scenario'}
                        size="small"
                        sx={{
                          backgroundColor: `${getScenarioColor(r.differenceFromHistorical)}20`,
                          color: getScenarioColor(r.differenceFromHistorical),
                          fontWeight: 'bold'
                        }} />
                    </Box>
                    <Chip label="Hypothetical" size="small" variant="outlined"
                      sx={{ mb: 2, color: '#94A3B8', borderColor: 'rgba(255,255,255,0.1)', fontSize: 10 }} />

                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                      <Box sx={{ p: 1.5, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Tip Amount</Typography>
                        <Typography variant="h6" sx={{ color: '#FFFFFF' }}>
                          {data.currency} {Number(r.tipAmount).toFixed(2)}
                        </Typography>
                      </Box>
                      <Box sx={{ p: 1.5, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Total Bill</Typography>
                        <Typography variant="h6" sx={{ color: '#FFFFFF' }}>
                          {data.currency} {Number(r.totalAmount).toFixed(2)}
                        </Typography>
                      </Box>

                      {r.differenceFromHistorical !== null && (
                        <Box sx={{ p: 1.5, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>vs Historical Median</Typography>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            {r.differenceFromHistorical > 0 ? (
                              <TrendingUpIcon sx={{ color: '#F59E0B', fontSize: 18 }} />
                            ) : r.differenceFromHistorical < 0 ? (
                              <TrendingDownIcon sx={{ color: '#10B981', fontSize: 18 }} />
                            ) : null}
                            <Typography variant="body1" sx={{
                              color: r.differenceFromHistorical > 0 ? '#F59E0B'
                                : r.differenceFromHistorical < 0 ? '#10B981' : '#FFFFFF',
                              fontWeight: 600
                            }}>
                              {r.differenceFromHistorical > 0 ? '+' : ''}{Number(r.differenceFromHistorical).toFixed(1)} pp
                            </Typography>
                            <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                              ({r.monetaryDifference > 0 ? '+' : ''}{data.currency} {Number(r.monetaryDifference).toFixed(2)})
                            </Typography>
                          </Box>
                        </Box>
                      )}

                      {r.budgetStatus && (
                        <Box sx={{ p: 1.5, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                            Hypothetical Monthly Budget Impact
                          </Typography>
                          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 0.5, mb: 0.5 }}>
                            <Typography variant="body2" sx={{ color: '#FFFFFF' }}>
                              {data.currency} {Number(r.monthlyProjectedTipAmount).toFixed(2)}/mo
                            </Typography>
                            <Chip label={getBudgetStatusLabel(r.budgetStatus)} size="small"
                              sx={{
                                backgroundColor: `${getBudgetStatusColor(r.budgetStatus)}20`,
                                color: getBudgetStatusColor(r.budgetStatus),
                                fontWeight: 'bold', fontSize: 10
                              }} />
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(Number(r.monthlyBudgetUsagePercentage), 100)}
                            sx={{
                              height: 6, borderRadius: 3,
                              backgroundColor: 'rgba(255,255,255,0.08)',
                              '& .MuiLinearProgress-bar': {
                                borderRadius: 3,
                                backgroundColor: getBudgetStatusColor(r.budgetStatus)
                              }
                            }}
                          />
                          <Typography variant="caption" sx={{ color: '#94A3B8', mt: 0.5, display: 'block' }}>
                            {Number(r.monthlyBudgetUsagePercentage).toFixed(1)}% of budget
                          </Typography>
                        </Box>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}

            {/* No History Alert */}
            {data.historicalTipCount === 0 && (
              <Grid item xs={12}>
                <Alert severity="info" sx={{ backgroundColor: 'rgba(56, 189, 248, 0.1)', color: '#FFFFFF',
                  '& .MuiAlert-icon': { color: '#38bdf8' } }}>
                  No {data.currency} tipping history found. Scenarios are based on your input bill amount.
                  Historical comparison and monthly projections are unavailable.
                </Alert>
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
      )}
    </Box>
  );
};

export default TipScenarioPage;
