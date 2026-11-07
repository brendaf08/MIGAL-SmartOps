# Badge

Etiqueta de una línea que clasifica un registro o un estado. Es la base de `StatusBadge` y `MovementBadge`.

## Uso

- `tone`: `neutral`, `brand`, `success`, `warning`, `danger`, `info` o `accent`; `icon`: nombre de ícono (16 px); el contenido es texto corto.
- Para estados de entidades del dominio (cotización, venta, guía, compra, recepción, documento, reserva, stock) usar `StatusBadge`, que centraliza el tono, el texto y el ícono.

## Reglas

- El color nunca es el único portador del significado: cada insignia lleva texto y, en los tonos semánticos, ícono.
- Texto breve (máximo tres palabras) sin punto final. Los roles (`Administrador`, `Vendedor`, `Almacenero`, `Reponedor`) usan tono `brand`.
- `accent` se reserva para elementos del agente de IA sobre superficies oscuras o con borde propio.

Equivalente en Mantine: `Badge` con `variant="light"` y colores semánticos del tema.
