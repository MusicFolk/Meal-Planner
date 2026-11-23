import { Box, Button, Card, Checkbox, LinearProgress, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api, ApiError } from '../../api/client';
import type { MealPlan, ShoppingItem, ShoppingList } from '../../api/types';
import { Loading } from '../../components/Loading';
import { PageTitle } from '../../components/PageTitle';
import { useMessage } from '../../context/MessageContext';
import { formatGrams } from '../../lib/format';
import { productCategoryLabels } from '../../lib/labels';

/** Pirkinių sąrašas, sugrupuotas pagal produktų grupes, su varnelėmis (UI-03, FR-19, FR-20). */
export function ShoppingListPage() {
  const navigate = useNavigate();
  const showMessage = useMessage();
  // Iš planų istorijos atidaromas pasirinkto plano sąrašas, kitaip – aktualaus plano (FR-22)
  const { planId } = useParams();
  const [list, setList] = useState<ShoppingList | null | undefined>(undefined);

  useEffect(() => {
    const planIdPromise = planId
      ? Promise.resolve(Number(planId))
      : api.get<MealPlan | undefined>('/api/meal-plans/active').then((plan) => plan?.id);
    planIdPromise
      .then((id) => (id ? api.get<ShoppingList>(`/api/meal-plans/${id}/shopping-list`) : null))
      .then(setList)
      .catch((e: ApiError) => showMessage('error', e.message));
  }, [planId, showMessage]);

  if (list === undefined) return <Loading />;

  if (list === null) {
    return (
      <Box sx={{ textAlign: 'center', pt: 6 }}>
        <Typography variant="h6">Produktų sąrašas bus sudarytas kartu su mitybos planu.</Typography>
        <Button variant="contained" sx={{ mt: 2 }} onClick={() => navigate('/planas')}>
          Į mitybos planą
        </Button>
      </Box>
    );
  }

  // Varnelė išsaugoma serveryje, todėl matoma ir kitame įrenginyje (UI-23)
  const toggle = async (item: ShoppingItem) => {
    const purchased = !item.purchased;
    const update = (value: boolean) =>
      setList((l) =>
        l
          ? {
              ...l,
              purchasedItems: l.purchasedItems + (value ? 1 : -1),
              categories: l.categories.map((c) => ({
                ...c,
                items: c.items.map((i) => (i.id === item.id ? { ...i, purchased: value } : i)),
              })),
            }
          : l,
      );
    update(purchased);
    try {
      await api.put(`/api/meal-plans/shopping-list/items/${item.id}`, { purchased });
    } catch (e) {
      update(!purchased);
      showMessage('error', e instanceof ApiError ? e.message : 'Nepavyko išsaugoti.');
    }
  };

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <PageTitle
        title="Produktų sąrašas"
        subtitle={`${list.purchasedItems} iš ${list.totalItems} nupirkta`}
        back={planId ? `/planas/istorija/${planId}` : undefined}
      />
      <LinearProgress
        variant="determinate"
        value={(list.purchasedItems / Math.max(1, list.totalItems)) * 100}
        sx={{ mb: 2, height: 8, borderRadius: 4 }}
        aria-label="Nupirkta produktų"
      />
      <Stack spacing={2}>
        {list.categories.map((category) => (
          <Card key={category.category}>
            <Typography variant="subtitle1" sx={{ px: 2, pt: 1.5, fontWeight: 700 }}>
              {productCategoryLabels[category.category]}
            </Typography>
            <List dense>
              {category.items.map((item) => (
                <ListItem key={item.id} disablePadding secondaryAction={<Typography>{formatGrams(item.totalGrams)}</Typography>}>
                  <ListItemButton onClick={() => toggle(item)}>
                    <ListItemIcon>
                      <Checkbox edge="start" checked={item.purchased} tabIndex={-1} slotProps={{ input: { 'aria-label': item.name } }} />
                    </ListItemIcon>
                    <ListItemText
                      primary={item.name}
                      sx={{ textDecoration: item.purchased ? 'line-through' : undefined, color: item.purchased ? 'text.secondary' : undefined }}
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          </Card>
        ))}
      </Stack>
    </Box>
  );
}
