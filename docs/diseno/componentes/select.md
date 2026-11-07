# Select

Selector de una opción entre varias, basado en el elemento nativo `select` para conservar teclado, lectores de pantalla y selector táctil del sistema operativo.

## Uso

- `options`: `[{ value, label, disabled }]`; `value` o `defaultValue`; `onChange(value, evento)`; `placeholder`; props de campo (`label`, `error`, `required`, `size`).

## Reglas

- Hasta unas 15 opciones. Con listas largas (productos, clientes) usar un selector con búsqueda, y con coincidencia difusa donde corresponda.
- Los motivos de movimientos manuales (merma, ajuste) son obligatorios (RF-KAR-14).
- Las opciones de proveedores y clientes inactivos no se ofrecen en operaciones nuevas (RF-PROV-06).

Equivalente en Mantine: `Select` (o `NativeSelect` cuando se requiere el selector nativo) y `Combobox` para búsqueda.
