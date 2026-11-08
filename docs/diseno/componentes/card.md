# Card

Contenedor de contenido relacionado, con encabezado opcional (título, descripción y acciones), cuerpo y pie.

## Uso

- `title`, `titleId` (enlaza `aria-labelledby`), `level` (nivel del encabezado, 3 por omisión; ajustar para no saltar niveles), `description`, `actions`, `footer`, `flat` (sin sombra), `padded={false}` para contenido que ocupa todo el ancho (tablas).
- Es la unidad de composición de paneles de detalle y formularios; contiene `Table`, `Stack` de campos o indicadores.

## Reglas

- Una tarjeta agrupa una sola idea; el título la nombra.
- No anidar más de un nivel: dentro de una tarjeta usar `flat`.
- Las acciones del encabezado se limitan a una o dos; las acciones del flujo (confirmar, cancelar) van en el pie.

Equivalente en Mantine: `Paper` o `Card` con `withBorder`.

## Comportamiento responsive

- Teléfono: relleno de 16 px en lugar de 20 px.
