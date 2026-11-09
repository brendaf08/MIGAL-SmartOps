# Pantallas de órdenes de compra

Este documento describe las pantallas del módulo `compras` (CU-12, rol Reponedor): el listado de órdenes de compra (P25), el formulario de orden nueva (P26) y el detalle de una orden. La recepción de mercancía se realiza en el módulo de recepciones y no forma parte de este documento.

## Listado (`/compras`)

Estructura: encabezado "Órdenes de compra" con la acción "Nueva orden de compra", pestañas de filtro por estado, campo de búsqueda y tabla.

- Pestañas: Todas, Pendientes, Recepción parcial y Completadas. Cada pestaña muestra el recuento de órdenes de su estado.
- Búsqueda: filtra por número de orden o nombre de proveedor, sin distinguir mayúsculas.
- Columnas: Orden, Proveedor, Fecha, Productos, Avance de recepción (unidades recibidas sobre pedidas, con barra), Total y Estado.
- Acción: la fila completa abre el detalle de la orden.

Estados: `Pendiente`, `Parcial` y `Completada`, representados con `StatusBadge` (entidad `compra`).

## Formulario de orden nueva (`/compras/nueva`)

Estructura: encabezado con ruta de navegación, tarjeta "Proveedor" y tarjeta "Productos", barra de acciones al pie.

| Campo | Regla de validación | Mensaje |
| --- | --- | --- |
| Proveedor | Obligatorio. Los proveedores inactivos aparecen deshabilitados. | "Seleccionar el proveedor." |
| Fecha de la orden | Obligatoria, por omisión la fecha actual. No puede ser anterior a hoy. | "Ingresar la fecha de la orden." / "La fecha no puede ser anterior a hoy." |
| Productos | Al menos una línea. Cada línea exige un producto, una cantidad entera mayor que 0 y un precio mayor que 0. | "Agregar al menos un producto." / "Revisar cada línea: ..." |

Comportamiento:

- Un proveedor inactivo no es seleccionable. La tarjeta muestra un aviso con el enlace "Registrar proveedor", que conduce a `/proveedores/nuevo`.
- Al elegir un proveedor, sus productos suministrados se ofrecen primero en los selectores de producto.
- Con el parámetro `sku` en la URL, el formulario precarga una línea con ese producto. La cantidad propuesta es la necesaria para superar el stock mínimo y el precio es el último precio de compra registrado.
- La barra de acciones muestra el resumen "N productos, total S/ X". La acción "Guardar orden de compra" queda deshabilitada mientras falte el proveedor o las líneas, con el motivo visible en la barra.
- Al guardar, un diálogo de confirmación describe la orden. Al confirmar, la orden se crea en estado `Pendiente`, se muestra una notificación y se abre el detalle.
- La validación de campos se ejecuta al enviar; el foco pasa al primer campo inválido.

## Detalle (`/compras/:id`)

Estructura: encabezado con número de orden, sello de estado, proveedor y fecha; tabla de líneas; tarjeta con los datos del proveedor.

- Columnas de la tabla: SKU, Producto, Pedida, Recibida, Pendiente, Precio y Subtotal. El pie muestra el total de la orden.
- Acción "Registrar recepción": visible mientras la orden no esté `Completada`. Conduce a `/recepciones/nueva?oc=<número>`.
- Orden inexistente: aviso de error con enlace de regreso al listado.

## Roles

El módulo pertenece al rol Reponedor. Un usuario de otro rol que intente acceder a `/compras` es redirigido al inicio de su rol.

## Diferencias respecto a los requerimientos

- Las consultas y las mutaciones usan datos de prueba en memoria; la persistencia depende de la API del backend.
- La validación de fecha se realiza con el campo nativo de tipo fecha, sin selector de calendario de Mantine.
- Los importes se redondean a céntimos en cada línea y en el total.

## Verificación

Las pruebas del módulo se encuentran en `frontend/src/features/abastecimiento/compras.pantallas.test.tsx` y cubren el listado y su filtro, la restricción por rol, la creación de una orden, el bloqueo con motivo visible, la exclusión de proveedores inactivos, las validaciones, la precarga por URL, el detalle, la orden inexistente y el análisis de accesibilidad con axe en las tres pantallas.
