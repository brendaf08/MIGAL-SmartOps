# Arquitectura Tecnológica de MIGAL SmartOps (Tech Stack)

La plataforma MIGAL SmartOps está diseñada bajo una arquitectura tecnológica en capas, con componentes especializados que se comunican mediante interfaces y protocolos estándar. Esta organización permite separar la presentación, la lógica de negocio, el procesamiento especializado y la persistencia de datos, favoreciendo la mantenibilidad, la integración y el cumplimiento de los requerimientos no funcionales del proyecto.

La arquitectura mantiene un **backend principal como núcleo transaccional**, responsable de aplicar las reglas comerciales y garantizar la consistencia del inventario en la tienda principal y los dos almacenes de MIGAL. Asimismo, incorpora mecanismos para controlar las reservas de existencias, el abastecimiento pendiente, las recepciones de mercadería y los despachos físicos, preservando la trazabilidad de las operaciones.

La arquitectura se divide en los siguientes componentes principales:

## 1. Capa de Presentación (Frontend - React)

- La interfaz de usuario se desarrollará como una aplicación web responsive, optimizada para navegadores de escritorio (PC All-in-One) y tablets utilizados en la ferretería.

- Se utilizará la librería React para construir una Single Page Application (SPA), mediante la cual los usuarios autorizados accederán a las funcionalidades correspondientes a sus roles: Administrador, Vendedor, Almacenero y Reponedor.

- La aplicación permitirá gestionar el catálogo de productos, consultar existencias por almacén, registrar cotizaciones y ventas, consultar reservas de stock, administrar proveedores, registrar órdenes de compra y recepciones, y gestionar los despachos.

- Durante el registro de operaciones comerciales, la interfaz mostrará las cantidades físicas, reservadas y libres de cada producto, tanto por ubicación como de manera consolidada. Esto permitirá identificar faltantes y conocer la disponibilidad antes de aprobar una venta.

- El frontend incorporará un componente visual interactivo de carga manual de archivos (drag-and-drop), compatible con formatos JPG, PNG y PDF, para procesar solicitudes de clientes y documentos utilizados durante la recepción de mercadería.

- Esta capa incluirá la interfaz conversacional del Agente de IA, permitiendo realizar consultas operativas en lenguaje natural desde el navegador, de acuerdo con los permisos del usuario autenticado.

## 2. Capa de Lógica de Negocio (Backend Principal - Java / Spring Boot)

- El núcleo transaccional de MIGAL SmartOps se implementará en Java mediante el framework Spring Boot, exponiendo una API REST para la comunicación con el frontend y los servicios especializados.

- El backend centralizará la aplicación de las reglas de negocio correspondientes a productos, inventario, cotizaciones, clientes, ventas, reservas, proveedores, abastecimiento, recepciones y despachos, evitando que estas validaciones dependan exclusivamente de la interfaz de usuario.

- La gestión de cotizaciones comprenderá su emisión, aceptación por parte del cliente y validación comercial por MIGAL. Una cotización aceptada y cuya atención haya sido considerada viable podrá convertirse en una única venta registrada, conservando sus productos, cantidades, precios y condiciones comerciales.

- Para aprobar ventas con stock insuficiente, el backend aplicará las reglas de viabilidad acordadas: las categorías de cemento, ladrillos, fierros y agregados podrán continuar mediante la aceptación automática de su viabilidad de abastecimiento; en las demás categorías, la aprobación dependerá de la evaluación de la propietaria responsable del abastecimiento. Esta validación no garantiza que la mercadería esté disponible inmediatamente.

- Al registrar una venta aprobada, el sistema consultará las existencias libres en todas las ubicaciones y asignará las cantidades disponibles mediante reservas vinculadas a los detalles de venta y a los inventarios correspondientes. Las cantidades que no puedan reservarse permanecerán pendientes de abastecimiento, sin generar salidas físicas de Kardex.

- La administración del inventario distinguirá entre **stock físico, stock reservado y stock libre**. El stock libre se calculará descontando las reservas activas de las existencias físicas, mientras que el stock mínimo funcionará como umbral de alerta para reposición y no como una restricción que impida reservar unidades disponibles.

- El módulo de abastecimiento gestionará las órdenes de compra asociadas a proveedores registrados y las recepciones de mercadería. El registro de una orden de compra no modificará las existencias físicas; estas se incrementarán únicamente cuando el personal autorizado confirme las cantidades efectivamente recibidas.

- El módulo de despacho verificará que todas las cantidades comprometidas de una venta se encuentren disponibles y reservadas antes de autorizar su salida. Conforme al proceso comercial establecido, la entrega al cliente se realizará de manera completa, sin despachos parciales. **Los movimientos de salida de Kardex se registrarán cuando se confirme la salida física de la mercadería**, no al aprobar la venta ni al generar o imprimir la guía de despacho.

- Las operaciones que comprometan existencias, confirmen recepciones, ejecuten traslados o registren despachos se realizarán mediante transacciones controladas, con mecanismos de concurrencia que eviten reservas duplicadas, cantidades negativas y movimientos de Kardex repetidos.

- El backend permitirá registrar cancelaciones autorizadas antes del despacho, conservando su motivo y liberando las reservas activas correspondientes, sin generar movimientos de salida ficticios.

- Asimismo, calculará alertas y puntos de reorden mediante reglas deterministas basadas en información de existencias y parámetros de reposición, sin incorporar modelos predictivos complejos de Machine Learning.

- Finalmente, el backend proporcionará las funciones de consulta autorizadas que utilizará el Agente de IA mediante *tool calling*, sin permitir que este acceda directamente a la base de datos ni ejecute modificaciones de inventario por su cuenta.

## 3. Capa de Servicios Especializados y Visión (Microservicio - Python)

- Los componentes relacionados con el procesamiento de imágenes y la normalización de información no estructurada se desacoplarán del backend principal mediante un microservicio desarrollado en Python.

- Este microservicio ejecutará el proceso de Extracción Óptica de Caracteres (OCR) para identificar e interpretar información textual contenida en fotografías, capturas de pantalla, notas manuscritas y documentos operativos cargados por los usuarios.

- Incluirá un mecanismo de Fuzzy Matching que comparará las denominaciones identificadas —incluyendo abreviaturas, jergas comerciales y nombres utilizados por clientes o proveedores— con el catálogo maestro de productos, proponiendo los SKU oficiales más próximos.

- Los resultados obtenidos se enviarán al backend principal mediante interfaces de comunicación definidas, para que puedan utilizarse como información preliminar en la elaboración de cotizaciones o el registro de recepciones de mercadería.

- La información extraída y normalizada deberá presentarse al usuario para su revisión y corrección. **El procesamiento OCR y Fuzzy Matching no generará por sí mismo ventas, reservas ni movimientos de Kardex.** La confirmación de estas operaciones permanecerá bajo responsabilidad del backend y de los usuarios autorizados.

## 4. Capa de Persistencia de Datos (Base de Datos - PostgreSQL)

- El almacenamiento de información se gestionará mediante PostgreSQL, utilizando un modelo relacional que mantenga la integridad de las entidades, sus asociaciones y los registros transaccionales.

- La base de datos almacenará el catálogo maestro de productos, sus categorías y características comerciales; la configuración de la tienda principal y los almacenes; así como los registros de usuarios, clientes y proveedores.

- Para el control de inventario, se mantendrán las existencias físicas por producto y ubicación, las reservas asignadas a los detalles de venta y el historial de movimientos de Kardex. Las cantidades reservadas y libres podrán obtenerse a partir de los registros correspondientes, evitando la duplicación innecesaria de saldos.

- La información comercial comprenderá las cotizaciones, sus detalles, los registros de aceptación y validación, las ventas aprobadas, los productos comprometidos y sus estados operativos. Cada venta conservará su relación con la cotización de origen y con el cliente correspondiente.

- Los procesos de abastecimiento y despacho conservarán sus respectivos registros de órdenes de compra, detalles, recepciones, guías de despacho y movimientos físicos de entrada o salida, permitiendo identificar la operación que originó cada movimiento.

- También se almacenarán las referencias a documentos digitales procesados mediante OCR y los datos extraídos, conservando su asociación con las operaciones correspondientes.

- La integridad de la información se garantizará mediante claves primarias y foráneas, restricciones de unicidad, validaciones de cantidades y transacciones ACID. Asimismo, se contemplarán mecanismos de respaldo y recuperación, así como trazabilidad de usuarios, fechas y operaciones relevantes.

## 5. Infraestructura y Despliegue (Docker)

- Los componentes principales de MIGAL SmartOps se empaquetarán mediante contenedores Docker para facilitar la reproducibilidad del entorno, el aislamiento de dependencias y el despliegue de la aplicación.

- El entorno contemplará los componentes correspondientes al frontend React, el backend Java/Spring Boot, el microservicio Python para OCR y Fuzzy Matching, y la base de datos PostgreSQL.

- La comunicación entre componentes se realizará mediante interfaces definidas. El frontend consumirá los servicios de la API REST, el backend coordinará el procesamiento especializado y únicamente los componentes autorizados accederán a los recursos de persistencia.

- La configuración de credenciales, conexiones y demás parámetros sensibles se gestionará de forma externa a las imágenes de los contenedores, evitando incorporar información confidencial en el código fuente o los artefactos de despliegue.

- Esta organización permitirá mantener separadas las responsabilidades de cada componente y realizar modificaciones o actualizaciones sin alterar innecesariamente el resto del sistema.

## 6. Diagrama de arquitectura tecnológica

El siguiente diagrama representa la organización de MIGAL SmartOps en sus principales capas tecnológicas: presentación, lógica de negocio, servicios especializados y persistencia de datos, integradas dentro de un entorno de despliegue basado en Docker.

![Arquitectura tecnológica de MIGAL SmartOps](imagenes/MIGAL-ARQ-01-arquitectura-tecnologica.png)

**Figura 1. Arquitectura tecnológica de MIGAL SmartOps.** El usuario autorizado interactúa con la aplicación web React, que se comunica mediante solicitudes HTTP/REST con el backend Spring Boot. Este coordina las operaciones de negocio, el procesamiento OCR y Fuzzy Matching mediante el microservicio Python, y la lectura y escritura transaccional en PostgreSQL.

El diagrama presenta la distribución tecnológica general del sistema. Las reglas específicas de aceptación comercial, reservas de stock, abastecimiento y confirmación física de despachos se implementan dentro de la capa de lógica de negocio, sin requerir componentes tecnológicos adicionales.

El alcance de esta arquitectura excluye la emisión directa de comprobantes electrónicos ante SUNAT, los módulos contables y financieros, la integración con la API oficial de WhatsApp Business y las funcionalidades de gestión de flota, ruteo o seguimiento GPS.
