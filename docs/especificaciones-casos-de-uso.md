# Especificaciones de casos de uso

> Fuente: [Casos de uso recibidos](../entregas/pc1/antecedentes/MIGAL-PC1-especificaciones-recibidas.pdf). Base migrada pendiente de revisión. Se mantienen las discrepancias recibidas para que el equipo las resuelva mediante cambios revisados.

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

Fuente: página 2.

### Actor principal

Usuario Interno

### Propósito

Autenticar el acceso seguro a MIGAL SmartOps mediante credenciales personales, garantizando que cada colaborador ingrese únicamente a los módulos autorizados según sus funciones.

### Precondiciones

1. El usuario debe estar previamente registrado en el sistema.

2. El usuario debe tener asignado un rol válido, sea de Administrador, Vendedor, Almacenero o Reponedor.

3. El sistema debe encontrarse disponible.

### Flujo principal

1. El usuario ingresa a la plataforma web a través del navegador.

2. El sistema presenta la interfaz de inicio de sesión solicitando nombre de usuario y contraseña.

3. El usuario ingresa sus credenciales personales y solicita el acceso.

4. El sistema consulta la base de datos PostgreSQL y valida que las credenciales coincidan con los registros.

5. El sistema identifica el rol del usuario autenticado.

6. El sistema redirige al usuario al panel principal y restringe las funcionalidades disponibles según sus permisos.

### Postcondiciones

La sesión del usuario queda activa. A partir de este momento, cualquier operación transaccional, como el registro de ventas, movimientos de Kardex, entre otros, registrará automáticamente a este usuario como responsable para fines de trazabilidad.

### Flujos alternativos

#### Flujo Alternativo 1 - Credenciales Inválidas

1. En el paso 4, el sistema detecta que el usuario no existe o la contraseña es incorrecta.

2. El sistema muestra un mensaje de error, por ejemplo: "Usuario o contraseña incorrectos".

3. El sistema limpia el campo de contraseña y retorna al paso 2 del flujo principal, permitiendo un nuevo intento.

#### Flujo Alternativo 2 - Usuario inactivo o sin permisos

1. En el paso 4, las credenciales son correctas, pero el usuario está desactivado o no tiene un rol asignado.

2. El sistema muestra un mensaje indicando que el acceso está denegado y debe contactar al administrador.

3. El sistema retorna al paso 2 del flujo principal.

<a id="cu-02"></a>

## CU-02: Gestionar usuarios y roles

Fuente: página 4.

### Actor principal

Administrador

### Propósito

Administrar los accesos, credenciales y roles del personal de la ferretería para garantizar la seguridad y correcta segregación de funciones.

### Precondiciones

1. El administrador debe haber iniciado sesión en el sistema mediante credenciales válidas.

2. El administrador debe poseer el rol de "Administrador".

### Flujo principal

1. El administrador ingresa al módulo de "Gestión de Usuarios".

2. El sistema despliega la lista del personal registrado y presenta las opciones: "Registrar Nuevo", "Modificar o Asignar Rol" y "Revocar Acceso".

3. El administrador selecciona la acción deseada.

4. Si selecciona "Registrar Nuevo", el sistema ejecuta el Flujo Alternativo 1.

5. Si selecciona "Modificar o Asignar Rol", el sistema ejecuta el Flujo Alternativo 2.

6. Si selecciona "Revocar Acceso", el sistema ejecuta el Flujo Alternativo 3.

### Postcondiciones

Los datos, credenciales y permisos del personal quedan actualizados en la base de datos centralizada de forma segura. Si se revoca un acceso, el sistema inactiva la cuenta, pero no elimina el historial del usuario para mantener la trazabilidad de los movimientos de Kardex realizados.

### Flujos alternativos

#### Flujo Alternativo 1 - Registrar nuevo usuario

1. El sistema muestra un formulario en blanco.

2. El administrador ingresa los datos personales, define un nombre de usuario, una contraseña temporal y selecciona el rol inicial.

3. El sistema verifica que el nombre de usuario no esté duplicado.

4. El sistema guarda el nuevo registro y retorna al paso 2 del flujo principal.

#### Flujo Alternativo 1.1 - Nombre de usuario ya existente

1. Si en el paso 3 del flujo alternativo 1 el sistema detecta que el nombre de usuario ya existe, muestra un mensaje de alerta y solicita ingresar uno distinto.

#### Flujo Alternativo 2 - Modificar o Asignar Rol

1. El administrador selecciona un usuario de la lista.

2. El sistema muestra los detalles actuales y el rol asignado.

3. El administrador modifica o asigna el rol.

4. El sistema actualiza la información y retorna al paso 2 del flujo principal.

#### Flujo Alternativo 3 - Revocar Accesos

1. El administrador selecciona un usuario y elige la opción de revocar acceso.

2. El sistema solicita confirmación.

3. El administrador confirma la acción.

4. El sistema inactiva las credenciales del usuario, impidiendo futuros inicios de sesión.

<a id="cu-03"></a>

## CU-03: Gestionar catálogo de productos

Fuente: página 6.

### Actor principal

Almacenero o Administrador

### Propósito

Administrar el catálogo maestro de la ferretería, permitiendo el registro, modificación y consulta de los productos, así como el establecimiento de umbrales para las alertas de stock mínimo.

### Precondiciones

1. El usuario debe haber iniciado sesión exitosamente.

2. El usuario debe poseer el rol de "Almacenero" o "Administrador".

### Flujo principal

1. El usuario ingresa al módulo "Productos".

2. El sistema muestra la lista general de productos y presenta las opciones: "Registrar Nuevo", "Modificar" y "Consultar Detalles".

3. El usuario selecciona la acción deseada.

4. Si selecciona "Registrar Nuevo", el sistema ejecuta el Flujo Alternativo 1.

5. Si selecciona "Modificar", el sistema ejecuta el Flujo Alternativo 2.

6. Si selecciona "Consultar Detalles", el sistema ejecuta el Flujo Alternativo 3.

### Postcondiciones

El catálogo maestro de productos queda actualizado de forma centralizada en la base de datos PostgreSQL. Estas operaciones son administrativas y no generan movimientos transaccionales en el Kardex.

### Flujos alternativos

#### Flujo Alternativo 1 - Registrar Nuevo Producto

1. El sistema muestra un formulario de registro.

2. El usuario ingresa el código único (SKU), nombre oficial, descripción, unidad de medida y estado (activo).

3. El usuario establece la cantidad numérica para el nivel mínimo de stock.

4. El sistema verifica que el SKU ingresado no exista previamente en la base de datos.

5. El sistema guarda el producto y retorna al paso 2 del flujo principal.

#### Flujo Alternativo 1.1 - Registrar Producto Existente

1. Si en el paso 4 del flujo alternativo 1 el sistema detecta que el código SKU ya existe, bloquea el registro, muestra un mensaje de error y solicita ingresar un código distinto.

#### Flujo Alternativo 2 - Modificar Producto

1. El usuario selecciona un producto de la lista.

2. El sistema recupera y muestra los datos vigentes.

3. El usuario actualiza los campos permitidos, por ejemplo, actualizar unidad de medida o modificar el stock mínimo.

4. El sistema valida los datos, guarda los cambios y retorna al flujo principal.

#### Flujo Alternativo 3 - Consultar Detalles

1. El usuario selecciona un producto o ingresa un criterio de búsqueda, como el SKU.

2. El sistema muestra la ficha técnica completa del producto sin opciones de edición.

<a id="cu-04"></a>

## CU-04: Consultar disponibilidad

Fuente: página 8.

### Actor principal

Almacenero o Vendedor

### Propósito

Consultar la disponibilidad de un producto específico en la tienda principal y en cada uno de los dos almacenes para informar sobre el stock real en tiempo real.

### Precondiciones

1. El usuario autorizado debe haber iniciado sesión exitosamente.

2. El usuario autorizado debe poseer el rol de "Almacenero" o "Vendedor"

### Flujo principal

1. El usuario autorizado ingresa al módulo de consultas de inventario.

2. El usuario autorizado ingresa el criterio de búsqueda, que puede ser SKU oficial, nombre o término comercial.

3. El sistema consulta las existencias en tiempo real dentro del Kardex.

4. El sistema presenta los resultados mostrando la disponibilidad del producto segmentada claramente por ubicación física, sea la tienda principal, el almacén 1 o el almacén 2.

### Postcondiciones

Ninguna. Se trata de una consulta de solo lectura que no modifica las existencias físicas ni genera registros en el Kardex.

### Flujos alternativos

#### Flujo Alternativo 1 - Producto No Encontrado

1. Si el criterio ingresado no coincide con ningún registro, el sistema muestra un mensaje indicando que no existen coincidencias.

#### Flujo Alternativo 2 - Término Ambiguo

1. Si el usuario autorizado ingresa un nombre informal que no coincide exactamente, el sistema invoca la búsqueda por nivel de similitud.

2. El sistema sugiere el/los productos con mayor coincidencia.

3. El usuario autorizado selecciona el producto correcto para visualizar su stock.

<a id="cu-05"></a>

## CU-05: Registrar movimiento

Fuente: página 10.

### Actor principal

Almacenero

### Propósito

Registrar movimientos manuales de inventario, tales como mermas o ajustes por descuadre, y traslados internos entre almacenes, manteniendo la precisión del stock físico y la trazabilidad.

### Precondiciones

1. El almacenero debe haber iniciado sesión en el sistema.

2. El almacenero debe tener asignado el rol de "Almacenero".

### Flujo principal

1. El almacenero ingresa al módulo de "Inventario" y selecciona "Registrar Movimiento Manual".

2. El sistema despliega un formulario solicitando el tipo de movimiento.

3. El almacenero selecciona entre: "Entrada por ajuste", "Salida por merma/ajuste" o "Traslado interno".

4. Si el almacenero selecciona Entrada o Salida por ajuste/merma, el sistema ejecuta el Flujo Alternativo 1.

5. Si el almacenero selecciona Traslado interno, el sistema ejecuta el Flujo Alternativo 2.

### Postcondiciones

El stock físico en el Kardex se actualiza. Cada movimiento registra la fecha, el usuario responsable, el tipo de movimiento y el motivo ingresado.

### Flujos alternativos

#### Flujo Alternativo 1 - Ajuste o Merma

1. El almacenero selecciona el producto, el almacén específico y digita la cantidad a mover.

2. El almacenero ingresa obligatoriamente un texto justificando el motivo, como puede ser producto dañado, sobrante detectado.

3. Si es una salida, el sistema verifica que la cantidad requerida no sea superior al stock disponible.

4. El almacenero confirma y el sistema registra la entrada o salida en el Kardex.

#### Flujo Alternativo 1.1 - Stock Insuficiente

1. Si en el paso 3 el sistema detecta que la salida es mayor al stock disponible, bloquea la operación y notifica al usuario.

#### Flujo Alternativo 2 - Traslado Interno

1. El almacenero selecciona el producto, el almacén de origen, el almacén de destino y la cantidad a trasladar.

2. El almacenero ingresa obligatoriamente un motivo justificatorio.

3. El sistema valida que el almacén de origen tenga stock suficiente.

4. El almacenero confirma la operación.

5. El sistema registra el traslado mediante partida doble: genera un movimiento de salida en el almacén de origen y un movimiento de entrada en el almacén de destino, ambos vinculados mediante el mismo traslado y motivo.

#### Flujo Alternativo 2.1 - Stock Insuficiente Si en el paso 3 el almacén de origen no tiene stock suficiente, el sistema impide el traslado.

<a id="cu-06"></a>

## CU-06: Generar orden de despacho

Fuente: página 12.

### Actor principal

Almacenero

### Propósito

Generar e imprimir el documento interno de Orden de Despacho, garantizando su correspondencia con una venta registrada sin duplicar los descuentos de stock.

### Precondiciones

1. El almacenero debe haber iniciado sesión con el rol de "Almacenero".

2. Debe existir en el sistema una Venta previamente confirmada pendiente de despacho.

### Flujo principal

1. El almacenero ingresa al módulo de Despachos.

2. El sistema lista las ventas recientes que aún no han sido entregadas.

3. El almacenero selecciona la venta a despachar.

4. El sistema extrae automáticamente los productos, cantidades y la dirección de entrega de la venta vinculada.

5. El almacenero confirma la generación de la Orden de Despacho.

6. El sistema guarda la orden vinculándola con el ID de la Venta original y actualiza su estado a "Emitida".

7. El almacenero procede a imprimir el documento físico para entregarlo al chofer.

### Postcondiciones

El estado de la orden de despacho queda registrada en el sistema. La emisión de este documento no altera ni reduce nuevamente las existencias en el Kardex, ya que dicha rebaja se aplicó durante el registro de la venta. Además, el sistema no gestionará procesos logísticos externos, como GPS, rutas o vehículos.

### Flujos alternativos

#### Flujo Alternativo 1 - Venta ya Despachada

1. Si el almacenero intenta generar una orden para una venta que ya tiene una orden de despacho vinculada previamente, el sistema advierte de la situación.

2. El sistema requiere una validación adicional para reimprimir, a fin de evitar despachos físicos duplicados.

<a id="cu-07"></a>

## CU-07: Consultar Kardex

Fuente: página 14.

### Actor principal

Almacenero

### Propósito

Consultar el historial detallado de movimientos transaccionales, que son las entradas, salidas, ajustes y traslados de un producto específico por almacén y rango de fechas para auditoría y trazabilidad.

### Precondiciones

1. El almacenero debe haber iniciado sesión en el sistema.

2. Debe tener acceso habilitado al módulo de inventarios/Kardex.

### Flujo principal

1. El almacenero ingresa al módulo "Kardex".

2. El almacenero define los parámetros de consulta obligatorios: código de producto (SKU), almacén específico y un rango temporal, es decir, fecha de inicio y fecha de fin.

3. El almacenero ejecuta la búsqueda.

4. El sistema consulta el historial en la base de datos.

5. El sistema presenta una tabla cronológica que detalla: fecha, usuario responsable, tipo de movimiento, cantidad afectada y la operación de origen que detonó el movimiento, como una venta, orden de recepción o ajuste manual.

### Postcondiciones

Ninguna. Es una operación de solo lectura para el usuario; no modifica los datos ni el inventario físico del sistema.

### Flujos alternativos

#### Flujo Alternativo 1 - Producto sin Movimientos

1. Si la consulta a la base de datos no arroja transacciones para el SKU en ese almacén y rango de fechas, el sistema muestra un mensaje indicando que no se registraron movimientos en dicho periodo.

#### Flujo Alternativo 2 - Parámetros Incompletos

1. Si el almacenero omite algún filtro obligatorio, como el rango de fechas, el sistema resalta el campo faltante y no ejecuta la consulta.

<a id="cu-08"></a>

## CU-08: Procesar documento con OCR

Fuente: página 16.

### Actor principal

Vendedor o Almacenero

### Propósito

Procesar documentos digitales mediante OCR y fuzzy matching para extraer productos y cantidades, normalizarlos contra el catálogo maestro y entregar la información estructurada al proceso operativo que corresponda, como cotización, venta o recepción.

### Precondiciones

1. El usuario debe haber iniciado sesión en el sistema.

2. El usuario debe tener un rol autorizado para cargar documentos.

3. Debe existir un documento digital en formato compatible, como imagen o PDF.

### Flujo principal

1. El usuario ingresa al módulo correspondiente y selecciona la opción "Procesar documento".

2. El sistema solicita la carga del documento digital.

3. El usuario selecciona y carga el archivo.

4. El sistema registra el documento y ejecuta el procesamiento OCR.

5. El sistema extrae el texto y las cantidades identificadas en el documento.

6. El sistema ejecuta el algoritmo de fuzzy matching para relacionar los productos identificados con el catálogo maestro.

7. El sistema muestra los productos encontrados, las cantidades detectadas y el nivel de coincidencia para su validación.

8. El usuario revisa y corrige los productos o cantidades que sean necesarios.

9. El sistema registra los detalles normalizados y los deja disponibles para el proceso operativo que originó el procesamiento.

### Postcondiciones

El documento queda registrado con estado de procesamiento actualizado. Los productos y cantidades identificados quedan normalizados mediante el catálogo maestro y disponibles para continuar con el proceso de cotización, venta o recepción, sin modificar automáticamente el stock.

### Flujos alternativos

#### Flujo Alternativo 1 - Producto no identificado

1. Si el sistema no encuentra una coincidencia suficiente con el catálogo, marca el producto como "No identificado".

2. El usuario selecciona manualmente el producto correcto del catálogo o corrige la información extraída.

3. El sistema registra la asociación validada.

#### Flujo Alternativo 2 - Documento ilegible

1. Si el OCR no puede extraer información suficiente, el sistema informa que el documento no pudo ser procesado correctamente.

2. El usuario puede cargar nuevamente el documento o continuar mediante registro manual.

#### Flujo Alternativo 3 - Coincidencia ambigua

1. Si existen múltiples productos con un nivel de coincidencia similar, el sistema muestra las alternativas encontradas.

2. El usuario selecciona el producto correcto antes de continuar.

<a id="cu-09"></a>

## CU-09: Gestionar cotización

Fuente: página 18. El original salta del paso 2 al 4 en el flujo principal; se conserva esa numeración en el archivo fuente.

### Actor principal

Vendedor

### Propósito

Crear y administrar cotizaciones para clientes, registrando los productos, cantidades, precios y condiciones comerciales, y controlar su ciclo de vida desde su emisión hasta su aceptación, rechazo o vencimiento.

### Precondiciones

1. El vendedor debe haber iniciado sesión en el sistema.

2. El vendedor debe tener asignado el rol de "Vendedor".

3. Los productos incluidos en la cotización deben existir en el catálogo maestro.

### Flujo principal

1. El vendedor ingresa al módulo de "Cotizaciones" y selecciona "Nueva Cotización".

2. El vendedor recibe automáticamente los datos desde el CU-08: Procesar documento con OCR o puede decidir ingresarlo manualmente

4. El vendedor realiza las correcciones correspondientes si identifica algún error.

5. El vendedor registra, cuando corresponda, descuentos y condiciones comerciales.

6. El sistema calcula el total de la cotización y establece la fecha de vencimiento.

7. El vendedor revisa la información y confirma la emisión.

8. El sistema registra la cotización con estado "Emitida".

9. El vendedor puede entregar la cotización al cliente.

### Postcondiciones

La cotización queda registrada con todos sus productos, cantidades, precios, descuentos, condiciones y fecha de vencimiento. Su estado queda establecido como "Emitida" y puede posteriormente pasar a "Aceptada", "Rechazada" o "Vencida". La emisión de una cotización no modifica el stock del inventario.

### Flujos alternativos

#### Flujo Alternativo 1 - Cotización aceptada

1. El vendedor registra la aceptación del cliente.

2. El sistema cambia el estado de la cotización a "Aceptada".

3. El vendedor selecciona la opción "Generar venta".

4. El sistema recupera los productos, cantidades y precios de la cotización.

5. El sistema verifica nuevamente la disponibilidad de stock.

6. Si existe stock suficiente, permite registrar la venta.

#### Flujo Alternativo 2 - Cotización rechazada

1. El cliente rechaza la propuesta.

2. El vendedor registra el rechazo.

3. El sistema cambia el estado de la cotización a "Rechazada".

#### Flujo Alternativo 3 - Cotización vencida

1. El sistema verifica la fecha de vencimiento de las cotizaciones emitidas.

2. Cuando se supera la fecha de vencimiento sin aceptación, cambia automáticamente el estado a "Vencida".

#### Flujo Alternativo 4 - Stock insuficiente al convertir en venta

1. Si el sistema detecta que uno o más productos no cuentan con stock suficiente, bloquea la conversión automática.

2. El sistema informa los productos que presentan insuficiencia de stock.

3. El vendedor puede modificar la operación o gestionar una nueva disponibilidad antes de registrar la venta.

<a id="cu-10"></a>

## CU-10: Registrar venta

Fuente: página 20.

### Actor principal

Vendedor

### Propósito

Registrar de manera estructurada una venta directa a un cliente, verificando la disponibilidad de inventario y ejecutando automáticamente la reducción del stock físico en tiempo real. La venta puede registrarse directamente o generarse a partir de una cotización previamente aceptada, sin generar comprobantes fiscales.

### Precondiciones

1. El vendedor debe tener sesión iniciada con el rol de Vendedor.

2. Los productos a vender deben existir en el catálogo maestro.

3. Para confirmar la venta, debe existir stock físico disponible en la ubicación seleccionada.

4. Si la venta proviene de una cotización, esta debe encontrarse en estado Aceptada y no haber sido convertida previamente en una venta.

### Flujo principal

1. El vendedor ingresa al módulo de Ventas y selecciona "Registrar Venta".

2. El vendedor registra los datos básicos referenciales del cliente, como nombre y/o documento.

3. El vendedor agrega los productos indicando el SKU, cantidades y precios correspondientes, o los recibe automáticamente desde el CU-08: Procesar documento con OCR.

4. De manera opcional, el vendedor puede seleccionar una cotización aceptada para cargar sus productos, cantidades y condiciones previamente registradas.

5. El sistema consulta y valida en tiempo real que exista stock disponible para las cantidades solicitadas en la ubicación seleccionada.

6. El sistema calcula y muestra el subtotal, descuento aplicado y monto total de la operación.

7. El vendedor revisa y confirma el registro de la venta.

8. El sistema guarda la transacción, manteniendo la relación con los productos y, cuando corresponda, con la cotización de origen.

9. El sistema genera automáticamente los movimientos correspondientes de salida en el Kardex y actualiza el stock físico.

10. Si la venta proviene de una cotización aceptada, el sistema registra la conversión de la cotización a venta.

### Postcondiciones

La venta queda registrada en el sistema. El inventario físico se reduce de manera automática y se genera el movimiento de salida correspondiente en el Kardex. Si la venta provino de una cotización, queda registrada la relación entre ambas y la cotización no puede convertirse nuevamente en otra venta. Esta transacción es estrictamente operativa: no genera cuentas por cobrar, estados de cuenta, asientos contables ni comprobantes fiscales ante SUNAT.

### Flujos alternativos

#### Flujo Alternativo 1 - Stock Insuficiente

1. Si la cantidad solicitada de un producto supera el stock físico disponible, el sistema alerta al vendedor del quiebre de inventario.

2. El sistema impide confirmar la venta hasta que se corrija la cantidad, se cambie la ubicación o se elimine el producto del registro.

#### Flujo Alternativo 2 - Aplicar Descuento

1. El vendedor ingresa el valor del descuento deseado y confirma la acción.

2. El sistema valida matemáticamente que el descuento sea válido, que no resulte en un valor negativo ni supere el subtotal de la venta.

3. El sistema calcula el nuevo monto total restando la rebaja aplicada.

4. El sistema actualiza los datos en pantalla y retorna al flujo principal.

#### Flujo Alternativo 3 - Venta desde cotización aceptada

1. El vendedor selecciona una cotización en estado Aceptada.

2. El sistema carga los productos, cantidades y condiciones registradas en la cotización.

3. El sistema vuelve a validar el stock disponible antes de confirmar la venta.

4. Si existe stock suficiente, continúa el flujo principal y registra la conversión de la cotización a venta.

5. Si no existe stock suficiente, el sistema impide la confirmación hasta que se regularicen las cantidades o disponibilidad.

<a id="cu-11"></a>

## CU-11: Registrar proveedor

Fuente: página 22.

### Actor principal

Reponedor

### Propósito

Mantener actualizado un directorio básico de proveedores y asociarlos formalmente con los productos que suministran, facilitando el posterior registro de órdenes de compra.

### Precondiciones

1. El reponedor debe haber iniciado sesión con el rol de "Reponedor".

2. Debe tener acceso al módulo de gestión de proveedores.

### Flujo principal

1. El reponedor ingresa al módulo de Proveedores dentro del sistema.

2. Selecciona la opción "Registrar Nuevo Proveedor".

3. Ingresa los datos básicos obligatorios: razón social, número de RUC, datos de contacto y condiciones comerciales básicas.

4. Asocia al proveedor con los SKU de los productos específicos que suministra al negocio.

5. El reponedor confirma y guarda el registro en la base de datos relacional.

### Postcondiciones

El directorio de proveedores y sus relaciones con los productos suministrados quedan almacenados de forma normalizada en la base de datos PostgreSQL, quedando listos para ser seleccionados al emitir Órdenes de Compra.

### Flujos alternativos

#### Flujo Alternativo 1 - RUC Duplicado o ya Existente

1. Si el sistema detecta que el número de RUC ingresado ya se encuentra registrado, bloquea la creación del duplicado.

2. El sistema emite una alerta y sugiere al reponedor modificar el registro existente en lugar de crear uno nuevo.

#### Flujo Alternativo 2 - Producto no encontrado para Asociación

1. Si al intentar vincular un producto, el SKU no aparece en el catálogo maestro, el sistema permite omitir la asociación temporalmente o redirige al módulo del catálogo para darlo de alta.

<a id="cu-12"></a>

## CU-12: Registrar orden de compra

Fuente: página 24.

### Actor principal

Reponedor

### Propósito

Registrar formalmente en el sistema una Orden de Compra previamente acordada con un proveedor externo, detallando los productos y precios pactados sin modificar directamente el inventario físico.

### Precondiciones

1. El reponedor debe haber iniciado sesión con el rol de "Reponedor"

2. El proveedor seleccionado debe estar previamente registrado en el directorio del sistema.

### Flujo principal

1. El reponedor ingresa al módulo de Abastecimiento y selecciona "Nueva Orden de Compra".

2. El reponedor selecciona al proveedor del directorio básico.

3. El sistema registra los datos mínimos obligatorios: proveedor, fecha, productos (SKU), cantidades requeridas y precios de compra acordados.

4. El reponedor confirma y guarda el registro de la Orden de Compra en el sistema.

### Postcondiciones

La Orden de Compra queda registrada en el sistema bajo un estado inicial "Pendiente". Esta operación representa lo solicitado al proveedor y no modifica ni incrementa directamente las existencias del inventario.

### Flujos alternativos

#### Flujo Alternativo 1 - Proveedor Inactivo o No Encontrado

1. Si el reponedor intenta seleccionar un proveedor dado de baja o que no existe, el sistema bloquea la selección y solicita registrarlo previamente, iniciando el flujo de CU-11.

<a id="cu-13"></a>

## CU-13: Registrar orden de recepción

Fuente: página 26.

### Actor principal

Almacenero

### Propósito

Registrar formalmente la recepción física de mercadería, asociada a una Orden de Compra , permitiendo opcionalmente utilizar un documento digital procesado mediante OCR y fuzzy matching para agilizar la carga de productos y cantidades, y generando los movimientos de entrada correspondientes en el Kardex.

### Precondiciones

1. El almacenero debe haber iniciado sesión en el sistema.

2. Debe existir una Orden de Compra pendiente.

3. Si se utiliza OCR, debe existir un documento digital disponible para su procesamiento.

### Flujo principal

1. El almacenero ingresa al módulo de Abastecimiento y selecciona “Nueva Orden de Recepción”.

2. El almacenero selecciona la Orden de Compra correspondiente y el sistema muestra los productos esperados.

3. De manera opcional, el almacenero puede cargar una fotografía o documento de la orden de recepción para procesarlo mediante OCR y fuzzy matching.

4. El sistema extrae y normaliza los productos y cantidades identificados en el documento.

5. El almacenero revisa y corrige la información precargada cuando sea necesario.

6. El almacenero realiza el conteo físico y registra las cantidades efectivamente recibidas.

7. El sistema valida la información registrada y calcula los movimientos de entrada correspondientes.

8. El almacenero confirma la recepción.

9. El sistema registra la Orden de Recepción y genera los movimientos de entrada en el Kardex utilizando únicamente las cantidades efectivamente recibidas.

10. El sistema actualiza su estado de acuerdo con las cantidades recibidas cuando corresponda.

### Postcondiciones

La Orden de Recepción queda registrada y asociada, cuando corresponda a su Orden de Compra. Los productos efectivamente recibidos generan movimientos de entrada en el Kardex y actualizan el stock del almacén correspondiente. Si se utilizó OCR, el documento procesado queda asociado como respaldo de la recepción.

### Flujos alternativos

#### Flujo Alternativo 1 - Documento ilegible o información no identificada

1. Si el OCR no puede identificar correctamente un producto o cantidad, el sistema informa la incidencia.

2. El almacenero corrige o registra manualmente la información antes de continuar.

#### Flujo Alternativo 2 - Recepción parcial

1. Si la cantidad recibida es menor a la cantidad pendiente de la Orden de Compra, el sistema registra únicamente la cantidad efectivamente recibida.

2. La Orden de Compra permanece pendiente o pasa a estado “Parcial”, según corresponda.

3. El sistema permite registrar futuras recepciones para completar las cantidades pendientes.

<a id="cu-14"></a>

## CU-14: Consultar agente de IA

Fuente: página 28.

### Actor principal

Usuario de cualquier rol

### Propósito

Permitir al usuario realizar consultas en lenguaje natural sobre inventario, productos y ubicaciones mediante una interfaz conversacional, sin alterar los datos transaccionales.

### Precondiciones

1. El usuario debe haber iniciado sesión en el sistema.

2. El usuario debe contar con los permisos correspondientes al módulo que intenta consultar.

### Flujo principal

1. El actor accede a la interfaz conversacional del Agente IA Consultivo integrada en MIGAL SmartOps.

2. El actor ingresa una consulta en lenguaje natural (ej. disponibilidad de stock, productos en nivel crítico, ubicación de mercadería).

3. El sistema procesa la consulta y el agente convierte la intención en llamadas a funciones o endpoints autorizados del backend mediante mecanismos de tool calling.

4. El backend verifica los permisos del usuario autenticado para la consulta efectuada.

5. El agente consulta la información registrada en el sistema (stock, ubicaciones, niveles críticos) sin modificar directamente los datos transaccionales.

6. El Agente IA devuelve una respuesta comprensible en lenguaje natural al actor.

### Postcondiciones

El usuario obtiene la información solicitada de manera clara. Los datos transaccionales del sistema (Kardex, ventas, compras) se mantienen inalterados, asegurando que la operación sea de solo lectura.

### Flujos alternativos

#### Flujo Alternativo 1 - Información Insuficiente o Desconocida

1. Si el agente no dispone de información suficiente para responder a la consulta del usuario.

2. El Agente IA indica explícitamente que no cuenta con la información, sin inventar cantidades o ubicaciones.

#### Flujo Alternativo 2 - Restricción de Permisos

1. Si el usuario realiza una consulta sobre un módulo para el cual no tiene permisos.

2. El sistema restringe la llamada al endpoint y el agente informa que no tiene autorización para acceder a esos datos.
