# Pantallas de cotizaciones

Este documento describe las pantallas P17 (lista de cotizaciones) y P18 (formulario de nueva cotización y modificación), y los cambios que el detalle de cotización (`CotizacionPage`) recibe para este flujo. Corresponde a los casos de uso CU-09 y al requerimiento RF-COT-05. El rol responsable es el Vendedor.

## Lista de cotizaciones (P17)

Ruta: `/cotizaciones`. Componentes: `PageHeader`, `Tabs`, `SearchInput`, `QueryTable` y `StatusBadge`.

```text
Cotizaciones                                   [Nueva cotización]
Cotizaciones emitidas a clientes y su estado.
[Todas n] [Emitidas n] [Aceptadas n] [Rechazadas n] [Vencidas n]     [Buscar por número o cliente]
Número | Cliente | Fecha | Vence | Total | Estado
```

- Las pestañas filtran por estado y muestran el recuento de cada uno. Los valores corresponden a `STATUS.cotizacion`.
- La búsqueda filtra por número de cotización o por nombre de cliente, sin distinguir mayúsculas. Una cotización sin cliente se busca como "Sin cliente".
- La columna "Vence" marca con un triángulo y el texto "vence en N días" las cotizaciones emitidas que vencen en tres días o menos.
- Una fila abre el detalle de la cotización. El botón "Nueva cotización" abre el formulario de alta.
- En teléfono, cada fila se presenta como ficha; la fecha de emisión se omite.

El estado mostrado es el vigente a la fecha de consulta: una cotización emitida cuyo vencimiento ya pasó se presenta como vencida. El vencimiento es inclusivo, de modo que la cotización sigue vigente el día en que vence.

## Formulario de cotización (P18)

Rutas: `/cotizaciones/nueva` (alta) y `/cotizaciones/:id/editar` (modificación). Componente: `FormPage` con las tarjetas "Cliente", "Productos" y "Condiciones".

```text
Nueva cotización | Editar cotización COT-0231
Cliente         [Seleccionar cliente, o continuar sin cliente]
Productos       [Cargar desde un documento]
                Producto | Cantidad | Precio | Subtotal | Quitar
                Subtotal | Descuento (S/) | Total
Condiciones     Validez en días (1 a 60, por omisión 7) | Vence el dd/mm/aaaa
                Forma de pago | Observaciones (opcional)
[Resumen: N productos, total S/ X]  [Cancelar] [Emitir cotización | Guardar cambios]
```

### Cliente

- El selector busca por nombre o documento entre los clientes registrados. La opción "Continuar sin cliente" emite la cotización sin cliente.
- Los clientes inactivos aparecen deshabilitados, con la marca "(inactivo)", y el texto de ayuda indica que no se pueden elegir.
- Es obligatorio elegir un cliente o la opción de continuar sin cliente.

### Productos

- El editor de líneas permite agregar y quitar productos. Cada línea tiene producto, cantidad y precio unitario.
- La cantidad es un entero mayor que cero y el precio es mayor que cero. El descuento se expresa en soles y no puede superar el subtotal.
- El botón "Cargar desde un documento" enlaza con `/documentos/procesar`, cuya integración corresponde al plan de recepción con OCR.
- Se requiere al menos una línea para emitir.

### Condiciones

- La validez se expresa en días naturales, entre 1 y 60. Por omisión es 7. El campo muestra la fecha de vencimiento calculada.
- La forma de pago es obligatoria. Las observaciones son opcionales y admiten hasta 500 caracteres.

### Acciones

- Emitir abre un diálogo de confirmación con la fecha de vencimiento y la consecuencia de que la emisión no modifica el stock. Al confirmar, se crea la cotización en estado Emitida y se muestra su detalle con una notificación.
- Modificar guarda los cambios directamente, sin diálogo, y conserva la fecha de emisión. El vencimiento se recalcula desde la emisión.
- Solo las cotizaciones emitidas se pueden modificar. Para cualquier otro estado, el formulario se muestra como solo lectura con un aviso informativo y la acción principal deshabilitada con su motivo.
- Cuando no hay líneas, la acción principal se deshabilita y el motivo aparece junto a ella.

## Detalle de cotización

El detalle (`CotizacionPage`) conserva su contenido y presenta las acciones según el estado:

| Estado | Acciones |
| --- | --- |
| Emitida | Modificar, Registrar rechazo, Registrar aceptación |
| Aceptada por el cliente | Convertir en venta (enlace a `/ventas/nueva?cotizacion=ID`) |
| Otros | Ninguna acción de cambio de estado |

- Registrar rechazo abre un diálogo de confirmación con la consecuencia de que la cotización queda rechazada y no puede volver a aceptarse.
- Registrar aceptación cambia el estado sin diálogo de confirmación. Véase la sección de diferencias.

## Estados

| Estado | Tono | Comportamiento en la pantalla |
| --- | --- | --- |
| Emitida | Informativo | Editable; admite aceptación y rechazo mientras esté vigente |
| Aceptada por el cliente | Marca | Solo lectura; admite conversión en venta |
| Rechazada | Peligro | Solo lectura |
| Vencida | Neutral | Solo lectura; se calcula al consultar |

## Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| Cliente | Obligatorio (cliente o sin cliente) | "Elegir un cliente o continuar sin cliente." |
| Líneas | Al menos una | "Agregar al menos un producto." |
| Producto | Obligatorio en cada línea | "Elegir un producto para cada línea." |
| Cantidad | Entero mayor que cero | "La cantidad debe ser un número entero." o "La cantidad debe ser mayor que 0." |
| Precio | Mayor que cero | "El precio debe ser mayor que 0." |
| Descuento | No negativo y no mayor que el subtotal | "El descuento no puede ser negativo." o "El descuento no puede superar el subtotal." |
| Validez | Entero entre 1 y 60 | "La validez debe estar entre 1 y 60 días." |
| Forma de pago | Obligatoria | "Elegir la forma de pago." |
| Observaciones | Hasta 500 caracteres | "Las observaciones no pueden superar 500 caracteres." |

Las reglas se definen en `cotizaciones/esquema.ts`. El campo de descuento se limita al subtotal al perder el foco. La cantidad se limita a un mínimo de 1 al perder el foco, comportamiento del componente compartido `LineasEditor`; por ello, la regla de cantidad mayor que cero se verifica en el esquema y no se alcanza desde la interfaz.

## Diferencias respecto a los requerimientos

- Aceptación sin confirmación. El plan indica un diálogo de confirmación para registrar la aceptación. Se mantiene la acción directa porque la prueba de regresión de `Cotizaciones.test.tsx` registra la aceptación con un solo clic. El rechazo sí requiere confirmación. Pendiente de resolver si la aceptación debe confirmarse.
- Identificación fiscal en el detalle. El detalle muestra la etiqueta "RUC" para todos los clientes. Una cotización a un cliente con DNI muestra el número de documento bajo esa etiqueta. Pendiente de ajustar el detalle para mostrar el tipo de documento.
- Dirección del cliente. Los clientes registrados no tienen dirección; la cotización emitida la guarda vacía.
- Nombres de estado. Los nombres de los estados de cotización están pendientes de armonizar con RF-VEN-12; se usan los valores de `STATUS.cotizacion`.

## Pruebas

Las pruebas de la pantalla están en `src/features/comercial/cotizaciones.pantallas.test.tsx`. Cubren el filtrado y la búsqueda del listado, la emisión con y sin cliente, la restricción de clientes inactivos, la validación de campos, el límite del descuento, la modificación y la solo lectura, el rechazo y la aceptación, la restricción por rol y la verificación de accesibilidad con `vitest-axe`.
