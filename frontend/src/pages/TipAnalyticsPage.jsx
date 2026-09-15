import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import analyticsApi from '../api/analyticsApi';
import exportApi from '../api/exportApi';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  MenuItem,
  Grid,
  Stack,
  Chip,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Analytics as AnalyticsIcon,
  TrendingUp as TrendIcon,
  Restaurant as RestaurantIcon,
  Star as StarIcon,
  FilterAlt as FilterIcon,
  RestartAlt as ResetIcon,
  ArrowBack as BackIcon,
  AccountBalanceWallet as WalletIcon,
} from '@mui/icons-material';
import { motion } from 'framer-motion';
import SkeletonLoader from '../components/SkeletonLoader';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  Area,
  AreaChart,
} from 'recharts';

const PERIODS = [
  { value: 'CURRENT_MONTH', label: 'Current Month' },
  { value: 'PREVIOUS_MONTH', label: 'Previous Month' },
  { value: 'CURRENT_YEAR', label: 'Current Year' },
  { value: 'LAST_30_DAYS', label: 'Last 30 Days' },
  { value: 'LAST_90_DAYS', label: 'Last 90 Days' },
  { value: 'ALL_TIME', label: 'All Time' },
];

const SERVICE_QUALITIES = [
  { value: '', label: 'All Ratings' },
  { value: 'POOR', label: 'Poor', color: '#FF5252' },
  { value: 'AVERAGE', label: 'Average', color: '#FFA726' },
  { value: 'GOOD', label: 'Good', color: '#66BB6A' },
  { value: 'EXCELLENT', label: 'Excellent', color: '#fd5b38' },
];

const TipAnalyticsPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [exportLoading, setExportLoading] = useState(false);
  const [analytics, setAnalytics] = useState(null);
  const [error, setError] = useState(null);

  // Filters
  const [period, setPeriod] = useState('ALL_TIME');
  const [restaurantFilter, setRestaurantFilter] = useState('');
  const [serviceQualityFilter, setServiceQualityFilter] = useState('');

  const getDateRangeForPeriod = (p) => {
    const now = new Date();
    let startDate = null;
    let endDate = null;
    if (p === 'CURRENT_MONTH') {
      startDate = new Date(now.getFullYear(), now.getMonth(), 1);
      endDate = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    } else if (p === 'PREVIOUS_MONTH') {
      startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      endDate = new Date(now.getFullYear(), now.getMonth(), 1);
    } else if (p === 'CURRENT_YEAR') {
      startDate = new Date(now.getFullYear(), 0, 1);
      endDate = new Date(now.getFullYear() + 1, 0, 1);
    } else if (p === 'LAST_30_DAYS') {
      startDate = new Date(now);
      startDate.setDate(now.getDate() - 30);
      endDate = new Date(now);
      endDate.setDate(now.getDate() + 1);
    } else if (p === 'LAST_90_DAYS') {
      startDate = new Date(now);
      startDate.setDate(now.getDate() - 90);
      endDate = new Date(now);
      endDate.setDate(now.getDate() + 1);
    }
    return {
      startDate: startDate ? startDate.toISOString() : null,
      endDate: endDate ? endDate.toISOString() : null,
    };
  };

  const handleExport = async (type, format = 'csv') => {
    try {
      setExportLoading(true);
      
      const { startDate, endDate } = getDateRangeForPeriod(period);
      
      let blob;
      let filename = 'export';
      
      if (type === 'tips') {
        const filters = {
          startDate,
          endDate,
          restaurantName: restaurantFilter || null,
          serviceQuality: serviceQualityFilter || null,
        };
        blob = await exportApi.exportTips(filters, format);
        filename = `tips.${format}`;
      } else if (type === 'analytics') {
        const request = {
          period,
          restaurantName: restaurantFilter || null,
          serviceQuality: serviceQualityFilter || null,
        };
        blob = await exportApi.exportAnalytics(request);
        filename = 'analytics.json';
      }

      // Create download link
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (err) {
      console.error('Export failed:', err);
      alert('Export failed. Please try again.');
    } finally {
      setExportLoading(false);
    }
  };

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const request = {
        period,
        restaurantName: restaurantFilter || null,
        serviceQuality: serviceQualityFilter || null,
      };
      const data = await analyticsApi.getTipAnalytics(request);
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to load analytics:', err);
      setError('Failed to load analytics data.');
    } finally {
      setLoading(false);
    }
  }, [period, restaurantFilter, serviceQualityFilter]);

  useEffect(() => {
    const timeout = setTimeout(fetchAnalytics, 300);
    return () => clearTimeout(timeout);
  }, [fetchAnalytics]);

  const resetFilters = () => {
    setPeriod('ALL_TIME');
    setRestaurantFilter('');
    setServiceQualityFilter('');
  };

  // Framer Motion
  const containerVariants = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.08 } },
  };
  const itemVariants = {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 24 } },
  };

  // Format helpers
  const formatPct = (v) => (v != null ? `${Number(v).toFixed(1)}%` : '—');
  const formatAmt = (v, currency) => {
    if (v == null) return '—';
    return `${currency || ''} ${Number(v).toFixed(2)}`.trim();
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', px: { xs: 2, md: 4 }, py: { xs: 4, md: 6 }, width: '100%' }}>
      {/* Header */}
      <Box sx={{ mb: 5, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }} component={motion.div} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={() => navigate('/dashboard')} sx={{ color: '#94A3B8' }}>
            <BackIcon />
          </IconButton>
          <Box>
            <Typography variant="h3" sx={{ color: '#FFFFFF', mb: 0.5 }}>
              Tip Analytics
            </Typography>
            <Typography variant="body1" sx={{ color: '#94A3B8' }}>
              Deterministic insights from your tipping history.
            </Typography>
          </Box>
        </Box>
        <Stack direction="row" spacing={2}>
          <Button 
            variant="outlined" 
            onClick={() => handleExport('tips', 'csv')} 
            disabled={exportLoading || loading || (analytics && analytics.totalTipCount === 0)}
            sx={{ borderColor: '#3A3F4B', color: '#94A3B8', '&:hover': { borderColor: '#fd5b38', color: '#fd5b38' } }}
          >
            {exportLoading ? 'Exporting...' : 'Export Tips (CSV)'}
          </Button>
          <Button 
            variant="outlined" 
            onClick={() => handleExport('analytics', 'json')} 
            disabled={exportLoading || loading || (analytics && analytics.totalTipCount === 0)}
            sx={{ borderColor: '#3A3F4B', color: '#94A3B8', '&:hover': { borderColor: '#fd5b38', color: '#fd5b38' } }}
          >
            Export Analytics (JSON)
          </Button>
        </Stack>
      </Box>

      {/* Filters */}
      <Card
        component={motion.div}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        sx={{ mb: 4, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}
      >
        <CardContent sx={{ py: 2.5, px: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
            <FilterIcon sx={{ color: '#94A3B8', fontSize: 20 }} />
            <Typography variant="subtitle2" sx={{ color: '#94A3B8', textTransform: 'uppercase', letterSpacing: 1 }}>
              Filters
            </Typography>
          </Box>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                value={period}
                onChange={(e) => setPeriod(e.target.value)}
                size="small"
                label="Period"
              >
                {PERIODS.map((p) => (
                  <MenuItem key={p.value} value={p.value}>{p.label}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                fullWidth
                value={restaurantFilter}
                onChange={(e) => setRestaurantFilter(e.target.value)}
                size="small"
                label="Restaurant"
                placeholder="Filter by restaurant..."
              />
            </Grid>
            <Grid item xs={12} sm={3} md={3}>
              <TextField
                select
                fullWidth
                value={serviceQualityFilter}
                onChange={(e) => setServiceQualityFilter(e.target.value)}
                size="small"
                label="Service Quality"
              >
                {SERVICE_QUALITIES.map((sq) => (
                  <MenuItem key={sq.value} value={sq.value}>{sq.label}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={1} md={3}>
              <Button
                startIcon={<ResetIcon />}
                onClick={resetFilters}
                size="small"
                sx={{ color: '#94A3B8', '&:hover': { color: '#FFFFFF' } }}
              >
                Reset
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Loading State */}
      {loading && (
        <Grid container spacing={3}>
          <Grid item xs={6} md={3}><SkeletonLoader height={100} /></Grid>
          <Grid item xs={6} md={3}><SkeletonLoader height={100} /></Grid>
          <Grid item xs={6} md={3}><SkeletonLoader height={100} /></Grid>
          <Grid item xs={6} md={3}><SkeletonLoader height={100} /></Grid>
          <Grid item xs={12}><SkeletonLoader height={300} /></Grid>
        </Grid>
      )}

      {/* Error State */}
      {error && !loading && (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="body1" sx={{ color: '#FF5252' }}>{error}</Typography>
          <Button onClick={fetchAnalytics} sx={{ mt: 2, color: '#fd5b38' }}>Retry</Button>
        </Box>
      )}

      {/* Empty State */}
      {!loading && !error && analytics && analytics.totalTipCount === 0 && (
        <Card sx={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <CardContent sx={{ p: 6, textAlign: 'center' }}>
            <AnalyticsIcon sx={{ fontSize: 48, color: '#3A3F4B', mb: 2 }} />
            <Typography variant="h6" sx={{ color: '#94A3B8', mb: 1 }}>No Tips Found</Typography>
            <Typography variant="body2" sx={{ color: '#5A5F6B' }}>
              {restaurantFilter || serviceQualityFilter
                ? 'No tips match your current filters. Try adjusting or resetting.'
                : 'Start recording tips to see your analytics here.'}
            </Typography>
            {(restaurantFilter || serviceQualityFilter) && (
              <Button onClick={resetFilters} sx={{ mt: 2, color: '#fd5b38' }}>Reset Filters</Button>
            )}
          </CardContent>
        </Card>
      )}

      {/* Data State */}
      {!loading && !error && analytics && analytics.totalTipCount > 0 && (
        <Box component={motion.div} variants={containerVariants} initial="hidden" animate="show">

          {/* Overview Stats */}
          <Grid container spacing={3} sx={{ mb: 4 }}>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <Typography variant="caption" sx={{ color: '#94A3B8', textTransform: 'uppercase', letterSpacing: 1 }}>Total Tips</Typography>
                  <Typography variant="h3" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 1 }}>{analytics.totalTipCount}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <Typography variant="caption" sx={{ color: '#94A3B8', textTransform: 'uppercase', letterSpacing: 1 }}>Avg Tip %</Typography>
                  <Typography variant="h3" sx={{ color: '#fd5b38', fontWeight: 700, mt: 1 }}>{formatPct(analytics.averageTipPercentage)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <Typography variant="caption" sx={{ color: '#94A3B8', textTransform: 'uppercase', letterSpacing: 1 }}>Median Tip %</Typography>
                  <Typography variant="h3" sx={{ color: '#38bdf8', fontWeight: 700, mt: 1 }}>{formatPct(analytics.medianTipPercentage)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} md={3}>
              <Card component={motion.div} variants={itemVariants}>
                <CardContent sx={{ textAlign: 'center', py: 3 }}>
                  <Typography variant="caption" sx={{ color: '#94A3B8', textTransform: 'uppercase', letterSpacing: 1 }}>Range</Typography>
                  <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 1 }}>
                    {formatPct(analytics.lowestTipPercentage)} – {formatPct(analytics.highestTipPercentage)}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Monthly Trend Chart */}
          {analytics.monthlyTrend && analytics.monthlyTrend.length > 0 && (
            <Card component={motion.div} variants={itemVariants} sx={{ mb: 4 }}>
              <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                  <TrendIcon sx={{ color: '#38bdf8' }} />
                  <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Monthly Trend</Typography>
                </Box>
                <Box sx={{ width: '100%', height: 280 }}>
                  <ResponsiveContainer>
                    <AreaChart data={analytics.monthlyTrend} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                      <defs>
                        <linearGradient id="tipGradient" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#38bdf8" stopOpacity={0.3} />
                          <stop offset="95%" stopColor="#38bdf8" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                      <XAxis dataKey="month" stroke="#94A3B8" tick={{ fill: '#94A3B8', fontSize: 12 }} />
                      <YAxis stroke="#94A3B8" tick={{ fill: '#94A3B8', fontSize: 12 }} unit="%" />
                      <RechartsTooltip
                        contentStyle={{ background: '#1A1F2E', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#FFFFFF' }}
                        labelStyle={{ color: '#94A3B8' }}
                        formatter={(value, name) => {
                          if (name === 'averageTipPercentage') return [`${Number(value).toFixed(1)}%`, 'Avg Tip %'];
                          if (name === 'tipCount') return [value, 'Tips'];
                          return [value, name];
                        }}
                      />
                      <Area type="monotone" dataKey="averageTipPercentage" stroke="#38bdf8" fill="url(#tipGradient)" strokeWidth={2} dot={{ fill: '#38bdf8', strokeWidth: 2, r: 4 }} />
                      <Line type="monotone" dataKey="tipCount" stroke="#fd5b38" strokeWidth={2} dot={{ fill: '#fd5b38', strokeWidth: 2, r: 3 }} yAxisId={0} />
                    </AreaChart>
                  </ResponsiveContainer>
                </Box>
              </CardContent>
            </Card>
          )}

          <Grid container spacing={3}>
            {/* Service Quality Breakdown */}
            {analytics.serviceQualityInsights && analytics.serviceQualityInsights.length > 0 && (
              <Grid item xs={12} md={6}>
                <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                  <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                      <StarIcon sx={{ color: '#FFA726' }} />
                      <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Service Quality Breakdown</Typography>
                    </Box>
                    <Box sx={{ width: '100%', height: 220, mb: 2 }}>
                      <ResponsiveContainer>
                        <BarChart
                          data={analytics.serviceQualityInsights.map((sq) => ({
                            ...sq,
                            name: sq.serviceQuality,
                            fill: SERVICE_QUALITIES.find((s) => s.value === sq.serviceQuality)?.color || '#94A3B8',
                          }))}
                          margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                          <XAxis dataKey="name" stroke="#94A3B8" tick={{ fill: '#94A3B8', fontSize: 11 }} />
                          <YAxis stroke="#94A3B8" tick={{ fill: '#94A3B8', fontSize: 12 }} />
                          <RechartsTooltip
                            contentStyle={{ background: '#1A1F2E', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#FFFFFF' }}
                            formatter={(value, name) => {
                              if (name === 'tipCount') return [value, 'Tips'];
                              if (name === 'averageTipPercentage') return [`${Number(value).toFixed(1)}%`, 'Avg Tip %'];
                              return [value, name];
                            }}
                          />
                          <Bar dataKey="tipCount" fill="#fd5b38" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </Box>
                    <Stack spacing={1}>
                      {analytics.serviceQualityInsights.map((sq) => {
                        const meta = SERVICE_QUALITIES.find((s) => s.value === sq.serviceQuality);
                        return (
                          <Box
                            key={sq.serviceQuality}
                            sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 1.5, background: 'rgba(0,0,0,0.2)', borderRadius: 1.5 }}
                          >
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Box sx={{ width: 8, height: 8, borderRadius: '50%', background: meta?.color || '#94A3B8' }} />
                              <Typography variant="body2" sx={{ color: '#FFFFFF' }}>{meta?.label || sq.serviceQuality}</Typography>
                            </Box>
                            <Box sx={{ display: 'flex', gap: 2 }}>
                              <Typography variant="caption" sx={{ color: '#94A3B8' }}>{sq.tipCount} tips</Typography>
                              <Typography variant="caption" sx={{ color: meta?.color || '#94A3B8' }}>avg {formatPct(sq.averageTipPercentage)}</Typography>
                            </Box>
                          </Box>
                        );
                      })}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Currency Breakdown */}
            {analytics.currencyBreakdown && analytics.currencyBreakdown.length > 0 && (
              <Grid item xs={12} md={6}>
                <Card component={motion.div} variants={itemVariants} sx={{ height: '100%' }}>
                  <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                      <WalletIcon sx={{ color: '#fd5b38' }} />
                      <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Currency Breakdown</Typography>
                      {analytics.currencyBreakdown.length > 1 && (
                        <Chip label="Multi-currency" size="small" sx={{ ml: 1, backgroundColor: 'rgba(255, 167, 38, 0.1)', color: '#FFA726', border: '1px solid rgba(255, 167, 38, 0.3)' }} />
                      )}
                    </Box>
                    <Stack spacing={2}>
                      {analytics.currencyBreakdown.map((curr) => (
                        <Box
                          key={curr.currency}
                          sx={{ p: 2.5, background: 'rgba(255,255,255,0.03)', borderRadius: 2, border: '1px solid rgba(255,255,255,0.06)' }}
                        >
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                            <Typography variant="subtitle1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{curr.currency}</Typography>
                            <Chip label={`${curr.tipCount} tips`} size="small" sx={{ backgroundColor: 'rgba(253, 91, 56, 0.15)', color: '#fd5b38' }} />
                          </Box>
                          <Grid container spacing={2}>
                            <Grid item xs={4}>
                              <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>Total</Typography>
                              <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                                {formatAmt(curr.totalTipAmount, curr.currency)}
                              </Typography>
                            </Grid>
                            <Grid item xs={4}>
                              <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>Average</Typography>
                              <Typography variant="body1" sx={{ color: '#fd5b38', fontWeight: 600 }}>
                                {formatAmt(curr.averageTipAmount, curr.currency)}
                              </Typography>
                            </Grid>
                            <Grid item xs={4}>
                              <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>Median</Typography>
                              <Typography variant="body1" sx={{ color: '#38bdf8', fontWeight: 600 }}>
                                {formatAmt(curr.medianTipAmount, curr.currency)}
                              </Typography>
                            </Grid>
                          </Grid>
                        </Box>
                      ))}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>

          {/* Restaurant Insights */}
          {analytics.restaurantInsights && analytics.restaurantInsights.length > 0 && (
            <Card component={motion.div} variants={itemVariants} sx={{ mt: 4 }}>
              <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
                  <RestaurantIcon sx={{ color: '#66BB6A' }} />
                  <Typography variant="h6" sx={{ color: '#FFFFFF' }}>Restaurant Insights</Typography>
                </Box>
                <Grid container spacing={2}>
                  {analytics.restaurantInsights.map((rest) => (
                    <Grid item xs={12} sm={6} md={4} key={rest.restaurantName}>
                      <Box
                        sx={{
                          p: 2.5,
                          background: 'rgba(255,255,255,0.03)',
                          borderRadius: 2,
                          border: '1px solid rgba(255,255,255,0.06)',
                          height: '100%',
                          transition: 'border-color 0.2s ease',
                          '&:hover': { borderColor: 'rgba(102, 187, 106, 0.3)' },
                        }}
                      >
                        <Typography variant="subtitle1" sx={{ color: '#FFFFFF', fontWeight: 600, mb: 1.5 }}>
                          {rest.restaurantName}
                        </Typography>
                        <Stack spacing={0.75}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="caption" sx={{ color: '#94A3B8' }}>Visits</Typography>
                            <Typography variant="caption" sx={{ color: '#FFFFFF' }}>{rest.visitCount}</Typography>
                          </Box>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="caption" sx={{ color: '#94A3B8' }}>Typical tip</Typography>
                            <Typography variant="caption" sx={{ color: '#fd5b38' }}>{formatPct(rest.medianTipPercentage)}</Typography>
                          </Box>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="caption" sx={{ color: '#94A3B8' }}>Last tip</Typography>
                            <Typography variant="caption" sx={{ color: '#38bdf8' }}>{formatPct(rest.lastTipPercentage)}</Typography>
                          </Box>
                          {rest.lastVisit && (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                              <Typography variant="caption" sx={{ color: '#94A3B8' }}>Last visit</Typography>
                              <Typography variant="caption" sx={{ color: '#FFFFFF' }}>
                                {new Date(rest.lastVisit).toLocaleDateString()}
                              </Typography>
                            </Box>
                          )}
                          {rest.mostCommonServiceQuality && (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                              <Typography variant="caption" sx={{ color: '#94A3B8' }}>Common rating</Typography>
                              <Typography
                                variant="caption"
                                sx={{ color: SERVICE_QUALITIES.find((s) => s.value === rest.mostCommonServiceQuality)?.color || '#94A3B8' }}
                              >
                                {rest.mostCommonServiceQuality.charAt(0) + rest.mostCommonServiceQuality.slice(1).toLowerCase()}
                              </Typography>
                            </Box>
                          )}
                        </Stack>
                      </Box>
                    </Grid>
                  ))}
                </Grid>
              </CardContent>
            </Card>
          )}
        </Box>
      )}
    </Box>
  );
};

export default TipAnalyticsPage;
