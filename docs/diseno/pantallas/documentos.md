# Documentos OCR

Este documento describe las pantallas del módulo `documentos`: la lista de documentos, el procesamiento de una carga (P23) y la revisión de las líneas extraídas por el OCR (P24). Implementa los casos de uso CU-08 y CU-13 y los requerimientos RF-OCR-01 a RF-OCR-06 y RF-FUZ-05 y RF-FUZ-06.

## Principio de uso

El OCR y el Fuzzy Matching solo precargan datos. Procesar un documento no genera ventas, reservas ni movimientos de Kardex; los movimientos se generan únicamente al confirmar la operación en el flujo de origen (recepción, cotización o venta).

## Lista de documentos (`/documentos`)

Estructura:

- Encabezado de página "Documentos OCR" con la acción principal "Procesar documento".
- Pestañas por estado, tomadas de `STATUS.documento`: Pendiente, Procesado por OCR y Validado. Cada pestaña muestra el recuento de documentos.
- Tabla "Documentos procesados" con las columnas Documento, Tipo, Fecha, Estado y Acción. La acción "Revisar" abre la pantalla P24 del documento.

Estados:

- Carga: la tabla muestra esqueletos mientras se consulta la lista.
- Error: aviso "No se pudieron cargar los documentos" con la indicación de verificar la conexión.
- Sin resultados: estado vacío de la tabla para la pestaña elegida.

## Procesar documento (`/documentos/procesar`, P23)

Estructura:

- Encabezado con migas de pan "Documentos OCR > Procesar documento".
- Aviso permanente "El procesamiento no modifica el inventario".
- Tarjeta "Documento" con el selector de tipo (Guía de proveedor, Cotización de cliente u Orden de compra), la zona de carga con la opción de cámara y el archivo con su estado.
- Indicador de progreso (Stepper) mientras el archivo se procesa.

Validaciones y flujos:

- Tipos admitidos: JPG, PNG y PDF de hasta 10 MB, según la zona de carga.
- Éxito: se abre la revisión del documento recién procesado.
- Documento ilegible: aviso "Documento ilegible" con las acciones "Cargar otra foto", que reinicia la carga, y "Registrar manualmente", que lleva al flujo de origen sin datos (o a la lista si no hay origen).
- Parámetro `destino` (`cotizacion`, `venta` o `recepcion`): conserva el flujo de origen. Un valor desconocido se ignora.

## Revisión del OCR (`/documentos/:id/revision`, P24)

Estructura:

- Encabezado con el nombre del archivo, el tipo y el sello de estado del documento.
- Tabla "Líneas extraídas del documento" con las columnas Texto extraído (fuente monoespaciada), Producto del catálogo (selector buscable), Coincidencia (`MatchConfidence` compacto) y Cantidad (editable).
- Barra de acciones con el resumen de líneas y sin asociar, el motivo del bloqueo y las acciones "Descartar documento" y "Usar estos datos".

Validaciones y flujos:

- Una línea sin producto asociado impide usar los datos. El motivo se muestra en la barra de acciones con el texto "Falta asociar N líneas a un producto del catálogo.".
- "Usar estos datos" marca el documento como validado, guarda las líneas revisadas y abre el flujo de origen con el parámetro `documento` (por ejemplo, `/recepciones/nueva?documento=ID`).
- "Descartar documento" exige confirmación en un diálogo con la acción y su consecuencia. Tras confirmar, el documento se quita de la lista.
- Documento inexistente o descartado: estado vacío con la acción "Volver a documentos".

## Restricción por rol

El módulo está disponible para los roles Vendedor y Almacenero. Un Reponedor que accede por una dirección directa es redirigido al primer módulo visible para su rol.

## Diferencias respecto a los requerimientos

- Los umbrales de coincidencia (0.85 y 0.60) son valores de ejemplo, pendientes de definir en el servicio Python. Se toman de `UMBRAL_COINCIDENCIA_ALTA` (en `features/recepcion/resumen.ts`) y de los valores por omisión de `MatchConfidence`.
- Los destinos `cotizacion` y `venta` apuntan a rutas que implementan los planes 06 y 08. Hasta entonces, la dirección no corresponde a ninguna pantalla y se muestra el aviso de página no encontrada.
- El procesamiento OCR es simulado: el servicio aún no existe y devuelve las mismas líneas para cualquier guía.
