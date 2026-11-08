# Sistema de diseño de MIGAL SmartOps

Este documento reúne los fundamentos visuales, los principios y las reglas de uso del sistema de diseño. Los valores normativos se encuentran en [tokens.json](tokens.json), cada componente se documenta en [componentes/](componentes/README.md) y el prototipo de referencia se encuentra en [prototipo/](prototipo/README.md).

MIGAL SmartOps es la plataforma web de gestión operativa e inventario de la Ferretería Progresol Migal. Este sistema de diseño define los tokens, los componentes y las pantallas de referencia del frontend (React, Vite, TypeScript y Mantine, según ADR-001), de modo que todas las vistas se construyan con las mismas piezas y comuniquen el estado del negocio de forma consistente.

## Contexto de uso

- Usuarios internos con cuatro roles: Administrador, Vendedor, Almacenero y Reponedor. La interfaz muestra solo los módulos que corresponden al rol; la autorización real la aplica el backend.
- Equipos de uso: computadoras All-in-One de escritorio, tablets y teléfonos en la tienda y los dos almacenes (RNF-05 contempla escritorio y tablets; el teléfono se incorpora por decisión del equipo). Los flujos de almacén (recepción, despacho, movimientos) se operan de pie y con toque, y la captura de documentos con la cámara; los flujos comerciales y administrativos, con teclado y ratón.
- Operaciones frecuentes: consultar disponibilidad, registrar cotizaciones y ventas, confirmar recepciones, confirmar salidas, procesar documentos con OCR y consultar al agente de IA.
- Prioridades de experiencia: velocidad de consulta (objetivo de 3 s), prevención de errores en operaciones que afectan el inventario y claridad sobre qué es físico, qué está reservado y qué está libre.

## Principios de diseño

1. **Visibilidad del estado.** Cada entidad con ciclo de vida muestra su estado con `StatusBadge` (texto, ícono y tono). Toda espera visible usa `Spinner`, `Skeleton` o `loading` en el botón.
2. **Prevención de errores.** Las acciones irreversibles (cancelar una venta, confirmar una salida o una recepción, desactivar un proveedor) pasan por `ConfirmDialog`, que enumera las consecuencias y deja el foco en la opción segura.
3. **Control del usuario.** Todo dato precargado por OCR o Fuzzy Matching se presenta para revisión y corrección antes de confirmarse. El sistema sugiere; el usuario decide.
4. **Consistencia.** Un concepto, un componente, un ícono y una palabra. Las excepciones se documentan aquí.
5. **Accesibilidad como base.** Contraste WCAG AA verificado en ambos temas, operación completa por teclado, objetivos táctiles de 44 px o más en tablets y estados nunca expresados solo con color.
6. **Estructura, presentación y comportamiento separados.** Los valores visuales viven en tokens; los estilos, en CSS Modules y en el tema de Mantine; el comportamiento, en hooks. Ningún componente declara colores literales.

## Rasgos propios

El sistema toma su lenguaje visual del lugar donde se usa: el mostrador, el estante y el almacén de una ferretería. Estos rasgos distinguen a MIGAL SmartOps de un panel genérico y se mantienen en todos los componentes nuevos.

- **Señalización industrial para los estados.** Correcto = cuadrado con marca, atención = triángulo, crítico = octógono, información = círculo. El estado se lee por la forma antes que por el color (`StatusBadge`, `Alert`, `Stat`).
- **Ficha de existencias.** En `StockSummary` la cantidad libre, la que se puede vender, es la cifra dominante; el stock físico y el reservado acompañan en un nivel menor.
- **Cinta por empaque.** `StockBar` incorpora una regla graduada cuyas marcas caen en múltiplos de la unidad de venta (cada 100 bolsas, cada millar, cada varilla) y dos marcas con forma de marcador de estante para el stock mínimo y el punto de reposición.
- **Dónde está.** `StockLocations` muestra la tienda y los dos almacenes como celdas de un plano esquemático, de ancho proporcional a su existencia y con el vacío dibujado con borde punteado. En teléfono es la respuesta a "¿hay y dónde?" dentro de cada ficha.
- **Constancia de cierre.** `Constancia` cierra las operaciones de inventario con el sello estampado una vez, la existencia antes y después de cada producto en doble línea contable y el siguiente paso; en teléfono agrega una vibración de 30 ms.
- **Cambio visible.** La fila afectada por una acción pasa de `brand-soft` a transparente durante 1.2 s (`changedKey` en `Table`); nada se anima en la carga.
- **Barra de confirmación.** `ActionBar` fija al pie reúne el resumen por estado, el motivo del bloqueo y las acciones; la acción principal solo se deshabilita junto con una explicación visible.
- **Etiqueta de estante.** El ítem activo de la navegación se marca con un cuadrado verde de contorno azul, como la etiqueta de un estante; es el único uso del verde de marca sobre fondo claro.
- **Rotulado de códigos.** SKU, documentos y RUC se escriben en monoespaciada (`t-sku`), como en un rótulo de bodega.
- **Plano y tinta.** El concepto central es el libro de existencias dibujado sobre papel de plano: en el tema claro, fondo azul pálido (`page`) con tarjetas casi blancas de matiz azul (`surface`), texto en tinta azul marino (`ink`), reglas de tinta translúcida (`line`) y una barra lateral en el azul profundo de la marca; en el tema oscuro, grises pizarra tenues en lugar de negros. Los colores del logo estructuran la interfaz sin saturarla: azul Progresol para barra lateral y acciones, verde neón solo como marcador y sello de marca.
- **Cifras de bodega.** Solo las cifras dominantes (el stock libre de una ficha, los indicadores) y el sello de documento usan una fuente condensada, como los números de las bodegas y las etiquetas de estante.
- **Doble línea contable.** Los totales de una tabla (`footer`) se cierran con doble línea superior y el total en peso 700, como en un libro contable.
- **Sello de documento.** El estado de una cotización, venta o guía se presenta en su encabezado como sello: doble contorno y fuente de rotulado (`StatusBadge` con `variant="sello"`).

## Contenido y tono

- Idioma: español. Registro neutro y profesional, orientado a la tarea; sin exclamaciones, sin diminutivos y sin humor.
- Formas verbales: los botones y las instrucciones usan infinitivo ("Registrar venta", "Confirmar recepción", "Revisar los datos extraídos"). Los mensajes describen hechos en indicativo ("Se reservaron 40 unidades en Almacén 1").
- Capitalización: tipo oración en títulos, botones, etiquetas y pestañas. Mayúsculas solo en siglas (SKU, RUC, OCR); los rótulos y encabezados de columna no se escriben en mayúsculas ni con espaciado amplio.
- Vocabulario del dominio, siempre igual: Físico, Reservado y Libre (stock); Entrada y Salida (Kardex); Despacho (salida física completa); Recepción (entrada de mercadería); Orden de compra; Cotización; Venta; Cliente; Proveedor. No alternar con sinónimos ("existencia" y "stock" se usan según este glosario: Stock en rótulos de estado, existencias en descripciones).
- Errores: indicar qué ocurrió y cómo corregirlo, junto al campo afectado. Ejemplo: "El RUC debe tener 11 dígitos." No usar mensajes técnicos ni culpar al usuario.
- Confirmaciones: el botón repite el verbo de la acción ("Cancelar venta"), nunca "Aceptar" ni "Sí".
- Formatos (configuración regional es-PE): importes `S/ 1,250.00` (`formatCurrency`), cantidades `1,240` (`formatNumber`), fechas `04/11/2026`, hora de 24 horas `10:42`, unidades de medida abreviadas tras la cantidad (`bolsas`, `m3`, `unid.`, `kg`).
- Códigos: SKU, números de documento y RUC se escriben en `t-sku` (monoespaciada).

## Color

- **Marca.** Azul Progresol `brand-7` (#00339F) y verde neón `accent` (#17FF44), tomados del logo. El azul es el color de acción: botón primario, enlaces, selección y pestaña activa (`brand` en ambos temas). El verde es un acento de marca, no un color de interfaz general.
- **Verde de acento.** Permitido únicamente con contorno `on-accent` o sobre azul de marca: marcador del ítem activo de la navegación, botón `accent`, marcas del agente de IA. Nunca como texto ni como relleno sin contorno sobre fondos claros (contraste 1.4:1 con el blanco). Texto sobre el verde: `on-accent`.
- **Superficies.** Tres niveles y un hueco. `page` para el fondo de contenido, `surface` para tarjetas, tablas y cabecera, `nav-bg` para la barra lateral (azul profundo de la marca en claro, gris pizarra más profundo en oscuro), `surface-raised` con `shadow-lg` solo para elementos flotantes (modales, menús), y `surface-sunken` para cabeceras de tabla y zonas de arrastre. `surface-hover` para filas con cursor encima. En el tema oscuro cada nivel sube pocos puntos de luminosidad, sin cambiar de tono.
- **Campos.** `field-bg`, más oscuro que la superficie que los contiene, indica que reciben contenido; al enfocarse el fondo pasa a `surface`.
- **Texto.** `ink` para texto principal, `ink-muted` para secundario y encabezados de tabla, `ink-subtle` para marcadores de posición y metadatos. El texto sobre un relleno `brand` usa `on-brand`; sobre `brand-soft`, `brand-soft-ink`; sobre `nav-bg`, `nav-ink` y `nav-ink-muted`.
- **Bordes.** `line` (tinta translúcida al 10 %) para divisiones decorativas y bordes de tarjeta: se funde con cualquier superficie y solo se percibe al buscarla. `line-strong` para todo control interactivo (3:1 con la superficie).
- **Estado.** Cuatro tonos con tres tokens cada uno: relleno de ícono o borde (`success`, `warning`, `danger`, `info`), fondo suave (`*-soft`) y texto (`*-ink`). Siempre acompañados de palabra e ícono; nunca el color solo.
  - `success`: completado, existencia suficiente. `warning`: pendiente de acción externa o stock en el mínimo. `danger`: error, cancelación o stock crítico. `info`: proceso neutral.
  - Verde y rojo no se distinguen solo por tono: cada uno lleva ícono propio (marca y círculo con signo) y texto.
- **Temas.** Claro (predeterminado) y oscuro tenue. El oscuro usa grises pizarra con un matiz azul (`page` #2B303A, `surface` #353B47) en lugar de negros, con el mismo contraste de texto verificado y los acentos de marca aclarados. El tema se aplica con `data-theme="light|dark"` en el elemento raíz; los valores de cada tema están en `tokens.json`.
- **Selector de modo.** `AppShell` muestra en la cabecera un botón (sol o luna) cuando recibe `onToggleTheme`; el botón cambia de etiqueta según el modo ("Cambiar a modo oscuro", "Cambiar a modo claro"). La preferencia inicial proviene de `prefers-color-scheme` y la elección del usuario se conserva en `localStorage`.
- **Contraste.** Todos los pares de texto documentados cumplen 4.5:1 como mínimo (la mayoría, 7:1) y los bordes de control, 3:1, en ambos temas. Excepción deliberada: el texto de controles deshabilitados.

## Tipografía

- Familias y papeles: Instrument Sans para todo el texto, los controles, las tablas, los encabezados de columna y los títulos de sección (`t-h2` a `t-h4`, `t-th`, `t-overline`, `t-data`); Bricolage Grotesque únicamente en el título de página (`t-h1`); Barlow Semi Condensed únicamente en las cifras grandes (`t-stat`, `t-numeral`) y en el sello de documento; IBM Plex Mono para códigos (`t-sku`). Se cargan desde Google Fonts en este sistema; en la aplicación se recomienda autoalojarlas con `@fontsource-variable/bricolage-grotesque`, `@fontsource-variable/instrument-sans`, `@fontsource/barlow-semi-condensed` y `@fontsource/ibm-plex-mono` para no depender de la red en la ferretería.
- Las fuentes de acento no se usan en datos corrientes, etiquetas, botones ni encabezados de columna: una interfaz de trabajo debe sentirse familiar, y las cifras de una tabla se leen mejor en la misma fuente que el resto del texto, con cifras tabulares y alineadas a la derecha.
- Escala: `t-h1` 28/36, `t-h2` 22/30, `t-h3` 18/26, `t-h4` 16/24; texto `t-body-lg` 16/24, `t-body` 14/20 (base), `t-body-sm` 13/18, `t-caption` 12/16 (mínimo permitido); controles `t-button-sm|md|lg`; datos `t-stat` 36/40, `t-numeral` 28/32, `t-data` 15/20, `t-th` 12/16, `t-sku` 13/18.
- Reglas: un solo `t-h1` por vista; `t-body` en escritorio y `t-body-lg` en tablets y campos `lg`; cantidades, precios y totales con cifras tabulares y alineación a la derecha; evitar más de tres tamaños distintos en una misma pantalla.
- Jerarquía: peso y color trabajan junto con el tamaño. Un dato se compone en tres niveles: valor (600 a 700, `ink`), etiqueta (500 a 600, `ink-muted`) y metadato (400, `ink-subtle`). La escala sigue una razón cercana a 1.2 sobre la base de 14 px.
- Cada vista nombra un único foco y lo hace ganar: en disponibilidad, la tabla; en recepción con OCR, las líneas por revisar; en una venta, el estado y la acción principal; en `StockSummary`, la cantidad libre. Lo demás se atenúa con color y espacio antes que con líneas.
- Densidad: relleno de celdas de 12 px en escritorio y 16 px en pantallas táctiles; relleno de tarjetas de 20 px; espacio entre grupos de 24 px, mayor que el espacio dentro de un grupo (8 a 12 px), para que la agrupación se perciba sin separadores.
- Títulos con `text-wrap: balance` y textos largos con `text-wrap: pretty`.

## Espaciado, radios y elevación

- Escala de 4 px (`space-1` a `space-12`). Entre campos de un formulario, `space-4`; entre tarjetas, `space-6`; margen de contenido en escritorio, `space-8`. El espaciado pertenece al contenedor (`Stack`), no a cada hijo.
- Radios: `radius-md` (6 px) en botones y campos; `radius-lg` (10 px) en tarjetas, zonas de arrastre y avisos; `radius-xl` (16 px) en modales y burbujas; `radius-sm` en insignias; `radius-full` en avatares y contadores.
- Profundidad: estrategia de bordes, no de sombras. Tarjetas, tablas, campos, cabecera y barra lateral se definen con `line`; la sombra se reserva para lo que flota sobre el contenido: `shadow-md` en menús y notificaciones, `shadow-lg` en modales. `shadow-xs` y `shadow-sm` quedan disponibles para casos puntuales y no se usan en los componentes base.
- Radios concéntricos: el radio de un contenedor es el del elemento interior más su relleno; un hijo que toca el borde comparte el radio del padre. Un radio grande no se aplica a elementos pequeños.
- Alturas de control: `control-sm` 32 px (filtros y tablas densas), `control-md` 40 px (escritorio), `control-lg` 48 px (tablets y flujos de almacén).

## Foco, estados e interacción

- Foco por teclado: anillo sólido de 2 px con separación de 2 px (`shadow-focus`); azul de marca (`focus`) en ambos temas, más oscuro en el claro y más claro en el oscuro. Nunca se elimina sin sustituto.
- Estados de control: reposo, cursor encima, presionado, foco, deshabilitado y carga (`loading` conserva el ancho y bloquea clics). Un control deshabilitado que no sea obvio se explica con un texto visible.
- Movimiento: transiciones de 120 a 200 ms con curva de salida `cubic-bezier(0.23, 1, 0.32, 1)` (nunca de entrada) y solo sobre propiedades concretas. Los botones reducen su escala a 0.97 al presionarse; modales y notificaciones aparecen desde `scale(0.95)` o 8 px de desplazamiento con opacidad. Las acciones repetidas con alta frecuencia (atajos, búsqueda) no se animan. Con `prefers-reduced-motion` desaparecen los desplazamientos.
- Objetivos táctiles: en dispositivos con puntero táctil (`pointer: coarse`) todo control alcanza 44 px de alto como mínimo, aunque su tamaño visual sea `sm` o `md`.
- Responsive: ver la sección Diseño responsive.

## Diseño responsive

Un solo código y un solo conjunto de componentes sirven a tres familias de dispositivos. No existe una versión móvil separada: cada componente recompone su disposición según el ancho disponible.

| Dispositivo | Ancho | Referencias | Uso previsto |
| --- | --- | --- | --- |
| Teléfono | Menos de 768 px (`bp-tablet`) | 360, 390 y 430 px | Consulta rápida de disponibilidad, captura de documentos con la cámara, confirmación de recepciones y despachos y consulta al agente de IA. |
| Tablet | 768 a 1023 px | 768 y 820 px, vertical y horizontal | Flujos de almacén y atención en mostrador, operados con toque. |
| Escritorio | 1024 px o más (`bp-desktop`, `bp-wide`) | 1280 y 1920 px | Gestión comercial, abastecimiento y administración, con teclado y ratón. |

Principios:

- **Tarea primero.** En teléfono se prioriza la tarea del momento: lo secundario se omite (`phone: false` en columnas de tabla) o se traslada al detalle.
- **Zona del pulgar.** Las acciones principales se ubican al pie o a ancho completo; las entradas de mensajes quedan fijas al pie.
- **Sin desplazamiento horizontal de la página.** Solo las tablas de comparación (`stack` desactivado) se desplazan dentro de su contenedor.
- **Objetivos táctiles.** 44 px como mínimo en teléfono y en cualquier pantalla táctil, 48 px en tablet y 52 a 56 px en el modo almacén; campos con 16 px de fuente para evitar el acercamiento automático de iOS; enlaces de las migas con objetivo de 44 px.
- **Cámara como entrada.** `Dropzone` con `camera` abre la cámara trasera para fotografiar notas, pedidos y guías de proveedores.
- **Áreas seguras y altura dinámica.** Declarar `viewport-fit=cover` y usar `env(safe-area-inset-*)` y unidades `dvh` en superficies que ocupan toda la pantalla (panel, hoja inferior).
- **Zoom y orientación.** La interfaz admite el zoom del navegador al 200 % sin desplazamiento horizontal y funciona en vertical y horizontal.
- **Sin dependencia del cursor.** Ninguna función requiere pasar el cursor por encima; el texto de ayuda de los íconos también está disponible por etiqueta accesible.

Comportamiento por componente:

| Componente | Teléfono | Tablet | Escritorio |
| --- | --- | --- | --- |
| `AppShell` | Panel deslizable con botón de menú y cabecera de 56 px | Barra lateral de íconos de 72 px | Barra lateral de 264 px |
| `PageHeader` | Título de 24 px y acciones apiladas a ancho completo | Acciones junto al título | Acciones junto al título |
| `Table` | Fichas: una por fila, columnas secundarias omitidas | Desplazamiento horizontal | Tabla completa |
| `Pagination` | Flechas y "Página X de N" | Números de página | Números y resumen |
| `Tabs` | Desplazables con ajuste | Pestañas completas | Pestañas completas |
| `Stepper` | Vertical | Horizontal | Horizontal |
| `Columns` | Una columna | Dos columnas o una | Según el diseño |
| `Modal` y `ConfirmDialog` | Hoja inferior con acciones apiladas | Diálogo centrado | Diálogo centrado |
| `Dropzone` | Botón "Tomar foto" y zona compacta | Foto o arrastre | Arrastre y selección |
| `StockSummary` | Libre en la fila superior y dos cifras debajo | Tres cifras en fila | Tres cifras en fila |
| `ChatComposer` | Fijo al pie, con área segura | Fijo en el panel | Fijo en el panel |

Matriz de verificación: 360 x 740, 390 x 844, 430 x 932, 768 x 1024, 1024 x 768 (tablet horizontal), 1280 x 720 y 1920 x 1080, en tema claro y oscuro, más el zoom del navegador al 200 %. Cada pantalla de referencia tiene su variante de teléfono o tablet en el prototipo.

Implementación en Mantine: los puntos de quiebre `xs`, `sm`, `md` y `lg` equivalen a `bp-phone`, `bp-tablet`, `bp-desktop` y `bp-wide`. Emplear `AppShell` con `navbar.breakpoint` en `sm` y `collapsed` para el panel deslizable, `SimpleGrid` con `cols` responsive para `Columns`, y la propiedad `visibleFrom` o `hiddenFrom` para omitir columnas en teléfono.

## Iconografía

- Conjunto propio de 63 íconos de trazo (`Icon`), rejilla de 24 px, trazo de 1.75 px y color heredado. Los íconos de dominio nombran conceptos del negocio (`kardex`, `almacen`, `tienda`, `producto`, `movimiento`, `traslado`, `despacho`, `recepcion`, `reserva`, `orden-compra`, `cotizacion`, `venta`, `proveedor`, `scan`, `sku`, `rol`, `sparkles` para el agente de IA).
- Un ícono acompaña a un texto; solo `IconButton` prescinde de texto y exige `label`. Los íconos decorativos se ocultan a lectores de pantalla.
- Sin emojis ni ilustraciones en la interfaz.
- Los íconos adicionales se dibujan con las mismas reglas y se agregan al diccionario de `Icon`; no se mezclan familias externas.

## Logo

El logo de Progresol (`frontend/src/assets/progresol-logo.png`) se usa en la cabecera de la barra lateral (44 px) y en la pantalla de acceso (72 px), sin recolorear ni deformar. Reglas de uso en la sección Logo de este documento.

## Componentes y composición

El sistema se organiza en cuatro capas; cada una solo depende de las anteriores:

1. **Tokens** (`tokens.json`, `tokens.css`, `frontend/src/theme/theme.ts`): valores de color, tipografía, espaciado, radio, sombra, tamaño, punto de quiebre y capa.
2. **Primitivos** (`components/`, grupos Acciones, Formularios, Retroalimentación, Estructura, Navegación y Datos): botones, campos, tablas, avisos, diálogos, pestañas. Genéricos y sin conocimiento del negocio.
3. **Componentes de dominio** (grupos Datos y estados e Inventario): `StatusBadge`, `MovementBadge`, `StockSummary`, `StockBar`, `MatchConfidence`, `ChatMessage`. Encapsulan el vocabulario y las reglas visuales del negocio y se construyen sobre los primitivos.
4. **Pantallas de referencia** (grupo Pantallas): composiciones completas que fijan la jerarquía, los espacios y los patrones de cada flujo.

Reglas de reutilización:

- Antes de crear un componente, buscar el existente que lo resuelva por composición. Un patrón que se repite en tres lugares se promueve a componente.
- Los estados de entidades siempre usan `StatusBadge`; los listados, `Table` con `Pagination`; los formularios, los campos del sistema dentro de `Stack`; las confirmaciones, `ConfirmDialog`.
- Las propiedades siguen un vocabulario común: `variant`, `size` (`sm|md|lg`), `tone`, `label`, `description`, `error`, `loading`, `disabled`, `onChange(valor)`.
- Ningún componente declara valores literales de color, tamaño o sombra: todo proviene de los tokens.

## Pantallas de trabajo

Reglas que se derivan de la crítica de las pantallas de almacén y rigen todo flujo de confirmación (recepción, despacho, venta, traslado):

- **Una respuesta antes que una tabla.** La pantalla de consulta responde primero lo que el usuario necesita saber: cuánto hay libre y dónde está (`StockSummary`, `StockLocations`, `StockBar`). El detalle numérico va debajo o en el detalle del producto.
- **Prevención visible.** Un error posible se muestra antes de confirmar, junto al dato: la diferencia entre lo pedido y lo recibido ("Faltan 20", "Sobran 5"), la línea sin producto asociado y el nivel de coincidencia. `ActionBar` resume el recuento y explica por qué la acción principal no está disponible; nunca queda un botón apagado sin motivo.
- **Bloquear solo lo que el negocio exige.** Una línea sin producto asociado impide confirmar; una recepción parcial (RF-COM-11) no. Esta regla es un supuesto pendiente de confirmar con el negocio.
- **Acciones masivas y atajos de una acción.** "Igualar coincidencias altas" y "Igual al pedido" evitan teclear cada cantidad; no se usan en líneas de coincidencia baja.
- **Acción principal al pie.** En teléfono y tablet la confirmación vive en `ActionBar`, en la zona del pulgar.
- **Cierre.** Toda confirmación termina en una `Constancia` que dice qué cambió y qué sigue.
- **Un solo lugar para cada estado.** El estado de un documento se muestra una vez como sello; el ciclo de vida, como `Stepper`, solo cuando se necesita para decidir.
- **Avisos breves.** Un `Alert` informativo ocupa como máximo dos líneas.
- **Modo almacén.** Un interruptor por equipo (`AppShell`, `warehouse`) que sube los controles a 52 a 56 px, muestra las etiquetas de la barra lateral en tablet y agranda las cifras.

## Integración con el stack

- **Mantine.** `frontend/src/theme/theme.ts` traduce los tokens a `createTheme` (paletas `brand` y `gray`, `primaryShade` 7 en claro y 2 en oscuro, radios, espaciado, familias tipográficas, sombras y puntos de quiebre). Cada componente de este sistema tiene un equivalente indicado en su README; los componentes del sistema que no existen en Mantine (`StatusBadge`, `StockSummary`, `StockBar`, `MatchConfidence`, `ChatMessage`, `PageHeader`, `EmptyState`) se implementan como componentes propios sobre los de Mantine.
- **Estilos.** Tema de Mantine para valores globales y CSS Modules para estilos específicos, en archivos externos, como establece ADR-001. `tokens.css` reside en `frontend/src/theme/` y se importa una vez; las variables (`--brand`, `--surface`, `--space-4`, `--radius-md`) se consumen desde los CSS Modules.
- **Modo claro y oscuro.** El selector `data-theme` de `tokens.css` se sincroniza con el esquema de color de Mantine (`data-mantine-color-scheme`); el detalle está en el encabezado de `mantine-theme.ts`.
- **Organización sugerida del paquete `frontend`**, alineada con los módulos de los diagramas de paquetes:

```text
frontend/src/
  theme/          tokens.css, theme.ts
  components/
    ui/           primitivos (Button, TextInput, Table, Modal, ...)
    domain/       StatusBadge, MovementBadge, StockSummary, StockBar, MatchConfidence
    layout/       AppShell, PageHeader, Breadcrumbs
  features/       seguridad, catalogo, inventario, documentos-ocr, comercial,
                  despacho, abastecimiento, agente-ia  (vistas, hooks, esquemas Zod)
  api/            cliente HTTP y tipos de contratos
  routes/         rutas y protección por rol
```

- **Validación y estados del servidor.** Los esquemas Zod producen los mensajes de `error` de cada campo; TanStack Query aporta los estados `isLoading` y `isError` que se traducen a `Skeleton`, `loading` y `Alert`.
- **Verificación.** `eslint-plugin-jsx-a11y` y Testing Library cubren etiquetas, roles y teclado; el contraste proviene de los tokens.
- **Documento HTML.** Declarar `<meta name="color-scheme" content="light dark">` y `<meta name="theme-color">` con el valor de `page` de cada tema; `bundle.css` ya fija `color-scheme` según `data-theme` para que los controles nativos y las barras de desplazamiento sigan el tema.

## Navegación por rol

Propuesta derivada de los actores de los casos de uso. La visibilidad es una ayuda de usabilidad; la autorización definitiva reside en el backend.

| Módulo | Casos de uso | Roles |
| --- | --- | --- |
| Disponibilidad | CU-04 | Almacenero, Vendedor |
| Catálogo | CU-03 | Almacenero, Administrador |
| Movimientos y Kardex | CU-05, CU-07 | Almacenero |
| Despachos | CU-06 | Almacenero |
| Recepciones | CU-13 | Almacenero |
| Documentos OCR | CU-08 | Vendedor, Almacenero |
| Cotizaciones y ventas | CU-09, CU-10 | Vendedor |
| Proveedores y órdenes de compra | CU-11, CU-12 | Reponedor |
| Agente de IA | CU-14 | Todos los roles |
| Usuarios y roles | CU-02 | Administrador |

## Supuestos y puntos pendientes

- El Administrador accede a todos los módulos: los requerimientos no lo establecen de forma expresa.
- El nivel de alerta de stock se calcula sobre el stock libre; confirmar si debe calcularse sobre el stock físico.
- RNF-05 contempla computadoras de escritorio y tablets. La compatibilidad con teléfonos se incorpora por decisión del equipo y debe registrarse en los requerimientos.
- La confirmación de una recepción se bloquea mientras exista una línea sin producto asociado y admite cantidades menores a las pedidas (recepción parcial). Confirmar la regla con el negocio.
- La tabla de Disponibilidad reemplaza las columnas Físico y Reservado por el reparto (`StockBar`) y por las ubicaciones; los valores exactos se consultan en el detalle del producto. Confirmar con RF-KAR-02 y RF-PRO-05 si deben mantenerse como columnas.
- El modo almacén se guarda por equipo; falta definir quién puede activarlo y si se guarda en el dispositivo o en el servidor.
- Los umbrales de `MatchConfidence` (0.85 y 0.60) son valores de ejemplo hasta que se definan en el servicio Python.
- Los nombres definitivos de los estados de cotización, venta y guía deben armonizarse con los casos de uso (tabla de limitaciones de la metodología); el mapa `STATUS` se actualiza una sola vez cuando se confirmen.
- La atención parcial de ventas (RF-VEN-12) difiere de la reserva parcial con despacho completo del modelo vigente; las pantallas de venta se ajustan cuando se resuelva.
- Tablas con ordenamiento, filtrado y paginación avanzados pueden requerir TanStack Table (consecuencia desfavorable registrada en ADR-001).

## Reglas de uso del logo

Logo de Progresol (Ferretería Progresol Migal), entregado por el equipo del proyecto.

- `frontend/src/assets/progresol-logo.png`: imagen raster de 417 x 417 px con esquinas transparentes. Disco azul `brand-7` (#00339F) con el símbolo solar verde (#17FF44) y el nombre "Progresol" en blanco. No es un trazo de un solo color: no recolorear con CSS.
- Uso: cabecera de la barra lateral (44 px de lado) y pantalla de acceso (72 px). Tamaño mínimo: 40 px.
- Sobre `nav-bg` en el tema claro (mismo azul que el disco), el disco se funde con el fondo y el símbolo y el nombre permanecen visibles; no agregar contorno ni sombra.
- Mantener las proporciones; no deformar, rotar ni aplicar filtros. Dejar un margen libre de al menos 8 px alrededor.
- No existe versión vectorial en el repositorio; se recomienda solicitarla para impresión y pantallas de alta densidad.
