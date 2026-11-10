# Especificaciones de casos de uso

Este documento presenta las especificaciones de los casos de uso de MIGAL SmartOps.

## Índice

- [CU-01: Iniciar sesión](#cu-01)
- [CU-02: Gestionar usuarios y roles](#cu-02)
- [CU-03: Gestionar catálogo de productos](#cu-03)
- [CU-04: Consultar disponibilidad](#cu-04)
- [CU-05: Registrar movimiento](#cu-05)
- [CU-06: Generar orden de despacho](#cu-06)
- [CU-07: Consultar Kardex](#cu-07)
- [CU-08: Procesar documento con OCR](#cu-08)
- [CU-09: Gestionar cotización](#cu-09)
- [CU-10: Registrar venta](#cu-10)
- [CU-11: Registrar proveedor](#cu-11)
- [CU-12: Registrar orden de compra](#cu-12)
- [CU-13: Registrar orden de recepción](#cu-13)
- [CU-14: Consultar agente de IA](#cu-14)

<a id="cu-01"></a>

## CU-01: Iniciar sesión

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-01: Iniciar sesión |
| **Actor principal** | Usuario Interno |
| **Propósito** | Autenticar el acceso seguro a MIGAL SmartOps mediante credenciales personales, garantizando que cada colaborador ingrese únicamente a los módulos autorizados según sus funciones. |
| **Precondiciones** | 1. El usuario debe estar previamente registrado en el sistema.<br>2. El usuario debe tener asignado un rol válido, sea de Administrador, Vendedor, Almacenero o Reponedor.<br>3. El sistema debe encontrarse disponible. |
| **Flujo principal** | 1. El usuario ingresa a la plataforma web a través del navegador.<br>2. El sistema presenta la interfaz de inicio de sesión solicitando nombre de usuario y contraseña.<br>3. El usuario ingresa sus credenciales personales y solicita el acceso.<br>4. El sistema consulta la base de datos PostgreSQL y valida que las credenciales coincidan con los registros.<br>5. El sistema identifica el rol del usuario autenticado.<br>6. El sistema redirige al usuario al panel principal y restringe las funcionalidades disponibles según sus permisos. |
| **Postcondiciones** | La sesión del usuario queda activa. A partir de este momento, cualquier operación transaccional, como el registro de ventas, movimientos de Kardex, entre otros, registrará automáticamente a este usuario como responsable para fines de trazabilidad. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Credenciales Inválidas**<br>1. En el paso 4, el sistema detecta que el usuario no existe o la contraseña es incorrecta.<br>2. El sistema muestra un mensaje de error, por ejemplo: "Usuario o contraseña incorrectos".<br>3. El sistema limpia el campo de contraseña y retorna al paso 2 del flujo principal, permitiendo un nuevo intento.<br>**Flujo Alternativo 2 - Usuario inactivo o sin permisos**<br>1. En el paso 4, las credenciales son correctas, pero el usuario está desactivado o no tiene un rol asignado.<br>2. El sistema muestra un mensaje indicando que el acceso está denegado y debe contactar al administrador.<br>3. El sistema retorna al paso 2 del flujo principal. |

<a id="cu-02"></a>

## CU-02: Gestionar usuarios y roles

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-02: Gestionar usuarios y roles |
| **Actor principal** | Administrador |
| **Propósito** | Administrar los accesos, credenciales y roles del personal de la ferretería para garantizar la seguridad y correcta segregación de funciones. |
| **Precondiciones** | 1. El administrador debe haber iniciado sesión en el sistema mediante credenciales válidas.<br>2. El administrador debe poseer el rol de "Administrador". |
| **Flujo principal** | 1. El administrador ingresa al módulo de "Gestión de Usuarios".<br>2. El sistema despliega la lista del personal registrado y presenta las opciones: "Registrar Nuevo", "Modificar o Asignar Rol" y "Revocar Acceso".<br>3. El administrador selecciona la acción deseada.<br>4. Si selecciona "Registrar Nuevo", el sistema ejecuta el Flujo Alternativo 1.<br>5. Si selecciona "Modificar o Asignar Rol", el sistema ejecuta el Flujo Alternativo 2.<br>6. Si selecciona "Revocar Acceso", el sistema ejecuta el Flujo Alternativo 3. |
| **Postcondiciones** | Los datos, credenciales y permisos del personal quedan actualizados en la base de datos centralizada de forma segura. Si se revoca un acceso, el sistema inactiva la cuenta, pero no elimina el historial del usuario para mantener la trazabilidad de los movimientos de Kardex realizados. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Registrar nuevo usuario**<br>1. El sistema muestra un formulario en blanco.<br>2. El administrador ingresa los datos personales, define un nombre de usuario, una contraseña temporal y selecciona el rol inicial.<br>3. El sistema verifica que el nombre de usuario no esté duplicado.<br>4. El sistema guarda el nuevo registro y retorna al paso 2 del flujo principal.<br>**Flujo Alternativo 1.1 - Nombre de usuario ya existente**<br>1. Si en el paso 3 del flujo alternativo 1 el sistema detecta que el nombre de usuario ya existe, muestra un mensaje de alerta y solicita ingresar uno distinto.<br>**Flujo Alternativo 2 - Modificar o Asignar Rol**<br>1. El administrador selecciona un usuario de la lista.<br>2. El sistema muestra los detalles actuales y el rol asignado.<br>3. El administrador modifica o asigna el rol.<br>4. El sistema actualiza la información y retorna al paso 2 del flujo principal.<br>**Flujo Alternativo 3 - Revocar Accesos**<br>1. El administrador selecciona un usuario y elige la opción de revocar acceso.<br>2. El sistema solicita confirmación.<br>3. El administrador confirma la acción.<br>4. El sistema inactiva las credenciales del usuario, impidiendo futuros inicios de sesión. |

<a id="cu-03"></a>

## CU-03: Gestionar catálogo de productos

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-03: Gestionar catálogo de productos |
| **Actor principal** | Almacenero o Administrador |
| **Propósito** | Administrar el catálogo maestro de la ferretería, permitiendo el registro, modificación y consulta de los productos, así como el establecimiento de umbrales para las alertas de stock mínimo. |
| **Precondiciones** | 1. El usuario debe haber iniciado sesión exitosamente.<br>2. El usuario debe poseer el rol de "Almacenero" o "Administrador". |
| **Flujo principal** | 1. El usuario ingresa al módulo "Productos".<br>2. El sistema muestra la lista general de productos y presenta las opciones: "Registrar Nuevo", "Modificar" y "Consultar Detalles".<br>3. El usuario selecciona la acción deseada.<br>4. Si selecciona "Registrar Nuevo", el sistema ejecuta el Flujo Alternativo 1.<br>5. Si selecciona "Modificar", el sistema ejecuta el Flujo Alternativo 2.<br>6. Si selecciona "Consultar Detalles", el sistema ejecuta el Flujo Alternativo 3. |
| **Postcondiciones** | El catálogo maestro de productos queda actualizado de forma centralizada en la base de datos PostgreSQL. Estas operaciones son administrativas y no generan movimientos transaccionales en el Kardex. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Registrar Nuevo Producto**<br>1. El sistema muestra un formulario de registro.<br>2. El usuario ingresa el código único (SKU), nombre oficial, descripción, unidad de medida y estado (activo).<br>3. El usuario establece la cantidad numérica para el nivel mínimo de stock.<br>4. El sistema verifica que el SKU ingresado no exista previamente en la base de datos.<br>5. El sistema guarda el producto y retorna al paso 2 del flujo principal.<br>**Flujo Alternativo 1.1 - Registrar Producto Existente**<br>1. Si en el paso 4 del flujo alternativo 1 el sistema detecta que el código SKU ya existe, bloquea el registro, muestra un mensaje de error y solicita ingresar un código distinto.<br>**Flujo Alternativo 2 - Modificar Producto**<br>1. El usuario selecciona un producto de la lista.<br>2. El sistema recupera y muestra los datos vigentes.<br>3. El usuario actualiza los campos permitidos, por ejemplo, actualizar unidad de medida o modificar el stock mínimo.<br>4. El sistema valida los datos, guarda los cambios y retorna al flujo principal.<br>**Flujo Alternativo 3 - Consultar Detalles**<br>1. El usuario selecciona un producto o ingresa un criterio de búsqueda, como el SKU.<br>2. El sistema muestra la ficha técnica completa del producto sin opciones de edición. |

<a id="cu-04"></a>

## CU-04: Consultar disponibilidad

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-04: Consultar disponibilidad |
| **Actor principal** | Almacenero o Vendedor |
| **Propósito** | Consultar la disponibilidad de un producto específico en la tienda principal y en cada uno de los dos almacenes para informar sobre el stock real en tiempo real. |
| **Precondiciones** | 1. El usuario autorizado debe haber iniciado sesión exitosamente.<br>2. El usuario autorizado debe poseer el rol de "Almacenero" o "Vendedor" |
| **Flujo principal** | 1. El usuario autorizado ingresa al módulo de consultas de inventario.<br>2. El usuario autorizado ingresa el criterio de búsqueda, que puede ser SKU oficial, nombre o término comercial.<br>3. El sistema consulta las existencias en tiempo real dentro del Kardex.<br>4. El sistema presenta los resultados mostrando la disponibilidad del producto segmentada claramente por ubicación física, sea la tienda principal, el almacén 1 o el almacén 2. |
| **Postcondiciones** | Ninguna. Se trata de una consulta de solo lectura que no modifica las existencias físicas ni genera registros en el Kardex. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Producto No Encontrado**<br>1. Si el criterio ingresado no coincide con ningún registro, el sistema muestra un mensaje indicando que no existen coincidencias.<br>**Flujo Alternativo 2 - Término Ambiguo**<br>1. Si el usuario autorizado ingresa un nombre informal que no coincide exactamente, el sistema invoca la búsqueda por nivel de similitud.<br>2. El sistema sugiere el/los productos con mayor coincidencia.<br>3. El usuario autorizado selecciona el producto correcto para visualizar su stock. |

<a id="cu-05"></a>

## CU-05: Registrar movimiento

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-05: Registrar movimiento |
| **Actor principal** | Almacenero |
| **Propósito** | Registrar movimientos manuales de inventario, tales como mermas o ajustes por descuadre, y traslados internos entre almacenes, manteniendo la precisión del stock físico y la trazabilidad. Estos movimientos no provienen de ventas y solo pueden reducir el stock libre, es decir, el stock físico no reservado. |
| **Precondiciones** | 1. El almacenero debe haber iniciado sesión en el sistema.<br>2. El almacenero debe tener asignado el rol de "Almacenero". |
| **Flujo principal** | 1. El almacenero ingresa al módulo de "Inventario" y selecciona "Registrar Movimiento Manual".<br>2. El sistema despliega un formulario solicitando el tipo de movimiento.<br>3. El almacenero selecciona entre: "Entrada por ajuste", "Salida por merma/ajuste" o "Traslado interno".<br>4. Si el almacenero selecciona Entrada o Salida por ajuste/merma, el sistema ejecuta el Flujo Alternativo 1.<br>5. Si el almacenero selecciona Traslado interno, el sistema ejecuta el Flujo Alternativo 2. |
| **Postcondiciones** | El stock físico en el Kardex se actualiza. Cada movimiento registra la fecha, el usuario responsable, el tipo de movimiento y el motivo ingresado. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Ajuste o Merma**<br>1. El almacenero selecciona el producto, el almacén específico y digita la cantidad a mover.<br>2. El almacenero ingresa obligatoriamente un texto justificando el motivo, como puede ser producto dañado, sobrante detectado.<br>3. Si es una salida, el sistema verifica que la cantidad requerida no sea superior al stock libre del almacén, sin afectar las reservas de ventas.<br>4. El almacenero confirma y el sistema registra la entrada o salida en el Kardex.<br>**Flujo Alternativo 1.1 - Stock Insuficiente**<br>1. Si en el paso 3 el sistema detecta que la salida es mayor al stock libre, bloquea la operación y notifica al usuario.<br>**Flujo Alternativo 2 - Traslado Interno**<br>1. El almacenero selecciona el producto, el almacén de origen, el almacén de destino y la cantidad a trasladar.<br>2. El almacenero ingresa obligatoriamente un motivo justificatorio.<br>3. El sistema valida que el almacén de origen tenga stock libre suficiente.<br>4. El almacenero confirma la operación.<br>5. El sistema registra el traslado mediante partida doble: genera un movimiento de salida en el almacén de origen y un movimiento de entrada en el almacén de destino, ambos vinculados mediante el mismo traslado y motivo.<br>**Flujo Alternativo 2.1 - Stock Insuficiente**<br>Si en el paso 3 el almacén de origen no tiene stock libre suficiente, el sistema impide el traslado. |

<a id="cu-06"></a>

## CU-06: Generar orden de despacho

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-06: Generar orden de despacho |
| **Actor principal** | Almacenero |
| **Propósito** | Generar e imprimir el documento interno de Orden de Despacho, garantizando su correspondencia con una venta con todas sus cantidades reservadas, sin duplicar las salidas de Kardex. |
| **Precondiciones** | 1. El almacenero debe haber iniciado sesión con el rol de "Almacenero".<br>2. Debe existir en el sistema una Venta con todas sus cantidades reservadas, en estado "pendiente de despacho" o "lista para despacho". |
| **Flujo principal** | 1. El almacenero ingresa al módulo de Despachos.<br>2. El sistema lista las ventas con todas sus cantidades reservadas que aún no han sido despachadas. Las ventas con cantidades pendientes de abastecimiento no pueden seleccionarse.<br>3. El almacenero selecciona la venta a despachar.<br>4. El sistema extrae automáticamente los productos, cantidades y la dirección de entrega de la venta vinculada.<br>5. El almacenero confirma la generación de la Orden de Despacho.<br>6. El sistema guarda la orden vinculándola con el ID de la Venta original y actualiza su estado a "Emitida".<br>7. El almacenero procede a imprimir el documento físico para entregarlo al chofer.<br>8. Al confirmar la salida física de la mercadería, el almacenero registra la confirmación en el sistema.<br>9. El sistema genera los movimientos de salida en el Kardex por las cantidades del despacho, consume las reservas de la venta y actualiza su estado a "despachada". |
| **Postcondiciones** | El estado de la orden de despacho queda registrada en el sistema. La emisión de este documento no modifica las existencias. La salida de Kardex se registra únicamente al confirmar la salida física del despacho único y completo. Además, el sistema no gestionará procesos logísticos externos, como GPS, rutas o vehículos. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Venta ya Despachada**<br>1. Si el almacenero intenta generar una orden para una venta que ya tiene una orden de despacho vinculada previamente, el sistema advierte de la situación.<br>2. El sistema requiere una validación adicional para reimprimir, a fin de evitar despachos físicos duplicados.<br>**Flujo Alternativo 2 - Venta con cantidades pendientes**<br>1. Si la venta tiene cantidades pendientes de abastecimiento, el sistema no permite generar la Orden de Despacho.<br>2. El sistema informa las cantidades pendientes de reserva. |

<a id="cu-07"></a>

## CU-07: Consultar Kardex

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-07: Consultar Kardex |
| **Actor principal** | Almacenero |
| **Propósito** | Consultar el historial detallado de movimientos transaccionales, que son las entradas, salidas, ajustes y traslados con filtros opcionales por producto (SKU), almacén, tipo de operación, origen y rango de fechas, para auditoría y trazabilidad. |
| **Precondiciones** | 1. El almacenero debe haber iniciado sesión en el sistema.<br>2. Debe tener acceso habilitado al módulo de inventarios/Kardex. |
| **Flujo principal** | 1. El almacenero ingresa al módulo "Kardex".<br>2. El almacenero define los filtros opcionales, combinables entre sí: código de producto (SKU), almacén, tipo de operación, origen del movimiento y rango de fechas inclusivo. Sin filtros, se listan todos los movimientos.<br>3. El almacenero ejecuta la búsqueda.<br>4. El sistema consulta el historial en la base de datos.<br>5. El sistema presenta una tabla cronológica que detalla: fecha, usuario responsable, tipo de movimiento, cantidad afectada y la operación de origen que detonó el movimiento, como un despacho, una recepción, un ajuste o un traslado. Los movimientos se presentan del más reciente al más antiguo, en páginas. |
| **Postcondiciones** | Ninguna. Es una operación de solo lectura para el usuario; no modifica los datos ni el inventario físico del sistema. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Producto sin Movimientos**<br>1. Si la consulta a la base de datos no arroja transacciones para el SKU en ese almacén y rango de fechas, el sistema muestra un mensaje indicando que no se registraron movimientos en dicho periodo.<br>**Flujo Alternativo 2 - Filtros con valores no válidos**<br>1. Si un filtro tiene un valor que no cumple su formato, como una fecha no válida, el sistema resalta el campo y no ejecuta la consulta. |

<a id="cu-08"></a>

## CU-08: Procesar documento con OCR

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-08: Procesar documento con OCR |
| **Actor principal** | Vendedor o Almacenero |
| **Propósito** | Procesar documentos digitales mediante OCR y fuzzy matching para extraer productos y cantidades, normalizarlos contra el catálogo maestro y entregar la información estructurada al proceso operativo que corresponda, como cotización, venta o recepción. |
| **Precondiciones** | 1. El usuario debe haber iniciado sesión en el sistema.<br>2. El usuario debe tener un rol autorizado para cargar documentos.<br>3. Debe existir un documento digital en formato compatible, como imagen o PDF. |
| **Flujo principal** | 1. El usuario ingresa al módulo correspondiente y selecciona la opción "Procesar documento".<br>2. El sistema solicita la carga del documento digital.<br>3. El usuario selecciona y carga el archivo.<br>4. El sistema registra el documento y ejecuta el procesamiento OCR.<br>5. El sistema extrae el texto y las cantidades identificadas en el documento.<br>6. El sistema ejecuta el algoritmo de fuzzy matching para relacionar los productos identificados con el catálogo maestro.<br>7. El sistema muestra los productos encontrados, las cantidades detectadas y el nivel de coincidencia para su validación.<br>8. El usuario revisa y corrige los productos o cantidades que sean necesarios.<br>9. El sistema registra los detalles normalizados y los deja disponibles para el proceso operativo que originó el procesamiento. |
| **Postcondiciones** | El documento queda registrado con estado de procesamiento actualizado. Los productos y cantidades identificados quedan normalizados mediante el catálogo maestro y disponibles para continuar con el proceso de cotización, venta o recepción, sin modificar automáticamente el stock. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Producto no identificado**<br>1. Si el sistema no encuentra una coincidencia suficiente con el catálogo, marca el producto como "No identificado".<br>2. El usuario selecciona manualmente el producto correcto del catálogo o corrige la información extraída.<br>3. El sistema registra la asociación validada.<br>**Flujo Alternativo 2 - Documento ilegible**<br>1. Si el OCR no puede extraer información suficiente, el sistema informa que el documento no pudo ser procesado correctamente.<br>2. El usuario puede cargar nuevamente el documento o continuar mediante registro manual.<br>**Flujo Alternativo 3 - Coincidencia ambigua**<br>1. Si existen múltiples productos con un nivel de coincidencia similar, el sistema muestra las alternativas encontradas.<br>2. El usuario selecciona el producto correcto antes de continuar. |

<a id="cu-09"></a>

## CU-09: Gestionar cotización

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-09: Gestionar cotización |
| **Actor principal** | Vendedor |
| **Propósito** | Crear y administrar cotizaciones para clientes registrados, registrando los productos, cantidades, precios y condiciones comerciales, y controlar su ciclo de vida desde su emisión hasta su aceptación, su validación por MIGAL, su rechazo o su vencimiento. Toda cotización se asocia a un cliente registrado. |
| **Precondiciones** | 1. El vendedor debe haber iniciado sesión en el sistema.<br>2. El vendedor debe tener asignado el rol de "Vendedor".<br>3. Los productos incluidos en la cotización deben existir en el catálogo maestro.<br>4. El cliente destinatario debe estar registrado y activo.<br>5. La cotización no puede emitirse sin un cliente asociado. Las operaciones de mostrador se registran con un registro de cliente cuyo documento de identificación es opcional. |
| **Flujo principal** | 1. El vendedor ingresa al módulo de "Cotizaciones" y selecciona "Nueva Cotización".<br>2. El vendedor recibe automáticamente los datos desde el CU-08: Procesar documento con OCR o puede decidir ingresarlo manualmente.<br>3. El vendedor realiza las correcciones correspondientes si identifica algún error.<br>4. El vendedor selecciona o registra el cliente destinatario, y registra los descuentos y las condiciones comerciales.<br>5. El sistema calcula el total de la cotización y establece la fecha de vencimiento.<br>6. El vendedor revisa la información y confirma la emisión.<br>7. El sistema registra la cotización con estado "Emitida".<br>8. El vendedor puede entregar la cotización al cliente. |
| **Postcondiciones** | La cotización queda registrada con todos sus productos, cantidades, precios, descuentos, condiciones y fecha de vencimiento, y asociada a un cliente activo. Su estado queda establecido como "Emitida" y puede posteriormente pasar a "Aceptada", "Rechazada" o "Vencida". Una cotización aceptada pasa a "Validada" cuando MIGAL confirma su viabilidad comercial. La emisión de una cotización no modifica ni reserva el stock del inventario. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Cotización aceptada**<br>1. El vendedor registra la aceptación del cliente.<br>2. El sistema cambia el estado de la cotización a "Aceptada".<br>3. Se registra la validación de viabilidad comercial por parte de MIGAL.<br>4. El sistema cambia el estado de la cotización a "Validada".<br>5. El vendedor selecciona la opción "Generar venta".<br>6. El sistema recupera los productos, cantidades y precios de la cotización y ejecuta el registro de la venta descrito en el CU-10, que reserva el stock disponible y registra el resto como pendiente de abastecimiento.<br>**Flujo Alternativo 2 - Cotización rechazada**<br>1. El cliente rechaza la propuesta.<br>2. El vendedor registra el rechazo.<br>3. El sistema cambia el estado de la cotización a "Rechazada".<br>**Flujo Alternativo 3 - Cotización vencida**<br>1. El sistema verifica la fecha de vencimiento de las cotizaciones emitidas.<br>2. Cuando se supera la fecha de vencimiento sin aceptación, cambia automáticamente el estado a "Vencida".<br>**Flujo Alternativo 4 - Stock insuficiente al generar la venta**<br>1. Si el sistema detecta que uno o más productos no cuentan con stock libre suficiente, no bloquea la generación de la venta.<br>2. El sistema reserva las cantidades disponibles por ubicación y registra el resto como pendiente de abastecimiento.<br>3. El sistema informa los productos con cantidades pendientes. La venta no podrá despacharse hasta completar sus reservas.<br>**Flujo Alternativo 5 - Cliente no registrado**<br>1. El vendedor intenta emitir la cotización sin un cliente seleccionado.<br>2. El sistema impide la emisión y solicita seleccionar o registrar un cliente activo.<br>3. El vendedor selecciona o registra el cliente. Si la operación corresponde a una venta de mostrador, se registra un cliente de mostrador con documento opcional. |

<a id="cu-10"></a>

## CU-10: Registrar venta

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-10: Registrar venta |
| **Actor principal** | Vendedor |
| **Propósito** | Registrar una venta aprobada que se origina en una cotización aceptada y validada por MIGAL, reservando por ubicación las existencias disponibles y registrando las cantidades faltantes como pendientes de abastecimiento. La venta no reduce el stock físico ni genera movimientos de Kardex: la salida se registra al confirmar el despacho único y completo (CU-06). La transacción no genera comprobantes fiscales. |
| **Precondiciones** | 1. El vendedor debe tener sesión iniciada con el rol de Vendedor.<br>2. Los productos a vender deben existir en el catálogo maestro.<br>3. El cliente debe estar registrado y activo.<br>4. Si la venta proviene de una cotización, esta debe encontrarse en estado Aceptada y Validada, y no haber sido convertida previamente en una venta.<br>5. La insuficiencia de stock no impide registrar la venta; las cantidades sin stock libre quedan pendientes de abastecimiento.<br>6. La cantidad reservada no puede superar la cantidad solicitada ni el stock libre de la ubicación seleccionada. |
| **Flujo principal** | 1. El vendedor ingresa al módulo de Ventas y selecciona "Registrar Venta".<br>2. El vendedor selecciona un cliente registrado y activo.<br>3. El vendedor selecciona una cotización aceptada y validada (Flujo Alternativo 2), o agrega los productos indicando el SKU, las cantidades y los precios correspondientes, o los recibe desde el CU-08: Procesar documento con OCR. En el registro directo, el sistema registra la cotización, su aceptación y su validación en un solo paso transaccional.<br>4. El sistema calcula el subtotal, el descuento aplicado y el monto total de la operación.<br>5. El sistema consulta el stock libre de cada producto por ubicación y reserva las cantidades disponibles. Las cantidades sin stock libre suficiente se registran como pendientes de abastecimiento.<br>6. El vendedor revisa la información y confirma el registro de la venta.<br>7. El sistema guarda la venta, sus reservas y, cuando corresponda, la relación con la cotización de origen, en una sola operación transaccional. La venta queda en estado "pendiente de despacho" si todas las cantidades están reservadas, o en estado "pendiente de abastecimiento" en caso contrario.<br>8. El sistema no genera movimientos de salida en el Kardex ni reduce el stock físico. |
| **Postcondiciones** | La venta queda registrada con sus cantidades solicitadas, reservadas y pendientes de abastecimiento. Las reservas identifican las existencias asignadas por ubicación y no modifican el stock físico. No se generan movimientos de salida en el Kardex hasta la confirmación del despacho único y completo, momento en que las reservas se consumen. Las cantidades pendientes permanecen registradas para su reserva posterior y nunca generan stock negativo. Si la venta proviene de una cotización, queda registrada la relación uno a uno entre ambas, y la cotización no puede convertirse nuevamente en otra venta. Esta transacción es estrictamente operativa: no genera cuentas por cobrar, estados de cuenta, asientos contables ni comprobantes fiscales ante SUNAT. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Aplicar descuento**<br>1. El vendedor ingresa el valor del descuento deseado y confirma la acción.<br>2. El sistema valida que el descuento no sea negativo ni supere el subtotal de la venta.<br>3. El sistema calcula el nuevo monto total restando la rebaja aplicada.<br>4. El sistema actualiza los datos en pantalla y retorna al flujo principal.<br>**Flujo Alternativo 2 - Venta desde cotización aceptada y validada**<br>1. El vendedor selecciona una cotización en estado Aceptada y Validada.<br>2. El sistema carga los productos, cantidades, precios y descuentos registrados en la cotización.<br>3. El sistema valida y reserva el stock libre disponible por ubicación, sin bloquear la operación por insuficiencia de stock.<br>4. El sistema continúa con el flujo principal y registra la conversión de la cotización en venta.<br>**Flujo Alternativo 3 - Reserva parcial**<br>1. El sistema detecta que el stock libre es insuficiente para una o más líneas de la venta.<br>2. El sistema reserva las cantidades disponibles por ubicación.<br>3. El sistema registra la diferencia como cantidad pendiente de abastecimiento.<br>4. La venta queda en estado "pendiente de abastecimiento" y no genera movimientos de salida en el Kardex.<br>5. El despacho no puede autorizarse hasta que todas las cantidades estén reservadas. No existe despacho parcial.<br>**Flujo Alternativo 4 - Entrega anticipada de una parte del pedido**<br>1. Si el cliente requiere recibir antes una parte del pedido, el vendedor divide la cotización aceptada y validada en dos cotizaciones.<br>2. Cada cotización resultante genera una venta independiente, con sus propias reservas y su propio despacho único y completo.<br>3. Se conserva la relación uno a uno entre cotización y venta.<br>**Flujo Alternativo 5 - Completar reservas pendientes**<br>1. El usuario consulta las ventas con cantidades pendientes de abastecimiento y selecciona una de ellas.<br>2. El sistema vuelve a consultar el stock libre disponible por ubicación.<br>3. El sistema reserva las cantidades pendientes que puedan atenderse, sin generar movimientos de salida en el Kardex.<br>4. Si todas las cantidades quedan reservadas, la venta cambia a estado "pendiente de despacho". Si aún existen cantidades pendientes, conserva el estado "pendiente de abastecimiento".<br>**Flujo Alternativo 6 - Stock insuficiente para completar**<br>1. El sistema detecta que el stock libre continúa siendo insuficiente.<br>2. El sistema informa la cantidad máxima que puede reservarse.<br>3. El sistema no permite reservar cantidades superiores al stock libre y mantiene la venta en estado "pendiente de abastecimiento".<br>**Flujo Alternativo 7 - Cancelar venta antes del despacho**<br>1. El usuario selecciona la venta y elige la opción de cancelar.<br>2. El sistema solicita un motivo obligatorio y el usuario lo ingresa.<br>3. El usuario confirma la cancelación.<br>4. El sistema libera las reservas activas de la venta, registra el motivo y cambia el estado a "cancelada".<br>5. El sistema no genera movimientos de Kardex. Las ventas despachadas no pueden cancelarse mediante este flujo. |

<a id="cu-11"></a>

## CU-11: Registrar proveedor

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-11: Registrar proveedor |
| **Actor principal** | Reponedor |
| **Propósito** | Mantener actualizado un directorio básico de proveedores y asociarlos formalmente con los productos que suministran, facilitando el posterior registro de órdenes de compra. |
| **Precondiciones** | 1. El reponedor debe haber iniciado sesión con el rol de "Reponedor".<br>2. Debe tener acceso al módulo de gestión de proveedores.<br>3. El RUC del proveedor no debe encontrarse registrado previamente. |
| **Flujo principal** | 1. El reponedor ingresa al módulo de Proveedores dentro del sistema.<br>2. Selecciona la opción "Registrar Nuevo Proveedor".<br>3. Ingresa los datos básicos obligatorios: razón social, número de RUC, datos de contacto y condiciones comerciales básicas.<br>4. Asocia al proveedor con los SKU de los productos específicos que suministra al negocio.<br>5. El reponedor confirma y guarda el registro en la base de datos relacional. |
| **Postcondiciones** | El directorio de proveedores y sus relaciones con los productos suministrados quedan almacenados de forma normalizada en la base de datos PostgreSQL, quedando listos para ser seleccionados al emitir Órdenes de Compra. |
| **Flujos alternativos** | **Flujo Alternativo 1 - RUC Duplicado o ya Existente**<br>1. Si el sistema detecta que el número de RUC ingresado ya se encuentra registrado, bloquea la creación del duplicado.<br>2. El sistema emite una alerta y sugiere al reponedor modificar el registro existente en lugar de crear uno nuevo.<br>**Flujo Alternativo 2 - Producto no encontrado para Asociación**<br>1. Si al intentar vincular un producto, el SKU no aparece en el catálogo maestro, el sistema permite omitir la asociación temporalmente o redirige al módulo del catálogo para darlo de alta. <br>**Flujo Alternativo 3 - Proveedor inactivo**<br>1. El usuario consulta un proveedor inactivo.<br>2. El sistema permite visualizar su información histórica.<br>3. El sistema impide utilizarlo en nuevas Órdenes de Compra.<br>4. El sistema informa que debe activarse antes de utilizarlo nuevamente.|

<a id="cu-12"></a>

## CU-12: Registrar orden de compra

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-12: Registrar orden de compra |
| **Actor principal** | Reponedor |
| **Propósito** | Registrar formalmente en el sistema una Orden de Compra previamente acordada con un proveedor externo, detallando los productos y precios pactados sin modificar directamente el inventario físico. |
| **Precondiciones** | 1. El reponedor debe haber iniciado sesión con el rol de "Reponedor"<br>2. El proveedor seleccionado debe estar previamente registrado y activo en el directorio del sistema.<br>3. Los productos incluidos deben existir en el catálogo maestro. |
| **Flujo principal** | 1. El reponedor ingresa al módulo de Abastecimiento y selecciona "Nueva Orden de Compra".<br>2. El reponedor selecciona al proveedor del directorio básico.<br>3. El sistema registra los datos mínimos obligatorios: proveedor, fecha, productos (SKU), cantidades requeridas y precios de compra acordados.<br>4. El reponedor confirma y guarda el registro de la Orden de Compra en el sistema. |
| **Postcondiciones** | La Orden de Compra queda registrada en el sistema bajo un estado inicial "Pendiente". Esta operación representa lo solicitado al proveedor y no modifica ni incrementa directamente las existencias del inventario. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Proveedor Inactivo o No Encontrado**<br>1. Si el reponedor intenta seleccionar un proveedor dado de baja o que no existe, el sistema bloquea la selección y solicita registrarlo previamente, iniciando el flujo de CU-11. |

<a id="cu-13"></a>

## CU-13: Registrar orden de recepción

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-13: Registrar orden de recepción |
| **Actor principal** | Almacenero |
| **Propósito** | Registrar formalmente la recepción física de mercadería, asociada a una Orden de Compra, permitiendo opcionalmente precargar productos y cantidades desde un documento validado tras su procesamiento mediante OCR y fuzzy matching. La recepción se registra en estado pendiente y genera los movimientos de entrada en el Kardex al validarse. |
| **Precondiciones** | 1. El almacenero debe haber iniciado sesión en el sistema.<br>2. Debe existir una Orden de Compra pendiente o parcial.<br>3. Si se utiliza un documento, este debe encontrarse en estado validado y no asociado a otra operación. |
| **Flujo principal** | 1. El almacenero ingresa al módulo de Abastecimiento y selecciona “Nueva Orden de Recepción”.<br>2. El almacenero selecciona la Orden de Compra correspondiente y el sistema muestra los productos esperados.<br>3. De manera opcional, el almacenero selecciona un documento de recepción en estado validado. El sistema precarga los productos y cantidades en la ubicación indicada; las líneas que el almacenero ingrese prevalecen sobre la precarga.<br>4. El almacenero revisa y corrige la información precargada cuando sea necesario.<br>5. El almacenero realiza el conteo físico y registra las cantidades efectivamente recibidas por producto y ubicación.<br>6. El sistema registra la recepción en estado pendiente. Este registro no modifica el inventario ni genera movimientos de Kardex, y la recepción puede modificarse mientras permanezca pendiente.<br>7. El almacenero valida la recepción.<br>8. El sistema compara las cantidades recibidas con la cantidad pendiente de la Orden de Compra y, en una sola transacción, genera un movimiento de entrada en el Kardex por cada línea con cantidad mayor a cero.<br>9. El sistema pasa la recepción a estado validada, registra la fecha de validación y actualiza el estado de la Orden de Compra a parcial o completada según su cantidad pendiente.<br>10. El sistema presenta las existencias antes y después de la operación y la Orden de Compra actualizada. |
| **Postcondiciones** | La Orden de Recepción queda registrada en estado pendiente y asociada a su Orden de Compra. Al validarse, pasa a estado validada, y los productos efectivamente recibidos generan movimientos de entrada en el Kardex que actualizan el stock del almacén correspondiente. Si se utilizó un documento, queda asociado como respaldo de la recepción. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Documento no validado o producto no identificado**<br>1. Si el documento no está en estado validado o ya apoya otra operación, el sistema rechaza la precarga y el almacenero registra la recepción manualmente.<br>2. Si un producto del documento no tiene una coincidencia confiable, el almacenero lo asocia manualmente o lo corrige antes de registrar la recepción.<br>**Flujo Alternativo 2 - Recepción parcial**<br>1. Si la cantidad recibida es menor a la cantidad pendiente de la Orden de Compra, el sistema registra únicamente la cantidad efectivamente recibida.<br>2. La Orden de Compra permanece pendiente o pasa a estado “Parcial”, según corresponda.<br>3. El sistema permite registrar futuras recepciones para completar las cantidades pendientes.<br>**Flujo Alternativo 3 - Sobrante sin nota**<br>1. Si la cantidad recibida de un producto supera su cantidad pendiente y la recepción no tiene observaciones, el sistema rechaza la validación y no genera movimientos.<br>2. El almacenero registra una observación que justifique el sobrante y valida nuevamente la recepción. |

<a id="cu-14"></a>

## CU-14: Consultar agente de IA

| Campo | Descripción |
| --- | --- |
| **Caso de uso** | CU-14: Consultar agente de IA |
| **Actor principal** | Usuario de cualquier rol |
| **Propósito** | Permitir al usuario realizar consultas en lenguaje natural sobre inventario, productos y ubicaciones mediante una interfaz conversacional, sin alterar los datos transaccionales. |
| **Precondiciones** | 1. El usuario debe haber iniciado sesión en el sistema.<br>2. El usuario debe contar con los permisos de su rol. Cada herramienta del agente se ejecuta con esos mismos permisos (RF-IA-09). |
| **Flujo principal** | 1. El actor accede a la interfaz conversacional del Agente IA Consultivo integrada en MIGAL SmartOps.<br>2. El actor ingresa un mensaje en lenguaje natural (por ejemplo, disponibilidad de stock, productos en nivel crítico o ubicación de mercadería) en una conversación nueva o existente.<br>3. La interfaz envía el mensaje mediante POST /agente/mensajes, con el campo `mensaje` y, si corresponde, `conversacionId`. El agente convierte la intención en llamadas a herramientas de solo lectura, listadas para el rol del usuario en GET /agente/herramientas, mediante mecanismos de tool calling.<br>4. El backend ejecuta cada herramienta con los permisos del usuario autenticado y registra las herramientas invocadas en la respuesta.<br>5. El agente consulta la información registrada en el sistema (stock, ubicaciones, niveles críticos) sin modificar directamente los datos transaccionales.<br>6. El Agente IA devuelve una respuesta en lenguaje natural con los campos `texto`, `incierta`, `herramientas` y `referencias`, y la conversación queda disponible para el usuario. |
| **Postcondiciones** | El usuario obtiene la información solicitada de manera clara. Los datos transaccionales del sistema (Kardex, ventas, compras) se mantienen inalterados, asegurando que la operación sea de solo lectura. Las conversaciones solo son accesibles para el usuario que las creó. |
| **Flujos alternativos** | **Flujo Alternativo 1 - Información Insuficiente o Desconocida**<br>1. Si el agente no dispone de información suficiente para responder a la consulta del usuario.<br>2. El Agente IA indica explícitamente que no cuenta con la información, sin inventar cantidades o ubicaciones.<br>**Flujo Alternativo 2 - Restricción de Permisos**<br>1. Si una herramienta no está permitida al rol del usuario, la solicitud no se interrumpe: la herramienta se informa en `herramientas` con el código `PERMISO_INSUFICIENTE`.<br>2. El texto de la respuesta declara que el agente no tiene autorización para acceder a esos datos. Si el rol no puede utilizar el agente, la operación responde `PERMISO_INSUFICIENTE` con estado 403.<br>**Flujo Alternativo 3 - Servicio no disponible**<br>1. Si el proveedor del modelo de lenguaje no responde, la operación responde `SERVICIO_NO_DISPONIBLE` con estado 503.<br>**Flujo Alternativo 4 - Conversación inexistente o ajena**<br>1. Si la conversación no existe o pertenece a otro usuario, la operación responde `RECURSO_NO_ENCONTRADO` sin distinguir ambos casos. |
