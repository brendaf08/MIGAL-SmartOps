# ADR-009: Ajustes de seguridad, sesión y componentes del contrato

## Contexto

La redacción de los contratos de seis módulos (autenticación y usuarios, catálogo, inventario, comercial, ventas y despacho) evidenció decisiones pendientes que los módulos restantes deben aplicar de forma uniforme. Complementa el [ADR-008](adr-008-ajustes-contrato-api.md) y el [ADR-006](adr-006-contrato-api.md). Los vacíos del modelo de datos que los contratos dejaron al descubierto se resuelven en migraciones aparte (V11 y V12).

## Decisión

1. **Roles.** Los roles se escriben en minúsculas, igual que la columna `usuario.rol` y el CHECK de la base de datos: `administrador`, `vendedor`, `almacenero` y `reponedor`. Rigen para `x-roles` y para la reclamación de rol del JWT.
2. **Sesión.** El JWT de acceso dura 8 horas, equivalente a una jornada. `SesionIniciada` informa su expiración. No se define renovación, cierre de sesión en el servidor ni cambio de contraseña en la versión 1: el frontend descarta el token al salir. El backend verifica el estado del usuario en cada solicitud, por lo que desactivar a un usuario cierra su acceso de inmediato.
3. **Protección del administrador.** No se puede desactivar a un usuario si es el único Administrador activo, ni desactivarse a uno mismo (`ADMINISTRADOR_REQUERIDO`). Una cuenta inactiva que inicia sesión recibe `CUENTA_INACTIVA` (403). Se agrega `GET /usuarios/{id}`.
4. **Catálogo de errores.** Se incorporan `CUENTA_INACTIVA`, `ADMINISTRADOR_REQUERIDO`, `CATEGORIA_NO_ENCONTRADA`, `PRODUCTO_INACTIVO`, `ALMACEN_INACTIVO`, `MOVIMIENTO_NO_CORREGIBLE`, `TRANSICION_NO_PERMITIDA` y `COTIZACION_NO_VENCIDA`. Cubrir una venta cerrada o sin faltantes responde 200 sin cambios y no tiene código de error.
5. **Despacho.** La guía se emite con `POST /guias-despacho` (no `POST /ventas/{id}/guias-despacho`). Al emitirla, la venta pasa de `pendiente_despacho` a `lista_para_despacho`. La cancelación de una venta lista para despacho anula su guía, que pasa al estado `anulada`.
6. **Idempotencia.** `Idempotency-Key` es obligatoria en las operaciones que afectan inventario o cambian un estado crítico: movimientos, traslados, confirmación de recepción y de salida, y las transiciones de cotización y venta. La emisión de la guía no la requiere, porque la unicidad de la guía por venta impide duplicados.
7. **Rechazo de cotizaciones.** El cliente y MIGAL rechazan con el mismo endpoint, y el servidor impide que un Vendedor rechace en nombre de MIGAL. El rechazo registra origen, usuario, fecha y motivo.
8. **Venta directa.** La cotización creada vence en la fecha de la operación. Cuando los productos mezclan categorías de aprobación automática y manual, la operación exige el rol Administrador. Al reservar, se prefiere la tienda principal y luego los almacenes por identificador.
9. **Consulta del Kardex.** Los filtros son opcionales y se corrige CU-07, que los declaraba obligatorios.
10. **Códigos de negocio.** Los códigos legibles (`V-0038`, `OC-0087`) son columnas generadas a partir del identificador y se exponen en el campo `codigo`. La clave de ruta sigue siendo el `id`.
11. **Componentes comunes.** Se agregan los esquemas `CantidadPositiva`, `CantidadNoNegativa` y `ActualizacionEstado`, los parámetros `sku`, `ubicacionId` y `estadoActivo`, y la descripción de la cabecera `IdempotentReplayed`. Los módulos nuevos los utilizan; los existentes pueden migrar de forma gradual.

## Justificación

- Un solo valor de rol evita traducciones entre el contrato, el JWT y la base de datos.
- Una duración de una jornada limita el riesgo de un token robado sin obligar a reiniciar sesión durante el día. Verificar el estado en cada solicitud compensa la ausencia de renovación y de cierre de sesión en el servidor.
- Los códigos de error específicos permiten al frontend mostrar mensajes útiles sin interpretar texto.
- Anular la guía de una venta cancelada evita documentos de despacho huérfanos.
- Las columnas generadas evitan secuencias adicionales y mantienen el código determinista.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Roles en PascalCase en el contrato | Obliga a convertirlos en cada capa. |
| Token de corta duración con renovación | Añade endpoints y almacenamiento de renovación que la versión 1 no necesita. |
| Impedir cancelar una venta con guía emitida | Contradice las transiciones del modelo de datos y obliga a un paso manual previo. |
| Secuencias por tipo de documento para los códigos | Introduce estado adicional y huecos sin aportar valor. |

## Consecuencias

**Favorables:**

- Los módulos restantes se escriben con criterios uniformes.
- El modelo de datos, el contrato y el frontend usan los mismos valores.

**Desfavorables:**

- Quien pierda el token debe iniciar sesión de nuevo y no hay cambio de contraseña en la versión 1. Se mitiga con el restablecimiento por el Administrador, que se evaluará en una versión posterior.
- El frontend debe adaptar roles, estados y códigos durante la integración.

## Referencias

- [ADR-003: Reglas de ventas, reservas, Kardex y despacho](adr-003-reglas-ventas-reservas-despacho.md)
- [ADR-006: Contrato de la API REST](adr-006-contrato-api.md)
- [ADR-008: Ajustes al contrato de la API REST](adr-008-ajustes-contrato-api.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
