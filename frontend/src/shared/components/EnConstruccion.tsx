import { Hammer } from 'lucide-react';
import { EncabezadoPagina, EstadoVacio, Tarjeta } from './ui';

export default function EnConstruccion({ codigo, titulo }: { codigo: string; titulo: string }) {
  return (
    <>
      <EncabezadoPagina titulo={titulo} descripcion={`Pantalla ${codigo}`} />
      <Tarjeta>
        <EstadoVacio icono={<Hammer size={26} aria-hidden />} titulo="En construcción"
          descripcion="Esta pantalla se está diseñando. Muy pronto estará disponible." />
      </Tarjeta>
    </>
  );
}
