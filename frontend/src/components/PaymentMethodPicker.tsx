import { FormControl, FormControlLabel, FormLabel, Radio, RadioGroup } from '@mui/material';
import type { PaymentMethod } from '../api/types';
import { paymentMethodLabels } from '../lib/labels';

export function PaymentMethodPicker({ value, onChange }: {
  value: PaymentMethod;
  onChange: (method: PaymentMethod) => void;
}) {
  return (
    <FormControl>
      <FormLabel id="payment-method">Mokėjimo būdas *</FormLabel>
      <RadioGroup
        aria-labelledby="payment-method"
        value={value}
        onChange={(e) => onChange(e.target.value as PaymentMethod)}
      >
        {(Object.keys(paymentMethodLabels) as PaymentMethod[]).map((m) => (
          <FormControlLabel key={m} value={m} control={<Radio />} label={paymentMethodLabels[m]} />
        ))}
      </RadioGroup>
    </FormControl>
  );
}
