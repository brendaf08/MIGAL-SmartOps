# Pantallas de despachos

Este documento describe las pantallas de despachos del frontend (P15 y P16), su estructura, sus estados, sus validaciones y sus diferencias respecto a los requerimientos. Corresponde al caso de uso CU-06 y al requerimiento RF-DES-01, con el rol Almacenero.

## Lista de despachos (P15)

Ruta: `/despachos`. Componente: `features/despacho/DespachosPage.tsx`.

### Estructura

1. Encabezado "Despachos".
2. Pestañas con recuento: "Pendientes de despacho" (estado `Emitida`), "En preparación" (`Preparacion`), "Salida confirmada" (`SalidaConfirmada`) y "Entregadas" (`Entregada`).
3. Campo de búsqueda por orden, venta o cliente, aplicado a la pestaña activa.
4. Tabla de órdenes con las columnas Orden, Venta, Cliente, Entrega, Productos y Estado. La columna Productos indica el número de líneas y, cuando la reserva no cubre la cantidad, aparece aviso "Reserva incompleta" con el ícono de triángulo.
5. Clic en una fila o tecla Enter sobre ella abre la ficha `/despachos/:id`.
6. Tarjeta "Ventas listas para generar orden" con las ventas en estado `ListaDespacho`, su cliente, su fecha y la orden existente, si la tienen. Cada fila ofrece la acción "Generar orden de despacho".

### Generación de la orden

- Para una venta sin orden, el diálogo de confirmación muestra el cliente, la dirección de entrega y las líneas a despachar. Al confirmar, la orden se crea en estado `Emitida` con un código correlativo `D-` y se abre su ficha. La venta conserva su estado.
- Para una venta que ya tiene orden, el diálogo "La venta ya tiene la orden D-00XX" exige el campo "Motivo de la reimpresión". El motivo no puede estar vacío; se valida con esquema Zod y el mensaje de error aparece junto al campo. Al confirmar, el motivo se registra y se abre la orden imprimible.

### Estados

| Estado | Presentación |
| --- | --- |
| Carga | Esqueletos en la tabla |
| Sin resultados | Estado vacío predeterminado de la tabla |
| Error | Aviso "No se pudieron cargar las órdenes" |
| Generación fallida | Notificación con el motivo del servidor |

### Validaciones

- El motivo de reimpresión es obligatorio.
- La generación exige que la venta esté en estado `ListaDespacho` y que no tenga orden previa.

## Orden de despacho imprimible (P16)

Ruta: `/despachos/:id/imprimir`. Componente: `features/despacho/ordenes/OrdenImprimiblePage.tsx`, con estilos en `OrdenImprimible.module.css`.

### Estructura

1. Barra de acciones, fuera del documento, con los botones "Imprimir" y "Volver a la orden". "Imprimir" invoca `window.print()`.
2. Documento en una hoja: logotipo y nombre del negocio, título "Orden de despacho" con el número de orden, datos de fecha, venta, cliente y dirección de entrega, tabla de productos (SKU, producto, cantidad y unidad) con el total de líneas, y tres espacios de firma: Despachó, Chofer y Recibió.

### Estilos

- El documento usa la paleta fija de tokens (escalas `brand` y `gray`), que no cambia con el tema de pantalla.
- El bloque `@media print` fija tamaño A4, márgenes de 15 mm, fondo blanco y texto negro. Oculta todo salvo la hoja y la barra de acciones. Sus valores literales están explicados con un comentario.

### Diferencias respecto a los requerimientos

- La orden no registra fecha propia: el documento muestra la fecha de la venta.
- El modelo de cliente no contiene dirección: se usa una dirección de prueba por cliente, con el texto "Dirección no registrada" como respaldo.
- El total del documento indica el número de líneas. No suma cantidades, porque cada producto tiene una unidad distinta.
- El enlace "Imprimir orden" en la ficha `DespachoPage` se incorpora en la rama de integración.

## Pruebas

El archivo `features/despacho/despachos.pantallas.test.tsx` cubre el agrupamiento por pestaña, el aviso de reserva incompleta, la generación de orden, la exigencia de motivo en la reimpresión, la restricción por rol, la accesibilidad con `vitest-axe` y los datos y la impresión del documento.
