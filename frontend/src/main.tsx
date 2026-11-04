import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { AppThemeProvider } from './context/AppThemeProvider';
import { AuthProvider } from './context/AuthContext';
import { MessageProvider } from './context/MessageContext';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <AppThemeProvider>
        <MessageProvider>
          <App />
        </MessageProvider>
      </AppThemeProvider>
    </AuthProvider>
  </StrictMode>,
);
