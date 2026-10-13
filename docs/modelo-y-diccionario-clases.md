# Modelo y diccionario de clases

## Índice

- [Diagrama de clases](#diagrama-clases)
- [Capa de presentación](#controllers)
- [Capa de negocio](#services)
- [Capa de acceso a datos](#repositories)
- [Capa de dominio](#models)
- [Clases asociativas](#asociativas)
- [Enumeraciones](#enumeraciones)

<a id="diagrama-clases"></a>

## Diagrama de clases (UML)

![Diagrama de clases de MIGAL SmartOps](imagenes/MIGAL-CLA-01-diagrama-clases.png)

<a id="controllers"></a>

## Capa de presentación (Controllers)

Encargada de exponer los endpoints de la API REST, recibir las peticiones HTTP provenientes del frontend y delegar su procesamiento a la capa de negocio. No contiene lógica de negocio.

| Clase | Responsabilidad principal | Métodos clave |
| --- | --- | --- |
| `AuthController` | Puerta de acceso y autenticación del sistema. | `login(credenciales): Token` |
| `UsuarioController` | Gestión administrativa de usuarios, cuentas y roles. | `getUsuarios()`<br>`postNuevoUsuario(datos)`<br>`putActualizarRol(idUsuario, rol)` |
| `ProductoController` | Recepción de solicitudes relacionadas con el catálogo maestro de productos. | `getFichaTecnica(sku)`<br>`postNuevoProducto(datos)`<br>`putActualizarProducto(idProducto, datos)` |
| `InventarioController` | Interfaz para consultas de stock, historial de Kardex y operaciones manuales de inventario. | `getStockPorUbicacion(sku): List<Inventario>`<br>`getHistorialKardex(sku, idAlmacen): List<MovimientoKardex>`<br>`postMovimientoManual(datos)`<br>`postTrasladoInterno(datos)` |
| `VentaController` | Recepción de las operaciones destinadas al registro de ventas. | `postNuevaVenta(datos)` |
| `CotizacionController` | Recepción de operaciones de creación, actualización y conversión de cotizaciones a clientes. | `postNuevaCotizacion(datos)`<br>`putActualizarEstado(idCotizacion, estado)`<br>`postConvertirAVenta(idCotizacion)` |
| `DespachoController` | Gestión de las ventas pendientes de entrega y generación de documentos de despacho. | `getVentasPendientes()`<br>`postGenerarGuia(idVenta)` |
| `AbastecimientoController` | Puerta de entrada para proveedores, órdenes de compra, recepciones de mercadería y procesamiento documental asociado a recepción. | `getProveedores()`<br>`postOrdenCompra(datos)`<br>`postOrdenRecepcion(datos)`<br>`postNuevoProveedor(datos)`<br>`postProcesarDocumentoRecepcion( archivo)` |
| `OcrDigitalController` | Interfaz genérica para la carga de documentos que serán procesados mediante OCR y Fuzzy Matching. | `uploadDocumento(archivo, tipoDocumento)` |
| `ChatIAController` | Recepción de consultas en lenguaje natural dirigidas al Agente Consultivo. | `postPreguntaAgente(mensaje)` |

<a id="services"></a>

## Capa de negocio (Services)

Es el núcleo de la aplicación. Contiene el 100 % de la lógica del negocio, las validaciones, los cálculos matemáticos y la orquestación de operaciones.

| Clase | Responsabilidad principal | Lógica de negocio clave |
| --- | --- | --- |
| `UsuarioService` | Validación de credenciales y administración de usuarios y roles. | `validarCredenciales(user, pass): Boolean`<br>`verificarDuplicidadUsername(username): Boolean`<br>`guardarUsuario(usuario): Usuario`<br>`listarUsuarios(): List<Usuario>`<br>`actualizarRol(idUsuario, rol): Usuario` |
| `ProductoService` | Mantener la integridad y consistencia del catálogo maestro. | `verificarExistenciaSku(sku): Boolean`<br>`registrarProducto(producto): Producto`<br>`actualizarProducto(idProducto, datos): Producto`<br>`obtenerFichaTecnica(sku): Producto` |
| `KardexService` | Motor transaccional del inventario y encargado de asegurar la coherencia del stock entre ubicaciones. | `consultarDisponibilidad(producto): Integer`<br>`consultarStockPorUbicacion(sku): List<Inventario>`<br>`consultarHistorialKardex(sku, idAlmacen): List<MovimientoKardex>`<br>`validarStockSuficiente(producto, cantidad): Boolean`<br>`registrarMovimiento(movimiento): Boolean`<br>`ejecutarTrasladoPartidaDoble(idOrigen, idDestino, sku, cantidad): Boolean` |
| `VentaService` | Procesamiento transaccional de ventas, independientemente de si se originan directamente o desde una cotización aceptada. | `procesarVenta(venta, detalles): Venta` |
| `CotizacionService` | Gestión del ciclo de vida de cotizaciones y de su eventual conversión en venta. | `crearCotizacion(datos): Cotizacion`<br>`actualizarEstado(idCotizacion, estado): Cotizacion`<br>`verificarVigencia(cotizacion): Boolean`<br>`convertirAVenta(idCotizacion): Venta` |
| `DespachoService` | Validación y emisión de documentos asociados a la entrega física de ventas registradas. | `obtenerVentasPendientes(): List<Venta>`<br>`validarGuiaExistente(idVenta): Boolean`<br>`crearGuiaDespacho(venta): GuiaDespacho` |
| `AbastecimientoService` | Gestión de proveedores, órdenes de compra, recepciones parciales o completas y apoyo documental durante la recepción. | `crearOrdenCompra(orden): OrdenCompra`<br>`registrarRecepcionFisica(idOrden, recepcion): Boolean`<br>`validarCantidadesRecibidas(cantSolicitada, cantReal): EstadoCompra`<br>`registrarProveedor(proveedor): Proveedor`<br>`listarProveedores(): List<Proveedor>`<br>`asociarCatalogoProveedor(idProveedor, idProducto, precio): Boolean`<br>`procesarDocumentoRecepcion(documento): List<DetalleExtraido>`<br>`precargarRecepcionDesdeDocumento(documento): OrdenRecepcion` |
| `OcrIAIntegratorService` | Comunicación con el microservicio Python y normalización de información extraída mediante OCR y Fuzzy Matching. | `procesarDocumento(documento): List<DetalleExtraido>`<br>`enviarDocumentoMicroservicio(documento)`<br>`ejecutarFuzzyMatching(termino)`<br>`asociarProducto(detalle, producto)` |
| `AgenteConsultivoService` | Interpretación de consultas en lenguaje natural y coordinación del Tool Calling autorizado. | `procesarConsultaLenguajeNatural(query): String` |

<a id="repositories"></a>

## Capa de acceso a datos (Repositories)

Actúan como interfaces (Data Access Objects). Su única responsabilidad es traducir las sentencias a SQL para interactuar con la base de datos PostgreSQL.

| Clase / Interfaz | Responsabilidad principal |
| --- | --- |
| `UsuarioRepository` | Persistencia y consulta de usuarios y credenciales. |
| `ProductoRepository` | Persistencia y consulta del catálogo maestro de productos. |
| `InventarioRepository` | Acceso a la información de existencias de productos por ubicación. |
| `MovimientoKardexRepository` | Persistencia y consulta del historial de movimientos del Kardex. |
| `VentaRepository` | Persistencia y consulta de ventas registradas. |
| `CotizacionRepository` | Persistencia y consulta de cotizaciones y sus estados. |
| `GuiaDespachoRepository` | Persistencia y consulta de documentos de despacho asociados a ventas. |
| `OrdenCompraRepository` | Persistencia y consulta de órdenes de compra. |
| `OrdenRecepcionRepository` | Persistencia y consulta de recepciones asociadas a órdenes de compra. |
| `DocumentoDigitalRepository` | Persistencia y consulta de documentos cargados para procesamiento OCR. |
| `DetalleExtraidoRepository` | Persistencia de los elementos obtenidos mediante OCR, incluyendo aquellos todavía pendientes de asociación con un producto. |
| `ProveedorRepository` | Persistencia y consulta del directorio de proveedores. |

<a id="models"></a>

## Capa de dominio (Models)

Son las clases de entidad (JPA Entities). Representan la estructura de información en la memoria RAM.

| Clase | Responsabilidad principal | Elementos principales |
| --- | --- | --- |
| `Usuario` | Representar a los colaboradores autenticados del sistema. | `idUsuario`<br>`nombres`<br>`apellidos`<br>`username`<br>`passwordHash`<br>`rol` |
| `Almacen` | Representar cada ubicación física donde se mantienen existencias. | `idAlmacen`<br>`nombre` |
| `Producto` | Representar los artículos del catálogo maestro. | `idProducto`<br>`sku`<br>`nombreOficial`<br>`descripcion`<br>`unidadMedida`<br>`precioReferencial`<br>`stockMinimo`<br>`estadoActivo` |
| `Venta` | Representar una operación comercial confirmada que produce salida de inventario. | `idVenta`<br>`fechaVenta`<br>`clienteNombre`<br>`clienteDocumento`<br>`totalVenta`<br>`descuento`<br>`usuario`<br>`calcularTotalVenta()` |
| `Cotizacion` | Representar una propuesta comercial dirigida a un cliente antes de concretar una venta. | `idCotizacion`<br>`fechaEmision`<br>`fechaVencimiento`<br>`clienteNombre`<br>`clienteDocumento`<br>`condicionesComerciales`<br>`descuento`<br>`estado`<br>`totalCotizacion`<br>`usuario`<br>`calcularTotal()` |
| `GuiaDespacho` | Representar el documento utilizado para controlar la entrega física correspondiente a una venta. | `idGuia`<br>`fechaDespacho`<br>`direccionEntrega`<br>`estado` |
| `MovimientoKardex` | Representar cada entrada o salida que afecta físicamente las existencias. | `idMovimiento`<br>`fechaHora`<br>`tipoOperacion`<br>`cantidad`<br>`motivo`<br>`usuario` |
| `OrdenCompra` | Representar una compra previamente acordada con un proveedor. | `idOrden`<br>`fechaEmision`<br>`estado`<br>`usuario` |
| `OrdenRecepcion` | Representar cada recepción física correspondiente a una Orden de Compra. | `idRecepcion`<br>`fechaRecepcion`<br>`estado`<br>`usuario` |
| `Proveedor` | Representar los proveedores registrados y utilizados durante el abastecimiento. | `idProveedor`<br>`ruc`<br>`razonSocial`<br>`contacto`<br>`condiciones` |
| `DocumentoDigital` | Representar los archivos operativos cargados para procesamiento OCR. | `idDocumento`<br>`rutaArchivo`<br>`fechaSubida`<br>`estadoProcesamiento`<br>`tipoDocumento`<br>`usuario` |
| `DetalleExtraido` | Representar cada elemento textual o cantidad identificada dentro de un DocumentoDigital, aun cuando todavía no posea producto asociado. | `idDetalleExtraido`<br>`textoCrudo`<br>`cantidadLeida` |

<a id="asociativas"></a>

## Clases asociativas de la capa de dominio

| Clase asociativa | Clases relacionadas | Responsabilidad |
| --- | --- | --- |
| `Inventario` | Producto + Almacen | Representar las existencias disponibles de cada producto en cada ubicación. Contiene cantidadDisponible. |
| `DetalleVenta` | Venta + Producto | Representar los productos incluidos en una venta, conservando cantidadVendida y precioUnitario. |
| `DetalleCotizacion` | Cotizacion + Producto | Representar los productos incluidos en una cotización, conservando cantidadCotizada y precioUnitario. |
| `DetalleCompra` | OrdenCompra + Producto | Representar los productos solicitados en una Orden de Compra mediante cantidadComprada y costoUnitario. |
| `DetalleRecepcion` | OrdenRecepcion + Producto | Representar las cantidades efectivamente recibidas de cada producto mediante cantidadRecibida. |
| `CatalogoProveedor` | Proveedor + Producto | Representar la asociación comercial entre proveedores y productos, incluyendo precioCompra. |

<a id="enumeraciones"></a>

## Enumeraciones del modelo de dominio

| Enumeración | Valores | Finalidad |
| --- | --- | --- |
| `RolUsuario` | `Administrador`<br>`Vendedor`<br>`Almacenero`<br>`Reponedor` | Controlar el nivel de acceso y permisos de los usuarios internos. |
| `TipoOperacion` | `Entrada`<br>`Salida` | Identificar el sentido de cada movimiento de Kardex. |
| `EstadoGuia` | `Impresa`<br>`En_Ruta`<br>`Entregada` | Representar el ciclo operativo de una guía de despacho. |
| `EstadoCompra` | `Pendiente`<br>`Parcial`<br>`Completada` | Representar el grado de cumplimiento de una Orden de Compra. |
| `EstadoRecepcion` | `Pendiente`<br>`Validada` | Representar el estado de validación de una recepción física. |
| `EstadoDocumento` | `Pendiente`<br>`Procesado_OCR`<br>`Validado` | Representar el estado del procesamiento de un DocumentoDigital. |
| `TipoDocumento` | `SolicitudCliente`<br>`PedidoCliente`<br>`DocumentoRecepcion` | Identificar el contexto operativo del documento cargado al módulo OCR. |
| `EstadoCotizacion` | `Emitida`<br>`Aceptada`<br>`Rechazada`<br>`Vencida` | Representar el ciclo de vida comercial de una cotización. |
