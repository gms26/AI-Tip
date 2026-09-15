import React, { useState, useEffect, useRef } from 'react';
import {
  Dialog,
  Box,
  Typography,
  InputBase,
  Chip,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Search as SearchIcon,
  Receipt as ReceiptIcon,
  Dashboard as DashboardIcon,
  History as HistoryIcon,
  AutoAwesome as AutoAwesomeIcon,
  Lightbulb as LightbulbIcon,
  CompareArrows as CompareIcon,
  Timeline as TimelineIcon,
  BarChart as BarChartIcon,
  TrendingUp as TrendingUpIcon,
  Star as StarIcon,
  EmojiEvents as EmojiEventsIcon,
  Groups as GroupsIcon,
  AssignmentInd as AssignmentIndIcon,
  HealthAndSafety as HealthAndSafetyIcon,
  Insights as InsightsIcon,
  ArrowForward as ArrowForwardIcon,
  Analytics as AnalyticsIcon,
  PieChart as PieChartIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

const SEARCH_ITEMS = [
  {
    title: 'Smart Tip Calculator',
    description: 'Calculate gratuity, split bill with friends, or scan receipts',
    path: '/tips',
    category: 'Core Tools',
    icon: <ReceiptIcon sx={{ color: '#fd5b38' }} />,
    badge: 'Primary',
  },
  {
    title: 'Executive Dashboard',
    description: 'Overview of tipping activity, quick stats and recent actions',
    path: '/dashboard',
    category: 'Core Tools',
    icon: <DashboardIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Gratuity Ledger History',
    description: 'Browse past tips, receipts, filtered logs, and export data',
    path: '/tip-history',
    category: 'Core Tools',
    icon: <HistoryIcon sx={{ color: '#94A3B8' }} />,
  },
  {
    title: 'Smart Coach',
    description: 'AI personalised dining feedback and gratuity coaching',
    path: '/coach',
    category: 'AI Intelligence',
    icon: <AutoAwesomeIcon sx={{ color: '#fd5b38' }} />,
    badge: 'AI',
  },
  {
    title: 'AI Recommendations',
    description: 'Context-aware tips tailored to venue, cuisine, and service level',
    path: '/recommendations',
    category: 'AI Intelligence',
    icon: <LightbulbIcon sx={{ color: '#fd5b38' }} />,
    badge: 'AI',
  },
  {
    title: 'What-If Scenarios',
    description: 'Simulate bill totals, service adjustments, and party splits',
    path: '/tip-scenarios',
    category: 'AI Intelligence',
    icon: <CompareIcon sx={{ color: '#fd5b38' }} />,
    badge: 'Simulation',
  },
  {
    title: 'Analytics & Insights',
    description: 'Deep financial metrics, heatmaps, spending charts, and trends',
    path: '/analytics',
    category: 'Analytics & Goals',
    icon: <BarChartIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Tip Forecast',
    description: 'Predictive expenditure modelling and seasonal trend analysis',
    path: '/tip-forecast',
    category: 'Analytics & Goals',
    icon: <TrendingUpIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Smart Goals',
    description: 'Monthly tipping budgets, savings goals & target tracking',
    path: '/tip-goals',
    category: 'Analytics & Goals',
    icon: <StarIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Hospitality Insights',
    description: 'Industry averages, dining trends, and hospitality benchmarks',
    path: '/tip-insights',
    category: 'Analytics & Goals',
    icon: <InsightsIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Achievements',
    description: 'Unlock milestones, dining badges, and tipping streaks',
    path: '/achievements',
    category: 'Analytics & Goals',
    icon: <EmojiEventsIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Tip Pools',
    description: 'Staff tip pooling, team shifts, hours-worked distributions',
    path: '/tip-pools',
    category: 'Workspace',
    icon: <GroupsIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Tipping Profile',
    description: 'Dining preferences, behavioural habits, and customised settings',
    path: '/tip-profile',
    category: 'Workspace',
    icon: <AssignmentIndIcon sx={{ color: '#fd5b38' }} />,
  },
  {
    title: 'Data Quality & Health',
    description: 'Data health checks, anomaly detection, and sync audits',
    path: '/data-quality',
    category: 'Workspace',
    icon: <HealthAndSafetyIcon sx={{ color: '#fd5b38' }} />,
  },
];

const CommandPalette = ({ open, onClose }) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const navigate = useNavigate();
  const inputRef = useRef(null);

  useEffect(() => {
    if (open) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
        }
      }, 50);
    }
  }, [open]);

  const filteredItems = SEARCH_ITEMS.filter((item) => {
    const q = query.toLowerCase().trim();
    if (!q) return true;
    return (
      item.title.toLowerCase().includes(q) ||
      item.description.toLowerCase().includes(q) ||
      item.category.toLowerCase().includes(q)
    );
  });

  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  const handleSelect = (item) => {
    onClose();
    navigate(item.path);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % Math.max(1, filteredItems.length));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) =>
        prev - 1 < 0 ? filteredItems.length - 1 : prev - 1
      );
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (filteredItems[selectedIndex]) {
        handleSelect(filteredItems[selectedIndex]);
      }
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          backgroundColor: '#0a0d17',
          backgroundImage: 'radial-gradient(ellipse at top right, rgba(253, 91, 56, 0.12), transparent 70%)',
          border: '1px solid rgba(253, 91, 56, 0.25)',
          borderRadius: 3.5,
          boxShadow: 'inset 0 1px 0 0 rgba(255, 255, 255, 0.15), 0 32px 80px rgba(0, 0, 0, 0.9)',
          overflow: 'hidden',
          top: -60,
        },
      }}
    >
      {/* Search Input Bar */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.8,
          px: 2.8,
          py: 2.2,
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        <SearchIcon sx={{ color: '#fd5b38', fontSize: 22 }} />
        <InputBase
          inputRef={inputRef}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Search features, tools, or AI models..."
          sx={{
            flexGrow: 1,
            color: '#F8FAFC',
            fontSize: '1rem',
            fontWeight: 600,
            '& input::placeholder': {
              color: '#64748B',
              opacity: 1,
            },
          }}
        />
        <Chip
          label="ESC"
          size="small"
          onClick={onClose}
          sx={{
            cursor: 'pointer',
            backgroundColor: 'rgba(255, 255, 255, 0.06)',
            color: '#94A3B8',
            fontSize: '0.68rem',
            fontWeight: 700,
            border: '1px solid rgba(255, 255, 255, 0.1)',
            height: 22,
          }}
        />
      </Box>

      {/* Results List */}
      <List
        sx={{
          maxHeight: 380,
          overflowY: 'auto',
          py: 1,
          px: 1,
        }}
      >
        {filteredItems.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center', color: '#64748B' }}>
            <Typography variant="body2">No matching features found for "{query}"</Typography>
          </Box>
        ) : (
          filteredItems.map((item, idx) => {
            const isSelected = idx === selectedIndex;
            return (
              <ListItemButton
                key={item.path}
                onClick={() => handleSelect(item)}
                onMouseEnter={() => setSelectedIndex(idx)}
                sx={{
                  borderRadius: 2.5,
                  py: 1.4,
                  px: 1.8,
                  mb: 0.5,
                  backgroundColor: isSelected ? 'rgba(253, 91, 56, 0.12)' : 'transparent',
                  border: isSelected ? '1px solid rgba(253, 91, 56, 0.3)' : '1px solid transparent',
                  borderLeft: isSelected ? '3px solid #fd5b38' : '1px solid transparent',
                  transition: 'all 0.15s ease',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.6,
                }}
              >
                <ListItemIcon
                  sx={{
                    minWidth: 38,
                    height: 38,
                    borderRadius: 2.5,
                    backgroundColor: 'rgba(255, 255, 255, 0.04)',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography
                        variant="body2"
                        sx={{
                          fontWeight: isSelected ? 700 : 600,
                          color: isSelected ? '#F8FAFC' : '#CBD5E1',
                        }}
                      >
                        {item.title}
                      </Typography>
                      {item.badge && (
                        <Chip
                          label={item.badge}
                          size="small"
                          sx={{
                            height: 18,
                            fontSize: '0.62rem',
                            fontWeight: 800,
                            backgroundColor:
                              item.badge === 'Primary'
                                ? 'rgba(253, 91, 56, 0.2)'
                                : item.badge === 'AI'
                                ? 'rgba(129, 140, 248, 0.2)'
                                : 'rgba(56, 189, 248, 0.2)',
                            color:
                              item.badge === 'Primary'
                                ? '#fd5b38'
                                : item.badge === 'AI'
                                ? '#A5B4FC'
                                : '#7DD3FC',
                            border: 'none',
                          }}
                        />
                      )}
                      <Typography
                        variant="caption"
                        sx={{
                          ml: 'auto',
                          color: '#64748B',
                          fontSize: '0.725rem',
                          fontWeight: 600,
                        }}
                      >
                        {item.category}
                      </Typography>
                    </Box>
                  }
                  secondary={
                    <Typography
                      variant="caption"
                      sx={{
                        color: '#94A3B8',
                        fontSize: '0.75rem',
                        display: 'block',
                        mt: 0.25,
                      }}
                    >
                      {item.description}
                    </Typography>
                  }
                />
                {isSelected && (
                  <ArrowForwardIcon sx={{ color: '#fd5b38', fontSize: 16, ml: 1 }} />
                )}
              </ListItemButton>
            );
          })
        )}
      </List>

      {/* Footer Instructions */}
      <Box
        sx={{
          px: 2.8,
          py: 1.4,
          borderTop: '1px solid rgba(255, 255, 255, 0.06)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          color: '#64748B',
          fontSize: '0.75rem',
          fontWeight: 600,
          backgroundColor: 'rgba(0, 0, 0, 0.3)',
        }}
      >
        <Box sx={{ display: 'flex', gap: 2.5 }}>
          <span>Navigate <b style={{ color: '#fd5b38' }}>↑</b> <b style={{ color: '#fd5b38' }}>↓</b></span>
          <span>Select <b style={{ color: '#fd5b38' }}>↵</b></span>
        </Box>
        <span>PT · Possible Intelligence</span>
      </Box>
    </Dialog>
  );
};

export default CommandPalette;
