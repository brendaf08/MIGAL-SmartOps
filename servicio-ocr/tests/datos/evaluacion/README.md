# Conjunto de evaluación

Documentos reales anonimizados para medir el pipeline con `python evaluar.py` (ADR-012, punto 8). La carpeta empieza
vacía: los documentos se recopilan en la ferretería (plan de implementación, paso 0.3).

## Contenido

- `catalogo.json`: lista de productos con los campos de `ProductoCatalogo` (`sku`, `nombreOficial`, `marca`,
  `presentacion`, `unidadMedida`). Conviene exportarla del catálogo real para que los nombres sean los de la ferretería.
- Por cada documento, el archivo (`nota-001.jpg`, `guia-001.pdf`) y un JSON con el mismo nombre:

```json
{
  "tipo": "solicitud_cliente",
  "lineas": [
    { "texto": "cem sol 42.5", "cantidad": 10, "sku": "CEM-001" },
    { "texto": "f° 1/2", "cantidad": 20, "sku": "FIE-012" }
  ]
}
```

Las líneas se listan en el orden en que aparecen en el documento.

## Reglas

- Al menos 20 solicitudes de cliente (notas manuscritas y capturas de WhatsApp) y 10 documentos de recepción.
- Antes de agregar un archivo, eliminar o tapar nombres, teléfonos, direcciones, RUC y DNI de clientes y proveedores.
- No agregar documentos que no se hayan anonimizado: el repositorio no debe contener datos personales.
