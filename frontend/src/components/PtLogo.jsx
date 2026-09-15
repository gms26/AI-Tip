import React from 'react';
import { Box, Typography } from '@mui/material';

/**
 * PtLogo — Stylish Cracker "PT" Brand Identity Mark for "Possible Tip"
 * 
 * Design Elements:
 * - Dynamic Celebration Firecracker / Rocket Cracker emblem interlinked with bold "PT" letterforms.
 * - Radiating celebration sparks, energetic cracker burst flare, and precision cyber facets.
 * - Single cohesive signature colour: Electric Coral (#fd5b38 / #ff7a5c) + Crisp Optical White (#ffffff).
 * - Text lockup: "PT" + "Possible" + "POSSIBLE TIP".
 */
const PtLogo = ({ 
  size = 40, 
  showText = true, 
  subtitle = 'POSSIBLE TIP',
  onClick,
  sx = {} 
}) => {
  return (
    <Box 
      onClick={onClick}
      sx={{ 
        display: 'inline-flex', 
        alignItems: 'center', 
        gap: 1.5, 
        cursor: onClick ? 'pointer' : 'default',
        userSelect: 'none',
        ...sx 
      }}
    >
      {/* Stylish Cracker Monogram Emblem */}
      <Box
        sx={{
          width: size,
          height: size,
          borderRadius: `${Math.round(size * 0.3)}px`,
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'linear-gradient(135deg, rgba(253, 91, 56, 0.22) 0%, rgba(20, 22, 32, 0.95) 100%)',
          border: '1.5px solid rgba(253, 91, 56, 0.5)',
          boxShadow: '0 0 24px rgba(253, 91, 56, 0.3), inset 0 1px 1px rgba(255, 255, 255, 0.25)',
          backdropFilter: 'blur(12px)',
          transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
          overflow: 'visible',
          '&:hover': {
            borderColor: '#fd5b38',
            boxShadow: '0 0 32px rgba(253, 91, 56, 0.6), inset 0 1px 2px rgba(255, 255, 255, 0.4)',
            transform: 'translateY(-2px) scale(1.04)',
          }
        }}
      >
        <svg 
          width={size * 0.72} 
          height={size * 0.72} 
          viewBox="0 0 64 64" 
          fill="none" 
          xmlns="http://www.w3.org/2000/svg"
        >
          <defs>
            <linearGradient id="ptCoralGradLogo" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#ff8a6a" />
              <stop offset="45%" stopColor="#fd5b38" />
              <stop offset="100%" stopColor="#e03e1b" />
            </linearGradient>
            <linearGradient id="ptWhiteGradLogo" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#ffffff" />
              <stop offset="100%" stopColor="#e2e8f0" />
            </linearGradient>
            <filter id="ptGlowLogo" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="1.6" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
            <filter id="ptShadowLogo" x="-30%" y="-30%" width="160%" height="160%">
              <feDropShadow dx="0" dy="2" stdDeviation="2.5" floodColor="#fd5b38" floodOpacity="0.45" />
            </filter>
          </defs>

          {/* ═══ CELEBRATION CRACKER SPARKS (Top-Right) ═══ */}
          {/* Main 4-point Diamond Cracker Burst */}
          <path d="M49 8 Q49 14 55 14 Q49 14 49 20 Q49 14 43 14 Q49 14 49 8 Z" fill="#ffffff" filter="url(#ptGlowLogo)" />
          {/* Spark Embers */}
          <circle cx="57" cy="9" r="1.4" fill="#ff8a6a" />
          <circle cx="55" cy="22" r="1.2" fill="#ffffff" />
          <circle cx="41" cy="7" r="1.1" fill="#fd5b38" />
          <circle cx="60" cy="16" r="0.9" fill="#ffb499" />
          {/* Cracker spark trail */}
          <path d="M37 17 Q43 13 46 14" stroke="#ffffff" strokeWidth="1.5" strokeLinecap="round" strokeDasharray="1.5 2.5" opacity="0.85" />

          {/* ═══ INTERLINKED CRACKER "PT" BRAND EMBLEM ═══ */}
          {/* P Letterform with Cracker Facet */}
          <path 
            d="M15 17 H27 C33.5 17 37 20.5 37 26.5 C37 32.5 33.5 36 27 36 H21 V49 H15 V17 Z" 
            fill="url(#ptCoralGradLogo)" 
            filter="url(#ptShadowLogo)" 
          />
          {/* P Inner Counter Cutout */}
          <path d="M21 23 H26.5 C29.5 23 31 24.2 31 26.5 C31 28.8 29.5 30 26.5 30 H21 V23 Z" fill="#131625" />
          {/* P Inner Accent Chevron */}
          <path d="M23 25 L26 26.5 L23 28" stroke="#ffffff" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />

          {/* T Letterform (Interlinked & Forward-Overlaid) */}
          <path 
            d="M29 25 H51 V31 H43 V49 H37 V31 H29 V25 Z" 
            fill="url(#ptWhiteGradLogo)" 
            filter="url(#ptShadowLogo)" 
          />
          {/* Rocket Cracker Tail Fin at T Stem Base */}
          <path d="M34 49 L40 43 L46 49 Z" fill="url(#ptCoralGradLogo)" />

          {/* Central Energy Spark Node */}
          <circle cx="27" cy="26.5" r="2.2" fill="#ffffff" filter="url(#ptGlowLogo)" />
        </svg>
      </Box>

      {/* Brand Typography */}
      {showText && (
        <Box sx={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <Typography
              variant="h5"
              sx={{
                fontWeight: 900,
                fontSize: size >= 38 ? '1.35rem' : '1.15rem',
                fontFamily: '"Plus Jakarta Sans", sans-serif',
                letterSpacing: '-0.03em',
                color: '#ffffff',
                lineHeight: 1,
              }}
            >
              PT
            </Typography>
            <Box
              component="span"
              sx={{
                width: 4,
                height: 4,
                borderRadius: '50%',
                background: '#fd5b38',
                boxShadow: '0 0 8px #fd5b38',
                display: 'inline-block'
              }}
            />
            <Typography
              variant="h6"
              sx={{
                fontWeight: 700,
                fontSize: size >= 38 ? '1.05rem' : '0.92rem',
                fontFamily: '"Plus Jakarta Sans", sans-serif',
                letterSpacing: '-0.02em',
                color: '#fd5b38',
                lineHeight: 1,
              }}
            >
              Possible
            </Typography>
          </Box>
          <Typography
            variant="caption"
            sx={{
              fontWeight: 800,
              fontSize: '0.62rem',
              letterSpacing: '0.14em',
              color: 'rgba(255, 255, 255, 0.45)',
              fontFamily: '"JetBrains Mono", monospace',
              textTransform: 'uppercase',
              mt: 0.35,
              lineHeight: 1,
            }}
          >
            {subtitle}
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default PtLogo;
