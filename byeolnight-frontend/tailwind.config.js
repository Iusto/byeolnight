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
    },
  },
  plugins: [],
}
