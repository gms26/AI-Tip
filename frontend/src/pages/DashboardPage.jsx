/**
 * Dashboard Page
 *
 * PURPOSE:
 * The authenticated user's home page. Displays welcome message,
 * user info, and a logout button. Day 1 scope — no tip features.
 *
 * FEATURES:
 * - Welcome message with user's name
 * - User info card (email, member since)
 * - Logout button
 * - Premium dark design with gradient accents
 *
 * WHY fetch user data from context?
 * The AuthContext already has user info from the JWT or
 * from the /api/users/me call on mount. No extra API call needed.
 */
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getGenerosityScore } from '../api/generosityApi';
import taxApi from '../api/taxApi';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Avatar,
  Chip,
  Divider,
  Stack,
  TextField,
  MenuItem,
} from '@mui/material';
import {
  Logout as LogoutIcon,
  AutoAwesome as SparkleIcon,
  Person as PersonIcon,
  Email as EmailIcon,
  Shield as ShieldIcon,
  Favorite as FavoriteIcon,
  AccountBalance as TaxIcon,
  InfoOutlined as InfoIcon,
} from '@mui/icons-material';

const DashboardPage = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  
  const [generosityData, setGenerosityData] = useState(null);
  const [loadingGenerosity, setLoadingGenerosity] = useState(true);

  // Day 10: Tax Tracking State
  const [taxData, setTaxData] = useState(null);
  const [taxLoading, setTaxLoading] = useState(true);
  const [taxPeriod, setTaxPeriod] = useState('CURRENT_YEAR');
  const [taxAssumption, setTaxAssumption] = useState(100);

  useEffect(() => {
    let mounted = true;
    const fetchGenerosity = async () => {
      try {
        setLoadingGenerosity(true);
        const data = await getGenerosityScore();
        if (mounted) {
          setGenerosityData(data);
        }
      } catch (err) {
        console.error('Failed to load generosity score:', err);
      } finally {
        if (mounted) setLoadingGenerosity(false);
      }
    };
    fetchGenerosity();
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    let mounted = true;
    const fetchTax = async () => {
      try {
        setTaxLoading(true);
        const data = await taxApi.getTaxSummary({
            period: taxPeriod,
            taxablePercentage: taxAssumption
        });
        if (mounted) {
            setTaxData(data);
        }
      } catch (err) {
        console.error('Failed to load tax summary:', err);
      } finally {
        if (mounted) setTaxLoading(false);
      }
    };
    
    const timeoutId = setTimeout(() => {
        if (taxAssumption >= 0 && taxAssumption <= 100) {
            fetchTax();
        }
    }, 500); 
    
    return () => { mounted = false; clearTimeout(timeoutId); };
  }, [taxPeriod, taxAssumption]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  /**
   * Generate initials for avatar.
   * WHY? More personal than a generic icon.
   */
  const getInitials = (name) => {
    if (!name) return '?';
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #0A0E1A 0%, #121829 50%, #0A0E1A 100%)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background decorations */}
      <Box
        sx={{
          position: 'absolute',
          width: 500,
          height: 500,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(253, 91, 56, 0.08) 0%, transparent 70%)',
          top: -200,
          right: -100,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          width: 400,
          height: 400,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(255, 138, 101, 0.06) 0%, transparent 70%)',
          bottom: -150,
          left: -100,
        }}
      />


      {/* Main Content */}
      <Box
        sx={{
          width: '100%',
          maxWidth: 1300,
          mx: 'auto',
          px: { xs: 2.5, sm: 3.5, md: 5 },
          pt: { xs: 3, md: 4 },
          pb: { xs: 4, md: 6 },
          position: 'relative',
          zIndex: 1,
        }}
      >
        {/* Welcome Section */}
        <Box sx={{ mb: 5 }}>
          <Typography
            variant="h3"
            sx={{
              fontWeight: 800,
              mb: 1,
              color: '#E8EAED',
              fontSize: { xs: '1.8rem', sm: '2.5rem' },
            }}
          >
            Welcome back,{' '}
            <Box
              component="span"
              sx={{
                background: 'linear-gradient(135deg, #fd5b38, #ff8a65)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              {user?.name || 'User'}
            </Box>
            ! 👋
          </Typography>
          <Typography variant="body1" sx={{ color: '#9AA0A6', fontSize: '1.05rem' }}>
            Your AI-powered tip assistant is ready. More features coming soon!
          </Typography>
        </Box>

        {/* User Profile Card */}
        <Card
          sx={{
            background: 'rgba(18, 24, 41, 0.7)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
            mb: 4,
          }}
        >
          <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 3,
                mb: 3,
                flexDirection: { xs: 'column', sm: 'row' },
                textAlign: { xs: 'center', sm: 'left' },
              }}
            >
              <Avatar
                sx={{
                  width: 72,
                  height: 72,
                  background: 'linear-gradient(135deg, #fd5b38 0%, #ff8a65 100%)',
                  fontSize: '1.5rem',
                  fontWeight: 700,
                  boxShadow: '0 4px 20px rgba(253, 91, 56, 0.3)',
                }}
              >
                {getInitials(user?.name)}
              </Avatar>
              <Box>
                <Typography variant="h5" sx={{ fontWeight: 700, color: '#E8EAED', mb: 0.5 }}>
                  {user?.name || 'User'}
                </Typography>
                <Chip
                  icon={<ShieldIcon sx={{ fontSize: 14 }} />}
                  label="Verified Account"
                  size="small"
                  sx={{
                    backgroundColor: 'rgba(0, 230, 118, 0.12)',
                    color: '#00E676',
                    fontWeight: 500,
                    fontSize: '0.75rem',
                    '& .MuiChip-icon': { color: '#00E676' },
                  }}
                />
              </Box>
            </Box>

            <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.06)', mb: 3 }} />

            <Stack spacing={2.5}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 40,
                    height: 40,
                    borderRadius: 1.5,
                    backgroundColor: 'rgba(253, 91, 56, 0.12)',
                  }}
                >
                  <PersonIcon sx={{ color: '#fd5b38', fontSize: 20 }} />
                </Box>
                <Box>
                  <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>
                    Full Name
                  </Typography>
                  <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>
                    {user?.name}
                  </Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 40,
                    height: 40,
                    borderRadius: 1.5,
                    backgroundColor: 'rgba(255, 138, 101, 0.12)',
                  }}
                >
                  <EmailIcon sx={{ color: '#ff8a65', fontSize: 20 }} />
                </Box>
                <Box>
                  <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>
                    Email Address
                  </Typography>
                  <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>
                    {user?.email}
                  </Typography>
                </Box>
              </Box>
            </Stack>
          </CardContent>
        </Card>

        {/* Generosity Score Card */}
        <Card
          sx={{
            background: 'rgba(18, 24, 41, 0.7)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
            mb: 4,
          }}
        >
          <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
              <FavoriteIcon sx={{ color: '#FF5252', fontSize: 28 }} />
              <Typography variant="h5" sx={{ fontWeight: 700, color: '#E8EAED' }}>
                Your Generosity Score
              </Typography>
            </Box>

            {loadingGenerosity ? (
              <Typography sx={{ color: '#9AA0A6' }}>Loading score...</Typography>
            ) : generosityData?.score === null || generosityData?.score === undefined ? (
              <Box>
                <Typography variant="h6" sx={{ color: '#E8EAED', mb: 1 }}>
                  Not enough history yet.
                </Typography>
                <Typography variant="body1" sx={{ color: '#9AA0A6' }}>
                  Create a few tips to see your tipping pattern.
                </Typography>
              </Box>
            ) : (
              <Box>
                <Typography
                  variant="h2"
                  sx={{
                    fontWeight: 800,
                    color: '#00E676',
                    mb: 1,
                  }}
                >
                  {generosityData.score} / 100
                </Typography>
                
                <Typography
                  variant="h6"
                  sx={{
                    fontWeight: 600,
                    color: '#ff8a65',
                    textTransform: 'uppercase',
                    letterSpacing: 1,
                    mb: 3,
                  }}
                >
                  {generosityData.category.replace('_', ' ')}
                </Typography>

                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 2 }}>
                  <Box>
                    <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>
                      FACTS
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>
                      Based on {generosityData.totalTips} tip{generosityData.totalTips !== 1 ? 's' : ''}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>
                      STATISTICS
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>
                      Median tip: {generosityData.medianTipPercentage}%
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>
                      SCORE
                    </Typography>
                    <Chip
                      label={`Confidence: ${generosityData.confidence}`}
                      size="small"
                      sx={{
                        backgroundColor:
                          generosityData.confidence === 'HIGH'
                            ? 'rgba(0, 230, 118, 0.12)'
                            : generosityData.confidence === 'MEDIUM'
                            ? 'rgba(255, 193, 7, 0.12)'
                            : 'rgba(255, 82, 82, 0.12)',
                        color:
                          generosityData.confidence === 'HIGH'
                            ? '#00E676'
                            : generosityData.confidence === 'MEDIUM'
                            ? '#FFC107'
                            : '#FF5252',
                        fontWeight: 600,
                      }}
                    />
                  </Box>
                </Box>
              </Box>
            )}
          </CardContent>
        </Card>

        {/* Day 10: Tax Tracking Card */}
        <Card
          sx={{
            background: 'rgba(18, 24, 41, 0.7)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
            mb: 4,
          }}
        >
          <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <TaxIcon sx={{ color: '#ff8a65', fontSize: 28 }} />
                <Typography variant="h5" sx={{ fontWeight: 700, color: '#E8EAED' }}>
                  Tax Impact & Tracking
                </Typography>
              </Box>
            </Box>

            <Box sx={{ display: 'flex', gap: 2, mb: 4, flexWrap: 'wrap' }}>
                <TextField
                  select
                  label="Period"
                  value={taxPeriod}
                  onChange={(e) => setTaxPeriod(e.target.value)}
                  sx={{ minWidth: 200 }}
                  size="small"
                >
                  <MenuItem value="CURRENT_MONTH">Current Month</MenuItem>
                  <MenuItem value="PREVIOUS_MONTH">Previous Month</MenuItem>
                  <MenuItem value="CURRENT_YEAR">Current Year</MenuItem>
                </TextField>
                
                <TextField
                  label="Estimated taxable-tip percentage"
                  type="number"
                  value={taxAssumption}
                  onChange={(e) => setTaxAssumption(Number(e.target.value))}
                  InputProps={{ inputProps: { min: 0, max: 100 } }}
                  sx={{ minWidth: 250 }}
                  size="small"
                />
            </Box>

            {taxLoading ? (
                <Typography sx={{ color: '#9AA0A6' }}>Loading tax data...</Typography>
            ) : taxData && taxData.tipCount === 0 ? (
                <Box>
                    <Typography variant="body1" sx={{ color: '#E8EAED' }}>
                        Tracked Tips: 0
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#9AA0A6' }}>
                        No tips found for this period.
                    </Typography>
                </Box>
            ) : taxData && taxData.currencyBreakdown && taxData.currencyBreakdown.length > 0 ? (
                <Box>
                    <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block', mb: 2 }}>
                        {taxData.startDate} to {taxData.endDate}
                    </Typography>

                    {/* Single Currency view vs Multi Currency view */}
                    {taxData.currencyBreakdown.length === 1 ? (
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
                            <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2, minWidth: 150 }}>
                                <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Tracked Tips</Typography>
                                <Typography variant="h5" sx={{ color: '#fff', fontWeight: 700 }}>
                                    {taxData.currencyBreakdown[0].currency} {taxData.totalTips.toFixed(2)}
                                </Typography>
                            </Box>
                            <Box sx={{ p: 2, background: 'rgba(253, 91, 56, 0.1)', border: '1px solid rgba(253, 91, 56, 0.3)', borderRadius: 2, minWidth: 150 }}>
                                <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Estimated Taxable</Typography>
                                <Typography variant="h5" sx={{ color: '#fd5b38', fontWeight: 700 }}>
                                    {taxData.currencyBreakdown[0].currency} {taxData.estimatedTaxableTips.toFixed(2)}
                                </Typography>
                                <Typography variant="caption" sx={{ color: '#fd5b38' }}>({taxData.taxablePercentage}% assumption)</Typography>
                            </Box>
                            <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                                <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Tips Recorded</Typography>
                                <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>{taxData.tipCount}</Typography>
                            </Box>
                            <Box sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2 }}>
                                <Typography variant="caption" sx={{ color: '#9AA0A6', display: 'block' }}>Average Tip</Typography>
                                <Typography variant="body1" sx={{ color: '#E8EAED', fontWeight: 500 }}>
                                    {taxData.currencyBreakdown[0].currency} {taxData.averageTip.toFixed(2)}
                                </Typography>
                            </Box>
                        </Box>
                    ) : (
                        <Box sx={{ mb: 3 }}>
                            <Typography variant="body1" sx={{ color: '#E8EAED', mb: 2 }}>
                                Currencies (Multiple currencies detected)
                            </Typography>
                            <Stack spacing={2}>
                                {taxData.currencyBreakdown.map((c) => (
                                    <Box key={c.currency} sx={{ p: 2, background: 'rgba(0,0,0,0.2)', borderRadius: 2, display: 'flex', gap: 3, flexWrap: 'wrap' }}>
                                        <Box>
                                            <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Currency</Typography>
                                            <Typography variant="h6" sx={{ color: '#fff' }}>{c.currency}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="caption" sx={{ color: '#9AA0A6' }}>Tracked</Typography>
                                            <Typography variant="h6" sx={{ color: '#fff' }}>{c.totalTips.toFixed(2)}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="caption" sx={{ color: '#fd5b38' }}>Taxable Est ({taxData.taxablePercentage}%)</Typography>
                                            <Typography variant="h6" sx={{ color: '#fd5b38' }}>{c.estimatedTaxableTips.toFixed(2)}</Typography>
                                        </Box>
                                    </Box>
                                ))}
                            </Stack>
                        </Box>
                    )}

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1.5, background: 'rgba(255, 255, 255, 0.05)', borderRadius: 2, mt: 2 }}>
                        <InfoIcon sx={{ color: '#9AA0A6', fontSize: 18 }} />
                        <Typography variant="caption" sx={{ color: '#9AA0A6' }}>
                            {taxData.disclaimer}
                        </Typography>
                    </Box>
                </Box>
            ) : null}
          </CardContent>
        </Card>

        {/* Tip Calculator Card */}
        <Card
          sx={{
            background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.15) 0%, rgba(255, 138, 101, 0.05) 100%)',
            backdropFilter: 'blur(12px)',
            border: '1px solid rgba(253, 91, 56, 0.3)',
            borderRadius: 3,
            transition: 'transform 0.2s, box-shadow 0.2s',
            cursor: 'pointer',
            '&:hover': {
                transform: 'translateY(-4px)',
                boxShadow: '0 12px 24px rgba(253, 91, 56, 0.2)',
            }
          }}
          onClick={() => navigate('/tips')}
        >
          <CardContent sx={{ py: 5, textAlign: 'center' }}>
            <SparkleIcon
              sx={{
                fontSize: 48,
                color: '#fd5b38',
                mb: 2,
              }}
            />
            <Typography variant="h5" sx={{ color: '#E8EAED', fontWeight: 700, mb: 1 }}>
              Open Tip Calculator
            </Typography>
            <Typography variant="body1" sx={{ color: '#9AA0A6', maxWidth: 400, mx: 'auto', mb: 3 }}>
              Calculate tips, split bills equally or custom, and save your history securely.
            </Typography>
            <Button 
                variant="contained" 
                size="large"
                onClick={(e) => { e.stopPropagation(); navigate('/tips'); }}
                sx={{ 
                    background: 'linear-gradient(135deg, #fd5b38 0%, #e04826 100%)',
                    borderRadius: 2,
                    px: 4
                }}
            >
                Launch Calculator
            </Button>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};

export default DashboardPage;
