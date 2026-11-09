# ADR-003: Reglas de ventas, reservas, Kardex y despacho

## Contexto

Los documentos de diseño presentan reglas contradictorias sobre el momento en que se registra la salida de Kardex, la obligatoriedad del cliente y la atención de ventas con stock insuficiente. La [Matriz de trazabilidad](../matriz-trazabilidad.md) y la sección 7 de la [Metodología de diseño y estándares](../metodologia-diseno-y-estandares.md) identifican estas diferencias como pendientes de conciliación. Deben resolverse antes de diseñar el esquema de base de datos y la API, pues determinan estados, restricciones y transacciones.

Las contradicciones son las siguientes:

| Tema | Requerimientos y casos de uso | Arquitectura, modelo de clases y modelo de datos |
| --- | --- | --- |
| Salida de Kardex | RF-VEN-04 y RF-VEN-14 la generan al confirmar la venta o su atención parcial. | Se genera al confirmar el despacho (arquitectura tecnológica, apartado 2). |
| Cliente | RF-CLI-03, RF-CLI-04, RF-CLI-06, RF-VEN-01 y RF-VEN-02 lo definen como opcional. | `id_cliente` es obligatorio en `Cotizacion` y `Venta`. |
| Stock insuficiente | RF-VEN-12 contempla atención parcial con entrega parcial. | Reserva parcial con despacho completo único. |
| Origen de la venta | RF-VEN-09 permite registrarla directamente o desde una cotización. | Toda venta se origina en una cotización aceptada y validada (`id_cotizacion` único). |

El negocio es una ferretería de materiales de construcción con tienda principal y dos almacenes. La entrega al cliente se realiza con transporte propio, y la gestión de flota, ruteo y seguimiento GPS está fuera del alcance (véase [Alcance y límites](../alcance-y-limites.md)). Los criterios de evaluación son: coherencia con la arquitectura vigente, integridad del inventario, simplicidad operativa y trazabilidad.

## Decisión

1. **Salida de Kardex.** Los movimientos de salida de Kardex se registran únicamente al confirmar la salida física del despacho. La aprobación de una venta solo genera reservas de stock. La recepción confirmada genera las entradas.
2. **Cliente obligatorio.** Toda cotización y toda venta se asocian a un cliente registrado. Las operaciones de mostrador utilizan un registro de cliente cuyo documento de identificación es opcional, como lo define el modelo de datos.
3. **Reserva parcial y despacho completo.** Cuando el stock libre es insuficiente, la venta aprobada reserva las cantidades disponibles por ubicación y registra el resto como pendiente de abastecimiento. El despacho es único y completo por venta y solo se autoriza cuando todas las cantidades están reservadas. No existe despacho parcial.
4. **Entrega anticipada de una parte del pedido.** Cuando el cliente requiere recibir antes una parte del pedido, la cotización se divide en dos cotizaciones, cada una con su propia venta, reservas y despacho. Se conserva así la relación uno a uno entre cotización y venta.
5. **Origen único de la venta.** Toda venta se origina en una cotización aceptada por el cliente y validada por MIGAL. El flujo de venta directa de la interfaz registra la cotización, su aceptación y su validación como un solo paso transaccional, sin exigir digitación duplicada.
6. **Cancelación.** Una venta puede cancelarse antes del despacho con motivo obligatorio. La cancelación libera las reservas activas y no genera movimientos de Kardex.
7. **Estados de la venta.** Pendiente de abastecimiento, pendiente de despacho, lista para despacho, despachada y cancelada, como define el modelo de datos.
8. **Ajustes manuales de inventario.** Las mermas, correcciones y traslados registrados mediante el movimiento manual de Kardex (CU-05) no provienen de una venta. Requieren motivo obligatorio y usuario responsable, y solo pueden reducir el stock libre, es decir, el stock físico que no se encuentra reservado. La regla 1 se aplica a las salidas de origen comercial: ninguna venta genera salidas fuera del despacho.

## Justificación

- **Salida en el despacho:** coincide con la regla de la arquitectura tecnológica y evita movimientos de salida de mercadería que aún se encuentra en el almacén. Separa el compromiso comercial (reserva) del hecho físico (Kardex).
- **Cliente obligatorio:** permite el historial por cliente (RF-CLI-05, RF-CLI-08) y simplifica las restricciones de integridad. El costo de registrar un cliente de mostrador es bajo, pues solo el nombre es obligatorio.
- **Despacho completo:** el material de construcción es pesado y voluminoso, y se transporta con recursos propios. Fraccionar un pedido multiplica los viajes, y la gestión de flota queda fuera del alcance. Un despacho único también evita estados intermedios de entrega que complican el Kardex y las cancelaciones.
- **Reserva parcial:** conserva la intención de RF-VEN-12 de atender lo disponible, pues compromete de inmediato las unidades existentes y evita que otra venta las consuma.
- **División de cotizaciones:** cubre la necesidad de entregas escalonadas sin introducir una entidad de entrega parcial.
- **Origen único:** reduce el modelo a una sola ruta de creación, lo que simplifica la API y las pruebas.

La interpretación de reserva parcial sin entrega parcial debe confirmarse frente a las observaciones académicas de PC2. En caso de rechazo, se registra un nuevo ADR que reemplace el punto 3 y se revisa el modelo de despacho.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Salida de Kardex al confirmar la venta | Descuenta existencias físicas que aún no salen del almacén y obliga a reversiones al cancelar. |
| Cliente opcional con ventas anónimas | Impide el historial por cliente y admite registros sin identificación, lo que debilita la trazabilidad. |
| Despacho parcial por venta | Introduce entregas fragmentadas, estados adicionales, múltiples guías por venta y gestión de rutas fuera del alcance. |
| Venta directa sin cotización | Mantiene dos rutas de creación con reglas distintas y contradice la relación uno a uno con la cotización. |
| Entidad Pedido independiente | Duplica la venta aprobada y no aporta información adicional al modelo. |

## Consecuencias

**Favorables:**

- Un solo conjunto de reglas para requerimientos, modelo de datos, esquema y API.
- Kardex refleja únicamente movimientos físicos reales, y las cancelaciones no requieren reversiones.
- El control de concurrencia se concentra en la reserva y en la confirmación del despacho.

**Desfavorables:**

- Los clientes que necesitan entregas escalonadas requieren dividir la cotización. Se mitiga ofreciendo esta acción en la interfaz.
- Los requerimientos RF-CLI-03, RF-CLI-04, RF-CLI-06, RF-VEN-01, RF-VEN-02, RF-VEN-04, RF-VEN-09, RF-VEN-12, RF-VEN-14, RF-VEN-15, RF-VEN-17, RF-VEN-18 y RNF-15, y las especificaciones de CU-09 y CU-10, deben actualizarse. Se realiza en un pull request posterior.
- El esquema final puede ajustar el modelo de datos. Estos ajustes se registran en ADR-005 y se reflejan en el diagrama entidad-relación.

## Referencias

- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Matriz de trazabilidad](../matriz-trazabilidad.md)
- [Metodología de diseño y estándares](../metodologia-diseno-y-estandares.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
- [Modelo y diccionario de clases](../modelo-y-diccionario-clases.md)
- [Requerimientos](../requerimientos.md)
