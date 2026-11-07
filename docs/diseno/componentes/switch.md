# Switch

Interruptor (`role="switch"`) para activar o desactivar una opción con efecto inmediato, como el estado activo de un cliente, proveedor o usuario.

## Uso

- `label`, `description`, `checked` / `defaultChecked`, `onChange`, `disabled`.

## Reglas

- Se usa cuando el cambio se aplica al instante; si requiere "Guardar", usar `Checkbox`.
- Desactivar clientes y proveedores no elimina historial (RF-CLI-08, RF-PROV-10): la descripción debe indicarlo.
- La etiqueta describe el estado afirmativo ("Cliente activo").

Equivalente en Mantine: `Switch`.
