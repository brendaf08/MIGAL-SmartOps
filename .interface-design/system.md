# Sistema de interfaz de MIGAL SmartOps

Registro de las decisiones de diseño vigentes. La versión completa, con componentes y pantallas de referencia, se mantiene en el artefacto del sistema de diseño (https://claude.ai/artifact/7bpusGM3ddhCYXfau5xVfT) y sus valores exportados en `frontend/src/theme/`.

## Dirección

- Usuarios: personal de una ferretería con cuatro roles (Administrador, Vendedor, Almacenero, Reponedor), en computadoras All-in-One y tablets.
- Sensación buscada: libro de existencias dibujado sobre papel de plano. Azul pálido, tinta azul marino, reglas finas, rotulado de bodega. Sobrio, preciso y legible antes que decorativo.
- Concepto: papel de plano azul pálido y tinta azul marino en el tema claro, con barra lateral en el azul profundo de la marca; en el tema oscuro, grises pizarra tenues (`page` #2B303A, `surface` #353B47), no negros. Selector de modo en la cabecera.
- Marca: azul Progresol `#00339F` como tinta de las acciones y verde neón `#17FF44` como sello de marca, solo con contorno `on-accent` sobre fondos claros.

## Rasgos propios

- Señalización industrial para estados: cuadrado con marca (correcto), triángulo (atención), octógono (crítico), círculo (información).
- Ficha de existencias: el stock libre es la cifra dominante; físico y reservado acompañan.
- Cinta métrica: regla graduada bajo la barra de stock.
- Etiqueta de estante: marcador cuadrado verde con contorno azul en el ítem activo de navegación.
- Cifras de bodega: fuente condensada solo para cifras dominantes y sello.
- Tipografía: Instrument Sans en todo el texto, los datos y los encabezados de sección; Bricolage Grotesque solo en el título de página; Barlow Semi Condensed solo en cifras grandes y sellos; IBM Plex Mono en códigos.
- Doble línea contable en los totales de tabla; sello de doble contorno para el estado de un documento.

## Profundidad y espaciado

- Estrategia: bordes, no sombras. `line` es tinta al 10 % de opacidad. Las sombras se reservan para lo que flota (`shadow-md`, `shadow-lg`).
- Base de espaciado: 4 px. Relleno de celdas 12 px (16 px en pantallas táctiles), de tarjetas 20 px, separación entre grupos 24 px.
- Radios: `radius-md` 6 px en controles, `radius-lg` 10 px en tarjetas, `radius-xl` 16 px en modales, concéntricos según el relleno.
- Superficies: `page`, `surface` (también cabecera), `nav-bg`, `surface-raised` solo para elementos flotantes, `surface-sunken` y `field-bg` para zonas hundidas.

## Jerarquía

- Escala cercana a 1.2 sobre 14 px: 12, 14, 16, 18, 22, 28; cifras 28 y 36 en la fuente de rotulado.
- Tres niveles por dato: valor (600 a 700, `ink`), etiqueta (500 a 600, `ink-muted`), metadato (400, `ink-subtle`).
- Un foco por vista: tabla en disponibilidad, líneas por revisar en recepción con OCR, totales y estado en cotización.

- Dónde está: `StockLocations` (tienda y almacenes como plano esquemático) y cinta por empaque con marcas de mínimo y reposición.
- Cierre: `Constancia` con sello estampado una vez y antes/después en doble línea; `ActionBar` fija al pie con el motivo del bloqueo visible.
- Modo almacén por equipo: controles de 52 a 56 px y etiquetas visibles en tablet.

## Patrones de componentes

- Button: 40 px de alto, relleno 0 16 px, radio 6 px, `t-button-md`; tamaños sm 32 px y lg 48 px; escala 0.97 al presionar.
- Campo: 40 px de alto, fondo `field-bg`, borde `line-strong`, foco con `shadow-focus` y fondo `surface`.
- Tabla: encabezado `surface-sunken` en `t-th` (sin mayúsculas), filas con `line`, cifras en `t-data` alineadas a la derecha, pie con doble línea.
- Barra lateral: 264 px (72 px en tablet), ítems de 44 px, fondo `nav-bg` (azul profundo de la marca; gris pizarra profundo en oscuro).
- Movimiento: 120 a 200 ms, curva `cubic-bezier(0.23, 1, 0.32, 1)`, solo `transform` y `opacity`; sin animación en acciones de alta frecuencia.

## Pendientes

- Confirmar si el Administrador accede a todos los módulos.
- Confirmar si el nivel de stock se calcula sobre el stock libre o el físico.
- Definir los umbrales de coincidencia (0.85 y 0.60 son valores de ejemplo).
- Armonizar los nombres definitivos de los estados de cotización, venta y guía con los casos de uso.
