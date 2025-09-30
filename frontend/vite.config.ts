import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      port: parseInt(env.VITE_PORT) || 3000,
      strictPort: true,
      host: true,
      open: false,
      hmr: {
            overlay: true  // Enable hot reload
          },
      watch: {
            usePolling: true, // Watch for changes
          }
    },
    build: {
      outDir: 'build'
    }
  }
})