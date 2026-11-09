# Pantallas del catálogo de productos

Este documento describe las pantallas del módulo `catalogo` (CU-03, RF-PRO-05 y requerimientos de catálogo). Roles con acceso: Almacenero y Administrador. Código: `frontend/src/features/catalogo/`.

## Lista de productos (P08)

Ruta: `/catalogo`.

Estructura:

1. Encabezado "Catálogo" con la acción "Registrar producto", que lleva a `/catalogo/nuevo`.
2. Barra de filtros: búsqueda por SKU o nombre (`SearchInput`), selector de categoría y selector de estado ("Todos", "Activo", "Inactivo").
3. Tabla "Catálogo de productos" con las columnas SKU, Producto (con categoría), Unidad, Stock mínimo y Estado. En teléfono se omite la unidad.
4. Paginación de 20 productos por página.

Comportamiento:

- La búsqueda se aplica con una espera de 300 ms tras la última tecla.
- Cambiar un filtro devuelve la lista a la primera página.
- Activar una fila (clic, Enter o Espacio) abre la ficha del producto.
- Sin coincidencias, la tabla muestra el estado vacío "Sin resultados".

Estados: carga (esqueletos en la tabla), error (aviso con el título "No se pudo cargar el catálogo") y resultado vacío.

## Registro y modificación de producto (P09)

Rutas: `/catalogo/nuevo` (registro) y `/catalogo/:sku/editar` (modificación). Ambas usan `FormPage` y el esquema `productoSchema` de `esquema.ts`.

Campos:

| Campo | Regla |
| --- | --- |
| SKU | Obligatorio. Se normaliza a mayúsculas y debe cumplir el formato AAA-000. Único. En modificación es de solo lectura. |
| Nombre oficial | Obligatorio. Mínimo 3 caracteres. |
| Descripción | Opcional. Máximo 300 caracteres. |
| Categoría | Obligatoria. Selector con las categorías del catálogo. |
| Unidad de medida | Obligatoria. Valores: bolsa, unidad, varilla, rollo, balde, m³ y kg. |
| Stock mínimo | Obligatorio. Entero mayor o igual a 0. Muestra la vista previa del efecto sobre las alertas. |
| Estado | Interruptor Activo o Inactivo. Por omisión, activo. |

Validaciones y mensajes, junto al campo:

- Formato de SKU incorrecto: "Usar el formato AAA-000, por ejemplo CEM-049."
- SKU ya registrado: "El SKU ya existe. Ingresar un código distinto." El guardado queda bloqueado hasta cambiar el código.
- Nombre con menos de 3 caracteres: "Ingresar al menos 3 caracteres."
- Descripción con más de 300 caracteres: "La descripción no puede superar 300 caracteres."
- Categoría o unidad sin seleccionar: "Seleccionar la categoría." o "Seleccionar la unidad de medida."
- Stock mínimo vacío: "Ingresar el stock mínimo." Negativo o no entero: mensajes específicos del mismo campo.

Resultado: el registro muestra la notificación "Producto registrado" y abre la ficha; la modificación muestra "Cambios guardados" y abre la ficha. "Cancelar" vuelve a la ficha o a la lista, según corresponda, sin guardar cambios.

Las acciones no son irreversibles, por lo que no pasan por `ConfirmDialog`.

## Ficha de producto (P10)

Ruta: `/catalogo/:sku`.

Estructura:

1. Encabezado con el nombre del producto como título, el SKU en fuente monoespaciada, el estado mediante `StatusBadge` y la acción "Modificar", que lleva a `/catalogo/:sku/editar`.
2. Tarjeta "Datos del producto" en lista de definición: categoría, unidad de medida, stock mínimo y descripción.
3. Tarjeta "Existencias" con `StockSummary`, `StockBar` y `StockLocations`, calculados con las funciones `resumen` y `ubicacionesDe` del módulo de inventario. Las cifras coinciden con la pantalla de disponibilidad.

La ficha no contiene controles de edición en línea; la modificación ocurre solo en el formulario.

SKU inexistente: aviso "El producto no existe" con un enlace de regreso al catálogo.

## Restricción por rol

Las rutas `/catalogo`, `/catalogo/nuevo`, `/catalogo/:sku` y `/catalogo/:sku/editar` solo son accesibles para Almacenero y Administrador. Un Vendedor que accede a ellas se redirige a la pantalla de inicio de su rol.

## Datos

Los datos son de prueba y se gestionan en memoria en `api.ts`. El registro agrega el producto al arreglo compartido de productos, de modo que la disponibilidad y los selectores de productos de otros módulos lo reflejan. La descripción y el estado se conservan por SKU fuera de ese arreglo. Un producto nuevo se registra sin existencias; las cantidades se incorporan mediante recepciones.

## Diferencias respecto a los requerimientos

- El estado de un producto no figura en el modelo de datos de inventario; se conserva en el módulo de catálogo hasta que el backend lo defina.
- Se supone que el stock mínimo se compara con el stock libre, según la vista previa del formulario. El criterio está pendiente de confirmar (stock libre o físico).

## Pruebas

Las pruebas de estas pantallas están en `frontend/src/features/catalogo/catalogo.pantallas.test.tsx`. Cubren el listado con paginación, búsqueda y filtros, el registro y las validaciones, la modificación con SKU de solo lectura, la ficha, la restricción por rol y la verificación de accesibilidad con axe.
