# MIGAL SmartOps

MIGAL SmartOps es una plataforma web de gestión operativa e inventario para la Ferretería Progresol Migal. Centraliza el catálogo de productos, las existencias de la tienda principal y sus dos almacenes, las cotizaciones, las ventas y el abastecimiento.

Incorpora procesamiento de documentos mediante OCR y coincidencia difusa (Fuzzy Matching) para apoyar la captura de información, además de un agente consultivo de IA que permite acceder a datos operativos según los permisos del usuario.

## Organización del trabajo

| Espacio | Función |
| --- | --- |
| GitHub | Documentación técnica, diagramas, código fuente y revisión de cambios mediante pull requests. |
| Notion | Asignación de tareas, responsables, fechas, observaciones y seguimiento de correcciones. |

La documentación de `docs/` contiene las definiciones técnicas acordadas del sistema. Los informes se preparan a partir de una versión identificada del repositorio.

## Documentación

| Documento | Contenido |
| --- | --- |
| [Presentación del proyecto](docs/presentacion-del-proyecto.md) | Problemática, propuesta de valor y objetivos. |
| [Alcance y límites](docs/alcance-y-limites.md) | Funcionalidades incluidas y exclusiones. |
| [Análisis de stakeholders](docs/analisis-stakeholders.md) | Interesados, necesidades y participación. |
| [Requerimientos](docs/requerimientos.md) | Requerimientos funcionales y no funcionales. |
| [Casos de uso](docs/casos-de-uso.md) | Actores, catálogo de casos de uso y diagramas por módulo. |
| [Especificaciones de casos de uso](docs/especificaciones-casos-de-uso.md) | Propósitos, precondiciones, flujos, alternativas y postcondiciones. |
| [Arquitectura tecnológica](docs/arquitectura-tecnologica.md) | Componentes, tecnologías y responsabilidades. |
| [Modelo y diccionario de datos](docs/modelo-y-diccionario-datos.md) | Entidades, atributos, relaciones y restricciones. |
| [Modelo y diccionario de clases](docs/modelo-y-diccionario-clases.md) | Clases, responsabilidades y operaciones. |
| [Flujo de datos](docs/flujo-de-datos.md) | Procesos, flujos y almacenes de datos. |
| [Diagramas de secuencia](docs/diagramas-secuencia.md) | Interacciones correspondientes a cada caso de uso. |
| [Diagramas de actividades](docs/diagramas-actividades.md) | Flujos de ejecución correspondientes a cada caso de uso. |
| [Matriz de trazabilidad](docs/matriz-trazabilidad.md) | Correspondencias entre requerimientos y artefactos. |

Todas las imágenes de los diagramas se almacenan en `docs/imagenes/`. Los documentos las muestran mediante enlaces relativos.

El código editable de los diagramas de secuencia se mantiene en `docs/fuentes/secuencia/`.

## Estructura del repositorio

```text
MIGAL-SmartOps/
├── README.md
├── CONTRIBUTING.md
├── requirements.txt
├── .gitignore
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
│   ├── fuentes/
│   │   └── secuencia/
│   └── imagenes/
├── scripts/
│   └── generar-secuencias.py
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

| Carpeta o archivo | Finalidad |
| --- | --- |
| `docs/` | Documentación técnica del sistema. |
| `docs/fuentes/` | Fuentes editables de los diagramas mantenidos en el repositorio. |
| `docs/fuentes/secuencia/` | Código de los diagramas de secuencia en sintaxis de SequenceDiagram.org. |
| `docs/imagenes/` | Imágenes utilizadas en la documentación. |
| `scripts/` | Herramientas de automatización del repositorio. |
| `requirements.txt` | Dependencias de Python para las herramientas generales del repositorio. |
| `backend/` | Backend principal. |
| `frontend/` | Aplicación web. |
| `servicio-python/` | Servicio especializado de OCR e IA. |
| `database/` | Archivos y migraciones de la base de datos. |
| `entregas/` | Informes y archivos correspondientes a cada hito. |
| `.github/` | Plantillas y configuración de colaboración en GitHub. |

La carpeta `.venv/` se crea localmente durante la preparación del entorno. No se sube al repositorio.

## Tecnologías definidas

| Componente | Tecnología |
| --- | --- |
| Frontend web | React |
| Backend principal | Java y Spring Boot |
| Servicios especializados | Python |
| Base de datos | PostgreSQL |
| Despliegue | Docker |

Las instrucciones de instalación, configuración y ejecución de la aplicación se mantienen en el README de cada componente.

El entorno descrito a continuación corresponde a las herramientas de documentación y no inicia la aplicación MIGAL SmartOps.

## Generación de diagramas de secuencia

El generador convierte los archivos de texto de SequenceDiagram.org en imágenes PNG.

### Requisitos

- Python 3.10 o posterior.
- Google Chrome instalado.
- Conexión a internet.

### Preparación del entorno

Abrir una terminal desde la raíz del repositorio, donde se encuentran `requirements.txt`, `docs/` y `scripts/`.

**macOS / Linux:**

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
```

**Windows:**

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Esta preparación se realiza una sola vez por computadora. Si cambia `requirements.txt`, volver a ejecutar el comando de instalación.

Selenium administra el controlador compatible con Chrome. La primera ejecución puede necesitar descargarlo.

### Preparación de las fuentes

Guardar cada diagrama en `docs/fuentes/secuencia/` con el siguiente formato:

```text
MIGAL-SEQ-CU-XX-descripcion.txt
```

Ejemplo:

```text
MIGAL-SEQ-CU-01-iniciar-sesion.txt
```

El archivo debe contener únicamente código compatible con SequenceDiagram.org. No incluir las marcas `@startuml`, `@enduml` ni delimitadores de bloques Markdown.

El generador actual se detiene si encuentra un archivo `.txt` vacío. Todos los archivos creados en esta carpeta deben tener contenido antes de ejecutarlo.

### Ejecución

**macOS / Linux:**

```sh
.venv/bin/python scripts/generar-secuencias.py
```

**Windows:**

```powershell
.\.venv\Scripts\python.exe scripts/generar-secuencias.py
```

El generador procesa todos los `.txt` de `docs/fuentes/secuencia/` y guarda las imágenes en `docs/imagenes/`.

La fuente y su imagen conservan el mismo nombre base:

```text
docs/fuentes/secuencia/MIGAL-SEQ-CU-01-iniciar-sesion.txt
docs/imagenes/MIGAL-SEQ-CU-01-iniciar-sesion.png
```

Si una imagen ya existe, se actualiza al ejecutar el generador. Guardar el archivo de texto no regenera automáticamente su imagen.

Eliminar una fuente no elimina automáticamente el PNG anterior.

### Visualización

Desde `docs/diagramas-secuencia.md`, enlazar cada imagen y su código:

```markdown
## CU-01: Iniciar sesión

![Diagrama de secuencia CU-01](imagenes/MIGAL-SEQ-CU-01-iniciar-sesion.png)

[Código fuente](fuentes/secuencia/MIGAL-SEQ-CU-01-iniciar-sesion.txt)
```

Para abrir la vista previa de Markdown en VS Code:

- macOS: `⌘ + Shift + V`.
- Windows / Linux: `Ctrl + Shift + V`.

Revisar que los textos, mensajes, participantes y fragmentos sean legibles. La generación de un PNG no verifica la corrección UML ni la coherencia con los demás documentos.

### Solución de problemas

| Problema | Comprobación |
| --- | --- |
| No se encuentra `python3` o `py` | Instalar Python y volver a abrir la terminal. |
| No se encuentra `requirements.txt` o el generador | Ejecutar los comandos desde la raíz del repositorio. |
| Aparece `No module named selenium` | Instalar las dependencias con el Python de `.venv`. |
| No se inicia Chrome o no carga la herramienta | Comprobar que Chrome esté instalado y que exista conexión a internet. |
| Se informa de un archivo vacío o nombre inválido | Completar la fuente o ajustar su nombre al estándar. |
| El diagrama no se representa correctamente | Revisar su sintaxis en SequenceDiagram.org. |
| La imagen no aparece en Markdown | Comprobar que el PNG exista y que su nombre coincida exactamente con el enlace. |

## Cómo contribuir

1. Consultar la tarea y su responsable en Notion.
2. Actualizar la copia local del repositorio desde `main`.
3. Crear una rama para el cambio.
4. Actualizar los archivos y artefactos relacionados.
5. Verificar el resultado.
6. Abrir un pull request hacia `main` y enlazar la tarea de Notion.
7. Obtener la revisión de otro integrante y resolver las observaciones.
8. Integrar el cambio y registrar la evidencia en Notion.

Las reglas completas se encuentran en [CONTRIBUTING.md](CONTRIBUTING.md).

## Equipo

- Alexander Diaz Ordoñez.
- Gabriel Gibson Martinez Arista.
- Brenda Nicole Mirano Flores.
- Luis Rodrigo Nuñez Principe.
- Diego Alonso Pinedo Aponte.

Las responsabilidades y fechas se mantienen en Notion. Cada integrante utiliza su propia cuenta de GitHub para conservar la autoría de sus contribuciones y revisiones.

## Versiones e informes

Los hitos se identifican mediante etiquetas de Git, por ejemplo `pc1-v1.0`.

Cada informe debe indicar la etiqueta o el commit utilizado como base. Los archivos correspondientes se conservan en `entregas/`, organizados por hito y versión.