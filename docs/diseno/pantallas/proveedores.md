# Pantallas de proveedores

Este documento describe las pantallas del módulo `proveedores` (CU-11, rol Reponedor): la lista de proveedores (P06) y el formulario de registro, edición y estado (P07). Corresponde a la rama `feat/pantallas-proveedores`.

## Lista de proveedores (P06)

Ruta: `/proveedores`. Archivo: `src/features/abastecimiento/ProveedoresPage.tsx`.

Estructura:

1. Encabezado "Proveedores", con la acción "Registrar proveedor" a la derecha. La acción navega a `/proveedores/nuevo`.
2. Campo de búsqueda "Razón social o RUC", con espera de 300 ms antes de consultar.
3. Pestañas "Todos", "Activos" e "Inactivos", que filtran por estado.
4. Tabla con las columnas Razón social (principal), RUC, Contacto, Productos (cantidad de SKU asociados, solo en escritorio) y Estado (insignia de registro).

Interacción: el clic en una fila navega a `/proveedores/:id`.

Estados: carga con esqueletos; error con aviso "No se pudieron cargar los proveedores"; sin coincidencias con el estado vacío de la tabla.

Restricción por rol: el módulo está disponible solo para el rol Reponedor. Un rol distinto es redirigido a la página de inicio.

## Registro, edición y estado (P07)

Rutas: `/proveedores/nuevo` (registro) y `/proveedores/:id` (edición). Archivos: `src/features/abastecimiento/proveedores/ProveedorFormPage.tsx`, `ProveedorFormPage.module.css` y `esquema.ts`.

Estructura, con `FormPage` y tres tarjetas:

| Tarjeta | Campos |
| --- | --- |
| Datos del proveedor | Razón social (obligatoria); RUC (obligatorio, 11 dígitos, inicio 10 o 20). |
| Contacto y condiciones | Persona de contacto (obligatoria); teléfono (9 dígitos, opcional); correo electrónico (formato válido, opcional); condiciones comerciales (obligatorias). |
| Productos que suministra | Selector múltiple `MultiSelect` con búsqueda por SKU o nombre, alimentado por el catálogo vigente. |

En edición se muestra además la tarjeta "Estado del proveedor", con la insignia de estado y la acción que corresponde: "Desactivar proveedor" si está activo, o "Activar proveedor" si está inactivo.

Acciones al pie: "Cancelar" (vuelve a la lista) y "Registrar proveedor" o "Guardar cambios".

### Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| Razón social | Obligatoria | Ingresar la razón social. |
| RUC | 11 dígitos | El RUC debe tener 11 dígitos. |
| RUC | Inicio 10 o 20 | El RUC debe empezar con 10 o 20. |
| Persona de contacto | Obligatoria | Ingresar la persona de contacto. |
| Teléfono | Vacío o 9 dígitos | El teléfono debe tener 9 dígitos. |
| Correo electrónico | Vacío o formato válido | Ingresar un correo válido. |
| Condiciones comerciales | Obligatorias | Ingresar las condiciones comerciales. |

Los mensajes aparecen junto al campo. El foco pasa al primer campo inválido al enviar.

### Flujos alternativos

1. RUC duplicado: el servicio rechaza el registro con `RucDuplicadoError`. El campo RUC muestra "Este RUC ya está registrado." y aparece el enlace "Modificar el proveedor existente", que apunta a la ficha del proveedor que ya usa el RUC.
2. SKU inexistente: si la búsqueda del selector no coincide con ningún producto, se muestra el aviso "El producto no está en el catálogo" con el enlace "Dar de alta en el catálogo" (`/catalogo/nuevo`) y la acción "Continuar sin asociar", que limpia la búsqueda y permite guardar sin productos asociados.
3. Proveedor inactivo: se muestra la alerta de advertencia "Este proveedor está inactivo y no puede usarse en nuevas órdenes de compra". Todos los campos quedan deshabilitados, la acción principal queda inhabilitada con el motivo "Activar el proveedor para modificarlo." y la acción de estado es "Activar proveedor".

### Confirmaciones

Todo cambio de estado pasa por `ConfirmDialog`:

- Desactivar: título "Desactivar a {razón social}", tono de peligro, consecuencias "El proveedor no podrá usarse en nuevas órdenes de compra" y "Sus datos y sus órdenes existentes se conservan". Confirmación "Desactivar proveedor"; cancelación "Cancelar".
- Activar: título "Activar a {razón social}", consecuencia "El proveedor podrá usarse en nuevas órdenes de compra". Confirmación "Activar proveedor".

Tras confirmar, la insignia de estado se actualiza y se muestra una notificación de resultado.

### Estados de carga y error

- Ficha inexistente: estado vacío "Proveedor no encontrado" con enlace de regreso a la lista.
- Guardado correcto: notificación de éxito y regreso a la lista.
- Error no previsto: notificación de error con el mensaje del servicio.

## Servicio simulado

Archivo: `src/features/abastecimiento/proveedores/api.ts`.

| Función | Comportamiento |
| --- | --- |
| `listarProveedores(filtro)` | Filtra por razón social o RUC y por estado. |
| `obtenerProveedor(id)` | Devuelve la ficha o falla con `NO_ENCONTRADO`. |
| `registrarProveedor(datos)` | Asigna el identificador `P-nnn` y el estado Activo; falla con `RucDuplicadoError` si el RUC existe. |
| `actualizarProveedor(id, datos)` | Modifica la ficha; falla con `RucDuplicadoError` si el RUC pertenece a otro proveedor. |
| `cambiarEstadoProveedor(id, estado)` | Cambia el estado del proveedor. |

Las funciones modifican en memoria el arreglo `PROVEEDORES` del módulo.

## Diferencias respecto a los requerimientos

- El teléfono se trata como opcional, porque la especificación indica su formato, pero no su carácter obligatorio. Queda pendiente confirmarlo.
- La tarjeta "Estado del proveedor" se agrega a la estructura de tres tarjetas, solo en edición, para alojar las acciones de activación y desactivación, que el formulario no admite en su barra de acciones.
- La pestaña "Órdenes de compra" se retira de la página de proveedores y pasa al módulo `compras`.
