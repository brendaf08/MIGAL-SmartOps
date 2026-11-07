# ChatComposer

Zona de entrada del chat: campo de texto, botón de envío y sugerencias de consultas frecuentes.

## Uso

- `value` / `onChange` (controlado) o sin props (interno), `onSend(texto)`, `suggestions` (lista de textos), `disabled`, `placeholder`.
- Enter envía; Mayús+Enter inserta un salto de línea. El botón de envío está deshabilitado mientras el campo esté vacío.

## Reglas

- La nota inferior recuerda el alcance del agente (solo consulta, según el rol).
- Mientras el agente responde, el campo se deshabilita y la conversación muestra la consulta en curso.
- Las sugerencias se limitan a tres y se adaptan al rol.
