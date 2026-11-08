# Componentes del sistema de diseño

Catálogo de componentes y pantallas de referencia. Cada documento indica cuándo usar el componente, qué provee el consumidor, las reglas de uso y su equivalente en Mantine. La vista previa de cada uno se encuentra en el [prototipo](../prototipo/README.md).

| Grupo | Componente | Descripción |
| --- | --- | --- |
| Fundamentos | [Icon](icon.md) | Conjunto de íconos de trazo para toda la interfaz, dibujado sobre una rejilla de 24 px con trazo de 1.75 px, terminaciones redondeadas y color heredado (`currentColor`). |
| Acciones | [Button](button.md) | Acción que el usuario dispara. |
| Acciones | [IconButton](icon-button.md) | Botón cuadrado que muestra solo un ícono. |
| Formularios | [Checkbox](checkbox.md) | Casilla de verificación para opciones independientes y selección múltiple en tablas. |
| Formularios | [Dropzone](dropzone.md) | Zona de carga de documentos operativos (notas manuscritas, capturas de pedidos y guías de proveedores) para su procesamiento mediante OCR. |
| Formularios | [NumberInput](number-input.md) | Entrada de cantidades con botones de disminuir y aumentar, soporte de teclado (flechas arriba y abajo), límites y unidad de medida visible. |
| Formularios | [SearchInput](search-input.md) | Campo de búsqueda con ícono de lupa y botón "Limpiar búsqueda" visible cuando hay texto. |
| Formularios | [Select](select.md) | Selector de una opción entre varias, basado en el elemento nativo `select` para conservar teclado, lectores de pantalla y selector táctil del sistema operativo. |
| Formularios | [Switch](switch.md) | Interruptor (`role="switch"`) para activar o desactivar una opción con efecto inmediato, como el estado activo de un cliente, proveedor o usuario. |
| Formularios | [TextInput](text-input.md) | Campo de una línea con etiqueta visible, texto de ayuda, mensaje de error y asociación automática entre etiqueta, ayuda y error (`for`, `aria-describedby`, `aria-invalid`). |
| Formularios | [Textarea](textarea.md) | Campo de texto de varias líneas para motivos, observaciones y condiciones comerciales. |
| Datos y estados | [Badge](badge.md) | Etiqueta de una línea que clasifica un registro o un estado. |
| Datos y estados | [MovementBadge](movement-badge.md) | Indica si un movimiento de Kardex es de entrada o de salida y, opcionalmente, su cantidad con signo. |
| Datos y estados | [Pagination](pagination.md) | Navegación entre páginas de un listado, con el resumen "Mostrando 41–60 de 238". |
| Datos y estados | [Stat](stat.md) | Indicador con una cifra destacada, para alertas operativas del panel inicial (stock crítico, ventas pendientes de abastecimiento, recepciones por validar, despachos listos). |
| Datos y estados | [StatusBadge](status-badge.md) | Insignia de estado para cada entidad del modelo de dominio. |
| Datos y estados | [Table](table.md) | Tabla de datos semántica (`table`, `th scope`, `aria-sort`) con ordenamiento, selección, estado de carga con esqueletos y estado vacío. |
| Retroalimentación | [Alert](alert.md) | Mensaje persistente dentro de la página que informa un estado, una advertencia o un error relacionado con el contenido cercano. |
| Retroalimentación | [ConfirmDialog](confirm-dialog.md) | Diálogo de confirmación para acciones que modifican inventario o no se pueden deshacer: cancelar una venta, confirmar el despacho, confirmar una recepción, desactivar un proveedor. |
| Retroalimentación | [EmptyState](empty-state.md) | Contenido que se muestra cuando una lista, tabla o panel no tiene datos. |
| Retroalimentación | [Modal](modal.md) | Ventana modal que concentra una tarea corta (registrar un movimiento manual, completar datos de un cliente) sin salir de la página. |
| Retroalimentación | [Notification](notification.md) | Mensaje flotante breve que confirma el resultado de una acción o informa un proceso en curso. |
| Retroalimentación | [Skeleton](skeleton.md) | Bloque animado que reserva el espacio del contenido mientras llega la respuesta del servidor. |
| Retroalimentación | [Spinner](spinner.md) | Indicador circular de actividad para esperas menores a unos segundos (botones, notificaciones, consulta del agente). |
| Navegación | [AppShell](app-shell.md) | Estructura general de la aplicación: barra lateral de navegación, cabecera con ruta y usuario, y área de contenido con enlace "Saltar al contenido". |
| Navegación | [Breadcrumbs](breadcrumbs.md) | Ruta jerárquica de la página actual, con enlaces a los niveles superiores. |
| Navegación | [PageHeader](page-header.md) | Cabecera estándar de cada página: ruta, título (`h1`), estado de la entidad, descripción y acciones principales. |
| Navegación | [Stepper](stepper.md) | Indicador de avance de un proceso con pasos ordenados. |
| Navegación | [Tabs](tabs.md) | Pestañas para alternar vistas de un mismo contexto, por ejemplo "Todos", "Stock crítico" y "Movimientos" dentro de inventario. |
| Estructura | [ActionBar](action-bar.md) | Barra de resumen y acciones que acompaña a un flujo de confirmación. |
| Estructura | [Card](card.md) | Contenedor de contenido relacionado, con encabezado opcional (título, descripción y acciones), cuerpo y pie. |
| Estructura | [Columns](columns.md) | Disposición en columnas con separación de la escala `space-*`. |
| Estructura | [Constancia](constancia.md) | Constancia que cierra una operación de inventario (recepción, despacho, ajuste, traslado). |
| Estructura | [Stack](stack.md) | Contenedor flexible que ordena a sus hijos en columna o fila con un espaciado tomado de la escala `space-*`. |
| Inventario | [MatchConfidence](match-confidence.md) | Indicador del grado de similitud entre un término extraído por OCR (o escrito por el usuario) y un producto del catálogo maestro. |
| Inventario | [StockBar](stock-bar.md) | Barra segmentada que muestra de un vistazo cómo se reparte el stock de un producto: libre, reservado y, cuando existe, la cantidad solicitada sin cobertura (pendiente de abastecimiento). |
| Inventario | [StockLocations](stock-locations.md) | Plano esquemático de dónde se encuentra un producto: una celda por ubicación (tienda principal y los dos almacenes), con la cantidad libre, una barra de la parte libre frente a la reservada y el ancho de la celda proporcional a la existencia. |
| Inventario | [StockSummary](stock-summary.md) | Resumen de la existencia de un producto en una ubicación o de forma consolidada. |
| Agente de IA | [ChatComposer](chat-composer.md) | Zona de entrada del chat: campo de texto, botón de envío y sugerencias de consultas frecuentes. |
| Agente de IA | [ChatMessage](chat-message.md) | Mensaje de la conversación con el agente consultivo de IA. |
| Pantallas | [PantallaAcceso](pantalla-acceso.md) | Pantalla de inicio de sesión (CU-01) con campos grandes (`size="lg"`) para uso táctil, aviso de error en línea y un único botón principal. |
| Pantallas | [PantallaAccesoMovil](pantalla-acceso-movil.md) | Variante de teléfono del inicio de sesión. |
| Pantallas | [PantallaAgenteIA](pantalla-agente-ia.md) | Pantalla del agente consultivo de IA (CU-14) con `ChatMessage` y `ChatComposer` dentro de `AppShell`. |
| Pantallas | [PantallaAgenteIAMovil](pantalla-agente-ia-movil.md) | Variante de teléfono del agente de IA. |
| Pantallas | [PantallaCotizacion](pantalla-cotizacion.md) | Pantalla de cotización a un cliente (CU-09) con `PageHeader` y sello de estado, `Stepper`, `Table` con pie de totales, `Alert` y tarjetas de cliente y condiciones. |
| Pantallas | [PantallaCotizacionMovil](pantalla-cotizacion-movil.md) | Variante de teléfono de la cotización. |
| Pantallas | [PantallaDespachoConfirmacion](pantalla-despacho-confirmacion.md) | Confirmación de la salida física de mercadería (CU-06). |
| Pantallas | [PantallaInventario](pantalla-inventario.md) | Pantalla de consulta de disponibilidad (CU-04) que integra `AppShell`, `PageHeader`, `Stat`, `Tabs`, `SearchInput`, `Table`, `StatusBadge` y `Pagination`. |
| Pantallas | [PantallaInventarioAlmacen](pantalla-inventario-almacen.md) | Disponibilidad con el modo almacén activo en una tablet: controles de 52 a 56 px, barra lateral con etiquetas visibles aun en tablet, cifras de 17 px y celdas más altas. |
| Pantallas | [PantallaInventarioMovil](pantalla-inventario-movil.md) | Variante de teléfono de la pantalla de disponibilidad. |
| Pantallas | [PantallaInventarioTablet](pantalla-inventario-tablet.md) | Variante de tablet de la pantalla de disponibilidad. |
| Pantallas | [PantallaRecepcionCierre](pantalla-recepcion-cierre.md) | Cierre de una recepción (CU-13) con `Constancia`. |
| Pantallas | [PantallaRecepcionCierreMovil](pantalla-recepcion-cierre-movil.md) | Variante de teléfono de la constancia de cierre: el sello, el resumen antes y después y las acciones a ancho completo, sin desplazamiento horizontal. |
| Pantallas | [PantallaRecepcionOCR](pantalla-recepcion-ocr.md) | Pantalla de recepción de mercadería con documento procesado por OCR (CU-13 y CU-08). |
| Pantallas | [PantallaRecepcionOCRMovil](pantalla-recepcion-ocr-movil.md) | Variante de teléfono de la recepción con OCR. |
