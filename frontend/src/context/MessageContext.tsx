import { Alert, Snackbar, type AlertColor } from '@mui/material';
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

/**
 * Pranešimų juosta: klaida (raudona), įspėjimas (oranžinė), informacija (mėlyna), patvirtinimas (žalia).
 * Klaidos ir įspėjimai lieka matomi, kol naudotojas juos uždaro.
 */
type MessageType = 'error' | 'warning' | 'info' | 'success';

interface Message {
  key: number;
  type: MessageType;
  text: string;
}

interface MessageApi {
  show: (type: MessageType, text: string) => void;
  clear: () => void;
}

const MessageContext = createContext<MessageApi | null>(null);

export function MessageProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<Message | null>(null);

  const show = useCallback((type: MessageType, text: string) => {
    setMessage({ key: Date.now(), type, text });
  }, []);

  const persistent = message?.type === 'error' || message?.type === 'warning';
  const clear = useCallback(() => setMessage(null), []);
  const value = useMemo(() => ({ show, clear }), [show, clear]);

  return (
    <MessageContext.Provider value={value}>
      {children}
      <Snackbar
        key={message?.key}
        open={message !== null}
        autoHideDuration={persistent ? null : 5000}
        onClose={(_, reason) => reason !== 'clickaway' && setMessage(null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity={(message?.type ?? 'info') as AlertColor}
          variant="filled"
          onClose={() => setMessage(null)}
          sx={{ width: '100%' }}
        >
          {message?.text}
        </Alert>
      </Snackbar>
    </MessageContext.Provider>
  );
}

function useMessageApi(): MessageApi {
  const ctx = useContext(MessageContext);
  if (!ctx) throw new Error('useMessage naudojamas be MessageProvider');
  return ctx;
}

export function useMessage() {
  return useMessageApi().show;
}

/** Uždaro rodomą pranešimą, pvz., pakartotinai pateikiant formą. */
export function useClearMessage() {
  return useMessageApi().clear;
}
