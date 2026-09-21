import React, { useState, useEffect } from 'react';
import { Box, Typography, Grid, Card, CardContent, CircularProgress, LinearProgress, Chip, Button } from '@mui/material';
import { motion } from 'framer-motion';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import StarIcon from '@mui/icons-material/Star';
import CollectionIcon from '@mui/icons-material/CollectionsBookmark';
import MasterIcon from '@mui/icons-material/WorkspacePremium';
import LegendIcon from '@mui/icons-material/MilitaryTech';
import VisitIcon from '@mui/icons-material/DirectionsWalk';
import RegularIcon from '@mui/icons-material/Storefront';
import ExploreIcon from '@mui/icons-material/Explore';
import FoodExploreIcon from '@mui/icons-material/Restaurant';
import CurrencyIcon from '@mui/icons-material/AttachMoney';
import ObserveIcon from '@mui/icons-material/Visibility';
import PersonalIcon from '@mui/icons-material/Person';
import BudgetIcon from '@mui/icons-material/AccountBalanceWallet';
import TrackIcon from '@mui/icons-material/Timeline';
import TeamIcon from '@mui/icons-material/Groups';
import PoolMasterIcon from '@mui/icons-material/Diversity3';
import ReceiptIcon from '@mui/icons-material/Receipt';
import ReconcileIcon from '@mui/icons-material/FactCheck';
import DataIcon from '@mui/icons-material/Analytics';
import InsightIcon from '@mui/icons-material/TipsAndUpdates';
import RefreshIcon from '@mui/icons-material/Refresh';
import achievementApi from '../api/achievementApi';

const getIconComponent = (iconName, isUnlocked) => {
  const props = {
    sx: {
      fontSize: 38,
      color: isUnlocked ? '#fd5b38' : '#64748B',
      filter: isUnlocked ? 'drop-shadow(0 0 12px rgba(253, 91, 56, 0.6))' : 'none',
      mb: 1.5,
    },
  };

  switch (iconName) {
    case 'StarIcon': return <StarIcon {...props} />;
    case 'CollectionIcon': return <CollectionIcon {...props} />;
    case 'MasterIcon': return <MasterIcon {...props} />;
    case 'LegendIcon': return <LegendIcon {...props} />;
    case 'VisitIcon': return <VisitIcon {...props} />;
    case 'RegularIcon': return <RegularIcon {...props} />;
    case 'ExploreIcon': return <ExploreIcon {...props} />;
    case 'FoodExploreIcon': return <FoodExploreIcon {...props} />;
    case 'CurrencyIcon': return <CurrencyIcon {...props} />;
    case 'ObserveIcon': return <ObserveIcon {...props} />;
    case 'PersonalIcon': return <PersonalIcon {...props} />;
    case 'BudgetIcon': return <BudgetIcon {...props} />;
    case 'TrackIcon': return <TrackIcon {...props} />;
    case 'TeamIcon': return <TeamIcon {...props} />;
    case 'PoolMasterIcon': return <PoolMasterIcon {...props} />;
    case 'ReceiptIcon': return <ReceiptIcon {...props} />;
    case 'ReconcileIcon': return <ReconcileIcon {...props} />;
    case 'DataIcon': return <DataIcon {...props} />;
    case 'InsightIcon': return <InsightIcon {...props} />;
    default: return <EmojiEventsIcon {...props} />;
  }
};

const AchievementsPage = () => {
  const [achievements, setAchievements] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      try {
        await achievementApi.evaluateAchievements();
      } catch (e) {
        console.warn('Evaluation skipped or non-fatal', e);
      }

      const [achs, sum] = await Promise.all([
        achievementApi.getAchievements(),
        achievementApi.getAchievementSummary(),
      ]);
      setAchievements(achs || []);
      setSummary(sum || null);
    } catch (err) {
      console.error(err);
      setError('Unable to synchronise achievement telemetry.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', p: 12 }}>
        <CircularProgress sx={{ color: '#fd5b38', mb: 2 }} />
        <Typography sx={{ color: '#94A3B8', fontSize: '0.85rem', fontWeight: 600 }}>
          Synchronising Prestige Badges...
        </Typography>
      </Box>
    );
  }

  if (error && achievements.length === 0) {
    return (
      <Box sx={{ maxWidth: 600, mx: 'auto', p: 6, textAlign: 'center' }}>
        <Box
          className="bevel-card"
          sx={{
            p: 4,
            border: '1px solid rgba(251, 113, 133, 0.3)',
          }}
        >
          <EmojiEventsIcon sx={{ fontSize: 48, color: '#FB7185', mb: 2 }} />
          <Typography variant="h5" sx={{ color: '#F8FAFC', fontWeight: 800, mb: 1 }}>
            Telemetry Link Offline
          </Typography>
          <Typography variant="body2" sx={{ color: '#94A3B8', mb: 3 }}>
            {error} Verify your authentication status or reconnect to the neural engine.
          </Typography>
          <Button
            variant="contained"
            startIcon={<RefreshIcon />}
            onClick={loadData}
            sx={{ background: 'linear-gradient(135deg, #fd5b38 0%, #d63d19 100%)', color: '#fff', fontWeight: 700, px: 3, py: 1 }}
          >
            Retry Synchronisation
          </Button>
        </Box>
      </Box>
    );
  }

  return (
    <Box
      component={motion.div}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      sx={{ px: { xs: 2, sm: 4, md: 6, lg: 8 }, py: { xs: 4, md: 6 }, width: '100%', maxWidth: '100%' }}
    >
      {/* Header */}
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
          <EmojiEventsIcon sx={{ fontSize: 24, color: '#FFFFFF' }} />
        </Box>
        <Box>
          <Typography className="kicker">PRESTIGE MILESTONES</Typography>
          <Typography variant="h4" sx={{ fontWeight: 800, color: '#F8FAFC', letterSpacing: '-0.025em' }}>
            Executive Achievements
          </Typography>
        </Box>
      </Box>

      {/* Overview Card */}
      {summary && (
        <Card className="webfolio-card" sx={{ mb: 4.5, p: { xs: 3, md: 4 }, border: '1px solid rgba(253, 91, 56, 0.3)', background: 'linear-gradient(145deg, rgba(20, 24, 41, 0.95), rgba(10, 13, 23, 0.98))' }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1.2fr 1fr' }, gap: 4, alignItems: 'center' }}>
            <Box>
              <Typography className="kicker" sx={{ mb: 0.5 }}>COMPLETION VELOCITY</Typography>
              <Typography variant="h3" className="tabular-num" sx={{ fontWeight: 800, color: '#F8FAFC' }}>
                {summary.unlockedAchievements}{' '}
                <span style={{ fontSize: '1.4rem', color: '#64748B', fontWeight: 600 }}>
                  / {summary.totalAchievements} Unlocked
                </span>
              </Typography>
              <Typography variant="body2" sx={{ color: '#94A3B8', mt: 0.5 }}>
                Mastery rating: <b style={{ color: '#fd5b38' }}>{summary.completionPercentage}%</b> across all tipping domains.
              </Typography>
            </Box>

            <Box sx={{ width: '100%' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="caption" sx={{ color: '#94A3B8', fontWeight: 700 }}>PRESTIGE METER</Typography>
                <Typography className="tabular-num" variant="caption" sx={{ color: '#fd5b38', fontWeight: 800 }}>
                  {summary.completionPercentage}%
                </Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={parseFloat(summary.completionPercentage) || 0}
                sx={{
                  height: 8,
                  borderRadius: 4,
                  backgroundColor: 'rgba(255,255,255,0.06)',
                  '& .MuiLinearProgress-bar': {
                    background: 'linear-gradient(90deg, #fd5b38 0%, #ff8566 100%)',
                  },
                }}
              />
            </Box>
          </Box>
        </Card>
      )}

      {/* Badge Grid */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' },
          gap: 3,
        }}
      >
        {achievements.map((ach, index) => (
          <Box key={ach.id}>
            <Card
              component={motion.div}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: Math.min(index * 0.03, 0.4) }}
              className={ach.unlocked ? 'bevel-card' : ''}
              sx={{
                height: '100%',
                background: ach.unlocked
                  ? 'linear-gradient(145deg, rgba(24, 28, 44, 0.95), rgba(13, 15, 23, 0.98))'
                  : 'rgba(13, 15, 23, 0.5)',
                backdropFilter: 'blur(16px)',
                border: ach.unlocked
                  ? '1px solid rgba(253, 91, 56, 0.3)'
                  : '1px solid rgba(255, 255, 255, 0.05)',
                boxShadow: ach.unlocked
                  ? 'inset 0 1px 0 0 rgba(253, 91, 56, 0.2), 0 8px 30px rgba(0, 0, 0, 0.6)'
                  : 'none',
                borderRadius: 3.5,
                position: 'relative',
                overflow: 'hidden',
                transition: 'all 0.3s ease',
                '&:hover': {
                  transform: 'translateY(-3px)',
                  borderColor: ach.unlocked ? '#fd5b38' : 'rgba(255, 255, 255, 0.12)',
                },
              }}
            >
              {ach.unlocked && (
                <Box
                  sx={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    height: 2,
                    background: 'linear-gradient(90deg, transparent, #fd5b38, transparent)',
                  }}
                />
              )}
              <CardContent sx={{ p: 3, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', height: '100%' }}>
                {getIconComponent(ach.icon, ach.unlocked)}

                <Chip
                  label={ach.category}
                  size="small"
                  sx={{
                    mb: 1.5,
                    fontSize: '0.65rem',
                    fontWeight: 800,
                    backgroundColor: ach.unlocked ? 'rgba(253, 91, 56, 0.15)' : 'rgba(255, 255, 255, 0.04)',
                    color: ach.unlocked ? '#fd5b38' : '#64748B',
                    border: ach.unlocked ? '1px solid rgba(253, 91, 56, 0.25)' : '1px solid rgba(255, 255, 255, 0.05)',
                  }}
                />

                <Typography variant="h6" sx={{ color: ach.unlocked ? '#F8FAFC' : '#94A3B8', fontWeight: 800, mb: 1, fontSize: '1rem' }}>
                  {ach.name}
                </Typography>

                <Typography variant="body2" sx={{ color: ach.unlocked ? '#CBD5E1' : '#64748B', mb: 3, flexGrow: 1, fontSize: '0.82rem', lineHeight: 1.5 }}>
                  {ach.description}
                </Typography>

                <Box sx={{ width: '100%' }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.6 }}>
                    <Typography variant="caption" sx={{ color: '#64748B', fontSize: '0.7rem' }}>
                      {ach.unlocked ? 'Achievement Unlocked' : 'Progress'}
                    </Typography>
                    <Typography className="tabular-num" variant="caption" sx={{ color: ach.unlocked ? '#fd5b38' : '#94A3B8', fontWeight: 700 }}>
                      {ach.progress} / {ach.requirement}
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(100, (ach.progress / ach.requirement) * 100)}
                    sx={{
                      height: 5,
                      borderRadius: 3,
                      backgroundColor: 'rgba(255,255,255,0.05)',
                      '& .MuiLinearProgress-bar': {
                        background: ach.unlocked ? 'linear-gradient(90deg, #fd5b38, #ff8566)' : '#334155',
                      },
                    }}
                  />
                  {ach.unlocked && ach.unlockedAt && (
                    <Typography variant="caption" sx={{ display: 'block', mt: 1, color: '#fd5b38', fontWeight: 600, fontSize: '0.7rem' }}>
                      UNLOCKED {new Date(ach.unlockedAt).toLocaleDateString()}
                    </Typography>
                  )}
                </Box>
              </CardContent>
            </Card>
          </Box>
        ))}
      </Box>
    </Box>
  );
};

export default AchievementsPage;
