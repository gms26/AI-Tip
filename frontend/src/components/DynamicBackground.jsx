import React, { useEffect, useRef } from 'react';

/**
 * DynamicBackground - A bespoke, non-plain atmospheric canvas
 * Renders an ambient kinetic particle-mesh with glowing nodes and subtle flowing wave lines,
 * overlaid on an architectural grid matrix with deep vignette lighting.
 * Strictly adheres to the unified Electric Coral (#fd5b38) palette without mixed colors.
 */
const DynamicBackground = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let animationFrameId;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    // Particle nodes
    const particleCount = Math.min(width > 768 ? 55 : 28, 70);
    const particles = [];
    const maxDistance = 140;

    let mouse = { x: width / 2, y: height / 2, active: false };

    class Particle {
      constructor() {
        this.x = Math.random() * width;
        this.y = Math.random() * height;
        this.vx = (Math.random() - 0.5) * 0.45;
        this.vy = (Math.random() - 0.5) * 0.45;
        this.radius = Math.random() * 1.8 + 0.8;
        this.baseAlpha = Math.random() * 0.4 + 0.2;
      }

      update() {
        this.x += this.vx;
        this.y += this.vy;

        if (this.x < 0) this.x = width;
        if (this.x > width) this.x = 0;
        if (this.y < 0) this.y = height;
        if (this.y > height) this.y = 0;

        // Subtle mouse attraction
        if (mouse.active) {
          const dx = mouse.x - this.x;
          const dy = mouse.y - this.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 180) {
            this.x += (dx / dist) * 0.6;
            this.y += (dy / dist) * 0.6;
          }
        }
      }

      draw() {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(253, 91, 56, ${this.baseAlpha})`;
        ctx.fill();
      }
    }

    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }

    // Gentle flowing digital wave parameters
    let waveOffset = 0;

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      // Draw subtle architectural digital wave lines across the screen
      waveOffset += 0.008;
      ctx.lineWidth = 1;
      
      // Wave 1
      ctx.beginPath();
      for (let x = 0; x <= width; x += 15) {
        const y = height * 0.35 + Math.sin(x * 0.003 + waveOffset) * 45 + Math.cos(x * 0.002 + waveOffset * 0.8) * 25;
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.strokeStyle = 'rgba(253, 91, 56, 0.05)';
      ctx.stroke();

      // Wave 2
      ctx.beginPath();
      for (let x = 0; x <= width; x += 15) {
        const y = height * 0.65 + Math.cos(x * 0.0025 - waveOffset * 0.9) * 55 + Math.sin(x * 0.0015 + waveOffset) * 30;
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.strokeStyle = 'rgba(253, 91, 56, 0.04)';
      ctx.stroke();

      // Draw connecting lines between close particles
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < maxDistance) {
            const alpha = (1 - dist / maxDistance) * 0.18;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(253, 91, 56, ${alpha})`;
            ctx.lineWidth = 0.8;
            ctx.stroke();
          }
        }
      }

      // Update and draw particle nodes
      particles.forEach((p) => {
        p.update();
        p.draw();
      });

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    const handleResize = () => {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };

    const handleMouseMove = (e) => {
      mouse.x = e.clientX;
      mouse.y = e.clientY;
      mouse.active = true;
    };

    const handleMouseLeave = () => {
      mouse.active = false;
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseleave', handleMouseLeave);

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
      }}
    >
      {/* Layer 1: Atmospheric Deep Radial Gradients (Non-plain, dynamic lighting) */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: `
            radial-gradient(circle 800px at 20% -10%, rgba(253, 91, 56, 0.14) 0%, transparent 70%),
            radial-gradient(circle 700px at 85% 30%, rgba(253, 91, 56, 0.10) 0%, transparent 65%),
            radial-gradient(circle 900px at 50% 110%, rgba(253, 91, 56, 0.12) 0%, transparent 70%),
            radial-gradient(ellipse 100% 100% at 50% 50%, #0d101d 0%, #080a12 60%, #05060b 100%)
          `,
        }}
      />

      {/* Layer 2: Architectural Cyber Grid & Geometric Dot Matrix */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          backgroundImage: `
            linear-gradient(to right, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
            linear-gradient(to bottom, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
            radial-gradient(rgba(253, 91, 56, 0.12) 1.5px, transparent 1.5px)
          `,
          backgroundSize: '48px 48px, 48px 48px, 96px 96px',
          backgroundPosition: '0 0, 0 0, 24px 24px',
          maskImage: 'radial-gradient(ellipse 95% 95% at 50% 50%, #000 40%, transparent 95%)',
          WebkitMaskImage: 'radial-gradient(ellipse 95% 95% at 50% 50%, #000 40%, transparent 95%)',
          opacity: 0.85,
        }}
      />

      {/* Layer 3: Interactive Kinetic Particle-Mesh Canvas */}
      <canvas
        ref={canvasRef}
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
        }}
      />

      {/* Layer 4: Subtle Luxury Vignette */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'radial-gradient(ellipse at center, transparent 40%, rgba(5, 6, 11, 0.65) 100%)',
        }}
      />
    </div>
  );
};

export default DynamicBackground;
