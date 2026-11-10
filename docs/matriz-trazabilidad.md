# Matriz de trazabilidad

La matriz de trazabilidad de MIGAL SmartOps permite establecer la correspondencia entre los requerimientos funcionales y no funcionales, los casos de uso y los principales componentes responsables de su realización.

Su propósito es facilitar la verificación de coherencia entre las especificaciones funcionales, los diagramas de actividades y secuencia, el modelo de clases, el modelo de datos, los Diagramas de Flujo de Datos (DFD) y la arquitectura tecnológica.

La matriz considera los requerimientos incorporados durante las correcciones de PC2, incluyendo la gestión independiente de clientes y proveedores, la atención de ventas con stock insuficiente y el control de las operaciones de abastecimiento. Las reglas de ventas, reservas, Kardex y despacho se establecen en el [ADR-003](adr/adr-003-reglas-ventas-reservas-despacho.md), que la matriz aplica a los requerimientos afectados. Las decisiones sobre el modelo de datos y el contrato de la API se establecen en los ADR [007](adr/adr-007-hosting-base-de-datos-supabase.md), [008](adr/adr-008-ajustes-contrato-api.md) y [009](adr/adr-009-ajustes-seguridad-y-componentes-contrato.md).

## Matriz de trazabilidad de requerimientos

| Requerimientos | CU | Realización principal | Punto que debe verificarse |
| --- | --- | --- | --- |
| RF-USR-01–04 | CU-01/02 | Usuario; UsuarioService; Gestión de Usuarios (7.0) | Autenticación, permisos por rol, vigencia de usuarios y trazabilidad de operaciones. |
| RF-PRO-01–06 | CU-03/04 | Producto; Categoria; ProductoService; Gestión de Catálogo (1.0) | SKU único, atributos obligatorios, jerarquía de categorías de dos niveles, actualización y consulta de productos. |
| RF-VEN-09; RF-VEN-11; RF-COT-09 | CU-09/10 | Categoria; Producto; CotizacionService; VentaService | Viabilidad de abastecimiento según el indicador `aprobacionAutomatica` de la categoría efectiva del producto. En la venta directa con categorías mixtas se exige el rol Administrador (ADR-009, decisión 8). |
| RF-KAR-01–02/05–08/14 | CU-04/05 | Inventario; MovimientoKardex; KardexService; Control de Inventario (3.0) | Existencias por almacén, stock mínimo, traslados, ajustes con motivo que solo afectan el stock libre y prevención de stock negativo. |
| RF-KAR-09–10/12 | CU-07/14 | Inventario; Producto; KardexService; AgenteConsultivoService | Alertas de stock mínimo, cálculo determinista del punto de reorden y consultas históricas con filtros opcionales por producto, ubicación, tipo de operación, origen y fecha. |
| RF-KAR-03/11 | CU-05/13 | OrdenRecepcion; DetalleRecepcion; MovimientoKardex | Recepción registrada en estado pendiente sin movimientos, entradas únicamente al validarla en una sola transacción y trazabilidad de la operación de origen. |
| RF-KAR-04 | CU-06/10 | Venta; GuiaDespacho; MovimientoKardex; DespachoService | La salida de Kardex se origina en la confirmación del despacho físico y no en el registro comercial de la venta (ADR-003, decisión 1). |
| RF-KAR-13; RF-DES-01–06 | CU-06 | GuiaDespacho; Venta; DespachoService; Gestión de Despacho (6.0) | Emisión, impresión, dirección de entrega, estados (incluida la anulación cuando la venta se cancela en lista para despacho), vínculo con la venta, fecha de entrega y prevención de salidas duplicadas. |
| Reimpresión de guía (sin RF propio en requerimientos) | CU-06 (flujo alternativo 1 del contrato) | ReimpresionGuia; GuiaDespacho; DespachoService | Motivo obligatorio, registro de solo anexión y ausencia de efectos sobre el estado de la guía y el inventario. |
| RF-OCR-01–08; RF-FUZ-01–07 | CU-08/04/09/13 | DocumentoDigital; DetalleExtraido; Producto; OcrIAIntegratorService; Procesamiento OCR (2.0) | Formatos admitidos, extracción, coincidencia con SKU, corrección manual y validación previa al registro definitivo. |
| RF-COT-01–04/06/08 | CU-08/09 | Cotizacion; DetalleCotizacion; CotizacionService; Gestión Comercial (4.0) | Emisión, importes, descuentos, vigencia, precarga OCR y ausencia de modificaciones de inventario durante la cotización. |
| RF-COT-05/07/09 | CU-09/10 | Cotizacion; Venta; CotizacionService; VentaService | Estados, aceptación del cliente, validación de viabilidad y conversión única de cotización a venta. |
| RF-COT-05/08 | CU-09 | Cotizacion; CotizacionService; Usuario | Rechazo por origen cliente (desde emitida) o MIGAL (desde aceptada, solo Administrador), con usuario, fecha y motivo no vacío, sin efecto sobre existencias (ADR-009, decisión 7). |
| RF-CLI-01–02/05/07–08 | CU-09/10 | Cliente; ClienteRepository; Cotizacion; Venta | Registro, consulta, estado e historial de cotizaciones y ventas asociadas a cada cliente. |
| RF-CLI-03–04/06; RF-VEN-01–02 | CU-09/10 | Cliente; Cotizacion; Venta; ClienteRepository | Asociación obligatoria de un cliente registrado en cotizaciones y ventas, con registro de mostrador de documento opcional (ADR-003, decisión 2). |
| RF-VEN-03/05–08/10–11/15–16 | CU-09/10 | Venta; DetalleVenta; Inventario; ReservaStock; VentaService | Disponibilidad, productos comprometidos, descuentos, registro de pendientes, trazabilidad y control de stock sin salidas anticipadas. |
| RF-VEN-04/09 | CU-06/09/10 | Cotizacion; Venta; GuiaDespacho; VentaService; DespachoService | Origen único de la venta en una cotización aceptada y validada, y salida de Kardex solo al confirmar el despacho (ADR-003, decisiones 1 y 5). |
| RF-VEN-12–14/17–18 | CU-06/10 | DetalleVenta; ReservaStock; Inventario; MovimientoKardex | Reserva parcial por ubicación, pendiente de abastecimiento, despacho único y completo, y trazabilidad hasta el movimiento de salida (ADR-003, decisión 3). |
| RF-VEN-19 | CU-06/10 | Venta; GuiaDespacho; ReservaStock; VentaService; DespachoService | Cancelación antes del despacho con motivo obligatorio, usuario autorizante y fecha; liberación de reservas activas; anulación de la guía emitida y ausencia de movimientos de Kardex (ADR-009, decisión 5). |
| RF-PROV-01–10 | CU-11/12/13 | Proveedor; CatalogoProveedor; ProveedorRepository; AbastecimientoService | Registro independiente, RUC único, estado activo, relación con productos, compras e historial conservado. |
| RF-COM-01–05/12 | CU-11/12 | Proveedor; OrdenCompra; DetalleCompra; AbastecimientoService | Proveedor registrado y activo, condiciones de compra y ausencia de movimientos físicos al emitir una orden. |
| RF-COM-06–11 | CU-05/13 | OrdenCompra; OrdenRecepcion; DetalleRecepcion; MovimientoKardex | Recepciones completas o parciales, cantidades verificadas y movimientos de entrada generados al validar la recepción. |
| RF-COM-13–16 | CU-08/13 | DocumentoDigital; DetalleExtraido; OrdenRecepcion; OcrIAIntegratorService | OCR como precarga de recepción, validación del almacenero y ausencia de ingresos automáticos antes de confirmar. |
| RF-OCR-07–08; RF-COM-15 | CU-08/13 | DocumentoDigital; DetalleExtraido; CoincidenciaDetalle; Cotizacion; OrdenRecepcion | Conservación del archivo y de su relación con la operación, asociación exclusiva de un documento validado con una cotización o una recepción, y precarga sin movimientos de inventario. Eliminación lógica con fecha de eliminación (V13). |
| RF-IA-01–09 | CU-14 | ChatIAController; AgenteConsultivoService; ConversacionAgente; MensajeAgente; KardexService; Consultas de IA (8.0) | Envío del campo `mensaje` mediante POST /agente/mensajes, herramientas de solo lectura filtradas por rol, PERMISO_INSUFICIENTE por herramienta, respuestas `incierta` sin cantidades inventadas y conversaciones propias. |
| Códigos de negocio (`codigo`) | Transversal | Cliente; Cotizacion; Venta; GuiaDespacho; OrdenCompra; OrdenRecepcion | Generación determinista por la base de datos, unicidad por tabla y ausencia de escritura desde la aplicación (V11; ADR-009, decisión 10). |
| RNF-01–05/09/11/13–14 | Transversal | React; Spring Boot; Python; PostgreSQL; Docker; servicios y control de acceso | Tecnologías, despliegue, compatibilidad, seguridad, disponibilidad y separación de componentes. |
| RNF-06–07 | CU-07/08 y transversal | Backend REST; PostgreSQL; OcrIAIntegratorService | Cumplimiento de tiempos objetivo para consultas habituales y procesamiento OCR. |
| RNF-08/10 | CU-05/06/10/13 | KardexService; VentaService; DespachoService; MovimientoKardex | Atomicidad de operaciones, identificación del responsable y trazabilidad de entradas y salidas. |
| RNF-12 | Transversal | PostgreSQL; infraestructura de despliegue | Ejecución periódica de respaldos y posibilidad de recuperación de información. |
| RNF-15–16 | CU-06/10 | VentaService; KardexService; DespachoService; ReservaStock | Atomicidad de las reservas y de la confirmación de despachos, y prohibición de reservar por encima del stock libre (ADR-003, decisión 3). |

La matriz agrupa los requerimientos por responsabilidad funcional para facilitar su lectura y mantenimiento. Los códigos se conservan de acuerdo con la documentación de requisitos del proyecto, sin modificar su contenido oficial.

Las referencias a los procesos 6.0, 7.0 y 8.0 corresponden a la actualización propuesta del DFD de Nivel 1 y deberán verificarse cuando se incorpore su diagrama definitivo.

## Consideraciones de trazabilidad

Para mantener la coherencia de los entregables, se consideran las siguientes precisiones:

**1. Cotizaciones y ventas.** El modelo propuesto establece que toda venta se origina a partir de una cotización aceptada por el cliente y validada por MIGAL (ADR-003, decisión 5). Una cotización podrá generar como máximo una venta, conservando las condiciones comerciales y los productos previamente registrados.

**2. Ventas con stock insuficiente.** El sistema distingue la cantidad comprometida de las cantidades reservadas y pendientes de abastecimiento. Las reservas se asignan por producto y ubicación, sin descontar las existencias físicas hasta la confirmación del despacho único y completo (ADR-003, decisión 3).

**3. Validación de abastecimiento.** La viabilidad se determina por el indicador `aprobacion_automatica` de la categoría efectiva del producto (modelo de datos; V10). Las categorías de cemento y aglomerantes, agregados, ladrillos y bloques, y fierro y acero de construcción, es decir, las de cemento, ladrillos, fierros y agregados, se aprueban automáticamente cuando exista stock insuficiente. En otras categorías, la aprobación dependerá de la evaluación de la responsable de abastecimiento. La regla queda formalizada en el diseño (ADR-009, decisión 8).

**4. Inventario y Kardex.** El registro de una venta no genera movimientos físicos de salida. La validación de una recepción incrementa las existencias, mientras que la confirmación del despacho completo genera las salidas correspondientes (ADR-003, decisión 1). Las operaciones deben mantener integridad transaccional y evitar duplicidades.

**5. Clientes y proveedores.** Ambas entidades se representan de manera independiente y conservan sus relaciones históricas con las operaciones correspondientes. La asociación de un cliente registrado es obligatoria en cotizaciones y ventas. Las operaciones de mostrador utilizan un registro de cliente cuyo documento de identificación es opcional (ADR-003, decisión 2).

**6. Despacho y cancelaciones.** Se considera una entrega completa por venta. Las cancelaciones antes del despacho requieren motivo obligatorio, liberan las reservas activas y no generan movimientos de Kardex (ADR-003, decisión 6). Cuando la venta se encuentra en lista para despacho, la guía emitida pasa a anulada sin movimientos de Kardex (ADR-009, decisión 5).

**7. Modelo de datos y contrato.** El modelo de datos vigente, las migraciones V10 a V13 y el contrato de la API se alinean con los ADR 007 (hosting en Supabase), 008 y 009 (ajustes del contrato, seguridad y componentes). Las conversaciones del agente de IA se describen en el contrato, pero aún no figuran en el modelo de datos.

Estas precisiones constituyen criterios del diseño propuesto y se alinean con el [ADR-003](adr/adr-003-reglas-ventas-reservas-despacho.md). Los requerimientos y casos de uso de ventas, cotizaciones, clientes, Kardex y despacho se actualizaron conforme a dicho ADR.

Permanecen pendientes la confirmación de la reserva parcial sin entrega parcial con el docente, a partir de las observaciones de PC2, y la incorporación expresa de la regla de viabilidad por categoría en los requisitos (punto 3). Si la interpretación de reserva parcial se rechaza, se registrará un nuevo ADR que reemplace la decisión 3 del ADR-003. Los diagramas de secuencia y de actividades de CU-05, CU-06, CU-09, CU-10 y CU-13 se actualizarán en un trabajo posterior.

## Uso

Cada modificación del proyecto deberá revisar los requerimientos, casos de uso, especificaciones, diagramas de actividades y secuencia, modelo de clases, modelo entidad-relación, flujo de datos y arquitectura tecnológica que puedan verse afectados.

Para mantener la trazabilidad, se utilizarán los identificadores de requisitos y casos de uso, junto con referencias a los archivos y diagramas correspondientes. Las modificaciones deberán registrarse mediante commits y pull requests de GitHub, conservando la evidencia de revisión e integración de los entregables.

La matriz se actualizará cuando se incorporen nuevos requerimientos, se modifiquen reglas funcionales o se aprueben correcciones que afecten la realización de los componentes.

La vinculación de un requerimiento con un componente indica su **realización prevista en el diseño**, pero no constituye por sí misma evidencia de implementación, ejecución de pruebas o cumplimiento verificado.
