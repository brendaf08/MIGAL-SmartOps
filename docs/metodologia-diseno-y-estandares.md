# Metodología de diseño y aplicación de estándares

El desarrollo de los entregables de MIGAL SmartOps se fundamenta en un enfoque de ingeniería de software orientado al análisis de requerimientos, el diseño estructural y la trazabilidad de las decisiones técnicas.

La metodología aplicada busca mantener la coherencia entre las necesidades operativas de la ferretería, los requerimientos funcionales y no funcionales, los casos de uso, los modelos de clases y datos, los Diagramas de Flujo de Datos (DFD) y la arquitectura tecnológica propuesta.

Para orientar este trabajo se consideran los cuerpos de conocimiento SWEBOK y SEBoK, así como los procesos de ciclo de vida establecidos en las normas ISO/IEC/IEEE 12207 e ISO/IEC/IEEE 15288. Estas referencias proporcionan criterios metodológicos para organizar los entregables y revisar su consistencia, sin implicar una certificación formal de cumplimiento.

## 1. Propósito

La metodología tiene como propósito establecer el procedimiento utilizado para analizar, actualizar y verificar los modelos de diseño de MIGAL SmartOps, considerando las observaciones recibidas durante la segunda práctica calificada (PC2).

Se adopta un enfoque incremental que permite revisar los componentes existentes, identificar las modificaciones necesarias y conservar aquellos elementos que continúan siendo válidos.

El procedimiento comprende las siguientes actividades:

- Revisar las observaciones académicas y los requerimientos del sistema.
- Identificar los procesos comerciales y operativos involucrados.
- Actualizar los casos de uso y las reglas de negocio correspondientes.
- Revisar las clases, entidades, atributos y relaciones del modelo de datos.
- Representar los intercambios de información mediante los DFD.
- Verificar la coherencia con la arquitectura tecnológica.
- Mantener la trazabilidad de los cambios y establecer criterios de validación.

Este enfoque permite desarrollar los entregables de manera progresiva, sin reconstruir innecesariamente los modelos existentes.

## 2. Fuentes de información y control de versiones

Para la actualización del diseño se consideran las siguientes fuentes:

- Las observaciones y correcciones correspondientes a PC2.
- La documentación académica y los entregables previos del proyecto MIGAL SmartOps.
- Los requerimientos funcionales y no funcionales registrados en el repositorio.
- Los casos de uso, especificaciones, diagramas de actividades y secuencia.
- La información operativa proporcionada por la ferretería MIGAL y las decisiones comerciales validadas durante el análisis.
- Los cambios documentales incorporados mediante el pull request #3 del repositorio.

GitHub se utiliza como mecanismo de control de versiones, permitiendo identificar modificaciones, conservar el historial de los archivos y realizar revisiones antes de integrar los entregables.

La documentación técnica se organiza principalmente en la carpeta `docs/`, donde se mantienen los modelos, diccionarios, diagramas y demás archivos de diseño.

Cuando se identifican diferencias entre las especificaciones existentes y las reglas operativas validadas, estas se registran como puntos pendientes de conciliación para evitar inconsistencias entre documentos.

## 3. Procedimiento de diseño

La actualización de los entregables se desarrolla mediante las siguientes etapas:

### 3.1. Revisión de requerimientos y casos de uso

Se analizan los requerimientos funcionales y no funcionales, las especificaciones de casos de uso y las observaciones académicas para identificar las operaciones afectadas.

Se presta especial atención a la gestión de cotizaciones, ventas con stock insuficiente, clientes, proveedores, inventario, abastecimiento y despacho.

Las modificaciones deben conservar correspondencia con los procesos descritos y con las responsabilidades de los usuarios del sistema.

### 3.2. Actualización del modelo de clases y datos

Se revisan las clases y entidades existentes para determinar qué atributos, relaciones y restricciones deben conservarse, modificarse o incorporarse.

El diseño considera la separación entre las operaciones comerciales y el control físico de inventario, distinguiendo los registros de cotizaciones y ventas de las reservas de existencias y los movimientos de Kardex.

Asimismo, se incorporan las relaciones necesarias para identificar clientes, proveedores, productos, almacenes y operaciones asociadas, manteniendo la integridad referencial y la trazabilidad histórica.

### 3.3. Modelado del flujo de información

Los Diagramas de Flujo de Datos representan las entradas, salidas y transformaciones de información del sistema.

El DFD de Nivel 0 identifica los intercambios entre MIGAL SmartOps y sus usuarios internos, mientras que el DFD de Nivel 1 descompone las principales funciones del sistema.

Durante su revisión se comprueba que los flujos representen información y no movimientos físicos de mercadería, y que exista correspondencia entre ambos niveles.

### 3.4. Revisión de la arquitectura tecnológica

Se verifica que la arquitectura propuesta pueda soportar las funcionalidades definidas mediante sus componentes tecnológicos:

- React para la presentación e interacción con los usuarios.
- Java y Spring Boot para la lógica de negocio y las operaciones transaccionales.
- Python para el procesamiento OCR y Fuzzy Matching.
- PostgreSQL para la persistencia e integridad de los datos.
- Docker para la organización y despliegue de los componentes.

La revisión considera las responsabilidades de cada capa, sus interfaces y los mecanismos necesarios para preservar la consistencia de las operaciones.

### 3.5. Trazabilidad y validación

Los requerimientos se relacionan con sus casos de uso, clases, servicios y componentes de realización mediante la matriz de trazabilidad.

Esta correspondencia permite identificar qué entregables deben revisarse cuando se modifica una regla funcional.

Finalmente, se establecen escenarios de comprobación para detectar contradicciones entre los documentos y verificar que las decisiones de diseño estén representadas de manera consistente.

## 4. Aplicación de estándares y referencias metodológicas

Las referencias de ingeniería de software y sistemas se utilizan para orientar la organización y revisión de los entregables.

| Referencia | Aplicación en MIGAL SmartOps | Evidencia documental |
|---|---|---|
| **SWEBOK** | Orienta el análisis de requerimientos, el diseño de software y la revisión de consistencia entre modelos. | Requerimientos, casos de uso, modelos de clases y datos, matriz de trazabilidad. |
| **SEBoK** | Apoya la comprensión del sistema, sus actores, límites, componentes e interfaces. | Alcance y límites, DFD, arquitectura tecnológica. |
| **ISO/IEC/IEEE 12207** | Proporciona referencias sobre procesos del ciclo de vida del software, diseño, documentación y verificación. | Modelos estructurales, arquitectura, especificaciones y criterios de revisión. |
| **ISO/IEC/IEEE 15288** | Orienta el análisis del sistema de interés, las funciones, interfaces y restricciones de su entorno. | Definición del alcance, procesos, actores y relaciones del sistema. |

### Aplicación al proyecto

**SWEBOK** permite organizar las actividades relacionadas con el análisis y diseño de software, procurando que los requerimientos se encuentren representados en los modelos técnicos correspondientes.

**SEBoK** contribuye a comprender MIGAL SmartOps como un sistema integrado por personas, procesos, información y componentes tecnológicos, cuyas interacciones deben mantenerse dentro de los límites definidos.

**ISO/IEC/IEEE 12207** se utiliza como referencia para la elaboración y revisión de artefactos del diseño de software, especialmente en la definición de componentes, interfaces y mecanismos de verificación.

**ISO/IEC/IEEE 15288** orienta la identificación de las fronteras del sistema de interés y sus relaciones con los actores y procesos del entorno operativo de la ferretería.

Estas referencias se aplican de manera proporcional al alcance académico del proyecto. Su utilización no implica que se hayan ejecutado todos los procesos descritos en ellas ni que exista una auditoría formal de cumplimiento normativo.

## 5. Decisiones principales de diseño

Durante la revisión del proyecto se establecieron decisiones orientadas a representar adecuadamente las operaciones comerciales y el control de existencias de MIGAL.

| Decisión | Justificación | Aplicación en el diseño |
|---|---|---|
| Representar el pedido aprobado mediante Venta | Evitar registros redundantes cuando la aprobación comercial origina directamente una venta. | Relación entre Cotizacion, Venta y DetalleVenta. |
| Incorporar reservas de stock | Identificar las cantidades comprometidas para cada venta y almacén. | ReservaStock, Inventario y DetalleVenta. |
| Diferenciar existencias físicas, reservadas y libres | Evitar comprometer una misma cantidad en operaciones distintas. | Consultas y reglas de control de inventario. |
| Registrar Kardex según movimientos físicos | Separar la aprobación comercial de la entrada o salida real de mercadería. | Movimientos de entrada por recepción y salida por despacho confirmado. |
| Mantener una entrega completa por venta | Representar la modalidad operativa acordada para atender pedidos de clientes. | Gestión de Despacho y validación de cantidades disponibles. |
| Incorporar Cliente y Proveedor como entidades independientes | Facilitar la identificación y trazabilidad de las operaciones. | Relaciones con cotizaciones, ventas, compras y recepciones. |
| Separar descuentos comerciales de movimientos de inventario | Distinguir los cambios de importes de los cambios de cantidades físicas. | Cotización, Venta, DFD y Kardex. |
| Controlar operaciones concurrentes | Evitar reservas duplicadas y actualizaciones inconsistentes. | Transacciones y validaciones del backend. |

Adicionalmente, la validación de ventas con stock insuficiente considera las condiciones de abastecimiento de la ferretería. Para cemento, ladrillos, fierros y agregados se contempla la aceptación automática de la viabilidad de abastecimiento, mientras que las demás categorías requieren evaluación de la propietaria responsable.

Estas decisiones forman parte del diseño propuesto y deben mantener correspondencia con los requerimientos y casos de uso definitivos.

## 6. Verificación de los entregables

La verificación documental busca identificar inconsistencias entre los modelos y comprobar que los elementos definidos respondan a los requerimientos correspondientes.

Los principales criterios de revisión son:

- **Requerimientos y casos de uso:** verificar que las reglas comerciales y los estados de las operaciones estén descritos de manera coherente.
- **Modelo de clases:** revisar atributos, responsabilidades, asociaciones y cardinalidades.
- **Modelo entidad-relación:** comprobar claves primarias, claves foráneas, restricciones y correspondencia con el diccionario de datos.
- **Diagramas de Flujo de Datos:** comprobar que los actores, procesos y flujos del Nivel 1 mantengan correspondencia con el diagrama de contexto.
- **Arquitectura tecnológica:** verificar la distribución de responsabilidades entre los componentes y las interfaces de comunicación.
- **Matriz de trazabilidad:** comprobar que los requerimientos tengan una realización identificable y puntos concretos de verificación.
- **Consistencia documental:** revisar nombres, identificadores, rutas de imágenes, referencias cruzadas y versiones de los archivos.

Asimismo, se consideran escenarios de validación relacionados con:

1. Registro de venta con stock suficiente.
2. Registro de venta aprobada con cantidades pendientes de abastecimiento.
3. Reserva de productos disponibles en diferentes almacenes.
4. Confirmación de recepción y generación de movimientos de entrada.
5. Cancelación autorizada de una venta antes del despacho.
6. Confirmación de despacho completo y prevención de salidas duplicadas.
7. Ejecución concurrente de reservas sobre un mismo inventario.
8. Validación de proveedores activos para registrar órdenes de compra.

Los escenarios representan verificaciones previstas para el diseño. Su definición no implica que las pruebas de software hayan sido implementadas o ejecutadas.

## 7. Limitaciones y aspectos pendientes de integración

La actualización de los entregables considera las reglas comerciales validadas durante el análisis de MIGAL. Sin embargo, permanecen algunas diferencias con los requerimientos y casos de uso incorporados durante las correcciones de PC2.

La principal diferencia corresponde a la atención de ventas con stock insuficiente. Mientras que determinados requerimientos contemplan atención y entrega parcial, el modelo operativo propuesto considera la **reserva parcial de existencias disponibles y un despacho completo posterior**.

Ambas operaciones deben distinguirse: reservar parcialmente permite comprometer unidades existentes sin entregarlas todavía; despachar parcialmente supone realizar una salida física de una parte del pedido.

Por ello, será necesario confirmar que la modalidad propuesta satisface las observaciones del docente y actualizar los requerimientos afectados cuando corresponda.

También deberá armonizarse la obligatoriedad de asociar clientes a cotizaciones y ventas, así como los estados definitivos de las operaciones comerciales.

Estas diferencias se mantienen identificadas en la matriz de trazabilidad para facilitar su revisión y evitar que se consideren resueltas sin la correspondiente validación.

## 8. Criterios de aceptación de los entregables

Los entregables de diseño de MIGAL SmartOps podrán considerarse integrados cuando cumplan las siguientes condiciones:

- Los requerimientos y casos de uso reflejen las reglas funcionales aprobadas.
- Los diagramas y diccionarios presenten atributos, relaciones y restricciones consistentes.
- Los DFD mantengan correspondencia entre sus niveles y con los procesos del sistema.
- La arquitectura tecnológica represente los componentes y responsabilidades definidos.
- La matriz de trazabilidad permita identificar los elementos afectados por cada requerimiento.
- Las diferencias funcionales identificadas hayan sido revisadas y resueltas por los responsables correspondientes.
- Los documentos y figuras finales se encuentren actualizados y correctamente referenciados dentro del repositorio.

La aceptación de estos entregables corresponde a la revisión académica del diseño y no debe interpretarse como evidencia de implementación completa, ejecución satisfactoria de pruebas de software o certificación de cumplimiento de estándares.
