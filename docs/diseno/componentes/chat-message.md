# ChatMessage

Mensaje de la conversación con el agente consultivo de IA. Distingue al usuario del agente, muestra las consultas que el agente realizó al backend (tool calling) y señala las respuestas sin datos suficientes.

## Uso

- `role` (`user` o `agent`), `name`, `time`, `tools` (`[{ label, status: 'running' | 'done' }]`), `uncertain` (respuesta sin información suficiente), contenido.
- Las consultas en curso muestran un indicador giratorio; las terminadas, una marca (visibilidad del estado del sistema).

## Reglas

- El agente solo consulta y responde: nunca presenta acciones que modifiquen datos (RF-IA-04). Las acciones se realizan desde las pantallas correspondientes.
- Cuando no hay información, el agente lo dice y no inventa cantidades ni ubicaciones (RF-IA-08); se usa `uncertain` con un texto que sugiera cómo reformular.
- Las cifras y ubicaciones citadas provienen de las consultas listadas; los permisos del rol del usuario limitan lo que el agente puede responder (RF-IA-09).
- El avatar del agente usa el azul de marca con el verde de acento, distinto del avatar de personas.
