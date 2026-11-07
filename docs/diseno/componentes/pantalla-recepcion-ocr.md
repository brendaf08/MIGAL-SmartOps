# PantallaRecepcionOCR

Pantalla de recepción de mercadería con documento procesado por OCR (CU-13 y CU-08). Compone `Stepper`, `Alert`, `Dropzone`/`FileItem`, `Table`, `Select`, `MatchConfidence` y `NumberInput`.

## Decisiones de diseño

- El flujo en tres pasos (cargar, revisar, confirmar) y el aviso permanente aclaran que el OCR solo precarga datos y que los movimientos de entrada se generan únicamente al confirmar (RF-COM-15, RF-COM-16).
- Cada línea muestra el texto extraído, el producto sugerido (editable), el nivel de coincidencia y la cantidad recibida (editable); las líneas con coincidencia baja muestran el error en el campo y piden validación manual.
- Las recepciones parciales se admiten: la cantidad recibida puede ser menor al pedido (RF-COM-11).
- Confirmar es la acción principal de la cabecera y abre un `ConfirmDialog` con las consecuencias.
