# Matriz de trazabilidad

La matriz de trazabilidad de MIGAL SmartOps permite establecer la correspondencia entre los requerimientos funcionales y no funcionales, los casos de uso y los principales componentes responsables de su realización.

Su propósito es facilitar la verificación de coherencia entre las especificaciones funcionales, los diagramas de actividades y secuencia, el modelo de clases, el modelo de datos, los Diagramas de Flujo de Datos (DFD) y la arquitectura tecnológica.

La matriz considera los requerimientos incorporados durante las correcciones de PC2, incluyendo la gestión independiente de clientes y proveedores, la atención de ventas con stock insuficiente y el control de las operaciones de abastecimiento. Asimismo, identifica aquellos puntos que requieren armonización con las reglas comerciales y el modelo de inventario propuesto para MIGAL SmartOps.

## Matriz de trazabilidad de requerimientos

| Requerimientos | CU | Realización principal | Punto que debe verificarse |
| --- | --- | --- | --- |
| RF-USR-01–04 | CU-01/02 | Usuario; UsuarioService; Gestión de Usuarios (7.0) | Autenticación, permisos por rol, vigencia de usuarios y trazabilidad de operaciones. |
| RF-PRO-01–06 | CU-03/04 | Producto; ProductoService; Gestión de Catálogo (1.0) | SKU único, atributos obligatorios, categorías, actualización y consulta de productos. |
| RF-KAR-01–02/05–08/14 | CU-04/05 | Inventario; MovimientoKardex; KardexService; Control de Inventario (3.0) | Existencias por almacén, stock mínimo, traslados, ajustes, motivos y prevención de stock negativo. |
| RF-KAR-09–10/12 | CU-07/14 | Inventario; Producto; KardexService; AgenteConsultivoService | Alertas de stock mínimo, cálculo determinista del punto de reorden y consultas históricas por fecha y ubicación. |
| RF-KAR-03/11 | CU-05/13 | OrdenRecepcion; DetalleRecepcion; MovimientoKardex | Registro de entradas únicamente por recepción confirmada y trazabilidad de la operación de origen. |
| RF-KAR-04 | CU-06/10 | Venta; GuiaDespacho; MovimientoKardex; DespachoService | **Alinear:** la salida de Kardex debe originarse en la confirmación del despacho físico y no en el registro comercial de la venta. |
| RF-KAR-13; RF-DES-01–06 | CU-06 | GuiaDespacho; Venta; DespachoService; Gestión de Despacho (6.0) | Emisión, impresión, dirección de entrega, estados, vínculo con la venta y prevención de salidas duplicadas. |
| RF-OCR-01–08; RF-FUZ-01–07 | CU-08/04/09/13 | DocumentoDigital; DetalleExtraido; Producto; OcrIAIntegratorService; Procesamiento OCR (2.0) | Formatos admitidos, extracción, coincidencia con SKU, corrección manual y validación previa al registro definitivo. |
| RF-COT-01–04/06/08 | CU-08/09 | Cotizacion; DetalleCotizacion; CotizacionService; Gestión Comercial (4.0) | Emisión, importes, descuentos, vigencia, precarga OCR y ausencia de modificaciones de inventario durante la cotización. |
| RF-COT-05/07/09 | CU-09/10 | Cotizacion; Venta; CotizacionService; VentaService | Estados, aceptación del cliente, validación de viabilidad y conversión única de cotización a venta. |
| RF-CLI-01–02/05/07–08 | CU-09/10 | Cliente; ClienteRepository; Cotizacion; Venta | Registro, consulta, estado e historial de cotizaciones y ventas asociadas a cada cliente. |
| RF-CLI-03–04/06; RF-VEN-01–02 | CU-09/10 | Cliente; Cotizacion; Venta; ClienteRepository | **Alinear:** obligatoriedad del cliente en cotizaciones y ventas frente a la asociación opcional prevista en los requisitos actuales. |
| RF-VEN-03/05–08/10–11/15–16 | CU-09/10 | Venta; DetalleVenta; Inventario; ReservaStock; VentaService | Disponibilidad, productos comprometidos, descuentos, registro de pendientes, trazabilidad y control de stock sin salidas anticipadas. |
| RF-VEN-04/09 | CU-06/09/10 | Cotizacion; Venta; GuiaDespacho; VentaService; DespachoService | **Alinear:** cotización obligatoria antes de la venta y separación entre aprobación comercial y salida física de inventario. |
| RF-VEN-12–14/17–18 | CU-06/10 | DetalleVenta; ReservaStock; Inventario; MovimientoKardex | **Alinear:** reserva de cantidades disponibles, abastecimiento de faltantes, atención completa y trazabilidad del despacho. |
| RF-PROV-01–10 | CU-11/12/13 | Proveedor; CatalogoProveedor; ProveedorRepository; AbastecimientoService | Registro independiente, RUC único, estado activo, relación con productos, compras e historial conservado. |
| RF-COM-01–05/12 | CU-11/12 | Proveedor; OrdenCompra; DetalleCompra; AbastecimientoService | Proveedor registrado y activo, condiciones de compra y ausencia de movimientos físicos al emitir una orden. |
| RF-COM-06–11 | CU-05/13 | OrdenCompra; OrdenRecepcion; DetalleRecepcion; MovimientoKardex | Recepciones completas o parciales, cantidades verificadas y movimientos de entrada correspondientes. |
| RF-COM-13–16 | CU-08/13 | DocumentoDigital; DetalleExtraido; OrdenRecepcion; OcrIAIntegratorService | OCR como precarga de recepción, validación del almacenero y ausencia de ingresos automáticos antes de confirmar. |
| RF-IA-01–09 | CU-14 | AgenteConsultivoService; KardexService; Consultas de IA (8.0) | Consultas autorizadas, tool calling, disponibilidad, productos críticos y respuestas sin modificación de datos. |
| RNF-01–05/09/11/13–14 | Transversal | React; Spring Boot; Python; PostgreSQL; Docker; servicios y control de acceso | Tecnologías, despliegue, compatibilidad, seguridad, disponibilidad y separación de componentes. |
| RNF-06–07 | CU-07/08 y transversal | Backend REST; PostgreSQL; OcrIAIntegratorService | Cumplimiento de tiempos objetivo para consultas habituales y procesamiento OCR. |
| RNF-08/10 | CU-05/06/10/13 | KardexService; VentaService; DespachoService; MovimientoKardex | Atomicidad de operaciones, identificación del responsable y trazabilidad de entradas y salidas. |
| RNF-12 | Transversal | PostgreSQL; infraestructura de despliegue | Ejecución periódica de respaldos y posibilidad de recuperación de información. |
| RNF-15–16 | CU-06/10 | VentaService; KardexService; DespachoService; ReservaStock | **Alinear:** consistencia transaccional de las cantidades comprometidas y reservadas, sin sobreventa ni despachos parciales. |

La matriz agrupa los requerimientos por responsabilidad funcional para facilitar su lectura y mantenimiento. Los códigos se conservan de acuerdo con la documentación de requisitos del proyecto, sin modificar su contenido oficial.

Las referencias a los procesos 6.0, 7.0 y 8.0 corresponden a la actualización propuesta del DFD de Nivel 1 y deberán verificarse cuando se incorpore su diagrama definitivo.

## Consideraciones de trazabilidad

Para mantener la coherencia de los entregables, se consideran las siguientes precisiones:

**1. Cotizaciones y ventas.** El modelo propuesto establece que toda venta se origina a partir de una cotización aceptada por el cliente y validada por MIGAL. Una cotización podrá generar como máximo una venta, conservando las condiciones comerciales y los productos previamente registrados.

**2. Ventas con stock insuficiente.** El sistema distingue la cantidad comprometida de las cantidades reservadas y pendientes de abastecimiento. Las reservas se asignan por producto y ubicación, sin descontar las existencias físicas hasta el despacho.

**3. Validación de abastecimiento.** Para cemento, ladrillos, fierros y agregados se contempla la aceptación automática de la viabilidad de abastecimiento cuando exista stock insuficiente. En otras categorías, la aprobación dependerá de la evaluación de la responsable de abastecimiento. Esta regla requiere incorporarse expresamente en los requisitos y casos de uso correspondientes.

**4. Inventario y Kardex.** El registro de una venta no genera movimientos físicos de salida. La confirmación de una recepción incrementa las existencias, mientras que la confirmación del despacho completo genera las salidas correspondientes. Las operaciones deben mantener integridad transaccional y evitar duplicidades.

**5. Clientes y proveedores.** Ambas entidades se representan de manera independiente y conservan sus relaciones históricas con las operaciones correspondientes. La obligatoriedad de identificar al cliente deberá armonizarse con los requisitos funcionales que actualmente permiten su asociación opcional.

**6. Despacho y cancelaciones.** Se considera una entrega completa por venta. Las cancelaciones autorizadas antes del despacho liberan las reservas y conservan el motivo correspondiente, sin generar movimientos de Kardex que no representen una salida real.

Estas precisiones constituyen criterios del diseño propuesto. Los requisitos actuales que describen ventas directas, atención parcial con salida inmediata de Kardex o clientes opcionales deberán revisarse para evitar contradicciones entre documentos. Asimismo, deberá verificarse que la interpretación de reserva parcial sin entrega parcial satisfaga las observaciones académicas de PC2.

## Uso

Cada modificación del proyecto deberá revisar los requerimientos, casos de uso, especificaciones, diagramas de actividades y secuencia, modelo de clases, modelo entidad-relación, flujo de datos y arquitectura tecnológica que puedan verse afectados.

Para mantener la trazabilidad, se utilizarán los identificadores de requisitos y casos de uso, junto con referencias a los archivos y diagramas correspondientes. Las modificaciones deberán registrarse mediante commits y pull requests de GitHub, conservando la evidencia de revisión e integración de los entregables.

La matriz se actualizará cuando se incorporen nuevos requerimientos, se modifiquen reglas funcionales o se aprueben correcciones que afecten la realización de los componentes.

La vinculación de un requerimiento con un componente indica su **realización prevista en el diseño**, pero no constituye por sí misma evidencia de implementación, ejecución de pruebas o cumplimiento verificado.
