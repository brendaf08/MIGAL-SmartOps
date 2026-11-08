# LineasEditor

Tabla editable de líneas de detalle de un documento: producto, cantidad y, cuando corresponde, precio unitario, subtotal por línea y totales. Se usa en cotizaciones, ventas, órdenes de compra y órdenes de recepción.

## Uso

- `lineas` y `onChange`: la lista es controlada por el formulario (`{ id, sku, cantidad, precio }`).
- `productos`: opciones del catálogo (`{ sku, nombre }`); `sugeridos` ofrece primero ciertos SKU.
- `conPrecio` agrega las columnas Precio y Subtotal y los totales; `conDescuento` agrega la fila de descuento global en soles.
- `error` muestra el mensaje de la lista completa, por ejemplo "Agregar al menos un producto."
- Las funciones de `calculos.ts` (`subtotalLineas`, `totalLineas`, `descuentoValido`) son la única fuente de los importes; el formulario las reutiliza para validar.

## Reglas

- Los importes se redondean a dos decimales y el total nunca es negativo.
- El descuento no puede ser negativo ni superar el subtotal.
- Los totales se presentan con la doble línea contable y el total en negrita.
- Quitar una línea no requiere confirmación; los cambios se guardan solo al enviar el formulario.

## Comportamiento responsive

- Teléfono: cada línea es una ficha con sus campos a ancho completo y el botón "Agregar producto" a ancho completo.
- Tablet y escritorio: tabla con desplazamiento horizontal dentro de su contenedor si hace falta.

Equivalente en Mantine: `Table`, `NumberInput` y `Select`, compuestos en `DataTable`.
