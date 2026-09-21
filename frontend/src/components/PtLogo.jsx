import React from 'react';
import { Box, Typography } from '@mui/material';
import logoImage from '../assets/logo.png';

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
      {/* User Uploaded Logo Emblem */}
      <Box
        component="img"
        src={logoImage}
        alt="Possible Tip Logo"
        sx={{
          width: size * 1.35,
          height: size * 1.35,
          objectFit: 'contain',
          borderRadius: `${Math.round(size * 1.35 * 0.2)}px`,
          transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
          '&:hover': {
            transform: 'scale(1.05)',
          }
        }}
      />

      {/* Brand Typography */}
      {showText && (
        <Box sx={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
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
