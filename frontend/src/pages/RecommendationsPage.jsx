import React, { useState, useEffect } from 'react';
import { Box, Typography, Card, CardContent, CircularProgress, Alert, Chip, Grid, Button, Paper, Divider, IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { tipRecommendationApi } from '../api/tipRecommendationApi';
import { tipRecommendationActionApi } from '../api/tipRecommendationActionApi';
import { 
    Error as ErrorIcon, 
    Warning as WarningIcon, 
    Info as InfoIcon, 
    CheckCircle as CheckCircleIcon, 
    AutoAwesome as AutoAwesomeIcon,
    Close as CloseIcon,
    AccessTime as AccessTimeIcon,
    Done as DoneIcon,
    History as HistoryIcon
} from '@mui/icons-material';

const RecommendationsPage = () => {
    const [recommendationsData, setRecommendationsData] = useState(null);
    const [historyData, setHistoryData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [snoozeAnchorEl, setSnoozeAnchorEl] = useState(null);
    const [selectedRecForSnooze, setSelectedRecForSnooze] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const [recData, histData] = await Promise.all([
                tipRecommendationApi.getRecommendations(),
                tipRecommendationActionApi.getHistory()
            ]);
            setRecommendationsData(recData);
            setHistoryData(histData);
            setError('');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (rec, action, snoozeDays = null) => {
        try {
            let snoozeUntil = null;
            if (action === 'SNOOZED' && snoozeDays) {
                const date = new Date();
                date.setDate(date.getDate() + snoozeDays);
                snoozeUntil = date.toISOString();
            }

            await tipRecommendationActionApi.performAction(rec.type, rec.currency, {
                action,
                snoozeUntil
            });

            // Optimistic update
            if (action === 'DISMISSED' || action === 'SNOOZED') {
                setRecommendationsData(prev => ({
                    ...prev,
                    recommendations: prev.recommendations.filter(r => r !== rec)
                }));
            } else if (action === 'REVIEWED') {
                setRecommendationsData(prev => ({
                    ...prev,
                    recommendations: prev.recommendations.map(r => 
                        r === rec ? { ...r, userAction: 'REVIEWED' } : r
                    )
                }));
            }
            
            // Reload history to reflect the new action
            const histData = await tipRecommendationActionApi.getHistory();
            setHistoryData(histData);
        } catch (err) {
            alert('Failed to perform action: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleSnoozeClick = (event, rec) => {
        setSnoozeAnchorEl(event.currentTarget);
        setSelectedRecForSnooze(rec);
    };

    const handleSnoozeClose = (days) => {
        if (days && selectedRecForSnooze) {
            handleAction(selectedRecForSnooze, 'SNOOZED', days);
        }
        setSnoozeAnchorEl(null);
        setSelectedRecForSnooze(null);
    };

    const getPriorityColor = (priority) => {
        switch (priority) {
            case 'HIGH': return 'error';
            case 'MEDIUM': return 'warning';
            case 'LOW': return 'info';
            default: return 'default';
        }
    };

    const getPriorityIcon = (priority) => {
        switch (priority) {
            case 'HIGH': return <ErrorIcon fontSize="small" sx={{ color: '#FF3B30' }} />;
            case 'MEDIUM': return <WarningIcon fontSize="small" sx={{ color: '#FF9500' }} />;
            case 'LOW': return <InfoIcon fontSize="small" sx={{ color: '#0A84FF' }} />;
            default: return <InfoIcon fontSize="small" sx={{ color: '#94A3B8' }} />;
        }
    };

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return <Alert severity="error">{error}</Alert>;
    }

    return (
        <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 }, width: '100%', maxWidth: '100%' }}>
            {/* ═══ Header ═══ */}
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 4, gap: 2 }}>
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
                    <AutoAwesomeIcon sx={{ fontSize: 24, color: '#FFFFFF' }} />
                </Box>
                <Box>
                    <Typography sx={{ fontSize: '0.75rem', fontWeight: 800, letterSpacing: '0.12em', color: '#fd5b38', textTransform: 'uppercase' }}>
                        INTELLIGENT ADVISORY
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 800, color: '#F8FAFC', letterSpacing: '-0.025em' }}>
                        Smart Recommendations
                    </Typography>
                </Box>
            </Box>

            {/* Summary Section */}
            {recommendationsData?.summary && (
                <Card sx={{ p: 3, mb: 4, background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))', border: '1px solid rgba(253, 91, 56, 0.25)', borderRadius: 3 }}>
                    <Grid container spacing={2} alignItems="center">
                        <Grid item xs={12} md={8}>
                            <Typography variant="h6" fontWeight="bold" sx={{ color: '#FFFFFF', mb: 1 }}>Overview</Typography>
                            <Typography variant="body1" sx={{ whiteSpace: 'pre-line', color: '#94A3B8' }}>
                                {recommendationsData.summary}
                            </Typography>
                        </Grid>
                    </Grid>
                </Card>
            )}

            {/* AI Explanation Section */}
            {recommendationsData?.aiExplanation && (
                <Card sx={{ mb: 4, background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.12) 0%, rgba(20, 24, 41, 0.9) 100%)', border: '1px solid rgba(253, 91, 56, 0.3)', borderRadius: 3 }}>
                    <CardContent>
                        <Box display="flex" alignItems="center" gap={1.5} mb={2}>
                            <AutoAwesomeIcon sx={{ color: '#fd5b38' }} />
                            <Typography variant="h6" fontWeight="bold" sx={{ color: '#fd5b38' }}>AI Explanation</Typography>
                        </Box>
                        <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap', color: '#E8EAED', lineHeight: 1.7 }}>
                            {recommendationsData.aiExplanation}
                        </Typography>
                    </CardContent>
                </Card>
            )}

            {/* Recommendations List */}
            <Typography variant="h5" fontWeight="bold" mb={3}>Action Plan</Typography>
            
            {(!recommendationsData || !recommendationsData.recommendations || recommendationsData.recommendations.length === 0) ? (
                <Card sx={{ p: 4, textAlign: 'center', backgroundColor: 'background.paper', borderRadius: 2, mb: 4 }}>
                    <CheckCircleIcon sx={{ fontSize: 60, color: 'success.main', mb: 2 }} />
                    <Typography variant="h5" gutterBottom fontWeight="medium">You're doing well.</Typography>
                    <Typography color="text.secondary">
                        There are no important recommendations based on your current data. 
                        More tipping history or active goals may be needed to generate personalised insights.
                    </Typography>
                </Card>
            ) : (
                <Grid container spacing={3} mb={4}>
                    {recommendationsData.recommendations.map((rec, index) => (
                        <Grid item xs={12} key={index}>
                            <Card sx={{ 
                                borderLeft: 6, 
                                borderColor: `${getPriorityColor(rec.priority)}.main`,
                                transition: 'transform 0.2s',
                                '&:hover': { transform: 'translateY(-4px)', boxShadow: 4 }
                            }}>
                                <CardContent>
                                    <Grid container spacing={2} alignItems="center">
                                        <Grid item>
                                            {getPriorityIcon(rec.priority)}
                                        </Grid>
                                        <Grid item xs>
                                            <Box display="flex" alignItems="center" gap={1} mb={1}>
                                                <Chip 
                                                    label={rec.priority} 
                                                    color={getPriorityColor(rec.priority)} 
                                                    size="small" 
                                                    sx={{ fontWeight: 'bold' }} 
                                                />
                                                <Typography variant="overline" color="text.secondary" fontWeight="bold">
                                                    {rec.type.replace(/_/g, ' ')}
                                                </Typography>
                                                {rec.userAction === 'REVIEWED' && (
                                                    <Chip label="Reviewed" size="small" variant="outlined" color="success" icon={<DoneIcon />} />
                                                )}
                                            </Box>
                                            <Typography variant="h6" fontWeight="bold" gutterBottom>{rec.title}</Typography>
                                            <Typography variant="body1" color="text.secondary" mb={2}>
                                                {rec.message}
                                            </Typography>
                                            
                                            {rec.supportingValue && (
                                                <Box sx={{ p: 1.5, backgroundColor: 'action.hover', borderRadius: 1, display: 'inline-block' }}>
                                                    <Typography variant="body2" color="text.secondary">
                                                        {rec.supportingValueLabel}: 
                                                        <Typography component="span" fontWeight="bold" ml={1} color="text.primary">
                                                            {rec.currency ? `${rec.currency} ` : ''}{rec.supportingValue}
                                                        </Typography>
                                                    </Typography>
                                                </Box>
                                            )}
                                        </Grid>
                                        <Grid item xs={12} md="auto" sx={{ display: 'flex', flexDirection: 'column', gap: 1, mt: { xs: 2, md: 0 } }}>
                                            <Button 
                                                variant="contained" 
                                                color="primary" 
                                                onClick={() => navigate(rec.action)}
                                                sx={{ borderRadius: 2 }}
                                            >
                                                Review Details
                                            </Button>
                                            <Box display="flex" justifyContent="flex-end" gap={1}>
                                                <Tooltip title="Mark as Reviewed">
                                                    <IconButton size="small" onClick={() => handleAction(rec, 'REVIEWED')} color="success">
                                                        <DoneIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Snooze">
                                                    <IconButton size="small" onClick={(e) => handleSnoozeClick(e, rec)} color="warning">
                                                        <AccessTimeIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Dismiss">
                                                    <IconButton size="small" onClick={() => handleAction(rec, 'DISMISSED')} color="error">
                                                        <CloseIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            </Box>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            )}

            <Menu
                anchorEl={snoozeAnchorEl}
                open={Boolean(snoozeAnchorEl)}
                onClose={() => handleSnoozeClose(null)}
            >
                <MenuItem onClick={() => handleSnoozeClose(1)}>Snooze for 1 day</MenuItem>
                <MenuItem onClick={() => handleSnoozeClose(3)}>Snooze for 3 days</MenuItem>
                <MenuItem onClick={() => handleSnoozeClose(7)}>Snooze for 7 days</MenuItem>
            </Menu>

            {/* Recommendation History Section */}
            <Box mt={6}>
                <Box display="flex" alignItems="center" gap={1} mb={3}>
                    <HistoryIcon color="action" />
                    <Typography variant="h5" fontWeight="bold">Recommendation History</Typography>
                </Box>
                {(!historyData || historyData.actions.length === 0) ? (
                    <Typography color="text.secondary">No recent recommendation actions.</Typography>
                ) : (
                    <Grid container spacing={2}>
                        {historyData.actions.slice(0, 10).map((hist, idx) => (
                            <Grid item xs={12} sm={6} md={4} key={idx}>
                                <Card variant="outlined" sx={{ backgroundColor: 'background.default' }}>
                                    <CardContent>
                                        <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
                                            <Typography variant="subtitle2" fontWeight="bold">
                                                {hist.recommendationType.replace(/_/g, ' ')}
                                            </Typography>
                                            <Chip 
                                                label={hist.action} 
                                                size="small" 
                                                color={hist.action === 'REVIEWED' ? 'success' : (hist.action === 'SNOOZED' ? 'warning' : 'error')}
                                                variant="outlined" 
                                            />
                                        </Box>
                                        <Typography variant="body2" color="text.secondary">
                                            {hist.currency ? `Currency: ${hist.currency}` : 'Global Recommendation'}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" display="block" mt={1}>
                                            {new Date(hist.createdAt).toLocaleString()}
                                        </Typography>
                                        {hist.snoozedUntil && (
                                            <Typography variant="caption" color="warning.main" display="block">
                                                Snoozed until {new Date(hist.snoozedUntil).toLocaleDateString()}
                                            </Typography>
                                        )}
                                    </CardContent>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Box>
        </Box>
    );
};

export default RecommendationsPage;
