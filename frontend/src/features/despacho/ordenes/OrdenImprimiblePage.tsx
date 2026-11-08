import { Button, Skeleton } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router';
import logo from '@/assets/progresol-logo.png';
import { Alert } from '@/components/ui/Alert/Alert';
import { Icon } from '@/components/ui/Icon/Icon';
import { formatDate, formatNumber } from '@/lib/format';
import { obtenerDocumentoOrden } from './api';
import classes from './OrdenImprimible.module.css';

const FIRMAS = ['Despachó', 'Chofer', 'Recibió'];

/** CU-06, orden de despacho imprimible en una hoja A4. La barra de acciones queda fuera del documento y no se imprime. */
export function OrdenImprimiblePage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ['orden-imprimible', id],
    queryFn: () => obtenerDocumentoOrden(id),
  });

  if (query.isError) {
    return (
      <Alert tone="warning" title="La orden no existe">
        Volver al listado y elegir otra orden.
      </Alert>
    );
  }
  if (!query.data) return <Skeleton height={240} />;

  const { orden, fecha, direccion, lineas } = query.data;

  return (
    <>
      <div className={classes.barra} role="toolbar" aria-label="Acciones de impresión">
        <Button leftSection={<Icon name="print" />} onClick={() => window.print()}>
          Imprimir
        </Button>
        <Button variant="default" onClick={() => void navigate(`/despachos/${orden.id}`)}>
          Volver a la orden
        </Button>
      </div>
      <article className={classes.hoja} aria-labelledby="titulo-orden">
        <header className={classes.encabezado}>
          <img src={logo} alt="Logotipo de Ferretería Progresol Migal" width={64} height={64} />
          <p className={classes.negocio}>Ferretería Progresol Migal</p>
        </header>
        <h1 id="titulo-orden" className={classes.titulo}>
          Orden de despacho {orden.id}
        </h1>
        <dl className={classes.datos}>
          <div>
            <dt>Fecha</dt>
            <dd>{formatDate(fecha)}</dd>
          </div>
          <div>
            <dt>Venta</dt>
            <dd className={classes.mono}>{orden.venta}</dd>
          </div>
          <div>
            <dt>Cliente</dt>
            <dd>{orden.cliente}</dd>
          </div>
          <div>
            <dt>Dirección de entrega</dt>
            <dd>{direccion}</dd>
          </div>
        </dl>
        <table className={classes.tabla}>
          <caption>Productos a despachar</caption>
          <thead>
            <tr>
              <th scope="col">SKU</th>
              <th scope="col">Producto</th>
              <th scope="col" className={classes.numerico}>
                Cantidad
              </th>
              <th scope="col">Unidad</th>
            </tr>
          </thead>
          <tbody>
            {lineas.map((l) => (
              <tr key={l.sku}>
                <td className={classes.mono}>{l.sku}</td>
                <td>{l.nombre}</td>
                <td className={classes.numerico}>{formatNumber(l.cantidad)}</td>
                <td>{l.unidad}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={2}>
                Total de líneas
              </th>
              <td className={classes.numerico}>{formatNumber(lineas.length)}</td>
              <td />
            </tr>
          </tfoot>
        </table>
        <ul className={classes.firmas}>
          {FIRMAS.map((firma) => (
            <li key={firma}>
              <span className={classes.linea} aria-hidden="true" />
              {firma}
            </li>
          ))}
        </ul>
      </article>
    </>
  );
}
