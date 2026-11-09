import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { formatNumber } from '@/lib/format';
import { renderApp } from '@/test/renderApp';
import { resumen } from './calculos';
import { MOVIMIENTOS, PRODUCTOS } from './fixtures';

expect.extend(matchers);

/** Los registros de movimiento modifican los datos de prueba en memoria: cada caso parte del estado inicial. */
const copia = <T,>(valor: T): T => JSON.parse(JSON.stringify(valor)) as T;
const ESTADO_INICIAL = { productos: copia(PRODUCTOS), movimientos: copia(MOVIMIENTOS) };

afterEach(() => {
  window.sessionStorage.clear();
  PRODUCTOS.splice(0, PRODUCTOS.length, ...copia(ESTADO_INICIAL.productos));
  MOVIMIENTOS.splice(0, MOVIMIENTOS.length, ...copia(ESTADO_INICIAL.movimientos));
});

/** Espera a que la tabla reciba datos: antes de la carga muestra filas de esqueleto. */
const esperarFilas = (tabla: HTMLElement) =>
  within(tabla).findAllByText(/Stock crítico|En el mínimo/);

const contar = (nivel: 'Critico' | 'Minimo') =>
  PRODUCTOS.filter((p) => resumen(p).nivel === nivel).length;

describe('Alertas de stock (P12)', () => {
  it('ordena las alertas por gravedad y muestra los contadores', async () => {
    renderApp('/alertas', 'Almacenero');

    const tabla = await screen.findByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    const criticos = contar('Critico');
    const minimos = contar('Minimo');
    const niveles = within(tabla)
      .getAllByRole('row')
      .slice(1)
      .map((fila) => (within(fila).queryByText('Stock crítico') ? 'Critico' : 'Minimo'));
    expect(niveles).toEqual([
      ...Array<string>(criticos).fill('Critico'),
      ...Array<string>(minimos).fill('Minimo'),
    ]);

    const etiqueta = (texto: string) => screen.getByText(texto).parentElement!;
    expect(within(etiqueta('Críticos')).getByText(String(criticos))).toBeInTheDocument();
    expect(
      within(etiqueta('Total en alerta')).getByText(String(criticos + minimos)),
    ).toBeInTheDocument();
  });

  it('filtra por nivel con las pestañas', async () => {
    renderApp('/alertas', 'Almacenero');
    await userEvent.click(await screen.findByRole('tab', { name: /Críticas/ }));

    const tabla = screen.getByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    expect(within(tabla).queryByText('En el mínimo')).not.toBeInTheDocument();
    expect(within(tabla).getAllByText('Stock crítico')).toHaveLength(contar('Critico'));
  });

  it('muestra la acción de crear orden de compra solo al Reponedor', async () => {
    renderApp('/alertas', 'Reponedor');
    expect(
      (await screen.findAllByRole('button', { name: 'Crear orden de compra' })).length,
    ).toBeGreaterThan(0);
  });

  it('no muestra la acción de crear orden de compra al Almacenero', async () => {
    renderApp('/alertas', 'Almacenero');
    const tabla = await screen.findByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    expect(screen.queryByRole('button', { name: 'Crear orden de compra' })).not.toBeInTheDocument();
  });

  it('no permite al Vendedor acceder a las alertas', async () => {
    renderApp('/alertas', 'Vendedor');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Alertas de stock' })).not.toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad (axe)', async () => {
    const { container } = renderApp('/alertas', 'Almacenero');
    await screen.findByRole('table', { name: 'Productos en alerta' });
    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

/** Producto de prueba del catálogo: cemento, con existencias en cada ubicación. */
const PRODUCTO = PRODUCTOS[0]!;

/** Elige una opción de un selector buscable: escribe el texto y toca la opción que comienza con él. */
async function elegir(campo: RegExp, texto: string) {
  const input = screen.getByRole<HTMLInputElement>('combobox', { name: campo });
  await userEvent.type(input, texto);
  /* El listado se vincula con la etiqueta de su campo (aria-labelledby); los de otros campos pueden seguir en el DOM. */
  const listado = await waitFor(() => {
    const id = input.labels?.[0]?.id;
    const el = Array.from(document.querySelectorAll<HTMLElement>('[role="listbox"]'))
      .reverse()
      .find((l) => l.getAttribute('aria-labelledby') === id);
    if (!el) throw new Error('El listado de opciones no está abierto');
    return el;
  });
  await userEvent.click(await within(listado).findByText(new RegExp(`^${texto}`)));
}

/** Completa el formulario de movimiento con los valores indicados (campos opcionales en blanco si no se pasan). */
async function llenarMovimiento(datos: {
  tipo?: string;
  origen?: string;
  destino?: string;
  almacen?: string;
  cantidad: number;
  motivo?: string;
}) {
  if (datos.tipo) await userEvent.click(screen.getByRole('radio', { name: datos.tipo }));
  await elegir(/Producto/, PRODUCTO.sku);
  if (datos.origen) await elegir(/Almacén de origen/, datos.origen);
  if (datos.destino) await elegir(/Almacén de destino/, datos.destino);
  if (datos.almacen) await elegir(/^Almacén/, datos.almacen);
  await userEvent.type(screen.getByRole('textbox', { name: /Cantidad/ }), String(datos.cantidad));
  await userEvent.type(
    screen.getByRole('textbox', { name: /Motivo/ }),
    datos.motivo ?? 'Ajuste por conteo físico del almacén',
  );
}

/* Cada caso teclea el formulario completo; el margen por omisión de 5 s no basta en la suite completa. */
describe('Registrar movimiento (P13)', { timeout: 30000 }, () => {
  it('registra una entrada por ajuste y muestra la constancia con antes y después', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');
    const antes = PRODUCTO.almacen1;

    await llenarMovimiento({ almacen: 'Almacén 1', cantidad: 5 });
    await userEvent.click(screen.getByRole('button', { name: 'Revisar movimiento' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar entrada por ajuste' });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar movimiento' }));

    const constancia = await screen.findByRole('status', { name: 'Movimiento registrado' });
    const tabla = within(constancia).getByRole('table', { name: 'Existencia antes y después' });
    expect(within(tabla).getByText(formatNumber(antes))).toBeInTheDocument();
    expect(within(tabla).getByText(formatNumber(antes + 5))).toBeInTheDocument();
  });

  it('registra un traslado como dos movimientos vinculados', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');

    await llenarMovimiento({
      tipo: 'Traslado interno',
      origen: 'Almacén 1',
      destino: 'Tienda principal',
      cantidad: 10,
    });
    await userEvent.click(screen.getByRole('button', { name: 'Revisar movimiento' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar traslado interno' });
    expect(
      within(dialogo).getByText('Se registrarán 2 movimientos en el Kardex.'),
    ).toBeInTheDocument();
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar movimiento' }));

    const constancia = await screen.findByRole('status', { name: 'Movimiento registrado' });
    expect(within(constancia).getByText(/dos movimientos vinculados/)).toBeInTheDocument();
    const tabla = within(constancia).getByRole('table', { name: 'Existencia antes y después' });
    expect(within(tabla).getAllByRole('row')).toHaveLength(3);
  });

  it('bloquea una salida que supera la existencia del almacén con el motivo visible', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');
    const hay = PRODUCTO.tienda;

    await llenarMovimiento({
      tipo: 'Salida por merma o ajuste',
      almacen: 'Tienda principal',
      cantidad: hay + 1,
    });

    const motivo = `Stock insuficiente: hay ${hay} y se solicitan ${hay + 1}`;
    expect((await screen.findAllByText(motivo)).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Revisar movimiento' })).toBeDisabled();
  });

  it('bloquea un traslado que supera la existencia del almacén de origen', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');
    const hay = PRODUCTO.almacen1;

    await llenarMovimiento({
      tipo: 'Traslado interno',
      origen: 'Almacén 1',
      destino: 'Almacén 2',
      cantidad: hay + 1,
    });

    expect(
      (await screen.findAllByText(`Stock insuficiente: hay ${hay} y se solicitan ${hay + 1}`))
        .length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Revisar movimiento' })).toBeDisabled();
  });

  it('exige un motivo de al menos diez caracteres', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');

    await llenarMovimiento({ almacen: 'Almacén 2', cantidad: 2, motivo: 'Corto' });
    await userEvent.click(screen.getByRole('button', { name: 'Revisar movimiento' }));

    expect(
      await screen.findByText('Ingresar el motivo con al menos 10 caracteres.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('exige almacenes de origen y destino distintos en un traslado', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');

    await llenarMovimiento({
      tipo: 'Traslado interno',
      origen: 'Almacén 1',
      destino: 'Almacén 1',
      cantidad: 1,
    });
    await userEvent.click(screen.getByRole('button', { name: 'Revisar movimiento' }));

    expect(
      await screen.findByText('El almacén de destino debe ser distinto del de origen.'),
    ).toBeInTheDocument();
  });

  it('no permite al Vendedor registrar movimientos', async () => {
    renderApp('/kardex/nuevo', 'Vendedor');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Registrar movimiento' })).not.toBeInTheDocument();
  });

  it('el formulario y la constancia no tienen violaciones de accesibilidad (axe)', async () => {
    const { container } = renderApp('/kardex/nuevo', 'Almacenero');
    await screen.findByRole('button', { name: 'Revisar movimiento' });
    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

/** Abre la hoja de filtros. Sin `matchMedia` el viewport se toma como teléfono, donde los filtros van en la hoja. */
async function abrirFiltros() {
  await userEvent.click(await screen.findByRole('button', { name: /^Filtros/ }));
  return screen.findByRole('dialog', { name: 'Filtros' });
}

describe('Movimientos y Kardex (P14)', { timeout: 30000 }, () => {
  it('rotula cada movimiento con su signo y su origen, y muestra el par de un traslado', async () => {
    renderApp('/kardex', 'Almacenero');

    expect(await screen.findByText('−200')).toBeInTheDocument();
    expect(screen.getAllByText('Traslado')).toHaveLength(2);
    expect(screen.getByText('Hacia Tienda principal')).toBeInTheDocument();
    expect(screen.getByText('Desde Almacén 2')).toBeInTheDocument();
  });

  it('combina tipo y origen y muestra cada filtro como etiqueta', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    const hoja = await abrirFiltros();
    await elegir(/^Tipo/, 'Salida');
    await elegir(/^Origen/, 'Ajuste');
    await userEvent.click(within(hoja).getByRole('button', { name: 'Ver resultados' }));

    const tabla = screen.getByRole('table', { name: 'Movimientos de Kardex' });
    await waitFor(() => expect(within(tabla).queryByText('−200')).not.toBeInTheDocument());
    expect(within(tabla).getByText('−3')).toBeInTheDocument();
    expect(screen.getByText('Tipo: Salida')).toBeInTheDocument();
    expect(screen.getByText('Origen: Ajuste')).toBeInTheDocument();
  });

  it('filtra por origen traslado y muestra los dos movimientos del traslado', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    const hoja = await abrirFiltros();
    await elegir(/^Origen/, 'Traslado');
    await userEvent.click(within(hoja).getByRole('button', { name: 'Ver resultados' }));

    const tabla = screen.getByRole('table', { name: 'Movimientos de Kardex' });
    await waitFor(() => expect(within(tabla).queryByText('−200')).not.toBeInTheDocument());
    expect(within(tabla).getAllByText('Traslado')).toHaveLength(2);
  });

  it('filtra por ubicación y por rango de fechas', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    const hoja = await abrirFiltros();
    await elegir(/^Ubicación/, 'Almacén 2');
    await userEvent.click(within(hoja).getByRole('button', { name: 'Ver resultados' }));
    const tabla = screen.getByRole('table', { name: 'Movimientos de Kardex' });
    await waitFor(() => expect(within(tabla).queryByText('−200')).not.toBeInTheDocument());
    expect(within(tabla).getAllByText('Traslado')).toHaveLength(1);

    const hoja2 = await abrirFiltros();
    await userEvent.click(within(hoja2).getByRole('button', { name: 'Limpiar filtros' }));
    fireEvent.change(within(hoja2).getByLabelText('Desde'), {
      target: { value: '2026-11-05' },
    });
    await userEvent.click(within(hoja2).getByRole('button', { name: 'Ver resultados' }));
    expect(await within(tabla).findByText('−200')).toBeInTheDocument();
    await waitFor(() => expect(within(tabla).queryByText('Traslado')).not.toBeInTheDocument());
  });

  it('busca por SKU o producto y quita una etiqueta de filtro', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    const hoja = await abrirFiltros();
    await userEvent.type(within(hoja).getByRole('searchbox', { name: /Buscar por SKU/ }), 'CEM');
    await userEvent.click(within(hoja).getByRole('button', { name: 'Ver resultados' }));
    const tabla = screen.getByRole('table', { name: 'Movimientos de Kardex' });
    await waitFor(() => expect(within(tabla).queryByText('TUB-006')).not.toBeInTheDocument());
    expect(within(tabla).getByText('CEM-001')).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole('button', { name: 'Quitar filtro Producto o SKU: CEM' }),
    );
    /* Los dos movimientos del traslado de TUB-006 vuelven a la lista. */
    expect(await within(tabla).findAllByText('TUB-006')).toHaveLength(2);
  });

  it('limpia todos los filtros y muestra el estado vacío cuando no hay coincidencias', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    const hoja = await abrirFiltros();
    await userEvent.type(within(hoja).getByRole('searchbox', { name: /Buscar por SKU/ }), 'zzz');
    await userEvent.click(within(hoja).getByRole('button', { name: 'Ver resultados' }));
    expect(await screen.findByText('Sin movimientos')).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole('button', { name: 'Limpiar filtros' })[0]!);
    expect(await screen.findByText('−200')).toBeInTheDocument();
    expect(screen.queryByText('Sin movimientos')).not.toBeInTheDocument();
  });

  it('abre el registro de movimiento desde la acción principal', async () => {
    renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');

    await userEvent.click(screen.getByRole('button', { name: 'Registrar movimiento' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Registrar movimiento' }),
    ).toBeInTheDocument();
  });

  it('muestra en el Kardex el movimiento recién registrado', async () => {
    renderApp('/kardex/nuevo', 'Almacenero');

    await llenarMovimiento({ almacen: 'Almacén 2', cantidad: 5 });
    await userEvent.click(screen.getByRole('button', { name: 'Revisar movimiento' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar entrada por ajuste' });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar movimiento' }));
    await screen.findByRole('status', { name: 'Movimiento registrado' });

    await userEvent.click(screen.getByRole('button', { name: 'Ver movimientos en el Kardex' }));

    expect(await screen.findByText('+5')).toBeInTheDocument();
  });

  it('no permite al Vendedor acceder al Kardex', async () => {
    renderApp('/kardex', 'Vendedor');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Movimientos y Kardex' })).not.toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad (axe)', async () => {
    const { container } = renderApp('/kardex', 'Almacenero');
    await screen.findByText('−200');
    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
