import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';
import { NorthEast as ArrowIcon } from '@mui/icons-material';
import DynamicBackground from '../components/DynamicBackground';
import PtLogo from '../components/PtLogo';
import { motion } from 'framer-motion';

// Animation variants
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.2, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.8, ease: [0.16, 1, 0.3, 1] } },
};

const floatVariants = {
  initial: { y: 0 },
  animate: {
    y: [-10, 10, -10],
    transition: {
      duration: 6,
      repeat: Infinity,
      ease: 'easeInOut',
    },
  },
};

const LandingPage = () => {
  const navigate = useNavigate();

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', position: 'relative', overflow: 'hidden' }}>
      <DynamicBackground />

      {/* Simple Top Navigation */}
      <Box
        component={motion.header}
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: 'easeOut' }}
        sx={{
          position: 'absolute',
          top: 0,
          zIndex: 1100,
          width: '100%',
          px: { xs: 3, md: 5 },
          py: 3,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <PtLogo onClick={() => navigate('/')} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            onClick={() => navigate('/login')}
            className="butn butn-md butn-bord"
            component={motion.button}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            sx={{ px: { xs: 2, md: 3 }, py: 1 }}
          >
            Sign In
          </Button>
          <Button
            onClick={() => navigate('/register')}
            className="butn butn-md butn-bg"
            component={motion.button}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            sx={{ px: { xs: 2, md: 3 }, py: 1, display: { xs: 'none', sm: 'inline-flex' } }}
          >
            Create Account
          </Button>
        </Box>
      </Box>

      {/* Main Marketing Content */}
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', px: { xs: 2, sm: 3, md: 5 }, py: { xs: 8, md: 10 }, mt: { xs: 8, md: 0 }, maxWidth: 1480, mx: 'auto', width: '100%', position: 'relative', zIndex: 1 }}>
        <Box
          component={motion.div}
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          sx={{ mb: { xs: 8, md: 12 }, position: 'relative' }}
        >
          <motion.div variants={itemVariants}>
            <Box sx={{ mb: 2.5 }}>
              <span className="title-bord" style={{ display: 'inline-block' }}>
                ✦ PT · POSSIBLE TIP INTELLIGENCE
              </span>
            </Box>
          </motion.div>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1.1fr 0.9fr' }, gap: 5, alignItems: 'center' }}>
            {/* Hero Headlines */}
            <Box>
              <motion.div variants={itemVariants}>
                <Typography
                  component="h1"
                  sx={{
                    fontSize: { xs: '3rem', sm: '4rem', md: '5.5rem' },
                    fontWeight: 800,
                    letterSpacing: '-0.04em',
                    lineHeight: 1.05,
                    color: '#ffffff',
                    mb: 3.5,
                  }}
                >
                  Explore every <br />
                  <span style={{ color: '#fd5b38', textShadow: '0 0 40px rgba(253,91,56,0.3)' }}>possible tip</span> in gratuity.
                </Typography>
              </motion.div>

              <motion.div variants={itemVariants}>
                <Typography
                  sx={{
                    fontSize: { xs: '1.05rem', md: '1.25rem' },
                    color: '#c6c8c9',
                    lineHeight: 1.7,
                    maxWidth: 680,
                    mb: 5.5,
                  }}
                >
                  Empowering patrons, fine diners, hospitality leaders, and finance teams with autonomous Possible Tip calculations, dynamic tax deductibility shields, and predictive gratuity telemetry.
                </Typography>
              </motion.div>

              {/* CTA Buttons */}
              <motion.div variants={itemVariants}>
                <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2.5 }}>
                  <Button
                    onClick={() => navigate('/login')}
                    className="butn butn-md butn-bg"
                    component={motion.button}
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    endIcon={<ArrowIcon sx={{ fontSize: '18px !important' }} />}
                    sx={{ px: 4.5, py: 1.8, fontSize: '1.05rem', borderRadius: '30px', boxShadow: '0 8px 32px rgba(253, 91, 56, 0.4)' }}
                  >
                    Enter Platform
                  </Button>
                </Box>
              </motion.div>
            </Box>

            {/* Right: Glassmorphic Abstract Intelligence Graphic */}
            <Box sx={{ display: { xs: 'none', lg: 'flex' }, justifyContent: 'center', position: 'relative' }}>
              <motion.div
                variants={floatVariants}
                initial="initial"
                animate="animate"
                style={{ position: 'relative', width: '100%', maxWidth: '500px', height: '500px' }}
              >
                {/* Glowing Orb Background */}
                <Box
                  sx={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: '350px',
                    height: '350px',
                    background: 'radial-gradient(circle, rgba(253,91,56,0.2) 0%, rgba(20,24,40,0) 70%)',
                    filter: 'blur(40px)',
                    zIndex: 0,
                  }}
                />
                
                {/* Main Glass Panel */}
                <Box
                  sx={{
                    position: 'absolute',
                    top: '10%',
                    left: '10%',
                    width: '80%',
                    height: '80%',
                    background: 'rgba(255, 255, 255, 0.03)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    borderRadius: '40px',
                    boxShadow: '0 30px 60px rgba(0,0,0,0.5), inset 0 0 20px rgba(255,255,255,0.05)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 1,
                    overflow: 'hidden',
                  }}
                >
                  {/* Dynamic Data Visualization Dashboard Inside Glass */}
                  <Box sx={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', p: { xs: 3, sm: 4 }, display: 'flex', flexDirection: 'column' }}>
                    {/* Header */}
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                      <Box>
                        <Typography sx={{ color: '#c6c8c9', fontSize: '0.75rem', fontWeight: 700, letterSpacing: '2px', mb: 0.5 }}>NEURAL ENGINE</Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box sx={{ width: 8, height: 8, borderRadius: '50%', background: '#10B981', boxShadow: '0 0 10px #10B981' }} />
                          <Typography sx={{ color: '#10B981', fontSize: '0.85rem', fontWeight: 600 }}>ACTIVE SYNC</Typography>
                        </Box>
                      </Box>
                      <Box sx={{ width: 40, height: 40, borderRadius: '12px', background: 'rgba(253,91,56,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(253,91,56,0.3)' }}>
                        <ArrowIcon sx={{ color: '#fd5b38', fontSize: 20 }} />
                      </Box>
                    </Box>

                    {/* Chart Area */}
                    <Box sx={{ flexGrow: 1, position: 'relative', display: 'flex', alignItems: 'flex-end', gap: { xs: 1, sm: 2 }, pb: 2 }}>
                      {/* Grid Lines */}
                      <Box sx={{ position: 'absolute', top: '25%', left: 0, right: 0, borderTop: '1px dashed rgba(255,255,255,0.05)', zIndex: 0 }} />
                      <Box sx={{ position: 'absolute', top: '50%', left: 0, right: 0, borderTop: '1px dashed rgba(255,255,255,0.05)', zIndex: 0 }} />
                      <Box sx={{ position: 'absolute', top: '75%', left: 0, right: 0, borderTop: '1px dashed rgba(255,255,255,0.05)', zIndex: 0 }} />
                      
                      {/* Animated Bars */}
                      {[30, 55, 40, 75, 50, 95, 65].map((h, i) => (
                        <Box key={i} sx={{ flex: 1, position: 'relative', height: '100%', display: 'flex', alignItems: 'flex-end', zIndex: 1 }}>
                          <motion.div
                            initial={{ height: '0%' }}
                            animate={{ height: `${h}%` }}
                            transition={{ duration: 1.5, delay: 0.8 + (i * 0.1), type: 'spring', bounce: 0.4 }}
                            style={{ 
                              width: '100%', 
                              background: i === 5 ? 'linear-gradient(180deg, #fd5b38 0%, rgba(253,91,56,0.1) 100%)' : 'rgba(255,255,255,0.1)', 
                              borderRadius: '6px 6px 0 0', 
                              position: 'relative' 
                            }}
                          >
                            {i === 5 && (
                              <Box sx={{ position: 'absolute', top: -28, left: '50%', transform: 'translateX(-50%)', background: '#fd5b38', px: 1, py: 0.5, borderRadius: '6px', fontSize: '0.75rem', fontWeight: 800, color: '#fff', boxShadow: '0 4px 12px rgba(253,91,56,0.4)' }}>
                                99.9%
                              </Box>
                            )}
                          </motion.div>
                        </Box>
                      ))}
                    </Box>

                    {/* Footer Stats */}
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2, pt: 2, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
                      <Box>
                        <Typography sx={{ color: '#757779', fontSize: '0.7rem', fontWeight: 600, mb: 0.5 }}>CALCULATIONS</Typography>
                        <Typography sx={{ color: '#fff', fontSize: '1.2rem', fontWeight: 800, fontFamily: 'monospace' }}>2.4M/s</Typography>
                      </Box>
                      <Box sx={{ textAlign: 'right' }}>
                        <Typography sx={{ color: '#757779', fontSize: '0.7rem', fontWeight: 600, mb: 0.5 }}>LATENCY</Typography>
                        <Typography sx={{ color: '#10B981', fontSize: '1.2rem', fontWeight: 800, fontFamily: 'monospace' }}>12ms</Typography>
                      </Box>
                    </Box>
                  </Box>
                </Box>
                
                {/* Floating Badge */}
                <Box
                  component={motion.div}
                  animate={{ y: [-15, 15, -15], rotate: [-2, 2, -2] }}
                  transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut' }}
                  sx={{
                    position: 'absolute',
                    bottom: '15%',
                    right: '5%',
                    background: 'rgba(20,24,40,0.8)',
                    backdropFilter: 'blur(10px)',
                    border: '1px solid rgba(253,91,56,0.4)',
                    borderRadius: '16px',
                    padding: '16px 24px',
                    zIndex: 2,
                    boxShadow: '0 20px 40px rgba(0,0,0,0.4)',
                  }}
                >
                  <Typography sx={{ color: '#fd5b38', fontWeight: 800, fontSize: '1.2rem', lineHeight: 1 }}>100%</Typography>
                  <Typography sx={{ color: '#c6c8c9', fontSize: '0.75rem', fontWeight: 600, letterSpacing: '0.5px' }}>TELEMETRY INTEGRITY</Typography>
                </Box>
              </motion.div>
            </Box>
          </Box>
        </Box>

        {/* ═══════════════════════════════════════════════════
            WEBFOLIO DUAL KINETIC MARQUEE TICKER
            ═══════════════════════════════════════════════════ */}
        <Box
          component={motion.div}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1, duration: 1 }}
          sx={{ width: '100%', mt: 'auto', pb: 4, overflow: 'hidden', position: 'relative' }}
        >
          {/* Marquee Fade Edges */}
          <Box sx={{ position: 'absolute', top: 0, left: 0, width: '10%', height: '100%', background: 'linear-gradient(90deg, #0A0E1A 0%, transparent 100%)', zIndex: 2, pointerEvents: 'none' }} />
          <Box sx={{ position: 'absolute', top: 0, right: 0, width: '10%', height: '100%', background: 'linear-gradient(270deg, #0A0E1A 0%, transparent 100%)', zIndex: 2, pointerEvents: 'none' }} />

          <Box className="main-marq">
            <Box className="slide-har st1 strok">
              <Box className="box">
                <span className="item">PT · POSSIBLE TIP <span className="star-divider">✦</span></span>
                <span className="item">NEURAL GRATUITY CALIBRATION <span className="star-divider">✦</span></span>
                <span className="item">TAX DEDUCTIBILITY SHIELD <span className="star-divider">✦</span></span>
                <span className="item">QUANTUM WHAT-IF SCENARIOS <span className="star-divider">✦</span></span>
                <span className="item">AUTONOMOUS DISPATCH LEDGER <span className="star-divider">✦</span></span>
              </Box>
              <Box className="box">
                <span className="item">PT · POSSIBLE TIP <span className="star-divider">✦</span></span>
                <span className="item">NEURAL GRATUITY CALIBRATION <span className="star-divider">✦</span></span>
                <span className="item">TAX DEDUCTIBILITY SHIELD <span className="star-divider">✦</span></span>
                <span className="item">QUANTUM WHAT-IF SCENARIOS <span className="star-divider">✦</span></span>
                <span className="item">AUTONOMOUS DISPATCH LEDGER <span className="star-divider">✦</span></span>
              </Box>
            </Box>
          </Box>

          <Box className="main-marq" sx={{ mt: -2 }}>
            <Box className="slide-har st2 non-strok">
              <Box className="box">
                <span className="item">POSSIBLE TIP EXPLORER <span className="star-divider">✦</span></span>
                <span className="item">SPEND VELOCITY FORECAST <span className="star-divider">✦</span></span>
                <span className="item">100% TELEMETRY INTEGRITY <span className="star-divider">✦</span></span>
                <span className="item">HOSPITALITY BENCHMARKS <span className="star-divider">✦</span></span>
                <span className="item">INSTANT SPLIT DISPATCH <span className="star-divider">✦</span></span>
              </Box>
              <Box className="box">
                <span className="item">POSSIBLE TIP EXPLORER <span className="star-divider">✦</span></span>
                <span className="item">SPEND VELOCITY FORECAST <span className="star-divider">✦</span></span>
                <span className="item">100% TELEMETRY INTEGRITY <span className="star-divider">✦</span></span>
                <span className="item">HOSPITALITY BENCHMARKS <span className="star-divider">✦</span></span>
                <span className="item">INSTANT SPLIT DISPATCH <span className="star-divider">✦</span></span>
              </Box>
            </Box>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default LandingPage;
