# ADR-002: Sistema de diseño del frontend

## Contexto

[ADR-001](adr-001-stack-frontend.md) define el stack del frontend (Vite, TypeScript, Mantine, CSS Modules, React Router, TanStack Query, Zod y herramientas de prueba) y establece como principios la consistencia visual, la accesibilidad según WCAG, los componentes reutilizables y la separación entre estructura, presentación y comportamiento. Queda pendiente definir la identidad visual, los componentes propios del dominio y la forma en que el diseño se transmite a la implementación.

Intervienen los siguientes requerimientos y restricciones:

| Requerimiento | Implicancia para el sistema de diseño |
| --- | --- |
| RNF-05, RNF-13 | Interfaz responsive para escritorio (All-in-One) y tablets, con navegadores modernos. El equipo incorpora además los teléfonos, que no figuran en RNF-05. |
| RF-USR-02, RF-USR-03, RNF-09 | Navegación y funcionalidades según los roles Administrador, Vendedor, Almacenero y Reponedor. |
| RF-KAR-02, RF-KAR-09, RF-PRO-05 | Presentación de stock físico, reservado y libre por ubicación y alertas por nivel mínimo. |
| RF-OCR-01, RF-OCR-02, RF-OCR-05, RF-OCR-06, RF-FUZ-05, RF-FUZ-06 | Carga de documentos y revisión de datos precargados con indicación del nivel de coincidencia. |
| RF-IA-01, RF-IA-04, RF-IA-08, RF-IA-09 | Interfaz conversacional que solo consulta, indica la falta de información y respeta los permisos. |
| RF-COT-05, RF-VEN-12, RF-DES-01 | Estados de cotizaciones, ventas y despachos visibles de forma consistente. |
| RNF-06 | Respuesta de consultas habituales en un máximo de 3 segundos, con estados de carga visibles. |

Los criterios de evaluación son: identidad coherente con el logo de Progresol, accesibilidad verificable, reutilización de componentes, mantenibilidad con el stack de ADR-001 y capacidad de transmitir el diseño a la implementación sin ambigüedad.

## Decisión

Adoptar un sistema de diseño propio, documentado en [docs/diseno/](../diseno/README.md), con los siguientes elementos:

| Elemento | Decisión |
| --- | --- |
| Tokens | Un archivo normativo (`tokens.json`) del que se derivan `tokens.css` y el tema de Mantine (`theme.ts`), ubicados en `frontend/src/theme/`. Ningún componente declara valores literales de color, tamaño ni sombra. |
| Identidad | Azul Progresol y verde del logo como colores de marca. El verde se limita a marcadores y sellos; el azul rige acciones y navegación. |
| Temas | Claro (predeterminado) y oscuro tenue, de grises pizarra en lugar de negros, aplicados con `data-theme` y sincronizados con el esquema de color de Mantine. La elección del usuario se conserva en `localStorage` y la preferencia inicial proviene de `prefers-color-scheme`. |
| Tipografía | Bricolage Grotesque en títulos, Instrument Sans en texto y controles, Barlow Semi Condensed en cifras y rótulos, e IBM Plex Mono en códigos. Se autoalojan mediante paquetes `@fontsource`. |
| Accesibilidad | Contraste WCAG AA verificado para cada par documentado en ambos temas, objetivos táctiles de 44 px, operación completa por teclado y estados que nunca dependen solo del color. |
| Responsive | Un solo código con tres composiciones: teléfono (menos de 768 px), tablet (768 a 1023 px) y escritorio (1024 px o más), con puntos de quiebre equivalentes a `xs`, `sm`, `md` y `lg` de Mantine. En teléfono: navegación en panel deslizable, tablas en fichas, diálogos como hoja inferior, acciones a ancho completo, campos de 16 px y captura de documentos con la cámara. Cada pantalla de referencia tiene su variante de teléfono o tablet. |
| Organización del código | `frontend/src/theme/`, `components/ui/` (primitivos), `components/domain/` (estados, stock, coincidencias, chat), `components/layout/` y `features/` por módulo funcional de [Diagramas de paquetes](../diagramas-paquetes.md). |
| Catálogo de componentes | Documentación de cada componente en `docs/diseno/componentes/` y un catálogo vivo con Storybook, que reemplaza al prototipo cuando los componentes reales lo igualen. |
| Prototipo | El prototipo de `docs/diseno/prototipo/` es una referencia visual y de comportamiento. No se importa desde el frontend. |
| Fuente de verdad | El artefacto del sistema de diseño durante la etapa de prototipo; el código de `frontend/` una vez estabilizados los componentes. El cambio de etapa se registra en un nuevo ADR. |

## Justificación

**Tokens como fuente única.** Concentrar los valores visuales en un solo archivo permite cambiar un color o una medida en un punto, mantener ambos temas coherentes y verificar el contraste de forma sistemática. Responde a la separación entre estructura y presentación de ADR-001.

**Tema de Mantine y CSS Modules.** El sistema se traduce al tema de Mantine y a variables CSS, de modo que las personalizaciones se limitan a los puntos que ADR-001 recomienda (tema y CSS Modules) y se mitiga la dependencia del ciclo de mantenimiento de la librería.

**Componentes de dominio.** El stock físico, reservado y libre, los estados de las entidades, el movimiento de Kardex, la coincidencia de OCR y el chat del agente no existen en Mantine. Encapsularlos garantiza que cada concepto se vea igual en todas las pantallas (RF-KAR-02, RF-COT-05, RF-FUZ-05, RF-IA-04).

**Diseño responsive con un solo código.** El personal alterna entre el mostrador, los almacenes y el escritorio: consulta disponibilidad desde el teléfono, opera recepciones y despachos en tablet y gestiona cotizaciones y compras en la computadora. Recomponer los mismos componentes evita mantener una versión móvil aparte, y la cámara del teléfono acorta la captura de notas y guías para el OCR (RF-OCR-01, RF-COM-13).

**Temas claro y oscuro tenue.** La operación en tienda y almacén ocurre con iluminaciones distintas; un tema oscuro de contraste moderado reduce el deslumbramiento sin recurrir a fondos negros. Ambos temas se verifican con los mismos criterios de contraste.

**Fuentes autoalojadas.** La ferretería puede operar con conexión inestable; autoalojar evita retrasos de carga y dependencias externas, y es compatible con el despliegue en contenedores (RNF-04).

**Prototipo y documentación versionada.** Mantener el diseño dentro del repositorio permite revisarlo en los pull requests, usarlo en integración continua y que cualquier herramienta de desarrollo lo lea sin acceso a servicios externos.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Usar Mantine sin personalizar | No expresa la identidad de Progresol ni los conceptos del dominio, y deja sin definir los estados, el stock y los flujos de OCR e IA. |
| Tailwind CSS con shadcn/ui | Ya descartada en ADR-001 por definir los estilos dentro del marcado. |
| Mantener el diseño solo en el artefacto | Depende de un enlace privado y de una sesión, no se versiona con el código y no puede usarse en integración continua. |
| Importar el prototipo como librería de componentes | El prototipo usa React sin tipado ni pruebas, y no sigue la estructura de ADR-001. |
| Fuentes del sistema operativo | Producen una apariencia distinta en cada equipo y no permiten el rotulado condensado de cifras y encabezados. |
| Versión móvil independiente o aplicación nativa | Duplica componentes y mantenimiento, y queda fuera del stack definido en ADR-001. |
| Limitar el diseño a escritorio y tablets, como en RNF-05 | No atiende el uso desde teléfonos que el equipo prevé en tienda y almacenes. |
| Tema único claro | No atiende el uso en entornos de poca luz ni la preferencia del usuario. |

## Consecuencias

**Favorables:**

- Cada decisión visual tiene una única fuente, trazable y revisable en los pull requests.
- La accesibilidad y la consistencia parten de una base verificada.
- La implementación puede apoyarse en una especificación completa de cada componente y pantalla.
- El tema de Mantine y los CSS Modules limitan el alcance de las personalizaciones.

**Desfavorables:**

- Hay que mantener la sincronización entre el artefacto, la documentación y el código mientras el diseño evoluciona. Se mitiga con la regla de fuente de verdad por etapa y con la exportación a `docs/diseno/` en el mismo pull request.
- Storybook y las pruebas de accesibilidad automatizadas agregan dependencias que no figuran en ADR-001. Se proponen como parte de esta decisión y se incorporan al montar el proyecto.
- Cuatro familias tipográficas aumentan el peso de la carga inicial. Se mitiga con fuentes variables, subconjuntos de caracteres latinos y precarga de las dos de uso principal.
- RNF-05 no incluye los teléfonos. Registrar la ampliación en los requerimientos y verificar cada pantalla en tres tamaños aumenta el esfuerzo de prueba. Se mitiga con la matriz de verificación documentada y las variantes del prototipo.
- Los supuestos pendientes (permisos del Administrador, nivel de stock sobre libre o físico, umbrales de coincidencia y nombres de estados) deben confirmarse antes de implementar las pantallas que dependen de ellos.

## Referencias

- [ADR-001: Stack tecnológico del frontend](adr-001-stack-frontend.md)
- [Sistema de diseño](../diseno/README.md)
- [Requerimientos](../requerimientos.md)
- [Diagramas de paquetes](../diagramas-paquetes.md)
- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
