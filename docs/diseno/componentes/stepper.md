# Stepper

Indicador de avance de un proceso con pasos ordenados. Aplica a flujos con ciclo de vida definido: cotización a venta a despacho, y carga de documento a revisión a confirmación.

## Uso

- `steps`: `[{ label, description }]`, `current` (índice base 0), `aria-label`.
- Los pasos completados muestran una marca y se anuncian como "(completado)"; el paso actual lleva `aria-current="step"`.

## Reglas

- Es informativo; la navegación entre pasos se realiza con botones del formulario.
- Etiquetas con sustantivo o participio ("Validada"), no verbos.
- Máximo cinco pasos; con más se agrupa o se usa una lista de estados.

Equivalente en Mantine: `Stepper`.
