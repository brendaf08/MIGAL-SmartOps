# Table

Tabla de datos semántica (`table`, `th scope`, `aria-sort`) con ordenamiento, selección, estado de carga con esqueletos y estado vacío. Es el contenedor de los listados de catálogo, inventario, Kardex, cotizaciones, ventas, órdenes y recepciones.

## Uso

- `columns`: lista de `{ key, header, align, width, numeric, mono, sortable, render }`. `numeric` alinea a la derecha y activa cifras tabulares; `mono` presenta SKU y documentos en fuente monoespaciada.
- `rows` y `rowKey` (nombre de campo o función); `sort` y `onSort(key, dir)` para el orden controlado; `loading`; `empty` (nodo, por omisión `EmptyState`); `onRowClick` y `selectedKey` para filas navegables (activables con Enter y Espacio); `density="sm"` para listados densos; `stickyHeader`; `caption` (texto accesible); `footer` (objeto `{ columna: contenido }` o lista de objetos) para totales.

## Reglas

- Una columna de cantidades se alinea a la derecha, con la unidad en el encabezado o junto al valor; las cantidades usan `formatNumber` y los importes `formatCurrency` (`S/ 1,250.00`).
- Máximo siete columnas visibles en tablet; el resto se mueve al detalle de la fila. Con ancho insuficiente la tabla se desplaza horizontalmente sin romper la fila.
- El estado se muestra con `StatusBadge` y nunca solo con color de fila.
- Mientras se consulta el servidor se usa `loading` (esqueletos); una consulta sin resultados usa `empty` con una acción que permita recuperarse (limpiar filtros).
- El ordenamiento, filtrado y paginación avanzados dependen de una librería complementaria (ADR-001); la API de `Table` es la interfaz estable y la implementación puede apoyarse en TanStack Table.

Equivalente en Mantine: `Table` de `@mantine/core` con TanStack Table para ordenamiento y paginación del lado del servidor.

## Otras propiedades

- `changedKey`: la fila con esa clave pasa de `brand-soft` a transparente durante 1.2 s, para mostrar qué dato cambió tras una acción (reserva, despacho, recepción). Se resalta como máximo una fila por acción y no se anima en la carga inicial.
- Columnas: `phoneOnly` muestra la columna solo en las fichas de teléfono (por ejemplo, la celda de ubicaciones).

## Comportamiento responsive

- Teléfono: cada fila se presenta como una ficha (`stack`, activo por omisión). El título de la ficha es la columna `primary`; las columnas marcadas con `phone: false` se omiten y las marcadas con `block: true` muestran su etiqueta sobre el control, a ancho completo. Los totales de `footer` conservan su doble línea.
- Tablet: la tabla conserva todas las columnas con desplazamiento horizontal dentro de su contenedor.
- Escritorio: tabla completa.
- Desactivar `stack` solo en tablas de comparación cuyo contenido no tiene sentido como ficha.
