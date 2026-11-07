# PantallaInventario

Pantalla de consulta de disponibilidad (CU-04) que integra `AppShell`, `PageHeader`, `Stat`, `Tabs`, `SearchInput`, `Table`, `StatusBadge` y `Pagination`. Sirve de referencia de composición para los listados del sistema.

## Decisiones de diseño

- La pestaña "En alerta" con su contador reemplaza a las tarjetas de indicadores: el estado general se percibe sin duplicar datos ni competir con la tabla, que es el foco de la pantalla.
- Columnas por ubicación (tienda principal, Almacén 1 y Almacén 2) y consolidado físico, reservado y libre, como exige la arquitectura (RF-KAR-02, RF-PRO-05).
- Búsqueda, pestañas de filtro y paginación en el servidor para mantener la respuesta por debajo de 3 s (RNF-06).
- El estado se comunica con `StatusBadge` (texto, ícono y tono).
