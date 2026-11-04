import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import { Badge, IconButton, List, ListItem, ListItemText, Popover, Typography } from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Notification } from '../api/types';
import { formatDateTime } from '../lib/format';

/** Pranešimai klientui sistemoje, pvz., apie atliktą dietologo peržiūrą (NI-11). */
export function NotificationBell() {
  const [items, setItems] = useState<Notification[]>([]);
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const load = useCallback(() => {
    api.get<Notification[]>('/api/notifications').then(setItems).catch(() => undefined);
  }, []);

  useEffect(load, [load]);

  const unread = items.filter((n) => !n.read).length;

  const open = (e: React.MouseEvent<HTMLElement>) => {
    setAnchor(e.currentTarget);
    if (unread > 0) {
      api.post('/api/notifications/read-all').catch(() => undefined);
    }
  };

  const close = () => {
    setAnchor(null);
    setItems((list) => list.map((n) => ({ ...n, read: true })));
  };

  return (
    <>
      <IconButton color="inherit" onClick={open} aria-label={`Pranešimai, neperskaitytų: ${unread}`}>
        <Badge badgeContent={unread} color="error">
          <NotificationsNoneIcon />
        </Badge>
      </IconButton>
      <Popover
        open={anchor !== null}
        anchorEl={anchor}
        onClose={close}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { width: 340, maxWidth: '90vw' } } }}
      >
        <Typography variant="subtitle1" sx={{ px: 2, pt: 1.5, fontWeight: 700 }}>
          Pranešimai
        </Typography>
        {items.length === 0 ? (
          <Typography sx={{ p: 2 }} color="text.secondary">
            Pranešimų nėra.
          </Typography>
        ) : (
          <List dense>
            {items.map((n) => (
              <ListItem key={n.id} sx={{ bgcolor: n.read ? undefined : 'action.hover' }}>
                <ListItemText primary={n.message} secondary={formatDateTime(n.createdAt)} />
              </ListItem>
            ))}
          </List>
        )}
      </Popover>
    </>
  );
}
