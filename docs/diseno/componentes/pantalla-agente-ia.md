# PantallaAgenteIA

Pantalla del agente consultivo de IA (CU-14) con `ChatMessage` y `ChatComposer` dentro de `AppShell`.

## Decisiones de diseño

- Las consultas al backend aparecen como etiquetas de estado en cada respuesta (en curso y terminadas).
- El rol del usuario se muestra en la cabecera para dejar claro que las respuestas respetan sus permisos (RF-IA-09).
- El agente no ofrece acciones de modificación; las respuestas sin datos se marcan como tales.
