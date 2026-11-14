import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Проксі: браузер звертається до /api на тому ж origin (localhost:5173), Vite пересилає запит на Spring Boot.
// Для браузера це один сайт, тому сесійна cookie і CSRF працюють без налаштування CORS на бекенді.
// 127.0.0.1, а не localhost: Node може спершу пробувати IPv6 (::1), і тоді проксі отримує 502.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8090',
    },
  },
})
