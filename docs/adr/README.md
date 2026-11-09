# Registros de decisiones de arquitectura

Esta carpeta contiene los registros de decisiones de arquitectura (ADR, Architecture Decision Records) de MIGAL SmartOps. Cada registro documenta una decisión técnica relevante, su contexto, las alternativas evaluadas y sus consecuencias.

## Índice

| ADR | Decisión |
| --- | --- |
| [ADR-001](adr-001-stack-frontend.md) | Stack tecnológico del frontend. |
| [ADR-002](adr-002-sistema-de-diseno.md) | Sistema de diseño del frontend. |
| [ADR-003](adr-003-reglas-ventas-reservas-despacho.md) | Reglas de ventas, reservas, Kardex y despacho. |
| [ADR-004](adr-004-arquitectura-backend.md) | Arquitectura y stack del backend. |
| [ADR-005](adr-005-base-de-datos.md) | Base de datos, migraciones y concurrencia. |
| [ADR-006](adr-006-contrato-api.md) | Contrato de la API REST. |
| [ADR-007](adr-007-hosting-base-de-datos-supabase.md) | Hosting de la base de datos en Supabase. |

## Registrar una decisión

1. Copiar [plantilla.md](plantilla.md) con el nombre `adr-NNN-descripcion.md`, donde `NNN` es el siguiente número correlativo y la descripción se escribe en minúsculas, sin tildes y separada por guiones.
2. Completar todas las secciones y referenciar los identificadores oficiales de requerimientos y casos de uso.
3. Agregar el registro al índice de este documento.

Para modificar una decisión registrada, se recomienda registrarla en un nuevo ADR que referencie al anterior y actualizar los documentos afectados.
