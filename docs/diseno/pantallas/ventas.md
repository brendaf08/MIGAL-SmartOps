# Pantallas de ventas: lista y detalle

Documenta las pantallas P19 (lista de ventas) y P21 (detalle y completar venta) del módulo `ventas`. Corresponden al caso de uso CU-10, en sus flujos alternativos de atención parcial y venta pendiente, y al requerimiento RF-VEN-12. El registro de una venta nueva (P20) se documenta en la rama del plan 08.

## Ruta y rol

| Pantalla | Ruta | Rol |
| --- | --- | --- |
| Lista de ventas (P19) | `/ventas` | Vendedor |
| Detalle de la venta (P21) | `/ventas/:id` | Vendedor |

La restricción por rol se toma de la navegación del módulo. Un rol no autorizado es redirigido a su pantalla de inicio.

## Estructura de la lista (P19)

1. Encabezado "Ventas", con la acción principal "Registrar venta", que conduce a `/ventas/nueva`.
2. Barra de herramientas: pestañas por estado con su contador, y un campo de búsqueda.
3. Tabla "Ventas" con las columnas Venta, Fecha, Cliente, Entrega, Total y Estado.

Las pestañas son Todas, Pendiente de abastecimiento, Pendiente de despacho, Lista para despacho, Despachada y Cancelada. El contador de cada una cuenta las ventas de ese estado, sin considerar la búsqueda.

La búsqueda filtra por número de venta o por nombre del cliente, sin distinguir mayúsculas. Un clic en una fila abre el detalle de la venta.

Una venta activa con unidades pendientes muestra, bajo su modalidad de entrega, un aviso con el texto "Pendiente: N unidades".

## Estructura del detalle (P21)

1. Encabezado con el número de la venta, la insignia de estado en variante sello, el cliente y la fecha.
2. Tarjeta "Productos" con la tabla de líneas (SKU, Producto, Solicitada, Atendida, Pendiente y Subtotal) y tres filas de totales: subtotal, descuento y total.
3. Aviso de advertencia cuando hay unidades pendientes. Explica la atención parcial con las cifras atendidas y solicitadas, y muestra la disponibilidad de cada producto pendiente con una barra de stock compacta.
4. Barra de acciones al pie, cuando la venta está activa. Presenta el resumen de unidades pendientes, el motivo de bloqueo y las acciones "Cancelar venta" y "Completar venta".
5. Diálogos de confirmación para completar y para cancelar.
6. Constancia, que aparece solo como resultado de completar la atención en esta pantalla.

## Estados

| Estado de la venta | Lista | Detalle |
| --- | --- | --- |
| Pendiente de abastecimiento | Pestaña propia; muestra pendientes | Aviso de atención parcial; acciones completar y cancelar |
| Pendiente de despacho | Pestaña propia; muestra pendientes si los tiene | Aviso si hay pendientes; acciones completar y cancelar |
| Lista para despacho | Pestaña propia; sin pendientes | Sin aviso; acción cancelar |
| Despachada | Pestaña propia; sin pendientes | Sin acciones |
| Cancelada | Pestaña propia; sin pendientes | Sin acciones |

Una venta cancelada o despachada no muestra unidades pendientes, aunque conserve la diferencia entre lo solicitado y lo atendido en sus datos.

## Reglas de atención

- Completar la venta atiende, por cada línea con pendientes, la menor cantidad entre lo pendiente y el stock libre del producto. Nunca atiende más que lo solicitado ni más que el stock.
- Si después de completar no quedan pendientes, la venta pasa a "Lista para despacho". En otro caso conserva su estado.
- Si ninguna línea tiene stock libre, la acción "Completar venta" queda deshabilitada y la barra de acciones muestra el motivo "Sin stock disponible para las unidades pendientes.".
- La constancia muestra la existencia libre antes y después de la atención de cada producto, lo atendido en esta operación y las unidades que quedan pendientes.

## Reglas de cancelación

- Cancelar una venta exige confirmación en un diálogo de tono peligroso, que enuncia la liberación de reservas y la irreversibilidad de la acción. El foco inicial queda en "Volver".
- La cancelación solo está disponible en los estados Pendiente de abastecimiento, Pendiente de despacho y Lista para despacho.
- Al cancelar, la venta pasa a "Cancelada" y las unidades que tenía atendidas dejan de estar reservadas.

## Validaciones y mensajes

| Situación | Tratamiento |
| --- | --- |
| Venta inexistente | Aviso "La venta no existe", con enlace al listado. |
| Completar sin stock | Acción deshabilitada con el motivo visible en la barra de acciones. |
| Completar sin pendientes | La acción no aparece. |
| Error del servidor al completar o cancelar | Notificación con el mensaje de error; la venta no cambia. |

## Datos y simulación

Las operaciones se realizan con `mockRequest`, en `api.ts` del módulo. Los datos de ventas se modifican en memoria. El stock libre se calcula con la existencia de los productos de prueba del módulo de inventario, que no se modifica: las reservas que generan completar y cancelar se registran como ajustes en memoria del módulo de ventas. Al integrar la API real, el servidor devolverá la existencia y las reservas, y estos ajustes se retirarán.

## Diferencias respecto a los requerimientos

- La atención parcial sigue el modelo de RF-VEN-12 en lo que respecta a las cantidades pendientes; los nombres de los estados están pendientes de armonizar, según el mapa central de estados.
- El stock libre se toma de la existencia consolidada del producto, no de la ubicación. La decisión sobre el stock mínimo, libre o físico, sigue pendiente.
- Completar no genera movimientos de Kardex en esta versión; la salida de mercadería corresponde al despacho (CU-06).
