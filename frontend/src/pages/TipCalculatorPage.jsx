/**
 * Tip Calculator Page
 *
 * PURPOSE:
 * A comprehensive calculator for bills and tips. Features real-time calculation,
 * equal/custom splitting, and the ability to save tips to history.
 *
 * DESIGN:
 * Premium dark glassmorphism aesthetic matching Day 1 styles.
 * Uses Material UI grid and stack components for responsive layout.
 */
import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Grid,
  Stack,
  InputAdornment,
  MenuItem,
  Divider,
  Snackbar,
  Alert,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import {
  Receipt as ReceiptIcon,
  Restaurant as RestaurantIcon,
  Group as GroupIcon,
  Save as SaveIcon,
  Delete as DeleteIcon,
  AttachMoney as MoneyIcon,
  Calculate as CalculateIcon,
  PersonAdd as PersonAddIcon,
  RemoveCircleOutlined as RemoveIcon,
  AutoAwesome as SparkleIcon,
  TrendingUp as TrendingUpIcon
} from '@mui/icons-material';
import tipsApi from '../api/tipsApi';
import aiApi from '../api/aiApi';
import personalizationApi from '../api/personalizationApi';
import currencyApi from '../api/currencyApi';
import serviceQualityApi from '../api/serviceQualityApi';
import tipTimingApi from '../api/tipTimingApi';
import { AccessTime as TimeIcon } from '@mui/icons-material';

const PRESET_TIPS = [10, 15, 18, 20];
const CURRENCIES = [
  { code: 'USD', symbol: '$', label: 'US Dollar' },
  { code: 'EUR', symbol: '€', label: 'Euro' },
  { code: 'GBP', symbol: '£', label: 'British Pound' },
  { code: 'INR', symbol: '₹', label: 'Indian Rupee' },
  { code: 'JPY', symbol: '¥', label: 'Japanese Yen' }
];

const TipCalculatorPage = () => {
  // Main form state
  const [billAmount, setBillAmount] = useState('');
  const [tipPercentage, setTipPercentage] = useState(15);
  const [customTip, setCustomTip] = useState('');
  const [restaurantName, setRestaurantName] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [displayCurrency, setDisplayCurrency] = useState('USD');

  // Currency conversion state
  const [exchangeRate, setExchangeRate] = useState(null);
  const [isConverting, setIsConverting] = useState(false);

  // Calculation state (local real-time)
  const [calculatedTip, setCalculatedTip] = useState(0);
  const [calculatedTotal, setCalculatedTotal] = useState(0);

  // Split state
  const [isSplitting, setIsSplitting] = useState(false);
  const [splitMode, setSplitMode] = useState('equal'); // 'equal' or 'custom'
  const [people, setPeople] = useState([{ name: 'Person 1', amount: '' }, { name: 'Person 2', amount: '' }]);
  const [splitResult, setSplitResult] = useState(null);

  // History state
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  // UI state
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [saving, setSaving] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [tipToDelete, setTipToDelete] = useState(null);
  
  // AI state
  const [restaurantType, setRestaurantType] = useState('CASUAL_DINING');
  const [serviceQuality, setServiceQuality] = useState('GOOD');
  const [country, setCountry] = useState('USA');
  const [occasion, setOccasion] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState(null);

  // Personalization state (Day 4)
  const [personalization, setPersonalization] = useState(null);
  const [restaurantInsight, setRestaurantInsight] = useState(null);

  // Service Quality stats state (Day 6)
  const [serviceQualityStats, setServiceQualityStats] = useState(null);

  // Tip Timing state (Day 9)
  const [timingResult, setTimingResult] = useState(null);
  const [timingLoading, setTimingLoading] = useState(false);
  const [lastTimingRestaurant, setLastTimingRestaurant] = useState(null);
  const [tipSaved, setTipSaved] = useState(false);

  // Real-time calculation logic
  const performCalculations = useCallback(() => {
    const bill = parseFloat(billAmount);
    if (isNaN(bill) || bill <= 0) {
      setCalculatedTip(0);
      setCalculatedTotal(0);
      setSplitResult(null);
      return;
    }

    const tipAmount = (bill * tipPercentage) / 100;
    const total = bill + tipAmount;

    setCalculatedTip(Number(tipAmount.toFixed(2)));
    setCalculatedTotal(Number(total.toFixed(2)));
    
    // Auto-update equal split if enabled
    if (isSplitting && splitMode === 'equal' && people.length >= 2) {
       calculateSplitOnBackend(bill, tipPercentage);
    } else {
        setSplitResult(null);
    }
  }, [billAmount, tipPercentage, isSplitting, splitMode, people.length]);

  useEffect(() => {
    performCalculations();
  }, [performCalculations]);

  useEffect(() => {
    loadHistory(0);
    loadPersonalizationSummary();
    loadServiceQualityStats();
  }, []);

  // Day 9: Debounced Tip Timing Effect
  useEffect(() => {
    const bill = parseFloat(billAmount);
    if (isNaN(bill) || bill <= 0 || !restaurantName || !tipPercentage || !serviceQuality || tipSaved) {
        setTimingResult(null);
        return;
    }

    const currentRestaurant = restaurantName.trim();
    
    // Only call API if restaurant changed or we haven't fetched yet
    if (currentRestaurant === lastTimingRestaurant && timingResult) {
        return;
    }

    const timerId = setTimeout(async () => {
        try {
            setTimingLoading(true);
            const req = {
                billAmount: bill,
                tipPercentage,
                restaurantName: currentRestaurant,
                serviceQuality,
                saved: tipSaved
            };
            const result = await tipTimingApi.getTipTiming(req);
            if (result.state !== 'NOT_READY') {
                setTimingResult(result);
            } else {
                setTimingResult(null);
            }
            setLastTimingRestaurant(currentRestaurant);
        } catch (err) {
            console.error('Failed to fetch tip timing', err);
        } finally {
            setTimingLoading(false);
        }
    }, 800);

    return () => clearTimeout(timerId);
  }, [billAmount, tipPercentage, restaurantName, serviceQuality, tipSaved, lastTimingRestaurant, timingResult]);

  const loadPersonalizationSummary = async () => {
    try {
      const data = await personalizationApi.getSummary();
      setPersonalization(data);
    } catch (error) {
      // Graceful degradation: personalization is optional
      console.warn('Could not load personalization summary');
    }
  };

  const loadServiceQualityStats = async () => {
    try {
      const data = await serviceQualityApi.getSummary();
      setServiceQualityStats(data);
    } catch (error) {
      // Graceful degradation: stats are optional
      console.warn('Could not load service quality stats');
    }
  };

  useEffect(() => {
    const fetchExchangeRate = async () => {
      if (currency === displayCurrency) {
        setExchangeRate(1.0);
        return;
      }

      setIsConverting(true);
      try {
        const data = await currencyApi.getExchangeRate(currency, displayCurrency);
        setExchangeRate(data.exchangeRate);
      } catch (error) {
        console.error('Failed to fetch exchange rate', error);
        setExchangeRate(null);
        showSnackbar('Currency conversion is temporarily unavailable.', 'warning');
      } finally {
        setIsConverting(false);
      }
    };

    fetchExchangeRate();
  }, [currency, displayCurrency]);

  const loadRestaurantInsight = async (name) => {
    if (!name || name.trim() === '') {
      setRestaurantInsight(null);
      return;
    }
    try {
      const data = await personalizationApi.getRestaurantPersonalization(name.trim());
      setRestaurantInsight(data.restaurantInsight);
    } catch (error) {
      console.warn('Could not load restaurant insight');
      setRestaurantInsight(null);
    }
  };

  const loadHistory = async (pageNum, append = false) => {
    try {
      setLoadingHistory(true);
      const data = await tipsApi.getUserTips(pageNum, 10);
      if (append) {
          setHistory(prev => [...prev, ...data.content]);
      } else {
          setHistory(data.content);
      }
      setHasMore(!data.last);
      setPage(pageNum);
    } catch (error) {
      showSnackbar('Failed to load history', 'error');
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleBillChange = (e) => {
    // Only allow positive numbers and max 2 decimals
    const val = e.target.value;
    if (val === '' || /^\d*\.?\d{0,2}$/.test(val)) {
      setBillAmount(val);
      setTipSaved(false);
    }
  };

  const handlePresetTip = (percent) => {
    setTipPercentage(percent);
    setCustomTip('');
    setTipSaved(false);
  };

  const handleCustomTipChange = (e) => {
    const val = e.target.value;
    if (val === '' || /^\d*\.?\d{0,2}$/.test(val)) {
      setCustomTip(val);
      const numVal = parseFloat(val);
      if (!isNaN(numVal) && numVal >= 0 && numVal <= 100) {
        setTipPercentage(numVal);
        setTipSaved(false);
      }
    }
  };

  const addPerson = () => {
    setPeople([...people, { name: `Person ${people.length + 1}`, amount: '' }]);
  };

  const removePerson = (index) => {
    if (people.length > 2) {
      const newPeople = [...people];
      newPeople.splice(index, 1);
      setPeople(newPeople);
    }
  };

  const handlePersonChange = (index, field, value) => {
    const newPeople = [...people];
    if (field === 'amount' && !/^\d*\.?\d{0,2}$/.test(value) && value !== '') {
        return; // invalid money format
    }
    newPeople[index][field] = value;
    setPeople(newPeople);
  };

  const calculateSplitOnBackend = async (bill = parseFloat(billAmount), tip = tipPercentage) => {
    if (isNaN(bill) || bill <= 0) return;

    try {
      const requestData = {
        billAmount: bill,
        tipPercentage: tip,
      };

      if (splitMode === 'equal') {
        requestData.people = people.map(p => p.name || 'Anonymous');
      } else {
        const customSplitMap = {};
        let totalCustom = 0;
        
        people.forEach(p => {
            const amt = parseFloat(p.amount || 0);
            totalCustom += amt;
            // Prevent duplicate names in map
            const name = p.name.trim() || `Person_${Math.random().toString(36).substr(2, 5)}`;
            customSplitMap[name] = amt;
        });
        
        // Basic frontend validation before sending
        const expectedTotal = Number((bill + (bill * tip) / 100).toFixed(2));
        const actualTotal = Number(totalCustom.toFixed(2));
        
        if (Math.abs(expectedTotal - actualTotal) > 0.05) {
             showSnackbar(`Custom amounts total $${actualTotal}, but should equal $${expectedTotal}`, 'warning');
             return;
        }

        requestData.customSplit = customSplitMap;
      }

      const result = await tipsApi.splitTip(requestData);
      setSplitResult(result);
      if (splitMode === 'custom') {
          showSnackbar('Custom split calculated successfully');
      }
    } catch (error) {
      showSnackbar(error.response?.data?.message || 'Split calculation failed', 'error');
    }
  };

  const handleSaveTip = async () => {
    const bill = parseFloat(billAmount);
    if (isNaN(bill) || bill <= 0) {
      showSnackbar('Please enter a valid bill amount', 'warning');
      return;
    }

    // Rule 3: serviceQuality is required before saving — validated here
    if (!serviceQuality) {
      showSnackbar('Please select a service quality rating', 'warning');
      return;
    }

    try {
      setSaving(true);
      await tipsApi.createTip({
        billAmount: bill,
        tipPercentage,
        restaurantName: restaurantName.trim(),
        currency,
        serviceQuality  // shared state — same value sent to AI and database
      });
      showSnackbar('Tip saved successfully!');
      setTipSaved(true);
      // Reload page 0 to show the new tip at the top
      loadHistory(0);
      loadServiceQualityStats(); // refresh stats after save
      
      // Optional: reset form after save
      // setBillAmount('');
      // setRestaurantName('');
    } catch (error) {
      showSnackbar(error.response?.data?.message || 'Failed to save tip', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTip = async () => {
    if (!tipToDelete) return;
    
    try {
      await tipsApi.deleteTip(tipToDelete);
      showSnackbar('Tip deleted');
      setHistory(history.filter(t => t.id !== tipToDelete));
    } catch (error) {
      showSnackbar('Failed to delete tip', 'error');
    } finally {
      setDeleteDialogOpen(false);
      setTipToDelete(null);
    }
  };

  const handleGetAiSuggestion = async () => {
    const bill = parseFloat(billAmount);
    if (isNaN(bill) || bill <= 0) {
      showSnackbar('Please enter a valid bill amount first', 'warning');
      return;
    }

    try {
      setAiLoading(true);
      const result = await aiApi.getAiSuggestion({
        billAmount: bill,
        restaurantType,
        serviceQuality,
        country,
        currency,
        occasion: occasion.trim() || null,
        restaurantName: restaurantName.trim() || null
      });
      setAiResult(result);
    } catch (error) {
      showSnackbar(error.response?.data?.message || 'AI recommendation is temporarily unavailable. You can still calculate your tip manually.', 'error');
    } finally {
      setAiLoading(false);
    }
  };

  const applyAiRecommendation = () => {
    if (aiResult) {
        setTipPercentage(aiResult.recommendedPercentage);
        setCustomTip(''); // Clear custom text box
        showSnackbar(`Applied AI recommendation of ${aiResult.recommendedPercentage}%`);
    }
  };

  const showSnackbar = (message, severity = 'success') => {
    setSnackbar({ open: true, message, severity });
  };

  const formatCurrency = (amount, curr = currency) => {
    const symbol = CURRENCIES.find(c => c.code === curr)?.symbol || '$';
    return `${symbol}${Number(amount).toFixed(2)}`;
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #0A0E1A 0%, #121829 100%)',
        pt: 4,
        pb: 8,
        px: { xs: 2, sm: 4 },
      }}
    >
      <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
        <Typography
          variant="h3"
          sx={{
            fontWeight: 800,
            mb: 4,
            background: 'linear-gradient(135deg, #6C63FF, #00D9FF)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            display: 'flex',
            alignItems: 'center',
            gap: 2
          }}
        >
          <CalculateIcon sx={{ fontSize: 40, color: '#6C63FF' }} />
          Tip Calculator
        </Typography>

        <Grid container spacing={4}>
          {/* LEFT COLUMN: Inputs & Controls */}
          <Grid item xs={12} md={7}>
            <Card
              sx={{
                background: 'rgba(18, 24, 41, 0.7)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: 3,
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
                mb: 4
              }}
            >
              <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
                <Stack spacing={4}>
                  
                  {/* Bill Details */}
                  <Box>
                    <Typography variant="h6" sx={{ color: '#E8EAED', mb: 2, fontWeight: 600 }}>
                      Bill Details
                    </Typography>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={8}>
                        <TextField
                          fullWidth
                          label="Bill Amount"
                          variant="outlined"
                          value={billAmount}
                          onChange={handleBillChange}
                          placeholder="0.00"
                          InputProps={{
                            startAdornment: (
                              <InputAdornment position="start">
                                <ReceiptIcon sx={{ color: '#6C63FF' }} />
                              </InputAdornment>
                            ),
                            sx: { fontSize: '1.2rem', fontWeight: 600 }
                          }}
                        />
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <TextField
                          select
                          fullWidth
                          label="Bill Currency"
                          value={currency}
                          onChange={(e) => setCurrency(e.target.value)}
                        >
                          {CURRENCIES.map((option) => (
                            <MenuItem key={option.code} value={option.code}>
                              {option.code} ({option.symbol})
                            </MenuItem>
                          ))}
                        </TextField>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <TextField
                          select
                          fullWidth
                          label="Display Currency"
                          value={displayCurrency}
                          onChange={(e) => setDisplayCurrency(e.target.value)}
                        >
                          {CURRENCIES.map((option) => (
                            <MenuItem key={option.code} value={option.code}>
                              {option.code} ({option.symbol})
                            </MenuItem>
                          ))}
                        </TextField>
                      </Grid>
                      <Grid item xs={12}>
                        <TextField
                          fullWidth
                          label="Restaurant / Note (Optional)"
                          variant="outlined"
                          value={restaurantName}
                          onChange={(e) => { setRestaurantName(e.target.value); setTipSaved(false); }}
                          onBlur={() => loadRestaurantInsight(restaurantName)}
                          InputProps={{
                            startAdornment: (
                              <InputAdornment position="start">
                                <RestaurantIcon sx={{ color: '#9AA0A6' }} />
                              </InputAdornment>
                            ),
                          }}
                        />
                      </Grid>
                    </Grid>
                  </Box>

                  <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.06)' }} />

                  {/* Tip Selection */}
                  <Box>
                    <Typography variant="h6" sx={{ color: '#E8EAED', mb: 2, fontWeight: 600 }}>
                      Tip Percentage: {tipPercentage}%
                    </Typography>
                    <Grid container spacing={2}>
                      {PRESET_TIPS.map((preset) => (
                        <Grid item xs={6} sm={3} key={preset}>
                          <Button
                            fullWidth
                            variant={tipPercentage === preset && customTip === '' ? "contained" : "outlined"}
                            onClick={() => handlePresetTip(preset)}
                            sx={{ 
                                py: 1.5, 
                                fontSize: '1.1rem',
                                borderColor: 'rgba(108, 99, 255, 0.3)',
                                ...(tipPercentage === preset && customTip === '' ? {
                                    background: 'linear-gradient(135deg, #6C63FF 0%, #4A42D4 100%)',
                                } : {
                                    color: '#E8EAED',
                                    '&:hover': { borderColor: '#6C63FF', background: 'rgba(108, 99, 255, 0.08)' }
                                })
                            }}
                          >
                            {preset}%
                          </Button>
                        </Grid>
                      ))}
                      <Grid item xs={12}>
                        <TextField
                          fullWidth
                          label="Custom Tip %"
                          variant="outlined"
                          value={customTip}
                          onChange={handleCustomTipChange}
                          placeholder="e.g. 12.5"
                          sx={{ mt: 1 }}
                        />
                      </Grid>
                    </Grid>
                  </Box>

                  <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.06)' }} />

                  {/* AI Suggestion Form */}
                  <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                       <SparkleIcon sx={{ color: '#00D9FF' }} />
                       <Typography variant="h6" sx={{ color: '#E8EAED', fontWeight: 600 }}>
                          Ask AI for Recommendation
                       </Typography>
                    </Box>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          select
                          fullWidth
                          label="Restaurant / Service Type"
                          value={restaurantType}
                          onChange={(e) => setRestaurantType(e.target.value)}
                        >
                          <MenuItem value="CASUAL_DINING">Casual Dining</MenuItem>
                          <MenuItem value="FINE_DINING">Fine Dining</MenuItem>
                          <MenuItem value="CAFE">Cafe / Coffee Shop</MenuItem>
                          <MenuItem value="FAST_FOOD">Fast Food / Takeout</MenuItem>
                          <MenuItem value="BAR">Bar / Bartender</MenuItem>
                          <MenuItem value="HAIR_SALON">Hair Salon / Barber</MenuItem>
                          <MenuItem value="TAXI">Taxi / Rideshare</MenuItem>
                          <MenuItem value="OTHER">Other</MenuItem>
                        </TextField>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          select
                          fullWidth
                          label="Service Quality"
                          value={serviceQuality}
                          onChange={(e) => { setServiceQuality(e.target.value); setTipSaved(false); }}
                        >
                          <MenuItem value="EXCELLENT">Excellent</MenuItem>
                          <MenuItem value="GOOD">Good</MenuItem>
                          <MenuItem value="AVERAGE">Average</MenuItem>
                          <MenuItem value="POOR">Poor</MenuItem>
                        </TextField>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          label="Country"
                          value={country}
                          onChange={(e) => setCountry(e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          fullWidth
                          label="Occasion (Optional)"
                          placeholder="e.g. Anniversary"
                          value={occasion}
                          onChange={(e) => setOccasion(e.target.value)}
                        />
                      </Grid>
                      <Grid item xs={12}>
                        <Button
                          fullWidth
                          variant="outlined"
                          onClick={handleGetAiSuggestion}
                          disabled={aiLoading || !billAmount || parseFloat(billAmount) <= 0}
                          startIcon={aiLoading ? <CircularProgress size={20} color="inherit" /> : <SparkleIcon />}
                          sx={{ 
                              py: 1.5,
                              color: '#00D9FF',
                              borderColor: 'rgba(0, 217, 255, 0.5)',
                              '&:hover': { borderColor: '#00D9FF', background: 'rgba(0, 217, 255, 0.1)' }
                          }}
                        >
                          {aiLoading ? 'Analyzing your service details...' : 'Get AI Suggestion'}
                        </Button>
                      </Grid>
                    </Grid>
                  </Box>

                  <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.06)' }} />

                  {/* Split Section */}
                  <Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                       <Typography variant="h6" sx={{ color: '#E8EAED', fontWeight: 600 }}>
                          Split Bill
                       </Typography>
                       <Button 
                         variant={isSplitting ? "outlined" : "text"} 
                         onClick={() => setIsSplitting(!isSplitting)}
                         color={isSplitting ? "primary" : "inherit"}
                         sx={{ color: isSplitting ? '#6C63FF' : '#9AA0A6' }}
                       >
                         {isSplitting ? 'Disable Split' : 'Enable Split'}
                       </Button>
                    </Box>

                    {isSplitting && (
                      <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2, border: '1px solid rgba(255,255,255,0.05)' }}>
                         <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
                           <Button 
                             variant={splitMode === 'equal' ? 'contained' : 'outlined'} 
                             onClick={() => setSplitMode('equal')}
                             size="small"
                             sx={splitMode === 'equal' ? { background: '#6C63FF' } : {}}
                           >
                             Equal Split
                           </Button>
                           <Button 
                             variant={splitMode === 'custom' ? 'contained' : 'outlined'} 
                             onClick={() => setSplitMode('custom')}
                             size="small"
                             sx={splitMode === 'custom' ? { background: '#6C63FF' } : {}}
                           >
                             Custom Amounts
                           </Button>
                         </Stack>

                         <Stack spacing={2}>
                           {people.map((person, index) => (
                              <Box key={index} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                                 <TextField
                                   size="small"
                                   value={person.name}
                                   onChange={(e) => handlePersonChange(index, 'name', e.target.value)}
                                   placeholder={`Person ${index + 1}`}
                                   sx={{ flexGrow: 1 }}
                                 />
                                 {splitMode === 'custom' && (
                                     <TextField
                                        size="small"
                                        value={person.amount}
                                        onChange={(e) => handlePersonChange(index, 'amount', e.target.value)}
                                        placeholder="Amount"
                                        sx={{ width: 120 }}
                                     />
                                 )}
                                 <IconButton 
                                   onClick={() => removePerson(index)} 
                                   disabled={people.length <= 2}
                                   color="error"
                                 >
                                    <RemoveIcon />
                                 </IconButton>
                              </Box>
                           ))}
                           
                           <Box sx={{ display: 'flex', justifyContent: 'space-between', pt: 1 }}>
                               <Button startIcon={<PersonAddIcon />} onClick={addPerson} sx={{ color: '#00D9FF' }}>
                                 Add Person
                               </Button>
                               
                               {splitMode === 'custom' && (
                                   <Button 
                                     variant="contained" 
                                     size="small" 
                                     onClick={() => calculateSplitOnBackend()}
                                     sx={{ background: 'linear-gradient(135deg, #00D9FF, #0088FF)' }}
                                   >
                                     Calculate Custom Split
                                   </Button>
                               )}
                           </Box>
                         </Stack>

                         {/* Split Results Display */}
                         {splitResult && (
                             <Box sx={{ mt: 3, pt: 2, borderTop: '1px dashed rgba(255,255,255,0.1)' }}>
                                <Typography variant="subtitle2" sx={{ color: '#9AA0A6', mb: 1 }}>
                                  Split Breakdown:
                                </Typography>
                                {splitResult.splits.map((s, i) => (
                                    <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                        <Typography variant="body2">{s.name}</Typography>
                                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                                            {formatCurrency(s.amountOwed)}
                                        </Typography>
                                    </Box>
                                ))}
                             </Box>
                         )}
                      </Box>
                    )}
                  </Box>

                </Stack>
              </CardContent>
            </Card>
          </Grid>

          {/* RIGHT COLUMN: Results & History */}
          <Grid item xs={12} md={5}>
            {/* Live Results Card */}
            <Card
              sx={{
                background: 'linear-gradient(135deg, rgba(108, 99, 255, 0.15) 0%, rgba(0, 217, 255, 0.05) 100%)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(108, 99, 255, 0.3)',
                borderRadius: 3,
                mb: 4
              }}
            >
              <CardContent sx={{ p: 4, textAlign: 'center' }}>
                <Typography variant="h6" sx={{ color: '#9AA0A6', mb: 1 }}>
                  Total Amount
                </Typography>
                <Typography 
                  variant="h2" 
                  sx={{ 
                    fontWeight: 800, 
                    color: '#fff',
                    mb: 3,
                    textShadow: '0 4px 20px rgba(108, 99, 255, 0.5)'
                  }}
                >
                  {formatCurrency(calculatedTotal)}
                </Typography>

                <Grid container spacing={2} sx={{ mb: 4 }}>
                  <Grid item xs={6}>
                    <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                        <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Bill</Typography>
                        <Typography variant="h6" sx={{ color: '#E8EAED' }}>{formatCurrency(billAmount || 0, currency)}</Typography>
                    </Box>
                  </Grid>
                  <Grid item xs={6}>
                    <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                        <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Tip ({tipPercentage}%)</Typography>
                        <Typography variant="h6" sx={{ color: '#00D9FF' }}>+{formatCurrency(calculatedTip, currency)}</Typography>
                    </Box>
                  </Grid>
                </Grid>

                {/* Day 5: Currency Conversion Block */}
                {currency !== displayCurrency && exchangeRate && (
                  <Box sx={{ mb: 4, p: 2, background: 'rgba(255, 255, 255, 0.05)', borderRadius: 2, border: '1px solid rgba(255,255,255,0.1)' }}>
                    <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block', mb: 1 }}>
                      Converted to {displayCurrency} (Rate: {exchangeRate})
                    </Typography>
                    {isConverting ? (
                      <CircularProgress size={24} sx={{ color: '#6C63FF' }} />
                    ) : (
                      <>
                        <Typography variant="h4" sx={{ fontWeight: 700, color: '#fff', mb: 2 }}>
                          {formatCurrency(calculatedTotal * exchangeRate, displayCurrency)}
                        </Typography>
                        <Grid container spacing={2}>
                          <Grid item xs={6}>
                            <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Bill</Typography>
                            <Typography variant="subtitle1" sx={{ color: '#E8EAED' }}>
                              {formatCurrency((parseFloat(billAmount) || 0) * exchangeRate, displayCurrency)}
                            </Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Tip</Typography>
                            <Typography variant="subtitle1" sx={{ color: '#00D9FF' }}>
                              +{formatCurrency(calculatedTip * exchangeRate, displayCurrency)}
                            </Typography>
                          </Grid>
                        </Grid>
                      </>
                    )}
                  </Box>
                )}

                {/* Day 6: Service Quality Rating — shared state with AI section */}
                <Box
                  sx={{
                    p: 2,
                    background: 'rgba(108, 99, 255, 0.06)',
                    borderRadius: 2,
                    border: '1px solid rgba(108, 99, 255, 0.18)',
                    mb: 1
                  }}
                >
                  <Typography variant="subtitle2" sx={{ color: '#9AA0A6', mb: 1.5, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 }}>
                    Service Quality
                  </Typography>
                  <Grid container spacing={1}>
                    {[
                      { value: 'POOR',      label: 'Poor',      color: '#FF5252', stars: '★' },
                      { value: 'AVERAGE',   label: 'Average',   color: '#FFA726', stars: '★★' },
                      { value: 'GOOD',      label: 'Good',      color: '#66BB6A', stars: '★★★' },
                      { value: 'EXCELLENT', label: 'Excellent', color: '#00D9FF', stars: '★★★★' },
                    ].map(({ value, label, color, stars }) => (
                      <Grid item xs={6} sm={3} key={value}>
                        <Button
                          id={`sq-btn-${value.toLowerCase()}`}
                          fullWidth
                          variant={serviceQuality === value ? 'contained' : 'outlined'}
                          onClick={() => { setServiceQuality(value); setTipSaved(false); }}
                          sx={{
                            py: 1,
                            flexDirection: 'column',
                            gap: 0.3,
                            borderColor: serviceQuality === value ? color : 'rgba(255,255,255,0.1)',
                            background: serviceQuality === value ? `${color}22` : 'transparent',
                            color: serviceQuality === value ? color : '#9AA0A6',
                            '&:hover': { borderColor: color, background: `${color}15` },
                            transition: 'all 0.2s ease'
                          }}
                        >
                          <Typography variant="caption" sx={{ fontSize: '0.65rem', letterSpacing: 0.5, lineHeight: 1 }}>
                            {stars}
                          </Typography>
                          <Typography variant="caption" sx={{ fontWeight: 600, fontSize: '0.72rem' }}>
                            {label}
                          </Typography>
                        </Button>
                      </Grid>
                    ))}
                  </Grid>
                </Box>

                {/* Day 9: Optimal Tip Timing Card */}
                {timingResult && !tipSaved && (
                  <Box
                    sx={{
                      p: 2,
                      mb: 2,
                      background: 'rgba(255, 255, 255, 0.05)',
                      borderRadius: 2,
                      border: '1px solid rgba(255,255,255,0.1)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 1
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TimeIcon sx={{ color: timingResult.state === 'RECOMMENDED' ? '#6C63FF' : '#00D9FF', fontSize: 20 }} />
                      <Typography variant="subtitle2" sx={{ color: '#E8EAED', fontWeight: 600 }}>
                        BEST TIME TO RECORD
                      </Typography>
                    </Box>
                    <Typography variant="body2" sx={{ color: '#9AA0A6' }}>
                      {timingResult.message}
                    </Typography>
                    {timingResult.state === 'RECOMMENDED' && timingResult.hasRestaurantHistory && (
                      <Box sx={{ mt: 1, p: 1.5, background: 'rgba(0,0,0,0.2)', borderRadius: 1 }}>
                        <Typography variant="caption" sx={{ color: '#00D9FF', display: 'block' }}>
                          Last tip at {timingResult.restaurantName}: {timingResult.lastTipPercentage}%
                        </Typography>
                        <Typography variant="caption" sx={{ color: '#9AA0A6' }}>
                          {timingResult.reason}
                        </Typography>
                      </Box>
                    )}
                  </Box>
                )}

                <Button
                  fullWidth
                  variant="contained"
                  size="large"
                  startIcon={saving ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                  onClick={handleSaveTip}
                  disabled={saving || !billAmount || parseFloat(billAmount) <= 0 || !serviceQuality}
                  sx={{ 
                    py: 1.5,
                    fontSize: '1.1rem',
                    background: 'linear-gradient(135deg, #6C63FF 0%, #4A42D4 100%)',
                    boxShadow: '0 8px 24px rgba(108, 99, 255, 0.4)'
                  }}
                >
                  Save Tip
                </Button>
              </CardContent>
            </Card>

            {/* Your Tipping Pattern Card (Day 4) */}
            {personalization && personalization.tipCount > 0 && (
              <Card
                sx={{
                  background: 'rgba(18, 24, 41, 0.7)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid rgba(108, 99, 255, 0.2)',
                  borderRadius: 3,
                  mb: 4
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                     <TrendingUpIcon sx={{ color: '#6C63FF' }} />
                     <Typography variant="subtitle1" sx={{ color: '#6C63FF', fontWeight: 700 }}>
                        YOUR TIPPING PATTERN
                     </Typography>
                  </Box>
                  
                  <Typography variant="body2" sx={{ color: '#E8EAED', mb: 2 }}>
                     {personalization.message}
                  </Typography>

                  <Grid container spacing={2} sx={{ mb: 2 }}>
                    <Grid item xs={4}>
                      <Box sx={{ p: 1.5, background: 'rgba(0,0,0,0.2)', borderRadius: 2, textAlign: 'center' }}>
                        <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Median</Typography>
                        <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 600 }}>{personalization.medianTipPercentage}%</Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={4}>
                      <Box sx={{ p: 1.5, background: 'rgba(0,0,0,0.2)', borderRadius: 2, textAlign: 'center' }}>
                        <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Tips</Typography>
                        <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 600 }}>{personalization.tipCount}</Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={4}>
                      <Box sx={{ p: 1.5, background: 'rgba(0,0,0,0.2)', borderRadius: 2, textAlign: 'center' }}>
                        <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Range</Typography>
                        <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 600 }}>{personalization.minimumTipPercentage}–{personalization.maximumTipPercentage}%</Typography>
                      </Box>
                    </Grid>
                  </Grid>

                  {/* Restaurant-specific insight */}
                  {restaurantInsight && restaurantInsight.visitCount > 0 && (
                    <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                      <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block', mb: 0.5 }}>
                        {restaurantInsight.restaurantName}
                      </Typography>
                      <Typography variant="body2" sx={{ color: '#E8EAED' }}>
                        {restaurantInsight.visitCount} visit{restaurantInsight.visitCount !== 1 ? 's' : ''} • Avg {restaurantInsight.averageTipPercentage}%
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#00D9FF' }}>
                        Last tip: {restaurantInsight.lastTipPercentage}% ({formatCurrency(restaurantInsight.lastTipAmount)})
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            )}

            {/* AI Recommendation Result Card */}
            {aiResult && (
              <Card
                sx={{
                  background: 'rgba(18, 24, 41, 0.7)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid #00D9FF',
                  borderRadius: 3,
                  mb: 4,
                  boxShadow: '0 4px 20px rgba(0, 217, 255, 0.2)'
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                     <SparkleIcon sx={{ color: '#00D9FF' }} />
                     <Typography variant="subtitle1" sx={{ color: '#00D9FF', fontWeight: 700 }}>
                        AI TIP RECOMMENDATION
                     </Typography>
                  </Box>
                  
                  <Typography variant="h3" sx={{ color: '#fff', fontWeight: 800, mb: 0.5 }}>
                     {aiResult.recommendedPercentage}%
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#9AA0A6', mb: 2 }}>
                     Suggested range: {aiResult.minimumPercentage}–{aiResult.maximumPercentage}%
                  </Typography>

                  {/* Show user's usual tip alongside AI recommendation */}
                  {personalization && personalization.tipCount >= 2 && (
                    <Typography variant="body2" sx={{ color: '#6C63FF', mb: 1 }}>
                      Your usual tip: {personalization.personalizedPercentage}%
                    </Typography>
                  )}
                  
                  <Typography variant="body1" sx={{ color: '#E8EAED', mb: 3, fontStyle: 'italic' }}>
                     "{aiResult.reason}"
                  </Typography>

                  {/* Day 7: Show backend-calculated confidence and source */}
                  <Box sx={{ display: 'flex', gap: 3, mb: 3, p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                      <Box>
                          <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Confidence</Typography>
                          <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 600 }}>
                              {aiResult.confidence}
                          </Typography>
                      </Box>
                      <Box>
                          <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Source</Typography>
                          <Typography variant="subtitle2" sx={{ color: '#fff', fontWeight: 600 }}>
                              {aiResult.personalizationSource ? aiResult.personalizationSource.replace(/_/g, ' ') : 'NONE'}
                          </Typography>
                      </Box>
                  </Box>
                  
                  <Box sx={{ display: 'flex', gap: 3, mb: 3 }}>
                      <Box>
                          <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Tip Amount</Typography>
                          <Typography variant="subtitle1" sx={{ color: '#fff', fontWeight: 600 }}>
                              {formatCurrency(aiResult.tipAmount)}
                          </Typography>
                      </Box>
                      <Box>
                          <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Total Amount</Typography>
                          <Typography variant="subtitle1" sx={{ color: '#fff', fontWeight: 600 }}>
                              {formatCurrency(aiResult.totalAmount)}
                          </Typography>
                      </Box>
                  </Box>

                  <Button
                    fullWidth
                    variant="contained"
                    onClick={applyAiRecommendation}
                    sx={{ 
                      background: 'rgba(0, 217, 255, 0.1)',
                      color: '#00D9FF',
                      border: '1px solid rgba(0, 217, 255, 0.5)',
                      '&:hover': { background: 'rgba(0, 217, 255, 0.2)' }
                    }}
                  >
                    Apply {aiResult.recommendedPercentage}% Tip
                  </Button>
                </CardContent>
              </Card>
            )}

            {/* Day 6: Service Quality Stats Card */}
            {serviceQualityStats && serviceQualityStats.totalRatedTips > 0 && (
              <Card
                sx={{
                  background: 'rgba(18, 24, 41, 0.7)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid rgba(0, 217, 255, 0.15)',
                  borderRadius: 3,
                  mb: 4
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <Typography sx={{ fontSize: '1.1rem' }}>⭐</Typography>
                    <Typography variant="subtitle1" sx={{ color: '#00D9FF', fontWeight: 700 }}>
                      SERVICE QUALITY SUMMARY
                    </Typography>
                  </Box>

                  <Grid container spacing={1} sx={{ mb: 2 }}>
                    {[
                      { key: 'poorCount',      label: 'Poor',      color: '#FF5252' },
                      { key: 'averageCount',   label: 'Average',   color: '#FFA726' },
                      { key: 'goodCount',      label: 'Good',      color: '#66BB6A' },
                      { key: 'excellentCount', label: 'Excellent', color: '#00D9FF' },
                    ].map(({ key, label, color }) => (
                      <Grid item xs={3} key={key}>
                        <Box sx={{ p: 1, background: 'rgba(0,0,0,0.2)', borderRadius: 2, textAlign: 'center' }}>
                          <Typography variant="h6" sx={{ color, fontWeight: 700 }}>{serviceQualityStats[key]}</Typography>
                          <Typography variant="caption" sx={{ color: '#9AA0A6' }}>{label}</Typography>
                        </Box>
                      </Grid>
                    ))}
                  </Grid>

                  {serviceQualityStats.mostCommon && (
                    <Typography variant="body2" sx={{ color: '#E8EAED', mb: 1 }}>
                      Most common: <span style={{ color: '#00D9FF', fontWeight: 600 }}>
                        {serviceQualityStats.mostCommon.charAt(0) + serviceQualityStats.mostCommon.slice(1).toLowerCase()}
                      </span>
                    </Typography>
                  )}

                  {serviceQualityStats.averageTipPercentageByQuality && Object.keys(serviceQualityStats.averageTipPercentageByQuality).length > 0 && (
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block', mb: 0.5 }}>Avg tip by quality:</Typography>
                      {Object.entries(serviceQualityStats.averageTipPercentageByQuality).map(([q, avg]) => (
                        <Typography key={q} variant="caption" sx={{ display: 'block', color: '#E8EAED' }}>
                          {q.charAt(0) + q.slice(1).toLowerCase()}: <span style={{ color: '#00D9FF' }}>{avg}%</span>
                        </Typography>
                      ))}
                    </Box>
                  )}
                </CardContent>
              </Card>
            )}

            {serviceQualityStats && serviceQualityStats.totalRatedTips === 0 && (
              <Card
                sx={{
                  background: 'rgba(18, 24, 41, 0.5)',
                  border: '1px solid rgba(255,255,255,0.05)',
                  borderRadius: 3,
                  mb: 4
                }}
              >
                <CardContent sx={{ p: 3, textAlign: 'center' }}>
                  <Typography variant="body2" sx={{ color: '#9AA0A6', fontStyle: 'italic' }}>
                    ⭐ No service ratings yet.
                  </Typography>
                </CardContent>
              </Card>
            )}

            {/* History Section */}
            <Box>
                <Typography variant="h6" sx={{ color: '#E8EAED', mb: 2, fontWeight: 600 }}>
                  Recent Tips
                </Typography>
                
                {loadingHistory && history.length === 0 ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                        <CircularProgress sx={{ color: '#6C63FF' }} />
                    </Box>
                ) : history.length === 0 ? (
                    <Typography variant="body2" sx={{ color: '#9AA0A6', fontStyle: 'italic', p: 2, textAlign: 'center' }}>
                        No saved tips yet.
                    </Typography>
                ) : (
                    <List sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        {history.map(tip => (
                            <ListItem 
                              key={tip.id}
                              sx={{ 
                                  background: 'rgba(255,255,255,0.03)', 
                                  borderRadius: 2,
                                  border: '1px solid rgba(255,255,255,0.05)'
                              }}
                            >
                                <ListItemText 
                                    primary={
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <Typography variant="subtitle1" sx={{ fontWeight: 600, color: '#fff' }}>
                                                {formatCurrency(tip.totalAmount, tip.currency)}
                                            </Typography>
                                            <Typography variant="body2" sx={{ color: '#00D9FF' }}>
                                                {tip.tipPercentage}% Tip
                                            </Typography>
                                        </Box>
                                    }
                                    secondary={
                                        <Box sx={{ mt: 0.5 }}>
                                            {tip.restaurantName && (
                                                <Typography variant="caption" sx={{ display: 'block', color: '#9AA0A6' }}>
                                                    📍 {tip.restaurantName}
                                                </Typography>
                                            )}
                                            {/* Day 6: Service Quality badge */}
                                            {tip.serviceQuality && (() => {
                                              const sqMeta = {
                                                POOR:      { label: 'Poor',      color: '#FF5252', stars: '★' },
                                                AVERAGE:   { label: 'Average',   color: '#FFA726', stars: '★★' },
                                                GOOD:      { label: 'Good',      color: '#66BB6A', stars: '★★★' },
                                                EXCELLENT: { label: 'Excellent', color: '#00D9FF', stars: '★★★★' },
                                              }[tip.serviceQuality];
                                              return sqMeta ? (
                                                <Typography variant="caption" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.4, color: sqMeta.color, mt: 0.25 }}>
                                                  {sqMeta.stars} {sqMeta.label} service
                                                </Typography>
                                              ) : null;
                                            })()}
                                            <Typography variant="caption" sx={{ display: 'block', color: 'rgba(255,255,255,0.4)', mt: 0.25 }}>
                                                {new Date(tip.createdAt).toLocaleDateString()}
                                            </Typography>
                                        </Box>
                                    }
                                />
                                <ListItemSecondaryAction>
                                    <IconButton 
                                        edge="end" 
                                        aria-label="delete"
                                        onClick={() => { setTipToDelete(tip.id); setDeleteDialogOpen(true); }}
                                        sx={{ color: '#FF5252', opacity: 0.7, '&:hover': { opacity: 1 } }}
                                    >
                                        <DeleteIcon />
                                    </IconButton>
                                </ListItemSecondaryAction>
                            </ListItem>
                        ))}
                    </List>
                )}
                
                {hasMore && (
                    <Button 
                      fullWidth 
                      variant="text" 
                      onClick={() => loadHistory(page + 1, true)}
                      disabled={loadingHistory}
                      sx={{ mt: 2, color: '#6C63FF' }}
                    >
                        Load More
                    </Button>
                )}
            </Box>
          </Grid>
        </Grid>
      </Box>

      {/* Snackbar for feedback */}
      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })}>
        <Alert severity={snackbar.severity} variant="filled" onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>

      {/* Delete Confirmation Dialog */}
      <Dialog 
        open={deleteDialogOpen} 
        onClose={() => setDeleteDialogOpen(false)}
        PaperProps={{
            sx: { background: '#121829', border: '1px solid rgba(255,255,255,0.1)', color: '#fff' }
        }}
      >
        <DialogTitle>Delete Tip?</DialogTitle>
        <DialogContent>
           <Typography variant="body2" sx={{ color: '#9AA0A6' }}>
               Are you sure you want to delete this tip? This action cannot be undone.
           </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} sx={{ color: '#9AA0A6' }}>Cancel</Button>
          <Button onClick={handleDeleteTip} color="error" variant="contained">Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TipCalculatorPage;
