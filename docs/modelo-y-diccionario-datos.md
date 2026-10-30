# Modelo y diccionario de datos

## Índice

- [Diagrama entidad-relación](#diagrama-entidad-relacion)
- [Entidades maestras y transaccionales](#entidades-maestras-y-transaccionales)
- [Relaciones con atributos](#relaciones-con-atributos)
- [Regla de consistencia en cantidades](#regla-consistencia-cantidades)
- [Relaciones sin atributos](#relaciones-sin-atributos)
- [Restricción de exclusividad documental](#exclusividad-documental)

<a id="diagrama-entidad-relacion"></a>

## Diagrama entidad-relación

![Diagrama entidad-relación de MIGAL SmartOps](imagenes/MIGAL-DER-01-modelo-entidad-relacion.png)

<a id="entidades-maestras-y-transaccionales"></a>

## Entidades maestras y transaccionales

### Usuario

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_usuario` | Único (PK) | Identificador interno del colaborador en el sistema. |
| `nombre_completo` | Compuesto | Nombre completo del usuario, compuesto por nombres y apellidos. |
| `username` | Único | Nombre de usuario utilizado para el inicio de sesión. |
| `password_hash` | Simple | Contraseña almacenada mediante representación hash. |
| `rol` | Simple | Nivel de acceso del usuario: Administrador, Vendedor, Almacenero o Reponedor. |

### Cliente

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_cliente` | Único (PK) | Identificador interno del cliente. |
| `nombre_razon_social` | Simple | Nombre completo del cliente o razón social cuando corresponda. |
| `documento_referencial` | Opcional | DNI, RUC u otro documento utilizado como referencia operativa, sin finalidad fiscal. |
| `telefono_contacto` | Opcional | Teléfono u otro medio de contacto del cliente. |
| `estado` | Simple | Estado del cliente, por ejemplo Activo o Inactivo. |

### Almacen

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_almacen` | Único (PK) | Identificador interno de la ubicación física. |
| `nombre` | Simple | Nombre descriptivo de la tienda o almacén. |

### Producto

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_producto` | Único (PK) | Identificador interno del producto en el catálogo maestro. |
| `sku` | Único | Código oficial y estandarizado del producto. |
| `nombre_oficial` | Simple | Denominación oficial del producto dentro del catálogo maestro. |
| `unidad_medida` | Simple | Unidad utilizada para el manejo del producto, como unidad, bolsa, metro, caja, entre otras. |
| `precio_referencial` | Simple | Precio de referencia utilizado como apoyo en las operaciones comerciales. |
| `stock_minimo` | Simple | Nivel mínimo de existencias utilizado como umbral para alertas de reposición. |
| `descripcion` | Simple | Detalle, características o especificaciones adicionales del producto. |
| `estado_activo` | Simple | Indicador de vigencia del producto dentro del catálogo maestro. |

### Venta

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_venta` | Único (PK) | Identificador interno de la transacción de venta. |
| `id_cliente` | Opcional (FK) | Referencia al cliente asociado a la venta. Puede ser nula cuando la operación no requiera identificarlo. |
| `fecha_venta` | Simple | Fecha y hora en que se registra la venta. |
| `descuento` | Opcional | Descuento general aplicado a la venta cuando corresponda. |
| `total_venta` | Derivado | Monto total calculado a partir de los detalles de venta y del descuento aplicado, cuando corresponda. |
| `estado` | Simple | Estado de la venta: PendienteStock, AtencionParcial, Confirmada, Completada o Cancelada. |
| `tipo_atencion` | Simple | Forma de atención: Completa, Parcial, PendienteAbastecimiento o SolicitudPendiente. |
| `cantidad_total_solicitada` | Derivado | Total de unidades solicitadas en la venta. |
| `cantidad_total_atendida` | Derivado | Total de unidades entregadas y descontadas del inventario. |
| `cantidad_total_pendiente` | Derivado | Total de unidades aún no atendidas. |

### Cotizacion

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_cotizacion` | Único (PK) | Identificador interno de la cotización realizada al cliente. |
| `id_cliente` | Opcional (FK) | Referencia al cliente asociado a la cotización. Puede ser nula cuando la cotización no requiera identificarlo. |
| `fecha_emision` | Simple | Fecha en que se emite la cotización. |
| `fecha_vencimiento` | Simple | Fecha límite hasta la cual la cotización mantiene su vigencia. |
| `condiciones_comerciales` | Opcional | Condiciones comerciales o términos aplicables a la propuesta. |
| `descuento` | Opcional | Descuento general aplicado a la cotización cuando corresponda. |
| `estado` | Simple | Estado de la cotización: Emitida, Aceptada, Rechazada o Vencida. |
| `total_cotizacion` | Derivado | Monto total calculado a partir de los productos, cantidades, precios y descuentos registrados. |

### Guia_Despacho

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_guia` | Único (PK) | Identificador interno de la orden o guía de despacho. |
| `fecha_despacho` | Simple | Fecha programada para la entrega física de la mercadería. |
| `direccion_entrega` | Simple | Dirección o ubicación de destino de la mercadería. |
| `estado` | Simple | Situación de la orden de despacho, por ejemplo Impresa, En ruta o Entregada. |

### Orden_Compra

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_orden` | Único (PK) | Identificador interno de la Orden de Compra. |
| `fecha_emision` | Simple | Fecha de creación de la Orden de Compra. |
| `estado` | Simple | Estado de cumplimiento de la Orden de Compra respecto a las recepciones realizadas: Pendiente, Parcial o Completada. |

### Orden_Recepcion

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_recepcion` | Único (PK) | Identificador interno de la recepción de mercadería registrada en el sistema. |
| `fecha_recepcion` | Simple | Fecha en que la mercadería es recibida físicamente. |
| `estado` | Simple | Estado de la recepción, por ejemplo Pendiente o Validada. |

### Movimiento_Kardex

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_movimiento` | Único (PK) | Identificador interno del movimiento registrado en el historial del Kardex. |
| `fecha_hora` | Simple | Momento exacto en que se produce el movimiento de inventario. |
| `tipo_operacion` | Simple | Clasificación del movimiento como Entrada o Salida. |
| `cantidad` | Simple | Cantidad de unidades afectadas por el movimiento. |
| `motivo` | Opcional | Justificación del movimiento cuando corresponda, especialmente en ajustes, mermas o traslados. |

### Proveedor

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_proveedor` | Único (PK) | Identificador interno del proveedor. |
| `ruc` | Único | Registro Único de Contribuyente del proveedor. |
| `razon_social` | Simple | Razón social o nombre comercial del proveedor. |
| `contacto` | Simple | Información de contacto del proveedor, como teléfono o correo electrónico. |
| `condiciones` | Opcional | Condiciones comerciales generales asociadas al proveedor, como plazos de entrega o condiciones de pago. |

### Documento_Digital

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_documento` | Único (PK) | Identificador interno del archivo cargado y procesado mediante OCR. |
| `ruta_archivo` | Simple | Ruta o referencia al archivo JPG, PNG o PDF almacenado para procesamiento. |
| `fecha_subida` | Simple | Fecha y hora en que el archivo es cargado al sistema. |
| `estado_procesamiento` | Simple | Estado del procesamiento del documento, por ejemplo Pendiente, Procesado o Validado. |
| `tipo_documento` | Simple | Tipo funcional del documento cargado, por ejemplo Solicitud de Cliente o Documento de Recepción. |

### Detalle_Extraido

| Atributo | Tipo de atributo | Descripción |
| --- | --- | --- |
| `id_detalle_extraido` | Único (PK) | Identificador interno del elemento obtenido mediante OCR. |
| `texto_crudo` | Simple | Texto original identificado por el motor OCR antes de su normalización. |
| `cantidad_leida` | Simple | Cantidad identificada automáticamente en el documento procesado. |

<a id="relaciones-con-atributos"></a>

## Relaciones con atributos

| Relación | Entidades que une | Atributos propios | Cardinalidad |
| --- | --- | --- | --- |
| Inventario | Producto + Almacen | `cantidad_disponible` (Derivado) | Muchos a Muchos. Un producto puede mantener existencias en múltiples almacenes y cada almacén puede contener múltiples productos. La cantidad disponible se obtiene a partir de los movimientos registrados en el Kardex. |
| Detalle_Venta | Venta + Producto | `cantidad_solicitada` (Simple)<br> `cantidad_atendida` (Simple)<br> `cantidad_pendiente` (Derivado)<br> `precio_unitario` (Simple) | Muchos a Muchos. Un detalle conserva la cantidad solicitada, la cantidad efectivamente atendida y la cantidad pendiente. |
| Detalle_Cotizacion | Cotizacion + Producto | `cantidad_cotizada` (Simple)<br> `precio_unitario` (Simple) | Muchos a Muchos. Una cotización contiene uno o varios productos y un mismo producto puede aparecer en múltiples cotizaciones. |
| Detalle_Compra | Orden_Compra + Producto | `cantidad_comprada` (Simple)<br> `costo_unitario` (Simple) | Muchos a Muchos. Una Orden de Compra puede contener varios productos y un mismo producto puede aparecer en múltiples órdenes de compra. |
| Detalle_Recepcion | Orden_Recepcion + Producto | `cantidad_recibida` (Simple) | Muchos a Muchos. Una recepción puede incluir múltiples productos y un mismo producto puede ser recibido en múltiples recepciones. |
| Catalogo_Proveedor | Proveedor + Producto | `precio_compra` (Simple) | Muchos a Muchos. Un proveedor puede suministrar varios productos y un mismo producto puede encontrarse asociado a múltiples proveedores. |
| Cliente_Venta | Cliente + Venta | `id_cliente` (FK opcional) | Un cliente puede estar asociado a muchas ventas. Una venta puede asociarse con cero o un cliente. |
| Cliente_Cotizacion | Cliente + Cotizacion | `id_cliente` (FK opcional) | Un cliente puede estar asociado a muchas cotizaciones. Una cotización puede asociarse con cero o un cliente. |

<a id="regla-consistencia-cantidades"></a>

## Regla de consistencia en cantidades

La cantidad pendiente se calcula como:

`cantidad_pendiente` = `cantidad_solicitada` - `cantidad_atendida`

La `cantidad_atendida` nunca puede ser mayor que la `cantidad_solicitada` ni que el `stock_disponible` al momento de la confirmación.

<a id="relaciones-sin-atributos"></a>

## Relaciones sin atributos

| Relación | Entidades que une | Lógica y cardinalidad |
| --- | --- | --- |
| Registra | Usuario + Movimiento_Kardex | 1. Un usuario puede registrar múltiples movimientos de Kardex; cada movimiento tiene un único usuario responsable. |
| Afecta_a | Producto + Movimiento_Kardex | 1. Un producto puede registrar múltiples movimientos de Kardex; cada movimiento afecta a un único producto. |
| Ocurre_en | Almacen + Movimiento_Kardex | 1. En un almacén pueden producirse múltiples movimientos; cada movimiento impacta en un único almacén. |
| Realiza | Usuario + Venta | 1. Un usuario puede registrar múltiples ventas; cada venta es registrada por un único usuario. |
| Emite | Usuario + Cotizacion | 1. Un usuario puede emitir múltiples cotizaciones; cada cotización es registrada por un único usuario. |
| Genera | Venta + Guia_Despacho | 1:0..1. Una venta puede generar como máximo una guía de despacho; toda guía de despacho debe encontrarse asociada a una única venta. |
| Autoriza | Usuario + Orden_Compra | 1. Un usuario puede registrar o autorizar múltiples órdenes de compra; cada orden de compra tiene un único usuario responsable. |
| Recibe | Usuario + Orden_Recepcion | 1. Un usuario puede validar múltiples recepciones; cada recepción es validada por un único usuario. |
| Deriva_en | Orden_Compra + Orden_Recepcion | 1. Una Orden de Compra puede derivar en una o varias recepciones debido a entregas parciales; cada Orden de Recepción corresponde a una única Orden de Compra. |
| Genera_Salida | Venta + Movimiento_Kardex | 1. Una venta confirmada genera uno o varios movimientos de salida, según los productos incluidos. Un movimiento de Kardex puede provenir como máximo de una venta. |
| Genera_Ingreso | Orden_Recepcion + Movimiento_Kardex | 1. Una recepción confirmada genera uno o varios movimientos de entrada según los productos efectivamente recibidos. Un movimiento de entrada puede provenir como máximo de una recepción. |
| Recibe | Proveedor + Orden_Compra | 1. Un proveedor puede encontrarse asociado a múltiples órdenes de compra; cada Orden de Compra corresponde a un único proveedor. |
| Sube | Usuario + Documento_Digital | 1. Un usuario puede cargar múltiples documentos digitales; cada documento es cargado por un único usuario. |
| Contiene | Documento_Digital + Detalle_Extraido | 1. Un documento digital puede contener cero o múltiples elementos extraídos; cada Detalle_Extraido pertenece obligatoriamente a un único Documento_Digital. |
| Asociado | Producto + Detalle_Extraido | 1 con asociación opcional desde el detalle. Un producto puede encontrarse asociado a múltiples detalles extraídos; cada Detalle_Extraido puede estar asociado a cero o un único producto mientras se completa el proceso de Fuzzy Matching o validación manual. |
| Origina_Cotizacion | Documento_Digital + Cotizacion | 0..1 : 0..1. Un Documento_Digital puede utilizarse para originar como máximo una cotización y una cotización puede haber sido creada manualmente o a partir de un único documento procesado. |
| Origina_Venta | Documento_Digital + Venta | 0..1 : 0..1. Un Documento_Digital puede utilizarse para originar directamente como máximo una venta y una venta puede registrarse manualmente o a partir de un único documento procesado. |
| Apoya_Recepcion | Documento_Digital + Orden_Recepcion | 0..1 : 0..1. Un Documento_Digital puede utilizarse como apoyo para precargar una recepción y una recepción puede registrarse manualmente o utilizar como máximo un documento procesado. |
| Se_Convierte_En | Cotizacion + Venta | 0..1 : 0..1. Una cotización aceptada puede convertirse como máximo en una venta; una venta puede haberse generado desde una cotización o haberse registrado directamente. |
| Genera_Salida_Atendida | Venta + Movimiento_Kardex | Una venta genera movimientos de salida únicamente por las cantidades atendidas. Las cantidades pendientes no generan movimientos. |
| Completa | Venta + Movimiento_Kardex | Una atención posterior completa una parte o la totalidad de la cantidad pendiente y queda vinculada con la venta original. |
| Realiza_Venta | Cliente + Venta | 0..1 : 0..* | Un cliente puede tener muchas ventas; una venta puede estar asociada opcionalmente a un único cliente. |
| Solicita_Cotizacion | Cliente + Cotizacion | 0..1 : 0..* | Un cliente puede tener muchas cotizaciones; una cotización puede estar asociada opcionalmente a un único cliente. |

<a id="exclusividad-documental"></a>

## Restricción de exclusividad de Documento_Digital

Las relaciones `Origina_Cotizacion`, `Origina_Venta` y `Apoya_Recepcion` son mutuamente excluyentes para un mismo `Documento_Digital`.

Un documento procesado deberá utilizarse como entrada principal de un único proceso operativo:

- Cotización a cliente.
- Venta directa.
- Recepción de mercadería.
