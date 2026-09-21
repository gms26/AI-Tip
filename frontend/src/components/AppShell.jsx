import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Typography,
  IconButton,
  Avatar,
  Menu,
  MenuItem,
  Tooltip,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Divider,
  Chip,
} from '@mui/material';
import {
  AutoAwesome as SparkleIcon,
  Logout as LogoutIcon,
  Dashboard as DashboardIcon,
  Receipt as ReceiptIcon,
  Groups as GroupsIcon,
  BarChart as BarChartIcon,
  EmojiEvents as EmojiEventsIcon,
  History as HistoryIcon,
  Star as StarIcon,
  HealthAndSafety as HealthAndSafetyIcon,
  TrendingUp as TrendingUpIcon,
  CompareArrows as CompareIcon,
  Lightbulb as LightbulbIcon,
  AssignmentInd as AssignmentIndIcon,
  Timeline as TimelineIcon,
  KeyboardArrowDown as ArrowDownIcon,
  Search as SearchIcon,
  Menu as MenuIcon,
  Close as CloseIcon,
  NorthEast as ArrowUpRightIcon,
  Insights as InsightsIcon,
  ArrowUpward as ArrowUpwardIcon,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PtLogo from './PtLogo';
import { motion, AnimatePresence } from 'framer-motion';
import AchievementNotifier from './AchievementNotifier';
import CommandPalette from './CommandPalette';
import DynamicBackground from './DynamicBackground';

// Grouped navigation structures for Webfolio mega-dropdowns
const AI_MENU_ITEMS = [
  {
    label: 'Smart Coach',
    path: '/coach',
    description: 'Personalised AI gratuity coaching & habit analysis',
    icon: <SparkleIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
    badge: 'NEURAL',
  },
  {
    label: 'AI Recommendations',
    path: '/recommendations',
    description: 'Context-aware tips tailored to venue & service tier',
    icon: <LightbulbIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
    badge: 'AI',
  },
  {
    label: 'What-If Scenarios',
    path: '/tip-scenarios',
    description: 'Simulate bill amounts, party splits & quantum tipping',
    icon: <CompareIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Tip Evolution',
    path: '/tip-evolution',
    description: 'Autonomous learning memory & preference adaptation',
    icon: <TimelineIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
];

const ANALYTICS_MENU_ITEMS = [
  {
    label: 'Spend Analytics',
    path: '/analytics',
    description: 'Detailed spend velocity, distribution & payment breakdown',
    icon: <BarChartIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Tip Forecast',
    path: '/tip-forecast',
    description: 'Predictive 30-day forecasting & seasonal gratuity models',
    icon: <TrendingUpIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Smart Goals',
    path: '/tip-goals',
    description: 'Monthly gratuity ceiling & progressive target tracking',
    icon: <StarIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Hospitality Insights',
    path: '/tip-insights',
    description: 'Macro dining patterns & hospitality service benchmarks',
    icon: <InsightsIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Achievements',
    path: '/achievements',
    description: 'Executive milestones, streak records & prestige badges',
    icon: <EmojiEventsIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
];

const WORKSPACE_MENU_ITEMS = [
  {
    label: 'Tip Pools',
    path: '/tip-pools',
    description: 'Team shift splits & collaborative staff distribution',
    icon: <GroupsIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Tip Profile',
    path: '/tip-profile',
    description: 'Dining persona, default gratuity tier & currency ledger',
    icon: <AssignmentIndIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
  {
    label: 'Data Quality & Health',
    path: '/data-quality',
    description: '100% telemetry integrity & anomaly security validation',
    icon: <HealthAndSafetyIcon sx={{ fontSize: 18, color: '#fd5b38' }} />,
  },
];

/* ── Webfolio Upper Link Button Styling ── */
const navButtonSx = (isActive) => ({
  fontFamily: '"Plus Jakarta Sans", sans-serif',
  color: isActive ? '#fd5b38' : '#ffffff',
  backgroundColor: 'transparent',
  px: 1.8,
  py: 0.8,
  fontSize: '0.95rem',
  fontWeight: 700,
  textTransform: 'uppercase',
  letterSpacing: '1px',
  position: 'relative',
  transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
  '&::after': {
    content: '""',
    position: 'absolute',
    bottom: 2,
    left: '15%',
    right: '15%',
    height: '2px',
    backgroundColor: '#fd5b38',
    transform: isActive ? 'scaleX(1)' : 'scaleX(0)',
    transition: 'transform 0.25s ease',
  },
  '&:hover': {
    backgroundColor: 'transparent',
    color: '#fd5b38',
    '&::after': {
      transform: 'scaleX(1)',
    },
  },
});

/* ── Webfolio Dropdown Menu Renderer ── */
const renderMenuItems = (items, anchorEl, setAnchor, navigate, location) => (
  <Menu
    anchorEl={anchorEl}
    open={Boolean(anchorEl)}
    onClose={() => setAnchor(null)}
    TransitionProps={{ timeout: 200 }}
    PaperProps={{
      sx: {
        width: 340,
        p: 1.2,
        background: 'rgba(21, 22, 29, 0.98)',
        backdropFilter: 'blur(24px) saturate(1.8)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        boxShadow: '0 24px 64px -8px rgba(0, 0, 0, 0.85)',
        borderRadius: '20px',
      },
    }}
  >
    {items.map((item) => {
      const isSelected = location.pathname === item.path;
      return (
        <MenuItem
          key={item.path}
          onClick={() => {
            setAnchor(null);
            navigate(item.path);
          }}
          selected={isSelected}
          sx={{
            py: 1.2,
            px: 1.5,
            gap: 1.5,
            borderRadius: '12px',
            mb: 0.4,
            transition: 'all 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
            '&:hover': {
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              transform: 'translateX(3px)',
            },
            '&.Mui-selected': {
              backgroundColor: 'rgba(253, 91, 56, 0.12)',
              borderLeft: '3px solid #fd5b38',
            },
          }}
        >
          <Box sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 36,
            height: 36,
            borderRadius: '10px',
            backgroundColor: 'rgba(255, 255, 255, 0.04)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            flexShrink: 0,
          }}>
            {item.icon}
          </Box>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2" sx={{ fontWeight: 700, color: isSelected ? '#fd5b38' : '#ffffff' }}>
                {item.label}
              </Typography>
              {item.badge && (
                <Chip
                  label={item.badge}
                  size="small"
                  sx={{
                    height: 18,
                    fontSize: '0.62rem',
                    fontWeight: 800,
                    background: 'rgba(253, 91, 56, 0.18)',
                    color: '#fd5b38',
                    border: '1px solid rgba(253, 91, 56, 0.35)',
                    letterSpacing: '0.04em',
                  }}
                />
              )}
            </Box>
            <Typography variant="caption" sx={{ color: '#c6c8c9', display: 'block', fontSize: '0.72rem', lineHeight: 1.35, mt: 0.25 }}>
              {item.description}
            </Typography>
          </Box>
        </MenuItem>
      );
    })}
  </Menu>
);

const AppShell = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  // Menus state
  const [userMenuAnchor, setUserMenuAnchor] = useState(null);
  const [aiMenuAnchor, setAiMenuAnchor] = useState(null);
  const [analyticsMenuAnchor, setAnalyticsMenuAnchor] = useState(null);
  const [workspaceMenuAnchor, setWorkspaceMenuAnchor] = useState(null);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  // Scroll listener for floating navbar backdrop tightening & back-to-top button
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 40);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Global Keyboard Shortcut: Cmd/Ctrl + K for Command Palette
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setCommandPaletteOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleLogout = () => {
    setUserMenuAnchor(null);
    logout();
    navigate('/login');
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    return name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);
  };

  const isAiActive = AI_MENU_ITEMS.some((i) => i.path === location.pathname);
  const isAnalyticsActive = ANALYTICS_MENU_ITEMS.some((i) => i.path === location.pathname);
  const isWorkspaceActive = WORKSPACE_MENU_ITEMS.some((i) => i.path === location.pathname);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: 'transparent', position: 'relative' }}>
      {/* ═══ Non-plain Atmospheric Dynamic Background ═══ */}
      <DynamicBackground />

      {/* ═══ Webfolio Floating Glassmorphic Navigation Bar ═══ */}
      <Box
        component="header"
        className="pt-navbar"
        sx={{
          fontFamily: '"Plus Jakarta Sans", sans-serif',
          position: 'sticky',
          top: 0,
          zIndex: 1100,
          width: '100%',
          borderRadius: 0,
          backgroundColor: scrolled ? 'rgba(12, 13, 16, 0.95)' : 'rgba(21, 22, 29, 0.8)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          boxShadow: scrolled
            ? '0 10px 40px -10px rgba(0, 0, 0, 0.85)'
            : 'none',
          px: { xs: 2, sm: 4, md: 6, lg: 8 },
          py: 2.5,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
      >
        {/* Left: Brand + Navigation Links */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1.5, lg: 4 } }}>
          {/* Mobile Drawer Trigger */}
          <IconButton
            onClick={() => setMobileDrawerOpen(true)}
            sx={{
              display: { xs: 'flex', lg: 'none' },
              color: '#ffffff',
              p: 1,
              '&:hover': { color: '#fd5b38', backgroundColor: 'rgba(255, 255, 255, 0.06)' },
            }}
          >
            <MenuIcon />
          </IconButton>

          {/* PT Brand Identity: PtLogo with Monogram & Subtitle */}
          <PtLogo size={80} onClick={() => navigate('/dashboard')} />

          {/* Desktop Navigation Links */}
          <Box sx={{ display: { xs: 'none', lg: 'flex' }, alignItems: 'center', gap: 2, ml: 3 }}>
            <Button
              onClick={() => navigate('/tips')}
              disableRipple
              sx={navButtonSx(location.pathname === '/tips')}
            >
              Calculator
            </Button>

            <Button
              onClick={() => navigate('/tip-history')}
              disableRipple
              sx={navButtonSx(location.pathname === '/tip-history')}
            >
              Ledger
            </Button>

            <Button
              onClick={(e) => setAiMenuAnchor(e.currentTarget)}
              endIcon={<ArrowDownIcon sx={{ fontSize: 16, transition: 'transform 0.2s', transform: aiMenuAnchor ? 'rotate(180deg)' : 'rotate(0)' }} />}
              disableRipple
              sx={navButtonSx(isAiActive)}
            >
              AI Suite
            </Button>
            {renderMenuItems(AI_MENU_ITEMS, aiMenuAnchor, setAiMenuAnchor, navigate, location)}

            <Button
              onClick={(e) => setAnalyticsMenuAnchor(e.currentTarget)}
              endIcon={<ArrowDownIcon sx={{ fontSize: 16, transition: 'transform 0.2s', transform: analyticsMenuAnchor ? 'rotate(180deg)' : 'rotate(0)' }} />}
              disableRipple
              sx={navButtonSx(isAnalyticsActive)}
            >
              Analytics
            </Button>
            {renderMenuItems(ANALYTICS_MENU_ITEMS, analyticsMenuAnchor, setAnalyticsMenuAnchor, navigate, location)}

            <Button
              onClick={(e) => setWorkspaceMenuAnchor(e.currentTarget)}
              endIcon={<ArrowDownIcon sx={{ fontSize: 16, transition: 'transform 0.2s', transform: workspaceMenuAnchor ? 'rotate(180deg)' : 'rotate(0)' }} />}
              disableRipple
              sx={navButtonSx(isWorkspaceActive)}
            >
              Workspace
            </Button>
            {renderMenuItems(WORKSPACE_MENU_ITEMS, workspaceMenuAnchor, setWorkspaceMenuAnchor, navigate, location)}
          </Box>
        </Box>

        {/* Right: Quick Search Capsule + Webfolio CTA Button + Profile */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.8 }}>
          {/* Spotlight Capsule */}
          <Tooltip title="Command Center (Ctrl+K)" arrow>
            <Button
              onClick={() => setCommandPaletteOpen(true)}
              startIcon={<SearchIcon sx={{ fontSize: 24, color: '#c6c8c9' }} />}
              sx={{
                display: { xs: 'none', sm: 'flex' },
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '30px',
                px: 4,
                py: 1.5,
                color: '#c6c8c9',
                fontSize: '1.3rem',
                textTransform: 'none',
                fontWeight: 600,
                gap: 2,
                minWidth: 320,
                justifyContent: 'flex-start',
                transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                '&:hover': {
                  background: 'rgba(255, 255, 255, 0.08)',
                  borderColor: 'rgba(253, 91, 56, 0.4)',
                  color: '#ffffff',
                },
              }}
            >
              <span style={{ flex: 1, textAlign: 'left' }}>Search...</span>
              <Chip
                label="⌘K"
                size="small"
                sx={{
                  height: 22,
                  fontSize: '0.75rem',
                  fontWeight: 800,
                  backgroundColor: 'rgba(253, 91, 56, 0.15)',
                  color: '#fd5b38',
                  border: '1px solid rgba(253, 91, 56, 0.3)',
                }}
              />
            </Button>
          </Tooltip>

          {/* Webfolio Signature Action Button */}
          <Button
            onClick={() => navigate('/tips')}
            className="butn butn-sm butn-bg"
            endIcon={<ArrowUpRightIcon sx={{ fontSize: '16px !important' }} />}
            component={motion.button}
            whileHover={{ scale: 1.04 }}
            whileTap={{ scale: 0.96 }}
            sx={{
              display: { xs: 'none', sm: 'inline-flex' },
              px: 3.2,
              py: 1,
              fontSize: '0.95rem',
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
              borderRadius: '30px',
              background: '#fd5b38',
              color: '#ffffff',
              boxShadow: '0 4px 18px rgba(253, 91, 56, 0.35)',
              '&:hover': {
                background: '#e04826',
                boxShadow: '0 8px 28px rgba(253, 91, 56, 0.5)',
              },
            }}
          >
            Calculate Tip
          </Button>

          {/* User Profile Ring */}
          <IconButton
            onClick={(e) => setUserMenuAnchor(e.currentTarget)}
            sx={{
              p: 0.4,
              border: '1.5px solid rgba(253, 91, 56, 0.4)',
              borderRadius: '50%',
              backgroundColor: 'rgba(253, 91, 56, 0.08)',
              transition: 'all 0.3s ease',
              '&:hover': {
                borderColor: '#fd5b38',
                boxShadow: '0 0 16px rgba(253, 91, 56, 0.4)',
              },
            }}
          >
            <Avatar
              sx={{
                width: 36,
                height: 36,
                backgroundColor: '#1b1c24',
                color: '#fd5b38',
                fontSize: '0.88rem',
                fontWeight: 800,
                border: '1px solid rgba(255, 255, 255, 0.1)',
              }}
            >
              {getInitials(user?.name)}
            </Avatar>
          </IconButton>

          <Menu
            anchorEl={userMenuAnchor}
            open={Boolean(userMenuAnchor)}
            onClose={() => setUserMenuAnchor(null)}
            PaperProps={{
              sx: {
                width: 260,
                p: 1,
                background: 'rgba(21, 22, 29, 0.98)',
                backdropFilter: 'blur(24px)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                boxShadow: '0 24px 64px rgba(0, 0, 0, 0.85)',
                borderRadius: '18px',
              },
            }}
          >
            <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid rgba(255, 255, 255, 0.08)', mb: 0.8 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.3 }}>
                <Typography variant="body2" sx={{ fontWeight: 800, color: '#ffffff' }}>
                  {user?.name || 'Executive User'}
                </Typography>
                <Chip label="PRO" size="small" sx={{ height: 16, fontSize: '0.55rem', fontWeight: 800, color: '#fd5b38', background: 'rgba(253, 91, 56, 0.15)' }} />
              </Box>
              <Typography variant="caption" sx={{ color: '#c6c8c9', display: 'block', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                {user?.email || 'user@possibletip.ai'}
              </Typography>
            </Box>
            <MenuItem
              onClick={() => {
                setUserMenuAnchor(null);
                navigate('/dashboard');
              }}
              sx={{ gap: 1.5, borderRadius: '10px', mb: 0.5 }}
            >
              <DashboardIcon sx={{ fontSize: 18, color: '#fd5b38' }} />
              Dashboard
            </MenuItem>
            <MenuItem
              onClick={() => {
                setUserMenuAnchor(null);
                navigate('/tip-profile');
              }}
              sx={{ gap: 1.5, borderRadius: '10px' }}
            >
              <AssignmentIndIcon sx={{ fontSize: 18, color: '#fd5b38' }} />
              Profile Settings
            </MenuItem>
            <Divider sx={{ my: 0.6 }} />
            <MenuItem onClick={handleLogout} sx={{ color: '#fb7185', gap: 1.5, fontWeight: 700, borderRadius: '10px' }}>
              <LogoutIcon sx={{ fontSize: 18 }} />
              Sign Out
            </MenuItem>
          </Menu>
        </Box>
      </Box>

      {/* ═══ Mobile Drawer — Webfolio Style ═══ */}
      <Drawer
        anchor="left"
        open={mobileDrawerOpen}
        onClose={() => setMobileDrawerOpen(false)}
        PaperProps={{
          sx: {
            width: 310,
            backgroundColor: '#0c0d10',
            borderRight: '1px solid rgba(255, 255, 255, 0.1)',
            p: 2.5,
          },
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pb: 2, borderBottom: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <PtLogo size={32} />
          <IconButton onClick={() => setMobileDrawerOpen(false)} sx={{ color: '#c6c8c9' }}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>

        <List sx={{ mt: 1.5, overflowY: 'auto', flexGrow: 1 }}>
          <ListSubheader sx={{ backgroundColor: 'transparent', color: '#fd5b38', fontSize: '0.7rem', fontWeight: 800, letterSpacing: '0.12em', lineHeight: '32px' }}>
            CORE PLATFORM
          </ListSubheader>
          {[
            { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon sx={{ fontSize: 19, color: '#fd5b38' }} /> },
            { label: 'Tip Calculator', path: '/tips', icon: <ReceiptIcon sx={{ fontSize: 19, color: '#fd5b38' }} /> },
            { label: 'Ledger History', path: '/tip-history', icon: <HistoryIcon sx={{ fontSize: 19, color: '#c6c8c9' }} /> },
          ].map((item) => (
            <ListItemButton
              key={item.path}
              onClick={() => { setMobileDrawerOpen(false); navigate(item.path); }}
              selected={location.pathname === item.path}
              sx={{
                borderRadius: '12px',
                mb: 0.5,
                '&.Mui-selected': {
                  backgroundColor: 'rgba(253, 91, 56, 0.12)',
                  borderLeft: '3px solid #fd5b38',
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 34 }}>{item.icon}</ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: location.pathname === item.path ? '#ffffff' : '#c6c8c9',
                }}
              />
            </ListItemButton>
          ))}

          <ListSubheader sx={{ backgroundColor: 'transparent', color: '#fd5b38', fontSize: '0.7rem', fontWeight: 800, letterSpacing: '0.12em', lineHeight: '32px', mt: 1.5 }}>
            AI SUITE
          </ListSubheader>
          {AI_MENU_ITEMS.map((item) => (
            <ListItemButton
              key={item.path}
              onClick={() => { setMobileDrawerOpen(false); navigate(item.path); }}
              selected={location.pathname === item.path}
              sx={{ borderRadius: '12px', mb: 0.5, '&.Mui-selected': { backgroundColor: 'rgba(253, 91, 56, 0.12)', borderLeft: '3px solid #fd5b38' } }}
            >
              <ListItemIcon sx={{ minWidth: 34 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 600 }} />
              {item.badge && (
                <Chip label={item.badge} size="small" sx={{ height: 18, fontSize: '0.6rem', fontWeight: 800, background: 'rgba(253, 91, 56, 0.18)', color: '#fd5b38' }} />
              )}
            </ListItemButton>
          ))}

          <ListSubheader sx={{ backgroundColor: 'transparent', color: '#fd5b38', fontSize: '0.7rem', fontWeight: 800, letterSpacing: '0.12em', lineHeight: '32px', mt: 1.5 }}>
            ANALYTICS
          </ListSubheader>
          {ANALYTICS_MENU_ITEMS.map((item) => (
            <ListItemButton
              key={item.path}
              onClick={() => { setMobileDrawerOpen(false); navigate(item.path); }}
              selected={location.pathname === item.path}
              sx={{ borderRadius: '12px', mb: 0.5, '&.Mui-selected': { backgroundColor: 'rgba(253, 91, 56, 0.12)', borderLeft: '3px solid #fd5b38' } }}
            >
              <ListItemIcon sx={{ minWidth: 34 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 600 }} />
            </ListItemButton>
          ))}

          <ListSubheader sx={{ backgroundColor: 'transparent', color: '#fd5b38', fontSize: '0.7rem', fontWeight: 800, letterSpacing: '0.12em', lineHeight: '32px', mt: 1.5 }}>
            WORKSPACE
          </ListSubheader>
          {WORKSPACE_MENU_ITEMS.map((item) => (
            <ListItemButton
              key={item.path}
              onClick={() => { setMobileDrawerOpen(false); navigate(item.path); }}
              selected={location.pathname === item.path}
              sx={{ borderRadius: '12px', mb: 0.5, '&.Mui-selected': { backgroundColor: 'rgba(253, 91, 56, 0.12)', borderLeft: '3px solid #fd5b38' } }}
            >
              <ListItemIcon sx={{ minWidth: 34 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 600 }} />
            </ListItemButton>
          ))}
        </List>

        <Box sx={{ pt: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <Button
            onClick={handleLogout}
            fullWidth
            variant="outlined"
            startIcon={<LogoutIcon />}
            sx={{
              borderColor: 'rgba(251, 113, 133, 0.3)',
              color: '#fb7185',
              borderRadius: '30px',
              fontWeight: 700,
              '&:hover': {
                borderColor: '#fb7185',
                backgroundColor: 'rgba(251, 113, 133, 0.08)',
              },
            }}
          >
            Sign Out
          </Button>
        </Box>
      </Drawer>

      {/* AI Command Palette Modal */}
      <CommandPalette open={commandPaletteOpen} onClose={() => setCommandPaletteOpen(false)} />

      {/* Achievement Notifier */}
      <AchievementNotifier />

      {/* ═══ Main Content Area ═══ */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          position: 'relative',
        }}
      >
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
            style={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </Box>

      {/* ═══ Webfolio Back To Top Floating Progress Button ═══ */}
      {scrolled && (
        <Box
          component={motion.div}
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.8 }}
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          sx={{
            position: 'fixed',
            bottom: 28,
            right: 28,
            width: 46,
            height: 46,
            borderRadius: '50%',
            backgroundColor: '#15161d',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            cursor: 'pointer',
            zIndex: 1000,
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.6)',
            transition: 'all 0.3s ease',
            '&:hover': {
              backgroundColor: '#fd5b38',
              borderColor: '#fd5b38',
              transform: 'translateY(-3px)',
              boxShadow: '0 12px 28px rgba(253, 91, 56, 0.45)',
            },
          }}
        >
          <ArrowUpwardIcon sx={{ fontSize: 20 }} />
        </Box>
      )}
    </Box>
  );
};

export default AppShell;
