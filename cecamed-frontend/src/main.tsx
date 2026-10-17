import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import '@fontsource-variable/atkinson-hyperlegible-next';
import './styles.css';
import { App } from './App';
import { ThemeProvider } from './theme/ThemeContext';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/Toast';
import { ConfirmProvider } from './components/ConfirmDialog';

import { API_BASE_URL } from './api/client';

async function start() {
  // Modo API simulada (npm run dev:mock): se carga solo en ese modo y no entra en la compilación normal.
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { installMockApi } = await import('./mocks/mockApi');
    installMockApi(new URL(API_BASE_URL, window.location.origin).pathname);
  }
  render();
}

function render() {
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <ThemeProvider>
        <ToastProvider>
          <ConfirmProvider>
            <AuthProvider>
              <App />
            </AuthProvider>
          </ConfirmProvider>
        </ToastProvider>
      </ThemeProvider>
    </BrowserRouter>
  </StrictMode>,
);
}

void start();
