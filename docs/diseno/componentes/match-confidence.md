# MatchConfidence

Indicador del grado de similitud entre un término extraído por OCR (o escrito por el usuario) y un producto del catálogo maestro. Comunica si la asociación puede aceptarse o requiere validación manual (RF-FUZ-03 a RF-FUZ-05).

## Uso

- `score` (0 a 1) y `thresholds` (`{ high, low }`; por omisión 0.85 y 0.60).
- Niveles: coincidencia alta (verde), revisar coincidencia (ámbar) y sin coincidencia (rojo, con la indicación de validación manual).

## Variante compacta

- `compact` omite el texto de validación manual y deja la insignia, el porcentaje y la barra, para celdas de tabla; la razón del bloqueo se muestra entonces en la columna de diferencia y en `ActionBar`.

## Reglas

- Los umbrales son valores de ejemplo; la definición de los umbrales corresponde al servicio Python y debe sincronizarse con esta interfaz.
- Aun con coincidencia alta, el usuario confirma la asociación (RF-FUZ-06); el indicador sugiere, no decide.
- La barra y el porcentaje se complementan con el texto del nivel (no solo color); expone `role="meter"`.
