# Button

Acción que el usuario dispara. Variantes: `primary` (acción principal de la vista), `secondary` (acción relevante de menor énfasis), `outline` (acciones neutrales y cancelación), `subtle` (acciones terciarias dentro de tablas y tarjetas), `danger` (acciones irreversibles) y `accent` (verde de marca, exclusivo para superficies oscuras y el agente de IA).

## Uso

- `variant`, `size` (`sm` 32 px, `md` 40 px, `lg` 48 px), `leftIcon` / `rightIcon` (nombre de ícono), `loading`, `disabled`, `fullWidth`; el resto de atributos se transmiten al `<button>`.
- Etiqueta con verbo en infinitivo y objeto, en minúscula salvo la primera letra: "Registrar venta", "Confirmar recepción", "Generar orden de despacho".
- Una sola acción `primary` por vista o por diálogo. En un par de acciones, `primary` a la derecha y `outline` (cancelar) a su izquierda.

## Reglas

- `loading` conserva el ancho del botón, anuncia `aria-busy` y bloquea nuevos clics; se aplica a toda acción que espera respuesta del servidor (visibilidad del estado del sistema).
- Las acciones que modifican inventario o cancelan una operación exigen confirmación mediante `ConfirmDialog`; el botón confirmatorio usa `danger` cuando la acción no se puede deshacer.
- En tablets y flujos de almacén usar `size="lg"` en la acción principal (objetivo táctil de 48 px).
- `accent` nunca se coloca sobre fondos claros: el verde neón tiene 1.4:1 de contraste con el blanco.
- Un botón deshabilitado debe ir acompañado de una explicación visible cuando el motivo no es evidente.

Equivalente en Mantine: `Button` con `variant` `filled` (primary), `light` (secondary), `default` (outline), `subtle`, y `color="danger"` o `color="accent"` definidos en el tema.

## Comportamiento responsive

- Teléfono: altura mínima de 44 px; en cabeceras y diálogos las acciones se expanden a ancho completo. Usar `size="lg"` en la acción principal de flujos que se operan con una mano.
