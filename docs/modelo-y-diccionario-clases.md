# Modelo y diccionario de clases

Este documento presenta el modelo de clases de MIGAL SmartOps, organizado según las capas de la arquitectura de software propuesta para el sistema.

El modelo identifica las principales clases, sus responsabilidades, atributos, métodos y relaciones, permitiendo representar los procesos de gestión comercial, catálogo de productos, inventario, abastecimiento, recepción de mercadería y despacho.

La actualización incorpora la gestión independiente de clientes y proveedores, el control de existencias físicas y reservadas por almacén, y la atención de ventas que requieren abastecimiento adicional. Asimismo, diferencia el registro comercial de una venta de los movimientos físicos de Kardex, conservando la trazabilidad de las operaciones.

Las clases se organizan en las capas de presentación, negocio, acceso a datos y dominio, de acuerdo con la arquitectura tecnológica definida para MIGAL SmartOps.

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

El diagrama de clases representa la estructura del sistema, identificando las principales entidades del dominio, sus atributos y relaciones, así como los componentes responsables de gestionar las operaciones.

Se consideran las relaciones entre cotizaciones, ventas, clientes, productos, inventarios, reservas, proveedores, órdenes de compra, recepciones y guías de despacho.

La asociación entre `DetalleVenta`, `ReservaStock` e `Inventario` permite representar las cantidades comprometidas para una venta desde una o varias ubicaciones físicas. De esta manera, las reservas se administran independientemente de las existencias físicas, que únicamente se modifican mediante movimientos confirmados de Kardex.

![Diagrama de clases de MIGAL SmartOps](imagenes/MIGAL-CLA-01-diagrama-clases.png)

Las multiplicidades del diagrama deberán mantener correspondencia con las relaciones definidas en el modelo entidad-relación. Los atributos derivados, como la cantidad libre o la cantidad pendiente de abastecimiento, podrán calcularse mediante operaciones del sistema sin almacenarse como datos independientes.

<a id="controllers"></a>

## Capa de presentación (Controllers)

Encargada de exponer los endpoints de la API REST, recibir las peticiones HTTP provenientes del frontend, validar los datos de entrada y delegar su procesamiento a la capa de negocio. No ejecuta directamente las reglas comerciales ni modifica las existencias del inventario.

| Clase | Responsabilidad principal | Métodos clave |
| --- | --- | --- |
| `AuthController` | Puerta de acceso y autenticación del sistema. | `login(credenciales): Token` |
| `UsuarioController` | Gestión administrativa de usuarios, cuentas, roles y estados de acceso. | `getUsuarios()`<br>`postNuevoUsuario(datos)`<br>`putActualizarRol(idUsuario, rol)`<br>`putActualizarEstado(idUsuario, estado)` |
| `ProductoController` | Recepción de solicitudes relacionadas con el catálogo maestro de productos. | `getFichaTecnica(sku)`<br>`postNuevoProducto(datos)`<br>`putActualizarProducto(idProducto, datos)` |
| `ClienteController` | Gestión de clientes registrados y consulta de sus datos comerciales. | `getClientes()`<br>`getCliente(idCliente)`<br>`postNuevoCliente(datos)`<br>`putActualizarCliente(idCliente, datos)`<br>`putActualizarEstado(idCliente, estado)` |
| `InventarioController` | Interfaz para consultar existencias físicas, reservadas y libres, historial de Kardex y operaciones manuales de inventario. | `getStockPorUbicacion(sku): List<Inventario>`<br>`getDisponibilidadGlobal(sku)`<br>`getHistorialKardex(sku, idAlmacen, fechas)`<br>`postMovimientoManual(datos)`<br>`postTrasladoInterno(datos)` |
| `VentaController` | Recepción de operaciones relacionadas con ventas aprobadas, cantidades pendientes, reservas y cancelaciones. | `getVentas()`<br>`getDetalleVenta(idVenta)`<br>`getVentasPendientes()`<br>`postNuevaVentaDesdeCotizacion(idCotizacion)`<br>`postCancelarVenta(idVenta, motivo)` |
| `CotizacionController` | Recepción de operaciones de emisión, aceptación, validación y conversión de cotizaciones en ventas. | `postNuevaCotizacion(datos)`<br>`getCotizacion(idCotizacion)`<br>`putActualizarEstado(idCotizacion, estado)`<br>`postRegistrarAceptacion(idCotizacion)`<br>`postValidarViabilidad(idCotizacion)`<br>`postConvertirAVenta(idCotizacion)` |
| `DespachoController` | Gestión de ventas pendientes de entrega, programación, generación de guías y confirmación de salida física. | `getVentasPendientes()`<br>`postGenerarGuia(idVenta)`<br>`postProgramarDespacho(idVenta, fecha)`<br>`postConfirmarSalida(idGuia)` |
| `AbastecimientoController` | Puerta de entrada para proveedores, órdenes de compra, recepciones de mercadería y procesamiento documental asociado. | `getProveedores()`<br>`postNuevoProveedor(datos)`<br>`putActualizarProveedor(idProveedor, datos)`<br>`putActualizarEstadoProveedor(idProveedor, estado)`<br>`postOrdenCompra(datos)`<br>`postOrdenRecepcion(datos)`<br>`postConfirmarRecepcion(idRecepcion)`<br>`postProcesarDocumentoRecepcion(archivo)` |
| `OcrDigitalController` | Interfaz para la carga y procesamiento de documentos mediante OCR y Fuzzy Matching. | `uploadDocumento(archivo, tipoDocumento)`<br>`getResultadoDocumento(idDocumento)`<br>`postValidarExtraccion(idDocumento, correcciones)` |
| `ChatIAController` | Recepción de consultas en lenguaje natural dirigidas al Agente Consultivo. | `postPreguntaAgente(mensaje)` |

El registro de una venta se realiza a partir de una cotización previamente aceptada y validada. Por tanto, `VentaController` no contempla la creación de ventas directas independientes de una cotización.

La incorporación de `ClienteController` permite gestionar la entidad Cliente de manera independiente, mientras que las operaciones de proveedores se mantienen dentro de `AbastecimientoController`, evitando duplicar responsabilidades.

<a id="services"></a>

## Capa de negocio (Services)

Constituye el núcleo funcional de MIGAL SmartOps. Contiene las reglas de negocio, las validaciones, los cálculos y la coordinación de operaciones entre las entidades del dominio y la capa de persistencia.

Esta capa es responsable de mantener la coherencia de los procesos comerciales y del inventario, aplicando mecanismos transaccionales para evitar reservas duplicadas, movimientos inconsistentes y cantidades físicas negativas.

| Clase | Responsabilidad principal | Lógica de negocio clave |
| --- | --- | --- |
| `UsuarioService` | Validación de credenciales y administración de usuarios y roles. | `validarCredenciales(user, pass): Boolean`<br>`verificarDuplicidadUsername(username): Boolean`<br>`guardarUsuario(usuario): Usuario`<br>`listarUsuarios(): List<Usuario>`<br>`actualizarRol(idUsuario, rol): Usuario`<br>`actualizarEstado(idUsuario, estado): Usuario` |
| `ProductoService` | Mantener la integridad y consistencia del catálogo maestro, incluyendo categorías, precios referenciales y stock mínimo. | `verificarExistenciaSku(sku): Boolean`<br>`registrarProducto(producto): Producto`<br>`actualizarProducto(idProducto, datos): Producto`<br>`obtenerFichaTecnica(sku): Producto`<br>`validarCategoriaProducto(categoria): Boolean` |
| `ClienteService` | Registrar, consultar, actualizar y desactivar clientes, conservando sus asociaciones comerciales. | `registrarCliente(cliente): Cliente`<br>`buscarCliente(identificador): Cliente`<br>`listarClientes(): List<Cliente>`<br>`actualizarCliente(idCliente, datos): Cliente`<br>`actualizarEstado(idCliente, estado): Cliente` |
| `InventarioService` | Consultar y administrar las existencias físicas, reservadas y libres por producto y almacén, así como las reservas asociadas a ventas. | `consultarStockPorUbicacion(sku): List<Inventario>`<br>`consultarDisponibilidadGlobal(sku): Decimal`<br>`calcularStockLibre(idInventario): Decimal`<br>`reservarDesdeUbicaciones(idDetalleVenta, cantidad): List<ReservaStock>`<br>`consultarReservas(idVenta): List<ReservaStock>`<br>`liberarReservas(idVenta): Boolean` |
| `KardexService` | Registrar y consultar movimientos físicos de inventario, asegurando la integridad de saldos y la trazabilidad por ubicación. | `consultarHistorialKardex(sku, idAlmacen, fechas): List<MovimientoKardex>`<br>`registrarMovimiento(movimiento): Boolean`<br>`registrarEntradaPorRecepcion(idRecepcion): Boolean`<br>`registrarSalidaPorGuia(idGuia): Boolean`<br>`ejecutarTrasladoPartidaDoble(idOrigen, idDestino, sku, cantidad): Boolean`<br>`registrarAjusteInventario(datos): Boolean` |
| `VentaService` | Registrar ventas originadas en cotizaciones aprobadas, coordinar reservas, administrar faltantes y gestionar cancelaciones autorizadas. | `registrarVentaDesdeCotizacion(idCotizacion): Venta`<br>`calcularCantidadesPendientes(idVenta)`<br>`consultarVentasPendientes(): List<Venta>`<br>`actualizarEstadoVenta(idVenta, estado): Venta`<br>`cancelarVentaAntesDespacho(idVenta, motivo): Venta` |
| `CotizacionService` | Gestionar el ciclo de vida de cotizaciones, su aceptación, evaluación de viabilidad comercial y conversión única en venta. | `crearCotizacion(datos): Cotizacion`<br>`actualizarEstado(idCotizacion, estado): Cotizacion`<br>`verificarVigencia(cotizacion): Boolean`<br>`registrarAceptacion(idCotizacion): Cotizacion`<br>`validarViabilidadAbastecimiento(idCotizacion): Boolean`<br>`convertirAVenta(idCotizacion): Venta` |
| `DespachoService` | Gestionar las guías de despacho, verificar la disponibilidad completa de los productos comprometidos y confirmar su salida física. | `obtenerVentasPendientes(): List<Venta>`<br>`validarGuiaExistente(idVenta): Boolean`<br>`validarCoberturaCompleta(idVenta): Boolean`<br>`crearGuiaDespacho(venta): GuiaDespacho`<br>`programarDespacho(idVenta, fecha): GuiaDespacho`<br>`confirmarSalidaCompleta(idGuia): Boolean` |
| `AbastecimientoService` | Gestionar proveedores, órdenes de compra, recepciones completas o parciales de proveedores y necesidades pendientes de abastecimiento. | `crearOrdenCompra(orden): OrdenCompra`<br>`registrarRecepcionFisica(idOrden, recepcion): OrdenRecepcion`<br>`confirmarRecepcion(idRecepcion): Boolean`<br>`validarCantidadesRecibidas(cantSolicitada, cantReal): EstadoCompra`<br>`registrarProveedor(proveedor): Proveedor`<br>`actualizarProveedor(idProveedor, datos): Proveedor`<br>`desactivarProveedor(idProveedor): Proveedor`<br>`listarProveedores(): List<Proveedor>`<br>`validarProveedorActivo(idProveedor): Boolean`<br>`asociarCatalogoProveedor(idProveedor, idProducto, precio): Boolean`<br>`procesarDocumentoRecepcion(documento): List<DetalleExtraido>` |
| `OcrIAIntegratorService` | Comunicación con el microservicio Python y normalización de información extraída mediante OCR y Fuzzy Matching. | `procesarDocumento(documento): List<DetalleExtraido>`<br>`enviarDocumentoMicroservicio(documento)`<br>`ejecutarFuzzyMatching(termino)`<br>`asociarProducto(detalle, producto)`<br>`validarDatosExtraidos(documento, correcciones)` |
| `AgenteConsultivoService` | Interpretación de consultas en lenguaje natural y coordinación del Tool Calling autorizado. | `procesarConsultaLenguajeNatural(query): String`<br>`validarPermisosConsulta(idUsuario, herramienta): Boolean` |

### Consideraciones de consistencia

**Cotizaciones y ventas.** La emisión o aceptación de una cotización no implica el registro automático de una venta. La conversión requiere la validación de viabilidad por MIGAL y solo puede realizarse una vez por cotización aprobada.

**Abastecimiento con stock insuficiente.** Para cemento, ladrillos, fierros y agregados se contempla la aceptación automática de la viabilidad de abastecimiento. En las demás categorías, la decisión dependerá de la evaluación de la propietaria responsable, considerando las condiciones y plazos de atención.

**Inventario y reservas.** `InventarioService` gestiona la asignación de existencias a cada venta, mientras que `KardexService` registra las entradas y salidas físicas. Una reserva no modifica las existencias físicas ni genera movimientos de Kardex.

**Recepciones.** El registro de una orden de compra no modifica el inventario. La confirmación de las cantidades efectivamente recibidas genera los movimientos de entrada correspondientes.

**Despacho.** La generación o impresión de una guía no produce una salida de Kardex. Los movimientos de salida se registran cuando se confirma que la mercadería completa abandona físicamente el almacén.

**Cancelaciones.** La cancelación autorizada antes del despacho debe liberar las reservas asociadas, registrar el motivo y conservar la evidencia del usuario que autorizó la operación, sin generar movimientos físicos inexistentes.

Las operaciones que afecten existencias y reservas deberán realizarse de forma transaccional para evitar inconsistencias ante accesos concurrentes o fallas durante su procesamiento.

<a id="repositories"></a>

## Capa de acceso a datos (Repositories)

Actúa como la capa de persistencia del sistema mediante interfaces que permiten consultar, registrar y actualizar la información almacenada en PostgreSQL.

Los repositorios proporcionan operaciones de acceso a datos a los servicios, sin implementar directamente las reglas comerciales o de inventario.

| Clase / Interfaz | Responsabilidad principal |
| --- | --- |
| `UsuarioRepository` | Persistencia y consulta de usuarios, credenciales, roles y estados de acceso. |
| `ProductoRepository` | Persistencia y consulta del catálogo maestro de productos. |
| `ClienteRepository` | Persistencia, consulta, actualización y desactivación de clientes. |
| `InventarioRepository` | Acceso a existencias físicas por producto y almacén, incluyendo consultas y bloqueos transaccionales para reservas. |
| `ReservaStockRepository` | Persistencia y consulta de las reservas asociadas a detalles de venta e inventarios específicos. |
| `MovimientoKardexRepository` | Persistencia y consulta del historial de movimientos físicos de inventario. |
| `VentaRepository` | Persistencia y consulta de ventas, sus estados y detalles comerciales. |
| `CotizacionRepository` | Persistencia y consulta de cotizaciones, estados y datos comerciales. |
| `GuiaDespachoRepository` | Persistencia y consulta de guías, programación y confirmación de salidas físicas. |
| `OrdenCompraRepository` | Persistencia y consulta de órdenes de compra asociadas a proveedores. |
| `OrdenRecepcionRepository` | Persistencia y consulta de recepciones asociadas a órdenes de compra. |
| `DocumentoDigitalRepository` | Persistencia y consulta de documentos cargados para procesamiento OCR. |
| `DetalleExtraidoRepository` | Persistencia de elementos extraídos mediante OCR, incluidos aquellos pendientes de asociación con un producto. |
| `ProveedorRepository` | Persistencia, consulta, actualización, activación y desactivación de proveedores. |

Las clases asociativas de detalle y catálogo podrán gestionarse mediante los repositorios de sus entidades principales, manteniendo las restricciones de integridad definidas en el modelo de datos.

La incorporación de `ReservaStockRepository` permitirá consultar las reservas activas de cada inventario y conservar el historial de aquellas que hayan sido liberadas o consumidas.

<a id="models"></a>

## Capa de dominio (Models)

Contiene las clases que representan las entidades y conceptos principales de MIGAL SmartOps. Estas clases mantienen la información necesaria para describir las operaciones y sus relaciones, pudiendo implementarse como entidades JPA en el backend Java.

Los atributos se expresan mediante nomenclatura camelCase. Los identificadores y claves foráneas correspondientes se detallan en el modelo entidad-relación y en el diccionario de datos.

| Clase | Responsabilidad principal | Elementos principales |
| --- | --- | --- |
| `Usuario` | Representar a los colaboradores autenticados, sus roles y el estado de acceso al sistema. | `idUsuario`<br>`nombres`<br>`apellidos`<br>`username`<br>`passwordHash`<br>`rol`<br>`estadoActivo`<br>`fechaRegistro`<br>`fechaActualizacion` |
| `Cliente` | Representar a la persona o entidad asociada a una cotización o venta, conservando su identificación e historial comercial. | `idCliente`<br>`tipoDocumento`<br>`numeroDocumento`<br>`nombreRazonSocial`<br>`telefonoContacto`<br>`direccionReferencia`<br>`estadoActivo`<br>`fechaRegistro`<br>`activar()`<br>`desactivar()` |
| `Almacen` | Representar cada ubicación física donde se mantienen existencias de productos. | `idAlmacen`<br>`nombre`<br>`tipoUbicacion`<br>`direccionReferencia`<br>`estadoActivo` |
| `Producto` | Representar los artículos del catálogo maestro y sus características comerciales. | `idProducto`<br>`sku`<br>`nombreOficial`<br>`descripcion`<br>`unidadMedida`<br>`precioReferencial`<br>`stockMinimo`<br>`estadoActivo`<br>`categoriaProducto`<br>`marca`<br>`presentacion`<br>`fechaRegistro`<br>`fechaActualizacion` |
| `Venta` | Representar el pedido comercial aprobado, sus importes y el estado de abastecimiento o despacho. | `idVenta`<br>`idCotizacion`<br>`idCliente`<br>`idUsuarioResponsable`<br>`fechaVenta`<br>`descuento`<br>`totalVenta`<br>`estado`<br>`fechaCancelacion`<br>`motivoCancelacion`<br>`calcularTotalVenta()`<br>`calcularPendiente()` |
| `Cotizacion` | Representar la propuesta comercial presentada al cliente, su aceptación y la validación de viabilidad por MIGAL. | `idCotizacion`<br>`idCliente`<br>`idUsuarioResponsable`<br>`fechaEmision`<br>`fechaVencimiento`<br>`condicionesComerciales`<br>`descuento`<br>`totalCotizacion`<br>`estado`<br>`fechaAceptacionCliente`<br>`fechaValidacionMigal`<br>`calcularTotal()`<br>`verificarVigencia()` |
| `GuiaDespacho` | Representar el documento utilizado para controlar la preparación y salida física completa de una venta. | `idGuia`<br>`idVenta`<br>`direccionEntrega`<br>`fechaDespacho`<br>`estado`<br>`fechaEmision`<br>`fechaSalidaReal`<br>`idUsuarioResponsable` |
| `MovimientoKardex` | Representar cada entrada o salida física que afecta las existencias de un producto en una ubicación determinada. | `idMovimiento`<br>`idInventario`<br>`tipoOperacion`<br>`cantidad`<br>`fechaHora`<br>`motivo`<br>`idUsuarioResponsable`<br>`origenMovimiento`<br>`idGuia`<br>`idRecepcion`<br>`referenciaTraslado` |
| `OrdenCompra` | Representar una compra acordada con un proveedor, sin modificar directamente las existencias físicas. | `idOrden`<br>`idProveedor`<br>`idUsuarioResponsable`<br>`fechaEmision`<br>`estado`<br>`fechaEntregaEstimada`<br>`observaciones`<br>`modalidadPago`<br>`validarProveedorActivo()` |
| `OrdenRecepcion` | Representar el registro de una recepción física de mercadería asociada a una orden de compra. | `idRecepcion`<br>`idOrden`<br>`idUsuarioResponsable`<br>`fechaRecepcion`<br>`estado`<br>`fechaValidacion`<br>`observaciones` |
| `Proveedor` | Representar a un proveedor registrado, sus datos comerciales y su estado de vigencia. | `idProveedor`<br>`ruc`<br>`razonSocial`<br>`contacto`<br>`condiciones`<br>`estadoActivo`<br>`fechaRegistro`<br>`activar()`<br>`desactivar()` |
| `DocumentoDigital` | Representar los archivos operativos cargados al sistema para su procesamiento mediante OCR. | `idDocumento`<br>`rutaArchivo`<br>`fechaSubida`<br>`estadoProcesamiento`<br>`tipoDocumento`<br>`idUsuario`<br>`idCotizacion`<br>`idRecepcion` |
| `DetalleExtraido` | Representar los elementos obtenidos mediante OCR, conservando el texto original y sus correcciones. | `idDetalleExtraido`<br>`idDocumento`<br>`textoCrudo`<br>`cantidadLeida`<br>`idProducto`<br>`textoCorregido` |

### Consideraciones del modelo de dominio

**Cliente y Proveedor** se mantienen como entidades independientes. Sus relaciones permiten consultar cotizaciones, ventas, compras y recepciones históricas sin duplicar innecesariamente sus datos.

**Venta** representa el pedido aprobado y conserva la relación obligatoria con la cotización que lo originó. No se introduce una clase adicional `Pedido`, debido a que ambos conceptos corresponden a una misma operación comercial en el flujo propuesto.

**Cotizacion** mantiene los datos comerciales y las fechas de aceptación y validación. Los nombres definitivos de sus estados deberán armonizarse con las especificaciones de casos de uso.

**GuiaDespacho** conserva la relación con una única venta. La fecha programada y la fecha de salida real corresponden a eventos diferentes, por lo que no deben utilizarse indistintamente.

**MovimientoKardex** conserva la trazabilidad de cada movimiento físico mediante su inventario, tipo de operación, cantidad, fecha, usuario responsable y operación de origen.

<a id="asociativas"></a>

## Clases asociativas de la capa de dominio

Las clases asociativas representan relaciones que contienen información propia y que deben conservarse como registros independientes dentro del modelo.

En particular, las reservas de stock requieren identificar tanto el detalle de venta como el inventario específico desde el cual se comprometen las unidades.

| Clase asociativa | Clases relacionadas | Atributos principales | Responsabilidad |
| --- | --- | --- | --- |
| `Inventario` | Producto + Almacen | `idInventario`<br>`idProducto`<br>`idAlmacen`<br>`cantidadFisica`<br>`cantidadReservada` (derivado)<br>`cantidadLibre` (derivado) | Representar las existencias físicas de cada producto por ubicación y calcular las cantidades reservadas y libres. |
| `ReservaStock` | DetalleVenta + Inventario | `idReserva`<br>`idDetalleVenta`<br>`idInventario`<br>`cantidadReservada`<br>`estado`<br>`fechaReserva`<br>`fechaCierre`<br>`idUsuarioResponsable` | Registrar las unidades asignadas a cada línea de venta desde un inventario específico, conservando su historial de reserva, liberación o consumo. |
| `DetalleVenta` | Venta + Producto | `idDetalleVenta`<br>`idVenta`<br>`idProducto`<br>`cantidadVendida`<br>`precioUnitario`<br>`subtotalDetalle` (derivado)<br>`cantidadReservadaActual` (derivado)<br>`cantidadPendienteAbastecimiento` (derivado) | Representar los productos y cantidades comercialmente comprometidos, así como consultar su cobertura mediante reservas. |
| `DetalleCotizacion` | Cotizacion + Producto | `idCotizacion`<br>`idProducto`<br>`cantidadCotizada`<br>`precioUnitario`<br>`subtotalDetalle` (derivado) | Representar los productos incluidos en una cotización y conservar las cantidades y precios ofrecidos. |
| `DetalleCompra` | OrdenCompra + Producto | `idOrden`<br>`idProducto`<br>`cantidadComprada`<br>`costoUnitario`<br>`subtotalDetalle` (derivado)<br>`cantidadPendienteRecepcion` (derivado) | Registrar los productos solicitados al proveedor y determinar las cantidades pendientes de recepción. |
| `DetalleRecepcion` | OrdenRecepcion + Producto + Almacen | `idRecepcion`<br>`idProducto`<br>`idAlmacenDestino`<br>`cantidadRecibida` | Representar las cantidades físicamente recibidas y la ubicación donde ingresan al inventario. |
| `CatalogoProveedor` | Proveedor + Producto | `idProveedor`<br>`idProducto`<br>`precioCompra`<br>`plazoEntregaReferencialDias`<br>`fechaActualizacionPrecio` | Representar la relación comercial entre proveedores y productos, incluyendo precios y plazos referenciales de abastecimiento. |

### Reglas de consistencia

- Un producto puede tener registros de inventario en diferentes almacenes, pero una misma combinación de producto y ubicación debe ser única.
- Cada reserva corresponde a un detalle de venta y a un inventario específico, permitiendo asignar unidades de un mismo producto desde varios almacenes.
- La cantidad reservada de un inventario se obtiene de la suma de reservas activas. El stock libre corresponde a las existencias físicas menos las reservas activas.
- La cantidad reservada de una línea de venta no puede superar la cantidad comercialmente comprometida.
- Una reserva no origina movimientos de salida de Kardex. Estos se generan únicamente al confirmar la salida física correspondiente al despacho.
- Las recepciones de proveedores pueden ser parciales y registrarse en diferentes momentos, manteniendo la relación con la orden de compra original.
- Los atributos derivados se calculan mediante métodos o consultas y no requieren necesariamente columnas adicionales en la base de datos.
- La confirmación de operaciones que afecten existencias deberá preservar la integridad transaccional y evitar registros duplicados.

<a id="enumeraciones"></a>

## Enumeraciones del modelo de dominio

Las enumeraciones permiten representar los estados y categorías utilizados por las clases del dominio, evitando el uso de valores arbitrarios en las operaciones del sistema.

| Enumeración | Valores | Finalidad |
| --- | --- | --- |
| `RolUsuario` | `Administrador`<br>`Vendedor`<br>`Almacenero`<br>`Reponedor` | Controlar el nivel de acceso y los permisos de los usuarios internos. |
| `CategoriaProducto` | `Cemento`<br>`Ladrillos`<br>`Fierros`<br>`Agregados`<br>`Otras` | Identificar las categorías utilizadas en las reglas de viabilidad de abastecimiento. |
| `TipoOperacion` | `Entrada`<br>`Salida` | Identificar el sentido de cada movimiento físico de Kardex. |
| `OrigenMovimiento` | `Despacho`<br>`Recepcion`<br>`Ajuste`<br>`Traslado` | Identificar la operación que origina un movimiento de inventario. |
| `EstadoReserva` | `Activa`<br>`Liberada`<br>`Consumida` | Representar el ciclo de vida de una reserva de existencias. |
| `EstadoGuia` | `Preparacion`<br>`Emitida`<br>`SalidaConfirmada`<br>`Entregada` | Representar la preparación, documentación y confirmación de entrega de mercadería. |
| `EstadoCompra` | `Pendiente`<br>`Parcial`<br>`Completada` | Representar el cumplimiento de una orden de compra frente a las recepciones registradas. |
| `EstadoRecepcion` | `Pendiente`<br>`Validada` | Representar la validación de una recepción física de mercadería. |
| `EstadoDocumento` | `Pendiente`<br>`Procesado_OCR`<br>`Validado` | Representar el procesamiento y validación de documentos digitales. |
| `TipoDocumento` | `SolicitudCliente`<br>`PedidoCliente`<br>`DocumentoRecepcion` | Identificar la procedencia y finalidad de los archivos operativos procesados mediante OCR. |
| `EstadoCotizacion` | `Emitida`<br>`AceptadaCliente`<br>`ValidadaMigal`<br>`Rechazada`<br>`Vencida` | Representar la emisión, aceptación del cliente, validación comercial, rechazo y vencimiento de cotizaciones. |
| `EstadoVenta` | `PendienteAbastecimiento`<br>`PendienteDespacho`<br>`ListaDespacho`<br>`Despachada`<br>`Cancelada` | Representar la situación operativa de una venta desde su aprobación hasta el despacho o cancelación. |

Los valores de `EstadoCotizacion`, `EstadoVenta` y `EstadoGuia` constituyen una propuesta coherente con el flujo comercial definido para MIGAL SmartOps. Su nomenclatura y transiciones deberán revisarse junto con los casos de uso definitivos.

El modelo no contempla una enumeración `TipoAtencion` con despacho parcial, debido a que el proceso operativo propuesto diferencia la reserva de cantidades disponibles de la entrega física completa al cliente. Esta decisión deberá conciliarse con los requerimientos de PC2 que actualmente contemplan atención parcial.

Las relaciones, multiplicidades y restricciones de persistencia de estas clases se complementan con el documento [Modelo y diccionario de datos](modelo-y-diccionario-datos.md). Su correspondencia con los procesos y servicios se mantiene mediante la [Matriz de trazabilidad](matriz-trazabilidad.md) y la [Arquitectura tecnológica](arquitectura-tecnologica.md).
