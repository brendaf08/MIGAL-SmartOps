# ADR-001: Stack tecnológico del frontend

## Contexto

La [Arquitectura tecnológica](../arquitectura-tecnologica.md) define el frontend de MIGAL SmartOps como una Single Page Application construida con React, que consume la API REST del backend Spring Boot. Queda pendiente definir la herramienta de construcción, el lenguaje, la librería de componentes y las librerías de soporte.

La elección debe satisfacer los siguientes requerimientos:

| Requerimiento | Implicancia para el frontend |
| --- | --- |
| RNF-05, RNF-13 | Interfaz responsive para escritorio y tablets, compatible con navegadores modernos. |
| RNF-06 | Consultas habituales con respuesta no mayor a 3 segundos. |
| RNF-04 | Despliegue mediante contenedores Docker. |
| RNF-09, RF-USR-02, RF-USR-03 | Funcionalidades restringidas según los roles de Administrador, Vendedor, Almacenero y Reponedor. |
| RNF-14 | Módulos desacoplados mediante interfaces definidas. |
| RF-OCR-01, RF-OCR-02, RF-COM-13 | Carga de archivos JPG, PNG y PDF mediante arrastrar y soltar. |
| RF-COM-15, RF-COT-04 | Revisión y corrección de los datos precargados mediante OCR antes de confirmarlos. |
| RF-IA-01 | Interfaz conversacional integrada para el Agente de IA. |

Además, la interfaz debe aplicar los principios de diseño de interfaces adoptados por el proyecto:

- Heurísticas de Nielsen: visibilidad del estado del sistema, prevención de errores, control del usuario y consistencia.
- Accesibilidad según WCAG.
- Separación entre estructura, presentación y comportamiento.
- Componentes reutilizables.
- Validación en el cliente con mensajes de error claros.

## Decisión

Construir el frontend con el siguiente stack:

| Elemento | Tecnología |
| --- | --- |
| Construcción y servidor de desarrollo | Vite |
| Lenguaje | TypeScript |
| Librería de componentes | Mantine, con sus paquetes de formularios, modales, notificaciones y carga de archivos |
| Estilos | Tema de Mantine y CSS Modules en archivos externos |
| Navegación | React Router |
| Estado del servidor | TanStack Query |
| Validación de formularios | Zod, integrado con el gestor de formularios de Mantine |
| Comunicación con el backend | Capa de cliente HTTP dedicada |
| Pruebas y verificación | Vitest, Testing Library y `eslint-plugin-jsx-a11y` |

El código se organiza en vistas por rol, componentes reutilizables, hooks con la lógica de interacción y una capa de acceso a la API. Esta organización corresponde al paquete `frontend` definido en los [Diagramas de paquetes](../diagramas-paquetes.md).

## Justificación

**Vite.** El frontend es una SPA autenticada, sin necesidad de renderizado en servidor ni de posicionamiento en buscadores. Vite genera archivos estáticos que se sirven desde un contenedor web liviano (RNF-04). Así se evita incorporar un servidor Node adicional a una arquitectura cuyo backend ya es Spring Boot.

**TypeScript.** El tipado de los contratos de la API REST permite detectar en el desarrollo las inconsistencias con el backend. Esto refuerza el desacoplamiento mediante interfaces definidas (RNF-14).

**Mantine.** Proporciona componentes que cubren directamente los requerimientos y principios del contexto:

- Un componente de carga de archivos mediante arrastrar y soltar, con filtrado por tipo de archivo (RF-OCR-01, RF-OCR-02).
- Modales de confirmación para acciones irreversibles, como cancelaciones y despachos (prevención de errores).
- Notificaciones, indicadores de carga y estados de los botones (visibilidad del estado del sistema y retroalimentación).
- Componentes con atributos ARIA, navegación por teclado y asociación automática entre etiquetas y campos (WCAG).
- Un tema centralizado de colores, tipografía y espaciado (consistencia).
- Una estructura de aplicación con navegación lateral y una cuadrícula responsive para escritorio y tablet (RNF-05).

Sus estilos se definen con CSS estándar y CSS Modules, en archivos separados del marcado y de la lógica.

**React Router.** Permite definir una ruta por módulo y proteger las rutas según el rol del usuario autenticado (RF-USR-03).

**TanStack Query.** Gestiona la caché y la revalidación de las consultas, lo que reduce las solicitudes repetidas al backend (RNF-06). Además, expone los estados de carga y error que la interfaz presenta al usuario.

**Zod.** Concentra las reglas de validación de cada formulario en un solo esquema, que se refleja en mensajes de error junto a cada campo. Esta validación complementa a la del backend sin reemplazarla.

**Capa de cliente HTTP.** Aísla la comunicación con la API REST de los componentes visuales, de modo que un cambio en los servicios no afecta las vistas (RNF-14).

**Vitest, Testing Library y `eslint-plugin-jsx-a11y`.** Permiten verificar el comportamiento de los componentes y detectar problemas de accesibilidad durante el desarrollo.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Next.js | Su principal aporte es el renderizado en servidor y el posicionamiento en buscadores, que no se requieren en una aplicación interna autenticada. Agrega un servidor Node al despliegue. |
| Tailwind CSS con shadcn/ui | Define los estilos mediante clases utilitarias dentro del marcado, lo que contradice la separación entre estructura y presentación. No incluye un componente de carga de archivos. |
| MUI (Material UI) | Define los estilos dentro del código JavaScript, impone la apariencia de Material Design y no incluye un componente de carga de archivos. |
| Ant Design | Define los estilos dentro del código JavaScript y su personalización visual requiere mayor esfuerzo. |
| JavaScript sin tipado | No permite verificar los contratos con el backend durante el desarrollo. |

## Consecuencias

**Favorables:**

- La mayoría de los patrones de interfaz requeridos se implementan con componentes existentes, sin desarrollo propio.
- La accesibilidad y la consistencia visual parten de una base común para todas las vistas.
- El frontend se despliega como contenido estático, sin dependencias de ejecución adicionales.

**Desfavorables:**

- El proyecto depende del ciclo de mantenimiento de Mantine. Para mitigarlo, se recomienda limitar las personalizaciones al tema y a los CSS Modules.
- TypeScript, TanStack Query y Zod exigen un aprendizaje inicial a los integrantes que no los conocen.
- Las tablas con ordenamiento, filtrado y paginación avanzados pueden requerir una librería complementaria.

## Referencias

- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Requerimientos](../requerimientos.md)
- [Diagramas de paquetes](../diagramas-paquetes.md)
