# Casos de uso

> Base documental pendiente de revisión. Las especificaciones completas están en [su documento independiente](especificaciones-casos-de-uso.md).

## Catálogo

| Código | Nombre | Referencia |
| --- | --- | --- |
| CU-01 | Iniciar sesión | [Especificación](especificaciones-casos-de-uso.md#cu-01) |
| CU-02 | Gestionar usuarios y roles | [Especificación](especificaciones-casos-de-uso.md#cu-02) |
| CU-03 | Gestionar catálogo de productos | [Especificación](especificaciones-casos-de-uso.md#cu-03) |
| CU-04 | Consultar disponibilidad | [Especificación](especificaciones-casos-de-uso.md#cu-04) |
| CU-05 | Registrar movimiento | [Especificación](especificaciones-casos-de-uso.md#cu-05) |
| CU-06 | Generar orden de despacho | [Especificación](especificaciones-casos-de-uso.md#cu-06) |
| CU-07 | Consultar Kardex | [Especificación](especificaciones-casos-de-uso.md#cu-07) |
| CU-08 | Procesar documento con OCR | [Especificación](especificaciones-casos-de-uso.md#cu-08) |
| CU-09 | Gestionar cotización | [Especificación](especificaciones-casos-de-uso.md#cu-09) |
| CU-10 | Registrar venta | [Especificación](especificaciones-casos-de-uso.md#cu-10) |
| CU-11 | Registrar proveedor | [Especificación](especificaciones-casos-de-uso.md#cu-11) |
| CU-12 | Registrar orden de compra | [Especificación](especificaciones-casos-de-uso.md#cu-12) |
| CU-13 | Registrar orden de recepción | [Especificación](especificaciones-casos-de-uso.md#cu-13) |
| CU-14 | Consultar agente de IA | [Especificación](especificaciones-casos-de-uso.md#cu-14) |

## Diagramas por módulo

### Usuarios roles

![usuarios-roles](diagramas/imagenes/MIGAL-DCU-01-usuarios-roles.png)

### Catalogo productos

![catalogo-productos](diagramas/imagenes/MIGAL-DCU-02-catalogo-productos.png)

### Inventario

![inventario](diagramas/imagenes/MIGAL-DCU-03-inventario.png)

### Ventas cotizaciones

![ventas-cotizaciones](diagramas/imagenes/MIGAL-DCU-04-ventas-cotizaciones.png)

### Abastecimiento

![abastecimiento](diagramas/imagenes/MIGAL-DCU-05-abastecimiento.png)

### Agente ia

![agente-ia](diagramas/imagenes/MIGAL-DCU-06-agente-ia.png)

## Pendiente de corrección

En abastecimiento, la extensión OCR debe apuntar desde Procesar documento con OCR hacia Registrar orden de recepción. La imagen recibida conserva la orientación observada.
