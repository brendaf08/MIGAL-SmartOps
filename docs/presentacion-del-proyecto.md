# Presentación del proyecto

## Problemática

La Ferretería Progresol Migal opera bajo un modelo de alta dependencia del conocimiento empírico de sus dueños (administradores), con registros manuales y no digitalizados centralizados en ellos. La determinación de precios y el conocimiento del catálogo residen principalmente en la memoria de los dueños, lo que genera un cuello de botella operativo diario, interrupciones constantes durante la atención y demoras o errores al responder solicitudes de clientes, preparar cotizaciones y concretar ventas.

A nivel logístico, no existe visibilidad en tiempo real de las existencias de la tienda ni de los dos almacenes. El control de stock es visual y empírico, lo que dificulta conocer con precisión la disponibilidad de mercadería y puede ocasionar quiebres de inventario en productos críticos.

Asimismo, las compras y recepciones de mercadería se gestionan de forma predominantemente manual. La información de proveedores, órdenes de compra y cantidades efectivamente recibidas no se encuentra centralizada, y durante la recepción el personal debe identificar y registrar manualmente los productos entregados, aun cuando las denominaciones utilizadas por los proveedores pueden diferir de las empleadas en el catálogo interno de la ferretería.

Herramientas convencionales como Excel resultan insuficientes para resolver esta situación, principalmente porque:

1. No permiten procesar de forma automatizada documentos no estructurados, como fotografías de notas manuscritas, capturas de pedidos digitales o documentos utilizados durante la recepción de mercadería.

2. No implementan reglas de integridad referencial ni control transaccional suficientes para mantener la consistencia de los datos entre múltiples almacenes, ventas, compras y recepciones.

3. No mitigan la ambigüedad en la denominación de productos, ya sea por nombres informales utilizados por clientes o por denominaciones comerciales empleadas por proveedores, ni automatizan el control del Kardex.

## Presentación y propuesta de valor

MIGAL SmartOps es una plataforma web de gestión operativa e inventario diseñada para reducir la dependencia del conocimiento empírico de los dueños de la Ferretería Progresol Migal y centralizar la información necesaria para la atención comercial, el control de existencias y el abastecimiento. Su valor diferencial se basa en tres pilares:

1. **Control centralizado de múltiples almacenes:** permite conocer las existencias en tiempo real de la tienda y los dos almacenes. Así, los colaboradores autorizados pueden verificar la disponibilidad de un producto, registrar movimientos, atender ventas y planificar el reabastecimiento antes de que ocurra un quiebre de stock.

2. **Digitalización y normalización de documentos operativos:** permite procesar fotografías, capturas y documentos mediante OCR y algoritmos de coincidencia difusa. La información extraída es relacionada con el SKU oficial del catálogo y posteriormente validada por el usuario, pudiendo utilizarse para preparar cotizaciones o ventas a clientes y para agilizar el registro de productos durante la recepción de mercadería.

3. **Operaciones articuladas por inventario (Core):** el stock real gobierna las operaciones cotidianas. Las ventas confirmadas generan automáticamente los movimientos de salida en el Kardex, mientras que las recepciones confirmadas generan los movimientos de entrada correspondientes. Las órdenes de despacho mantienen trazabilidad con las ventas sin volver a descontar existencias, y los niveles mínimos de stock permiten identificar necesidades de reposición.

## Objetivos

### Objetivo general

Desarrollar e implementar una plataforma web modular (MIGAL SmartOps) que centralice la gestión de inventario de múltiples almacenes, estandarice el catálogo de productos, permita gestionar cotizaciones y ventas, y automatice la captura y normalización de documentos operativos mediante OCR y coincidencia difusa, para que los colaboradores de la Ferretería Progresol Migal trabajen con mayor autonomía, trazabilidad e información oportuna.

### Objetivos específicos

1. **Implementar el núcleo transaccional multi-almacén:** construir una API REST en Spring Boot con PostgreSQL que gestione el Kardex de existencias físicas en tiempo real y mantenga sincronizadas las existencias de la tienda y los dos almacenes durante operaciones de venta, recepción, ajustes y traslados internos.

2. **Desarrollar el módulo de extracción óptica de documentos:** implementar un microservicio en Python con OCR capaz de procesar imágenes y archivos de documentos operativos, incluyendo solicitudes o pedidos de clientes y documentos utilizados durante la recepción de mercadería, extrayendo información no estructurada para su posterior validación y utilización dentro del sistema.

3. **Construir el motor de normalización del catálogo mediante Fuzzy Matching:** diseñar un algoritmo de coincidencia difusa que relacione términos ambiguos, abreviaturas, nombres comerciales utilizados por clientes y denominaciones empleadas por proveedores con el SKU oficial del catálogo maestro, reduciendo errores de identificación y digitación.

4. **Desplegar un asistente consultivo mediante agente de IA y tool calling:** integrar en el portal web una interfaz conversacional capaz de consultar de forma controlada los endpoints autorizados del backend y responder en lenguaje natural sobre disponibilidad de productos, niveles críticos de stock y ubicación física de mercadería, respetando los permisos del usuario autenticado.
