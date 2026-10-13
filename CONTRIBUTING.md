# Reglas de contribución

Estas reglas se aplican a todos los integrantes y a los cambios de documentación, diagramas, código y base de datos de MIGAL SmartOps.

## 1. Fuentes oficiales

- `docs/` contiene la documentación técnica del sistema.
- `docs/imagenes/` contiene todas las imágenes de diagramas.
- `backend/`, `frontend/`, `servicio-python/` y `database/` contienen los componentes de implementación.
- `entregas/` contiene los informes y archivos de cada hito, organizados por entrega y versión.
- Notion contiene las tareas, responsables, fechas, observaciones y estados de seguimiento.

Los informes se elaboran desde una versión identificada del repositorio. Una corrección detectada en un informe debe incorporarse primero a la documentación oficial mediante una Pull Request y después reflejarse en el informe.

## 2. Asignación y coordinación

Cada tarea de Notion debe incluir:

- Descripción del resultado esperado.
- Responsable.
- Fecha objetivo.
- Criterios de aceptación.
- Requerimientos, CU y artefactos afectados.
- Enlace a la Pull Request como evidencia del resultado.

Antes de modificar un archivo, consultar el trabajo en curso y coordinar con quienes estén trabajando en el mismo contenido. Las decisiones sobre reglas compartidas deben quedar registradas en la tarea y reflejadas en la documentación correspondiente.

## 3. Ramas

`main` contiene la versión integrada del proyecto. Después de la carga inicial, todos los cambios se incorporan mediante Pull Requests.

Crear cada rama desde una copia actualizada de `main`. Utilizar nombres en minúsculas, sin espacios ni tildes y con palabras separadas por guiones.

| Prefijo | Uso | Ejemplo |
| --- | --- | --- |
| `docs/` | Documentación y diagramas | `docs/recepciones-parciales` |
| `feat/` | Funcionalidades nuevas | `feat/registro-ventas` |
| `fix/` | Corrección de errores del software | `fix/validacion-stock` |
| `refactor/` | Reorganización de código sin cambiar su comportamiento | `refactor/servicio-kardex` |
| `chore/` | Configuración y mantenimiento | `chore/configuracion-docker` |

Cada rama debe abordar un cambio concreto o un conjunto de cambios relacionados. No utilizar ramas permanentes por integrante.

## 4. Commits

Cada commit debe representar un avance coherente y explicar el cambio realizado.

Formato:

```text
tipo: descripcion concreta
```

Ejemplos:

```text
docs: alinear CU-13 con recepciones parciales
docs: actualizar cardinalidades del DER
feat: registrar ventas con salida de inventario
fix: impedir traslados con stock insuficiente
```

Evitar mensajes como `cambios`, `actualizado`, `final` o `arreglos`.

## 5. Pull Requests

Toda Pull Request debe utilizar la [plantilla del repositorio](.github/pull_request_template.md) e incluir:

1. Enlace a la tarea de Notion.
2. Explicación de qué cambió y por qué.
3. Identificación de los RF, CU, diagramas, clases, tablas o componentes afectados.
4. Actualizaciones de los artefactos relacionados.
5. Descripción de la verificación realizada.
6. Pendientes y colaboración de otros integrantes, cuando corresponda.

Una corrección que afecta varios artefactos debe presentarse de forma conjunta. Si requiere varias PR, registrar sus dependencias en Notion y mantener la tarea abierta hasta completar el conjunto.

Las propuestas incompletas pueden abrirse como borrador. Deben señalar claramente lo que falta antes de solicitar aprobación.

## 6. Revisión e integración

- Toda PR requiere la aprobación de al menos otro integrante.
- El revisor comprueba el contenido y su coherencia con los artefactos relacionados.
- El autor resuelve las observaciones antes de integrar.
- Si se agregan cambios después de la aprobación, el revisor debe comprobarlos antes de la integración.
- Las PR se integran mediante **Squash and merge**, con un título que describa el resultado.
- Después de integrar, se elimina la rama terminada y se registra la evidencia en Notion.
- Los demás integrantes actualizan su copia local antes de continuar el trabajo.

Estas reglas rigen para todo el equipo. Cuando el plan de GitHub lo permita, se configuran también como restricciones de `main`. Los archivos del repositorio no sustituyen esa configuración.

## 7. Documentación Markdown

- Utilizar nombres de archivo en minúsculas, sin espacios ni tildes y separados por guiones.
- Mantener un título principal por documento y organizar el contenido mediante encabezados.
- Incorporar un índice interno cuando la extensión del documento lo requiera.
- Utilizar enlaces relativos para documentos e imágenes del repositorio.
- Mantener los identificadores oficiales de requerimientos y CU.
- Definir las reglas de negocio en su sección correspondiente y referenciarlas desde los demás artefactos.
- Actualizar índices, enlaces y referencias cuando se incorpore o reorganice contenido.
- Registrar las observaciones pendientes en Notion; el documento debe expresar de forma clara la definición técnica acordada.

Los cambios de alcance, permisos, estados o reglas de negocio deben explicar la decisión y sus efectos sobre los demás entregables. Mantener los códigos existentes; cualquier cambio de identificación requiere actualizar todas sus referencias.

## 8. Imágenes y diagramas

Todas las imágenes se almacenan en una única carpeta:

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

Reglas de actualización:

- Conservar el nombre al actualizar el mismo diagrama.
- Utilizar el código oficial del CU cuando corresponda.
- Usar nombres sin espacios ni tildes; la descripción se escribe en minúsculas y con guiones.
- No incorporar nombres de integrantes, fechas ni sufijos como `final`, `final2` o `v2` al archivo vigente.
- Utilizar PNG o SVG y comprobar que textos, relaciones y símbolos sean legibles.
- Referenciar la imagen desde el Markdown del entregable correspondiente.
- Mantener una sola imagen vigente por identificador. Las alternativas se comparan en la tarea de Notion antes de seleccionar la versión que se integrará.
- Conservar la fuente editable en la herramienta compartida acordada y registrar su enlace en el Markdown correspondiente. Actualizar la fuente y su exportación dentro del mismo cambio.
- Comprobar que los demás documentos que muestran esa imagen sigan siendo coherentes con su contenido.

Desde un documento ubicado directamente en `docs/`, el enlace a una imagen se escribe así:

```markdown
![Secuencia CU-01](imagenes/MIGAL-SEQ-CU-01-iniciar-sesion.png)
```

El nombre identifica el artefacto; Git conserva su historial de versiones.

## 9. Coherencia y trazabilidad

Antes de solicitar revisión, comprobar la cadena:

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

Actualizar la matriz de trazabilidad cuando cambien las correspondencias entre artefactos. Si un entregable relacionado no requiere cambios, explicar el motivo en la PR cuando la relación no sea evidente.

## 10. Código y base de datos

- Mantener cada implementación en la carpeta del componente correspondiente.
- Documentar versiones, dependencias, configuración y ejecución en el README de cada componente.
- Actualizar la documentación cuando cambie el comportamiento descrito.
- Ejecutar las comprobaciones pertinentes antes de solicitar revisión y registrar sus resultados en la PR.
- Incorporar los cambios de esquema en `database/migraciones/`, siguiendo la herramienta y convención acordadas por el equipo.
- Utilizar datos de prueba para desarrollo.
- Mantener credenciales y configuración privada fuera del repositorio. Los ejemplos deben contener valores ficticios.

La incorporación de un componente nuevo debe actualizar la estructura y los enlaces del README principal.

## 11. Versiones e informes

Cada hito se identifica mediante una etiqueta de Git, por ejemplo `pc1-v1.0`. El informe correspondiente debe indicar la etiqueta o el commit utilizado como base.

Una versión publicada se conserva. Si requiere cambios, se crea otra versión y se identifica el nuevo estado del repositorio. La preparación del informe se realiza después de integrar las modificaciones técnicas necesarias.

Guardar los archivos en una carpeta identificada dentro de `entregas/`, por ejemplo `entregas/pc1/v1.0/`. Incluir un README con el hito, la versión, la fecha, los archivos incluidos y la etiqueta o commit utilizado.

## 12. Cierre de tareas y contribuciones

Una tarea se marca como terminada en Notion cuando:

- Cumple sus criterios de aceptación.
- Sus cambios están revisados e integrados.
- Los artefactos relacionados están actualizados.
- La evidencia está enlazada en Notion.

Las contribuciones incluyen elaboración, implementación, revisión y colaboración. Cada integrante utiliza su propia cuenta. La participación se valora mediante el trabajo aceptado y su evidencia, no únicamente mediante el número de commits.
