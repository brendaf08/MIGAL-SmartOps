# AppShell

Estructura general de la aplicación: barra lateral de navegación, cabecera con ruta y usuario, y área de contenido con enlace "Saltar al contenido". La barra lateral usa el azul profundo de la marca (gris pizarra más profundo en el modo oscuro) y la cabecera, la superficie de las tarjetas; el ítem activo lleva un marcador cuadrado verde con contorno azul, como la etiqueta de un estante.

## Uso

- `nav`: grupos `[{ label, items: [{ key, label, icon, roles, badge, href }] }]`; `role` (rol del usuario autenticado, filtra los ítems con `roles`); `current`; `onNavigate(key)`; `user` (`{ name, role }`); `onLogout`; `logo` (nodo); `header` (nodo en la cabecera); `collapsed`; `theme` (`light` o `dark`) y `onToggleTheme` (muestra el botón de cambio de modo en la cabecera).
- Un ítem sin `roles` es visible para todos los usuarios (por ejemplo, Agente de IA). La visibilidad de la navegación es una ayuda de usabilidad: la autorización real la aplica el backend (RNF-09).

## Reglas

- Barra lateral expandida (264 px) desde 1024 px; en tablets (menos de 1024 px) se contrae a íconos (72 px) y cada ítem conserva su `title`.
- La navegación se agrupa por módulo funcional (Operación, Comercial, Abastecimiento, Asistente, Administración). El ítem activo lleva `aria-current="page"`, el fondo `nav-active`, el ícono y el texto en `nav-ink` y el marcador de acento.
- El logo de Progresol se muestra sobre el azul profundo de la barra, con el símbolo verde y el nombre en blanco legibles; la barra es el principal punto de color de la interfaz.
- El botón de modo (sol o luna) se ubica a la izquierda del usuario; su etiqueta indica la acción, no el estado.
- Objetivos táctiles de 44 px de alto. El anillo de foco usa `focus`.
- Las rutas protegidas por rol se registran de la misma forma en React Router; el ítem oculto equivale a una ruta no accesible.
- La matriz de roles de la propuesta se deriva de los actores de los casos de uso; el acceso total del Administrador a los módulos operativos es un supuesto pendiente de confirmar.

Equivalente en Mantine: `AppShell` con `AppShell.Navbar`, `AppShell.Header` y `NavLink`.

## Comportamiento responsive

- Teléfono (menos de 768 px): la barra lateral se convierte en un panel deslizable de hasta 300 px (86 % del ancho) que se abre con el botón de menú de la cabecera, se cierra con Escape, con el velo o al elegir un ítem, y vuelve el foco al botón. La cabecera mide 56 px y muestra el menú, el nombre del sistema, el cambio de modo y solo el avatar del usuario.
- Tablet (768 a 1023 px): barra lateral de íconos (72 px) con `title` en cada ítem.
- Escritorio (1024 px o más): barra lateral expandida (264 px).
- Se respetan las áreas seguras (`env(safe-area-inset-*)`) y el panel declara `aria-expanded` y `aria-controls` en el botón de menú.
