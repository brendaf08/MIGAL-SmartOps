// Genera docs/ai-studio/<modulo>.md: el prompt COMPLETO de cada integrante para AI Studio.
// Uso: npm run prompts   (vuelve a ejecutarlo si cambia algo de src/shared/)
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';

const leer = (ruta) => readFileSync(ruta, 'utf8').trimEnd();

const archivosKit = [
  ['src/index.css', 'css'],
  ['src/shared/tipos.ts', 'ts'],
  ['src/shared/formato.ts', 'ts'],
  ['src/shared/data/semillas.ts', 'ts'],
  ['src/shared/auth/AuthContext.tsx', 'tsx'],
  ['src/shared/auth/permisos.ts', 'ts'],
  ['src/shared/components/ui.tsx', 'tsx'],
  ['src/shared/components/Logo.tsx', 'tsx'],
  ['src/shared/components/EnConstruccion.tsx', 'tsx'],
];

const promptBase = (modulo) => `Eres un desarrollador frontend. Construye pantallas para MIGAL SmartOps, el sistema de gestión de una ferretería en Perú.

STACK OBLIGATORIO (no uses otras librerías)
- React + TypeScript, React Router (react-router-dom), Tailwind CSS y lucide-react para iconos.
- Sin backend: usa solo datos de prueba locales.
- Diseño para escritorio y tablet (desde 768 px). No diseñes para móvil.

ARCHIVOS COMPARTIDOS
- Crea EXACTAMENTE los archivos de src/shared/ y src/index.css que pego abajo. NO los modifiques nunca.
- Si Tailwind se carga por CDN, configura en tailwind.config los colores de src/index.css (primario #15803D, primario-oscuro #166534, primario-suave #DCFCE7, lateral #0B3B24, secundario #64748B, tenue #94A3B8, fondo #F8FAF9, superficie #FFFFFF, borde #EEF2EF, borde-campo #858F9C, texto #0F1A14, exito #15803D, info #1D4ED8, alerta #B45309, error #B91C1C) y las fuentes Inter (sans) y Plus Jakarta Sans (display).
- Usa solo esas clases de color (bg-primario, text-error, border-borde...). Nunca colores de Tailwind como green-600 o blue-600.

KIT VISUAL (obligatorio, para que todas las pantallas se vean iguales)
- Usa los componentes de src/shared/components/ui.tsx: EncabezadoPagina (título y botones de cada pantalla), Tarjeta (contenedores), Boton (variantes primario, secundario, peligro, fantasma), Campo y Selector (formularios con etiqueta y error) y EtiquetaEstado (estados).
- Cada pantalla empieza con <EncabezadoPagina titulo=... descripcion=... acciones=... /> y su contenido va en <Tarjeta>.
- Tablas: encabezado con bg-fondo text-secundario text-sm font-semibold, filas con border-b border-borde y hover:bg-fondo, celdas con px-4 py-3, montos alineados a la derecha.
- Estilo profesional y sobrio: bordes redondeados (rounded-xl en tarjetas, rounded-lg en botones), sombras suaves, mucho espacio en blanco, iconos de lucide-react de 18 a 20 px junto a las acciones.
- No dibujes menú lateral ni barra superior: ya existen en el sistema.
- Estados vacíos con <EstadoVacio icono titulo descripcion accion />.

PRINCIPIOS DE DISEÑO (estética SaaS B2B premium, nada de plantilla genérica)
- Espaciado generoso y consistente: gap-6, p-8, space-y-6. Nada amontonado.
- Capas: el lienzo es bg-fondo; tarjetas y contenedores en bg-superficie. Separa con sombras suaves (shadow-sm, ring-1 ring-black/[0.04]) en vez de bordes; si usas bordes, solo border-borde.
- Jerarquía de texto estricta: títulos text-texto, descripciones text-secundario, metadatos no esenciales text-tenue. Nunca negro puro.
- Iconos solo de lucide-react, tamaño 18 a 20 px (el trazo 1.5 ya está configurado globalmente).
- Botones rounded-lg con hover sutil (ya resuelto en Boton). Nunca colores sólidos brillantes para resaltar selecciones: usa bg-primario-suave o fondos translúcidos.
- Los campos de formulario siempre con borde visible (ya resuelto en Campo y Selector) para que se distingan.
- Crea src/App.tsx con el App de prueba que pego abajo, tal cual.

DÓNDE VA TU CÓDIGO
- Todo tu código va dentro de src/modules/${modulo}/, con subcarpetas pages/, components/ y data/.
- src/modules/${modulo}/index.tsx exporta: export const rutas: RutaModulo[] (tipo de src/shared/tipos.ts), con un elemento por pantalla (codigo, path, titulo, elemento, enMenu). Usa EXACTAMENTE los códigos, paths y enMenu que indico abajo.
- Cada pantalla es un archivo en pages/ con nombre código + PascalCase (ej. pages/P12AlertasStock.tsx) y export default.
- Importa solo desde src/shared/ con rutas relativas (../../shared/... o ../../../shared/...). Nunca importes de otro módulo. Para ir a otra pantalla usa su ruta con Link o useNavigate.
- No crees routers dentro del módulo: solo usa Link, useNavigate y useParams.

DATOS Y REGLAS
- Usa exactamente los tipos y nombres de campos de src/shared/tipos.ts, en snake_case. No traduzcas ni inventes campos.
- Lee los datos de src/shared/data/semillas.ts. Los datos nuevos del módulo se guardan en estado (useState) o en src/modules/${modulo}/data/.
- El usuario y el rol se leen con useAuth() de src/shared/auth/AuthContext.tsx.
- Montos con formatearSoles() y fechas con formatearFecha() de src/shared/formato.ts (S/ y dd/mm/aaaa).
- El stock está por ubicación en inventario (Tienda principal, Almacén 1, Almacén 2); no existe un campo único de stock.
- Reglas visibles: nunca stock negativo; el descuento no puede superar el subtotal; el cliente es obligatorio en cotizaciones y ventas. Muestra mensajes de error claros y deshabilita el botón cuando no se cumpla.
- Muestra los estados con sus nombres exactos de tipos.ts, como etiquetas de color (exito, alerta, error, secundario).

ACCESIBILIDAD Y ESTILO
- Fuente Inter, texto base 16 px, áreas de clic de mínimo 44×44 px (min-h-11), contraste WCAG AA.
- Etiquetas visibles en todos los campos. Confirmación antes de acciones destructivas.
- Todo el texto de la interfaz en español.

Si algo no está especificado, elige la opción más simple y no agregues librerías.`;

const appPrueba = (modulo, inicial) => `import { HashRouter, Routes, Route, Link } from 'react-router-dom';
import { AuthProvider } from './shared/auth/AuthContext';
import { usuarios } from './shared/data/semillas';
import { rutas } from './modules/${modulo}';

// SOLO PARA PROBAR EN AI STUDIO. Para probar otro rol cambia el índice:
// usuarios[0] admin · [1] vendedor · [2] almacenero · [3] reponedor
export default function App() {
  return (
    <AuthProvider usuarioInicial={${inicial}}>
      <HashRouter>
        <nav className="flex flex-wrap gap-2 p-4 bg-superficie border-b">
          {rutas.map((r) => (
            <Link key={r.codigo} to={r.path.replace(/:id/g, '1')} className="min-h-11 px-4 py-2 rounded text-primario">
              {r.codigo} {r.titulo}
            </Link>
          ))}
        </nav>
        <main className="p-6">
          <Routes>
            {rutas.map((r) => <Route key={r.codigo} path={r.path} element={r.elemento} />)}
          </Routes>
        </main>
      </HashRouter>
    </AuthProvider>
  );
}`;

const modulos = {
  usuarios: {
    persona: 'Gabriel', inicial: 'usuarios[0]',
    pantallas: `- P01 Inicio de sesión · /login · enMenu false · CU-01. Campos username y contraseña; usa iniciarSesion() de useAuth(). Si falla: «Usuario o contraseña incorrectos». Si entra, navega a /agente. (Para probarla, cambia usuarioInicial a null en App.tsx.)
- P02 Usuarios: lista · /usuarios · enMenu true · CU-02. Tabla con nombres, apellidos, username, rol, estado_activo y fecha_registro; filtros por rol y estado; botón «Nuevo usuario» a /usuarios/nuevo; botón «Revocar acceso» que abre P04.
- P04 Confirmar revocar acceso · ventana modal dentro de P02, SIN ruta. Muestra el nombre del usuario; al confirmar pone estado_activo en false. No se puede revocar al usuario con sesión iniciada.
- P03 Usuario: registro y rol · /usuarios/nuevo · enMenu false · CU-02. Campos nombres, apellidos, username (único) y rol (los 4 roles); fecha_registro con hoyISO().
- P05 Agente de IA: chat · /agente · enMenu true · CU-14. Chat con respuestas simuladas (sin IA real): responde el stock por ubicación de un producto buscado en semillas. Indica que es versión de prueba.
- P06 Proveedores: lista · /proveedores · enMenu true · CU-11. Tabla con búsqueda por ruc o razon_social y botón «Nuevo proveedor».
- P07 Proveedor: formulario · /proveedores/nuevo · enMenu false · CU-11. Campos ruc (11 dígitos, único), razon_social, contacto, condiciones; productos asociados con precio_compra y plazo_entrega_referencial_dias (CatalogoProveedor).`,
  },
  productos: {
    persona: 'Brenda', inicial: 'usuarios[0]',
    pantallas: `- P08 Productos: lista · /productos · enMenu true · CU-03. Tabla con sku, nombre_oficial, categoria_producto, marca, precio_referencial y stock libre total (suma de cantidad_libre en inventario); etiqueta «Stock crítico» si está bajo stock_minimo; búsqueda y filtro por categoría; cada fila lleva a /productos/<id_producto>.
- P09 Producto: formulario · /productos/nuevo · enMenu false · CU-03. Todos los campos de Producto; sku único, precio_referencial mayor a 0, stock_minimo de 0 o más.
- P10 Producto: ficha · /productos/:id · enMenu false · CU-03. Datos del producto y tabla de stock por ubicación (cantidad_fisica, cantidad_reservada, cantidad_libre).
- P11 Disponibilidad · /disponibilidad · enMenu true · CU-04. Buscador de producto; muestra las 3 ubicaciones con sus cantidades, resaltando dónde hay stock libre.
- P25 Órdenes de compra: lista · /compras · enMenu true · CU-12. Lista de ordenesCompra con estados Pendiente, Parcial, Completada; filtro por estado y proveedor (OrdenCompra y DetalleCompra de semillas).
- P26 Orden de compra: nueva · /compras/nueva · enMenu false · CU-12. Proveedor activo, productos (sugiere precio_compra de catalogoProveedor), cantidad_comprada y costo_unitario; subtotal_detalle calculado; cantidades mayores a 0; estado inicial Pendiente; fecha_entrega_estimada y modalidad_pago opcionales.`,
  },
  inventario: {
    persona: 'Nuñez', inicial: 'usuarios[2]',
    pantallas: `- P12 Alertas de stock crítico · /inventario/alertas · enMenu true. Productos cuya suma de cantidad_libre es menor a stock_minimo, con detalle por ubicación; botón «Crear orden de compra» que navega a /compras/nueva.
- P13 Registrar movimiento · /inventario/movimientos/nuevo · enMenu true · CU-05. En pantalla el usuario elige Ajuste, Merma o Traslado. Se guarda como MovimientoKardex con origen_movimiento Ajuste (Entrada o Salida) o Traslado; una Merma se guarda como Ajuste de Salida con motivo que empieza con «Merma». Campos: producto, almacén (y almacén destino si es Traslado: dos movimientos, Salida y Entrada, con la misma referencia_traslado), cantidad positiva y motivo obligatorio. Bloquea el registro si el stock físico quedaría negativo.
- P14 Kardex · /inventario/kardex · enMenu true · CU-07. Filtros por producto, almacén y rango de fechas; tabla de MovimientoKardex con fecha_hora, origen_movimiento, entrada, salida y saldo (datos de prueba en data/, con orígenes Despacho, Recepcion, Ajuste, Merma y Traslado).
- P15 Despachos · /despachos · enMenu true · CU-06. Ventas en estado PendienteDespacho o ListaParaDespacho (ventas y detallesVenta de semillas, con su cliente). Botón «Generar guía» en las PendienteDespacho: crea una GuiaDespacho con estado Emitida (direccion_entrega, fecha_despacho opcional), pasa la venta a ListaParaDespacho y va a /despachos/<id_venta>/imprimir. Si la venta ya tiene guía, advierte y pide confirmación para reimprimir.
- P16 Guía de despacho imprimible · /despachos/:id/imprimir · enMenu false · CU-06. GuiaDespacho en formato limpio para imprimir con window.print() (botones con clase no-imprimir): cliente, direccion_entrega y productos con cantidad_vendida. Estados EnPreparacion, Emitida, SalidaConfirmada, Entregada. Imprimir NO descuenta stock. Botón «Confirmar salida» (con confirmación): pasa a SalidaConfirmada, registra fecha_salida_real, la venta pasa a Despachada y muestra que se generan las salidas de Kardex (origen Despacho). Luego «Marcar entregada».`,
  },
  ventas: {
    persona: 'Pinedo', inicial: 'usuarios[1]',
    pantallas: `- P19 Ventas: lista · /ventas · enMenu true · CU-10. Lista de ventas con sus 5 estados (PendienteAbastecimiento, PendienteDespacho, ListaParaDespacho, Despachada, Cancelada) como etiquetas; total_venta en S/; filtro por estado y cliente; las ventas PendienteAbastecimiento llevan a /ventas/<id_venta>/atencion.
- P20 Registrar venta · /ventas/nueva · enMenu true · CU-10. Cliente obligatorio (buscar o registrar en una ventana modal). Opcional: partir de una cotización en estado Aceptada que no se haya convertido antes (guarda id_cotizacion y carga detallesCotizacion); si no, id_cotizacion es null (venta directa). Agregar productos mostrando el stock libre por ubicación; cada DetalleVenta con cantidad_vendida, precio_unitario y subtotal_detalle; descuento no mayor a la suma de subtotales; total_venta en S/. Registrar la venta RESERVA el stock libre disponible (no lo descuenta: eso ocurre al confirmar el despacho). Si todo queda reservado, la venta queda PendienteDespacho; si falta stock, queda PendienteAbastecimiento mostrando cantidad_pendiente_abastecimiento por producto. Nunca se permite stock negativo.
- P21 Atención de venta pendiente · /ventas/:id/atencion · enMenu false · CU-10. DetalleVenta con cantidad_vendida, cantidad_reservada_actual y cantidad_pendiente_abastecimiento. Botón «Reservar stock disponible» que cubre lo pendiente con el stock libre; cuando no queda nada pendiente, la venta pasa a PendienteDespacho. Botón «Cancelar venta» con confirmación: motivo_cancelacion obligatorio, fecha_cancelacion automática y las reservas pasan a Liberada.
- P22 Clientes · /clientes · enMenu true · CU-10. Lista y registro; DNI de 8 dígitos o RUC de 11.`,
  },
  cotizaciones: {
    persona: 'Alexander', inicial: 'usuarios[1]',
    pantallas: `- P17 Cotizaciones: lista · /cotizaciones · enMenu true · CU-09. Lista con estados Emitida, Aceptada, Rechazada, Vencida; filtro por estado; botón «Nueva cotización».
- P18 Cotización: nueva o editar · /cotizaciones/nueva · enMenu false · CU-09. Cliente obligatorio, productos con cantidad_cotizada, precio_unitario y subtotal_detalle; descuento no mayor a la suma de subtotales; total_cotizacion; fecha_emision, fecha_vencimiento y condiciones_comerciales.
- P23 Procesar documento · /documentos/procesar · enMenu true · CU-08. Zona de arrastre que acepta solo PNG, JPG y PDF. Elegir tipo_documento: SolicitudCliente o DocumentoRecepcion. Función procesarDocumento(archivo) en data/ que tras 1,5 s devuelve { documento: DocumentoDigital, detalles: DetalleConCoincidencia[] } (tipos de tipos.ts) con estado_procesamiento «Procesado OCR». Si el documento es ilegible, muestra el aviso y permite cargar otro o continuar manualmente. Luego navega a /documentos/<id_documento>/revision pasando el resultado con navigate(ruta, { state }).
- P24 Revisión del OCR · /documentos/:id/revision · enMenu false · CU-08. Tabla de DetalleConCoincidencia con texto_crudo, cantidad_leida y producto sugerido; nivel_coincidencia (calculado, no se guarda) como etiqueta Alta, Media, Baja o No identificado; si hay coincidencia ambigua, muestra las alternativas para elegir; permite corregir id_producto, cantidad_leida y texto_corregido. Al confirmar, estado_procesamiento queda Validado. El OCR nunca crea ventas ni mueve stock por sí mismo.
- P27 Orden de recepción · /recepciones/nueva · enMenu true · CU-13. Elegir una OrdenCompra Pendiente o Parcial (ordenesCompra y detallesCompra de semillas); crea una OrdenRecepcion con DetalleRecepcion por producto: cantidad_recibida no mayor a cantidad_pendiente_recepcion e id_almacen_destino (se puede repartir entre almacenes); observaciones; botón opcional «Leer con OCR» a /documentos/procesar. Estado Pendiente hasta confirmar; al confirmar queda Validada con fecha_validacion. Aviso: solo la recepción Validada mueve el Kardex. (Para probar como almacenero, usa usuarios[2].)`,
  },
};

mkdirSync('docs/ai-studio', { recursive: true });

for (const [modulo, { persona, inicial, pantallas }] of Object.entries(modulos)) {
  const kit = archivosKit
    .map(([ruta, lang]) => `### ${ruta}\n\n\`\`\`${lang}\n${leer(ruta)}\n\`\`\``)
    .join('\n\n');

  const md = `<!-- GENERADO con "npm run prompts". No editar a mano. -->
# Prompt para AI Studio · módulo ${modulo} (${persona})

Copia TODO este archivo y pégalo como primer mensaje en un proyecto nuevo de Google AI Studio.

${promptBase(modulo)}

## Archivos compartidos (créalos tal cual)

${kit}

### src/App.tsx (App de prueba)

\`\`\`tsx
${appPrueba(modulo, inicial)}
\`\`\`

## Pantallas a generar (módulo ${modulo})

${pantallas}
`;
  writeFileSync(`docs/ai-studio/${modulo}.md`, md);
  console.log(`docs/ai-studio/${modulo}.md`);
}
