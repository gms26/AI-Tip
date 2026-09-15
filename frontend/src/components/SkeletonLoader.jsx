import { Box } from '@mui/material';
import { motion } from 'framer-motion';

/**
 * Custom Skeleton Loader using Framer Motion
 * Provides a much smoother, premium pulsing animation than standard MUI Skeletons.
 */
const SkeletonLoader = ({ 
  width = '100%', 
  height = 20, 
  borderRadius = 4, 
  variant = 'rectangular',
  sx = {}
}) => {
  const isCircular = variant === 'circular';

  return (
    <Box
      component={motion.div}
      initial={{ opacity: 0.5 }}
      animate={{ opacity: [0.5, 0.8, 0.5] }}
      transition={{
        duration: 1.5,
        repeat: Infinity,
        ease: 'easeInOut',
      }}
      sx={{
        width,
        height,
        borderRadius: isCircular ? '50%' : borderRadius,
        backgroundColor: 'rgba(255, 255, 255, 0.08)',
        ...sx,
      }}
    />
  );
};

export default SkeletonLoader;
