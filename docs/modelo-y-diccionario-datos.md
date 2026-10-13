# Modelo y diccionario de datos

> Estado: base documental migrada, pendiente de revisión del equipo.
> Fuente: [Software - entregables](../entregas/pc1/antecedentes/MIGAL-PC1-entregables-recibidos.pdf), páginas 25–32.
> Se conserva el contenido recibido; las observaciones de auditoría no se consideran corregidas ni aprobadas por esta migración.

**Pendientes:** cardinalidad Genera_Ingreso, vínculo de traslado, atributos de Detalle_Extraido y nombre Relationship. Este diccionario es conceptual; no representa un esquema SQL implementado.

![DER](diagramas/imagenes/MIGAL-DER-01-modelo-entidad-relacion.png)

### Fuente: página 25, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
| Usuario | id_usuario | Único (PK) | Identificador interno del colaborador en el sistema. |
|  | nombre_completo | Compuesto | Nombre completo del usuario, compuesto por nombres y apellidos. |

### Fuente: página 26, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
|  | username | Único | Nombre de usuario utilizado para el inicio de sesión. |
|  | password_hash | Simple | Contraseña almacenada mediante representación hash. |
|  | rol | Simple | Nivel de acceso del usuario: Administrador, Vendedor, Almacenero o Reponedor. |
| Almacen | id_almacen | Único (PK) | Identificador interno de la ubicación física. |
|  | nombre | Simple | Nombre descriptivo de la tienda o almacén. |
| Producto | id_producto | Único (PK) | Identificador interno del producto en el catálogo maestro. |
|  | sku | Único | Código oficial y estandarizado del producto. |
|  | nombre_oficial | Simple | Denominación oficial del producto dentro del catálogo maestro. |
|  | unidad_medida | Simple | Unidad utilizada para el manejo del producto, como unidad, bolsa, metro, caja, entre otras. |
|  | precio_referencial | Simple | Precio de referencia utilizado como apoyo en las operaciones comerciales. |
|  | stock_minimo | Simple | Nivel mínimo de existencias utilizado como umbral para alertas de reposición. |
|  | descripcion | Simple | Detalle, características o especificaciones adicionales del producto. |
|  | estado_activo | Simple | Indicador de vigencia del producto dentro del catálogo maestro. |
| Venta | id_venta | Único (PK) | Identificador interno de la transacción de venta. |
|  | fecha_venta | Simple | Fecha y hora en que se registra la venta. |

### Fuente: página 27, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
|  | cliente_nombre | Opcional | Nombre referencial del cliente cuando se encuentre disponible. |
|  | cliente_documento | Opcional | DNI, RUC u otro documento referencial del cliente, sin finalidad fiscal. |
|  | descuento | Opcional | Descuento general aplicado a la venta cuando corresponda. |
|  | total_venta | Derivado | Monto total calculado a partir de los detalles de venta y del descuento aplicado, cuando corresponda. |
| Cotizacion | id_cotizacion | Único (PK) | Identificador interno de la cotización realizada al cliente. |
|  | fecha_emision | Simple | Fecha en que se emite la cotización. |
|  | fecha_vencimiento | Simple | Fecha límite hasta la cual la cotización mantiene su vigencia. |
|  | cliente_nombre | Opcional | Nombre referencial del cliente al que se dirige la cotización. |
|  | cliente_documento | Opcional | DNI, RUC u otro documento referencial del cliente. |
|  | condiciones_comerciales | Opcional | Condiciones comerciales o términos aplicables a la propuesta. |
|  | descuento | Opcional | Descuento general aplicado a la cotización cuando corresponda. |
|  | estado | Simple | Estado de la cotización: Emitida, Aceptada, Rechazada o Vencida. |
|  | total_cotizacion | Derivado | Monto total calculado a partir de los productos, cantidades, precios y descuentos registrados. |
| Guia_Despacho | id_guia | Único (PK) | Identificador interno de la orden o guía de despacho. |
|  | fecha_despacho | Simple | Fecha programada para la entrega física de la mercadería. |
|  | direccion_entrega | Simple | Dirección o ubicación de destino de la mercadería. |

### Fuente: página 28, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
|  | estado | Simple | Situación de la orden de despacho, por ejemplo Impresa, En ruta o Entregada. |
| Orden_Compra | id_orden | Único (PK) | Identificador interno de la Orden de Compra. |
|  | fecha_emision | Simple | Fecha de creación de la Orden de Compra. |
|  | estado | Simple | Estado de cumplimiento de la Orden de Compra respecto a las recepciones realizadas: Pendiente, Parcial o Completada. |
| Orden_Recepcion | id_recepcion | Único (PK) | Identificador interno de la recepción de mercadería registrada en el sistema. |
|  | fecha_recepcion | Simple | Fecha en que la mercadería es recibida físicamente. |
|  | estado | Simple | Estado de la recepción, por ejemplo Pendiente o Validada. |
| Movimiento_Kardex | id_movimiento | Único (PK) | Identificador interno del movimiento registrado en el historial del Kardex. |
|  | fecha_hora | Simple | Momento exacto en que se produce el movimiento de inventario. |
|  | tipo_operacion | Simple | Clasificación del movimiento como Entrada o Salida. |
|  | cantidad | Simple | Cantidad de unidades afectadas por el movimiento. |
|  | motivo | Opcional | Justificación del movimiento cuando corresponda, especialmente en ajustes, mermas o traslados. |
| Proveedor | id_proveedor | Único (PK) | Identificador interno del proveedor. |
|  | ruc | Único | Registro Único de Contribuyente del proveedor. |
|  | razon_social | Simple | Razón social o nombre comercial del proveedor. |
|  | contacto | Simple | Información de contacto del proveedor, como teléfono o correo electrónico. |

### Fuente: página 29, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
|  | condiciones | Opcional | Condiciones comerciales generales asociadas al proveedor, como plazos de entrega o condiciones de pago. |
| Documento_Digital | id_documento | Único (PK) | Identificador interno del archivo cargado y procesado mediante OCR. |
|  | ruta_archivo | Simple | Ruta o referencia al archivo JPG, PNG o PDF almacenado para procesamiento. |
|  | fecha_subida | Simple | Fecha y hora en que el archivo es cargado al sistema. |
|  | estado_procesamiento | Simple | Estado del procesamiento del documento, por ejemplo Pendiente, Procesado o Validado. |
|  | tipo_documento | Simple | Tipo funcional del documento cargado, por ejemplo Solicitud de Cliente o Documento de Recepción. |
| Detalle_Extraido | id_detalle_extraido | Único (PK) | Identificador interno del elemento obtenido mediante OCR. |
|  | texto_crudo | Simple | Texto original identificado por el motor OCR antes de su normalización. |
|  | cantidad_leida | Simple | Cantidad identificada automáticamente en el documento procesado. |

### Fuente: página 29, tabla o continuación 2

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
| Inventario | Producto + Almacen | cantidad_disponible Derivado | Muchos a Muchos. Un producto puede mantener existencias en múltiples almacenes y cada almacén puede contener múltiples productos. La cantidad disponible se obtiene a partir de los movimientos registrados en el Kardex. |

### Fuente: página 30, tabla o continuación 1

| Entidad / relación | Atributo / entidades | Tipo / atributos propios | Descripción / cardinalidad |
| --- | --- | --- | --- |
| Detalle_Venta | Venta + Producto | cantidad_vendida Simple precio_unitario Simple | Muchos a Muchos. Una venta contiene uno o varios productos y un mismo producto puede formar parte de múltiples ventas. |
| Detalle_Cotizacion | Cotizacion + Producto | cantidad_cotizada Simple precio_unitario Simple | Muchos a Muchos. Una cotización contiene uno o varios productos y un mismo producto puede aparecer en múltiples cotizaciones. |
| Detalle_Compra | Orden_Compra + Producto | cantidad_comprada Simple costo_unitario Simple | Muchos a Muchos. Una Orden de Compra puede contener varios productos y un mismo producto puede aparecer en múltiples órdenes de compra. |
| Detalle_Recepcion | Orden_Recepcion + Producto | cantidad_recibida Simple | Muchos a Muchos. Una recepción puede incluir múltiples productos y un mismo producto puede ser recibido en múltiples recepciones. |
| Catalogo_Proveedor | Proveedor + Producto | precio_compra Simple | Muchos a Muchos. Un proveedor puede suministrar varios productos y un mismo producto puede encontrarse asociado a múltiples proveedores. |

### Fuente: página 30, tabla o continuación 2

| Relación | Entidades | Lógica y cardinalidad |
| --- | --- | --- |
| Registra | Usuario + Movimiento_Kardex | 1. Un usuario puede registrar múltiples movimientos de Kardex; cada movimiento tiene un único usuario responsable. |
| Afecta_a | Producto + Movimiento_Kardex | 1. Un producto puede registrar múltiples movimientos de Kardex; cada movimiento afecta a un único producto. |

### Fuente: página 31, tabla o continuación 1

| Relación | Entidades | Lógica y cardinalidad |
| --- | --- | --- |
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

### Fuente: página 32, tabla o continuación 1

| Relación | Entidades | Lógica y cardinalidad |
| --- | --- | --- |
| Contiene | Documento_Digital + Detalle_Extraido | 1. Un documento digital puede contener cero o múltiples elementos extraídos; cada Detalle_Extraido pertenece obligatoriamente a un único Documento_Digital. |
| Asociado | Producto + Detalle_Extraido | 1 con asociación opcional desde el detalle. Un producto puede encontrarse asociado a múltiples detalles extraídos; cada Detalle_Extraido puede estar asociado a cero o un único producto mientras se completa el proceso de Fuzzy Matching o validación manual. |
| Origina_Cotizacion | Documento_Digital + Cotizacion | 0..1 : 0..1. Un Documento_Digital puede utilizarse para originar como máximo una cotización y una cotización puede haber sido creada manualmente o a partir de un único documento procesado. |
| Origina_Venta | Documento_Digital + Venta | 0..1 : 0..1. Un Documento_Digital puede utilizarse para originar directamente como máximo una venta y una venta puede registrarse manualmente o a partir de un único documento procesado. |
| Apoya_Recepcion | Documento_Digital + Orden_Recepcion | 0..1 : 0..1. Un Documento_Digital puede utilizarse como apoyo para precargar una recepción y una recepción puede registrarse manualmente o utilizar como máximo un documento procesado. |
| Se_Convierte_En | Cotizacion + Venta | 0..1 : 0..1. Una cotización aceptada puede convertirse como máximo en una venta; una venta puede haberse generado desde una cotización o haberse registrado directamente. |

## Exclusividad de Documento_Digital

Las relaciones Origina_Cotizacion, Origina_Venta y Apoya_Recepcion son mutuamente excluyentes
para un mismo Documento_Digital.
Un documento procesado deberá utilizarse como entrada principal de un único proceso operativo:
Cotización a cliente, Venta directa o Recepción de mercadería.
