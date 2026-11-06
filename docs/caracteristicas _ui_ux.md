# Entrega parcial — Paleta visual y criterios WCAG AA

## 1. Principios visuales
La interfaz debe priorizar:

- Lectura rápida de inventario, alertas y estados.
- Bajo contraste lumínico agresivo y baja saturación en fondos.
- Jerarquía clara entre información, acciones y advertencias.
- Uso consistente de colores, estados e iconografía.
- Prevención de errores en operaciones críticas.
- Compatibilidad con escritorio y tablets.
- Cumplimiento de WCAG 2.2 nivel AA.

El color no debe ser el único medio para comunicar estados; siempre debe combinarse con texto, iconos o etiquetas.

## 2. Paleta principal

| Token | Color | Uso |
|---|---:|---|
| `color-bg` | `#F7F8F6` | Fondo general de la aplicación. |
| `color-surface` | `#FFFFFF` | Tarjetas, modales, formularios y tablas. |
| `color-text-primary` | `#1F2933` | Títulos, contenido principal y datos críticos. |
| `color-text-secondary` | `#52606D` | Ayudas, descripciones y metadatos. |
| `color-border` | `#CBD5E1` | Bordes, separadores y controles inactivos. |
| `color-primary` | `#1F6F5B` | Acción principal, navegación activa y enlaces internos. |
| `color-primary-soft` | `#E8F1EE` | Fondos suaves, selección y estados relacionados con la acción principal. |
| `color-success` | `#166534` | Operaciones completadas y estados positivos. |
| `color-warning` | `#B45309` | Stock bajo, advertencias y situaciones que requieren atención. |
| `color-danger` | `#B42318` | Errores, bloqueos y acciones destructivas. |
| `color-info` | `#1D4ED8` | Información, ayuda y avisos neutrales. |

### Aplicación recomendada

- Utilizar `#F7F8F6` como fondo general para reducir el resplandor del blanco puro.
- Utilizar `#FFFFFF` para separar visualmente tarjetas, formularios y áreas de trabajo.
- Reservar `color-danger` para errores reales o acciones destructivas.
- No utilizar colores de alerta como decoración.
- Mantener el verde primario para acciones principales, no para todos los elementos interactivos.
- Evitar fondos completamente saturados en bloques extensos.


## 3. Contraste validado

Los siguientes contrastes cumplen el mínimo WCAG AA para texto normal cuando se aplican sobre fondos claros:

| Combinación | Contraste aproximado | Resultado |
|---|---:|---|
| `#1F2933` sobre `#F7F8F6` | 13.85:1 | Cumple AAA |
| `#52606D` sobre `#F7F8F6` | 6.06:1 | Cumple AA |
| `#1F6F5B` sobre `#F7F8F6` | 5.66:1 | Cumple AA |
| `#B45309` sobre `#F7F8F6` | 4.71:1 | Cumple AA |
| `#B42318` sobre `#F7F8F6` | 6.17:1 | Cumple AA |
| `#166534` sobre `#F7F8F6` | 6.69:1 | Cumple AA |
| `#1D4ED8` sobre `#F7F8F6` | 6.29:1 | Cumple AA |

### Regla de uso

Los textos normales deberán alcanzar al menos **4.5:1**.  
Los textos grandes deberán alcanzar al menos **3:1**.  
Los componentes gráficos, bordes funcionales y estados visuales deberán alcanzar al menos **3:1** cuando sean necesarios para identificar controles.

## 4. Estados de componentes para el UI Kit

Todos los componentes interactivos deberán contemplar como mínimo estos estados:

| Estado | Requisito |
|---|---|
| Default | Presentación normal del componente. |
| Hover | Cambio visual sutil, sin depender únicamente del color. |
| Focus | Indicador visible y suficiente para navegación con teclado. |
| Active | Indica que el control está siendo presionado o seleccionado. |
| Disabled | No disponible; debe conservar legibilidad y explicar la causa cuando sea necesario. |
| Loading | Indica procesamiento y evita envíos duplicados. |
| Error | Incluye mensaje textual y orientación para corregir. |
| Success | Confirma el resultado de una operación. |
| Warning | Advierte sobre un riesgo antes de continuar. |

El estado de foco debe utilizar un contorno visible, por ejemplo:

```css
:focus-visible {
  outline: 3px solid #1D4ED8;
  outline-offset: 2px;
}
```

No se debe eliminar el foco mediante `outline: none` sin proporcionar una alternativa equivalente.

## 5. Criterios WCAG AA para M2 y M4

### 5.1 Percepción

- Todo contenido importante debe ser distinguible sin depender solo del color.
- Las alertas deben incluir texto e icono.
- Las imágenes informativas deben tener texto alternativo.
- Los iconos funcionales deben tener nombre accesible.
- El contenido no debe perder información al ampliar la pantalla hasta un 200%.
- La interfaz debe funcionar en orientación horizontal y vertical cuando sea posible.

### 5.2 Tipografía y legibilidad

- Usar una tipografía sans serif de alta legibilidad.
- Tamaño base recomendado: **16 px**.
- Interlineado recomendado: entre **1.4 y 1.6**.
- No justificar párrafos extensos.
- No utilizar texto completo en mayúsculas para información operativa.
- Mantener títulos claramente diferenciados por tamaño, peso y espacio.
- Evitar textos secundarios con contraste insuficiente.

### 5.3 Teclado

- Toda función debe poder utilizarse con teclado.
- El orden de tabulación debe seguir el orden visual y operativo.
- No debe existir ninguna trampa de teclado.
- El foco debe ser visible en botones, enlaces, campos, pestañas, tablas interactivas y modales.
- Los modales deben devolver el foco al elemento que los abrió al cerrarse.
- `Escape` debe cerrar modales no destructivos.
- `Enter` y `Space` deben funcionar de forma coherente en controles equivalentes.

### 5.4 Formularios

- Cada campo debe tener una etiqueta visible.
- No usar solo placeholders como etiquetas.
- Los campos obligatorios deben identificarse mediante texto o atributo accesible.
- Los errores deben aparecer junto al campo correspondiente.
- El mensaje de error debe explicar cómo corregirlo.
- El formulario debe conservar los datos válidos después de un error.
- Las cantidades deben mostrar sus unidades: `unidades`, `kg`, `m`, `S/`, etc.
- Los campos de stock, cantidades y precios deben aceptar únicamente formatos válidos.

### 5.5 Tablas y datos operativos

- Cada tabla debe tener encabezados identificables.
- Las columnas críticas deben mantener una jerarquía visual clara.
- Los estados deben mostrar texto además de color.
- Las tablas deben permitir lectura adecuada en tablets.
- Los filtros activos deben ser visibles y fáciles de retirar.
- La información de stock debe diferenciar claramente:
  - Disponible.
  - Reservado.
  - Pendiente.
  - Bajo mínimo.
  - Sin stock.
- Las tablas no deben depender exclusivamente de animaciones o cambios de color.

### 5.6 Zonas de clic y táctiles

- Tamaño mínimo recomendado: **44 × 44 px**.
- Mantener separación entre botones cercanos.
- No colocar acciones destructivas inmediatamente junto a acciones de confirmación.
- Las filas seleccionables deben tener un área de interacción clara.
- Los iconos aislados deben incluir etiqueta accesible o tooltip complementario.
- Los controles deben conservar su usabilidad en tabletas y pantallas táctiles.

### 5.7 Alertas y notificaciones

- Las alertas deben ser breves y específicas.
- Los errores críticos deben bloquear la operación cuando exista riesgo de inconsistencia.
- Los mensajes de éxito no deben desaparecer antes de poder ser leídos.
- Las alertas no deben mover inesperadamente el contenido principal.
- Los mensajes deben anunciarse mediante regiones accesibles como `role="alert"` o `aria-live`.
- Nunca mostrar únicamente mensajes genéricos como “Ocurrió un error”.

Ejemplo adecuado:

> No se puede confirmar la venta: el producto SKU-104 no tiene stock suficiente. Disponible: 3 unidades; solicitadas: 5.

### 5.8 Confirmaciones

Se debe solicitar confirmación antes de:

- Confirmar ventas.
- Confirmar recepciones.
- Registrar movimientos manuales.
- Confirmar despachos.
- Desactivar usuarios, productos o proveedores.
- Descartar información introducida.

La confirmación debe indicar:

1. Qué acción se ejecutará.
2. Qué datos serán afectados.
3. Si la acción modifica el inventario.
4. Si puede deshacerse.
5. Qué botón confirma y cuál cancela.

## 6. Jerarquía visual para las vistas

### Organizacion de Vistas

Cada vista deberá organizarse en este orden:

1. **Título de la vista.**
2. **Descripción breve o contexto.**
3. **Estado operativo importante.**
4. **Acción principal.**
5. **Filtros y búsqueda.**
6. **Información principal.**
7. **Acciones secundarias.**
8. **Ayuda o información complementaria.**

Debe existir una sola acción primaria visualmente dominante por sección.

### Ejemplo: Vistas de Gestion

#### Vista 1: alertas y eventos

Debe priorizar:

- Nivel de prioridad.
- Tipo de alerta.
- Fecha y hora.
- Estado de atención.
- Producto o proceso afectado.
- Acción para abrir el detalle.

#### Vista 2: gestión de usuarios

Debe priorizar:

- Nombre del usuario.
- Rol.
- Estado.
- Último acceso o actividad relevante.
- Acción para editar.
- Acción para activar o desactivar con confirmación.

## 7. Variables CSS para implementar el UI Kit

```css
:root {
  --color-bg: #F7F8F6;
  --color-surface: #FFFFFF;
  --color-text-primary: #1F2933;
  --color-text-secondary: #52606D;
  --color-border: #CBD5E1;

  --color-primary: #1F6F5B;
  --color-primary-soft: #E8F1EE;

  --color-success: #166534;
  --color-warning: #B45309;
  --color-danger: #B42318;
  --color-info: #1D4ED8;

  --focus-ring: #1D4ED8;
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --control-min-size: 44px;
}
```
