import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Chip,
  Alert,
  CircularProgress,
  TextField,
  MenuItem,
  IconButton,
  Button,
  Stack,
} from '@mui/material';
import {
  WarningAmber as WarningAmberIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  ArrowBack as BackIcon,
  HealthAndSafety as HealthIcon,
  CheckCircle as CheckCircleIcon,
  FilterAlt as FilterIcon,
  RestartAlt as ResetIcon,
  Shield as ShieldIcon,
  BugReport as BugIcon,
} from '@mui/icons-material';
import { motion } from 'framer-motion';
import dataQualityApi from '../api/dataQualityApi';

const TipDataQualityPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    currency: '',
    severity: '',
    anomalyType: ''
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const activeFilters = Object.fromEntries(
        Object.entries(filters).filter(([_, v]) => v !== '')
      );
      const res = await dataQualityApi.getDataQuality(activeFilters);
      setData(res);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch data quality', err);
      setError('Could not load data quality information.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [filters]);

  const handleFilterChange = (e) => {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  };

  const resetFilters = () => {
    setFilters({ currency: '', severity: '', anomalyType: '' });
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'HIGH': return '#FF6B6B';
      case 'WARNING': return '#fd5b38';
      case 'INFO': return '#38bdf8';
      default: return '#94A3B8';
    }
  };

  const getSeverityBg = (severity) => {
    switch (severity) {
      case 'HIGH': return 'rgba(244, 63, 94, 0.08)';
      case 'WARNING': return 'rgba(253, 91, 56, 0.08)';
      case 'INFO': return 'rgba(56, 189, 248, 0.08)';
      default: return 'rgba(148, 163, 184, 0.08)';
    }
  };

  const getSeverityIcon = (severity) => {
    const iconSx = { fontSize: 20, color: getSeverityColor(severity) };
    switch (severity) {
      case 'HIGH': return <ErrorIcon sx={iconSx} />;
      case 'WARNING': return <WarningAmberIcon sx={iconSx} />;
      case 'INFO': return <InfoIcon sx={iconSx} />;
      default: return null;
    }
  };

  // Animation variants
  const containerVariants = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.06 } },
  };
  const itemVariants = {
    hidden: { opacity: 0, y: 14 },
    show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 24 } },
  };

  const healthScore = data ? (data.totalTips > 0 ? Math.round((data.cleanTips / data.totalTips) * 100) : 100) : 0;
  const healthColor = healthScore >= 90 ? '#10B981' : healthScore >= 70 ? '#38bdf8' : '#FF6B6B';

  if (loading && !data) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <Box sx={{ textAlign: 'center' }}>
          <CircularProgress sx={{ color: '#fd5b38', mb: 2 }} size={48} />
          <Typography variant="body2" sx={{ color: '#64748B' }}>Analysing data quality...</Typography>
        </Box>
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 8, px: 3 }}>
        <Alert
          severity="error"
          sx={{ borderRadius: 3, border: '1px solid rgba(244, 63, 94, 0.2)', background: 'rgba(244, 63, 94, 0.08)' }}
        >
          {error}
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ px: { xs: 2, sm: 4, md: 6, lg: 8 }, py: { xs: 4, md: 6 }, width: '100%', maxWidth: '100%' }}>
      {/* ═══ Page Header ═══ */}
      <Box
        component={motion.div}
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        sx={{ mb: 5, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 2 }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={() => navigate('/dashboard')} sx={{ color: '#64748B', '&:hover': { color: '#F8FAFC' } }}>
            <BackIcon />
          </IconButton>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <Typography variant="h3" sx={{ color: '#FFFFFF', fontWeight: 700 }}>
                Data Quality
              </Typography>
              <Chip
                icon={<ShieldIcon sx={{ fontSize: 14 }} />}
                label="Health Monitor"
                size="small"
                sx={{
                  height: 24,
                  fontSize: '0.7rem',
                  fontWeight: 600,
                  background: 'rgba(16, 185, 129, 0.1)',
                  color: '#34D399',
                  border: '1px solid rgba(16, 185, 129, 0.2)',
                }}
              />
            </Box>
            <Typography variant="body1" sx={{ color: '#64748B', mt: 0.5 }}>
              System integrity monitoring & anomaly detection
            </Typography>
          </Box>
        </Box>
      </Box>

      <Box component={motion.div} variants={containerVariants} initial="hidden" animate="show">
        {/* ═══ Health Score Cards ═══ */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          {/* Health Score */}
          <Grid item xs={12} sm={6} md={3}>
            <Card component={motion.div} variants={itemVariants}>
              <CardContent sx={{ textAlign: 'center', py: 3.5, position: 'relative', overflow: 'hidden' }}>
                <Box sx={{
                  position: 'absolute', top: 0, left: 0, right: 0, height: 3,
                  background: `linear-gradient(90deg, transparent, ${healthColor}, transparent)`,
                  opacity: 0.7,
                }} />
                <Typography variant="caption" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600 }}>
                  Health Score
                </Typography>
                <Box sx={{ position: 'relative', display: 'inline-flex', mt: 2, mb: 1 }}>
                  <CircularProgress
                    variant="determinate"
                    value={healthScore}
                    size={72}
                    thickness={4}
                    sx={{
                      color: healthColor,
                      '& .MuiCircularProgress-circle': { strokeLinecap: 'round' },
                    }}
                  />
                  <Box sx={{
                    position: 'absolute', inset: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Typography variant="h5" sx={{ color: healthColor, fontWeight: 800 }}>
                      {healthScore}%
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Total Tips */}
          <Grid item xs={6} sm={6} md={3}>
            <Card component={motion.div} variants={itemVariants}>
              <CardContent sx={{ textAlign: 'center', py: 3.5 }}>
                <Typography variant="caption" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600 }}>
                  Total Records
                </Typography>
                <Typography variant="h3" sx={{ color: '#FFFFFF', fontWeight: 700, mt: 1 }}>
                  {data?.totalTips || 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          {/* Clean Records */}
          <Grid item xs={6} sm={6} md={3}>
            <Card component={motion.div} variants={itemVariants}>
              <CardContent sx={{ textAlign: 'center', py: 3.5, position: 'relative', overflow: 'hidden' }}>
                <Box sx={{
                  position: 'absolute', top: 0, left: 0, right: 0, height: 3,
                  background: 'linear-gradient(90deg, transparent, #10B981, transparent)',
                  opacity: 0.5,
                }} />
                <Typography variant="caption" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600 }}>
                  Clean Records
                </Typography>
                <Typography variant="h3" sx={{ color: '#10B981', fontWeight: 700, mt: 1 }}>
                  {data?.cleanTips || 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          {/* Anomalies */}
          <Grid item xs={12} sm={6} md={3}>
            <Card component={motion.div} variants={itemVariants}>
              <CardContent sx={{ textAlign: 'center', py: 3.5, position: 'relative', overflow: 'hidden' }}>
                {(data?.anomalyCount > 0) && (
                  <Box sx={{
                    position: 'absolute', top: 0, left: 0, right: 0, height: 3,
                    background: 'linear-gradient(90deg, transparent, #fd5b38, transparent)',
                    opacity: 0.6,
                  }} />
                )}
                <Typography variant="caption" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600 }}>
                  Anomalies
                </Typography>
                <Typography variant="h3" sx={{
                  color: data?.anomalyCount > 0 ? '#fd5b38' : '#10B981',
                  fontWeight: 700, mt: 1,
                }}>
                  {data?.anomalyCount || 0}
                </Typography>
                <Stack direction="row" spacing={0.75} justifyContent="center" sx={{ mt: 1.5 }}>
                  {data?.highSeverityCount > 0 && (
                    <Chip size="small" label={`${data.highSeverityCount} High`}
                      sx={{ height: 22, fontSize: '0.7rem', fontWeight: 600, background: 'rgba(244, 63, 94, 0.12)', color: '#FF8E8E', border: '1px solid rgba(244, 63, 94, 0.2)' }}
                    />
                  )}
                  {data?.warningCount > 0 && (
                    <Chip size="small" label={`${data.warningCount} Warn`}
                      sx={{ height: 22, fontSize: '0.7rem', fontWeight: 600, background: 'rgba(253, 91, 56, 0.12)', color: '#fd5b38', border: '1px solid rgba(253, 91, 56, 0.2)' }}
                    />
                  )}
                  {data?.infoCount > 0 && (
                    <Chip size="small" label={`${data.infoCount} Info`}
                      sx={{ height: 22, fontSize: '0.7rem', fontWeight: 600, background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8', border: '1px solid rgba(56, 189, 248, 0.2)' }}
                    />
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* ═══ Filters ═══ */}
        <Card component={motion.div} variants={itemVariants} sx={{ mb: 4 }}>
          <CardContent sx={{ py: 2.5, px: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
              <FilterIcon sx={{ color: '#64748B', fontSize: 20 }} />
              <Typography variant="subtitle2" sx={{ color: '#64748B', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600 }}>
                Filters
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2.5, alignItems: 'center' }}>
              <TextField
                select
                name="currency"
                value={filters.currency}
                onChange={handleFilterChange}
                size="small"
                label="Currency"
                sx={{
                  minWidth: 200,
                  flex: { xs: '1 1 100%', sm: '1 1 200px', md: '0 0 220px' },
                  '& .MuiInputBase-root': { height: 44, borderRadius: 2 },
                  '& .MuiInputLabel-root': { fontSize: '0.95rem' }
                }}
              >
                <MenuItem value="">All Currencies</MenuItem>
                {data?.currencies?.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>

              <TextField
                select
                name="severity"
                value={filters.severity}
                onChange={handleFilterChange}
                size="small"
                label="Severity"
                sx={{
                  minWidth: 200,
                  flex: { xs: '1 1 100%', sm: '1 1 200px', md: '0 0 220px' },
                  '& .MuiInputBase-root': { height: 44, borderRadius: 2 },
                  '& .MuiInputLabel-root': { fontSize: '0.95rem' }
                }}
              >
                <MenuItem value="">All Severities</MenuItem>
                <MenuItem value="HIGH">High Severity</MenuItem>
                <MenuItem value="WARNING">Warning</MenuItem>
                <MenuItem value="INFO">Informational</MenuItem>
              </TextField>

              <TextField
                select
                name="anomalyType"
                value={filters.anomalyType}
                onChange={handleFilterChange}
                size="small"
                label="Anomaly Type"
                sx={{
                  minWidth: 240,
                  flex: { xs: '1 1 100%', sm: '1 1 240px', md: '0 0 260px' },
                  '& .MuiInputBase-root': { height: 44, borderRadius: 2 },
                  '& .MuiInputLabel-root': { fontSize: '0.95rem' }
                }}
              >
                <MenuItem value="">All Anomaly Types</MenuItem>
                <MenuItem value="UNUSUAL_TIP_PERCENTAGE">Unusual Percentage</MenuItem>
                <MenuItem value="EXTREME_TIP_AMOUNT">Extreme Amount</MenuItem>
                <MenuItem value="UNUSUAL_BILL_AMOUNT">Missing/Unusual Bill</MenuItem>
                <MenuItem value="DUPLICATE_LIKE_RECORD">Duplicate-like Record</MenuItem>
                <MenuItem value="MISSING_RESTAURANT">Missing Restaurant</MenuItem>
                <MenuItem value="INVALID_CURRENCY">Invalid Currency</MenuItem>
              </TextField>

              <Button
                startIcon={<ResetIcon />}
                onClick={resetFilters}
                variant="outlined"
                sx={{
                  height: 44,
                  px: 3,
                  borderRadius: 2,
                  borderColor: 'rgba(255, 255, 255, 0.15)',
                  color: '#94A3B8',
                  fontWeight: 600,
                  textTransform: 'none',
                  fontSize: '0.92rem',
                  '&:hover': { borderColor: '#fd5b38', color: '#fd5b38', backgroundColor: 'rgba(253, 91, 56, 0.08)' }
                }}
              >
                Reset Filters
              </Button>
            </Box>
          </CardContent>
        </Card>

        {/* ═══ Anomaly Details ═══ */}
        <Box component={motion.div} variants={itemVariants}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
            <BugIcon sx={{ color: '#fd5b38' }} />
            <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600 }}>
              Anomaly Details
            </Typography>
            {data?.anomalies?.length > 0 && (
              <Chip
                label={`${data.anomalies.length} found`}
                size="small"
                sx={{ fontSize: '0.7rem', fontWeight: 600, background: 'rgba(253, 91, 56, 0.1)', color: '#fd5b38', border: '1px solid rgba(253, 91, 56, 0.2)' }}
              />
            )}
          </Box>

          {data?.anomalies?.length === 0 ? (
            <Card>
              <CardContent sx={{ py: 6, textAlign: 'center' }}>
                <CheckCircleIcon sx={{ fontSize: 48, color: '#10B981', mb: 2, opacity: 0.8 }} />
                <Typography variant="h6" sx={{ color: '#34D399', mb: 1 }}>All Clear</Typography>
                <Typography variant="body2" sx={{ color: '#64748B' }}>
                  No data anomalies detected. Your tipping records meet all quality standards.
                </Typography>
              </CardContent>
            </Card>
          ) : (
            <Stack spacing={2}>
              {data.anomalies.map((anomaly) => (
                <Card
                  key={anomaly.id}
                  sx={{
                    borderLeft: `4px solid ${getSeverityColor(anomaly.severity)}`,
                    transition: 'border-color 0.2s, box-shadow 0.2s',
                    '&:hover': {
                      borderColor: 'rgba(255, 255, 255, 0.15)',
                      boxShadow: '0 8px 30px rgba(0, 0, 0, 0.4)',
                    },
                  }}
                >
                  <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                    {/* Header Row */}
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
                        {getSeverityIcon(anomaly.severity)}
                        <Typography variant="subtitle1" sx={{ color: '#F8FAFC', fontWeight: 600 }}>
                          {anomaly.anomalyType ? anomaly.anomalyType.replace(/_/g, ' ') : 'Anomaly'}
                        </Typography>
                        <Chip
                          size="small"
                          label={anomaly.severity}
                          sx={{
                            height: 22,
                            fontSize: '0.65rem',
                            fontWeight: 700,
                            letterSpacing: 0.5,
                            background: getSeverityBg(anomaly.severity),
                            color: getSeverityColor(anomaly.severity),
                            border: `1px solid ${getSeverityColor(anomaly.severity)}30`,
                          }}
                        />
                      </Box>
                      {anomaly.date && (
                        <Typography variant="caption" sx={{ color: '#64748B', whiteSpace: 'nowrap' }}>
                          {new Date(anomaly.date).toLocaleDateString()}
                        </Typography>
                      )}
                    </Box>

                    {/* Description */}
                    <Typography variant="body2" sx={{ color: '#94A3B8', mb: 2.5, lineHeight: 1.6 }}>
                      {anomaly.description}
                    </Typography>

                    {/* Details Grid */}
                    <Grid container spacing={2} sx={{ p: 2, borderRadius: 2, background: 'rgba(255, 255, 255, 0.02)', border: '1px solid rgba(255, 255, 255, 0.04)' }}>
                      <Grid item xs={6} sm={3}>
                        <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                          Restaurant
                        </Typography>
                        <Typography variant="body2" sx={{ color: '#CBD5E1', fontWeight: 500, mt: 0.25 }}>
                          {anomaly.restaurantName || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6} sm={3}>
                        <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                          Bill Amount
                        </Typography>
                        <Typography variant="body2" sx={{ color: '#CBD5E1', fontWeight: 500, mt: 0.25 }}>
                          {anomaly.billAmount !== null ? anomaly.billAmount.toFixed(2) : 'N/A'} {anomaly.currency}
                        </Typography>
                      </Grid>
                      <Grid item xs={6} sm={3}>
                        <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                          Tip Amount
                        </Typography>
                        <Typography variant="body2" sx={{ color: '#38bdf8', fontWeight: 500, mt: 0.25 }}>
                          {anomaly.tipAmount !== null ? anomaly.tipAmount.toFixed(2) : 'N/A'} {anomaly.currency}
                        </Typography>
                      </Grid>
                      <Grid item xs={6} sm={3}>
                        <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                          Tip %
                        </Typography>
                        <Typography variant="body2" sx={{ color: '#fd5b38', fontWeight: 600, mt: 0.25 }}>
                          {anomaly.tipPercentage !== null ? `${anomaly.tipPercentage}%` : 'N/A'}
                        </Typography>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </Box>
      </Box>
    </Box>
  );
};

export default TipDataQualityPage;
