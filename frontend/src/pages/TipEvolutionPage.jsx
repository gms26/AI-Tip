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
    Divider,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    ToggleButton,
    ToggleButtonGroup,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip
} from '@mui/material';
import {
    Timeline as TimelineIcon,
    TrendingUp as TrendingUpIcon,
    TrendingDown as TrendingDownIcon,
    TrendingFlat as TrendingFlatIcon,
    AutoAwesome as AutoAwesomeIcon,
    CalendarMonth as CalendarIcon,
    Restaurant as RestaurantIcon,
    Speed as SpeedIcon,
    Stars as StarsIcon,
    Refresh as RefreshIcon,
    InfoOutlined as InfoIcon,
    MonetizationOn as MoneyIcon
} from '@mui/icons-material';
import { motion } from 'framer-motion';
import {
    ResponsiveContainer,
    LineChart,
    Line,
    AreaChart,
    Area,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    Legend
} from 'recharts';
import { useNavigate } from 'react-router-dom';
import { tipEvolutionApi } from '../api/tipEvolutionApi';

const TipEvolutionPage = () => {
    const navigate = useNavigate();
    const [evolution, setEvolution] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [period, setPeriod] = useState('LAST_6_MONTHS');
    const [selectedCurrency, setSelectedCurrency] = useState('');
    const [activeCurrencyTab, setActiveCurrencyTab] = useState(null);

    const loadEvolution = async (selectedPeriod = period, curr = selectedCurrency) => {
        try {
            setLoading(true);
            setError(null);
            const data = await tipEvolutionApi.getEvolution(curr || undefined, selectedPeriod);
            setEvolution(data);
            if (data?.currencyTimelines?.length > 0) {
                if (!activeCurrencyTab || !data.currencyTimelines.some(c => c.currency === activeCurrencyTab)) {
                    setActiveCurrencyTab(data.currencyTimelines[0].currency);
                }
            } else {
                setActiveCurrencyTab(null);
            }
        } catch (err) {
            console.error('Failed to load tip evolution data', err);
            setError('Failed to load tipping evolution data. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadEvolution(period, selectedCurrency);
    }, [period, selectedCurrency]);

    const handlePeriodChange = (event, newPeriod) => {
        if (newPeriod) {
            setPeriod(newPeriod);
        }
    };

    const handleCurrencyChange = (event) => {
        const val = event.target.value;
        setSelectedCurrency(val);
    };

    const getDirectionDetails = (dir) => {
        switch (dir) {
            case 'MORE_GENEROUS':
                return {
                    label: 'Becoming More Generous',
                    color: '#10b981',
                    bgColor: 'rgba(16, 185, 129, 0.15)',
                    borderColor: 'rgba(16, 185, 129, 0.3)',
                    icon: <TrendingUpIcon sx={{ color: '#10b981', fontSize: 24 }} />,
                    desc: 'Your median tip percentage has increased by 3.0% or more compared to earlier months.'
                };
            case 'MORE_CONSERVATIVE':
                return {
                    label: 'Becoming More Conservative',
                    color: '#f59e0b',
                    bgColor: 'rgba(245, 158, 11, 0.15)',
                    borderColor: 'rgba(245, 158, 11, 0.3)',
                    icon: <TrendingDownIcon sx={{ color: '#f59e0b', fontSize: 24 }} />,
                    desc: 'Your median tip percentage has decreased by 3.0% or more compared to earlier months.'
                };
            case 'STABLE':
                return {
                    label: 'Stable Tipping Pattern',
                    color: '#3b82f6',
                    bgColor: 'rgba(59, 130, 246, 0.15)',
                    borderColor: 'rgba(59, 130, 246, 0.3)',
                    icon: <TrendingFlatIcon sx={{ color: '#3b82f6', fontSize: 24 }} />,
                    desc: 'Your median tip percentage has remained steady within ±3.0% across time.'
                };
            case 'INSUFFICIENT_DATA':
            default:
                return {
                    label: 'Building Baseline',
                    color: '#94a3b8',
                    bgColor: 'rgba(148, 163, 184, 0.12)',
                    borderColor: 'rgba(148, 163, 184, 0.25)',
                    icon: <InfoIcon sx={{ color: '#94a3b8', fontSize: 24 }} />,
                    desc: 'Keep logging tips across at least two distinct calendar months to unlock directional trend analysis.'
                };
        }
    };

    const getStyleColor = (style) => {
        switch (style) {
            case 'VERY_GENEROUS': return { label: 'Very Generous', color: '#10b981' };
            case 'GENEROUS': return { label: 'Generous', color: '#06b6d4' };
            case 'MODERATE': return { label: 'Moderate', color: '#3b82f6' };
            case 'CONSERVATIVE': return { label: 'Conservative', color: '#8b5cf6' };
            default: return { label: style || 'N/A', color: '#94a3b8' };
        }
    };

    const getBehaviorColor = (type) => {
        switch (type) {
            case 'VERY_CONSISTENT': return { label: 'Very Consistent', color: '#10b981' };
            case 'CONSISTENT': return { label: 'Consistent', color: '#06b6d4' };
            case 'VARIABLE': return { label: 'Variable', color: '#f59e0b' };
            case 'HIGHLY_VARIABLE': return { label: 'Highly Variable', color: '#ef4444' };
            default: return { label: 'Evaluating (<5 tips)', color: '#64748b' };
        }
    };

    // Active currency timeline
    const activeCurrencyTimeline = evolution?.currencyTimelines?.find(c => c.currency === activeCurrencyTab)
        || evolution?.currencyTimelines?.[0];

    // Chart data prepared from active currency monthly timeline
    const chartData = (activeCurrencyTimeline?.monthlyTimeline || []).map(m => ({
        month: m.month,
        medianPct: m.medianTipPercentage != null ? Number(m.medianTipPercentage) : null,
        averagePct: m.averageTipPercentage != null ? Number(m.averageTipPercentage) : null,
        tipCount: m.tipCount,
        uniqueRestaurants: m.uniqueRestaurantCount,
        avgAmount: m.averageTipAmount != null ? Number(m.averageTipAmount) : null,
        consistency: m.consistencyScore != null ? Number(m.consistencyScore) : null,
        tipStyle: m.tipStyle,
        behaviorType: m.behaviorType
    }));

    // Service Quality Chart Data
    const sqChartData = (() => {
        if (!evolution?.serviceQualityEvolution || evolution.serviceQualityEvolution.length === 0) {
            return [];
        }
        const monthMap = {};
        evolution.serviceQualityEvolution.forEach(sq => {
            const qualityName = sq.serviceQuality;
            Object.entries(sq.monthlyValues || {}).forEach(([m, val]) => {
                if (!monthMap[m]) monthMap[m] = { month: m };
                monthMap[m][qualityName] = Number(val);
            });
        });
        return Object.values(monthMap).sort((a, b) => a.month.localeCompare(b.month));
    })();

    const dirInfo = getDirectionDetails(activeCurrencyTimeline?.overallDirection || evolution?.overallDirection);

    return (
        <Container maxWidth="xl" sx={{ py: 4 }}>
            {/* Header */}
            <Box sx={{ mb: 4, display: 'flex', flexDirection: { xs: 'column', md: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' }, gap: 2 }}>
                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
                        <Box sx={{
                            width: 42,
                            height: 42,
                            borderRadius: '12px',
                            background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 4px 14px rgba(59, 130, 246, 0.35)'
                        }}>
                            <TimelineIcon sx={{ color: '#ffffff', fontSize: 24 }} />
                        </Box>
                        <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: '#f8fafc', letterSpacing: '-0.02em' }}>
                            Tipping Evolution & Timeline
                        </Typography>
                    </Box>
                    <Typography variant="body1" sx={{ color: '#94a3b8' }}>
                        Deterministic historical analysis of your tipping generosity, consistency, and patterns over time.
                    </Typography>
                </Box>

                {/* Filters */}
                <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1.5 }}>
                    <ToggleButtonGroup
                        value={period}
                        exclusive
                        onChange={handlePeriodChange}
                        size="small"
                        sx={{
                            backgroundColor: 'rgba(30, 41, 59, 0.7)',
                            borderRadius: '10px',
                            p: '3px',
                            border: '1px solid rgba(255, 255, 255, 0.08)',
                            '& .MuiToggleButton-root': {
                                color: '#94a3b8',
                                border: 'none',
                                borderRadius: '8px',
                                px: 1.8,
                                py: 0.6,
                                textTransform: 'none',
                                fontWeight: 500,
                                fontSize: '0.825rem',
                                '&.Mui-selected': {
                                    backgroundColor: '#3b82f6',
                                    color: '#ffffff',
                                    '&:hover': {
                                        backgroundColor: '#2563eb'
                                    }
                                }
                            }
                        }}
                    >
                        <ToggleButton value="LAST_3_MONTHS">3 Months</ToggleButton>
                        <ToggleButton value="LAST_6_MONTHS">6 Months</ToggleButton>
                        <ToggleButton value="LAST_12_MONTHS">12 Months</ToggleButton>
                        <ToggleButton value="ALL_TIME">All Time</ToggleButton>
                    </ToggleButtonGroup>

                    <FormControl size="small" sx={{ minWidth: 130 }}>
                        <Select
                            value={selectedCurrency}
                            onChange={handleCurrencyChange}
                            displayEmpty
                            sx={{
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                color: '#f8fafc',
                                borderRadius: '10px',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                fontSize: '0.85rem',
                                '& .MuiOutlinedInput-notchedOutline': { border: 'none' }
                            }}
                        >
                            <MenuItem value="">All Currencies</MenuItem>
                            <MenuItem value="USD">USD ($)</MenuItem>
                            <MenuItem value="INR">INR (₹)</MenuItem>
                            <MenuItem value="EUR">EUR (€)</MenuItem>
                            <MenuItem value="GBP">GBP (£)</MenuItem>
                            <MenuItem value="CAD">CAD ($)</MenuItem>
                        </Select>
                    </FormControl>

                    <Button
                        variant="outlined"
                        onClick={() => loadEvolution(period, selectedCurrency)}
                        startIcon={<RefreshIcon />}
                        size="small"
                        sx={{
                            color: '#94a3b8',
                            borderColor: 'rgba(255, 255, 255, 0.1)',
                            borderRadius: '10px',
                            textTransform: 'none',
                            px: 1.5,
                            py: 0.8,
                            '&:hover': {
                                borderColor: 'rgba(255, 255, 255, 0.25)',
                                color: '#ffffff',
                                backgroundColor: 'rgba(255, 255, 255, 0.04)'
                            }
                        }}
                    >
                        Refresh
                    </Button>
                </Box>
            </Box>

            {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
                    <CircularProgress size={48} sx={{ color: '#3b82f6' }} />
                </Box>
            ) : error ? (
                <Alert severity="error" sx={{ mb: 4, borderRadius: 2 }}>{error}</Alert>
            ) : !evolution || evolution.totalTipCount === 0 ? (
                /* Empty State */
                <Card sx={{
                    borderRadius: 3,
                    p: 6,
                    textAlign: 'center',
                    backgroundColor: 'rgba(30, 41, 59, 0.5)',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    backdropFilter: 'blur(10px)'
                }}>
                    <Box sx={{
                        width: 64,
                        height: 64,
                        borderRadius: '50%',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        mx: 'auto',
                        mb: 2
                    }}>
                        <CalendarIcon sx={{ fontSize: 32, color: '#3b82f6' }} />
                    </Box>
                    <Typography variant="h5" sx={{ fontWeight: 600, color: '#f8fafc', mb: 1 }}>
                        No Tip Evolution Data Yet
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#94a3b8', maxWidth: 480, mx: 'auto', mb: 3 }}>
                        {evolution?.message || 'Add some tips across multiple months to unlock your behavioural evolution and timeline trends.'}
                    </Typography>
                    <Button
                        variant="contained"
                        onClick={() => navigate('/tips')}
                        sx={{
                            borderRadius: 2,
                            px: 3,
                            py: 1,
                            backgroundColor: '#3b82f6',
                            textTransform: 'none',
                            fontWeight: 600,
                            '&:hover': { backgroundColor: '#2563eb' }
                        }}
                    >
                        Log Your First Tip
                    </Button>
                </Card>
            ) : (
                <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
                    {/* Multi-Currency Tabs if > 1 currency */}
                    {evolution.currencyTimelines && evolution.currencyTimelines.length > 1 && (
                        <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" sx={{ color: '#94a3b8', mr: 1, fontWeight: 500 }}>
                                Active Currencies:
                            </Typography>
                            {evolution.currencyTimelines.map(ct => (
                                <Chip
                                    key={ct.currency}
                                    label={`${ct.currency} (${ct.totalTipCount} tips)`}
                                    clickable
                                    onClick={() => setActiveCurrencyTab(ct.currency)}
                                    sx={{
                                        fontWeight: 600,
                                        borderRadius: '8px',
                                        backgroundColor: activeCurrencyTab === ct.currency ? '#3b82f6' : 'rgba(255, 255, 255, 0.05)',
                                        color: activeCurrencyTab === ct.currency ? '#ffffff' : '#94a3b8',
                                        border: '1px solid',
                                        borderColor: activeCurrencyTab === ct.currency ? '#3b82f6' : 'rgba(255, 255, 255, 0.1)',
                                        '&:hover': {
                                            backgroundColor: activeCurrencyTab === ct.currency ? '#2563eb' : 'rgba(255, 255, 255, 0.1)'
                                        }
                                    }}
                                />
                            ))}
                        </Box>
                    )}

                    {/* Top Stat Cards */}
                    <Grid container spacing={2.5} sx={{ mb: 4 }}>
                        {/* Direction Card */}
                        <Grid size={{ xs: 12, md: 4 }}>
                            <Card sx={{
                                height: '100%',
                                borderRadius: 3,
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                border: `1px solid ${dirInfo.borderColor}`,
                                backdropFilter: 'blur(10px)',
                                p: 1
                            }}>
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                                        <Typography variant="body2" sx={{ color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                            Behavioural Direction
                                        </Typography>
                                        <Box sx={{ p: 0.75, borderRadius: '8px', backgroundColor: dirInfo.bgColor }}>
                                            {dirInfo.icon}
                                        </Box>
                                    </Box>
                                    <Typography variant="h5" sx={{ fontWeight: 700, color: dirInfo.color, mb: 1 }}>
                                        {dirInfo.label}
                                    </Typography>
                                    <Typography variant="body2" sx={{ color: '#94a3b8', lineHeight: 1.5 }}>
                                        {dirInfo.desc}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* Median Tip % Card */}
                        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
                            <Card sx={{
                                height: '100%',
                                borderRadius: 3,
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                backdropFilter: 'blur(10px)',
                                p: 1
                            }}>
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                                        <Typography variant="body2" sx={{ color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                            Median Tip Percentage
                                        </Typography>
                                        <StarsIcon sx={{ color: '#38bdf8', fontSize: 24 }} />
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1.5, mb: 1 }}>
                                        <Typography variant="h4" sx={{ fontWeight: 700, color: '#f8fafc' }}>
                                            {activeCurrencyTimeline?.currentMedianTipPercentage != null
                                                ? `${activeCurrencyTimeline.currentMedianTipPercentage}%`
                                                : evolution.currentMedianTipPercentage != null
                                                    ? `${evolution.currentMedianTipPercentage}%`
                                                    : '—'}
                                        </Typography>
                                        {activeCurrencyTimeline?.historicalMedianTipPercentage != null && (
                                            <Typography variant="body2" sx={{ color: '#64748b' }}>
                                                earlier: {activeCurrencyTimeline.historicalMedianTipPercentage}%
                                            </Typography>
                                        )}
                                    </Box>
                                    <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                        Average Tip %: {activeCurrencyTimeline?.currentAverageTipPercentage != null ? `${activeCurrencyTimeline.currentAverageTipPercentage}%` : '—'}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* Activity Volume Card */}
                        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
                            <Card sx={{
                                height: '100%',
                                borderRadius: 3,
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                backdropFilter: 'blur(10px)',
                                p: 1
                            }}>
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                                        <Typography variant="body2" sx={{ color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                            Active Timeline
                                        </Typography>
                                        <CalendarIcon sx={{ color: '#a855f7', fontSize: 24 }} />
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1.5, mb: 1 }}>
                                        <Typography variant="h4" sx={{ fontWeight: 700, color: '#f8fafc' }}>
                                            {activeCurrencyTimeline?.monthsWithActivity || evolution.activeMonths}
                                        </Typography>
                                        <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                            active months
                                        </Typography>
                                    </Box>
                                    <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                        {activeCurrencyTimeline?.totalTipCount || evolution.totalTipCount} total tips analysed in period
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>

                    {/* AI Behavioral Explanation Card (Gemini) */}
                    {evolution.aiExplanation && (
                        <Card sx={{
                            mb: 4,
                            borderRadius: 3,
                            background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(139, 92, 246, 0.08) 100%)',
                            border: '1px solid rgba(139, 92, 246, 0.25)',
                            backdropFilter: 'blur(12px)',
                            overflow: 'hidden'
                        }}>
                            <CardContent sx={{ p: 3 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                                    <Box sx={{
                                        width: 32,
                                        height: 32,
                                        borderRadius: '8px',
                                        background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center'
                                    }}>
                                        <AutoAwesomeIcon sx={{ color: '#ffffff', fontSize: 18 }} />
                                    </Box>
                                    <Typography variant="h6" sx={{ fontWeight: 700, color: '#f8fafc' }}>
                                        AI Behavioural Evolution Insights
                                    </Typography>
                                </Box>
                                <Typography variant="body1" sx={{ color: '#cbd5e1', lineHeight: 1.7, whiteSpace: 'pre-line' }}>
                                    {evolution.aiExplanation}
                                </Typography>
                            </CardContent>
                        </Card>
                    )}

                    {/* Visual Charts Section */}
                    <Grid container spacing={3} sx={{ mb: 4 }}>
                        {/* Monthly Percentage Trajectory */}
                        <Grid size={{ xs: 12, lg: 8 }}>
                            <Card sx={{
                                height: '100%',
                                borderRadius: 3,
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                backdropFilter: 'blur(10px)',
                                p: 2
                            }}>
                                <Box sx={{ mb: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <Box>
                                        <Typography variant="h6" sx={{ fontWeight: 600, color: '#f8fafc' }}>
                                            Monthly Tip Percentage Trends ({activeCurrencyTimeline?.currency || 'All'})
                                        </Typography>
                                        <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                            Comparison of monthly median vs. average tipping percentages
                                        </Typography>
                                    </Box>
                                </Box>
                                <Box sx={{ width: '100%', height: 320 }}>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={chartData} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                                            <defs>
                                                <linearGradient id="medianGradient" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4}/>
                                                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0}/>
                                                </linearGradient>
                                                <linearGradient id="avgGradient" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4}/>
                                                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.0}/>
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
                                            <XAxis dataKey="month" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                                            <YAxis stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} tickFormatter={(val) => `${val}%`} />
                                            <RechartsTooltip
                                                contentStyle={{ backgroundColor: '#1e293b', borderColor: 'rgba(255,255,255,0.1)', borderRadius: '8px', color: '#f8fafc' }}
                                                formatter={(value, name) => [`${value}%`, name === 'medianPct' ? 'Median Tip %' : 'Average Tip %']}
                                            />
                                            <Legend wrapperStyle={{ color: '#94a3b8', fontSize: '0.85rem' }} />
                                            <Area type="monotone" dataKey="medianPct" name="Median Tip %" stroke="#3b82f6" strokeWidth={2.5} fillOpacity={1} fill="url(#medianGradient)" />
                                            <Area type="monotone" dataKey="averagePct" name="Average Tip %" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#avgGradient)" />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </Box>
                            </Card>
                        </Grid>

                        {/* Tips Count & Unique Restaurants */}
                        <Grid size={{ xs: 12, lg: 4 }}>
                            <Card sx={{
                                height: '100%',
                                borderRadius: 3,
                                backgroundColor: 'rgba(30, 41, 59, 0.7)',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                backdropFilter: 'blur(10px)',
                                p: 2
                            }}>
                                <Box sx={{ mb: 2 }}>
                                    <Typography variant="h6" sx={{ fontWeight: 600, color: '#f8fafc' }}>
                                        Activity & Restaurant Diversity
                                    </Typography>
                                    <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                        Monthly tips logged vs. unique restaurants visited
                                    </Typography>
                                </Box>
                                <Box sx={{ width: '100%', height: 320 }}>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
                                            <XAxis dataKey="month" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                                            <YAxis stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                                            <RechartsTooltip
                                                contentStyle={{ backgroundColor: '#1e293b', borderColor: 'rgba(255,255,255,0.1)', borderRadius: '8px', color: '#f8fafc' }}
                                            />
                                            <Legend wrapperStyle={{ color: '#94a3b8', fontSize: '0.85rem' }} />
                                            <Bar dataKey="tipCount" name="Tips Logged" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="uniqueRestaurants" name="Unique Restaurants" fill="#38bdf8" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Box>
                            </Card>
                        </Grid>
                    </Grid>

                    {/* Service Quality Evolution (if available) */}
                    {sqChartData.length > 0 && (
                        <Card sx={{
                            mb: 4,
                            borderRadius: 3,
                            backgroundColor: 'rgba(30, 41, 59, 0.7)',
                            border: '1px solid rgba(255, 255, 255, 0.08)',
                            backdropFilter: 'blur(10px)',
                            p: 2
                        }}>
                            <Box sx={{ mb: 2 }}>
                                <Typography variant="h6" sx={{ fontWeight: 600, color: '#f8fafc' }}>
                                    Service Quality Tipping Patterns
                                </Typography>
                                <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                    How your tipping adapts across different service ratings (Poor, Average, Good, Excellent)
                                </Typography>
                            </Box>
                            <Box sx={{ width: '100%', height: 260 }}>
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={sqChartData} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
                                        <XAxis dataKey="month" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                                        <YAxis stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} tickFormatter={(val) => `${val}%`} />
                                        <RechartsTooltip
                                            contentStyle={{ backgroundColor: '#1e293b', borderColor: 'rgba(255,255,255,0.1)', borderRadius: '8px', color: '#f8fafc' }}
                                            formatter={(value) => [`${value}%`]}
                                        />
                                        <Legend wrapperStyle={{ color: '#94a3b8', fontSize: '0.85rem' }} />
                                        <Line type="monotone" dataKey="EXCELLENT" name="Excellent" stroke="#10b981" strokeWidth={2} dot={{ r: 3 }} />
                                        <Line type="monotone" dataKey="GOOD" name="Good" stroke="#3b82f6" strokeWidth={2} dot={{ r: 3 }} />
                                        <Line type="monotone" dataKey="AVERAGE" name="Average" stroke="#f59e0b" strokeWidth={2} dot={{ r: 3 }} />
                                        <Line type="monotone" dataKey="POOR" name="Poor" stroke="#ef4444" strokeWidth={2} dot={{ r: 3 }} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Box>
                        </Card>
                    )}

                    {/* Detailed Monthly Timeline Table */}
                    <Card sx={{
                        borderRadius: 3,
                        backgroundColor: 'rgba(30, 41, 59, 0.7)',
                        border: '1px solid rgba(255, 255, 255, 0.08)',
                        backdropFilter: 'blur(10px)',
                        overflow: 'hidden'
                    }}>
                        <Box sx={{ p: 2.5, borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
                            <Typography variant="h6" sx={{ fontWeight: 600, color: '#f8fafc' }}>
                                Monthly Breakdown & Consistency ({activeCurrencyTimeline?.currency || 'All'})
                            </Typography>
                            <Typography variant="body2" sx={{ color: '#94a3b8' }}>
                                Complete month-by-month record of your tipping statistics, styles, and consistency metrics
                            </Typography>
                        </Box>
                        <TableContainer>
                            <Table sx={{ minWidth: 650 }}>
                                <TableHead sx={{ backgroundColor: 'rgba(15, 23, 42, 0.6)' }}>
                                    <TableRow>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>MONTH</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>TIPS</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>MEDIAN %</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>AVERAGE %</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>TIP STYLE</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>CONSISTENCY</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>MEDIAN AMOUNT</TableCell>
                                        <TableCell sx={{ color: '#94a3b8', fontWeight: 600, fontSize: '0.8rem' }}>RESTAURANTS</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {(activeCurrencyTimeline?.monthlyTimeline || []).map((row) => {
                                        const styleDetails = getStyleColor(row.tipStyle);
                                        const behaviorDetails = getBehaviorColor(row.behaviorType);
                                        return (
                                            <TableRow key={row.month} sx={{ '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.02)' } }}>
                                                <TableCell sx={{ color: '#f8fafc', fontWeight: 600 }}>{row.month}</TableCell>
                                                <TableCell sx={{ color: '#cbd5e1' }}>{row.tipCount}</TableCell>
                                                <TableCell sx={{ color: '#38bdf8', fontWeight: 600 }}>
                                                    {row.medianTipPercentage != null ? `${row.medianTipPercentage}%` : '—'}
                                                </TableCell>
                                                <TableCell sx={{ color: '#cbd5e1' }}>
                                                    {row.averageTipPercentage != null ? `${row.averageTipPercentage}%` : '—'}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        label={styleDetails.label}
                                                        size="small"
                                                        sx={{
                                                            fontSize: '0.75rem',
                                                            fontWeight: 600,
                                                            color: styleDetails.color,
                                                            backgroundColor: `${styleDetails.color}20`,
                                                            borderRadius: '6px'
                                                        }}
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {row.consistencyScore != null ? (
                                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                            <Typography variant="body2" sx={{ color: '#f8fafc', fontWeight: 600 }}>
                                                                {row.consistencyScore}
                                                            </Typography>
                                                            <Chip
                                                                label={behaviorDetails.label}
                                                                size="small"
                                                                sx={{
                                                                    fontSize: '0.7rem',
                                                                    color: behaviorDetails.color,
                                                                    backgroundColor: `${behaviorDetails.color}15`,
                                                                    borderRadius: '6px'
                                                                }}
                                                            />
                                                        </Box>
                                                    ) : (
                                                        <Typography variant="caption" sx={{ color: '#64748b' }}>
                                                            &lt;5 tips
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell sx={{ color: '#cbd5e1' }}>
                                                    {row.medianTipAmount != null ? `${activeCurrencyTimeline?.currency || ''} ${row.medianTipAmount}` : '—'}
                                                </TableCell>
                                                <TableCell sx={{ color: '#cbd5e1' }}>{row.uniqueRestaurantCount}</TableCell>
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Card>
                </motion.div>
            )}
        </Container>
    );
};

export default TipEvolutionPage;
