// Logo de la marca. Coloca el archivo en public/logo.png (fondo transparente).
// Si no existe, se muestra un isotipo de respaldo.
import { useState } from 'react';
import { Wrench } from 'lucide-react';

export default function Logo({ claro = false, compacto = false }: { claro?: boolean; compacto?: boolean }) {
  const [sinImagen, setSinImagen] = useState(false);
  return (
    <div className="flex items-center gap-3">
      {sinImagen ? (
        <span className={`grid size-10 place-items-center rounded-xl ${claro ? 'bg-white/10 text-white' : 'bg-primario text-white'}`}>
          <Wrench size={22} aria-hidden />
        </span>
      ) : (
        <img src="/logo.png" alt="Logo" className="h-10 w-auto" onError={() => setSinImagen(true)} />
      )}
      {!compacto && (
        <div className="leading-tight">
          <p className={`font-display text-lg font-extrabold tracking-tight ${claro ? 'text-white' : 'text-primario-oscuro'}`}>MIGAL</p>
          <p className={`text-xs font-medium uppercase tracking-widest ${claro ? 'text-white/60' : 'text-secundario'}`}>SmartOps</p>
        </div>
      )}
    </div>
  );
}
