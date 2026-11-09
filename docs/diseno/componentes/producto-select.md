# ProductoSelect y MoneyInput

Dos campos especializados que evitan repetir configuración en cada formulario.

## ProductoSelect

Selector buscable de productos del catálogo. La etiqueta de cada opción es `SKU, nombre` y la lista desplegable tiene un ancho propio de 360 px para que las opciones no se partan en varias líneas cuando el campo es angosto.

- `productos`: lista de `{ sku, nombre }`. El componente no conoce el catálogo; en la aplicación se obtiene con `opcionesProducto()` de `features/catalogo/opciones.ts`.
- `value` y `onChange`: el SKU elegido o `null`.
- `sugeridos`: SKU que se muestran primero, por ejemplo los que suministra el proveedor elegido.
- Acepta las demás propiedades de `Select` (`label`, `error`, `aria-label`).

## MoneyInput

Campo de importes en soles con prefijo `S/ `, dos decimales fijos, miles con coma y punto decimal (`S/ 1,250.00`), sin botones de aumento y sin valores negativos. Acepta las propiedades de `NumberInput` salvo las de formato.

## Reglas

- Todo campo de importe usa `MoneyInput`; todo selector de producto usa `ProductoSelect`.
- Cada campo lleva etiqueta visible o `aria-label`; el error se muestra junto al campo.

Equivalente en Mantine: `Select` y `NumberInput`.
