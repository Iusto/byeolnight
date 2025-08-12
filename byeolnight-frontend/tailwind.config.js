/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        space: '#0b0c2a',
        starlight: '#e2e8f0',
      },
      dropShadow: {
        glow: '0 0 8px #8b5cf6',
      },
      animation: {
        'glow': 'glow 2s ease-in-out infinite alternate',
        'fade': 'fade 3s ease-in-out infinite',
        'glitch': 'glitch 0.5s infinite',
        'blink-fast': 'blink 0.5s infinite',
        'halo': 'halo 2s linear infinite',
        'love': 'love 1.5s ease-in-out infinite',
        'sparkle': 'sparkle 1s ease-in-out infinite',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'fadeIn': 'fadeIn 0.3s ease-in-out',
        'float': 'float 3s ease-in-out infinite',
        'fade-in': 'fadeInUp 0.8s ease-out forwards',
        'fade-in-up': 'fadeInUp 0.6s ease-out forwards',
        'fade-in-delay': 'fadeInDelay 1.2s ease-out forwards',
        'spin-slow': 'spin 8s linear infinite',
        'cosmic-float': 'cosmicFloat 6s ease-in-out infinite',
        'star-twinkle': 'starTwinkle 2s ease-in-out infinite',
        'planet-orbit': 'planetOrbit 20s linear infinite',
        'nebula-drift': 'nebulaDrift 15s ease-in-out infinite',
      },
      keyframes: {
        glow: {
          '0%': { boxShadow: '0 0 5px #8b5cf6, 0 0 10px #8b5cf6' },
          '100%': { boxShadow: '0 0 10px #8b5cf6, 0 0 20px #8b5cf6, 0 0 30px #8b5cf6' },
        },
        fade: {
          '0%, 100%': { opacity: '0.5' },
          '50%': { opacity: '1' },
        },
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(30px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        fadeInDelay: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '50%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        cosmicFloat: {
          '0%, 100%': { transform: 'translateY(0px) rotate(0deg)' },
          '33%': { transform: 'translateY(-10px) rotate(1deg)' },
          '66%': { transform: 'translateY(-5px) rotate(-1deg)' },
        },
        starTwinkle: {
          '0%, 100%': { opacity: '0.3', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.2)' },
        },
        planetOrbit: {
          '0%': { transform: 'rotate(0deg) translateX(20px) rotate(0deg)' },
          '100%': { transform: 'rotate(360deg) translateX(20px) rotate(-360deg)' },
        },
        nebulaDrift: {
          '0%': { transform: 'translateX(-10px) translateY(-5px)' },
          '25%': { transform: 'translateX(10px) translateY(-10px)' },
          '50%': { transform: 'translateX(15px) translateY(5px)' },
          '75%': { transform: 'translateX(-5px) translateY(10px)' },
          '100%': { transform: 'translateX(-10px) translateY(-5px)' },
        },
        glitch: {
          '0%': { transform: 'translate(0)' },
          '20%': { transform: 'translate(-2px, 2px)' },
          '40%': { transform: 'translate(-2px, -2px)' },
          '60%': { transform: 'translate(2px, 2px)' },
          '80%': { transform: 'translate(2px, -2px)' },
          '100%': { transform: 'translate(0)' },
        },
        blink: {
          '0%, 50%': { opacity: '1' },
          '51%, 100%': { opacity: '0.3' },
        },
        halo: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        love: {
          '0%, 100%': { transform: 'scale(1)' },
          '50%': { transform: 'scale(1.1)' },
        },
        sparkle: {
          '0%, 100%': { transform: 'scale(1) rotate(0deg)' },
          '50%': { transform: 'scale(1.2) rotate(180deg)' },
        },
      },
    },
  },
  plugins: [],
}
