/**
 * Tema de Mantine para MIGAL SmartOps.
 *
 * Ubicación prevista: frontend/src/theme/theme.ts
 * Fuente de los valores: tokens.json del sistema de diseño (project/tokens.json).
 *
 * Integración:
 *  1. Importar `tokens.css` una sola vez en el punto de entrada de la aplicación.
 *  2. Envolver la aplicación con <MantineProvider theme={theme}>.
 *  3. Sincronizar el tema claro u oscuro con el selector `data-theme` de tokens.css:
 *       const { colorScheme } = useMantineColorScheme();
 *       useEffect(() => document.documentElement.setAttribute('data-theme', colorScheme), [colorScheme]);
 *     El esquema predeterminado es "light".
 *  4. Los estilos específicos de cada componente se escriben en CSS Modules y consumen las variables de
 *     tokens.css (--brand, --surface, --space-4, --radius-md), sin valores literales.
 */
import { createTheme, type MantineColorsTuple } from '@mantine/core';

/** Paleta de marca: azul Progresol (índice 7 = #00339F). */
const brand: MantineColorsTuple = [
  '#EEF3FF', '#DCE6FF', '#B9CCFF', '#8FAAF7', '#6286EA',
  '#3A62D6', '#1B47BE', '#00339F', '#002A83', '#001E5E',
];

/** Neutros de papel y cemento: superficies, bordes y texto atenuado del tema claro. */
const gray: MantineColorsTuple = [
  '#F4F2ED', '#EBE8E1', '#DDD9D0', '#C5C0B4', '#A19B8D',
  '#7C766A', '#5A5549', '#403C33', '#2A2722', '#171512',
];

/**
 * Verde de acento del logo. Es un color único: Mantine exige diez tonos, por lo que se repite el valor.
 * Uso restringido a superficies oscuras (barra lateral, botón "accent", agente de IA).
 */
const accent: MantineColorsTuple = Array(10).fill('#17FF44') as unknown as MantineColorsTuple;

/**
 * Colores de estado (índice 6 = relleno del tema claro; índice 3 = relleno del tema oscuro tenue).
 * Los valores normativos son los de tokens.json; los tonos intermedios completan los diez tonos que exige Mantine.
 */
const success: MantineColorsTuple = [
  '#E3F6EA', '#C6EDD4', '#9BE0B5', '#3DDC84', '#25B86A',
  '#16984F', '#0E7A3B', '#0A5C2B', '#084A23', '#05361A',
];
const warning: MantineColorsTuple = [
  '#FFF1D6', '#FFE3AB', '#FFD98A', '#F5B83D', '#E0961F',
  '#CC7A12', '#B45309', '#7A3A00', '#5E2D00', '#451F00',
];
const danger: MantineColorsTuple = [
  '#FDE8E8', '#FBD0D1', '#FFB3B6', '#FF8A8F', '#E85A60',
  '#D4363D', '#C1272D', '#9B1C22', '#7A1519', '#5A0F12',
];
const info: MantineColorsTuple = [
  '#E2F3FC', '#C2E5F8', '#A8DCFA', '#6CC3F5', '#3BA5E0',
  '#1F87C4', '#0B6BA8', '#0A5282', '#083F66', '#052C47',
];

export const theme = createTheme({
  colors: { brand, gray, accent, success, warning, danger, info },
  primaryColor: 'brand',
  primaryShade: { light: 7, dark: 2 },
  autoContrast: true,
  luminanceThreshold: 0.4,

  fontFamily: '"Instrument Sans", system-ui, -apple-system, "Segoe UI", sans-serif',
  /** Fuente de cifras grandes y sello de documento (nunca en datos de tabla ni etiquetas): usar var(--font-display) en los CSS Modules. */
  other: { fontFamilyDisplay: '"Barlow Semi Condensed", "Arial Narrow", "Instrument Sans", system-ui, sans-serif' },
  fontFamilyMonospace: '"IBM Plex Mono", ui-monospace, "Cascadia Mono", Consolas, monospace',
  /** Bricolage Grotesque solo en el título de página (h1): lo aplica global.css con var(--font-heading). Los demás encabezados usan Instrument Sans. */
  headings: {
    fontFamily: '"Instrument Sans", system-ui, -apple-system, "Segoe UI", sans-serif',
    fontWeight: '700',
    sizes: {
      h1: { fontSize: '28px', lineHeight: '36px', fontWeight: '700' },
      h2: { fontSize: '22px', lineHeight: '30px', fontWeight: '700' },
      h3: { fontSize: '18px', lineHeight: '26px', fontWeight: '600' },
      h4: { fontSize: '16px', lineHeight: '24px', fontWeight: '600' },
    },
  },
  fontSizes: { xs: '12px', sm: '13px', md: '14px', lg: '16px', xl: '18px' },
  lineHeights: { xs: '16px', sm: '18px', md: '20px', lg: '24px', xl: '26px' },

  /** Escala de espaciado: xs = space-2, sm = space-3, md = space-4, lg = space-6, xl = space-8. */
  spacing: { xs: '8px', sm: '12px', md: '16px', lg: '24px', xl: '32px' },

  /** Radios: xs = radius-xs, sm = radius-sm, md = radius-md, lg = radius-lg, xl = radius-xl. */
  radius: { xs: '2px', sm: '4px', md: '6px', lg: '10px', xl: '16px' },
  defaultRadius: 'md',

  shadows: {
    xs: '0 1px 2px #0e1b3d14',
    sm: '0 2px 6px #0e1b3d14',
    md: '0 6px 16px #0e1b3d1f',
    lg: '0 16px 40px #0e1b3d2e',
    xl: '0 16px 40px #0e1b3d2e',
  },

  /** Puntos de quiebre: teléfono grande desde 36em (576 px), tablet desde 48em (768 px), escritorio desde 64em (1024 px) y pantallas All-in-One desde 80em. Por debajo de 48em rige el diseño de teléfono. */
  breakpoints: { xs: '36em', sm: '48em', md: '64em', lg: '80em', xl: '90em' },

  /** Mantine dibuja el anillo con el color primario; los componentes propios usan var(--shadow-focus) de tokens.css. */
  focusRing: 'auto',
  cursorType: 'pointer',

  components: {
    Button: { defaultProps: { size: 'md', radius: 'md' } },
    ActionIcon: { defaultProps: { size: 'lg', radius: 'md', variant: 'subtle' } },
    TextInput: { defaultProps: { size: 'md', radius: 'md' } },
    NumberInput: { defaultProps: { size: 'md', radius: 'md', thousandSeparator: ',', decimalSeparator: '.' } },
    Select: { defaultProps: { size: 'md', radius: 'md', allowDeselect: false } },
    Textarea: { defaultProps: { size: 'md', radius: 'md', autosize: true, minRows: 3 } },
    Badge: { defaultProps: { variant: 'light', radius: 'sm' } },
    Paper: { defaultProps: { radius: 'lg', withBorder: true } },
    Modal: { defaultProps: { radius: 'xl', centered: true, trapFocus: true, closeOnClickOutside: false } },
    Alert: { defaultProps: { variant: 'light', radius: 'lg' } },
    Table: { defaultProps: { highlightOnHover: true, verticalSpacing: 'sm', horizontalSpacing: 'md' } },
  },
});

export type AppTheme = typeof theme;
