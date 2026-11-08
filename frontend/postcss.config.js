/** PostCSS: mixins de Mantine (`smaller-than`, `larger-than`) y variables de puntos de quiebre para los CSS Modules. */
export default {
  plugins: {
    'postcss-preset-mantine': {},
    'postcss-simple-vars': {
      variables: {
        'mantine-breakpoint-xs': '36em',
        'mantine-breakpoint-sm': '48em',
        'mantine-breakpoint-md': '64em',
        'mantine-breakpoint-lg': '80em',
        'mantine-breakpoint-xl': '90em',
      },
    },
  },
};
