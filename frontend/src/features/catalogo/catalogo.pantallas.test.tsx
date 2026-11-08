import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { PRODUCTOS } from '@/features/inventario/fixtures';

afterEach(() => window.sessionStorage.clear());

const contar = (predicado: (sku: string, categoria: string) => boolean) =>
  PRODUCTOS.filter((p) => predicado(p.sku, p.categoria)).length;

describe('Catálogo, lista', () => {
  it('muestra 20 productos por página y cambia de página', async () => {
    renderApp('/catalogo', 'Almacenero');
    const table = await screen.findByRole('table', { name: 'Catálogo de productos' });
    await within(table).findByText(PRODUCTOS[0]!.sku);
    expect(within(table).getAllByRole('row')).toHaveLength(21);

    await userEvent.click(screen.getByRole('button', { name: 'Página siguiente' }));

    await within(table).findByText(PRODUCTOS[20]!.sku);
  });

  it('busca por SKU y filtra por categoría', async () => {
    renderApp('/catalogo', 'Almacenero');
    const table = await screen.findByRole('table', { name: 'Catálogo de productos' });
    await within(table).findByText(PRODUCTOS[0]!.sku);

    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar producto' }), 'CEM-0');
    const cemento = contar((sku) => sku.startsWith('CEM-'));
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(cemento + 1));

    await userEvent.clear(screen.getByRole('searchbox', { name: 'Buscar producto' }));
    await userEvent.click(screen.getByRole('combobox', { name: 'Filtrar por categoría' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Ladrillos', hidden: true }));
    const ladrillos = contar((_, c) => c === 'Ladrillos');
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(ladrillos + 1));
  });

  it('filtra por estado y muestra el estado vacío sin coincidencias', async () => {
    renderApp('/catalogo', 'Almacenero');
    await screen.findByRole('table', { name: 'Catálogo de productos' });

    await userEvent.click(screen.getByRole('combobox', { name: 'Filtrar por estado' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Inactivo', hidden: true }));

    expect(await screen.findByText('Sin resultados')).toBeInTheDocument();
  });

  it('no permite a un Vendedor acceder al catálogo', async () => {
    renderApp('/catalogo', 'Vendedor');

    expect(
      await screen.findByRole('table', { name: 'Disponibilidad por producto' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('table', { name: 'Catálogo de productos' }),
    ).not.toBeInTheDocument();
  });
});

/** Elige una opción de un `Select` de Mantine por su nombre accesible. */
async function elegir(campo: string, opcion: string) {
  await userEvent.click(screen.getByRole('combobox', { name: campo }));
  await userEvent.click(await screen.findByRole('option', { name: opcion, hidden: true }));
}

/** Completa el formulario con valores válidos salvo los indicados. */
async function llenarFormulario(valores: { sku?: string; nombre?: string; minimo?: string } = {}) {
  if (valores.sku !== undefined) {
    await userEvent.type(await screen.findByRole('textbox', { name: 'SKU' }), valores.sku);
  }
  await userEvent.type(
    screen.getByRole('textbox', { name: 'Nombre oficial' }),
    valores.nombre ?? 'Producto de prueba',
  );
  await elegir('Categoría', 'Otras');
  await elegir('Unidad de medida', 'kg');
  await userEvent.type(screen.getByRole('textbox', { name: 'Stock mínimo' }), valores.minimo ?? '5');
}

describe('Catálogo, formulario', () => {
  it('registra un producto válido y confirma el registro', async () => {
    renderApp('/catalogo/nuevo', 'Almacenero');

    await llenarFormulario({ sku: 'tst-001' });
    await userEvent.click(screen.getByRole('button', { name: 'Registrar producto' }));

    expect(await screen.findByText('Producto registrado')).toBeInTheDocument();
  });

  it('bloquea el guardado con un SKU que ya existe', async () => {
    renderApp('/catalogo/nuevo', 'Administrador');

    await llenarFormulario({ sku: 'CEM-001' });
    await userEvent.click(screen.getByRole('button', { name: 'Registrar producto' }));

    expect(
      await screen.findByText('El SKU ya existe. Ingresar un código distinto.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Registrar producto' })).toBeInTheDocument();
  });

  it('valida el formato del SKU, el nombre y que el stock mínimo no sea negativo', async () => {
    renderApp('/catalogo/nuevo', 'Almacenero');

    await llenarFormulario({ sku: 'ab1', nombre: 'AB', minimo: '-1' });
    await userEvent.click(screen.getByRole('button', { name: 'Registrar producto' }));

    expect(await screen.findByText('Usar el formato AAA-000, por ejemplo CEM-049.')).toBeInTheDocument();
    expect(screen.getByText('Ingresar al menos 3 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('El stock mínimo no puede ser negativo.')).toBeInTheDocument();
  });

  it('pide el stock mínimo y la unidad cuando faltan', async () => {
    renderApp('/catalogo/nuevo', 'Almacenero');

    await userEvent.type(await screen.findByRole('textbox', { name: 'SKU' }), 'TST-002');
    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre oficial' }), 'Producto sin unidad');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar producto' }));

    expect(await screen.findByText('Ingresar el stock mínimo.')).toBeInTheDocument();
    expect(screen.getByText('Seleccionar la unidad de medida.')).toBeInTheDocument();
  });

  it('muestra la vista previa del efecto del stock mínimo', async () => {
    renderApp('/catalogo/nuevo', 'Almacenero');

    await userEvent.type(await screen.findByRole('textbox', { name: 'Stock mínimo' }), '200');

    expect(await screen.findByText('Alerta cuando el stock libre sea 200 o menos.')).toBeInTheDocument();
  });

  it('modifica la unidad y el stock mínimo sin permitir cambiar el SKU', async () => {
    renderApp('/catalogo/CEM-001/editar', 'Almacenero');
    const sku = await screen.findByRole('textbox', { name: 'SKU' });
    expect(sku).toHaveValue('CEM-001');
    expect(sku).toHaveAttribute('readonly');

    await elegir('Unidad de medida', 'rollo');
    const minimo = screen.getByRole('textbox', { name: 'Stock mínimo' });
    await userEvent.clear(minimo);
    await userEvent.type(minimo, '250');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Cambios guardados')).toBeInTheDocument();
  });

  it('no permite a un Vendedor acceder al formulario', async () => {
    renderApp('/catalogo/nuevo', 'Vendedor');

    expect(
      await screen.findByRole('table', { name: 'Disponibilidad por producto' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'SKU' })).not.toBeInTheDocument();
  });
});
