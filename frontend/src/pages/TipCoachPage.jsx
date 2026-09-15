import React, { useState, useEffect } from 'react';
import { 
    Box, 
    Container, 
    Typography, 
    Card, 
    CardContent, 
    Grid,
    Button,
    Chip,
    CircularProgress,
    Alert,
    Divider
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import tipCoachApi from '../api/tipCoachApi';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import LightbulbIcon from '@mui/icons-material/Lightbulb';
import TrackChangesIcon from '@mui/icons-material/TrackChanges';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import AssignmentIcon from '@mui/icons-material/Assignment';
import ExploreIcon from '@mui/icons-material/Explore';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

const TipCoachPage = () => {
    const [coaching, setCoaching] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        loadCoaching();
    }, []);

    const loadCoaching = async () => {
        try {
            setLoading(true);
            const data = await tipCoachApi.getCoaching();
            setCoaching(data);
        } catch (err) {
            console.error('Failed to load coaching data', err);
            setError('Failed to load your coaching insights. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <Container maxWidth="md" sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxWidth="md" sx={{ mt: 4 }}>
                <Alert severity="error">{error}</Alert>
            </Container>
        );
    }

    if (!coaching) return null;

    const getFocusIcon = (focus) => {
        switch (focus) {
            case 'BUDGET': return <AccountBalanceWalletIcon fontSize="large" color="error" />;
            case 'FORECAST': return <TrendingUpIcon fontSize="large" color="warning" />;
            case 'GOAL': return <TrackChangesIcon fontSize="large" color="primary" />;
            case 'DATA_QUALITY': return <ReportProblemIcon fontSize="large" color="error" />;
            case 'CONSISTENCY': return <AssignmentIcon fontSize="large" color="info" />;
            case 'TIP_OPTIMIZATION': return <LightbulbIcon fontSize="large" color="secondary" />;
            case 'EXPLORATION': return <ExploreIcon fontSize="large" color="info" />;
            case 'POSITIVE_PROGRESS': return <CheckCircleIcon fontSize="large" color="success" />;
            default: return <AutoAwesomeIcon fontSize="large" color="disabled" />;
        }
    };

    const handleActionClick = () => {
        switch (coaching.focus) {
            case 'BUDGET':
            case 'FORECAST':
                navigate('/dashboard'); // Assuming budget/forecast are on dashboard
                break;
            case 'GOAL':
                navigate('/goals');
                break;
            case 'DATA_QUALITY':
                navigate('/dashboard'); // Or data quality page if it exists
                break;
            case 'CONSISTENCY':
            case 'TIP_OPTIMIZATION':
            case 'EXPLORATION':
                navigate('/tip-profile');
                break;
            case 'POSITIVE_PROGRESS':
            case 'NO_ACTION':
            default:
                navigate('/dashboard');
                break;
        }
    };

    return (
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
            <Box mb={4}>
                <Typography variant="h4" component="h1" gutterBottom fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <AutoAwesomeIcon color="primary" /> Smart Tipping Coach
                </Typography>
                <Typography variant="subtitle1" color="text.secondary">
                    Your tipping history, goals, budget and patterns — brought together.
                </Typography>
            </Box>

            {/* Hero Section */}
            <Card className="webfolio-card" sx={{ mb: 4, background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))', border: '1px solid rgba(253, 91, 56, 0.3)' }}>
                <CardContent sx={{ textAlign: 'center', py: 5 }}>
                    <Box sx={{ mb: 2 }}>
                        {getFocusIcon(coaching.focus)}
                    </Box>
                    <Chip label={`Focus: ${coaching.focus.replace('_', ' ')}`} color="primary" variant="outlined" sx={{ mb: 2, fontWeight: 800, borderColor: 'rgba(253, 91, 56, 0.4)', color: '#fd5b38' }} />
                    <Typography variant="h5" component="h2" gutterBottom fontWeight="bold" sx={{ color: '#F8FAFC' }}>
                        {coaching.headline}
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#94A3B8', maxWidth: 700, mx: 'auto' }} paragraph>
                        {coaching.summary}
                    </Typography>
                    <Button variant="contained" className="butn butn-bg" size="large" onClick={handleActionClick} sx={{ mt: 2, borderRadius: 2.5, px: 4, background: 'linear-gradient(135deg, #fd5b38 0%, #d63d19 100%)', color: '#fff', fontWeight: 700 }}>
                        {coaching.nextAction}
                    </Button>
                </CardContent>
            </Card>

            {/* AI Summary Section */}
            {coaching.aiExplanation && (
                <Card className="bevel-card" sx={{ mb: 4, p: 1, border: '1px solid rgba(56, 189, 248, 0.25)' }}>
                    <CardContent>
                        <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, color: '#38bdf8', fontWeight: 700 }}>
                            <AutoAwesomeIcon fontSize="small" sx={{ color: '#38bdf8' }} /> AI Coach Summary
                        </Typography>
                        <Typography variant="body1" sx={{ color: '#CBD5E1', lineHeight: 1.6 }}>
                            {coaching.aiExplanation}
                        </Typography>
                    </CardContent>
                </Card>
            )}

            {/* Snapshots Grid */}
            <Grid container spacing={3}>
                
                {/* Profile Snapshot */}
                {coaching.profileSummary && (
                    <Grid item xs={12} md={6}>
                        <Card className="bevel-card" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Typography variant="h6" gutterBottom sx={{ color: '#F8FAFC', fontWeight: 700 }}>Your Tipping Profile</Typography>
                                <Divider sx={{ mb: 2 }} />
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                    <Typography color="text.secondary">Behaviour:</Typography>
                                    <Typography fontWeight="bold" sx={{ color: '#F8FAFC' }}>{coaching.profileSummary.overallBehaviorType || 'N/A'}</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                    <Typography color="text.secondary">Style:</Typography>
                                    <Typography fontWeight="bold" sx={{ color: '#F8FAFC' }}>{coaching.profileSummary.overallTipStyle || 'N/A'}</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                    <Typography color="text.secondary">Consistency:</Typography>
                                    <Typography fontWeight="bold" sx={{ color: '#10B981' }}>{coaching.profileSummary.consistencyScore !== null ? `${coaching.profileSummary.consistencyScore}/100` : 'N/A'}</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <Typography color="text.secondary">Recent Trend:</Typography>
                                    <Typography fontWeight="bold" sx={{ color: '#F8FAFC' }}>{coaching.profileSummary.recentTrend || 'N/A'}</Typography>
                                </Box>
                            </CardContent>
                            <Box sx={{ p: 2, pt: 0 }}>
                                <Button variant="outlined" fullWidth onClick={() => navigate('/tip-profile')} sx={{ borderColor: 'rgba(56, 189, 248, 0.3)', color: '#38bdf8' }}>View Profile</Button>
                            </Box>
                        </Card>
                    </Grid>
                )}

                {/* Goals Snapshot */}
                {coaching.goalSummary && coaching.goalSummary.length > 0 && (
                    <Grid item xs={12} md={6}>
                        <Card className="bevel-card" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Typography variant="h6" gutterBottom sx={{ color: '#F8FAFC', fontWeight: 700 }}>Active Goals</Typography>
                                <Divider sx={{ mb: 2 }} />
                                {coaching.goalSummary.map((goal, idx) => (
                                    <Box key={goal.id} sx={{ mb: 2 }}>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                            <Typography variant="body2" fontWeight="bold" sx={{ color: '#F8FAFC' }}>{goal.goalType} Goal</Typography>
                                            <Typography variant="body2" sx={{ color: '#fd5b38', fontWeight: 800 }}>{goal.progressPercentage}%</Typography>
                                        </Box>
                                        <Box sx={{ width: '100%', bgcolor: 'rgba(255, 255, 255, 0.08)', borderRadius: 5, height: 8 }}>
                                            <Box sx={{ width: `${Math.min(100, goal.progressPercentage)}%`, background: 'linear-gradient(90deg, #fd5b38, #ff8566)', height: '100%', borderRadius: 5 }} />
                                        </Box>
                                    </Box>
                                ))}
                            </CardContent>
                            <Box sx={{ p: 2, pt: 0 }}>
                                <Button variant="outlined" fullWidth onClick={() => navigate('/tip-goals')} sx={{ borderColor: 'rgba(253, 91, 56, 0.3)', color: '#fd5b38' }}>View Goals</Button>
                            </Box>
                        </Card>
                    </Grid>
                )}

                {/* Budget Snapshot */}
                <Grid item xs={12} md={6}>
                    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <CardContent sx={{ flexGrow: 1 }}>
                            <Typography variant="h6" gutterBottom>Budget</Typography>
                            <Divider sx={{ mb: 2 }} />
                            {coaching.budgetSummary ? (
                                <>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                        <Typography color="text.secondary">Status:</Typography>
                                        <Chip 
                                            label={coaching.budgetSummary.status.replace('_', ' ')} 
                                            size="small"
                                            color={
                                                coaching.budgetSummary.status === 'ON_TRACK' ? 'success' : 
                                                (coaching.budgetSummary.status === 'APPROACHING_LIMIT' ? 'warning' : 'error')
                                            } 
                                        />
                                    </Box>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                        <Typography color="text.secondary">Current Usage:</Typography>
                                        <Typography fontWeight="bold">{coaching.budgetSummary.usagePercentage.toFixed(1)}%</Typography>
                                    </Box>
                                </>
                            ) : (
                                <Typography variant="body2" color="text.secondary">No budget configured.</Typography>
                            )}
                        </CardContent>
                        <Box sx={{ p: 2, pt: 0 }}>
                            <Button variant="outlined" fullWidth onClick={() => navigate('/dashboard')}>View Budget</Button>
                        </Box>
                    </Card>
                </Grid>

                {/* Forecast Snapshot */}
                {coaching.forecastSummary && (
                    <Grid item xs={12} md={6}>
                        <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Typography variant="h6" gutterBottom>Forecast</Typography>
                                <Divider sx={{ mb: 2 }} />
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                    <Typography color="text.secondary">Projected Tips:</Typography>
                                    <Typography fontWeight="bold">{coaching.forecastSummary.projectedTipCount}</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                    <Typography color="text.secondary">Projected Amount:</Typography>
                                    <Typography fontWeight="bold">{coaching.forecastSummary.projectedTotalTipAmount}</Typography>
                                </Box>
                                {coaching.forecastSummary.budgetStatus && (
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                        <Typography color="text.secondary">Budget Status:</Typography>
                                        <Typography fontWeight="bold">{coaching.forecastSummary.budgetStatus.replace('_', ' ')}</Typography>
                                    </Box>
                                )}
                            </CardContent>
                        </Card>
                    </Grid>
                )}
            </Grid>
            
            {/* Recommendations Section */}
            {coaching.recommendations && coaching.recommendations.length > 0 && (
                <Box sx={{ mt: 4 }}>
                    <Typography variant="h5" gutterBottom>Recommended Actions</Typography>
                    <Grid container spacing={2}>
                        {coaching.recommendations.map((rec, idx) => (
                            <Grid item xs={12} md={4} key={idx}>
                                <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                                    <CardContent>
                                        <Typography variant="subtitle2" color="primary" gutterBottom>
                                            {rec.type.replace('_', ' ')}
                                        </Typography>
                                        <Typography variant="h6" gutterBottom>{rec.headline}</Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            {rec.description}
                                        </Typography>
                                    </CardContent>
                                    <Box sx={{ p: 2, pt: 0, mt: 'auto' }}>
                                        <Button variant="contained" size="small" fullWidth onClick={() => navigate('/recommendations')}>View Recommendation</Button>
                                    </Box>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                </Box>
            )}

        </Container>
    );
};

export default TipCoachPage;
