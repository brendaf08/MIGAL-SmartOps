# ADR-008: Ajustes al contrato de la API REST

## Contexto

La construcción de la base común del contrato OpenAPI y su contraste con el frontend, el diccionario de datos y los ADR 003 a 007 revelaron incoherencias y vacíos en el [ADR-006](adr-006-contrato-api.md). Deben resolverse antes de escribir los documentos de cada módulo, para que todos apliquen los mismos criterios.

## Decisión

1. **Estructura.** El contrato se organiza en `backend/api/componentes/` (elementos comunes) y `backend/api/modulos/` (un documento OpenAPI independiente por módulo). El documento único se genera con `redocly join` y no se edita a mano. `join` no conserva la seguridad global, por lo que cada operación declara su `security`.
2. **Enumeraciones.** Los valores de enumeración del contrato son los mismos que almacena la base de datos: minúsculas con guion bajo y sin acentos (`pendiente_abastecimiento`, `salida_confirmada`, `procesado_ocr`, `entrada`). Esto reemplaza el uso de PascalCase indicado en el ADR-006.
3. **Códigos de error.** El catálogo del ADR-006 es la única fuente. Se incorpora `ERROR_INTERNO` (500). El frontend sustituye `NO_ENCONTRADO` y `ORDEN_NO_ENCONTRADA` por `RECURSO_NO_ENCONTRADO`, que identifica el recurso en `detail`. Las extensiones del problema (`proveedorId`, `clienteId`) son propiedades opcionales de `Problema`.
4. **Cantidades.** Todas las cantidades admiten hasta tres decimales, conforme al diccionario de datos.
5. **Paginación.** Todos los listados que pueden crecer se paginan: clientes, productos, proveedores, órdenes de compra, recepciones, cotizaciones, ventas, guías de despacho, usuarios, documentos y movimientos. Los catálogos acotados (almacenes y categorías) devuelven la lista completa.
6. **Identificadores.** Todo recurso se identifica en la ruta con su `id` técnico. El SKU y los códigos de negocio son campos y filtros de búsqueda (`?sku=`), no claves de ruta. Productos y almacenes tienen `PUT /{recurso}/{id}/estado`, como los demás maestros.
7. **Identidad de servicio.** El servicio de OCR se autentica con `serviceAuth`, un token de servicio que no es un rol de usuario. Sus operaciones declaran `x-servicio` con el alcance requerido, no `x-roles`.
8. **Operaciones abiertas a todos los autenticados.** Declaran explícitamente los cuatro roles en `x-roles`.
9. **Validación de viabilidad.** La validación de MIGAL de una cotización aceptada la realiza un usuario con rol Administrador, que corresponde a la propietaria responsable del abastecimiento. No se crea un rol adicional. MIGAL también puede rechazar una cotización aceptada, como contempla el diccionario de datos, y el rechazo registra quién lo realizó.
10. **Traslados.** Se agrega `POST /inventario/traslados` (Administrador y Almacenero), que registra de forma atómica la salida y la entrada vinculadas por `referenciaTraslado`.
11. **Estados de la guía de despacho.** Son los del diccionario de datos: `preparacion`, `emitida`, `salida_confirmada` y `entregada`.

## Justificación

- La estructura de documentos independientes evita que todos los pull requests de módulo modifiquen un mismo archivo raíz.
- Alinear las enumeraciones con la base de datos elimina una traducción y sus errores, y se corresponde con las restricciones CHECK de las migraciones.
- Un catálogo único de errores permite al frontend manejar los errores sin tablas de equivalencia.
- Identificar siempre por `id` mantiene un único criterio de rutas y evita exponer datos de negocio mutables en las URL.
- Un token de servicio separado impide que el servicio de OCR adquiera permisos de un rol de usuario.
- Mantener al Administrador como validador respeta el modelo de cuatro roles (RF-USR-02) y la información disponible sobre la propietaria.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Documento raíz mantenido a mano | Todos los pull requests de módulo lo modificarían, con conflictos frecuentes. |
| Enumeraciones en PascalCase | Exige convertir valores entre el contrato y la base de datos. |
| Rol de usuario para el servicio de OCR | Mezcla identidades de máquina con roles de personas. |
| Rol propio de abastecimiento | Introduce un quinto rol que los requerimientos no definen. |
| SKU como clave de ruta | Rompe el criterio uniforme de identificación por `id`. |

## Consecuencias

**Favorables:**

- Los ocho módulos se escriben con los mismos criterios y en pull requests independientes.
- El contrato, la base de datos y el frontend comparten los mismos valores de enumeración.

**Desfavorables:**

- El frontend debe adaptar sus constantes de estado y de error, y sus rutas de producto, en la fase de integración. Se mitiga con el cliente tipado generado desde el contrato.
- Si la propietaria deja de ser Administrador, la validación requerirá un rol propio y un nuevo ADR.

## Referencias

- [ADR-003: Reglas de ventas, reservas, Kardex y despacho](adr-003-reglas-ventas-reservas-despacho.md)
- [ADR-006: Contrato de la API REST](adr-006-contrato-api.md)
- [ADR-007: Hosting de la base de datos en Supabase](adr-007-hosting-base-de-datos-supabase.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
