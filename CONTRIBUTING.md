# Reglas de contribución

Estas reglas se aplican a todos los integrantes y a los cambios de documentación, diagramas, código y base de datos de MIGAL SmartOps.

## 1. Organización y fuentes oficiales

| Ubicación | Contenido |
| --- | --- |
| `docs/` | Definiciones técnicas del sistema. |
| `docs/fuentes/` | Fuentes editables de diagramas mantenidos en el repositorio. |
| `docs/imagenes/` | Imágenes utilizadas en la documentación. |
| `scripts/` | Herramientas de automatización. |
| `backend/`, `frontend/`, `servicio-python/` y `database/` | Componentes de implementación. |
| `entregas/` | Informes y archivos de cada hito. |
| Notion | Tareas, responsables, fechas, observaciones y seguimiento. |

Los informes se elaboran desde una versión identificada del repositorio.

Una corrección técnica detectada durante la preparación de un informe debe incorporarse a la documentación correspondiente mediante un pull request y reflejarse también en el informe.

## 2. Asignación y coordinación

Cada tarea de Notion debe incluir:

- Resultado esperado.
- Responsable.
- Fecha objetivo.
- Criterios de aceptación.
- Requerimientos, casos de uso y artefactos afectados.
- Enlace al pull request como evidencia del resultado.

Antes de modificar un archivo, consultar las tareas en curso y coordinar con quienes estén trabajando en el mismo contenido.

Las decisiones sobre alcance, permisos, estados y reglas de negocio deben quedar registradas en la tarea y reflejadas en la documentación correspondiente.

## 3. Ramas

`main` contiene la versión integrada del proyecto. Los cambios se incorporan mediante pull requests.

Crear cada rama desde una copia actualizada de `main`. Utilizar nombres en minúsculas, sin espacios ni tildes y con palabras separadas por guiones.

| Prefijo | Uso | Ejemplo |
| --- | --- | --- |
| `docs/` | Documentación y diagramas | `docs/secuencias-autenticacion` |
| `feat/` | Funcionalidades nuevas | `feat/registro-ventas` |
| `fix/` | Corrección de errores del software | `fix/validacion-stock` |
| `refactor/` | Reorganización de código sin cambiar su comportamiento | `refactor/servicio-kardex` |
| `chore/` | Configuración y mantenimiento | `chore/generador-secuencias` |

Cada rama debe abordar un cambio concreto o un conjunto de cambios relacionados. No utilizar ramas permanentes por integrante.

## 4. Commits

Cada commit debe representar un avance coherente y explicar el cambio realizado.

Formato:

```text
tipo: descripcion concreta
```

Ejemplos:

```text
docs: actualizar secuencia del CU-01
docs: alinear CU-13 con recepciones parciales
feat: registrar ventas con salida de inventario
fix: impedir traslados con stock insuficiente
chore: configurar generador de secuencias
```

Evitar mensajes como `cambios`, `actualizado`, `final` o `arreglos`.

## 5. Pull requests

Cada pull request debe incluir:

1. Enlace a la tarea de Notion.
2. Explicación de qué cambió y por qué.
3. Identificación de los requerimientos, casos de uso y artefactos afectados.
4. Actualizaciones de los archivos relacionados.
5. Descripción de la verificación realizada.
6. Pendientes o dependencias, cuando corresponda.

Utilizar la plantilla del repositorio cuando esté disponible.

Una corrección que afecta varios artefactos debe presentarse de forma conjunta. Si requiere varios pull requests, registrar sus dependencias en Notion y mantener la tarea abierta hasta completar el conjunto.

Los cambios incompletos pueden abrirse como borrador. Deben indicar lo que falta antes de solicitar aprobación.

## 6. Revisión e integración

- Todo pull request requiere la aprobación de al menos otro integrante.
- El revisor comprueba el contenido y su coherencia con los artefactos relacionados.
- El autor resuelve las observaciones antes de integrar.
- Si se agregan cambios después de la aprobación, deben revisarse antes de la integración.
- Los cambios se integran mediante **Squash and merge**, con un título que describa el resultado.
- Después de integrar, se elimina la rama terminada y se registra la evidencia en Notion.
- Los demás integrantes actualizan su copia local antes de continuar el trabajo.

La exigencia técnica de pull requests y aprobaciones debe configurarse en las reglas de protección de `main` en GitHub. Este documento establece el procedimiento del equipo, pero no activa esas restricciones por sí mismo.

## 7. Documentación Markdown

- Utilizar nombres de archivo en minúsculas, sin espacios ni tildes y separados por guiones.
- Mantener un título principal por documento y una jerarquía coherente de encabezados.
- Incorporar un índice interno cuando la extensión lo requiera.
- Utilizar enlaces relativos para documentos e imágenes del repositorio.
- Mantener los identificadores oficiales de requerimientos y casos de uso.
- Referenciar las reglas de negocio desde los artefactos que las utilizan.
- Actualizar índices, enlaces y referencias cuando se incorpore o reorganice contenido.
- Revisar las tablas y las imágenes mediante la vista previa.
- Registrar las observaciones pendientes en Notion.

Los documentos deben expresar las definiciones técnicas acordadas.

Los cambios de alcance, permisos, estados o reglas de negocio deben explicar la decisión y sus efectos sobre los demás artefactos. Si cambia un identificador, actualizar todas sus referencias.

## 8. Imágenes y nomenclatura

Todas las imágenes se almacenan en:

```text
docs/imagenes/
```

Formato del nombre:

```text
MIGAL-TIPO-IDENTIFICADOR-descripcion.extension
```

| Tipo | Significado | Ejemplo |
| --- | --- | --- |
| `ARQ` | Arquitectura | `MIGAL-ARQ-01-arquitectura-tecnologica.png` |
| `DER` | Entidad-relación | `MIGAL-DER-01-modelo-entidad-relacion.png` |
| `CLA` | Clases | `MIGAL-CLA-01-diagrama-clases.png` |
| `DCU` | Casos de uso | `MIGAL-DCU-01-usuarios-roles.png` |
| `SEQ` | Secuencia | `MIGAL-SEQ-CU-01-iniciar-sesion.png` |
| `ACT` | Actividades | `MIGAL-ACT-CU-01-iniciar-sesion.png` |
| `DFD` | Flujo de datos | `MIGAL-DFD-N0-contexto.png` |
| `PAQ` | Paquetes | `MIGAL-PAQ-01-componentes-y-capas.png` |

Reglas:

- Conservar el nombre al actualizar el mismo diagrama.
- Utilizar el código oficial del caso de uso cuando corresponda.
- Mantener las siglas y los identificadores en mayúsculas.
- Escribir la descripción en minúsculas, sin espacios ni tildes y separada por guiones.
- No incorporar nombres de integrantes, fechas ni sufijos como `final`, `final2` o `v2` al archivo vigente.
- Utilizar PNG o SVG y comprobar la legibilidad.
- Mantener una imagen vigente por identificador.
- Referenciar cada imagen desde el documento correspondiente.
- Actualizar los enlaces al renombrar o mover archivos.

Desde un documento ubicado directamente en `docs/`, el enlace se escribe así:

```markdown
![Secuencia CU-01](imagenes/MIGAL-SEQ-CU-01-iniciar-sesion.png)
```

Git conserva el historial de versiones de cada archivo.

## 9. Fuentes editables de diagramas

Conservar la fuente editable de cada diagrama.

Cuando se mantenga en el repositorio, guardarla en `docs/fuentes/`, organizada por tipo de diagrama. Cuando se mantenga en una herramienta compartida, registrar su enlace en el documento correspondiente.

Actualizar la fuente y su imagen dentro del mismo cambio. Evitar conservar únicamente una exportación que el equipo no pueda modificar.

### 9.1. Diagramas de secuencia

Las fuentes se almacenan en:

```text
docs/fuentes/secuencia/
```

Utilizar archivos `.txt` con sintaxis compatible con SequenceDiagram.org.

No incluir `@startuml`, `@enduml` ni delimitadores de bloques Markdown.

El nombre base de la fuente debe coincidir con el de su imagen:

```text
MIGAL-SEQ-CU-01-iniciar-sesion.txt
MIGAL-SEQ-CU-01-iniciar-sesion.png
```

Procedimiento:

1. Editar o agregar la fuente.
2. Guardar el archivo.
3. Ejecutar `scripts/generar-secuencias.py` siguiendo las instrucciones del README.
4. Revisar el PNG y su visualización en Markdown.
5. Incluir la fuente y la imagen actualizada en el mismo pull request.

Las imágenes generadas no se editan directamente.

El generador actual procesa todos los `.txt` de la carpeta y se detiene si encuentra archivos vacíos. Antes de ejecutarlo, completar todas las fuentes presentes.

Si se elimina o renombra una fuente, revisar y retirar las imágenes anteriores que ya no correspondan.

En el documento de secuencias, enlazar también el código editable:

```markdown
[Código fuente](fuentes/secuencia/MIGAL-SEQ-CU-01-iniciar-sesion.txt)
```

### 9.2. Diagramas de paquetes

Las fuentes se almacenan en:

```text
docs/fuentes/paquetes/
```

Utilizar archivos `.puml` con sintaxis de PlantUML, delimitados por `@startuml` y `@enduml`.

El nombre base de la fuente debe coincidir con el de su imagen:

```text
MIGAL-PAQ-01-componentes-y-capas.puml
MIGAL-PAQ-01-componentes-y-capas.png
```

Procedimiento:

1. Editar o agregar la fuente.
2. Exportar el PNG con la extensión PlantUML de VS Code o el servidor de PlantUML y guardarlo en `docs/imagenes/`.
3. Revisar el PNG y su visualización en `docs/diagramas-paquetes.md`.
4. Incluir la fuente, la imagen y el documento actualizados en el mismo pull request.

Los elementos de cada paquete deben corresponder a las clases del modelo de clases vigente.

### 9.3. Diagramas de actividades y otros modelos

Mantener su fuente editable y exportación mediante la herramienta acordada para cada tipo de diagrama.

El generador de secuencias no procesa diagramas de actividades, DER, clases ni otros modelos.

## 10. Coherencia y trazabilidad

Antes de solicitar revisión, comprobar la correspondencia entre:

**Requerimientos → Casos de uso → Especificaciones → Secuencias y actividades → Clases → Modelo de datos → Arquitectura.**

Verificar especialmente:

- Actores y permisos.
- Nombres e identificadores.
- Estados y transiciones.
- Precondiciones y postcondiciones.
- Flujos principales y alternativos.
- Operaciones, parámetros y retornos.
- Entidades, atributos, relaciones y cardinalidades.
- Origen y efecto de las operaciones sobre el inventario.
- Responsabilidades de los componentes y capas.

Actualizar la matriz de trazabilidad cuando cambien las correspondencias.

Si un artefacto relacionado no requiere modificaciones, explicar el motivo en el pull request cuando la relación no sea evidente.

La generación automática de una imagen no reemplaza la revisión técnica del modelo.

## 11. Código, dependencias y base de datos

- Mantener cada implementación en la carpeta de su componente.
- Documentar versiones, dependencias, configuración y ejecución en el README correspondiente.
- Actualizar la documentación cuando cambie el comportamiento descrito.
- Ejecutar las comprobaciones pertinentes y registrar sus resultados en el pull request.
- Incorporar los cambios de esquema en `database/migraciones/`, siguiendo la herramienta y convención acordadas.
- Utilizar datos de prueba para desarrollo.
- Mantener credenciales y configuración privada fuera del repositorio.
- Utilizar valores ficticios en los archivos de ejemplo.

El `requirements.txt` de la raíz contiene las dependencias de las herramientas generales de Python del repositorio.

Las dependencias del servicio Python se mantienen en su componente, según su configuración de ejecución.

Cuando cambien las dependencias generales:

1. Actualizar `requirements.txt`.
2. Verificar la herramienta afectada.
3. Actualizar las instrucciones del README si cambia la preparación o ejecución.

No subir `.venv/`, dependencias instaladas, archivos temporales, respaldos locales ni archivos operativos excluidos por `.gitignore`.

## 12. Versiones e informes

Cada hito se identifica mediante una etiqueta de Git, por ejemplo `pc1-v1.0`.

El informe correspondiente debe indicar la etiqueta o el commit utilizado como base.

Una versión publicada se conserva. Si requiere cambios, se crea otra versión y se identifica el nuevo estado del repositorio.

Guardar los archivos en una carpeta dentro de `entregas/`, por ejemplo:

```text
entregas/pc1/v1.0/
```

Incluir un README que identifique:

- Hito.
- Versión.
- Fecha.
- Archivos incluidos.
- Etiqueta o commit utilizado.

## 13. Cierre de tareas y contribuciones

Una tarea se marca como terminada en Notion cuando:

- Cumple sus criterios de aceptación.
- Sus cambios están revisados e integrados.
- Los artefactos relacionados están actualizados.
- La evidencia está enlazada en Notion.

Las contribuciones incluyen elaboración, implementación, revisión y colaboración.

Cada integrante utiliza su propia cuenta de GitHub. La participación se valora mediante el trabajo aceptado y su evidencia, no únicamente mediante el número de commits.