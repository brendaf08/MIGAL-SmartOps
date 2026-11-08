# Pantallas de inventario y movimientos

Este documento describe las pantallas de alertas de stock (P12), registro de movimiento (P13) y Kardex con filtros (P14) del módulo de inventario. Corresponde a los casos de uso CU-05 y CU-07 y a los requerimientos RF-KAR-02, RF-KAR-09 y RF-USR-04.

## Alcance por rol

| Pantalla | Ruta | Roles | Estado |
| --- | --- | --- | --- |
| P12 Alertas de stock | `/alertas` | Almacenero, Reponedor | Implementada |
| P13 Registrar movimiento | `/kardex/nuevo` | Almacenero | Implementada |
| P14 Movimientos y Kardex | `/kardex` | Almacenero | Implementada |

El control de acceso por rol se aplica en `src/routes/navigation.ts` y en `RequireRole`. Un Vendedor que accede a estas rutas se redirige a su página de inicio.

## P12. Alertas de stock

### Estructura

1. Encabezado "Alertas de stock" con una descripción breve.
2. Tres indicadores en fila: Críticos, En el mínimo y Total en alerta.
3. Barra de pestañas (Todas, Críticas, En el mínimo) con su recuento, y campo de búsqueda por nombre o SKU.
4. Tabla ordenada de mayor a menor gravedad. Columnas: Producto (con SKU), Libre, Mínimo, Faltante, barra de stock frente al mínimo, Nivel y Acciones.
5. Acciones por fila: "Ver disponibilidad" y, solo para el Reponedor, "Crear orden de compra", que lleva a `/compras/nueva?sku=<SKU>`.

### Estados

- Carga: filas de esqueleto.
- Sin alertas: estado vacío positivo, "No hay productos en alerta".
- Sin coincidencias con el filtro o la búsqueda: "Sin resultados", con la acción "Limpiar filtros".
- Error de consulta: aviso con título y sin lista.

### Reglas

- El nivel se calcula con `resumen(p).nivel` sobre el stock libre (físico menos reservado). Un libre de cero o menor es crítico; un libre igual o menor que el mínimo está en el mínimo.
- El faltante es el mínimo menos el libre.
- El orden es crítico primero y, dentro de cada nivel, mayor faltante primero.

## P13. Registrar movimiento

### Estructura

1. Tarjeta "Tipo de movimiento" con tres opciones: Entrada por ajuste, Salida por merma o ajuste y Traslado interno.
2. Tarjeta "Datos del movimiento": producto (selector buscable), existencias del producto por ubicación, almacén (entrada y salida) o almacenes de origen y destino (traslado), cantidad entera y motivo.
3. Barra de acciones al pie con el resumen de movimientos, el motivo del bloqueo y las acciones Cancelar y "Revisar movimiento".
4. Diálogo de confirmación con las consecuencias: número de movimientos en el Kardex y la irreversibilidad de la acción.
5. Constancia con el sello del tipo, la existencia antes y después por ubicación, y las acciones "Registrar otro movimiento" y "Ver movimientos en el Kardex".

### Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| Producto | Obligatorio | "Seleccionar un producto." |
| Almacén (entrada y salida) | Obligatorio | "Seleccionar el almacén." |
| Almacén de origen (traslado) | Obligatorio | "Seleccionar el almacén de origen." |
| Almacén de destino (traslado) | Obligatorio y distinto del origen | "El almacén de destino debe ser distinto del de origen." |
| Cantidad | Entero mayor que cero | "La cantidad debe ser mayor que cero." |
| Cantidad (salida y traslado) | No supera la existencia física del origen | "Stock insuficiente: hay N y se solicitan M." |
| Motivo | Al menos 10 caracteres | "Ingresar el motivo con al menos 10 caracteres." |

La validación de stock usa la existencia física de la ubicación de origen, no el stock libre. Cuando se supera, la acción principal queda deshabilitada y el motivo aparece en la barra de acciones y junto al campo de cantidad.

### Efectos

- Entrada y salida generan un movimiento con origen "Ajuste".
- El traslado genera dos movimientos vinculados con origen "Traslado": salida en el almacén de origen y entrada en el de destino, con la misma fecha, producto y cantidad.
- Las existencias del producto se actualizan en memoria, así como las alertas y la disponibilidad que las consultan.

## P14. Movimientos y Kardex

### Estructura

1. Encabezado con la acción principal "Registrar movimiento".
2. Filtros combinables: búsqueda por SKU o producto, tipo (Entrada, Salida), origen (Despacho, Recepción, Ajuste, Traslado), ubicación y rango de fechas (desde y hasta, inclusivos).
3. Etiquetas de los filtros activos, cada una con su acción de quitar, y acción "Limpiar filtros".
4. Tabla con las columnas Fecha, SKU, Producto, Movimiento, Origen, Ubicación y Responsable, con paginación de 20 filas.

### Comportamiento

- En tablet y escritorio los filtros se muestran en línea. En teléfono se agrupan en una hoja (`Modal`) que se abre con el botón "Filtros", con el número de filtros activos.
- El movimiento de signo negativo se muestra como "−" seguido de la cantidad, con la palabra del tipo; el origen se muestra con su texto ("Recepción" para el origen Recepcion).
- Un movimiento de traslado indica su par: "Hacia" el almacén de destino en la salida y "Desde" el de origen en la entrada.
- Tras registrar un movimiento, la fila recién creada se resalta al llegar al Kardex.
- La búsqueda se aplica tras una espera de 300 ms.

## Pruebas

Las pruebas se encuentran en `src/features/inventario/inventario.pantallas.test.tsx`. Cubren el orden y los contadores de alertas, el filtro por nivel, el botón de compra según el rol, los tres tipos de movimiento con sus bloqueos por stock, la validación del motivo y de los almacenes, los filtros combinables con sus etiquetas, el estado vacío, la restricción por rol y la verificación con `vitest-axe` en cada pantalla. La verificación de los puntos de quiebre (390, 768 y 1280 px) y de los temas se realiza de forma visual, no en las pruebas.

## Diferencias respecto a los requerimientos

- El motivo del movimiento se valida, pero el modelo de Kardex vigente no lo guarda. Queda pendiente agregarlo al tipo `Movimiento` cuando exista la API.
- El nivel de alerta se calcula sobre el stock libre. Esta decisión depende de la confirmación pendiente sobre si el stock mínimo se compara con el libre o con el físico.
- La barra de alertas usa la escala no compacta para mostrar la marca del mínimo, porque la variante compacta de `StockBar` no la muestra.
- El Kardex no tiene paginación en el servidor; la paginación es local sobre la lista filtrada, consistente con los datos simulados.
- El responsable de cada movimiento se toma del usuario de la sesión.
