/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Trading Platform Colors
        trading: {
          profit: {
            DEFAULT: '#52c41a',
            light: '#73d13d',
            dark: '#3f8600',
          },
          loss: {
            DEFAULT: '#ff4d4f',
            light: '#ff7875',
            dark: '#cf1322',
          },
        },
        primary: {
          50: '#f0f4ff',
          100: '#e0eaff',
          200: '#c7d7fe',
          300: '#a5bbfc',
          400: '#8197f8',
          500: '#667eea',
          600: '#5568d3',
          700: '#4854b0',
          800: '#3d4690',
          900: '#353d74',
        },
        secondary: {
          50: '#faf5ff',
          100: '#f3e8ff',
          200: '#e9d5ff',
          300: '#d8b4fe',
          400: '#c084fc',
          500: '#a855f7',
          600: '#9333ea',
          700: '#7e22ce',
          800: '#6b21a8',
          900: '#581c87',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['Fira Code', 'monospace'],
      },
      boxShadow: {
        'widget': '0 2px 8px rgba(0, 0, 0, 0.08)',
        'widget-hover': '0 4px 16px rgba(0, 0, 0, 0.12)',
      },
      animation: {
        'price-up': 'priceUp 0.6s ease-out',
        'price-down': 'priceDown 0.6s ease-out',
        'pulse-profit': 'pulseProfit 2s ease-in-out infinite',
        'pulse-loss': 'pulseLoss 2s ease-in-out infinite',
      },
      keyframes: {
        priceUp: {
          '0%': { backgroundColor: 'rgba(82, 196, 26, 0.3)' },
          '100%': { backgroundColor: 'transparent' },
        },
        priceDown: {
          '0%': { backgroundColor: 'rgba(255, 77, 79, 0.3)' },
          '100%': { backgroundColor: 'transparent' },
        },
        pulseProfit: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(82, 196, 26, 0.4)' },
          '50%': { boxShadow: '0 0 0 8px rgba(82, 196, 26, 0)' },
        },
        pulseLoss: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(255, 77, 79, 0.4)' },
          '50%': { boxShadow: '0 0 0 8px rgba(255, 77, 79, 0)' },
        },
      },
    },
  },
  plugins: [],
};
