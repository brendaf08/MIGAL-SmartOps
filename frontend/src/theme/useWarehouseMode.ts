import { useCallback, useEffect, useState } from 'react';

/** Clave de localStorage del modo almacén; la elección es por equipo, no por usuario. */
export const WAREHOUSE_MODE_STORAGE_KEY = 'migal-warehouse-mode';

const leer = () => {
  try {
    return window.localStorage.getItem(WAREHOUSE_MODE_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
};

/**
 * Modo almacén: sube los controles a 52–56 px y muestra las etiquetas de la navegación en tablet.
 * Se conserva en el equipo y se aplica con el atributo `data-warehouse` del elemento raíz, de modo que
 * también alcanza a los diálogos y menús que se montan fuera de la estructura.
 */
export function useWarehouseMode() {
  const [warehouse, setWarehouse] = useState(leer);

  useEffect(() => {
    const raiz = document.documentElement;
    if (warehouse) raiz.dataset.warehouse = 'true';
    else delete raiz.dataset.warehouse;
    try {
      window.localStorage.setItem(WAREHOUSE_MODE_STORAGE_KEY, String(warehouse));
    } catch {
      // Sin almacenamiento disponible, el modo dura solo la sesión.
    }
    return () => {
      delete raiz.dataset.warehouse;
    };
  }, [warehouse]);

  const toggle = useCallback(() => setWarehouse((actual) => !actual), []);

  return { warehouse, toggle };
}
