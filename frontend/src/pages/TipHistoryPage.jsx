import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  Box, Typography, Grid, TextField, MenuItem, Button, 
  Card, CardContent, CircularProgress, IconButton, Stack,
  TablePagination, Chip, Table, TableBody, TableCell, 
  TableContainer, TableHead, TableRow, Paper, Tooltip, 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Divider, Alert, Snackbar, Collapse
} from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

// Project-Specific Tailored Icons
import SearchIcon from '@mui/icons-material/SearchRounded';
import RestartAltIcon from '@mui/icons-material/RestartAltRounded';
import FileDownloadIcon from '@mui/icons-material/FileDownloadRounded';
import AddIcon from '@mui/icons-material/AddRounded';
import ViewListIcon from '@mui/icons-material/ViewListRounded';
import ViewModuleIcon from '@mui/icons-material/ViewModuleRounded';
import TuneIcon from '@mui/icons-material/TuneRounded';
import VerifiedIcon from '@mui/icons-material/VerifiedRounded';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongRounded';
import StarIcon from '@mui/icons-material/StarRounded';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineRounded';
import CloseIcon from '@mui/icons-material/CloseRounded';
import TrendingUpIcon from '@mui/icons-material/TrendingUpRounded';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWalletRounded';
import RestaurantIcon from '@mui/icons-material/RestaurantRounded';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonthRounded';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpwardRounded';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownwardRounded';
import ShieldIcon from '@mui/icons-material/ShieldRounded';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesomeRounded';
import PrintIcon from '@mui/icons-material/PrintRounded';
import NorthEastIcon from '@mui/icons-material/NorthEastRounded';

// Components & APIs
import PtLogo from '../components/PtLogo';
import tipHistoryApi from '../api/tipHistoryApi';
import tipsApi from '../api/tipsApi';
import exportApi from '../api/exportApi';
import SkeletonLoader from '../components/SkeletonLoader';

const CURRENCIES = ['ALL', 'USD', 'EUR', 'GBP', 'INR', 'JPY', 'CAD', 'AUD'];
const SERVICE_QUALITIES = [
  { value: 'ALL', label: 'All Tiers', stars: 0 },
  { value: 'EXCELLENT', label: 'Excellent', stars: 4 },
  { value: 'GOOD', label: 'Good', stars: 3 },
  { value: 'AVERAGE', label: 'Average', stars: 2 },
  { value: 'POOR', label: 'Poor', stars: 1 }
];

const SORT_OPTIONS = [
  { value: 'createdAt', label: 'Date Settled' },
  { value: 'tipAmount', label: 'Gratuity Amount' },
  { value: 'tipPercentage', label: 'Gratuity Rate %' },
  { value: 'billAmount', label: 'Bill Subtotal' },
  { value: 'restaurantName', label: 'Venue Name' }
];

const INITIAL_FILTERS = {
  restaurantName: '',
  currency: 'ALL',
  serviceQuality: 'ALL',
  startDate: '',
  endDate: '',
  minTipPercentage: '',
  maxTipPercentage: '',
  minBillAmount: '',
  maxBillAmount: '',
  sortBy: 'createdAt',
  sortDirection: 'DESC'
};

const TipHistoryPage = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [seeding, setSeeding] = useState(false);
  const [error, setError] = useState('');
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'info' });
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isFirstLoad, setIsFirstLoad] = useState(true);
  const [viewMode, setViewMode] = useState('table'); // 'table' | 'cards'
  const [advancedOpen, setAdvancedOpen] = useState(false);

  // Modal inspection & deletion
  const [inspectingTip, setInspectingTip] = useState(null);
  const [deleteTargetTip, setDeleteTargetTip] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchHistory = useCallback(async (currentPage, currentSize, currentFilters) => {
    setLoading(true);
    setError('');
    
    const payload = {
      page: currentPage,
      size: currentSize,
      sortBy: currentFilters.sortBy,
      sortDirection: currentFilters.sortDirection
    };
    
    if (currentFilters.restaurantName?.trim()) payload.restaurantName = currentFilters.restaurantName.trim();
    if (currentFilters.currency && currentFilters.currency !== 'ALL') payload.currency = currentFilters.currency;
    if (currentFilters.serviceQuality && currentFilters.serviceQuality !== 'ALL') payload.serviceQuality = currentFilters.serviceQuality;
    if (currentFilters.startDate) payload.startDate = currentFilters.startDate;
    if (currentFilters.endDate) payload.endDate = currentFilters.endDate;
    if (currentFilters.minTipPercentage !== '') payload.minTipPercentage = parseInt(currentFilters.minTipPercentage, 10);
    if (currentFilters.maxTipPercentage !== '') payload.maxTipPercentage = parseInt(currentFilters.maxTipPercentage, 10);
    if (currentFilters.minBillAmount !== '') payload.minBillAmount = parseFloat(currentFilters.minBillAmount);
    if (currentFilters.maxBillAmount !== '') payload.maxBillAmount = parseFloat(currentFilters.maxBillAmount);

    try {
      const data = await tipHistoryApi.searchTipHistory(payload);
      setResults(data);
      if (isFirstLoad) setIsFirstLoad(false);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to query gratuity ledger');
      if (isFirstLoad) setIsFirstLoad(false);
    } finally {
      setLoading(false);
    }
  }, [isFirstLoad]);

  useEffect(() => {
    fetchHistory(page, rowsPerPage, filters);
  }, [page, rowsPerPage, fetchHistory]);

  const handleSearchTrigger = () => {
    setPage(0);
    fetchHistory(0, rowsPerPage, filters);
  };

  const handleReset = () => {
    setFilters(INITIAL_FILTERS);
    setPage(0);
    fetchHistory(0, rowsPerPage, INITIAL_FILTERS);
  };

  const handleFilterChange = (field) => (event) => {
    setFilters(prev => ({ ...prev, [field]: event.target.value }));
  };

  // Quick Date Horizon Presets
  const handleDatePreset = (preset) => {
    const today = new Date();
    let start = '';
    let end = today.toISOString().slice(0, 10);

    if (preset === 'ALL') {
      start = '';
      end = '';
    } else if (preset === 'TODAY') {
      start = end;
    } else if (preset === '7DAYS') {
      const d = new Date();
      d.setDate(today.getDate() - 7);
      start = d.toISOString().slice(0, 10);
    } else if (preset === '30DAYS') {
      const d = new Date();
      d.setDate(today.getDate() - 30);
      start = d.toISOString().slice(0, 10);
    } else if (preset === 'THIS_MONTH') {
      const d = new Date(today.getFullYear(), today.getMonth(), 1);
      start = d.toISOString().slice(0, 10);
    }

    const updated = { ...filters, startDate: start, endDate: end };
    setFilters(updated);
    setPage(0);
    fetchHistory(0, rowsPerPage, updated);
  };

  const hasActiveFilters = useMemo(() => {
    return (
      (filters.restaurantName?.trim() || '') !== '' ||
      filters.currency !== 'ALL' ||
      filters.serviceQuality !== 'ALL' ||
      filters.startDate !== '' ||
      filters.endDate !== '' ||
      filters.minTipPercentage !== '' ||
      filters.maxTipPercentage !== '' ||
      filters.minBillAmount !== '' ||
      filters.maxBillAmount !== '' ||
      filters.sortBy !== 'createdAt' ||
      filters.sortDirection !== 'DESC'
    );
  }, [filters]);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.restaurantName?.trim()) count++;
    if (filters.currency !== 'ALL') count++;
    if (filters.serviceQuality !== 'ALL') count++;
    if (filters.startDate) count++;
    if (filters.endDate) count++;
    if (filters.minTipPercentage !== '') count++;
    if (filters.maxTipPercentage !== '') count++;
    if (filters.minBillAmount !== '') count++;
    if (filters.maxBillAmount !== '') count++;
    return count;
  }, [filters]);

  // Compute live ledger summary metrics
  const ledgerMetrics = useMemo(() => {
    if (!results || !results.content || results.content.length === 0) {
      return {
        totalDispatches: results?.totalElements || 0,
        volumeTotal: 0,
        gratuitiesTotal: 0,
        meanRate: 0,
        topVenue: '—'
      };
    }

    const items = results.content;
    const vol = items.reduce((acc, curr) => acc + (Number(curr.billAmount) || 0), 0);
    const tips = items.reduce((acc, curr) => acc + (Number(curr.tipAmount) || 0), 0);
    const avgPct = items.reduce((acc, curr) => acc + (Number(curr.tipPercentage) || 0), 0) / items.length;

    const venues = {};
    items.forEach(i => {
      const name = i.restaurantName || 'Unknown';
      venues[name] = (venues[name] || 0) + 1;
    });
    const sortedVenues = Object.entries(venues).sort((a, b) => b[1] - a[1]);
    const top = sortedVenues[0] ? sortedVenues[0][0] : '—';

    return {
      totalDispatches: results.totalElements || items.length,
      volumeTotal: vol,
      gratuitiesTotal: tips,
      meanRate: avgPct.toFixed(1),
      topVenue: top
    };
  }, [results]);

  // Handle Export CSV
  const handleExportCSV = async () => {
    setExporting(true);
    try {
      const exportPayload = {
        restaurantName: filters.restaurantName?.trim() || undefined,
        currency: filters.currency !== 'ALL' ? filters.currency : undefined,
        serviceQuality: filters.serviceQuality !== 'ALL' ? filters.serviceQuality : undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined
      };
      const blob = await exportApi.exportTips(exportPayload, 'csv');
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `PT_Gratuity_Ledger_${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      setNotification({ open: true, message: 'Gratuity Ledger exported successfully.', severity: 'success' });
    } catch (err) {
      setNotification({ open: true, message: 'Failed to export ledger CSV.', severity: 'error' });
    } finally {
      setExporting(false);
    }
  };

  // Quick Seed Demo Data
  const handleSeedDemoData = async () => {
    setSeeding(true);
    try {
      const sampleTips = [
        { restaurantName: 'Le Bernardin', billAmount: 285.00, tipPercentage: 20.00, currency: 'USD', serviceQuality: 'EXCELLENT' },
        { restaurantName: 'Nobu Downtown', billAmount: 195.50, tipPercentage: 18.00, currency: 'USD', serviceQuality: 'GOOD' },
        { restaurantName: 'Eleven Madison Park', billAmount: 420.00, tipPercentage: 22.00, currency: 'USD', serviceQuality: 'EXCELLENT' },
        { restaurantName: 'Cote Korean Steakhouse', billAmount: 160.00, tipPercentage: 20.00, currency: 'USD', serviceQuality: 'GOOD' },
        { restaurantName: 'Gramercy Tavern', billAmount: 110.00, tipPercentage: 15.00, currency: 'USD', serviceQuality: 'AVERAGE' }
      ];

      for (const t of sampleTips) {
        await tipsApi.createTip(t);
      }

      setNotification({ open: true, message: 'Seeded 5 prestigious gratuity dispatches into ledger.', severity: 'success' });
      await fetchHistory(0, rowsPerPage, filters);
    } catch (err) {
      setNotification({ open: true, message: 'Failed to seed sample records.', severity: 'error' });
    } finally {
      setSeeding(false);
    }
  };

  // Handle Delete Tip
  const confirmDeleteTip = async () => {
    if (!deleteTargetTip) return;
    setDeleting(true);
    try {
      await tipsApi.deleteTip(deleteTargetTip.id);
      setNotification({ open: true, message: 'Ledger record successfully purged.', severity: 'info' });
      setDeleteTargetTip(null);
      await fetchHistory(page, rowsPerPage, filters);
    } catch (err) {
      setNotification({ open: true, message: 'Failed to delete record.', severity: 'error' });
    } finally {
      setDeleting(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return { date: '—', time: '—' };
    const d = new Date(dateString);
    return {
      date: d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }),
      time: d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
    };
  };

  const renderQualityStars = (quality) => {
    const q = (quality || '').toUpperCase();
    let stars = 0;
    let label = 'Unrated';
    let color = '#858b96';

    if (q === 'EXCELLENT') { stars = 4; label = 'Excellent'; color = '#fd5b38'; }
    else if (q === 'GOOD') { stars = 3; label = 'Good'; color = '#ff8566'; }
    else if (q === 'AVERAGE') { stars = 2; label = 'Average'; color = '#cdd2d8'; }
    else if (q === 'POOR') { stars = 1; label = 'Poor'; color = '#fb7185'; }

    return (
      <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.6, px: 1.25, py: 0.35, borderRadius: '999px', background: `${color}14`, border: `1px solid ${color}33` }}>
        <Box sx={{ display: 'flex' }}>
          {[...Array(stars || 1)].map((_, idx) => (
            <StarIcon key={idx} sx={{ fontSize: 13, color }} />
          ))}
        </Box>
        <Typography variant="caption" sx={{ color, fontWeight: 800, fontSize: '0.68rem', letterSpacing: '0.04em', textTransform: 'uppercase' }}>
          {label}
        </Typography>
      </Box>
    );
  };

  return (
    <Box sx={{ px: { xs: 2.5, sm: 3.5, md: 5 }, pt: { xs: 3, md: 4 }, pb: { xs: 4, md: 6 }, width: '100%', maxWidth: 1300, mx: 'auto' }}>
      
      {/* ═══════════════════════════════════════════════════
          PT LEDGER HEADER & ACTIONS
          ═══════════════════════════════════════════════════ */}
      <Box 
        className="webfolio-card"
        sx={{ 
          p: { xs: 3, md: 4.5 }, 
          mb: 4, 
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' }, gap: 3, mb: 3.5 }}>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.25 }}>
              <span className="title-bord">
                ✦ PT DISPATCH LEDGER
              </span>
              <Chip 
                label="CRYPTOGRAPHIC AUDIT TRAIL" 
                size="small"
                sx={{ 
                  background: 'rgba(253, 91, 56, 0.1)', 
                  border: '1px solid rgba(253, 91, 56, 0.3)', 
                  color: '#fd5b38',
                  fontWeight: 800,
                  fontSize: '0.65rem',
                  letterSpacing: '0.08em'
                }} 
              />
            </Box>
            <Typography 
              variant="h3" 
              sx={{ 
                fontWeight: 800, 
                letterSpacing: '-0.035em', 
                color: '#fff',
                fontFamily: '"Space Grotesk", sans-serif',
                lineHeight: 1.15,
                mb: 1
              }}
            >
              Historical Gratuity Ledger
            </Typography>
            <Typography variant="body1" sx={{ color: '#cdd2d8', maxWidth: 720, fontSize: '1.05rem', lineHeight: 1.6 }}>
              Permanent record of dining dispatches, hospitality service indexes, and IRS § 274 tax-deductible distributions across fine dining establishments.
            </Typography>
          </Box>

          <Stack direction="row" spacing={1.5} sx={{ width: { xs: '100%', sm: 'auto' }, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              size="small"
              startIcon={exporting ? <CircularProgress size={16} color="inherit" /> : <FileDownloadIcon />}
              onClick={handleExportCSV}
              disabled={exporting || (results?.totalElements || 0) === 0}
              className="butn butn-sm butn-bord"
            >
              Export CSV
            </Button>

            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={() => navigate('/tips')}
              className="butn butn-sm butn-bg"
            >
              Dispatch Tip
            </Button>

            {/* View Mode Switcher */}
            <Box sx={{ display: 'inline-flex', p: 0.5, borderRadius: '999px', background: 'rgba(14, 18, 32, 0.7)', border: '1px solid rgba(255,255,255,0.08)' }}>
              <Tooltip title="Ledger Table View">
                <IconButton 
                  size="small" 
                  onClick={() => setViewMode('table')}
                  sx={{ 
                    color: viewMode === 'table' ? '#fd5b38' : '#858b96', 
                    background: viewMode === 'table' ? 'rgba(253, 91, 56, 0.16)' : 'transparent',
                    borderRadius: '999px',
                    p: 0.75
                  }}
                >
                  <ViewListIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Card Dossier View">
                <IconButton 
                  size="small" 
                  onClick={() => setViewMode('cards')}
                  sx={{ 
                    color: viewMode === 'cards' ? '#fd5b38' : '#858b96', 
                    background: viewMode === 'cards' ? 'rgba(253, 91, 56, 0.16)' : 'transparent',
                    borderRadius: '999px',
                    p: 0.75
                  }}
                >
                  <ViewModuleIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          </Stack>
        </Box>

        {/* 5-METRIC PT TELEMETRY RIBBON */}
        <Box 
          sx={{ 
            display: 'grid', 
            gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(3, 1fr)', md: 'repeat(5, 1fr)' }, 
            gap: 2,
            pt: 3,
            borderTop: '1px solid rgba(255, 255, 255, 0.08)'
          }}
        >
          <Box sx={{ p: 2, borderRadius: '16px', background: 'rgba(14, 18, 32, 0.55)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="caption" sx={{ color: '#858b96', display: 'block', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700 }}>
              01 / Recorded Dispatches
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#fff', fontFamily: '"JetBrains Mono", monospace', mt: 1 }}>
              {ledgerMetrics.totalDispatches}
            </Typography>
          </Box>

          <Box sx={{ p: 2, borderRadius: '16px', background: 'rgba(14, 18, 32, 0.55)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="caption" sx={{ color: '#858b96', display: 'block', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700 }}>
              02 / Settled Volume
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#cdd2d8', fontFamily: '"JetBrains Mono", monospace', mt: 1 }}>
              ${ledgerMetrics.volumeTotal.toFixed(2)}
            </Typography>
          </Box>

          <Box sx={{ p: 2, borderRadius: '16px', background: 'rgba(253, 91, 56, 0.08)', border: '1px solid rgba(253, 91, 56, 0.28)' }}>
            <Typography variant="caption" sx={{ color: '#fd5b38', display: 'block', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 800 }}>
              03 / Cumulative Tips
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', mt: 1 }}>
              ${ledgerMetrics.gratuitiesTotal.toFixed(2)}
            </Typography>
          </Box>

          <Box sx={{ p: 2, borderRadius: '16px', background: 'rgba(253, 91, 56, 0.08)', border: '1px solid rgba(253, 91, 56, 0.28)' }}>
            <Typography variant="caption" sx={{ color: '#fd5b38', display: 'block', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 800 }}>
              04 / Effective Mean Rate
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', mt: 1 }}>
              {ledgerMetrics.meanRate}%
            </Typography>
          </Box>

          <Box sx={{ p: 2, borderRadius: '16px', background: 'rgba(14, 18, 32, 0.55)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="caption" sx={{ color: '#858b96', display: 'block', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700 }}>
              05 / Prime Venue
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 800, color: '#fff', fontFamily: '"JetBrains Mono", monospace', mt: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {ledgerMetrics.topVenue}
            </Typography>
          </Box>
        </Box>
      </Box>

      {/* ═══════════════════════════════════════════════════
          INTELLIGENT SEARCH & CHRONOLOGICAL HORIZON CONSOLE
          ═══════════════════════════════════════════════════ */}
      <Box 
        className="webfolio-card"
        sx={{ 
          p: 3, 
          mb: 4, 
          background: 'rgba(20, 24, 41, 0.85)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.1)'
        }}
      >
        {/* ROW 1: OMNI SEARCH & CURRENCY CAPSULES */}
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', lg: 'row' }, gap: 2, alignItems: 'center' }}>
          {/* Omni Search Field */}
          <Box sx={{ position: 'relative', flex: 1, width: '100%' }}>
            <TextField
              fullWidth
              size="small"
              placeholder="Search by venue name, dining notes, or tags..."
              value={filters.restaurantName}
              onChange={handleFilterChange('restaurantName')}
              onKeyDown={(e) => { if (e.key === 'Enter') handleSearchTrigger(); }}
              slotProps={{
                input: {
                  startAdornment: <SearchIcon sx={{ color: '#fd5b38', mr: 1.5, fontSize: 20 }} />
                }
              }}
              sx={{
                '& .MuiInputBase-root': {
                  background: 'rgba(10, 13, 23, 0.75)',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  borderRadius: '14px',
                  color: '#fff',
                  px: 2,
                  py: 0.8,
                  fontSize: '1.15rem',
                  '&:hover': { borderColor: 'rgba(253, 91, 56, 0.4)' },
                  '&.Mui-focused': { borderColor: '#fd5b38', boxShadow: '0 0 16px rgba(253, 91, 56, 0.25)' },
                  '& input::placeholder': {
                    fontSize: '1.05rem'
                  }
                }
              }}
            />
          </Box>

          {/* Quick Currency Selector */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, overflowX: 'auto', maxWidth: '100%', py: 0.5 }}>
            {['ALL', 'USD', 'EUR', 'GBP', 'INR'].map((curr) => (
              <Button
                key={curr}
                size="small"
                variant={filters.currency === curr ? 'contained' : 'outlined'}
                onClick={() => {
                  setFilters(prev => ({ ...prev, currency: curr }));
                  setPage(0);
                  fetchHistory(0, rowsPerPage, { ...filters, currency: curr });
                }}
                sx={{
                  minWidth: 'auto',
                  px: 1.75,
                  py: 0.6,
                  fontSize: '0.78rem',
                  borderRadius: '999px',
                  fontWeight: 700,
                  fontFamily: '"JetBrains Mono", monospace',
                  ...(filters.currency === curr ? {
                    background: '#fd5b38',
                    color: '#ffffff',
                    boxShadow: '0 2px 12px rgba(253, 91, 56, 0.4)'
                  } : {
                    color: '#cdd2d8',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    background: 'rgba(10, 13, 23, 0.4)',
                    '&:hover': { borderColor: '#fd5b38', color: '#ffffff' }
                  })
                }}
              >
                {curr}
              </Button>
            ))}
          </Box>

          {/* Filter Trigger & Search Action */}
          <Stack direction="row" spacing={1} sx={{ width: { xs: '100%', lg: 'auto' }, justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              size="small"
              startIcon={<TuneIcon sx={{ fontSize: 16 }} />}
              onClick={() => setAdvancedOpen(!advancedOpen)}
              sx={{
                px: 2,
                py: 0.8,
                borderRadius: '999px',
                fontSize: '0.8rem',
                fontWeight: 700,
                borderColor: advancedOpen || activeFilterCount > 0 ? '#fd5b38' : 'rgba(255,255,255,0.12)',
                color: advancedOpen || activeFilterCount > 0 ? '#fd5b38' : '#cdd2d8',
                background: advancedOpen ? 'rgba(253, 91, 56, 0.1)' : 'rgba(10, 13, 23, 0.4)'
              }}
            >
              Parameters {activeFilterCount > 0 && `(${activeFilterCount})`}
            </Button>

            {hasActiveFilters && (
              <Tooltip title="Reset all search parameters">
                <IconButton 
                  size="small" 
                  onClick={handleReset}
                  sx={{ color: '#cdd2d8', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '999px', p: 0.8 }}
                >
                  <RestartAltIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}

            <Button
              variant="contained"
              size="small"
              onClick={handleSearchTrigger}
              disabled={loading}
              className="butn butn-sm butn-bg"
              sx={{ minWidth: 100 }}
            >
              {loading ? <CircularProgress size={16} color="inherit" /> : 'Search'}
            </Button>
          </Stack>
        </Box>

        {/* ROW 2: CHRONOLOGICAL HORIZON (ZERO OVERLAP GUARANTEE) */}
        <Box sx={{ mt: 2.5, pt: 2.5, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: { xs: 'flex-start', md: 'center' }, justifyContent: 'space-between', gap: 2, mb: 1.5 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CalendarMonthIcon sx={{ color: '#fd5b38', fontSize: 18 }} />
              <Typography variant="caption" sx={{ color: '#fff', fontWeight: 800, letterSpacing: '0.08em', textTransform: 'uppercase', fontSize: '0.72rem' }}>
                Chronological Horizon
              </Typography>
            </Box>

            {/* Quick Horizon Pills */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8, flexWrap: 'wrap' }}>
              {[
                { label: 'All Time', key: 'ALL' },
                { label: 'Today', key: 'TODAY' },
                { label: 'Last 7 Days', key: '7DAYS' },
                { label: 'Last 30 Days', key: '30DAYS' },
                { label: 'This Month', key: 'THIS_MONTH' }
              ].map(p => (
                <Chip
                  key={p.key}
                  label={p.label}
                  size="small"
                  clickable
                  onClick={() => handleDatePreset(p.key)}
                  sx={{
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    borderRadius: '999px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    color: '#cdd2d8',
                    '&:hover': { background: 'rgba(253, 91, 56, 0.15)', borderColor: '#fd5b38', color: '#fff' }
                  }}
                />
              ))}
            </Box>
          </Box>

          {/* Dedicated From Date & To Date Inputs (Clean, Spacious, No Overlapping) */}
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Box sx={{ p: 1.5, borderRadius: '14px', background: 'rgba(10, 13, 23, 0.65)', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <Typography variant="caption" sx={{ color: '#858b96', fontSize: '0.68rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', display: 'block', mb: 0.5 }}>
                  FROM DATE (START)
                </Typography>
                <TextField
                  fullWidth
                  type="date"
                  size="small"
                  value={filters.startDate}
                  onChange={handleFilterChange('startDate')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'transparent',
                      color: '#ffffff',
                      fontFamily: '"JetBrains Mono", monospace',
                      fontSize: '0.85rem'
                    },
                    '& input::-webkit-calendar-picker-indicator': {
                      filter: 'invert(1)',
                      cursor: 'pointer'
                    }
                  }}
                />
              </Box>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Box sx={{ p: 1.5, borderRadius: '14px', background: 'rgba(10, 13, 23, 0.65)', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <Typography variant="caption" sx={{ color: '#858b96', fontSize: '0.68rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', display: 'block', mb: 0.5 }}>
                  TO DATE (END)
                </Typography>
                <TextField
                  fullWidth
                  type="date"
                  size="small"
                  value={filters.endDate}
                  onChange={handleFilterChange('endDate')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'transparent',
                      color: '#ffffff',
                      fontFamily: '"JetBrains Mono", monospace',
                      fontSize: '0.85rem'
                    },
                    '& input::-webkit-calendar-picker-indicator': {
                      filter: 'invert(1)',
                      cursor: 'pointer'
                    }
                  }}
                />
              </Box>
            </Grid>
          </Grid>
        </Box>

        {/* ROW 3: ADVANCED QUANT PARAMETERS DRAWER (COLLAPSIBLE) */}
        <Collapse in={advancedOpen} timeout={250}>
          <Box 
            sx={{ 
              mt: 2.5, 
              pt: 2.5, 
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' },
              gap: 2.5
            }}
          >
            {/* Field 1: Service Quality Rating */}
            <Box>
              <Typography variant="caption" sx={{ color: '#fd5b38', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.75, display: 'block' }}>
                Service Tier Rating
              </Typography>
              <TextField
                select
                fullWidth
                size="small"
                value={filters.serviceQuality}
                onChange={handleFilterChange('serviceQuality')}
                sx={{
                  '& .MuiInputBase-root': {
                    background: 'rgba(10, 13, 23, 0.75)',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    borderRadius: '12px',
                    color: '#fff',
                    fontSize: '0.85rem'
                  }
                }}
              >
                {SERVICE_QUALITIES.map(q => (
                  <MenuItem key={q.value} value={q.value} sx={{ fontSize: '0.85rem' }}>
                    {q.label}
                  </MenuItem>
                ))}
              </TextField>
            </Box>

            {/* Field 2: Bill Amount Range */}
            <Box>
              <Typography variant="caption" sx={{ color: '#fd5b38', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.75, display: 'block' }}>
                Bill Threshold ($)
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  placeholder="Min $"
                  value={filters.minBillAmount}
                  onChange={handleFilterChange('minBillAmount')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'rgba(10, 13, 23, 0.75)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontSize: '0.85rem',
                      fontFamily: '"JetBrains Mono", monospace'
                    }
                  }}
                />
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  placeholder="Max $"
                  value={filters.maxBillAmount}
                  onChange={handleFilterChange('maxBillAmount')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'rgba(10, 13, 23, 0.75)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontSize: '0.85rem',
                      fontFamily: '"JetBrains Mono", monospace'
                    }
                  }}
                />
              </Box>
            </Box>

            {/* Field 3: Tip Percentage Range */}
            <Box>
              <Typography variant="caption" sx={{ color: '#fd5b38', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.75, display: 'block' }}>
                Tip Rate % Range
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  placeholder="Min %"
                  value={filters.minTipPercentage}
                  onChange={handleFilterChange('minTipPercentage')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'rgba(10, 13, 23, 0.75)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontSize: '0.85rem',
                      fontFamily: '"JetBrains Mono", monospace'
                    }
                  }}
                />
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  placeholder="Max %"
                  value={filters.maxTipPercentage}
                  onChange={handleFilterChange('maxTipPercentage')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'rgba(10, 13, 23, 0.75)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontSize: '0.85rem',
                      fontFamily: '"JetBrains Mono", monospace'
                    }
                  }}
                />
              </Box>
            </Box>

            {/* Field 4: Sorting & Sequence */}
            <Box>
              <Typography variant="caption" sx={{ color: '#fd5b38', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.75, display: 'block' }}>
                Sequence Order
              </Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  value={filters.sortBy}
                  onChange={handleFilterChange('sortBy')}
                  sx={{
                    '& .MuiInputBase-root': {
                      background: 'rgba(10, 13, 23, 0.75)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontSize: '0.85rem'
                    }
                  }}
                >
                  {SORT_OPTIONS.map(opt => (
                    <MenuItem key={opt.value} value={opt.value} sx={{ fontSize: '0.85rem' }}>
                      {opt.label}
                    </MenuItem>
                  ))}
                </TextField>

                <Tooltip title={filters.sortDirection === 'DESC' ? 'Descending Order' : 'Ascending Order'}>
                  <IconButton
                    size="small"
                    onClick={() => setFilters(prev => ({ ...prev, sortDirection: prev.sortDirection === 'DESC' ? 'ASC' : 'DESC' }))}
                    sx={{
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                      borderRadius: '12px',
                      color: '#fd5b38',
                      px: 1.5,
                      background: 'rgba(253, 91, 56, 0.1)'
                    }}
                  >
                    {filters.sortDirection === 'DESC' ? <ArrowDownwardIcon fontSize="small" /> : <ArrowUpwardIcon fontSize="small" />}
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>
          </Box>
        </Collapse>
      </Box>

      {/* ERROR FEEDBACK */}
      {error && (
        <Alert severity="error" sx={{ mb: 3, background: 'rgba(251, 113, 133, 0.15)', border: '1px solid rgba(251, 113, 133, 0.3)', color: '#fda4af' }}>
          {error}
        </Alert>
      )}

      {/* ═══════════════════════════════════════════════════
          RESULTS DISPLAY: TABLE VIEW OR CARDS DOSSIER
          ═══════════════════════════════════════════════════ */}
      {isFirstLoad ? (
        <Stack spacing={2}>
          {[1, 2, 3, 4].map(i => <SkeletonLoader key={i} height={75} />)}
        </Stack>
      ) : results && results.totalElements === 0 ? (
        
        /* BESPOKE PT EMPTY VAULT STATE */
        <Box 
          className="webfolio-card"
          sx={{ 
            textAlign: 'center', 
            py: 9, 
            px: 3,
            border: '1px solid rgba(255, 255, 255, 0.1)'
          }}
        >
          <Box 
            sx={{ 
              width: 76, 
              height: 76, 
              mx: 'auto', 
              mb: 2.5, 
              borderRadius: '24px', 
              background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.15) 0%, rgba(202, 255, 51, 0.08) 100%)',
              border: '1.5px solid rgba(253, 91, 56, 0.35)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 30px rgba(253, 91, 56, 0.25)'
            }}
          >
            <ShieldIcon sx={{ color: '#fd5b38', fontSize: 36 }} />
          </Box>
          <Typography variant="h4" sx={{ color: '#fff', fontWeight: 800, mb: 1, fontFamily: '"Space Grotesk", sans-serif' }}>
            {hasActiveFilters ? 'No Gratuity Dispatches Matched Search Criteria' : 'Gratuity Ledger Awaiting Initial Settlement'}
          </Typography>
          <Typography variant="body2" sx={{ color: '#cdd2d8', maxWidth: 540, mx: 'auto', mb: 3.5, lineHeight: 1.6 }}>
            {hasActiveFilters 
              ? 'Try widening your chronological horizon or adjusting your bill and tip threshold filters.' 
              : 'Every gratuity calculated in Possible Tip is permanently recorded with tax deductibility telemetry, service analytics, and cryptographic integrity.'}
          </Typography>

          <Stack direction="row" spacing={2} sx={{ justifyContent: 'center', flexWrap: 'wrap' }}>
            {hasActiveFilters ? (
              <Button 
                variant="outlined" 
                startIcon={<RestartAltIcon />} 
                onClick={handleReset}
                className="butn butn-md butn-bord"
              >
                Clear Parameters
              </Button>
            ) : (
              <>
                <Button 
                  variant="contained" 
                  startIcon={<AddIcon />} 
                  onClick={() => navigate('/tips')}
                  className="butn butn-md butn-bg"
                >
                  Dispatch First Gratuity
                </Button>
                <Button 
                  variant="outlined" 
                  startIcon={seeding ? <CircularProgress size={18} color="inherit" /> : <AutoAwesomeIcon />} 
                  onClick={handleSeedDemoData}
                  disabled={seeding}
                  className="butn butn-md butn-bord"
                >
                  Initialize Demo Dispatches
                </Button>
              </>
            )}
          </Stack>
        </Box>

      ) : viewMode === 'table' ? (
        
        /* ═══ PT LEDGER TABLE VIEW ═══ */
        <Box className="webfolio-card" sx={{ borderRadius: '24px', overflow: 'hidden', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
          <TableContainer component={Paper} sx={{ background: 'transparent' }}>
            <Table sx={{ minWidth: 800 }}>
              <TableHead sx={{ background: 'rgba(10, 13, 23, 0.85)', borderBottom: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <TableRow>
                  <TableCell sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase', py: 2.2 }}>
                    Settled Venue & Timestamp
                  </TableCell>
                  <TableCell sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    Service Quality
                  </TableCell>
                  <TableCell align="right" sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    Bill Subtotal
                  </TableCell>
                  <TableCell align="right" sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    Gratuity Dispatched
                  </TableCell>
                  <TableCell align="center" sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    Rate %
                  </TableCell>
                  <TableCell align="center" sx={{ color: '#cdd2d8', fontWeight: 800, fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    Audit & Actions
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {results?.content.map((tip) => {
                  const formatted = formatDate(tip.createdAt);
                  return (
                    <TableRow 
                      key={tip.id}
                      hover
                      sx={{ 
                        borderBottom: '1px solid rgba(255,255,255,0.06)',
                        '&:hover': { background: 'rgba(253, 91, 56, 0.04) !important' },
                        transition: 'background 0.18s ease'
                      }}
                    >
                      {/* Venue & Date */}
                      <TableCell sx={{ py: 2.2 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                          <Box 
                            sx={{ 
                              width: 40, 
                              height: 40, 
                              borderRadius: '12px', 
                              background: 'rgba(253, 91, 56, 0.12)', 
                              border: '1.5px solid rgba(253, 91, 56, 0.35)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              color: '#fd5b38',
                              fontWeight: 900,
                              fontSize: '0.95rem'
                            }}
                          >
                            {(tip.restaurantName || 'P')[0].toUpperCase()}
                          </Box>
                          <Box>
                            <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 700, fontSize: '0.94rem' }}>
                              {tip.restaurantName || 'Private Dining Venue'}
                            </Typography>
                            <Typography variant="caption" sx={{ color: '#858b96', fontFamily: '"JetBrains Mono", monospace', fontSize: '0.72rem' }}>
                              {formatted.date} • {formatted.time}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>

                      {/* Service Rating */}
                      <TableCell>
                        {renderQualityStars(tip.serviceQuality)}
                      </TableCell>

                      {/* Bill Subtotal */}
                      <TableCell align="right">
                        <Typography variant="body2" sx={{ color: '#cdd2d8', fontFamily: '"JetBrains Mono", monospace', fontWeight: 600 }}>
                          {tip.currency} {Number(tip.billAmount).toFixed(2)}
                        </Typography>
                      </TableCell>

                      {/* Gratuity Dispatched */}
                      <TableCell align="right">
                        <Typography variant="body1" sx={{ color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', fontWeight: 800 }}>
                          +{tip.currency} {Number(tip.tipAmount).toFixed(2)}
                        </Typography>
                      </TableCell>

                      {/* Effective Rate */}
                      <TableCell align="center">
                        <Chip 
                          label={`${tip.tipPercentage}%`} 
                          size="small"
                          sx={{ 
                            background: 'rgba(253, 91, 56, 0.14)', 
                            color: '#fd5b38', 
                            border: '1px solid rgba(253, 91, 56, 0.35)',
                            fontFamily: '"JetBrains Mono", monospace',
                            fontWeight: 800,
                            fontSize: '0.75rem'
                          }} 
                        />
                      </TableCell>

                      {/* Actions */}
                      <TableCell align="center">
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'center' }}>
                          <Tooltip title="Inspect Audit Slip Voucher">
                            <Button
                              size="small"
                              variant="outlined"
                              onClick={() => setInspectingTip(tip)}
                              sx={{ 
                                minWidth: 'auto',
                                px: 1.5,
                                py: 0.4,
                                fontSize: '0.72rem',
                                color: '#fd5b38',
                                borderColor: 'rgba(253, 91, 56, 0.3)',
                                borderRadius: '999px',
                                '&:hover': { borderColor: '#fd5b38', background: 'rgba(253, 91, 56, 0.1)' }
                              }}
                            >
                              Audit Slip
                            </Button>
                          </Tooltip>

                          <Tooltip title="Purge Record">
                            <IconButton 
                              size="small" 
                              onClick={() => setDeleteTargetTip(tip)}
                              sx={{ color: '#858b96', '&:hover': { color: '#fb7185' } }}
                            >
                              <DeleteOutlineIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>

          <TablePagination
            component="div"
            count={results?.totalElements || 0}
            page={page}
            onPageChange={(e, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
            rowsPerPageOptions={[5, 10, 20, 50]}
            sx={{ 
              color: '#858b96', 
              background: 'rgba(10, 13, 23, 0.85)',
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
              '& .MuiTablePagination-select': { color: '#fd5b38' }
            }}
          />
        </Box>

      ) : (

        /* ═══ PT CARD DOSSIER VIEW ═══ */
        <Box>
          <Box 
            sx={{ 
              display: 'grid', 
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr', lg: 'repeat(3, 1fr)' }, 
              gap: 3 
            }}
          >
            {results?.content.map((tip) => {
              const formatted = formatDate(tip.createdAt);
              return (
                <Card 
                  key={tip.id}
                  className="webfolio-card"
                  sx={{ 
                    p: 3, 
                    position: 'relative',
                    transition: 'transform 0.2s cubic-bezier(0.16, 1, 0.3, 1)'
                  }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                      <Box 
                        sx={{ 
                          width: 38, 
                          height: 38, 
                          borderRadius: '12px', 
                          background: 'rgba(253, 91, 56, 0.12)', 
                          border: '1px solid rgba(253, 91, 56, 0.35)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: '#fd5b38',
                          fontWeight: 900
                        }}
                      >
                        {(tip.restaurantName || 'P')[0].toUpperCase()}
                      </Box>
                      <Box>
                        <Typography variant="subtitle1" sx={{ color: '#fff', fontWeight: 700, lineHeight: 1.2 }}>
                          {tip.restaurantName || 'Private Dining Venue'}
                        </Typography>
                        <Typography variant="caption" sx={{ color: '#858b96', fontFamily: '"JetBrains Mono", monospace' }}>
                          {formatted.date}
                        </Typography>
                      </Box>
                    </Box>

                    <Chip 
                      label={`${tip.tipPercentage}%`} 
                      size="small"
                      sx={{ 
                        background: 'rgba(253, 91, 56, 0.14)', 
                        color: '#fd5b38', 
                        border: '1px solid rgba(253, 91, 56, 0.35)',
                        fontFamily: '"JetBrains Mono", monospace',
                        fontWeight: 800
                      }} 
                    />
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', my: 2.5, p: 1.75, borderRadius: '14px', background: 'rgba(10, 13, 23, 0.65)', border: '1px solid rgba(255,255,255,0.06)' }}>
                    <Box>
                      <Typography variant="caption" sx={{ color: '#858b96', display: 'block', fontSize: '0.7rem' }}>Bill Subtotal</Typography>
                      <Typography variant="body1" sx={{ color: '#cdd2d8', fontFamily: '"JetBrains Mono", monospace', fontWeight: 600 }}>
                        {tip.currency} {Number(tip.billAmount).toFixed(2)}
                      </Typography>
                    </Box>
                    <Box sx={{ textAlign: 'right' }}>
                      <Typography variant="caption" sx={{ color: '#fd5b38', display: 'block', fontSize: '0.7rem', fontWeight: 700 }}>Gratuity Settled</Typography>
                      <Typography variant="h6" sx={{ color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', fontWeight: 800 }}>
                        +{tip.currency} {Number(tip.tipAmount).toFixed(2)}
                      </Typography>
                    </Box>
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pt: 1.5, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
                    {renderQualityStars(tip.serviceQuality)}

                    <Stack direction="row" spacing={0.5}>
                      <Tooltip title="View Audit Voucher">
                        <IconButton size="small" onClick={() => setInspectingTip(tip)} sx={{ color: '#fd5b38' }}>
                          <ReceiptLongIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Purge Record">
                        <IconButton size="small" onClick={() => setDeleteTargetTip(tip)} sx={{ color: '#858b96', '&:hover': { color: '#fb7185' } }}>
                          <DeleteOutlineIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </Box>
                </Card>
              );
            })}
          </Box>

          <TablePagination
            component="div"
            count={results?.totalElements || 0}
            page={page}
            onPageChange={(e, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
            rowsPerPageOptions={[6, 12, 24]}
            sx={{ 
              color: '#858b96', 
              mt: 3,
              '& .MuiTablePagination-select': { color: '#fd5b38' }
            }}
          />
        </Box>
      )}

      {/* ═══════════════════════════════════════════════════
          INSPECTION MODAL: PT GRATUITY AUDIT SLIP
          ═══════════════════════════════════════════════════ */}
      <Dialog 
        open={Boolean(inspectingTip)} 
        onClose={() => setInspectingTip(null)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          className: 'webfolio-card',
          sx: {
            background: 'rgba(16, 20, 36, 0.98)',
            border: '1px solid rgba(253, 91, 56, 0.35)',
            borderRadius: '24px',
            p: 1
          }
        }}
      >
        {inspectingTip && (
          <>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                <PtLogo size={28} showText={false} />
                <Typography variant="h6" sx={{ fontWeight: 800, color: '#fff', letterSpacing: '-0.02em' }}>
                  PT Gratuity Audit Voucher
                </Typography>
              </Box>
              <IconButton size="small" onClick={() => setInspectingTip(null)} sx={{ color: '#858b96' }}>
                <CloseIcon fontSize="small" />
              </IconButton>
            </DialogTitle>

            <DialogContent>
              {/* Slip Body with Receipt Styling */}
              <Box sx={{ p: 3, borderRadius: '18px', background: 'rgba(10, 13, 23, 0.75)', border: '1px dashed rgba(253, 91, 56, 0.35)', my: 1 }}>
                <Box sx={{ textAlign: 'center', mb: 2.5, pb: 2, borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
                  <Typography variant="caption" sx={{ color: '#fd5b38', letterSpacing: '0.14em', fontWeight: 800, textTransform: 'uppercase', display: 'block' }}>
                    ✦ PT · POSSIBLE TIP AUDIT SYSTEM
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800, color: '#fff', mt: 0.5 }}>
                    {inspectingTip.restaurantName || 'Private Dining Venue'}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#858b96', fontFamily: '"JetBrains Mono", monospace' }}>
                    {formatDate(inspectingTip.createdAt).date} • {formatDate(inspectingTip.createdAt).time}
                  </Typography>
                </Box>

                <Stack spacing={1.5} sx={{ mb: 2.5 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" sx={{ color: '#858b96' }}>Bill Subtotal</Typography>
                    <Typography variant="body2" sx={{ color: '#cdd2d8', fontFamily: '"JetBrains Mono", monospace' }}>
                      {inspectingTip.currency} {Number(inspectingTip.billAmount).toFixed(2)}
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" sx={{ color: '#858b96' }}>Service Rating</Typography>
                    <Box>{renderQualityStars(inspectingTip.serviceQuality)}</Box>
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" sx={{ color: '#858b96' }}>Gratuity Rate</Typography>
                    <Typography variant="body2" sx={{ color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', fontWeight: 700 }}>
                      {inspectingTip.tipPercentage}%
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" sx={{ color: '#fd5b38', fontWeight: 700 }}>Dispatched Gratuity</Typography>
                    <Typography variant="body1" sx={{ color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', fontWeight: 800 }}>
                      +{inspectingTip.currency} {Number(inspectingTip.tipAmount).toFixed(2)}
                    </Typography>
                  </Box>

                  <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.08)', my: 1 }} />

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle1" sx={{ color: '#fff', fontWeight: 800 }}>Total Settled</Typography>
                    <Typography variant="h5" sx={{ color: '#fd5b38', fontFamily: '"JetBrains Mono", monospace', fontWeight: 800 }}>
                      {inspectingTip.currency} {(Number(inspectingTip.billAmount) + Number(inspectingTip.tipAmount)).toFixed(2)}
                    </Typography>
                  </Box>
                </Stack>

                {/* Fiscal & Tax Note */}
                <Box sx={{ p: 1.75, borderRadius: '12px', background: 'rgba(253, 91, 56, 0.08)', border: '1px solid rgba(253, 91, 56, 0.25)' }}>
                  <Typography variant="caption" sx={{ color: '#fd5b38', display: 'block', fontWeight: 800 }}>
                    IRC § 274 TAX DEDUCTION COMPLIANT:
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#858b96', display: 'block', mt: 0.25, lineHeight: 1.5 }}>
                    Transaction cryptographically indexed under patron ledger. Eligible for corporate hospitality and business deduction verification.
                  </Typography>
                </Box>
              </Box>
            </DialogContent>

            <DialogActions sx={{ px: 3, pb: 2.5 }}>
              <Button 
                startIcon={<PrintIcon />}
                onClick={() => window.print()} 
                className="butn butn-sm butn-bord"
              >
                Print Slip
              </Button>
              <Button 
                onClick={() => setInspectingTip(null)} 
                className="butn butn-sm butn-bg"
              >
                Dismiss
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* ═══════════════════════════════════════════════════
          CONFIRM DELETE DIALOG
          ═══════════════════════════════════════════════════ */}
      <Dialog 
        open={Boolean(deleteTargetTip)} 
        onClose={() => setDeleteTargetTip(null)}
        PaperProps={{
          className: 'webfolio-card',
          sx: { background: '#121629', border: '1px solid rgba(251, 113, 133, 0.4)', borderRadius: '24px' }
        }}
      >
        <DialogTitle sx={{ color: '#fff', fontWeight: 800 }}>
          Purge Ledger Record?
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ color: '#cdd2d8' }}>
            Are you sure you want to purge the gratuity record for <strong>{deleteTargetTip?.restaurantName || 'this venue'}</strong>? This cryptographic audit ledger action cannot be reversed.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2.5 }}>
          <Button onClick={() => setDeleteTargetTip(null)} sx={{ color: '#858b96' }} disabled={deleting}>
            Cancel
          </Button>
          <Button 
            onClick={confirmDeleteTip} 
            variant="contained" 
            color="error"
            disabled={deleting}
            sx={{ fontWeight: 800, borderRadius: '999px', px: 2.5 }}
          >
            {deleting ? <CircularProgress size={16} color="inherit" /> : 'Confirm Purge'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* NOTIFICATION TOAST */}
      <Snackbar 
        open={notification.open} 
        autoHideDuration={4000} 
        onClose={() => setNotification(prev => ({ ...prev, open: false }))}
      >
        <Alert 
          severity={notification.severity} 
          variant="filled" 
          onClose={() => setNotification(prev => ({ ...prev, open: false }))}
          sx={{ borderRadius: '12px' }}
        >
          {notification.message}
        </Alert>
      </Snackbar>

    </Box>
  );
};

export default TipHistoryPage;
