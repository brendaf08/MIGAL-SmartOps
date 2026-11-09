# Recepciones

Este documento describe las pantallas del módulo `recepciones`: el historial de recepciones y el flujo de nueva recepción desde una orden de compra (P27), con documento opcional procesado por OCR. Implementa el caso de uso CU-13 y los requerimientos RF-COM-06 a RF-COM-16.

## Principio de uso

La recepción actualiza la orden de compra y genera los movimientos de entrada al Kardex solo al confirmar. El OCR solo precarga las cantidades que el usuario revisa. Una orden admite varias recepciones hasta completarse.

## Historial de recepciones (`/recepciones`)

Estructura:

- Encabezado "Recepciones" con la acción "Nueva recepción".
- Tabla "Recepciones" con las columnas Recepción, Orden de compra, Proveedor, Almacén, Fecha, Estado y Estado de la orden (`StatusBadge` de la orden de compra).

## Nueva recepción (`/recepciones/nueva`, P27)

El flujo consta de tres pasos, indicados con un `Stepper`, y una constancia de cierre.

### Paso 1: orden de compra

- Tabla "Órdenes de compra para recibir" con las órdenes en estado Pendiente o Recepción parcial. Muestra el proveedor, el número de productos, lo que falta recibir y el estado.
- Acción "Elegir" por orden. La orden elegida muestra la tabla "Productos esperados" con el pedido, lo recibido y lo que falta por recibir de cada producto.
- Con el parámetro `oc=ID` la orden llega preseleccionada.
- Barra de acciones: "Cancelar" y "Continuar". Sin orden elegida, "Continuar" está deshabilitado y la barra explica el motivo.

### Paso 2: documento (opcional)

- Zona de carga de la guía con la opción de cámara y el estado del archivo.
- Con el parámetro `documento=ID`, las líneas de una revisión del OCR ya validada (P24) llegan precargadas en este paso.
- Con una guía procesada, la tabla "Líneas extraídas" muestra las líneas del OCR con la misma tabla de revisión del OCR (`LineasOcrTabla`).
- Barra de acciones: "Atrás", "Registrar sin documento" y "Usar datos de la guía". Esta última exige que todas las líneas tengan producto asociado.

### Paso 3: conteo y confirmación

- Tabla "Conteo de la recepción" con las columnas Texto, Producto, Coincidencia, Pendiente, Recibido (editable) y Diferencia. La acción "Igual al pedido" iguala lo recibido al pendiente de una línea, y "Igualar coincidencias altas" aplica lo mismo a las líneas con coincidencia alta.
- Cada línea de la orden aparece una vez. Las líneas del documento que no pertenecen a la orden se muestran como sobrantes.
- Un sobrante exige la nota "Nota del sobrante"; sin ella, la barra de acciones explica el bloqueo.
- "Confirmar recepción" abre un diálogo con las consecuencias: los movimientos de entrada en el Kardex del almacén y, si corresponde, la recepción parcial.

### Constancia

- Sello con el estado de la recepción, título "Recepción R-XXXX registrada" y la existencia antes y después de cada producto.
- Nota: "Recepción parcial: quedan por recibir N unidades en M líneas." si la orden sigue en recepción parcial; "Orden completada." en caso contrario.
- Acción "Registrar otra recepción", visible solo mientras la orden sea parcial. Reinicia el flujo con la misma orden.

## Estados

- Orden de compra: Pendiente, Recepción parcial o Completada. La orden pasa a Completada cuando todas sus líneas alcanzan la cantidad pedida.
- Recepción: Validada tras confirmar.

## Validaciones

- Cada línea debe tener producto asociado antes de confirmar.
- Un sobrante exige una nota antes de confirmar.
- Una orden completada no admite nuevas recepciones; el servicio lo informa con un aviso.

## Restricción por rol

El módulo está disponible para el rol Almacenero.

## Diferencias respecto a los requerimientos

- El almacén de recepción es fijo (Almacén 1) en esta versión; la API indicará el almacén de cada orden.
- Los sobrantes que no pertenecen a la orden registran entradas en el Kardex, pero no modifican la orden de compra.
- La cantidad recibida no tiene tope: un valor superior a lo pendiente se trata como sobrante, con nota obligatoria, en lugar de bloquear la captura.
