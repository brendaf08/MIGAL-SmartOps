# Alcance y límites

> Estado: base documental migrada, pendiente de revisión del equipo.
> Fuente: [Software - entregables](../entregas/pc1/antecedentes/MIGAL-PC1-entregables-recibidos.pdf), páginas 6–10.
> Se conserva el contenido recibido; las observaciones de auditoría no se consideran corregidas ni aprobadas por esta migración.



## Límites

Para garantizar el cumplimiento estricto del cronograma de 16 semanas y mitigar riesgos técnicos, de integración y regulatorios, el sistema no incluirá:

- Módulos de Contabilidad y Finanzas: No se gestionarán cuentas corrientes, cobros a crédito, estados de cuenta, balances contables ni emisión de notas de crédito o débito. La venta se registra únicamente como una salida transaccional de mercadería que descarga el Kardex.

- Ruteo y Gestión de Flota Vehicular: El módulo de despacho se limita a generar una orden interna asociada a una venta previamente registrada para mantener la trazabilidad de la entrega física; no incluye monitoreo de camiones, optimización de rutas ni asignación de choferes. La generación o impresión de la orden de despacho no vuelve a reducir las existencias, debido a que la salida de inventario se registra al confirmarse la venta.

- Facturación electrónica directa ante SUNAT: No se reemplaza el software KeyFacil. El sistema no emitirá comprobantes de pago electrónicos oficiales ni se conectará a los Web Services de la administración tributaria, manteniéndose KeyFacil como sistema legado en paralelo.

- Integración con la API oficial de WhatsApp Business: No se implementará conexión directa con la API de Meta para evitar sobrecostos por licenciamiento por conversación y demoras de verificación empresarial. La ingesta de solicitudes o pedidos digitales se limitará a la carga manual de capturas de pantalla, fotografías o archivos soportados desde el portal web.

- Acceso directo para actores externos: La plataforma no contará con portales de autoservicio para maestros de obra, clientes ni perfiles para choferes de flota. Las cotizaciones, órdenes de despacho físico y el registro de ventas serán gestionados y consultados exclusivamente por el personal interno.

- Aplicaciones móviles nativas y renderizado 3D: No se desarrollarán aplicaciones para Android/iOS ni visores tridimensionales de distribución de almacén. La solución se implementará como una aplicación web responsive optimizada para navegadores de escritorio (PC All-in-One) y tablets.

- Modelos predictivos complejos mediante Machine Learning: Se descarta el entrenamiento de modelos de series temporales para predecir demanda estacional. El reaprovisionamiento se gestionará mediante reglas de negocio deterministas (stock mínimo de seguridad y cálculo de punto de reorden).

## Matriz de fronteras

Síntesis organizada de la matriz recibida (páginas 7–10). La fuente completa se conserva en el PDF enlazado al inicio.

| Dimensión | Elemento | Frontera | Delimitación |
| --- | --- | --- | --- |
| Actores | Usuarios internos: Administrador, Vendedor, Almacenero y Reponedor | Dentro | Acceso web autenticado por roles. |
| Actores | Choferes | Fuera | Reciben la orden interna impresa; no acceden al software. |
| Actores | Clientes y maestros de obra | Fuera | Sin acceso directo; datos referenciales en cotización o venta. |
| Sistemas externos | WhatsApp / Meta | Fuera | Carga manual de capturas y archivos; sin integración API. |
| Sistemas externos | KeyFacil / SUNAT | Fuera | Facturación paralela, sin intercambio directo de datos. |
| Operación | Kardex multi-almacén | Dentro | Existencias por tienda y dos almacenes. |
| Operación | Registro de venta | Dentro | Registro directo o desde cotización aceptada; salida al confirmar. |
| Operación | Abastecimiento y recepción | Dentro | Compras acordadas; entradas por recepción confirmada y validada físicamente. OCR opcional como precarga. |
| Operación | Cotizaciones y selección de proveedores | Fuera | Negociación y evaluación realizadas fuera del sistema. |
| Operación | Orden de despacho | Dentro | Documento interno vinculado con venta; no descuenta stock nuevamente. |
| Operación | Cotización a clientes | Dentro | Productos, cantidades, precios, descuentos, condiciones y vigencia; no reserva ni modifica existencias. |
| Procesamiento | OCR | Dentro | Procesamiento de documentos operativos cargados manualmente. |
| Procesamiento | Fuzzy Matching | Dentro | Sugerencias de asociación con el SKU oficial y validación humana. |
| Procesamiento | Agente consultivo | Dentro | Consultas autorizadas de stock y ubicación en lenguaje natural. |
| Operación | Contabilidad, finanzas y créditos | Fuera | Sin cuentas corrientes, cobranzas ni balances. |
| Operación | Ruteo y flota | Fuera | Sin rutas, choferes, vehículos ni GPS. |
| Procesamiento | Pronóstico complejo | Fuera | Reposición mediante reglas deterministas de mínimo y reorden. |
| Cliente | Web responsive | Dentro | Navegadores de escritorio y tablets. |
| Cliente | App nativa Android / iOS | Fuera | Se utiliza una aplicación web. |
