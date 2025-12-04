/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        'sans': ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        burgundy: {
          50: '#fdf2f2',
          100: '#fae6e6',
          200: '#f5bfc0',
          300: '#ef999a',
          400: '#e04d4f',
          500: '#d10004',
          600: '#bc0004',
          700: '#730d10', // primary burgundy
          800: '#5a0a0d',
          900: '#3d0709',
          light: '#8a3e41',
          lighter: '#a26f71',
          dark: '#5a0a0d',
        }
      }
    },
  },
  plugins: [],
}