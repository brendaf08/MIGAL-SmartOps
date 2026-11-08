# Instrucciones para el frontend de MIGAL SmartOps

Estas instrucciones se aplican a todo el trabajo dentro de `frontend/`. Complementan las reglas de [CONTRIBUTING.md](../CONTRIBUTING.md) y las decisiones de [ADR-001](../docs/adr/adr-001-stack-frontend.md) y [ADR-002](../docs/adr/adr-002-sistema-de-diseno.md).

## Contexto del producto

MIGAL SmartOps es la plataforma web de gestión operativa e inventario de la Ferretería Progresol Migal. El frontend es una SPA para usuarios internos con cuatro roles: Administrador, Vendedor, Almacenero y Reponedor. Se usa en computadoras All-in-One y tablets. Consume la API REST del backend Spring Boot; nunca accede a la base de datos ni al servicio Python.

## Fuentes de consulta, en orden

1. [docs/diseno/sistema-de-diseno.md](../docs/diseno/sistema-de-diseno.md): fundamentos, principios y reglas de uso.
2. [docs/diseno/componentes/](../docs/diseno/componentes/README.md): especificación de cada componente y pantalla.
3. [docs/diseno/tokens.json](../docs/diseno/tokens.json) y `src/theme/`: valores normativos.
4. [docs/diseno/prototipo/](../docs/diseno/prototipo/README.md): comportamiento y aspecto de referencia; consultarlo, no importarlo.
5. [docs/requerimientos.md](../docs/requerimientos.md), [docs/especificaciones-casos-de-uso.md](../docs/especificaciones-casos-de-uso.md) y [docs/diagramas-paquetes.md](../docs/diagramas-paquetes.md): comportamiento funcional y módulos.

Ante una contradicción entre el diseño y los requerimientos, prevalecen los requerimientos; registrar la diferencia en el pull request.

## Stack

Vite, TypeScript (modo estricto), React, Mantine (tema y paquetes de formularios, modales, notificaciones y carga de archivos), CSS Modules en archivos externos, React Router, TanStack Query, Zod, Vitest, Testing Library y `eslint-plugin-jsx-a11y`. Para el catálogo de componentes, Storybook. No agregar otras librerías de interfaz ni de estilos sin un ADR.

## Estructura del código

```text
src/
  theme/          tokens.css, theme.ts (valores generados o derivados de tokens.json)
  assets/         logo y recursos estáticos
  components/
    ui/           primitivos genéricos (Button, TextInput, Table, Modal, ...)
    domain/       StatusBadge, MovementBadge, StockSummary, StockBar, MatchConfidence, chat
    layout/       AppShell, PageHeader, Breadcrumbs
  features/       un directorio por módulo funcional: seguridad, catalogo, inventario,
                  documentos-ocr, comercial, despacho, abastecimiento, agente-ia
  api/            cliente HTTP y tipos de los contratos de la API REST
  routes/         definición de rutas y protección por rol
```

Dependencias en una sola dirección: `features` usa `components`; `components/domain` usa `components/ui`; `components/ui` no conoce el negocio. Los componentes visuales no llaman a la API: reciben datos por propiedades o por hooks de `features`.

## Reglas de implementación

- **Tokens, no literales.** Usar las variables de `tokens.css` (`--brand`, `--surface`, `--space-4`, `--radius-md`) o el tema de Mantine. Prohibido escribir colores, tamaños de fuente, radios o sombras literales en los componentes.
- **Reutilizar antes de crear.** Buscar en `components/` y en el catálogo de `docs/diseno/componentes/` antes de crear un componente. Promover a componente un patrón que se repita en tres lugares.
- **Estados con `StatusBadge`.** Todo estado de cotización, venta, guía, compra, recepción, documento o reserva se muestra con `StatusBadge`. El mapa de estados reside en un único archivo.
- **Estilos.** Tema de Mantine para valores globales y CSS Modules para estilos específicos. Sin estilos en línea salvo valores dinámicos.
- **Formatos.** Importes `S/ 1,250.00`, cantidades con separador de miles, fechas `dd/mm/aaaa`, usando `Intl` con `es-PE`. Cifras tabulares y alineadas a la derecha en tablas.
- **Texto de interfaz.** En español, tipo oración, botones con verbo en infinitivo ("Registrar venta"). Los errores indican qué ocurrió y cómo corregirlo, junto al campo. Sin emojis.
- **Tipos y validación.** Contratos de la API tipados; esquemas Zod para cada formulario, con mensajes de error del diseño. La validación del cliente complementa la del backend.
- **Roles.** La navegación y las rutas se filtran por rol, pero la autorización real la aplica el backend.
- **Operaciones sobre el inventario.** Toda acción irreversible (cancelar venta, confirmar salida o recepción, desactivar proveedor) pasa por `ConfirmDialog` con sus consecuencias. El OCR y el Fuzzy Matching solo precargan datos que el usuario revisa y confirma. El agente de IA solo consulta.

## Accesibilidad y calidad

- Contraste WCAG AA, operación completa por teclado, foco visible, objetivos táctiles de 44 px y estados con texto y forma, no solo color.
- Etiqueta visible en cada campo; `aria-label` obligatorio en botones de solo ícono; encabezados en orden.
- Cada componente nuevo incluye: prueba con Testing Library (roles, nombres accesibles y teclado), historia de Storybook y verificación con `axe`.
- Comprobar en 390 px (teléfono), 768 px (tablet) y 1280 px (All-in-One), en tema claro y oscuro, y con el zoom del navegador al 200 %.
- **Responsive.** Una sola base de código con tres composiciones (teléfono, tablet y escritorio) según [Diseño responsive](../docs/diseno/sistema-de-diseno.md). Usar los puntos de quiebre del tema de Mantine (`xs`, `sm`, `md`, `lg`), nunca anchos fijos en píxeles ni valores de media query sueltos. En teléfono: navegación en panel deslizable, tablas en fichas con las columnas secundarias omitidas, diálogos como hoja inferior, acciones principales a ancho completo, objetivos táctiles de 44 px y campos de 16 px. Sin desplazamiento horizontal de la página. Declarar `viewport-fit=cover` y respetar las áreas seguras.

## Flujo de trabajo

1. Partir de una tarea de Notion y de una rama desde `main` con el prefijo que corresponda (`feat/`, `fix/`, `refactor/`, `chore/`, `docs/`).
2. Planificar antes de implementar cambios que afecten varias capas. Un pull request por grupo de componentes o por módulo funcional; no mezclar tareas independientes.
3. Implementar siguiendo el orden: tokens y tema, primitivos, componentes de dominio, pantallas por módulo. Cada componente se compara con su vista previa del prototipo.
4. Ejecutar antes de abrir el pull request: lint, comprobación de tipos, pruebas y build. Registrar los resultados en la descripción.
5. Commits atómicos con el formato de CONTRIBUTING.md (sección 4); el historial reciente usa `tipo(ámbito): descripción` en minúsculas y en modo imperativo. Sin líneas de coautoría.
6. Si el diseño cambia, actualizar `docs/diseno/` y los tokens en el mismo pull request.

## Fuera de alcance

Comprobantes electrónicos ante SUNAT, módulos contables y financieros, integración con WhatsApp Business y funcionalidades de flota o seguimiento GPS (ver [alcance y límites](../docs/alcance-y-limites.md)).

## Pendientes que condicionan la implementación

- Confirmar si el Administrador accede a todos los módulos.
- Confirmar si el nivel de stock mínimo se calcula sobre el stock libre o el físico.
- Definir los umbrales de coincidencia del Fuzzy Matching (valores de ejemplo: 0.85 y 0.60).
- Armonizar los nombres definitivos de los estados de cotización, venta y guía.
- Resolver la diferencia entre la atención parcial de ventas (RF-VEN-12) y la reserva parcial con despacho completo del modelo vigente.
