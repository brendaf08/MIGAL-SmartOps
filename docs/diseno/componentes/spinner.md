# Spinner

Indicador circular de actividad para esperas menores a unos segundos (botones, notificaciones, consulta del agente).

## Uso

- `size` (px) y `label` (texto accesible; "Cargando" por omisión). Hereda el color del texto.
- Acompañar con un texto que diga qué se espera ("Consultando disponibilidad").

## Reglas

- Para tablas y paneles con estructura conocida preferir `Skeleton`, que evita saltos de diseño.
- Con `prefers-reduced-motion` la rotación se ralentiza.
- Esperas mayores a 10 s informan el avance o permiten cancelar.

Equivalente en Mantine: `Loader`.
