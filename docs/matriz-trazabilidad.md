# Matriz de trazabilidad

| Requerimientos | CU | Realización principal | Punto que debe verificarse |
| --- | --- | --- | --- |
| RF-USR-01–04 | CU-01/02 | Usuario; UsuarioService | Estado activo y revocación |
| RF-PRO-01–06; RF-KAR-08 | CU-03/04 | Producto; Inventario | Contratos y búsqueda aproximada |
| RF-KAR-01/02/05/06/07/14 | CU-04/05 | MovimientoKardex; KardexService | Vínculo traslado, motivo y concurrencia |
| RF-KAR-03/11; RF-COM-06–11/13–16 | CU-13/08 | OrdenRecepcion; movimientos | Parciales, acumulados y cardinalidad de origen |
| RF-KAR-04; RF-VEN-01–18 | CU-10 | Venta; DetalleVenta; movimientos Parciales | Origen, atomicidad y consulta |
| RF-VEN-11–18 | CU-10 | Venta; DetalleVenta; VentaService | Registro de solicitud, cantidades solicitadas, atendidas y pendientes manejando  |
| RF-KAR-12 | CU-07 | KardexService | Propagar fechas |
| RF-KAR-13; RF-DES-01–06 | CU-06 | GuiaDespacho | Dirección, estados y duplicidad |
| RF-OCR-01–08; RF-FUZ-01–07 | CU-08/04 | DocumentoDigital; DetalleExtraido | Atributos, validación y ubicación de Fuzzy |
| RF-COT-01–09 | CU-09/10 | Cotizacion; Venta | Guardas y conversión única |
| RF-COM-01–05/12 | CU-11/12 | Proveedor; OrdenCompra; CatalogoProveedor | Ramas invertidas y proveedor inactivo |
| RF-IA-01–09; RF-KAR-09 | CU-14 | AgenteConsultivoService | Motor, herramientas y críticos |
| RF-KAR-10 | Realización por completar | Backend / configuración por definir | Regla de reorden |
| RNF-01–16 | Transversal | Arquitectura y servicios | Concurrencia, medición y respaldos |
| RF-PROV-01–04 | CU-11/CU-12 | Proveedor, repositorios y servicios | Gestión y asociación de proveedores con productos. | 
| RF-PROV-05–10 | CU-11/CU-12/CU-13 | Proveedor, OrdenCompra y OrdenRecepcion | Validación, cardinalidad, historial y trazabilidad. | 
| RF-COM-01–11 | CU-12/CU-13 | OrdenCompra, DetalleCompra y Proveedor | Compras y recepciones vinculadas al proveedor sin afectar inventario directamente. |

## Uso

Cada cambio revisa los requerimientos, CU, especificaciones, secuencias, actividades, clases, datos y arquitectura afectados. Referenciar los archivos concretos y los identificadores. Registrar las tareas en Notion y enlazar la PR como evidencia.
