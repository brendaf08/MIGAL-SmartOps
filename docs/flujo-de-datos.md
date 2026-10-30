# Flujo de datos

Este documento presenta los Diagramas de Flujo de Datos (DFD) de MIGAL SmartOps, organizados por nivel, con el propósito de representar el intercambio de información entre los usuarios internos, los procesos del sistema y los almacenes de datos.

Los diagramas permiten identificar cómo se capturan, procesan, almacenan y consultan los datos relacionados con el catálogo de productos, cotizaciones, ventas, inventario, reservas de stock, abastecimiento, recepciones y despachos.

Los flujos representan **información intercambiada por el sistema**, no movimientos físicos de mercadería. Las entradas y salidas reales de productos se reflejan mediante registros y movimientos de Kardex, una vez confirmadas las operaciones correspondientes.

## Índice

- [Nivel 0: Diagrama de contexto](#nivel-0)
- [Nivel 1: Descomposición del sistema](#nivel-1)
- [Consideraciones de consistencia](#consideraciones)
- [Documentación relacionada](#documentacion-relacionada)

<a id="nivel-0"></a>

## Nivel 0: Diagrama de contexto

El diagrama de contexto representa MIGAL SmartOps como un único proceso e identifica los principales intercambios de información que atraviesan la frontera del sistema.

Se consideran cuatro actores internos: Administrador, Vendedor, Almacenero y Reponedor, cuyas interacciones responden a las responsabilidades y permisos establecidos para la operación de la ferretería.

![Diagrama de flujo de datos de nivel 0: Contexto de MIGAL SmartOps](imagenes/MIGAL-DFD-N0-contexto.png)

**Figura 1. Diagrama de contexto de MIGAL SmartOps.**

Los principales flujos de información entre los actores y el sistema se describen a continuación:

| Actor | Información que proporciona | Información que recibe |
|---|---|---|
| **Administrador** | Datos de usuarios, asignación de roles y consultas administrativas. | Estado de usuarios, información de seguimiento y resultados de consultas autorizadas. |
| **Vendedor** | Solicitudes de clientes, datos comerciales, aceptación de cotizaciones y consultas de disponibilidad. | Cotizaciones, información de existencias, cantidades reservadas, faltantes y estados de las ventas. |
| **Almacenero** | Cantidades recibidas, validación de recepciones, ajustes de inventario, traslados y confirmación de salida física. | Órdenes de recepción, guías de despacho, reservas por ubicación y resultados de las operaciones registradas. |
| **Reponedor** | Información de proveedores, órdenes de compra y datos para la evaluación de abastecimiento. | Necesidades de reposición, faltantes, alertas de stock mínimo y estado de compras y recepciones. |

Las solicitudes de clientes y los acuerdos con proveedores son incorporados al sistema por los usuarios internos autorizados. Por tanto, no se considera acceso directo de clientes o proveedores mediante portales externos.

Asimismo, el sistema ofrece consultas mediante un agente de inteligencia artificial, de acuerdo con los permisos de cada usuario y sin otorgarle capacidad de modificar directamente la información transaccional.

<a id="nivel-1"></a>

## Nivel 1: Descomposición del sistema

El diagrama de Nivel 1 descompone el proceso general de MIGAL SmartOps en sus principales procesos internos, mostrando los flujos de información entre estos, los actores y los almacenes de datos.

Esta descomposición permite distinguir las operaciones comerciales, el control de existencias, las reservas de stock y los movimientos físicos de Kardex, evitando que el registro de una venta sea interpretado como una salida inmediata de mercadería.

![Diagrama de flujo de datos de nivel 1: Descomposición de MIGAL SmartOps](imagenes/MIGAL-DFD-N1-descomposicion.png)

**Figura 2. Diagrama de descomposición funcional de MIGAL SmartOps.**

### Procesos principales

| Código | Proceso | Descripción |
|---|---|---|
| **1.0** | Gestión de Catálogo | Administra la información maestra de productos, sus características, categorías, precios referenciales y parámetros de stock mínimo. |
| **2.0** | Procesamiento OCR de Documentos | Extrae información de archivos cargados y normaliza denominaciones comerciales mediante Fuzzy Matching, generando datos preliminares que requieren revisión. |
| **3.0** | Control de Inventario | Consulta existencias físicas, determina cantidades reservadas y libres por almacén, administra reservas y registra movimientos de Kardex por recepciones, despachos, ajustes y traslados. |
| **4.0** | Gestión Comercial | Registra cotizaciones, aceptación del cliente, validación comercial y ventas aprobadas. Coordina las solicitudes de reserva y mantiene información de faltantes y estados operativos. |
| **5.0** | Gestión de Abastecimiento | Administra proveedores, órdenes de compra y recepciones; registra las necesidades pendientes y proporciona información de viabilidad para la aprobación comercial. |
| **6.0** | Gestión de Despacho | Verifica la cobertura completa de los productos comprometidos, administra las guías y procesa la confirmación de salida física para su registro en Kardex. |
| **7.0** | Gestión de Usuarios | Administra cuentas de colaboradores, asignación de roles y estados de acceso. |
| **8.0** | Consultas de IA | Procesa consultas autorizadas sobre información operativa y presenta resultados sin modificar los registros transaccionales. |

### Almacenes de datos

| Código | Almacén de datos | Información que conserva |
|---|---|---|
| **D1** | Catálogo de Productos | Identificación, características y clasificación de productos. |
| **D2** | Inventario y Kardex | Existencias físicas por ubicación e historial de movimientos. |
| **D3** | Registro de Ventas | Ventas aprobadas, productos comprometidos y estados operativos. |
| **D4** | Registro de Cotizaciones | Propuestas comerciales, detalles y estados de aceptación y validación. |
| **D5** | Compras y Recepciones | Órdenes de compra, productos solicitados y cantidades recibidas. |
| **D6** | Catálogo de Proveedores | Proveedores registrados y relaciones con productos. |
| **D7** | Registro de Clientes | Identificación y datos referenciales de los clientes. |
| **D8** | Reservas de Stock | Cantidades asignadas a cada detalle de venta y ubicación. |
| **D9** | Guías de Despacho | Documentos de despacho, estados y confirmaciones de salida. |
| **D10** | Usuarios y Roles | Cuentas, roles y vigencia de los accesos internos. |
| **D11** | Documentos Digitales | Archivos cargados, resultados OCR y datos extraídos. |

Los almacenes de datos representan agrupaciones lógicas de información. Su descomposición en entidades y relaciones específicas se desarrolla en el modelo entidad-relación y el diccionario de datos del proyecto.

<a id="consideraciones"></a>

## Consideraciones de consistencia

Para garantizar la coherencia entre ambos niveles y los demás modelos de MIGAL SmartOps, se consideran las siguientes reglas:

**1. Cotización y registro comercial.** La cotización registra los productos, cantidades, precios y condiciones ofrecidas. Su emisión y aceptación no generan movimientos de inventario. Una vez aceptada por el cliente y validada su viabilidad por MIGAL, podrá originar una única venta.

**2. Validación de abastecimiento.** Cuando existan faltantes, se aplicarán las reglas comerciales acordadas para las categorías de cemento, ladrillos, fierros y agregados. En otras categorías, la viabilidad será evaluada por la responsable de abastecimiento antes de aprobar la venta.

**3. Reservas de existencias.** Una venta aprobada podrá reservar unidades disponibles desde varios almacenes. Las reservas se vincularán a los detalles de venta y a las ubicaciones correspondientes. Las cantidades reservadas no constituyen salidas físicas de Kardex.

**4. Stock físico, reservado y libre.** El sistema distinguirá las existencias físicas de las unidades comprometidas. El stock libre se determinará descontando las reservas activas de las existencias físicas de cada producto y ubicación.

**5. Recepción de mercadería.** La confirmación de una recepción permitirá registrar entradas de Kardex por las cantidades efectivamente recibidas y el almacén correspondiente. Las órdenes de compra y los resultados OCR no modificarán por sí mismos las existencias.

**6. Despacho completo.** El sistema verificará que todos los productos comprometidos de la venta estén disponibles antes de confirmar su salida. La generación o impresión de una guía no produce descuentos físicos; estos se registran al confirmar la salida de la mercadería.

**7. Cancelaciones y trazabilidad.** La cancelación autorizada de una venta antes del despacho liberará las reservas correspondientes y conservará la información de la operación, sin producir movimientos físicos inexistentes.

**8. Integridad del flujo de datos.** Los procesos interactuarán con los almacenes de datos mediante flujos identificados y no se representarán transferencias directas entre actores externos y almacenes de datos, ni entre dos almacenes sin intervención de un proceso.

Las entradas y salidas externas identificadas en el Nivel 0 deberán mantener correspondencia con los flujos representados en el Nivel 1. De esta manera, los diagramas reflejarán una misma frontera del sistema y una descomposición funcional consistente.

<a id="documentacion-relacionada"></a>

## Documentación relacionada

- [Alcance y límites](alcance-y-limites.md).
- [Requerimientos](requerimientos.md).
- [Casos de uso](casos-de-uso.md).
- [Modelo y diccionario de datos](modelo-y-diccionario-datos.md).
- [Arquitectura tecnológica](arquitectura-tecnologica.md).
