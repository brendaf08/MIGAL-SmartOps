# Dropzone

Zona de carga de documentos operativos (notas manuscritas, capturas de pedidos y guías de proveedores) para su procesamiento mediante OCR. Admite arrastrar y soltar y selección desde el equipo, con validación de tipo y tamaño.

## Uso

- `accept` (por omisión JPG, PNG y PDF: `image/jpeg`, `image/png`, `application/pdf`), `maxSize` (10 MB por omisión), `multiple`, `onDrop(files)`, `onReject(errores)`, `disabled`.
- `state` fuerza una representación (`dragover`, `processing`, `error`) cuando el proceso depende del servidor; `error` muestra un mensaje externo.
- `FileItem` lista cada archivo con nombre, tamaño, progreso y estado (`uploading`, `done`, `error`, `processed`).

## Reglas

- La zona completa es un control de teclado (etiqueta sobre un `input type="file"`) y muestra el anillo de foco; no se depende del arrastre.
- Los errores se explican: formato no admitido, tamaño excedido, documento ilegible, con la acción para reintentar.
- Durante el procesamiento se informa el estado y la espera aproximada (objetivo de 10 s, RNF-07); el resultado se presenta siempre para revisión y corrección antes de utilizarlo (RF-OCR-05, RF-OCR-06).
- La carga y el procesamiento no generan ventas, reservas ni movimientos de Kardex; el mensaje contiguo debe aclararlo.

Equivalente en Mantine: `Dropzone` de `@mantine/dropzone` con `MIME_TYPES.png`, `jpeg` y `pdf`.

## Comportamiento responsive

- `camera` agrega el botón "Tomar foto", que abre la cámara trasera del teléfono o la tablet (`capture="environment"`) para fotografiar notas, pedidos y guías; es el modo principal de carga en teléfono, donde no existe el arrastre.
- Teléfono: la zona reduce su altura y relleno; el arrastre se conserva como alternativa en equipos que lo admiten.
