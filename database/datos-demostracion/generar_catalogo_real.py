"""Genera el SQL del catálogo real de MIGAL a partir de la exportación de productos de Odoo.

Solo para el entorno de desarrollo local. El Excel y el SQL generado contienen los precios reales del negocio y no se
versionan: el SQL se escribe en ``database/datos-demostracion/local/``, que Git ignora.

Uso, desde la raíz del repositorio y con ``openpyxl`` instalado (``pip install openpyxl``):

    python database/datos-demostracion/generar_catalogo_real.py RUTA/Catalogo_Migal.xlsx [RUTA/precios.csv]

``precios.csv`` es opcional: líneas ``codigo,precio`` que completan los productos sin precio en la exportación (por
ejemplo, los tomados de notas de venta). Un producto que sigue sin precio se carga con precio 0 y se informa.

El SQL resultante se ejecuta después de ``demo.sql``. Es idempotente y realiza lo siguiente:

- Desactiva los productos de ejemplo (los SKU que no son códigos de Odoo de cinco dígitos), de modo que el OCR y el
  agente solo vean el catálogo real.
- Registra cada producto con su código de Odoo como SKU, su categoría de punto de venta y su unidad.
- Asigna existencias deterministas por código: cerca del 8 % agotado, cerca del 12 % bajo el mínimo y el resto con
  stock normal, para demostrar las alertas y la disponibilidad.
- Agrega los productos con precio al catálogo de los dos proveedores de ``demo.sql`` según su categoría.
"""

import csv
import sys
from decimal import Decimal
from pathlib import Path

import openpyxl

SALIDA = Path(__file__).parent / "local" / "catalogo-real.sql"

UNIDADES = {"Unidades": "unidad", "kg": "kg", "mtr": "metro", "m³": "m3"}

# Categorías de materiales pesados: stock mínimo mayor y existencias también en el Almacén 2.
PESADAS = {
    "Cemento y aglomerantes": 100,
    "Agregados": 10,
    "Ladrillos y bloques": 1000,
    "Fierro y acero de construcción": 100,
}
PROVEEDOR_PESADOS = "20100070970"  # Distribuidora Cementera del Sur (demo.sql)
PROVEEDOR_FERRETERIA = "20551122334"  # Ferretería Industrial Lima (demo.sql)
CATEGORIAS_FERRETERIA = {
    "Tuberías y conexiones",
    "Conducción eléctrica",
    "Tomas y controles",
    "Adhesivos y cintas",
    "Sujetadores y fijaciones",
}


def texto_sql(valor: str) -> str:
    return "'" + valor.replace("'", "''") + "'"


def leer_precios(ruta: str | None) -> dict[str, Decimal]:
    if not ruta:
        return {}
    with open(ruta, encoding="utf-8") as archivo:
        return {fila[0].strip().zfill(5): Decimal(fila[1]) for fila in csv.reader(archivo) if len(fila) >= 2}


def existencias(codigo: str, minimo: int, pesado: bool) -> tuple[int, int, int]:
    """Tienda, Almacén 1 y Almacén 2, deterministas por código."""
    cubeta = int(codigo) * 2654435761 % 100
    if cubeta < 8:
        return 0, 0, 0
    if cubeta < 20:
        return max(minimo // 4, 1), 0, 0
    return minimo, minimo * 2, minimo * 3 if pesado else 0


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    libro = openpyxl.load_workbook(sys.argv[1], read_only=True, data_only=True)
    maestros = {f[0]: f for f in list(libro["PRODUCTOS MAESTROS"].iter_rows(values_only=True))[1:] if f[0]}
    precios_extra = leer_precios(sys.argv[2] if len(sys.argv) > 2 else None)

    filas, sin_precio = [], []
    for fila in list(libro["PRODUCTOS"].iter_rows(values_only=True))[1:]:
        codigo, id_maestro, nombre, precio, unidad = fila[0], fila[1], fila[2], fila[6], fila[8]
        if not codigo:
            continue
        maestro = maestros.get(id_maestro)
        categoria = maestro[7] if maestro else None
        if not categoria:
            sys.exit(f"El producto {codigo} no tiene categoría.")
        if precio is None and maestro:
            precio = maestro[4]
        precio = Decimal(str(precio)) if precio is not None else precios_extra.get(codigo)
        if precio is None:
            sin_precio.append(codigo)
            precio = Decimal(0)
        minimo = PESADAS.get(categoria, 50 if unidad == "kg" else 10)
        tienda, alm1, alm2 = existencias(codigo, minimo, categoria in PESADAS)
        if categoria in PESADAS:
            proveedor = PROVEEDOR_PESADOS
        elif categoria in CATEGORIAS_FERRETERIA:
            proveedor = PROVEEDOR_FERRETERIA
        else:
            proveedor = None
        filas.append(
            f"\t\t({texto_sql(codigo)}, {texto_sql(nombre.strip())}, {texto_sql(UNIDADES.get(unidad, 'unidad'))}, "
            f"{precio:.2f}, {minimo}, {texto_sql(categoria)}, {tienda}, {alm1}, {alm2}, "
            f"{texto_sql(proveedor) if proveedor and precio > 0 else 'NULL'})"
        )

    SALIDA.parent.mkdir(exist_ok=True)
    SALIDA.write_text(PLANTILLA.replace("{filas}", ",\n".join(filas)), encoding="utf-8", newline="\n")
    print(f"{len(filas)} productos escritos en {SALIDA}")
    print(f"{len(sin_precio)} sin precio, cargados con precio 0.")


PLANTILLA = """-- Catálogo real de MIGAL generado por generar_catalogo_real.py. NO versionar: contiene los precios del negocio.
-- Solo para desarrollo local, después de demo.sql. Es idempotente.

DO $catalogo$
DECLARE
	v_admin bigint;
	v_faltantes text;
BEGIN
	SELECT min(id_usuario) INTO v_admin FROM usuario WHERE rol = 'administrador';
	IF v_admin IS NULL OR NOT EXISTS (SELECT 1 FROM almacen WHERE nombre = 'Almacén 1') THEN
		RAISE EXCEPTION 'Cargar primero datos-demostracion/demo.sql.';
	END IF;

	CREATE TEMP TABLE catalogo_real (
		sku text, nombre text, unidad text, precio numeric, minimo numeric, categoria text,
		tienda numeric, alm1 numeric, alm2 numeric, proveedor text
	) ON COMMIT DROP;
	INSERT INTO catalogo_real VALUES
{filas};

	SELECT string_agg(DISTINCT r.categoria, ', ') INTO v_faltantes
	  FROM catalogo_real r LEFT JOIN categoria c ON c.nombre = r.categoria WHERE c.id_categoria IS NULL;
	IF v_faltantes IS NOT NULL THEN
		RAISE EXCEPTION 'Categorías inexistentes: %', v_faltantes;
	END IF;

	-- Los productos de ejemplo dejan de ofrecerse al OCR y al agente; su historial se conserva.
	UPDATE producto SET estado_activo = false, fecha_actualizacion = now()
	 WHERE sku !~ '^[0-9]{5}$' AND estado_activo;

	CREATE TEMP TABLE catalogo_nuevo ON COMMIT DROP AS
	SELECT r.* FROM catalogo_real r WHERE NOT EXISTS (SELECT 1 FROM producto p WHERE p.sku = r.sku);

	INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
	SELECT n.sku, n.nombre, n.unidad, n.precio, n.minimo, c.id_categoria
	  FROM catalogo_nuevo n JOIN categoria c ON c.nombre = n.categoria;

	INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
	SELECT p.id_producto, a.id_almacen, s.cantidad
	  FROM catalogo_nuevo n
	  JOIN producto p ON p.sku = n.sku
	  CROSS JOIN LATERAL (VALUES ('Tienda', n.tienda), ('Almacén 1', n.alm1), ('Almacén 2', n.alm2)) AS s(ubicacion, cantidad)
	  JOIN almacen a ON a.nombre = s.ubicacion
	 WHERE s.cantidad > 0;

	INSERT INTO movimiento_kardex (id_inventario, fecha_hora, tipo_operacion, cantidad, motivo, id_usuario_responsable, origen_movimiento)
	SELECT i.id_inventario, now(), 'entrada', i.cantidad_fisica, 'Carga inicial del catálogo real', v_admin, 'ajuste'
	  FROM inventario i JOIN producto p ON p.id_producto = i.id_producto
	 WHERE p.sku IN (SELECT sku FROM catalogo_nuevo);

	INSERT INTO catalogo_proveedor (id_proveedor, id_producto, precio_compra, plazo_entrega_referencial_dias)
	SELECT pr.id_proveedor, p.id_producto, round(n.precio * 0.85, 2), CASE WHEN pr.ruc = '20100070970' THEN 3 ELSE 5 END
	  FROM catalogo_nuevo n
	  JOIN producto p ON p.sku = n.sku
	  JOIN proveedor pr ON pr.ruc = n.proveedor;

	RAISE NOTICE 'Productos nuevos del catálogo real: %', (SELECT count(*) FROM catalogo_nuevo);
END
$catalogo$;
"""

if __name__ == "__main__":
    main()
