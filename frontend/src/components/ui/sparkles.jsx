import React, { useEffect, useState } from "react";
import Particles, { initParticlesEngine } from "@tsparticles/react";
import { loadSlim } from "@tsparticles/slim";
import { Box } from "@mui/material";

export const SparklesCore = ({
  background = "transparent",
  minSize = 0.4,
  maxSize = 1,
  particleDensity = 1200,
  className = "",
  particleColor = "#FFFFFF",
  sx = {}
}) => {
  const [init, setInit] = useState(false);

  useEffect(() => {
    initParticlesEngine(async (engine) => {
      await loadSlim(engine);
    }).then(() => {
      setInit(true);
    });
  }, []);

  if (!init) return null;

  return (
    <Box sx={{ ...sx, position: 'absolute', inset: 0 }} className={className}>
      <Particles
        id="tsparticles"
        options={{
          background: { color: { value: background } },
          fullScreen: { enable: false, zIndex: 1 },
          fpsLimit: 120,
          interactivity: {
            events: {
              onHover: { enable: true, mode: "repulse" },
              resize: true,
            },
            modes: {
              repulse: { distance: 100, duration: 0.4 },
            },
          },
          particles: {
            color: { value: particleColor },
            move: {
              direction: "none",
              enable: true,
              outModes: { default: "bounce" },
              random: true,
              speed: 0.5,
              straight: false,
            },
            number: {
              density: { enable: true, width: 400, height: 400 },
              value: particleDensity,
            },
            opacity: {
              value: { min: 0.1, max: 1 },
              animation: { enable: true, speed: 1, minimumValue: 0.1 },
            },
            shape: { type: "circle" },
            size: {
              value: { min: minSize, max: maxSize },
            },
          },
          detectRetina: true,
        }}
        style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}
      />
    </Box>
  );
};
