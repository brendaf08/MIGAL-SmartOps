# ADR-013: Rol de demostración con los permisos de los cuatro roles

## Contexto

El sistema define cuatro roles del negocio (Administrador, Vendedor, Almacenero y Reponedor) y el contrato asigna a cada operación los roles que pueden invocarla mediante `x-roles` (ADR-006, ADR-009). El Administrador conserva el alcance acordado: consulta los módulos y valida las operaciones que lo requieren, pero no registra ventas, recepciones, despachos ni órdenes de compra, que corresponden al rol operativo de cada caso de uso (CU-05 a CU-14).

Para presentar el sistema completo hacía falta iniciar sesión con cuatro cuentas distintas. La alternativa de ampliar los permisos del Administrador cambia el modelo de roles del contrato y de los casos de uso, y deja de reflejar la separación de funciones que exige el negocio.

## Decisión

Se agrega el rol `demostracion`, que no es un rol del negocio:

1. **Permisos:** reúne los de los cuatro roles. El backend le otorga las autoridades `ROLE_ADMINISTRADOR`, `ROLE_VENDEDOR`, `ROLE_ALMACENERO` y `ROLE_REPONEDOR` (además de `ROLE_DEMOSTRACION`), de modo que cada `@PreAuthorize` existente lo admite sin enumerarlo. Las reglas que dependen del rol (rechazo y validación de cotizaciones, aprobación de ventas, tipos de documento OCR y herramientas del Agente de IA) lo tratan como la suma de los cuatro.
2. **Desactivado por defecto:** solo funciona si `MIGAL_ROL_DEMOSTRACION=true`. Con el indicador apagado, la cuenta no puede iniciar sesión y sus tokens vigentes se rechazan.
3. **Creación:** la cuenta se crea al arrancar a partir de `MIGAL_DEMO_USERNAME` y `MIGAL_DEMO_CLAVE`, del mismo modo que el Administrador inicial. La API de usuarios no puede asignarlo (`RolAsignable` del contrato) y la interfaz no permite modificar su rol; el Administrador sí puede revocar su acceso.
4. **Persistencia:** la migración V18 agrega `demostracion` a la restricción `ck_usuario_rol`.
5. **Contrato:** `RolAcceso` incluye `demostracion` solo en respuestas; `x-roles` sigue listando únicamente los roles del negocio.
6. **Despliegue de demostración:** `scripts/levantar-demo.ps1` habilita el rol y genera la cuenta `demo` con una clave aleatoria cuando crea el `.env`.

## Justificación

- Mantiene el Administrador y la matriz de permisos tal como los definen el contrato y los casos de uso, por lo que las pruebas de autorización por rol siguen siendo válidas.
- Facilita la presentación del sistema sin crear cuatro cuentas ni cambiar de sesión entre módulos.
- Al estar apagado por defecto y no poder asignarse por la API, no amplía la superficie de acceso de un despliegue de uso real.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Dar al Administrador todos los permisos | Cambia el alcance del rol acordado en los casos de uso y el contrato, y elimina la separación de funciones. |
| Crear cuatro cuentas de demostración | Obliga a cerrar e iniciar sesión en cada módulo durante la presentación. |
| Agregar `demostracion` a cada `x-roles` | Duplica la matriz en las más de noventa operaciones que lo declaran y la hace propensa a omisiones. |

## Consecuencias

**Favorables:**

- Una sola cuenta recorre todos los módulos y flujos.
- El modelo de roles del negocio no cambia.

**Desfavorables:**

- Una cuenta con todos los permisos es sensible. Se mitiga con el indicador apagado por defecto, la creación solo desde el entorno y la posibilidad de revocar su acceso.
- Las operaciones que registra la cuenta de demostración figuran a su nombre en el historial, no al de un rol del negocio.

## Referencias

- [ADR-006](adr-006-contrato-api.md)
- [ADR-009](adr-009-ajustes-seguridad-y-componentes-contrato.md)
- [Contrato de autenticación y usuarios](../../backend/api/modulos/autenticacion-usuarios.yaml)
- [Migración V18](../../database/migraciones/V18__rol_demostracion.sql)
