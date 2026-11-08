import { NumberInput, type NumberInputProps } from '@mantine/core';

export type MoneyInputProps = Omit<
  NumberInputProps,
  'prefix' | 'decimalScale' | 'fixedDecimalScale' | 'thousandSeparator' | 'decimalSeparator'
>;

/** Campo de importes en soles: prefijo `S/ `, dos decimales fijos y separadores `es-PE` (`S/ 1,250.00`). */
export function MoneyInput({ min = 0, hideControls = true, ...props }: MoneyInputProps) {
  return (
    <NumberInput
      prefix="S/ "
      decimalScale={2}
      fixedDecimalScale
      thousandSeparator=","
      decimalSeparator="."
      allowNegative={false}
      min={min}
      hideControls={hideControls}
      inputMode="decimal"
      {...props}
    />
  );
}
