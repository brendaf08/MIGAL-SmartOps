import { Button } from '@mantine/core';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';
import logo from '@/assets/progresol-logo.png';
import { Icon } from '@/components/ui/Icon/Icon';
import classes from './DocumentoImprimible.module.css';

export interface DatoDocumento {
  etiqueta: string;
  valor: ReactNode;
  mono?: boolean;
}

export interface ColumnaDocumento<T> {
  clave: string;
  titulo: string;
  numerica?: boolean;
  mono?: boolean;
  celda: (fila: T) => ReactNode;
}

/** Fila de totales al pie: el rótulo ocupa las columnas indicadas y el valor va en la última numérica. */
export interface PieDocumento {
  rotulo: string;
  valor: ReactNode;
  /** Posición (desde 0) de la columna donde va el valor; por omisión, la última. */
  columna?: number;
}

interface DocumentoImprimibleProps<T> {
  titulo: string;
  datos: DatoDocumento[];
  tablaTitulo: string;
  columnas: ColumnaDocumento<T>[];
  filas: T[];
  clave: (fila: T) => string;
  pie?: PieDocumento[];
  firmas?: string[];
  /** Ruta a la que vuelve el botón "Volver". */
  volverA: string;
  volverEtiqueta: string;
}

/**
 * Documento imprimible en una hoja A4 (orden de despacho, cotización, venta, orden de compra). La barra de acciones
 * queda fuera de la hoja y no se imprime; "Imprimir" abre el diálogo del navegador, desde donde también se guarda en PDF.
 */
export function DocumentoImprimible<T>({
  titulo,
  datos,
  tablaTitulo,
  columnas,
  filas,
  clave,
  pie = [],
  firmas = [],
  volverA,
  volverEtiqueta,
}: DocumentoImprimibleProps<T>) {
  const navigate = useNavigate();
  const ultima = columnas.length - 1;
  return (
    <>
      <div className={classes.barra} role="toolbar" aria-label="Acciones de impresión">
        <Button leftSection={<Icon name="print" />} onClick={() => window.print()}>
          Imprimir
        </Button>
        <Button variant="default" onClick={() => void navigate(volverA)}>
          {volverEtiqueta}
        </Button>
      </div>
      <article className={classes.hoja} aria-labelledby="titulo-documento">
        <header className={classes.encabezado}>
          <img src={logo} alt="Logotipo de Ferretería Progresol Migal" width={64} height={64} />
          <p className={classes.negocio}>Ferretería Progresol Migal</p>
        </header>
        <h1 id="titulo-documento" className={classes.titulo}>
          {titulo}
        </h1>
        <dl className={classes.datos}>
          {datos.map((d) => (
            <div key={d.etiqueta}>
              <dt>{d.etiqueta}</dt>
              <dd className={d.mono ? classes.mono : undefined}>{d.valor}</dd>
            </div>
          ))}
        </dl>
        <table className={classes.tabla}>
          <caption>{tablaTitulo}</caption>
          <thead>
            <tr>
              {columnas.map((c) => (
                <th key={c.clave} scope="col" className={c.numerica ? classes.numerico : undefined}>
                  {c.titulo}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filas.map((fila) => (
              <tr key={clave(fila)}>
                {columnas.map((c) => (
                  <td
                    key={c.clave}
                    className={[c.numerica ? classes.numerico : '', c.mono ? classes.mono : '']
                      .filter(Boolean)
                      .join(' ')}
                  >
                    {c.celda(fila)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
          {pie.length > 0 && (
            <tfoot>
              {pie.map((p) => (
                <tr key={p.rotulo}>
                  <th scope="row" colSpan={p.columna ?? ultima}>
                    {p.rotulo}
                  </th>
                  <td className={classes.numerico}>{p.valor}</td>
                  {Array.from({ length: ultima - (p.columna ?? ultima) }, (_, i) => (
                    <td key={i} />
                  ))}
                </tr>
              ))}
            </tfoot>
          )}
        </table>
        {firmas.length > 0 && (
          <ul className={classes.firmas}>
            {firmas.map((firma) => (
              <li key={firma}>
                <span className={classes.linea} aria-hidden="true" />
                {firma}
              </li>
            ))}
          </ul>
        )}
      </article>
    </>
  );
}
