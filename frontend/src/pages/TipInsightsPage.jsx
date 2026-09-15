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
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Container sx={{ mt: 5 }}>
        <Alert severity="error">{error}</Alert>
      </Container>
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

  return (
    <Container sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        Smart Tip Insights & Spending Trends
      </Typography>
      
      <Grid container spacing={3}>
        {/* Overall Trend Card */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%', p: 2 }}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Overall Tipping Trend
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', mt: 2, mb: 3 }}>
                {overallTrend.trendDirection === 'INCREASING' && <><TrendingUp color="success" fontSize="large" /> <Typography variant="h5" color="success.main" sx={{ ml: 1 }}>Increasing</Typography></>}
                {overallTrend.trendDirection === 'DECREASING' && <><TrendingDown color="error" fontSize="large" /> <Typography variant="h5" color="error.main" sx={{ ml: 1 }}>Decreasing</Typography></>}
                {overallTrend.trendDirection === 'STABLE' && <><TrendingFlat color="primary" fontSize="large" /> <Typography variant="h5" color="primary.main" sx={{ ml: 1 }}>Stable</Typography></>}
                {overallTrend.trendDirection === 'INSUFFICIENT_DATA' && <><InfoOutlined color="action" fontSize="large" /> <Typography variant="h5" color="textSecondary" sx={{ ml: 1 }}>Not enough data</Typography></>}
              </Box>
              <Typography variant="body1">
                <strong>Recent Average:</strong> {overallTrend.recentAveragePercentage != null ? overallTrend.recentAveragePercentage + '%' : 'N/A'}
              </Typography>
              <Typography variant="body1">
                <strong>Previous Average:</strong> {overallTrend.historicalAveragePercentage != null ? overallTrend.historicalAveragePercentage + '%' : 'N/A'}
              </Typography>
              {overallTrend.percentageChange != null && (
                <Typography variant="body1">
                  <strong>Change:</strong> {overallTrend.percentageChange > 0 ? '+' : ''}{overallTrend.percentageChange}%
                </Typography>
              )}
              <Box sx={{ mt: 2 }}>
                <Chip label={`Confidence: ${overallTrend.confidence}`} color={overallTrend.confidence === 'HIGH' ? 'success' : overallTrend.confidence === 'MEDIUM' ? 'warning' : 'default'} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* AI Insight Card */}
        <Grid item xs={12} md={8}>
          <Card sx={{ height: '100%', p: 2 }}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                AI Interpretation
              </Typography>
              {aiMessage ? (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="h6" color="primary" gutterBottom>
                    {aiMessage.headline}
                  </Typography>
                  <Typography variant="body1" paragraph>
                    {aiMessage.explanation}
                  </Typography>
                  <Alert severity="info" sx={{ mt: 2 }}>
                    <strong>Suggestion:</strong> {aiMessage.suggestion}
                  </Alert>
                  <Typography variant="caption" color="textSecondary" sx={{ mt: 2, display: 'block' }}>
                    * This interpretation is generated by AI based on your factual backend trends.
                  </Typography>
                </Box>
              ) : (
                <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
                  AI insight generation is currently unavailable. Your factual backend stats are displayed below.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Monthly Trend Chart */}
        <Grid item xs={12}>
          <Card sx={{ p: 2 }}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Monthly Average Tip Percentage
              </Typography>
              <Box sx={{ height: 300, mt: 3 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={monthlyData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" />
                    <YAxis domain={['auto', 'auto']} tickFormatter={(tick) => `${tick}%`} />
                    <Tooltip formatter={(value) => `${value}%`} />
                    <Legend />
                    <Line type="monotone" dataKey="averageTipPercentage" stroke="#8884d8" name="Avg Tip %" />
                  </LineChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Top Restaurants */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%', p: 2 }}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Top Restaurants
              </Typography>
              {topRestaurants && topRestaurants.length > 0 ? (
                <TableContainer component={Paper} elevation={0} sx={{ mt: 2 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Restaurant</TableCell>
                        <TableCell align="right">Visits</TableCell>
                        <TableCell align="right">Avg %</TableCell>
                        <TableCell align="right">Trend</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {topRestaurants.map((rt, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{rt.restaurantName} <Typography variant="caption" color="textSecondary">({rt.currency})</Typography></TableCell>
                          <TableCell align="right">{rt.tipCount}</TableCell>
                          <TableCell align="right">{rt.averageTipPercentage}%</TableCell>
                          <TableCell align="right">
                            {rt.trendDirection === 'INCREASING' && <TrendingUp color="success" fontSize="small" />}
                            {rt.trendDirection === 'DECREASING' && <TrendingDown color="error" fontSize="small" />}
                            {rt.trendDirection === 'STABLE' && <TrendingFlat color="primary" fontSize="small" />}
                            {rt.trendDirection === 'INSUFFICIENT_DATA' && "-"}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
                  Need at least 2 visits to a restaurant to show trends.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Service Quality Chart */}
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%', p: 2 }}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Service Quality Impact
              </Typography>
              {serviceQualityData && serviceQualityData.length > 0 ? (
                <Box sx={{ height: 250, mt: 3 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={serviceQualityData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="quality" />
                      <YAxis domain={[0, 'auto']} tickFormatter={(tick) => `${tick}%`} />
                      <Tooltip formatter={(value) => `${value}%`} />
                      <Bar dataKey="averageTipPercentage" fill="#82ca9d" name="Avg Tip %" />
                    </BarChart>
                  </ResponsiveContainer>
                </Box>
              ) : (
                <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
                  No service quality ratings available.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
};

export default TipInsightsPage;
