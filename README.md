# MIGAL SmartOps

MIGAL SmartOps es una plataforma web de gestión operativa e inventario para la Ferretería Progresol Migal. Centraliza el catálogo de productos, las existencias de la tienda y sus dos almacenes, las cotizaciones, las ventas y el abastecimiento.

Incorpora procesamiento de documentos mediante OCR y coincidencia difusa para apoyar la captura de información, además de un asistente consultivo que permite acceder a datos operativos según los permisos del usuario.

## Organización del trabajo

| Espacio | Función |
| --- | --- |
| **GitHub** | Documentación técnica, diagramas, código e historial de cambios revisados. |
| **Notion** | Tareas, responsables, fechas, observaciones y seguimiento de correcciones. |

La documentación de `docs/` constituye la fuente oficial de las definiciones técnicas del sistema. Los informes del proyecto se preparan a partir de una versión identificada del repositorio.

## Documentación

| Documento | Contenido |
| --- | --- |
| [Presentación del proyecto](docs/presentacion-del-proyecto.md) | Problemática, propuesta de valor y objetivos. |
| [Alcance y límites](docs/alcance-y-limites.md) | Funcionalidades incluidas y exclusiones. |
| [Análisis de stakeholders](docs/analisis-stakeholders.md) | Interesados, necesidades y participación. |
| [Requerimientos](docs/requerimientos.md) | Requerimientos funcionales, no funcionales y reglas de negocio. |
| [Casos de uso](docs/casos-de-uso.md) | Actores, catálogo de CU y diagramas por módulo. |
| [Especificaciones de casos de uso](docs/especificaciones-casos-de-uso.md) | Propósitos, precondiciones, flujos, alternativas y postcondiciones. |
| [Arquitectura tecnológica](docs/arquitectura-tecnologica.md) | Componentes, tecnologías y responsabilidades. |
| [Modelo y diccionario de datos](docs/modelo-y-diccionario-datos.md) | Entidades, atributos, relaciones y restricciones. |
| [Modelo y diccionario de clases](docs/modelo-y-diccionario-clases.md) | Clases, responsabilidades y operaciones. |
| [Flujo de datos](docs/flujo-de-datos.md) | Procesos, flujos y almacenes de datos. |
| [Diagramas de secuencia](docs/diagramas-secuencia.md) | Interacciones correspondientes a cada CU. |
| [Diagramas de actividades](docs/diagramas-actividades.md) | Flujos de ejecución correspondientes a cada CU. |
| [Matriz de trazabilidad](docs/matriz-trazabilidad.md) | Correspondencias entre requerimientos y artefactos. |

Todas las imágenes de diagramas se encuentran en [docs/imagenes/](docs/imagenes/). Cada documento muestra las imágenes que le corresponden mediante enlaces relativos.

## Estructura del repositorio

```text
MIGAL-SmartOps/
├── README.md
├── CONTRIBUTING.md
├── .gitignore
├── .gitattributes
├── docs/
│   ├── presentacion-del-proyecto.md
│   ├── alcance-y-limites.md
│   ├── analisis-stakeholders.md
│   ├── requerimientos.md
│   ├── casos-de-uso.md
│   ├── especificaciones-casos-de-uso.md
│   ├── arquitectura-tecnologica.md
│   ├── modelo-y-diccionario-datos.md
│   ├── modelo-y-diccionario-clases.md
│   ├── flujo-de-datos.md
│   ├── diagramas-secuencia.md
│   ├── diagramas-actividades.md
│   ├── matriz-trazabilidad.md
│   └── imagenes/
├── backend/
├── frontend/
├── servicio-python/
├── database/
│   └── migraciones/
├── entregas/
└── .github/
    ├── CODEOWNERS
    └── pull_request_template.md
```

Las instrucciones de instalación, configuración y ejecución se mantienen en la carpeta de cada componente: [backend](backend/), [frontend](frontend/), [servicio Python](servicio-python/) y [base de datos](database/).

## Cómo contribuir

1. Consultar la tarea y su responsable en Notion.
2. Actualizar la copia local del repositorio desde `main`.
3. Crear una rama para el cambio.
4. Actualizar los archivos y artefactos relacionados.
5. Abrir una Pull Request hacia `main` y enlazar la tarea de Notion.
6. Obtener la revisión de otro integrante y resolver las observaciones.
7. Integrar el cambio y registrar la evidencia en Notion.

Las reglas de nomenclatura, documentación, revisión e integración se establecen en [CONTRIBUTING.md](CONTRIBUTING.md).

## Equipo

- Alexander Diaz Ordoñez.
- Gabriel Gibson Martinez Arista.
- Brenda Nicole Mirano Flores.
- Luis Rodrigo Nuñez Principe.
- Diego Alonso Pinedo Aponte.

Las responsabilidades y fechas de trabajo se mantienen en Notion. Cada integrante utiliza su propia cuenta de GitHub para conservar la autoría de sus contribuciones y revisiones.

## Versiones del proyecto

Los hitos se identifican mediante etiquetas de Git, por ejemplo `pc1-v1.0`. Cada informe debe indicar la etiqueta o el commit utilizado como base para que su contenido pueda relacionarse con la documentación y el código correspondientes.

Los informes y archivos de cada hito se conservan en [entregas/](entregas/), organizados por entrega y versión.
