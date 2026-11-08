# Pantalla de registro de venta

Este documento describe la pantalla para registrar una venta (P20, ruta `/ventas/nueva`), con la selección de cliente, los productos, el descuento, la modalidad de atención y la constancia de cierre. Corresponde a la etapa B del plan 08 (CU-10, RF-VEN-12, rol Vendedor).

## Estructura de la pantalla

Encabezado "Registrar venta", con migas de pan hacia Ventas. Tarjetas apiladas:

1. Cliente: selector `ClientePicker` (búsqueda por nombre o documento, "Continuar sin cliente" y registro en línea). Debajo, el origen de los productos: "Manual", "Desde una cotización aceptada" o "Desde un documento".
2. Productos: editor de líneas `LineasEditor` con producto, cantidad, precio, descuento global en soles y totales.
3. Disponibilidad: tabla de las líneas con la cantidad solicitada, el stock libre y una barra compacta `StockBar`. Una línea que supera el stock libre muestra un triángulo y el texto "Stock libre: N".
4. Atención de las líneas sin existencias: solo aparece si alguna línea no está cubierta. Ofrece tres modalidades (ver abajo).

Barra de acciones al pie `ActionBar`: resumen con el número de productos, subtotal, descuento y total; motivo del bloqueo visible; acciones "Cancelar" y "Confirmar venta".

Confirmación: `ConfirmDialog` con las consecuencias "Se registrarán N salidas en el Kardex" y, si corresponde, "Quedan M unidades pendientes". La acción "Volver" es la segura.

Constancia: sello con el estado de la venta, título "Venta V-NNNN registrada", tabla de existencia antes y después de lo atendido, nota con lo pendiente y las acciones "Ver la venta" y "Registrar otra venta".

## Modalidades de atención

Cuando una línea supera el stock libre, debe elegirse una modalidad antes de confirmar:

| Modalidad | Comportamiento |
| --- | --- |
| Atender parcialmente | Atiende lo disponible en cada línea sin cobertura y registra la diferencia como pendiente. |
| Venta pendiente por abastecimiento | Atiende las líneas cubiertas; las demás quedan pendientes, sin atención. La venta queda en estado Pendiente de abastecimiento. |
| Solicitud pendiente del cliente | Igual que la anterior, con la pendencia atribuida a la confirmación del cliente. |

La regla se implementa en `calcularAtencion` (`registro/atencion.ts`). Varias líneas del mismo producto comparten el stock disponible, de modo que la cantidad atendida nunca supera la existencia ni lo solicitado.

## Estados

| Estado | Comportamiento |
| --- | --- |
| Sin líneas | Motivo visible "Agregar al menos un producto."; confirmación bloqueada. |
| Sin cliente elegido | Motivo "Elegir el cliente o continuar sin cliente."; confirmación bloqueada. |
| Modalidad pendiente | Motivo "Elegir cómo atender las líneas sin existencias."; confirmación bloqueada. |
| Descuento inválido | Mensaje junto al descuento; confirmación bloqueada. |
| Cotización no utilizable | Aviso "La cotización no está aceptada o ya se convirtió en venta." y ausencia de líneas cargadas. |
| Confirmada | Constancia con la venta registrada. |

## Origen desde una cotización

Con `?cotizacion=ID` o al elegir una cotización del selector, se cargan las líneas, el descuento (el porcentaje de la cotización convertido a soles sobre el subtotal) y el cliente con el mismo RUC, si está registrado. Solo se ofrecen cotizaciones en estado Aceptada por el cliente que no tengan una venta asociada.

## Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| Cliente | Obligatorio (puede ser "Continuar sin cliente"). | "Elegir el cliente o continuar sin cliente." |
| Líneas | Al menos una; cada una con producto y cantidad de al menos 1. | "Agregar al menos un producto.", "Seleccionar un producto en cada línea.", "La cantidad de cada línea es de al menos 1." |
| Descuento | No negativo y no mayor que el subtotal. | "El descuento no puede ser negativo ni mayor que el subtotal." |
| Modalidad | Obligatoria si alguna línea no está cubierta. | "Elegir cómo atender las líneas sin existencias." |

## Registro de la venta

La venta se agrega en memoria con el siguiente código `V-NNNN`, la fecha del día, el cliente (si se eligió), la cotización de origen (si corresponde), el estado y la entrega (Completa o Parcial) que resultan de la atención. Una venta vinculada a una cotización hace que esta deje de ofrecerse en el selector.

## Diferencias respecto a los requerimientos y decisiones adoptadas

- Las cantidades de la constancia se expresan como stock libre antes y después de lo atendido. Las salidas del Kardex no se simulan en esta etapa porque el inventario y sus movimientos son datos de solo lectura para este módulo; la integración corresponde al plan 04.
- La disponibilidad por línea se presenta en una tabla aparte y no bajo cada línea del editor, porque `LineasEditor` no ofrece un espacio por línea para contenido adicional.
- La modalidad "Solicitud pendiente del cliente" se distingue solo en su etiqueta; ambas modalidades pendientes producen el mismo estado, como indica el plan.
- El motivo de bloqueo muestra un solo mensaje, en el orden: líneas, cotización, cliente, modalidad y descuento.
- La opción "Desde un documento" enlaza a `/documentos/procesar` sin modificar las líneas; la carga desde el OCR corresponde al plan 10.

## Pruebas

El archivo `registro.pantallas.test.tsx` cubre el cálculo de atención con sus límites, el registro con stock suficiente, la modalidad obligatoria y la atención parcial, el descuento inválido, la venta sin cliente, el bloqueo sin líneas, la carga de una cotización aceptada, la cotización no utilizable, la restricción por rol y el análisis de accesibilidad del formulario.
