# FormPage

Estructura de un formulario de página completa: encabezado de página, tarjetas apiladas con los campos y una barra de acciones al pie. Es el patrón de registro y edición de usuarios, productos, proveedores, cotizaciones, ventas y órdenes de compra.

## Uso

- `title`, `description` y `breadcrumbs` alimentan a `PageHeader`.
- `children`: una o más `Card` con los campos agrupados por tema.
- `summary` y `reason`: se muestran en `ActionBar`; `reason` explica por qué no se puede enviar.
- `submitLabel`, `submitting`, `canSubmit`, `onSubmit` y `onCancel`.
- `onSubmit` recibe el evento del formulario; con `form.onSubmit(...)` de Mantine la validación del esquema Zod se ejecuta antes de invocar la función de guardado.

## Reglas

- Una sola acción principal con verbo en infinitivo ("Registrar usuario"); la acción secundaria es siempre "Cancelar".
- Al enviar con errores, el foco pasa al primer campo inválido y cada error aparece junto a su campo.
- Una acción principal deshabilitada siempre va acompañada de `reason` visible.
- El formulario usa `noValidate`: la validación es la del esquema, no la del navegador.

## Comportamiento responsive

- Teléfono: tarjetas a ancho completo y barra de acciones fija al pie con el área segura.
- Tablet y escritorio: tarjetas apiladas con el ancho de contenido y barra al pie.

Equivalente en Mantine: `form`, `Stack` y el componente propio `ActionBar`.
