# Pantallas de usuarios y roles

Este documento describe las pantallas del módulo `usuarios` (CU-02, requerimientos RF-USR-01 a RF-USR-03 y RNF-09). El rol Administrador es el único con acceso a este módulo. Las pantallas se implementan en `frontend/src/features/seguridad/`.

## Lista de usuarios (P02)

Ruta: `/usuarios`.

### Estructura

1. Encabezado "Usuarios y roles", con la acción principal "Registrar usuario", que navega a `/usuarios/nuevo`.
2. Pestañas de filtro por estado: "Todos", "Activos" e "Inactivos", con el recuento de cada una.
3. Campo de búsqueda "Buscar usuario", que filtra por nombre o usuario con una espera de 300 ms.
4. Tabla "Usuarios" con las columnas Nombre (principal), Usuario (monoespaciada), Rol, Estado y Acciones.

### Acciones por fila

- "Modificar rol": enlaza a `/usuarios/:id`.
- "Revocar acceso": se muestra solo para usuarios activos. Abre el diálogo de confirmación descrito más adelante.
- Un usuario activo que coincide con la sesión iniciada conserva el botón "Revocar acceso" deshabilitado, con el motivo "No se puede revocar la propia cuenta" visible y en el atributo `title`.

### Estados

- Carga: esqueletos en la tabla.
- Vacío: el estado "No hay usuarios que coincidan", con la indicación de cambiar la búsqueda o el filtro.
- Error: aviso "No se pudieron cargar los usuarios" con la indicación de verificar la conexión.

## Formulario de usuario (P03)

Rutas: `/usuarios/nuevo` (registro) y `/usuarios/:id` (edición de rol). Se utiliza el componente `FormPage` con dos tarjetas: "Datos personales" y "Acceso".

### Campos y validaciones

| Campo | Modo | Regla | Mensaje |
| --- | --- | --- | --- |
| Nombre completo | Registro y edición | Obligatorio, mínimo 3 caracteres tras recortar espacios. | "Ingresar el nombre completo, con al menos 3 caracteres." |
| Usuario | Registro | Obligatorio, de 4 a 20 caracteres, solo minúsculas, números y los signos `.`, `_` y `-`. | "Ingresar un usuario de 4 a 20 caracteres." o "Usar solo minúsculas, números y los signos . _ -, sin espacios." |
| Usuario | Edición | Solo lectura. | No aplica. |
| Contraseña temporal | Registro | Obligatoria, mínimo 8 caracteres, con al menos una letra y un número. | "La contraseña temporal debe tener al menos 8 caracteres.", "... debe incluir una letra." o "... debe incluir un número." |
| Rol | Registro y edición | Obligatorio; valores de `ROLES`. | "Seleccionar un rol." |

Las validaciones usan un esquema Zod por modo (`esquema.ts`). El resolutor conserva el primer mensaje de cada campo.

### Flujos

- Registro correcto: se crea el usuario con estado Activo, se muestra la notificación "Usuario registrado." y se vuelve a la lista.
- Usuario duplicado: el error aparece junto al campo Usuario con el texto "El usuario ya está registrado. Ingresar uno distinto.". Los demás datos se conservan.
- Edición: se modifican el nombre y el rol; el usuario y la contraseña no cambian. Se muestra la notificación "Usuario actualizado." y se vuelve a la lista.
- Cancelar: vuelve a la lista sin guardar cambios.

## Revocación de acceso (P04)

Diálogo `ConfirmDialog` abierto desde la lista.

- Título: "Revocar acceso de {usuario}".
- Consecuencias: "El usuario no podrá iniciar sesión.", "Su historial de movimientos se conserva." y "El acceso puede restituirse editando al usuario.".
- Acción de confirmación: "Revocar acceso", con tono de peligro. La acción de cancelar enfoca el primer control del diálogo.

Al confirmar, el estado del usuario pasa a Inactivo, la fila se actualiza al refrescar la consulta y se muestra la notificación "Acceso revocado a {usuario}.". No se recarga la página.

## Restricción por rol

Las rutas del módulo son accesibles solo para el rol Administrador. Un Almacenero que intenta acceder a `/usuarios` o `/usuarios/nuevo` se redirige al inicio.

## Diferencias respecto a los requerimientos

- La autorización real la aplica el backend. En esta versión, la sesión simulada identifica al usuario que inicia sesión por su nombre; la comparación para bloquear la revocación propia usa ese nombre, dado que el nombre es único en los datos de prueba.
- La contraseña temporal se captura en el formulario de registro. El mecanismo de cambio obligatorio en el primer inicio de sesión depende del backend y no se simula.
- El acceso revocado se restituye editando al usuario; en esta versión no existe una acción específica de restitución.
- Los datos se modifican en memoria; la persistencia depende de la API del backend.
