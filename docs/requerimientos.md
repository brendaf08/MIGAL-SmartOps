# Requerimientos

> Estado: base documental migrada, pendiente de revisión del equipo.
> Fuente: [Software - entregables](../entregas/pc1/antecedentes/MIGAL-PC1-entregables-recibidos.pdf), páginas 13–20.
> Se conserva el contenido recibido; las observaciones de auditoría no se consideran corregidas ni aprobadas por esta migración.

**Pendientes conocidos:** RF-KAR-03 necesita distinguir entradas de abastecimiento de ajustes y traslados; RF-KAR-10 requiere una regla de reorden verificable. Consulte [trazabilidad](matriz-trazabilidad.md).

## Inventario y Kardex

| Código | Requerimiento |
| --- | --- |
| RF-KAR-01 | El sistema deberá administrar de manera independiente las existencias correspondientes a la tienda principal y a los dos almacenes adicionales. |
| RF-KAR-02 | El sistema deberá permitir consultar las existencias de cada producto por ubicación. |
| RF-KAR-03 | El sistema deberá registrar movimientos de entrada de inventario únicamente cuando una recepción de mercadería haya sido confirmada por el personal de almacén. |
| RF-KAR-04 | El sistema deberá registrar los movimientos de salida correspondientes cuando una venta sea confirmada. |
| RF-KAR-05 | Cada movimiento de Kardex deberá registrar como mínimo producto, ubicación, tipo de movimiento, cantidad, fecha y usuario responsable. |
| RF-KAR-06 | El sistema deberá impedir una salida o traslado cuando la cantidad requerida sea superior al stock disponible. |
| RF-KAR-07 | El sistema deberá registrar los traslados internos mediante partida doble, generando un movimiento de salida en el almacén de origen y un movimiento de entrada en el almacén de destino, ambos vinculados mediante un mismo traslado y motivo. |
| RF-KAR-08 | El sistema deberá permitir definir niveles mínimos de stock para los productos. |
| RF-KAR-09 | El sistema deberá identificar aquellos productos cuya existencia sea igual o inferior al nivel mínimo configurado. |
| RF-KAR-10 | El sistema deberá calcular el punto de reorden utilizando reglas deterministas del negocio, sin emplear modelos predictivos complejos de Machine Learning. |
| RF-KAR-11 | El sistema deberá mantener trazabilidad entre un movimiento de Kardex y la operación que lo originó, cuando corresponda. |
| RF-KAR-12 | El sistema deberá permitir consultar el historial de movimientos de un producto por almacén y rango de fechas. |
| RF-KAR-13 | El sistema deberá permitir generar e imprimir una Orden de Despacho asociada a una venta registrada, incluyendo como mínimo la dirección de entrega y su estado. |
| RF-KAR-14 | El sistema deberá permitir registrar movimientos manuales de inventario, como mermas y ajustes, exigiendo el ingreso de un motivo justificatorio. |

## OCR

| Código | Requerimiento |
| --- | --- |
| RF-OCR-01 | El sistema deberá permitir cargar manualmente fotografías o archivos de documentos operativos (notas manuscritas, capturas de solicitudes o pedidos de clientes y órdenes de recepción), desde la aplicación web. |
| RF-OCR-02 | El sistema deberá aceptar archivos en formatos PNG, JPG y PDF. |
| RF-OCR-03 | El archivo cargado deberá ser enviado al microservicio Python encargado del procesamiento OCR. |
| RF-OCR-04 | El microservicio deberá extraer el contenido textual identificable del archivo recibido. |
| RF-OCR-05 | El sistema deberá presentar el contenido obtenido al usuario antes de utilizarlo en una operación definitiva. |
| RF-OCR-06 | El usuario deberá poder corregir manualmente los datos extraídos cuando sean incorrectos. |
| RF-OCR-07 | El sistema deberá conservar la relación entre el archivo procesado y la información estructurada obtenida. |
| RF-OCR-08 | Una vez que la información extraída haya sido revisada y validada por el usuario, el sistema deberá permitir transferir los datos estructurados (cotizaciones, ventas o recepciones de mercadería) |

## Normalización

| Código | Requerimiento |
| --- | --- |
| RF-FUZ-01 | El sistema deberá recibir términos de productos obtenidos mediante OCR o ingresados manualmente. |
| RF-FUZ-02 | El sistema deberá comparar los términos recibidos con el catálogo maestro de productos. |
| RF-FUZ-03 | El sistema deberá calcular un nivel de similitud entre el término recibido y las denominaciones registradas. |
| RF-FUZ-04 | El sistema deberá sugerir el SKU correspondiente al producto con mayor similitud cuando se alcance el umbral definido. |
| RF-FUZ-05 | Cuando la coincidencia no alcance el nivel mínimo definido, el sistema deberá solicitar validación manual del usuario. |
| RF-FUZ-06 | El usuario deberá poder confirmar o corregir la asociación propuesta. |
| RF-FUZ-07 | Una vez validada la asociación, cualquier operación posterior que utilice el producto deberá emplear el SKU oficial del catálogo maestro. |

## IA consultiva

| Código | Requerimiento |
| --- | --- |
| RF-IA-01 | El sistema deberá proporcionar a los usuarios internos una interfaz conversacional integrada en MIGAL SmartOps. |
| RF-IA-02 | El usuario deberá poder realizar consultas en lenguaje natural relacionadas con inventario, productos y ubicación de mercadería disponible. |
| RF-IA-03 | El agente deberá convertir la intención de la consulta en llamadas a funciones o endpoints autorizados del backend mediante mecanismos de tool calling. |
| RF-IA-04 | El agente deberá consultar información registrada en el sistema y devolver una respuesta comprensible sin modificar directamente los datos transaccionales. |
| RF-IA-05 | El agente deberá permitir consultar la disponibilidad de stock de un producto. |
| RF-IA-06 | El agente deberá permitir consultar productos que se encuentren en nivel crítico de stock. |
| RF-IA-07 | El agente deberá permitir consultar la ubicación o almacén en el que se encuentra disponible un producto. |
| RF-IA-08 | Cuando el agente no disponga de información suficiente, deberá indicarlo sin inventar cantidades o ubicaciones. |
| RF-IA-09 | Las consultas efectuadas por el agente deberán respetar los permisos correspondientes al usuario autenticado. |

## Usuarios y roles

| Código | Requerimiento |
| --- | --- |
| RF-USR-01 | El sistema deberá permitir que los usuarios internos inicien sesión mediante credenciales personales. |
| RF-USR-02 | El sistema deberá gestionar permisos de acceso de acuerdo con los roles de Administrador, Vendedor, Almacenero y Reponedor. |
| RF-USR-03 | El sistema deberá restringir las funcionalidades disponibles según los permisos del usuario autenticado. |
| RF-USR-04 | El sistema deberá registrar al usuario responsable de las operaciones que produzcan modificaciones en el inventario. |

## Catálogo

| Código | Requerimiento |
| --- | --- |
| RF-PRO-01 | El sistema deberá permitir registrar productos en un catálogo maestro. |
| RF-PRO-02 | Cada producto deberá poseer como mínimo un SKU único, nombre oficial, descripción, unidad de medida y estado. |
| RF-PRO-03 | El sistema deberá permitir consultar y actualizar la información de los productos registrados. |
| RF-PRO-04 | El sistema deberá impedir el registro de productos con un SKU duplicado. |
| RF-PRO-05 | El sistema deberá permitir consultar la disponibilidad de un producto en la tienda principal y en cada uno de los dos almacenes. |
| RF-PRO-06 | El sistema deberá permitir establecer un nivel mínimo de stock para cada producto. |

## Ventas

| Código | Requerimiento |
| --- | --- |
| RF-VEN-01 | El sistema deberá permitir registrar una venta indicando como mínimo productos, cantidades, precios y monto total, pudiendo registrar los datos referenciales del cliente cuando correspondan. |
| RF-VEN-02 | El sistema deberá permitir registrar los datos básicos del cliente asociados a la venta cuando estos se encuentren disponibles. |
| RF-VEN-03 | El sistema deberá consultar y validar el stock disponible antes de confirmar la venta. |
| RF-VEN-04 | Al confirmarse la venta, el sistema deberá generar los movimientos correspondientes de salida en el Kardex. |
| RF-VEN-05 | El sistema deberá mantener la relación entre la venta y los productos incluidos en ella. |
| RF-VEN-06 | El sistema deberá permitir consultar las ventas registradas con fines operativos y de trazabilidad. |
| RF-VEN-07 | El sistema deberá permitir registrar un descuento aplicado a la venta cuando corresponda, bajo criterio del usuario autorizado. |
| RF-VEN-08 | El registro de una venta no deberá generar cuentas por cobrar, estados de cuenta, operaciones contables ni comprobantes fiscales ante SUNAT. |
| RF-VEN-09 | El sistema deberá permitir registrar una venta directamente o generarla a partir de una cotización previamente aceptada. |
| RF-VEN-10 | Cuando una venta se origine a partir de una cotización aceptada, el sistema deberá recuperar los productos, cantidades, precios, descuentos y datos referenciales previamente registrados, evitando su digitación nuevamente. |

## Abastecimiento

| Código | Requerimiento |
| --- | --- |
| RF-COM-01 | El sistema deberá permitir registrar una Orden de Compra previamente acordada con un proveedor. |
| RF-COM-02 | La Orden de Compra deberá registrar como mínimo proveedor, fecha, productos, cantidades, precios de compra y estado. |
| RF-COM-03 | El sistema deberá mantener un directorio básico de proveedores. |
| RF-COM-04 | El sistema deberá permitir asociar proveedores con los productos que suministran. |
| RF-COM-05 | El registro o confirmación de una Orden de Compra no deberá modificar directamente las existencias del inventario. |
| RF-COM-06 | El sistema deberá permitir registrar una Orden de Recepción asociada a una Orden de Compra. |
| RF-COM-07 | La Orden de Recepción deberá permitir registrar la cantidad efectivamente recibida de cada producto. |
| RF-COM-08 | El personal de almacén deberá poder verificar y corregir las cantidades recibidas antes de confirmar la recepción. |
| RF-COM-09 | Al confirmar una Orden de Recepción, el sistema deberá generar movimientos de entrada al Kardex utilizando únicamente las cantidades efectivamente recibidas. |
| RF-COM-10 | El sistema deberá conservar la trazabilidad entre la Orden de Compra, la recepción física y los movimientos de entrada generados en el Kardex. |
| RF-COM-11 | El sistema deberá permitir registrar recepciones parciales cuando la cantidad efectivamente recibida sea menor a la cantidad solicitada. |
| RF-COM-12 | El sistema no deberá gestionar solicitudes de cotización a proveedores, comparación de ofertas ni evaluación o selección automática de proveedores. |
| RF-COM-13 | El sistema deberá permitir cargar opcionalmente una fotografía o archivo del documento recibido junto con la mercadería para procesarlo mediante OCR y obtener información preliminar de los productos y cantidades entregadas. |
| RF-COM-14 | Los términos de productos extraídos desde documentos del proveedor podrán ser procesados mediante Fuzzy Matching para asociarlos con los SKU oficiales registrados en el catálogo maestro. |
| RF-COM-15 | La información obtenida mediante OCR y Fuzzy Matching deberá utilizarse únicamente como precarga de la recepción y deberá poder ser revisada y corregida por el personal responsable antes de su confirmación. |
| RF-COM-16 | El procesamiento automático de documentos de recepción no deberá generar movimientos de inventario por sí mismo; los movimientos de entrada al Kardex se producirán únicamente cuando la recepción sea confirmada. |

## Despacho

| Código | Requerimiento |
| --- | --- |
| RF-DES-01 | El sistema deberá permitir generar una Orden de Despacho asociada a una venta previamente registrada. |
| RF-DES-02 | La Orden de Despacho deberá contener como mínimo los productos, cantidades, dirección de entrega y estado. |
| RF-DES-03 | El sistema deberá permitir imprimir la Orden de Despacho para su utilización durante la entrega física de la mercadería. |
| RF-DES-04 | La Orden de Despacho deberá mantener referencia a la venta que la originó. |
| RF-DES-05 | La generación o impresión de una Orden de Despacho no deberá volver a reducir las existencias cuando la salida ya haya sido registrada al confirmarse la venta. |
| RF-DES-06 | El sistema no gestionará rutas, vehículos, choferes, optimización logística ni seguimiento GPS. |

## Cotizaciones

| Código | Requerimiento |
| --- | --- |
| RF-COT-01 | El sistema deberá permitir al Vendedor registrar una cotización dirigida a un cliente antes de concretar una venta. |
| RF-COT-02 | La cotización deberá permitir registrar, como mínimo, los productos, cantidades, precios unitarios, descuentos aplicables, condiciones comerciales, fecha de emisión y fecha de vencimiento. |
| RF-COT-03 | El sistema deberá calcular el monto total de la cotización a partir de los productos, cantidades, precios y descuentos registrados. |
| RF-COT-04 | El sistema deberá permitir que los productos y cantidades de una cotización puedan ser ingresados manualmente o precargados a partir de información previamente procesada mediante OCR y Fuzzy Matching. |
| RF-COT-05 | El sistema deberá gestionar el ciclo de vida de la cotización mediante los estados Emitida, Aceptada, Rechazada y Vencida. |
| RF-COT-06 | El sistema deberá identificar como vencida una cotización cuando se haya superado su fecha de vigencia sin haber sido aceptada. |
| RF-COT-07 | El sistema deberá permitir convertir una cotización aceptada en una venta, reutilizando los datos de productos, cantidades, precios y descuentos registrados. |
| RF-COT-08 | La creación, emisión, rechazo o vencimiento de una cotización no deberá modificar ni reservar existencias del inventario. |
| RF-COT-09 | Antes de confirmar una venta originada a partir de una cotización aceptada, el sistema deberá volver a consultar y validar el stock disponible. |

## Requerimientos no funcionales

| Código | Categoría | Requerimiento |
| --- | --- | --- |
| RNF-01 | Base de datos | El sistema deberá utilizar PostgreSQL como sistema gestor de base de datos relacional. |
| RNF-02 | Arquitectura | El backend principal deberá implementarse utilizando Java con Spring Boot y exponer los servicios necesarios mediante API REST. |
| RNF-03 | Arquitectura IA | El procesamiento OCR y los componentes especializados de IA deberán ejecutarse mediante un microservicio desarrollado en Python. |
| RNF-04 | Despliegue | Los principales componentes deberán poder desplegarse mediante contenedores Docker. |
| RNF-05 | Compatibilidad | La solución deberá implementarse como aplicación web responsive optimizada para computadoras de escritorio y tablets. |
| RNF-06 | Rendimiento | Las consultas habituales de productos, inventario, compras,cotizaciones y ventas deberán responder en un tiempo objetivo no mayor a 3 segundos bajo condiciones normales de operación. |
| RNF-07 | Rendimiento OCR | El procesamiento OCR deberá devolver un resultado en un tiempo objetivo no mayor a 10 segundos bajo condiciones normales. |
| RNF-08 | Integridad transaccional | Las operaciones que modifiquen inventario deberán ejecutarse de forma transaccional para evitar inconsistencias o actualizaciones parciales. |
| RNF-09 | Seguridad | Las funcionalidades internas deberán requerir autenticación y autorización basada en permisos o roles. |
| RNF-10 | Trazabilidad | Todo movimiento de Kardex deberá registrar fecha, usuario responsable, tipo de movimiento y operación de origen cuando corresponda. |
| RNF-11 | Disponibilidad | El sistema deberá encontrarse disponible durante el horario operativo de la ferretería, salvo mantenimiento programado o fallas de infraestructura externa. |
| RNF-12 | Recuperabilidad | La base de datos deberá contar con mecanismos periódicos de respaldo. |
| RNF-13 | Navegadores | La plataforma deberá ser compatible con navegadores web modernos utilizados en los equipos disponibles en la ferretería. |
| RNF-14 | Mantenibilidad | Los módulos principales deberán encontrarse desacoplados mediante interfaces claramente definidas. |

## Reglas de negocio pendientes de consolidación

Las reglas existentes se encuentran en los RF y CU. Antes de aprobar esta base, consolidar las decisiones sobre reorden, existencias críticas por ubicación, recepciones acumuladas, precisión monetaria, estados de despacho y exclusividad documental. No se asignan reglas nuevas como si hubieran sido aprobadas.
