# TextInput

Campo de una línea con etiqueta visible, texto de ayuda, mensaje de error y asociación automática entre etiqueta, ayuda y error (`for`, `aria-describedby`, `aria-invalid`).

## Uso

- `label` (obligatoria), `description`, `error`, `required` (agrega asterisco y `aria-required`), `optional` (agrega "(opcional)"), `leftIcon`, `rightSection`, `size` (`sm`, `md`, `lg`); el resto de atributos pasa al `<input>`.
- Marcar como `required` o `optional` según la regla del requerimiento: cliente y datos de contacto de una venta son opcionales (RF-CLI-01, RF-CLI-06).

## Reglas

- Etiqueta siempre visible; el marcador de posición solo muestra un ejemplo de formato, no reemplaza la etiqueta.
- El mensaje de error dice qué ocurrió y cómo corregirlo ("El RUC debe tener 11 dígitos."), se muestra junto al campo y se anuncia a lectores de pantalla.
- En códigos (SKU, RUC, números de documento) y usuarios, desactivar la corrección ortográfica (`spellCheck={false}`) y el autocompletado (`autoComplete="off"`); en inicio de sesión mantener `autocomplete` de usuario y contraseña.
- Validar al salir del campo o al enviar, no mientras el usuario escribe por primera vez. La validación del cliente (Zod) complementa la del backend.
- `size="lg"` en tablets y pantallas de acceso (16 px de fuente evita el acercamiento automático en iOS).

Equivalente en Mantine: `TextInput` con `useForm` y `zodResolver`.

## Comportamiento responsive

- Teléfono: los campos miden 44 px de alto como mínimo y usan 16 px de fuente para evitar el acercamiento automático de iOS. Lo mismo aplica a `NumberInput`, `Select`, `Textarea` y `SearchInput`.
