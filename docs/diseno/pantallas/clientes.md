# Pantallas de clientes

Este documento describe la pantalla de lista de clientes (P22), el diálogo de registro y modificación, y el selector de cliente que reutilizan las cotizaciones y las ventas. Corresponde a la etapa A del plan 08 (CU-10, rol Vendedor).

## Estructura de la pantalla

Lista de clientes (`/clientes`):

1. Encabezado: título "Clientes", descripción y acción principal "Registrar cliente" a la derecha.
2. Campo de búsqueda "Buscar cliente" con marcador "Nombre o documento".
3. Pestañas "Activos" e "Inactivos", con el recuento de cada una.
4. Tabla "Clientes" con las columnas Nombre, Documento (fuente monoespaciada), Teléfono, Correo y Estado. Un cliente sin teléfono o correo muestra un guion.

Diálogo de registro o modificación (`ClienteFormModal`):

1. Campos: Tipo de documento (DNI o RUC), Documento, Nombre o razón social, Teléfono (opcional), Correo (opcional).
2. Pie: "Desactivar cliente" (solo al modificar un cliente activo), "Cancelar" y la acción principal "Registrar cliente" o "Guardar cambios".

Selector de cliente (`ClientePicker`), usado en cotizaciones y ventas:

1. Campo "Cliente" con búsqueda por nombre o documento.
2. Primera opción "Continuar sin cliente".
3. Clientes inactivos deshabilitados, con la causa en la etiqueta.
4. Botón "Registrar cliente nuevo", que abre el mismo diálogo y deja seleccionado el cliente guardado.

En teléfono, las acciones del diálogo se apilan a ancho completo, con la principal arriba. La tabla se presenta en fichas mediante `DataTable`.

## Estados

| Estado | Comportamiento |
| --- | --- |
| Cargando | Filas esqueleto en la tabla. |
| Error de carga | Aviso "No se pudieron cargar los clientes" con la indicación de verificar la conexión. |
| Sin resultados | Estado vacío de la tabla, con el texto "Sin resultados". |
| Documento repetido | Error en el campo Documento y aviso con la opción "Seleccionar cliente existente" (solo en el selector). |
| Cliente desactivado | Aparece en la pestaña Inactivos con la insignia "Inactivo" y no se ofrece en el selector. |

## Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| Tipo de documento | Obligatorio: DNI o RUC. | "Seleccionar el tipo de documento." |
| Documento (DNI) | Exactamente 8 dígitos. | "El DNI tiene 8 dígitos." |
| Documento (RUC) | 11 dígitos que empiezan en 10 o 20. | "El RUC tiene 11 dígitos y empieza en 10 o 20." |
| Documento | Único entre todos los clientes, activos o inactivos. | "Ya existe un cliente con este documento." |
| Nombre o razón social | Obligatorio. | "Ingresar el nombre o la razón social." |
| Teléfono | Opcional; si se escribe, 9 dígitos. | "El teléfono tiene 9 dígitos." |
| Correo | Opcional; si se escribe, formato de correo. | "Ingresar un correo válido, por ejemplo nombre@dominio.pe." |

Los mensajes aparecen junto al campo. La validación del servidor complementa la del cliente.

## Acciones y confirmaciones

- Registrar cliente: guarda el cliente con código correlativo `CLI-NNN` y estado Activo.
- Guardar cambios: modifica los datos; conserva el código y el estado.
- Desactivar cliente: exige confirmación en `ConfirmDialog`, con las consecuencias "El cliente deja de ofrecerse al registrar ventas" y "Su historial de cotizaciones y ventas se conserva". La reactivación de un cliente no forma parte de esta etapa.

## Restricción por rol

La ruta `/clientes` es accesible solo para el rol Vendedor. Un Almacenero que intente acceder es redirigido a su módulo de inicio.

## Diferencias respecto a los requerimientos y decisiones adoptadas

- "Continuar sin cliente" se ofrece como primera opción del selector y no como botón aparte. Con esta opción la venta se registra sin cliente identificado, como indica el plan.
- La unicidad del documento abarca también a los clientes inactivos, para que un documento no quede asociado a dos registros.
- La opción "Seleccionar cliente existente" solo aparece en el selector, donde el cliente repetido puede usarse en la venta o cotización en curso. En la lista no se ofrece.
- La reactivación de un cliente inactivo no está definida en el plan; queda pendiente.

## Pruebas

El archivo `clientes.pantallas.test.tsx` cubre el registro desde el selector y desde la lista, las validaciones de documento, teléfono y correo, el documento repetido, la selección sin cliente, los clientes inactivos, la búsqueda, la desactivación con confirmación, la restricción por rol y el análisis de accesibilidad de la lista y del diálogo.
