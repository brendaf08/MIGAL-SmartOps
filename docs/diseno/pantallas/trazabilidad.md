# Matriz de trazabilidad: casos de uso y pantallas

Relaciona cada caso de uso de [especificaciones-casos-de-uso.md](../../especificaciones-casos-de-uso.md) con las pantallas del frontend que lo implementan, la ruta, el rol con acceso y las pruebas que lo verifican. Los identificadores de pantalla (P01 a P27) provienen de la distribución del trabajo del equipo.

## Casos de uso y pantallas

| Caso de uso | Pantallas | Rutas | Roles | Pruebas |
| --- | --- | --- | --- | --- |
| CU-01 Iniciar sesión | P01 inicio de sesión | `/acceso` | Todos | `LoginPage.test.tsx` |
| CU-02 Gestionar usuarios y roles | P02 lista, P03 formulario, P04 revocar acceso | `/usuarios`, `/usuarios/nuevo`, `/usuarios/:id` | Administrador | `usuarios.pantallas.test.tsx` |
| CU-03 Gestionar catálogo | P08 lista, P09 formulario, P10 ficha | `/catalogo`, `/catalogo/nuevo`, `/catalogo/:sku`, `/catalogo/:sku/editar` | Almacenero, Administrador | `catalogo.pantallas.test.tsx` |
| CU-04 Consultar disponibilidad | P11 disponibilidad con detalle | `/disponibilidad` | Almacenero, Vendedor | `DisponibilidadPage.test.tsx` |
| CU-05 Registrar movimiento | P13 formulario con constancia, P12 alertas de stock | `/kardex/nuevo`, `/alertas` | Almacenero (P12 también Reponedor) | `inventario.pantallas.test.tsx` |
| CU-06 Generar orden de despacho | P15 lista y generación, P16 orden imprimible, confirmación de salida | `/despachos`, `/despachos/:id`, `/despachos/:id/imprimir` | Almacenero | `despachos.pantallas.test.tsx`, `Despachos.test.tsx` |
| CU-07 Consultar Kardex | P14 Kardex con filtros | `/kardex` | Almacenero | `inventario.pantallas.test.tsx` |
| CU-08 Procesar documento con OCR | P23 carga, P24 revisión, lista de documentos | `/documentos`, `/documentos/procesar`, `/documentos/:id/revision` | Vendedor, Almacenero | `documentos.pantallas.test.tsx` |
| CU-09 Gestionar cotización | P17 lista, P18 formulario, detalle con aceptación y rechazo | `/cotizaciones`, `/cotizaciones/nueva`, `/cotizaciones/:id`, `/cotizaciones/:id/editar` | Vendedor | `cotizaciones.pantallas.test.tsx`, `Cotizaciones.test.tsx` |
| CU-10 Registrar venta | P19 lista, P20 registro, P21 detalle y completar, P22 clientes | `/ventas`, `/ventas/nueva`, `/ventas/:id`, `/clientes` | Vendedor | `ventas.pantallas.test.tsx`, `registro.pantallas.test.tsx`, `clientes.pantallas.test.tsx` |
| CU-11 Registrar proveedor | P06 lista, P07 formulario | `/proveedores`, `/proveedores/nueva`, `/proveedores/:id` | Reponedor | `proveedores.pantallas.test.tsx` |
| CU-12 Registrar orden de compra | P25 lista, P26 formulario, detalle | `/compras`, `/compras/nueva`, `/compras/:id` | Reponedor (consulta también Almacenero) | `compras.pantallas.test.tsx` |
| CU-13 Registrar orden de recepción | P27 recepción desde una orden de compra, con OCR opcional | `/recepciones`, `/recepciones/nueva` | Almacenero | `recepcion.pantallas.test.tsx`, `Recepciones.test.tsx` |
| CU-14 Consultar agente de IA | P05 chat | `/agente-ia` | Todos | `AgenteIAPage.test.tsx` |

## Verificaciones transversales

| Verificación | Cobertura | Archivo |
| --- | --- | --- |
| Accesibilidad con `axe` | 30 rutas, incluidas las secundarias, con un rol autorizado | `a11y.rutas.test.tsx`, `a11y.test.tsx` |
| Accesibilidad de componentes | Todas las historias de Storybook | `historias.a11y.test.tsx` |
| Módulos sin pantalla pendiente | Todos los módulos de la navegación abren su pantalla principal | `a11y.rutas.test.tsx` |
| Módulos visibles por rol | Cada rol ve solo los módulos que le corresponden | `a11y.rutas.test.tsx` |
| Enlaces entre módulos | Alertas a compras, compras a recepción, cotización a venta, documento a cotización o venta, despacho a impresión | `flujos.integracion.test.tsx` |

## Decisiones pendientes con el negocio

- El Almacenero consulta las órdenes de compra en solo lectura para registrar sus recepciones; la creación queda reservada al Reponedor.
- Si el Administrador accede a todos los módulos (hoy la navegación lo limita a catálogo, agente de IA y usuarios).
- Los umbrales de coincidencia del Fuzzy Matching (valores de ejemplo 0.85 y 0.60).
- Los nombres definitivos de los estados de cotización, venta y guía.
- Quién puede activar el modo almacén y si se guarda en el servidor.
