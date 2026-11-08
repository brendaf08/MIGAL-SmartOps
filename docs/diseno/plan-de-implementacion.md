# Plan de implementación del sistema de diseño

Este documento traduce las últimas decisiones del sistema de diseño (diseño responsive, crítica de las pantallas de almacén, constancia de cierre, ubicaciones de stock y modo almacén) en fases de implementación para `frontend/`. Cada fase corresponde a un pull request y se ejecuta en una sesión de trabajo independiente de Claude Code.

## Punto de partida

El frontend ya existe y reutiliza el vocabulario del sistema. Antes de cada fase se comprueba la brecha real contra la especificación, sin reescribir lo que ya cumple.

| Elemento | Estado observado en `frontend/src` |
| --- | --- |
| Tema, tokens y modo claro u oscuro | Implementado (`theme/`). Los tokens y `theme.ts` se actualizaron con el diseño vigente. |
| `AppShell` | Existe con `Burger`, barra lateral en `sm` y `md`. Falta verificar el panel deslizable, el cierre con Escape y el foco, el botón de modo almacén y la cabecera compacta. |
| `StatusBadge` | Existe con la variante de sello. |
| `DataTable`, `QueryTable`, `Pagination` | Existen. Falta el modo de fichas en teléfono (`primary`, `phone`, `phoneOnly`, `block`), `changedKey` y la paginación compacta. |
| `StockSummary`, `StockBar`, `MatchConfidence` | Existen. Falta la cinta por empaque, las marcas de mínimo y reposición, la variante compacta y `MatchConfidence` usa la fuente de cifras en un dato corriente. |
| `Dropzone` | Existe. Falta la captura con cámara. |
| Pantallas de recepción, disponibilidad y despacho | Existen (`features/recepcion`, `features/inventario`, `features/despacho`). Falta la lógica de diferencias, el bloqueo con motivo, las ubicaciones y la constancia. |
| Componentes nuevos | `ActionBar`, `StockLocations` y `Constancia` no existen. |
| Rama de trabajo | `feat/pantallas-pc3` acumula decenas de commits sin integrar a `main`. |

## Fuentes de consulta

- Especificación visual y de comportamiento: [componentes](componentes/README.md) y [sistema de diseño](sistema-de-diseno.md).
- Referencia ejecutable: [prototipo](prototipo/README.md), con una variante de teléfono, tablet y modo almacén de las pantallas clave.
- Reglas del proyecto: [frontend/CLAUDE.md](../../frontend/CLAUDE.md) y [ADR-002](../adr/adr-002-sistema-de-diseno.md).

## Fases

Las fases F2 y F3 son independientes entre sí y pueden ejecutarse en paralelo, en sesiones separadas con su propio árbol de trabajo. Las demás se ejecutan en orden.

### F0. Tipografía y tokens

- Comprobar `tokens.css` contra [tokens.json](tokens.json) y registrar los tamaños `control-xl`, `bp-phone`, `header-compact`, `drawer-width` y `touch-min`.
- Aplicar los papeles tipográficos: Bricolage Grotesque solo en el título de página, Instrument Sans en el resto, Barlow Semi Condensed solo en cifras grandes y sello. Ajustar `cssVariablesResolver.ts`, `global.css` y los títulos que usan la fuente de encabezados sin ser el título de página.
- Retirar `ff="var(--font-display)"` de los datos corrientes (por ejemplo, el porcentaje de `MatchConfidence`) y de las celdas de tabla.
- Criterio de aceptación: ningún dato de tabla, etiqueta ni botón usa fuentes de acento; las pruebas existentes siguen pasando.

### F1. Responsive base

- `AppShell`: panel deslizable en teléfono con botón de menú (`aria-expanded`, `aria-controls`), velo, cierre con Escape y al elegir un ítem, foco devuelto al botón, cabecera de 56 px, áreas seguras y barra de íconos en tablet con `title`.
- `DataTable`: modo de fichas en teléfono con las propiedades `primary`, `phone`, `phoneOnly` y `block`; `changedKey`; desplazamiento interno en tablet.
- `Pagination` compacta, `Modal` como hoja inferior, `Stepper` vertical, `PageHeader` con acciones a ancho completo, `Columns` apilable y campos de 16 px en teléfono.
- Objetivos táctiles: 44 px en teléfono, 48 px en tablet, enlaces de las migas con área de 44 px.
- Criterio de aceptación: sin desplazamiento horizontal de la página entre 360 y 1920 px; variantes del prototipo reproducidas a 390, 768 y 1280 px en ambos temas; prueba `axe` por pantalla.

### F2. Flujo de recepción

- Crear `ActionBar` en `components/layout` o `components/ui` según corresponda.
- `RecepcionOCRPage`: columna de diferencia ("Completa", "Faltan N", "Sobran N", "Sin asociar"), botón "Igual al pedido", acción "Igualar coincidencias altas", resumen por estado, bloqueo de "Confirmar recepción" mientras exista una línea sin asociar y motivo visible. Mover la confirmación a `ActionBar`.
- Extraer el cálculo (completas, parciales, sobrantes y sin asociar) a una función pura con pruebas unitarias.
- `MatchConfidence` con variante `compact`; `Dropzone` con captura de cámara.
- Criterio de aceptación: la tabla de líneas se ve completa a 1280 px sin desplazamiento; una línea sin producto bloquea la confirmación y lo explica; una recepción parcial se permite y se resalta.

### F3. Disponibilidad con identidad

- Crear `StockLocations`; extender `StockBar` con `compact`, `empaque`, `minimo` y `reposicion`.
- `DisponibilidadPage`: columnas "Reparto" (escritorio) y "Dónde está" (fichas de teléfono); pestaña "En alerta" con contador de tono de atención; `ProductoDetalle` con `StockSummary` y la cinta por empaque.
- El paso de la cinta se deriva de la presentación del producto; si el dato no existe en la API, usar décimas del total y registrar el campo como pendiente.
- Criterio de aceptación: en teléfono cada ficha muestra la cantidad libre y las tres ubicaciones; en escritorio la columna de reparto reemplaza a Físico y Reservado, que se consultan en el detalle.

### F4. Constancia de cierre

- Crear `Constancia` con el sello estampado una sola vez, el resumen antes y después en doble línea, la nota y las acciones; vibración de 30 ms solo en dispositivos que la admiten y sin movimiento reducido.
- Usarla al confirmar una recepción y una salida (`DespachoPage`), con el cambio visible de dato en la tabla de origen.
- Criterio de aceptación: la constancia se monta solo como resultado de la acción, tiene `role="status"` y aparece fija con movimiento reducido.

### F5. Modo almacén

- `AppShell`: propiedades `warehouse` y `onToggleWarehouse`, botón con `aria-pressed`, persistencia por equipo en el almacenamiento local (clave `migal-warehouse-mode`) y aplicación de los tamaños `control-xl` y de la barra lateral con etiquetas en tablet.
- Criterio de aceptación: con el modo activo, los controles miden 52 a 56 px y la navegación muestra etiquetas en tablet; el interruptor no aparece en teléfono.

### F6. Calidad y catálogo

- Incorporar Storybook con una historia por componente y una prueba de accesibilidad con `axe` por pantalla.
- Capturas de regresión visual a 390, 768 y 1280 px en ambos temas (opcional, con Playwright).
- Actualizar `frontend/README.md` y registrar en un ADR el paso de la fuente de verdad del diseño al código.

## Procedimiento por fase en Claude Code

1. Crear una rama desde la rama de trabajo vigente (`feat/<fase>`), o un árbol de trabajo propio si se ejecutan fases en paralelo.
2. Leer `frontend/CLAUDE.md`, esta fase y los documentos de componentes que cita. Auditar la brecha real antes de editar.
3. Planificar los cambios (modo plan) cuando la fase toque varias capas.
4. Escribir las pruebas primero: roles, nombres accesibles, teclado y la regla de negocio de la fase.
5. Implementar y comparar con la vista previa del prototipo a 390, 768 y 1280 px, en tema claro y oscuro.
6. Ejecutar `npm run lint`, `npm run typecheck`, `npm test` y `npm run build`.
7. Revisar con `/code-review` y abrir el pull request con la plantilla, enlazando la tarea de Notion y las referencias a RF y RNF.
8. Commits atómicos con el formato de CONTRIBUTING.md.

## Pendientes que condicionan las fases

- F2: confirmar con el negocio que solo se bloquea por líneas sin asociar y que las recepciones parciales se admiten.
- F3: confirmar si Físico y Reservado deben mantenerse como columnas (RF-KAR-02, RF-PRO-05) y de dónde proviene la unidad de empaque.
- F5: definir quién activa el modo almacén y si se guarda en el dispositivo o en el servidor.
- RNF-05: incorporar los teléfonos al requerimiento.
- Integrar `feat/pantallas-pc3` a `main` antes de abrir las ramas de fase, o ramificar desde ella y abrir el pull request contra `main` al terminar.
