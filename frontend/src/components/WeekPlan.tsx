import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import { Box, ButtonBase, Card, CardContent, LinearProgress, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import type { FirstDayOfWeek, MealPlan } from '../api/types';
import { formatDate, formatDayHeading, formatInt, formatWeekRange, planCalendar } from '../lib/format';
import { AiBadge, DemoBadge } from './AiBadge';
import { DishCard } from './DishCard';

interface Props {
  plan: MealPlan;
  firstDayOfWeek: FirstDayOfWeek;
  title?: string;
  commentCount?: (dishId: number) => number;
  onDishClick: (dishId: number) => void;
}

/** Savaitės planuoklis: 7 dienų juosta, dienos suvestinė ir valgymų kortelės (UI-01, FR-17, NFR-49). */
export function WeekPlan({ plan, firstDayOfWeek, title = 'Savaitės planas', commentCount, onDishClick }: Props) {
  const [selectedDay, setSelectedDay] = useState<number | null>(null);

  const calendar = planCalendar(plan.createdAt, firstDayOfWeek);
  const todayNumber = ((new Date().getDay() + 6) % 7) + 1;
  const inThisWeek = calendar.some((d) => formatDate(d.date) === formatDate(new Date()));
  const dayNumber = selectedDay ?? (inThisWeek ? todayNumber : calendar[0].dayNumber);
  const day = plan.days.find((d) => d.dayNumber === dayNumber) ?? plan.days[0];
  const calendarDay = calendar.find((c) => c.dayNumber === day.dayNumber)!;
  const target = plan.dailyCalories ?? day.totalCalories;
  const deviation = day.deviationPercent ?? (target ? ((day.totalCalories - target) / target) * 100 : 0);

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="body2" color="text.secondary">
          {formatWeekRange(calendar[0].date, calendar[6].date)}
        </Typography>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap' }}>
          <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
            {title}
          </Typography>
          {plan.aiGenerated ? <AiBadge size="medium" /> : <DemoBadge />}
        </Stack>
      </Box>

      <Box
        role="tablist"
        aria-label="Savaitės dienos"
        sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: { xs: 0.5, sm: 1 } }}
      >
        {calendar.map((c) => {
          const selected = c.dayNumber === day.dayNumber;
          const planDay = plan.days.find((d) => d.dayNumber === c.dayNumber);
          return (
            <ButtonBase
              key={c.dayNumber}
              role="tab"
              aria-selected={selected}
              aria-label={formatDayHeading(c)}
              onClick={() => setSelectedDay(c.dayNumber)}
              sx={{
                flexDirection: 'column',
                py: 1,
                borderRadius: 2,
                border: 1,
                borderColor: selected ? 'text.primary' : 'divider',
                bgcolor: selected ? 'text.primary' : 'background.paper',
                color: selected ? 'background.paper' : 'text.primary',
              }}
            >
              <Typography variant="caption">{c.short}</Typography>
              <Typography sx={{ fontWeight: 700, fontSize: '1.15rem' }}>{c.date.getDate()}</Typography>
              {planDay && (
                <Typography variant="caption" sx={{ display: { xs: 'none', md: 'block' }, opacity: 0.8 }}>
                  {formatInt(planDay.totalCalories)} kcal
                </Typography>
              )}
            </ButtonBase>
          );
        })}
      </Box>

      <Card>
        <CardContent>
          <Stack direction="row" sx={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
            <Typography sx={{ fontWeight: 600 }}>{formatDayHeading(calendarDay)}</Typography>
            <Typography sx={{ fontWeight: 600 }}>
              {formatInt(day.totalCalories)} / {formatInt(target)} kcal
            </Typography>
          </Stack>
          <LinearProgress
            variant="determinate"
            value={Math.min(100, (day.totalCalories / target) * 100)}
            sx={{ my: 1.5, height: 8, borderRadius: 4 }}
            aria-label="Dienos kaloringumas"
          />
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <CheckCircleOutlineIcon color={Math.abs(deviation) <= 10 ? 'success' : 'warning'} fontSize="small" />
            <Typography variant="body2">
              {Math.abs(deviation) <= 10 ? 'Atitinka paros poreikį' : 'Nukrypsta nuo paros poreikio'} (
              {deviation >= 0 ? '+' : ''}
              {deviation.toFixed(1).replace('.', ',')} %)
            </Typography>
          </Stack>
        </CardContent>
      </Card>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
        {day.meals.map((meal) => (
          <DishCard
            key={meal.type}
            meal={meal}
            aiGenerated={plan.aiGenerated}
            commentCount={commentCount?.(meal.dish.id) ?? 0}
            onClick={() => onDishClick(meal.dish.id)}
          />
        ))}
      </Box>
    </Stack>
  );
}
