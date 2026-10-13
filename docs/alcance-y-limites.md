# Alcance y límites del proyecto

## Límites del proyecto

Para garantizar el cumplimiento estricto del cronograma de 16 semanas y mitigar riesgos técnicos, de integración y regulatorios, el sistema no incluirá:

- **Módulos de Contabilidad y Finanzas:** No se gestionarán cuentas corrientes, cobros a crédito, estados de cuenta, balances contables ni emisión de notas de crédito o débito. La venta se registra únicamente como una salida transaccional de mercadería que descarga el Kardex.

- **Ruteo y Gestión de Flota Vehicular:** El módulo de despacho se limita a generar una orden interna asociada a una venta previamente registrada para mantener la trazabilidad de la entrega física; no incluye monitoreo de camiones, optimización de rutas ni asignación de choferes. La generación o impresión de la orden de despacho no vuelve a reducir las existencias, debido a que la salida de inventario se registra al confirmarse la venta.

- **Facturación electrónica directa ante SUNAT:** No se reemplaza el software KeyFacil. El sistema no emitirá comprobantes de pago electrónicos oficiales ni se conectará a los Web Services de la administración tributaria, manteniéndose KeyFacil como sistema legado en paralelo.

- **Integración con la API oficial de WhatsApp Business:** No se implementará conexión directa con la API de Meta para evitar sobrecostos por licenciamiento por conversación y demoras de verificación empresarial. La ingesta de solicitudes o pedidos digitales se limitará a la carga manual de capturas de pantalla, fotografías o archivos soportados desde el portal web.

- **Acceso directo para actores externos:** La plataforma no contará con portales de autoservicio para maestros de obra, clientes ni perfiles para choferes de flota. Las cotizaciones, órdenes de despacho físico y el registro de ventas serán gestionados y consultados exclusivamente por el personal interno.

- **Aplicaciones móviles nativas y renderizado 3D:** No se desarrollarán aplicaciones para Android/iOS ni visores tridimensionales de distribución de almacén. La solución se implementará como una aplicación web responsive optimizada para navegadores de escritorio (PC All-in-One) y tablets.

- **Modelos predictivos complejos mediante Machine Learning:** Se descarta el entrenamiento de modelos de series temporales para predecir demanda estacional. El reaprovisionamiento se gestionará mediante reglas de negocio deterministas (stock mínimo de seguridad y cálculo de punto de reorden).

## Matriz técnica de fronteras del sistema (ISO/IEC 15288)

| Dimensión de frontera | Elemento / Componente | Delimitación | Tipo de interfaz / Justificación técnica |
| --- | --- | --- | --- |
| Actores y Usuarios | Usuarios Internos (Administrador, Vendedor, Almacenero, Reponedor) | DENTRO | Interfaz Gráfica Web (GUI) mediante autenticación basada en roles (Administrador, Vendedor, Almacenero y Reponedor). |
| Actores y Usuarios | Choferes de flota (SMP) | FUERA | Entidad Externa. Reciben únicamente la orden de despacho física impresa generada tras la descarga de stock. |
| Actores y Usuarios | Maestros de obra y clientes | FUERA | Entidad Externa. No interactúan directamente con el software; sus datos básicos pueden registrarse durante la elaboración de una cotización o al concretar una venta directa. |
| Sistemas Externos | Meta / WhatsApp Cloud API | FUERA | Interfaz de frontera: Carga manual de archivos (PNG, JPG, PDF) mediante componente web (drag-and-drop), sin consumo de API REST de Meta. |
| Sistemas Externos | Sistema Fiscal KeyFacil / SUNAT | FUERA | Frontera cerrada: Mantenido como sistema paralelo para facturación oficial; opera sin intercambio directo de datos con el software. |
| Módulos y Procesamiento | Control de Kardex Multi-Almacén (CORE) | DENTRO | Núcleo transaccional en PostgreSQL que controla en tiempo real las existencias en tienda y 2 almacenes. |
| Módulos y Procesamiento | Registro de Venta Directa | DENTRO | Captura operativa de transacciones (cliente, productos, cantidades, precios y monto total) con deducción automática e inmediata de inventario al confirmarse la venta. La venta puede registrarse directamente o generarse a partir de una cotización previamente aceptada. |
| Módulos y Procesamiento | Abastecimiento Operativo (Órdenes de Compra e Ingresos) | DENTRO | Registro de órdenes de compra y recepción de mercadería para incrementar el stock físico en Kardex. Durante la recepción, el sistema podrá utilizar de forma opcional OCR y normalización difusa para precargar la información contenida en documentos del proveedor, manteniéndose obligatoria la validación del personal antes de actualizar el inventario. |
| Módulos y Procesamiento | Cotizaciones y Selección de Proveedores | FUERA | Excluido del software. La solicitud, comparación y evaluación de cotizaciones provenientes de proveedores es un proceso manual y empírico realizado por los dueños fuera del sistema; el software solo recibe la compra ya acordada. |
| Módulos y Procesamiento | Órdenes de Despacho Físico | DENTRO | Emisión de documento interno asociado a una venta previamente registrada para apoyar y mantener la trazabilidad de la entrega física de la mercadería. Su generación o impresión no vuelve a reducir el stock, debido a que la salida del Kardex se registra al confirmarse la venta. |
| Módulos y Procesamiento | Cotizaciones a Clientes | DENTRO | Registro de propuestas comerciales dirigidas a clientes, incluyendo productos, cantidades, precios, descuentos, condiciones y periodo de vigencia. La cotización podrá encontrarse en estado emitida, aceptada, rechazada o vencida y podrá convertirse posteriormente en una venta. Su emisión no modifica ni reserva existencias del inventario. |
| Módulos y Procesamiento | Extracción Óptica de Texto (OCR) | DENTRO | Microservicio en Python para segmentar y digitalizar texto a partir de fotos de notas manuscritas y capturas de pedidos. |
| Módulos y Procesamiento | Normalización Lingüística (Fuzzy Logic) | DENTRO | Algoritmo difuso para asociar términos comerciales, ambiguos o denominaciones utilizadas por clientes y proveedores con el catálogo oficial de SKU de la ferretería. |
| Módulos y Procesamiento | Agente Consultivo (Tool Calling) | DENTRO | Asistente en lenguaje natural para consultas operativas sobre stock físico, ubicación en almacén. |
| Módulos y Procesamiento | Contabilidad, Finanzas y Créditos | FUERA | Excluido del alcance del software; la evaluación crediticia y balances contables siguen bajo gestión manual externa. |
| Módulos y Procesamiento | Ruteo y Gestión de Flota | FUERA | Excluido por tiempo de desarrollo; la logística de vehículos sigue bajo la coordinación directa del administrador. |
| Módulos y Procesamiento | Modelos de IA de Pronóstico Complejo | FUERA | Excluido por tiempo y falta de histórico estructurado; se utiliza cálculo estándar de punto de reorden. |
| Infraestructura y Clientes | Terminal Web Responsive | DENTRO | Acceso vía navegador web estándar optimizado para la PC All-in-One y tablets de mostrador. |
| Infraestructura y Clientes | App Móvil Nativa (Android / iOS) | FUERA | Excluido para concentrar los esfuerzos de desarrollo en una única base de código web desacoplada. |