import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    // Progresyvioji žiniatinklio programa (NFR-02, NFR-17)
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'Meal Planner – asmeninis mitybos planas',
        short_name: 'Meal Planner',
        description: 'Individualus savaitės mitybos planas su receptais ir produktų sąrašu',
        lang: 'lt',
        theme_color: '#2e7d32',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        icons: [{ src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
      workbox: {
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  server: {
    port: 5173,
    // Kūrimo metu /api/ užklausos perduodamos programų serveriui
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
