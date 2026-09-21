import React, { useState, useEffect } from 'react';
import { Box, Container, Grid, Card, CardContent, Typography, CircularProgress, Alert, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip } from '@mui/material';
import { TrendingUp, TrendingDown, TrendingFlat, InfoOutlined } from '@mui/icons-material';
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, LineChart, Line, CartesianGrid } from 'recharts';
import { getTipInsights } from '../api/tipInsightsApi';

const TipInsightsPage = () => {
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchInsights = async () => {
      try {
        const data = await getTipInsights();
        setInsights(data);
      } catch (err) {
        setError('Failed to load insights. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    fetchInsights();
  }, []);

  if (loading) {
    return (
      <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 }, display: 'flex', justifyContent: 'center', p: 8 }}>
        <CircularProgress sx={{ color: '#fd5b38' }} />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 } }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  const overallTrend = insights?.overallTrend || {
    trendDirection: 'INSUFFICIENT_DATA',
    recentAveragePercentage: null,
    historicalAveragePercentage: null,
    percentageChange: null,
    confidence: 'LOW'
  };
  const topRestaurants = Array.isArray(insights?.topRestaurants) ? insights.topRestaurants : [];
  const serviceQualityTrends = insights?.serviceQualityTrends || {};
  const monthlyTrends = insights?.monthlyTrends || {};
  const generatedInsightMessage = insights?.generatedInsightMessage || null;
  
  // Prepare data for monthly chart
  const monthlyData = Object.keys(monthlyTrends).map(month => ({
    month,
    averageTipPercentage: monthlyTrends[month]?.averageTipPercentage ?? 0,
    tipCount: monthlyTrends[month]?.tipCount ?? 0
  }));

  // Prepare data for service quality chart
  const serviceQualityData = Object.keys(serviceQualityTrends).map(quality => ({
    quality,
    averageTipPercentage: serviceQualityTrends[quality]?.averageTipPercentage ?? 0
  }));

  let aiMessage = null;
  if (generatedInsightMessage) {
    try {
      aiMessage = typeof generatedInsightMessage === 'string' 
        ? JSON.parse(generatedInsightMessage) 
        : generatedInsightMessage;
    } catch (e) {
      aiMessage = { explanation: generatedInsightMessage };
    }
  }

  const cardStyle = {
    background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))',
    backdropFilter: 'blur(16px)',
    border: '1px solid rgba(253, 91, 56, 0.25)',
    borderRadius: 3.5,
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.35)',
    height: '100%',
    p: 1
  };

  return (
    <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 }, width: '100%', maxWidth: 1300, mx: 'auto' }}>
      {/* ═══ Header ═══ */}
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
            <TrendingUp sx={{ fontSize: 24, color: '#FFFFFF' }} />
          </Box>
          <Box>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 800, letterSpacing: '0.12em', color: '#fd5b38', textTransform: 'uppercase' }}>
              INTELLIGENCE & TRENDS
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#F8FAFC', letterSpacing: '-0.025em', mt: 0.2 }}>
              Smart Tip Insights & Spending Trends
            </Typography>
          </Box>
        </Box>
      </Box>
      
      <Grid container spacing={3}>
        {/* Overall Trend Card */}
        <Grid item xs={12} md={4}>
          <Card sx={cardStyle}>
            <CardContent>
              <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', mb: 2 }}>
                Overall Tipping Trend
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', mt: 1, mb: 2.5 }}>
                {overallTrend.trendDirection === 'INCREASING' && <><TrendingUp sx={{ color: '#00e676', fontSize: 36 }} /> <Typography variant="h5" sx={{ color: '#00e676', ml: 1, fontWeight: 700 }}>Increasing</Typography></>}
                {overallTrend.trendDirection === 'DECREASING' && <><TrendingDown sx={{ color: '#ff5252', fontSize: 36 }} /> <Typography variant="h5" sx={{ color: '#ff5252', ml: 1, fontWeight: 700 }}>Decreasing</Typography></>}
                {overallTrend.trendDirection === 'STABLE' && <><TrendingFlat sx={{ color: '#fd5b38', fontSize: 36 }} /> <Typography variant="h5" sx={{ color: '#fd5b38', ml: 1, fontWeight: 700 }}>Stable</Typography></>}
                {overallTrend.trendDirection === 'INSUFFICIENT_DATA' && <><InfoOutlined sx={{ color: '#94A3B8', fontSize: 36 }} /> <Typography variant="h5" sx={{ color: '#94A3B8', ml: 1, fontWeight: 600 }}>Not enough data</Typography></>}
              </Box>
              <Typography variant="body1" sx={{ color: '#E8EAED', mb: 0.8 }}>
                <strong style={{ color: '#FFFFFF' }}>Recent Average:</strong> {overallTrend.recentAveragePercentage != null ? overallTrend.recentAveragePercentage + '%' : 'N/A'}
              </Typography>
              <Typography variant="body1" sx={{ color: '#E8EAED', mb: 0.8 }}>
                <strong style={{ color: '#FFFFFF' }}>Historical Average:</strong> {overallTrend.historicalAveragePercentage != null ? overallTrend.historicalAveragePercentage + '%' : 'N/A'}
              </Typography>
              {overallTrend.percentageChange != null && (
                <Typography variant="body1" sx={{ color: '#E8EAED', mb: 1 }}>
                  <strong style={{ color: '#FFFFFF' }}>Change:</strong> {overallTrend.percentageChange > 0 ? '+' : ''}{overallTrend.percentageChange}%
                </Typography>
              )}
              <Box sx={{ mt: 2.5 }}>
                <Chip 
                  label={`Confidence: ${overallTrend.confidence}`} 
                  sx={{
                    fontWeight: 700,
                    background: overallTrend.confidence === 'HIGH' ? 'rgba(0, 230, 118, 0.15)' : 'rgba(253, 91, 56, 0.15)',
                    color: overallTrend.confidence === 'HIGH' ? '#00e676' : '#fd5b38',
                    border: '1px solid rgba(253, 91, 56, 0.3)'
                  }} 
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* AI Insight Card */}
        <Grid item xs={12} md={8}>
          <Card sx={cardStyle}>
            <CardContent>
              <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', mb: 1.5 }}>
                AI Interpretation
              </Typography>
              {aiMessage ? (
                <Box sx={{ mt: 1.5 }}>
                  <Typography variant="h6" sx={{ color: '#fd5b38', fontWeight: 700, mb: 1 }}>
                    {aiMessage.headline}
                  </Typography>
                  <Typography variant="body1" sx={{ color: '#E8EAED', mb: 2, lineHeight: 1.7 }}>
                    {aiMessage.explanation}
                  </Typography>
                  <Alert 
                    severity="info" 
                    sx={{ 
                      mt: 2, 
                      backgroundColor: 'rgba(253, 91, 56, 0.1)', 
                      color: '#F8FAFC', 
                      border: '1px solid rgba(253, 91, 56, 0.3)',
                      '& .MuiAlert-icon': { color: '#fd5b38' }
                    }}
                  >
                    <strong>Suggestion:</strong> {aiMessage.suggestion}
                  </Alert>
                  <Typography variant="caption" sx={{ color: '#64748B', mt: 2, display: 'block' }}>
                    * This interpretation is generated by AI based on your factual backend trends.
                  </Typography>
                </Box>
              ) : (
                <Typography variant="body2" sx={{ color: '#94A3B8', mt: 2 }}>
                  AI insight generation is currently unavailable. Your factual backend stats are displayed below.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Monthly Trend Chart */}
        <Grid item xs={12}>
          <Card sx={cardStyle}>
            <CardContent>
              <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', mb: 2 }}>
                Monthly Average Tip Percentage
              </Typography>
              <Box sx={{ height: 320, mt: 2 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={monthlyData} margin={{ top: 10, right: 25, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.08)" />
                    <XAxis dataKey="month" stroke="#94A3B8" />
                    <YAxis domain={['auto', 'auto']} stroke="#94A3B8" tickFormatter={(tick) => `${tick}%`} />
                    <Tooltip 
                      formatter={(value) => [`${value}%`, 'Avg Tip %']}
                      contentStyle={{ backgroundColor: '#15161d', borderColor: '#fd5b38', borderRadius: 10, color: '#fff' }}
                    />
                    <Legend />
                    <Line 
                      type="monotone" 
                      dataKey="averageTipPercentage" 
                      stroke="#fd5b38" 
                      strokeWidth={3}
                      dot={{ r: 4, fill: '#fd5b38' }}
                      activeDot={{ r: 7, fill: '#ff8a65' }}
                      name="Avg Tip %" 
                    />
                  </LineChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Top Restaurants */}
        <Grid item xs={12} md={6}>
          <Card sx={cardStyle}>
            <CardContent>
              <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', mb: 2 }}>
                Top Restaurants
              </Typography>
              {topRestaurants && topRestaurants.length > 0 ? (
                <TableContainer component={Paper} elevation={0} sx={{ mt: 1, background: 'transparent' }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow sx={{ borderBottom: '1px solid rgba(255, 255, 255, 0.1)' }}>
                        <TableCell sx={{ color: '#fd5b38', fontWeight: 700 }}>Restaurant</TableCell>
                        <TableCell align="right" sx={{ color: '#fd5b38', fontWeight: 700 }}>Visits</TableCell>
                        <TableCell align="right" sx={{ color: '#fd5b38', fontWeight: 700 }}>Avg %</TableCell>
                        <TableCell align="right" sx={{ color: '#fd5b38', fontWeight: 700 }}>Trend</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {topRestaurants.map((rt, idx) => (
                        <TableRow key={idx} sx={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)', '&:hover': { backgroundColor: 'rgba(253, 91, 56, 0.05)' } }}>
                          <TableCell sx={{ color: '#FFFFFF', fontWeight: 600 }}>
                            {rt.restaurantName} <Typography variant="caption" sx={{ color: '#94A3B8' }}>({rt.currency})</Typography>
                          </TableCell>
                          <TableCell align="right" sx={{ color: '#E8EAED' }}>{rt.tipCount}</TableCell>
                          <TableCell align="right" sx={{ color: '#fd5b38', fontWeight: 700 }}>{rt.averageTipPercentage}%</TableCell>
                          <TableCell align="right">
                            {rt.trendDirection === 'INCREASING' && <TrendingUp sx={{ color: '#00e676', fontSize: 20 }} />}
                            {rt.trendDirection === 'DECREASING' && <TrendingDown sx={{ color: '#ff5252', fontSize: 20 }} />}
                            {rt.trendDirection === 'STABLE' && <TrendingFlat sx={{ color: '#fd5b38', fontSize: 20 }} />}
                            {rt.trendDirection === 'INSUFFICIENT_DATA' && <span style={{ color: '#94A3B8' }}>-</span>}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Typography variant="body2" sx={{ color: '#94A3B8', mt: 2 }}>
                  Need at least 2 visits to a restaurant to show trends.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Service Quality Chart */}
        <Grid item xs={12} md={6}>
          <Card sx={cardStyle}>
            <CardContent>
              <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', mb: 2 }}>
                Service Quality Impact
              </Typography>
              {serviceQualityData && serviceQualityData.length > 0 ? (
                <Box sx={{ height: 260, mt: 2 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={serviceQualityData} margin={{ top: 10, right: 20, left: 10, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.08)" />
                      <XAxis dataKey="quality" stroke="#94A3B8" />
                      <YAxis domain={[0, 'auto']} stroke="#94A3B8" tickFormatter={(tick) => `${tick}%`} />
                      <Tooltip 
                        formatter={(value) => [`${value}%`, 'Avg Tip %']}
                        contentStyle={{ backgroundColor: '#15161d', borderColor: '#fd5b38', borderRadius: 10, color: '#fff' }}
                      />
                      <Bar dataKey="averageTipPercentage" fill="#fd5b38" radius={[4, 4, 0, 0]} name="Avg Tip %" />
                    </BarChart>
                  </ResponsiveContainer>
                </Box>
              ) : (
                <Typography variant="body2" sx={{ color: '#94A3B8', mt: 2 }}>
                  No service quality ratings available.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default TipInsightsPage;
