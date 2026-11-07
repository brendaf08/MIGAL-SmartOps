# Frontend de MIGAL SmartOps

Aplicación web de MIGAL SmartOps. React + TypeScript + Vite, Tailwind CSS, React Router y lucide-react. Por ahora no hay backend: los datos de prueba están en `src/shared/data/semillas.ts`, con los nombres del [modelo y diccionario de datos](../docs/modelo-y-diccionario-datos.md).

## Instalar y ejecutar

Requiere Node.js LTS (20.19 o superior). Todos los comandos se ejecutan dentro de `frontend/`.

```bash
cd frontend
npm install
npm run dev      # abre http://localhost:5173
npm run build    # comprueba que todo compila
```

Usuarios de prueba (contraseña `migal123`): `admin`, `vendedor`, `almacenero`, `reponedor`.

## Módulos y responsables

| Integrante | Carpeta (lo único que toca) | Pantallas | Prompt para AI Studio |
| --- | --- | --- | --- |
| Gabriel | `frontend/src/modules/usuarios/` | P01–P07 | `frontend/docs/ai-studio/usuarios.md` |
| Brenda | `frontend/src/modules/productos/` | P08–P11, P25–P26 | `frontend/docs/ai-studio/productos.md` |
| Nuñez | `frontend/src/modules/inventario/` | P12–P16 | `frontend/docs/ai-studio/inventario.md` |
| Pinedo | `frontend/src/modules/ventas/` | P19–P22 | `frontend/docs/ai-studio/ventas.md` |
| Alexander | `frontend/src/modules/cotizaciones/` | P17–P18, P23–P24, P27 | `frontend/docs/ai-studio/cotizaciones.md` |

## Cómo trabaja cada integrante

1. **Genera en AI Studio.** Copia **todo** tu archivo de `frontend/docs/ai-studio/` y pégalo como primer mensaje en un proyecto nuevo de Google AI Studio. Ya trae el diseño, los tipos, los datos y tus pantallas.
2. **Guarda las capturas** en `docs/imagenes/` (en la raíz del repo), según las reglas de nomenclatura de [CONTRIBUTING.md](../CONTRIBUTING.md):
   - Primera versión: `MIGAL-WF-P20-registrar-venta.png`
   - Versión final: `MIGAL-AF-P20-registrar-venta.png`
3. **Copia solo tu carpeta.** Descarga el proyecto de AI Studio como .zip. De ahí solo te sirve `src/modules/<tu-modulo>/`: reemplaza con ella `frontend/src/modules/<tu-modulo>/`. Lo demás se descarta.
4. **Prueba y sube** en una rama nueva, siguiendo las convenciones del repo:

```bash
git checkout main
git pull
git checkout -b feat/frontend-<tu-modulo>
cd frontend && npm install && npm run build && cd ..
git add frontend/src/modules/<tu-modulo> docs/imagenes
git commit -m "feat: agregar pantallas de <tu-modulo>"
git push -u origin feat/frontend-<tu-modulo>
```

5. **Abre el Pull Request** hacia `main` con la plantilla del repo. Se integra con **Squash and merge** y luego se borra la rama. Para la siguiente entrega, crea una rama nueva desde `main` actualizado.

## Reglas

1. Solo tocas `frontend/src/modules/<tu-modulo>/` y tus capturas `MIGAL-WF-…` y `MIGAL-AF-…` en `docs/imagenes/`.
2. Tu `index.tsx` exporta `rutas` con los **mismos códigos, rutas y `enMenu`** que ya tiene. Así tu pantalla aparece sola en el menú y con sus permisos.
3. Nunca importas de otro módulo. Para ir a otra pantalla usas su ruta (`<Link to="/compras/nueva">`).
4. Campos con los nombres de `src/shared/tipos.ts`. Montos con `formatearSoles()` y fechas con `formatearFecha()`.
5. Componentes de `src/shared/components/ui.tsx` y colores del tema (`bg-primario`, `text-secundario`…), nunca colores sueltos.
6. No instalas librerías nuevas. Si necesitas algo en `src/shared/`, lo pides al responsable del frontend.

## Verificación automática

Cada PR que toca `frontend/` pasa por **Verificar frontend** (GitHub Actions). Falla si cambia archivos fuera de un único módulo y sus capturas, o si el proyecto no compila. Si sale ❌, abre el detalle del check: dice qué archivo sobra o qué error hay.

Los cambios de base (`src/shared/`, `App.tsx`, `routes.tsx`, configuración) los hace el responsable del frontend y llevan la etiqueta `frontend-base` en el PR.

## Estructura

```
frontend/
  src/
    index.css            paleta y fuentes (Tailwind @theme)
    App.tsx              menú por rol, barra superior y rutas protegidas
    routes.tsx           une los 5 módulos
    shared/
      tipos.ts           tipos de entidades y estados (diccionario de datos)
      formato.ts         formatearSoles, formatearFecha, hoyISO
      navegacion.ts      grupos e iconos del menú
      data/semillas.ts   datos de prueba compartidos
      auth/              useAuth() y permisos por rol
      components/        ui.tsx, Logo, EnConstruccion
    modules/<modulo>/
      index.tsx          exporta `rutas`
      pages/  components/  data/
  docs/ai-studio/        un prompt completo por integrante
  scripts/               generador de prompts
  public/logo.png        logo (opcional)
```

## Estilo

Verde profesional. Fuentes: Inter (texto) y Plus Jakarta Sans (títulos). Todo está en `src/index.css`.

| Clase | Uso | Hex |
| --- | --- | --- |
| primario | Botones principales, enlaces, foco | #15803D |
| primario-oscuro | Hover, títulos de marca | #166534 |
| primario-suave | Fondos de selección | #DCFCE7 |
| lateral | Menú lateral | #0B3B24 |
| secundario | Descripciones, texto secundario | #64748B |
| tenue | Metadatos no esenciales | #94A3B8 |
| fondo | Lienzo de página | #F8FAF9 |
| superficie | Tarjetas, modales | #FFFFFF |
| borde | Divisores sutiles | #EEF2EF |
| borde-campo | Borde de campos de formulario | #858F9C |
| texto | Títulos y texto principal | #0F1A14 |
| exito / info / alerta / error | Estados | #15803D / #1D4ED8 / #B45309 / #B91C1C |

## Para el responsable del frontend

Si cambias algo de `src/shared/` o `src/index.css`, ejecuta `npm run prompts` dentro de `frontend/` para regenerar `docs/ai-studio/` y avisa al equipo.
