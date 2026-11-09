# Skeleton

Bloque animado que reserva el espacio del contenido mientras llega la respuesta del servidor.

## Uso

- `width`, `height`, `radius`; se compone con filas de texto, tarjetas y celdas (la tabla lo aplica al usar `loading`).
- Es decorativo (`aria-hidden`); el contenedor indica `aria-busy` mientras carga.

## Reglas

- La forma del esqueleto replica la del contenido final.
- No se usa cuando la espera es inferior a unos 300 ms.

Equivalente en Mantine: `Skeleton`.
