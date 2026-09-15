import React, { useState, useEffect } from 'react';
import { Box, Typography, Card, CardContent, CircularProgress, Alert, Chip, Grid, Divider, Paper } from '@mui/material';
import { motion } from 'framer-motion';
import {
    Person as PersonIcon,
    AutoAwesome as AutoAwesomeIcon,
    Restaurant as RestaurantIcon,
    Stars as StarsIcon,
    Timeline as TimelineIcon,
    TrendingUp as TrendingUpIcon,
    TrendingDown as TrendingDownIcon,
    TrendingFlat as TrendingFlatIcon,
    CheckCircle as CheckCircleIcon,
    Info as InfoIcon
} from '@mui/icons-material';
import { tipProfileApi } from '../api/tipProfileApi';

const TipProfilePage = () => {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const loadProfile = async () => {
            try {
                setLoading(true);
                const data = await tipProfileApi.getProfile();
                setProfile(data);
                setError('');
            } catch (err) {
                setError('Failed to load tipping profile');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        loadProfile();
    }, []);

    const getBehaviorLabel = (type) => {
        switch (type) {
            case 'VERY_CONSISTENT': return 'Very Consistent';
            case 'CONSISTENT': return 'Consistent';
            case 'VARIABLE': return 'Variable';
            case 'HIGHLY_VARIABLE': return 'Highly Variable';
            default: return 'Unknown';
        }
    };

    const getBehaviorColor = (type) => {
        switch (type) {
            case 'VERY_CONSISTENT': return 'success';
            case 'CONSISTENT': return 'info';
            case 'VARIABLE': return 'warning';
            case 'HIGHLY_VARIABLE': return 'error';
            default: return 'default';
        }
    };

    const getStyleLabel = (style) => {
        switch (style) {
            case 'CONSERVATIVE': return 'Conservative';
            case 'MODERATE': return 'Moderate';
            case 'GENEROUS': return 'Generous';
            case 'VERY_GENEROUS': return 'Very Generous';
            default: return 'Unknown';
        }
    };

    const getTrendIcon = (trend) => {
        switch (trend) {
            case 'MORE_GENEROUS': return <TrendingUpIcon sx={{ color: '#4caf50' }} />;
            case 'MORE_CONSERVATIVE': return <TrendingDownIcon sx={{ color: '#f44336' }} />;
            case 'STABLE': return <TrendingFlatIcon sx={{ color: '#2196f3' }} />;
            default: return <InfoIcon sx={{ color: '#9e9e9e' }} />;
        }
    };

    const getTrendLabel = (trend) => {
        switch (trend) {
            case 'MORE_GENEROUS': return 'More generous recently';
            case 'MORE_CONSERVATIVE': return 'More conservative recently';
            case 'STABLE': return 'Relatively stable';
            default: return 'Not enough history';
        }
    };

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
                <CircularProgress sx={{ color: '#fd5b38' }} />
            </Box>
        );
    }

    if (error) {
        return (
            <Box sx={{ p: 4, maxWidth: 1200, mx: 'auto' }}>
                <Alert severity="error" sx={{ borderRadius: 2 }}>{error}</Alert>
            </Box>
        );
    }

    const hasData = profile && profile.totalTipCount > 0;

    return (
        <Box sx={{ p: { xs: 2, md: 4 }, maxWidth: 1200, mx: 'auto' }}>
            <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', gap: 2 }}>
                <PersonIcon sx={{ fontSize: 32, color: '#fd5b38' }} />
                <Box>
                    <Typography variant="h4" sx={{ fontWeight: 800, color: '#FFFFFF', letterSpacing: '-0.5px' }}>
                        My Tipping Profile
                    </Typography>
                    <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.7)' }}>
                        Understand your tipping behaviour from your actual history.
                    </Typography>
                </Box>
            </Box>

            {!hasData ? (
                <Card sx={{ background: 'rgba(255,255,255,0.03)', backdropFilter: 'blur(10px)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 4 }}>
                    <CardContent sx={{ textAlign: 'center', py: 8 }}>
                        <InfoIcon sx={{ fontSize: 64, color: 'rgba(255,255,255,0.2)', mb: 2 }} />
                        <Typography variant="h6" sx={{ color: '#FFFFFF', mb: 1 }}>No History Yet</Typography>
                        <Typography sx={{ color: 'rgba(255,255,255,0.7)' }}>{profile.message}</Typography>
                    </CardContent>
                </Card>
            ) : (
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
                    <Grid container spacing={3}>
                        
                        {/* HERO SECTION */}
                        <Grid item xs={12}>
                            <Card sx={{ 
                                background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.1) 0%, rgba(20, 25, 40, 0.8) 100%)',
                                border: '1px solid rgba(253, 91, 56, 0.25)', 
                                borderRadius: 4,
                                position: 'relative',
                                overflow: 'hidden'
                            }}>
                                <CardContent sx={{ p: 4 }}>
                                    <Grid container spacing={4} alignItems="center">
                                        <Grid item xs={12} md={4}>
                                            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,0.5)', fontWeight: 600 }}>OVERALL STYLE</Typography>
                                            <Typography variant="h3" sx={{ color: '#FFFFFF', fontWeight: 800, mt: 1, mb: 2 }}>
                                                {profile.overallTipStyle ? getStyleLabel(profile.overallTipStyle) : 'Pending'}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={12} md={4}>
                                            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,0.5)', fontWeight: 600 }}>BEHAVIOUR</Typography>
                                            <Box sx={{ mt: 1 }}>
                                                {profile.overallBehaviorType ? (
                                                    <Chip 
                                                        label={getBehaviorLabel(profile.overallBehaviorType)} 
                                                        color={getBehaviorColor(profile.overallBehaviorType)}
                                                        sx={{ fontWeight: 600, fontSize: '1rem', py: 2 }} 
                                                    />
                                                ) : (
                                                    <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.5)' }}>Need 5+ tips</Typography>
                                                )}
                                            </Box>
                                        </Grid>
                                        <Grid item xs={12} md={4}>
                                            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,0.5)', fontWeight: 600 }}>CONSISTENCY SCORE</Typography>
                                            <Typography variant="h3" sx={{ color: '#FFFFFF', fontWeight: 800, mt: 1 }}>
                                                {profile.consistencyScore !== null ? `${profile.consistencyScore} / 100` : '—'}
                                            </Typography>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* KEY STATISTICS & RECENT TREND */}
                        <Grid item xs={12} md={6}>
                            <Card sx={{ height: '100%', background: 'rgba(20, 25, 40, 0.6)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 4 }}>
                                <CardContent sx={{ p: 3 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                                        <TimelineIcon sx={{ color: '#38bdf8' }} />
                                        <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 600 }}>Key Statistics</Typography>
                                    </Box>
                                    <Grid container spacing={2}>
                                        <Grid item xs={6}>
                                            <Paper sx={{ p: 2, background: 'rgba(0,0,0,0.2)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 2 }}>
                                                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>Total Tips</Typography>
                                                <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600, mt: 0.5 }}>{profile.totalTipCount}</Typography>
                                            </Paper>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Paper sx={{ p: 2, background: 'rgba(0,0,0,0.2)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 2 }}>
                                                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>Median Tip</Typography>
                                                <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600, mt: 0.5 }}>{profile.historicalMedianTipPercentage}%</Typography>
                                            </Paper>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Paper sx={{ p: 2, background: 'rgba(0,0,0,0.2)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 2 }}>
                                                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>Average Tip</Typography>
                                                <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600, mt: 0.5 }}>{profile.historicalAverageTipPercentage}%</Typography>
                                            </Paper>
                                        </Grid>
                                        <Grid item xs={6}>
                                            <Paper sx={{ p: 2, background: 'rgba(0,0,0,0.2)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 2 }}>
                                                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>Range</Typography>
                                                <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 600, mt: 0.5 }}>{profile.tipPercentageMin}% - {profile.tipPercentageMax}%</Typography>
                                            </Paper>
                                        </Grid>
                                    </Grid>

                                    <Divider sx={{ my: 3, borderColor: 'rgba(255,255,255,0.1)' }} />
                                    
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                                        <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 600 }}>Recent Behaviour</Typography>
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                                        {getTrendIcon(profile.recentTrend)}
                                        <Box>
                                            <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 500 }}>
                                                {getTrendLabel(profile.recentTrend)}
                                            </Typography>
                                            {profile.recentTrend && profile.recentMedianTipPercentage && (
                                                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>
                                                    Recent median: {profile.recentMedianTipPercentage}%
                                                </Typography>
                                            )}
                                        </Box>
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* AI EXPLANATION */}
                        {profile.aiExplanation && (
                            <Grid item xs={12} md={6}>
                                <Card sx={{ height: '100%', background: 'linear-gradient(135deg, rgba(19, 22, 34, 0.9) 0%, rgba(20, 25, 40, 0.9) 100%)', border: '1px solid rgba(253, 91, 56, 0.3)', borderRadius: 4 }}>
                                    <CardContent sx={{ p: 3 }}>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                                            <AutoAwesomeIcon sx={{ color: '#fd5b38' }} />
                                            <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 600 }}>AI Profile Summary</Typography>
                                        </Box>
                                        <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.7, whiteSpace: 'pre-line' }}>
                                            {profile.aiExplanation}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            </Grid>
                        )}

                        {/* CURRENCY PROFILES */}
                        <Grid item xs={12}>
                            <Typography variant="h5" sx={{ color: '#FFFFFF', fontWeight: 700, mb: 2, mt: 2 }}>Currency Profiles</Typography>
                            <Grid container spacing={2}>
                                {profile.currencyProfiles.map((cp) => (
                                    <Grid item xs={12} sm={6} md={4} key={cp.currency}>
                                        <Card sx={{ background: 'rgba(20, 25, 40, 0.6)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 3 }}>
                                            <CardContent>
                                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                                                    <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 700 }}>{cp.currency}</Typography>
                                                    <Chip label={`${cp.tipCount} tips`} size="small" sx={{ background: 'rgba(255,255,255,0.1)', color: '#FFFFFF' }} />
                                                </Box>
                                                <Grid container spacing={1}>
                                                    <Grid item xs={6}>
                                                        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>Median %</Typography>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{cp.medianTipPercentage}%</Typography>
                                                    </Grid>
                                                    <Grid item xs={6}>
                                                        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>Average %</Typography>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{cp.averageTipPercentage}%</Typography>
                                                    </Grid>
                                                    <Grid item xs={6}>
                                                        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>Avg Amount</Typography>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{cp.averageTipAmount}</Typography>
                                                    </Grid>
                                                    <Grid item xs={6}>
                                                        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>Style</Typography>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{getStyleLabel(cp.tipStyle)}</Typography>
                                                    </Grid>
                                                </Grid>
                                            </CardContent>
                                        </Card>
                                    </Grid>
                                ))}
                            </Grid>
                        </Grid>

                        {/* RESTAURANTS & SERVICE QUALITY */}
                        <Grid item xs={12} md={6}>
                            <Card sx={{ height: '100%', background: 'rgba(20, 25, 40, 0.6)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 4 }}>
                                <CardContent sx={{ p: 3 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                                        <RestaurantIcon sx={{ color: '#fd5b38' }} />
                                        <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 600 }}>Where You Tip Most</Typography>
                                    </Box>
                                    {profile.topRestaurants && profile.topRestaurants.length > 0 ? (
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                            {profile.topRestaurants.map((r, i) => (
                                                <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                                                    <Box>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600 }}>{r.restaurantName}</Typography>
                                                        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>{r.tipCount} visits</Typography>
                                                    </Box>
                                                    <Box sx={{ textAlign: 'right' }}>
                                                        <Typography variant="body1" sx={{ color: '#4caf50', fontWeight: 600 }}>Avg: {r.averageTipPercentage}%</Typography>
                                                    </Box>
                                                </Box>
                                            ))}
                                        </Box>
                                    ) : (
                                        <Typography sx={{ color: 'rgba(255,255,255,0.5)' }}>No restaurant data available.</Typography>
                                    )}
                                </CardContent>
                            </Card>
                        </Grid>

                        <Grid item xs={12} md={6}>
                            <Card sx={{ height: '100%', background: 'rgba(20, 25, 40, 0.6)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 4 }}>
                                <CardContent sx={{ p: 3 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                                        <StarsIcon sx={{ color: '#38bdf8' }} />
                                        <Typography variant="h6" sx={{ color: '#FFFFFF', fontWeight: 600 }}>Tipping by Service Quality</Typography>
                                    </Box>
                                    {profile.topServiceQualities && profile.topServiceQualities.length > 0 ? (
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                            {profile.topServiceQualities.map((sq, i) => (
                                                <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                                                    <Box>
                                                        <Typography variant="body1" sx={{ color: '#FFFFFF', fontWeight: 600, textTransform: 'capitalize' }}>{sq.serviceQuality.toLowerCase()}</Typography>
                                                        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.5)' }}>{sq.count} ratings</Typography>
                                                    </Box>
                                                    <Box sx={{ textAlign: 'right' }}>
                                                        <Typography variant="body1" sx={{ color: '#4caf50', fontWeight: 600 }}>Avg: {sq.averageTipPercentage}%</Typography>
                                                    </Box>
                                                </Box>
                                            ))}
                                        </Box>
                                    ) : (
                                        <Typography sx={{ color: 'rgba(255,255,255,0.5)' }}>No service quality ratings available.</Typography>
                                    )}
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>
                </motion.div>
            )}
        </Box>
    );
};

export default TipProfilePage;
