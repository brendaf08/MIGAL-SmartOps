#!/usr/bin/env python3
"""Siembra datos de demostración de MIGAL SmartOps a través de la API REST real.

Usa los flujos del negocio (cotización, venta, guía, orden de compra, recepción, Kardex), de modo que el stock, las
reservas y el historial queden consistentes. Requiere el catálogo real cargado y un usuario con todos los permisos
(el rol de demostración). Las credenciales salen del entorno o de .env (MIGAL_DEMO_USERNAME y MIGAL_DEMO_CLAVE) y no
se imprimen.

Uso:
    python scripts/sembrar-demo.py --dry-run
    python scripts/sembrar-demo.py
    python scripts/sembrar-demo.py --con-ocr --imagenes Documentos-Escanear
"""

import argparse
import json
import mimetypes
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import date, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent


def natural(texto):
    """Texto para guardar: sin nombres de archivo entre paréntesis."""
    return re.sub(r"\s*\([^)]*\)", "", texto).strip()


CLIENTES = [
    ("20600100011", "Constructora Andina del Perú S.A.C.", "999000011", "Av. Los Constructores 120, Lima"),
    ("20600100022", "Inmobiliaria Los Olivos S.A.C.", "999000022", "Jr. Las Flores 45, Los Olivos"),
    ("20600100033", "Ferretería El Maestro E.I.R.L.", "999000033", "Av. Túpac Amaru 890, Comas"),
    ("20600100044", "Servicios Generales Pachacútec S.R.L.", "999000044", "Calle Los Pinos 310, Ate"),
    ("20600100055", "Corporación Vial del Sur S.A.", "999000055", "Av. Industrial 2200, Villa El Salvador"),
    ("20600100066", "Edificaciones Miraflores S.A.C.", "999000066", "Av. Larco 1150, Miraflores"),
    ("20600100077", "Contratistas Unidos del Perú S.A.C.", "999000077", "Jr. Huancavelica 560, Cercado de Lima"),
    ("20600100088", "Carpintería Metálica San José E.I.R.L.", "999000088", "Av. Pachacútec 75, San Juan de Miraflores"),
    ("20600100099", "Minera Pampa Verde S.A.C.", "999000099", "Av. El Sol 400, Arequipa"),
]
CLIENTE_MOSTRADOR = "Cliente de mostrador (demo)"

# razón social, contacto, condiciones, catálogo [(sku, precio de compra, plazo en días)]
PROVEEDORES = {
    "20600200011": ("Inversiones Reyfam S.A.C.", "Ventas Reyfam; 999000101", "Pago contra entrega",
                    [("00135", 8.0, 3), ("00245", 6.29, 3), ("00244", 6.21, 3), ("00246", 6.29, 3), ("00110", 5.18, 3),
                     ("00056", 39.2, 5), ("00323", 9.36, 3), ("00324", 8.8, 3), ("00029", 13.62, 5), ("00030", 10.77, 5),
                     ("00202", 21.9, 5), ("00448", 10.62, 3), ("00140", 225.0, 7), ("00662", 3.2, 3), ("00447", 10.62, 3),
                     ("00185", 16.32, 5)]),
    "20600200022": ("Corporación Paredmetal S.A.C.", "Ventas Paredmetal; 999000102", "Crédito a 15 días",
                    [("00059", 47.0, 7), ("00032", 1.35, 7), ("00272", 1.75, 7), ("00230", 95.0, 7), ("00289", 51.0, 7),
                     ("00060", 62.5, 7), ("00058", 49.5, 7)]),
    "20600200033": ("Comercial S & C Castillo S.A.C.", "Ventas Castillo; 999000103", "Pago contado",
                    [("00295", 19.5, 3), ("00298", 26.3, 3), ("00293", 38.3, 3), ("00290", 14.4, 3)]),
    "20600200044": ("Industrias Jhomeron S.A.", "Ventas Jhomeron; 999000104", "Crédito a 30 días",
                    [("00116", 8.5, 5), ("00120", 8.5, 5), ("00238", 12.0, 5), ("00274", 3.3, 5), ("00337", 13.0, 5),
                     ("00119", 2.5, 5), ("00378", 18.82, 5), ("00375", 18.82, 5)]),
    "20600200055": ("Carpinorte S.A.C.", "Ventas Carpinorte; 999000105", "Pago contado",
                    [("00054", 27.0, 4), ("00055", 37.5, 4), ("00051", 13.5, 4), ("00052", 20.0, 4)]),
}

# Notas de venta del talonario (líneas: sku, cantidad, precio). Los SKU repetidos se suman.
NOTA_326 = [("00762", 96, 50.6), ("00755", 110, 33), ("00760", 100, 18.6), ("00763", 90, 13.9), ("00017", 50, 5.5),
            ("00018", 40, 5.5), ("00079", 20, 6.5), ("00064", 500, 31), ("00743", 60, 65), ("00077", 20, 80),
            ("00160", 5000, 0.9), ("00758", 26, 75), ("00762", 20, 50.6), ("00760", 100, 18.6), ("00755", 100, 33)]
NOTA_327 = [("00756", 70, 8), ("00017", 80, 5.5), ("00018", 30, 5.5), ("00165", 1000, 3.0), ("00306", 80, 5.7),
            ("00103", 200, 0.6), ("00760", 25, 18.6), ("00064", 30, 31), ("00160", 6000, 0.9), ("00162", 3000, 0.74),
            ("00064", 150, 31), ("00306", 80, 5.7), ("00103", 200, 0.7)]
NOTA_329 = [("00295", 20, 23), ("00293", 3, 43), ("00454", 9, 29), ("00085", 100, 2.5), ("00315", 100, 2.0),
            ("00086", 100, 3.5), ("00096", 21, 8), ("00014", 24, 1), ("00286", 10, 33), ("00131", 50, 2.5),
            ("00657", 30, 2.8), ("00133", 30, 2.5), ("00288", 15, 1.8)]
NOTA_218 = [("00046", 2, 175), ("00740", 80, 4.6), ("00306", 6, 5.5), ("00103", 6, 0.6)]
NOTA_663 = [("00165", 120, 3.2), ("00743", 3, 65), ("00077", 1, 75), ("00064", 60, 30), ("00291", 2, 17),
            ("00301", 1, 34)]
NOTA_330 = [("00762", 60, 50.6), ("00760", 50, 18.6)]
NOTA_773 = [("00762", 37, 55.3), ("00755", 29, 35.8), ("00760", 50, 20.2)]

# cliente (índice), nota, estado final, descripción
COTIZACIONES = [
    (0, "326", NOTA_326, "validada_venta", "Nota de venta N° 326 (1.jpg)"),
    (1, "327", NOTA_327, "aceptada", "Nota de venta N° 327 (2.jpg)"),
    (2, "329", NOTA_329, "rechazada", "Nota de venta N° 329 (4.jpg)"),
    (3, "218", NOTA_218, "despachada", "Nota de venta N° 218 (PHOTO-2026-06-05-19-00-57)"),
    (4, "663", NOTA_663, "guia_emitida", "Nota de venta N° 663 (PHOTO-2026-06-08-19-27-51)"),
    (5, "330", NOTA_330, "emitida", "Nota de venta N° 330 (PHOTO-2026-08-27-17-56-45)"),
    (6, "773", NOTA_773, "dividida", "Nota de venta N° 773 (PHOTO-2026-11-08-11-39-46)"),
]

# (proveedor RUC, líneas [(sku, cantidad, costo)], estado final, descripción)
ORDENES = [
    ("20600200011", [("00135", 6, 8.0), ("00245", 3, 6.29), ("00244", 3, 6.21), ("00246", 6, 6.29), ("00110", 6, 5.18),
                     ("00056", 3, 39.2), ("00323", 3, 9.36), ("00324", 6, 8.8), ("00029", 6, 13.62), ("00030", 6, 10.77),
                     ("00202", 12, 21.9)], "pendiente", "Factura Reyfam (PHOTO-2026-06-19-22-35-14)"),
    ("20600200055", [("00054", 25, 27.0), ("00055", 40, 37.5), ("00051", 20, 13.5), ("00052", 20, 20.0)],
     "parcial", "Guía y factura Carpinorte (PHOTO-2026-06-19-22-39-51 y -35-58)"),
    ("20600200033", [("00295", 25, 19.5), ("00298", 25, 26.3), ("00293", 15, 38.3), ("00290", 20, 14.4)],
     "completada", "Factura Castillo (PHOTO-2026-06-19-22-38-06)"),
    ("20600200022", [("00059", 3, 47.0), ("00032", 6, 1.35), ("00272", 6, 1.75), ("00230", 1, 95.0), ("00289", 4, 51.0),
                     ("00060", 4, 62.5), ("00058", 5, 49.5)], "pendiente", "Factura Paredmetal (PHOTO-2026-06-19-22-38-19)"),
    ("20600200044", [("00116", 2, 8.5), ("00120", 6, 8.5), ("00238", 4, 12.0), ("00274", 12, 3.3), ("00337", 6, 13.0),
                     ("00119", 6, 2.5)], "pendiente", "Factura Jhomeron (PHOTO-2026-06-19-22-38-35)"),
]

# Ventas directas de mostrador: (SKU, cantidad, precio) y destino final
VENTAS_DIRECTAS = [
    ([("00064", 12, 31), ("00743", 2, 65)], "despachada"),
    ([("00046", 1, 175), ("00306", 4, 5.7), ("00103", 4, 0.6)], "despachada"),
    ([("00295", 6, 23), ("00085", 20, 2.5)], "guia_anulada"),
    ([("00160", 300, 0.9)], "pendiente_despacho"),
]

# Movimientos manuales: tipo, SKU, almacén, cantidad, motivo (el traslado usa origen y destino)
MOVIMIENTOS = [
    ("entrada", "00064", "Tienda", 40, "Ajuste por conteo físico mensual de cemento"),
    ("salida", "00160", "Almacén 2", 25, "Merma por rotura de ladrillos en el patio"),
    ("entrada", "00762", "Almacén 1", 30, "Ajuste por varillas encontradas en el conteo"),
    ("salida", "00291", "Almacén 1", 2, "Merma por tubos de desagüe rotos en el traslado"),
    ("traslado", "00760", ("Almacén 1", "Tienda"), 20, "Reposición de la tienda con fierro de 3/8"),
]

# imagen, tipo, qué hacer (validar_y_asociar_cotizacion / validar_y_asociar_recepcion / solo_procesar)
DOCUMENTOS_OCR = [
    ("PHOTO-2026-06-05-19-00-57.jpg", "solicitud_cliente", "cotizacion:218"),
    ("PHOTO-2026-06-19-22-38-06.jpg", "documento_recepcion", "recepcion:20600200033"),
    ("2.jpg", "solicitud_cliente", None),
    ("PHOTO-2026-06-19-22-39-51.jpg", "documento_recepcion", None),
]


class ErrorApi(Exception):
    def __init__(self, estado, codigo, detalle):
        super().__init__(f"HTTP {estado} {codigo}: {detalle}")
        self.estado, self.codigo, self.detalle = estado, codigo, detalle


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/") + "/api/v1"
        self.token = None

    def _pedir(self, metodo, ruta, cuerpo=None, consulta=None, cabeceras=None, bruto=None):
        url = self.base + ruta
        if consulta:
            url += "?" + urllib.parse.urlencode({k: v for k, v in consulta.items() if v is not None})
        datos = bruto
        encabezados = {"Accept": "application/json", **(cabeceras or {})}
        if cuerpo is not None:
            datos = json.dumps(cuerpo).encode("utf-8")
            encabezados["Content-Type"] = "application/json"
        if self.token:
            encabezados["Authorization"] = "Bearer " + self.token
        if metodo == "POST":
            encabezados.setdefault("Idempotency-Key", str(uuid.uuid4()))
        peticion = urllib.request.Request(url, data=datos, headers=encabezados, method=metodo)
        try:
            with urllib.request.urlopen(peticion, timeout=120) as respuesta:
                texto = respuesta.read().decode("utf-8")
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as error:
            texto = error.read().decode("utf-8", "replace")
            try:
                problema = json.loads(texto)
            except ValueError:
                problema = {}
            raise ErrorApi(error.code, problema.get("code") or problema.get("codigo") or "", problema.get("detail")
                           or problema.get("title") or texto[:200]) from None

    def get(self, ruta, **consulta):
        return self._pedir("GET", ruta, consulta=consulta)

    def post(self, ruta, cuerpo=None):
        return self._pedir("POST", ruta, cuerpo=cuerpo)

    def put(self, ruta, cuerpo):
        return self._pedir("PUT", ruta, cuerpo=cuerpo)

    def subir(self, ruta, campos, archivo):
        limite = "----migal" + uuid.uuid4().hex
        partes = []
        for nombre, valor in campos.items():
            partes.append(f'--{limite}\r\nContent-Disposition: form-data; name="{nombre}"\r\n\r\n{valor}\r\n'.encode())
        tipo = mimetypes.guess_type(archivo.name)[0] or "application/octet-stream"
        partes.append((f'--{limite}\r\nContent-Disposition: form-data; name="archivo"; filename="{archivo.name}"\r\n'
                       f"Content-Type: {tipo}\r\n\r\n").encode() + archivo.read_bytes() + b"\r\n")
        partes.append(f"--{limite}--\r\n".encode())
        return self._pedir("POST", ruta, bruto=b"".join(partes),
                           cabeceras={"Content-Type": f"multipart/form-data; boundary={limite}"})

    def paginas(self, ruta, **consulta):
        pagina, resultado = 1, []
        while True:
            respuesta = self.get(ruta, page=pagina, pageSize=100, **consulta)
            resultado += respuesta["items"]
            if len(resultado) >= respuesta["total"] or not respuesta["items"]:
                return resultado
            pagina += 1


def leer_credenciales():
    valores = {k: os.environ.get(k) for k in ("MIGAL_DEMO_USERNAME", "MIGAL_DEMO_CLAVE")}
    archivo = RAIZ / ".env"
    if archivo.exists() and not all(valores.values()):
        for linea in archivo.read_text(encoding="utf-8").splitlines():
            linea = linea.strip()
            if not linea or linea.startswith("#") or "=" not in linea:
                continue
            clave, _, valor = linea.partition("=")
            clave = clave.strip()
            if clave in valores and not valores[clave]:
                valores[clave] = valor.strip().strip('"').strip("'")
    if not all(valores.values()):
        sys.exit("Faltan MIGAL_DEMO_USERNAME y MIGAL_DEMO_CLAVE (entorno o .env).")
    return valores["MIGAL_DEMO_USERNAME"], valores["MIGAL_DEMO_CLAVE"]


def fundir(lineas):
    """Suma las cantidades de los SKU repetidos y conserva el primer precio."""
    por_sku = {}
    for sku, cantidad, precio in lineas:
        if sku in por_sku:
            por_sku[sku][0] += cantidad
        else:
            por_sku[sku] = [cantidad, precio]
    return [(sku, c, p) for sku, (c, p) in por_sku.items()]


class Sembrador:
    def __init__(self, api, forzar, carpeta_imagenes):
        self.api = api
        self.forzar = forzar
        self.imagenes = carpeta_imagenes
        self.productos = {}
        self.clientes = {}
        self.proveedores = {}
        self.ubicaciones = {}
        self.cotizaciones = {}
        self.recepciones = {}
        self.cuenta = {}

    def contar(self, clave, n=1):
        self.cuenta[clave] = self.cuenta.get(clave, 0) + n

    def aviso(self, texto):
        print("  ! " + texto)

    def producto(self, sku):
        if sku not in self.productos:
            pagina = self.api.get("/productos", sku=sku, pageSize=1)
            self.productos[sku] = pagina["items"][0] if pagina["items"] else None
        return self.productos[sku]

    def libre(self, sku):
        producto = self.producto(sku)
        return float(self.api.get(f"/productos/{producto['id']}/existencias")["libre"]) if producto else 0.0

    def lineas_validas(self, lineas, limitar=False):
        """Solo los SKU del catálogo; con limitar, se completa con un ajuste el stock que falte para despachar."""
        resultado = []
        for sku, cantidad, precio in fundir(lineas):
            if not self.producto(sku):
                self.aviso(f"SKU {sku} no está en el catálogo; se omite.")
                continue
            if limitar:
                faltante = int(-(-(cantidad - self.libre(sku)) // 1))
                if faltante > 0:
                    self.ajustar_stock(sku, faltante)
            resultado.append((sku, cantidad, precio))
        return resultado

    def ajustar_stock(self, sku, cantidad):
        self.api.post("/inventario/movimientos", {
            "tipoOperacion": "entrada", "productoId": self.producto(sku)["id"],
            "ubicacionId": self.ubicaciones["Almacén 1"], "cantidad": cantidad,
            "motivo": "Ajuste por carga inicial de inventario"})
        self.contar("ajustes de stock inicial")

    def lineas_api(self, lineas):
        return [{"productoId": self.producto(s)["id"], "cantidad": c, "precioUnitario": p} for s, c, p in lineas]

    # --- maestros ---------------------------------------------------------------------------------------------
    def maestros(self):
        print("Maestros")
        for almacen in self.api.get("/almacenes"):
            self.ubicaciones[almacen["nombre"]] = almacen["id"]
            if almacen.get("tipoUbicacion") == "tienda":
                self.ubicaciones.setdefault("Tienda", almacen["id"])
        existentes = {c.get("numeroDocumento"): c for c in self.api.paginas("/clientes")}
        for ruc, nombre, telefono, direccion in CLIENTES:
            cliente = existentes.get(ruc)
            if not cliente:
                cliente = self.api.post("/clientes", {"tipoDocumento": "ruc", "numeroDocumento": ruc,
                                                      "nombreRazonSocial": nombre, "telefonoContacto": telefono,
                                                      "direccionReferencia": direccion})
                self.contar("clientes")
            self.clientes[ruc] = cliente["id"]
        mostrador = next((c for c in self.api.paginas("/clientes") if c["nombreRazonSocial"] == CLIENTE_MOSTRADOR), None)
        if not mostrador:
            mostrador = self.api.post("/clientes", {"nombreRazonSocial": CLIENTE_MOSTRADOR,
                                                    "direccionReferencia": "Retiro en tienda"})
            self.contar("clientes")
        self.clientes["mostrador"] = mostrador["id"]
        existentes_prov = {p["ruc"]: p for p in self.api.paginas("/proveedores")}
        for ruc, (razon, contacto, condiciones, catalogo) in PROVEEDORES.items():
            proveedor = existentes_prov.get(ruc)
            if not proveedor:
                proveedor = self.api.post("/proveedores", {"ruc": ruc, "razonSocial": razon, "contacto": contacto,
                                                           "condiciones": condiciones})
                self.contar("proveedores")
            self.proveedores[ruc] = proveedor["id"]
            productos = [{"sku": s, "precioCompra": p, "plazoEntregaReferencialDias": d} for s, p, d in catalogo
                         if self.producto(s)]
            self.api.put(f"/proveedores/{proveedor['id']}/catalogo", {"productos": productos})

    # --- cotizaciones, ventas y despachos --------------------------------------------------------------------
    def cotizaciones_y_ventas(self):
        print("Cotizaciones, ventas y despachos")
        primero = self.clientes[CLIENTES[0][0]]
        if not self.forzar and self.api.get("/cotizaciones", clienteId=primero, pageSize=1)["total"] > 0:
            print("  ya existen cotizaciones de demostración; se omite (usar --forzar para repetir).")
            return False
        orden = sorted(COTIZACIONES, key=lambda c: c[3] not in ("despachada", "guia_emitida"))
        for indice, nota, lineas, destino, descripcion in orden:
            if destino == "validada_venta":
                self.ventas_directas()
            limitar = destino in ("despachada", "guia_emitida")
            validas = self.lineas_validas(lineas, limitar=limitar)
            if not validas:
                self.aviso(f"Nota {nota}: sin líneas válidas; se omite.")
                continue
            cuerpo = {"clienteId": self.clientes[CLIENTES[indice][0]], "lineas": self.lineas_api(validas),
                      "descuento": 0, "validezDias": 30,
                      "condicionesComerciales": f"Según {natural(descripcion)[0].lower() + natural(descripcion)[1:]}. Pago a 7 días, entrega en obra."}
            cotizacion = self.api.post("/cotizaciones", cuerpo)
            self.contar("cotizaciones")
            cid = cotizacion["id"]
            self.cotizaciones[nota] = cid
            if destino == "rechazada":
                self.api.post(f"/cotizaciones/{cid}/rechazo", {"origen": "cliente",
                                                               "motivo": "El cliente pidió un plazo de entrega menor."})
            elif destino == "dividida":
                dividir = validas[-1]
                self.api.post(f"/cotizaciones/{cid}/division",
                              {"lineas": [{"productoId": self.producto(dividir[0])["id"],
                                           "cantidad": max(1, dividir[1] // 2)}]})
                self.contar("cotizaciones")
            elif destino != "emitida":
                self.api.post(f"/cotizaciones/{cid}/aceptacion")
                if destino != "aceptada":
                    self.api.post(f"/cotizaciones/{cid}/validacion")
                if destino in ("validada_venta", "despachada", "guia_emitida"):
                    dias = 3 if destino == "guia_emitida" else 9
                    entrega = (date.today() + timedelta(days=dias)).isoformat()
                    venta = self.api.post("/ventas", {"idCotizacion": cid, "fechaEntregaRequerida": entrega})
                    self.contar("ventas")
                    self.despachar(venta["id"], destino)
        return True

    def despachar(self, venta_id, destino):
        if destino == "validada_venta":
            return
        guia = self.api.post("/guias-despacho", {"idVenta": venta_id})
        self.contar("guias")
        if destino == "guia_emitida":
            return
        self.api.post(f"/guias-despacho/{guia['id']}/salida")
        self.contar("salidas confirmadas")
        self.api.post(f"/guias-despacho/{guia['id']}/entrega")

    def ventas_directas(self):
        for lineas, destino in VENTAS_DIRECTAS:
            validas = self.lineas_validas(lineas, limitar=True)
            if not validas:
                continue
            venta = self.api.post("/ventas/directas", {"clienteId": self.clientes["mostrador"], "descuento": 0,
                                                       "lineas": self.lineas_api(validas)})
            self.contar("ventas")
            if destino == "despachada":
                self.despachar(venta["id"], "despachada")
            elif destino == "guia_anulada":
                self.api.post("/guias-despacho", {"idVenta": venta["id"]})
                self.contar("guias")
                self.api.post(f"/ventas/{venta['id']}/cancelacion",
                              {"motivo": "El cliente canceló el pedido antes del despacho."})
                self.contar("ventas canceladas (guía anulada)")

    # --- compras y recepciones --------------------------------------------------------------------------------
    def compras_y_recepciones(self):
        print("Órdenes de compra y recepciones")
        reyfam = self.proveedores["20600200011"]
        if not self.forzar and self.api.get("/ordenes-compra", proveedorId=reyfam, pageSize=1)["total"] > 0:
            print("  ya existen órdenes de compra de demostración; se omite (usar --forzar para repetir).")
            return
        for ruc, lineas, destino, descripcion in ORDENES:
            validas = [(s, c, p) for s, c, p in lineas if self.producto(s)]
            orden = self.api.post("/ordenes-compra", {
                "proveedorId": self.proveedores[ruc],
                "lineas": [{"sku": s, "cantidadComprada": c, "costoUnitario": p} for s, c, p in validas]})
            self.contar("órdenes de compra")
            if destino == "pendiente":
                continue
            if destino == "completada":
                recibidas = [(s, c) for s, c, _ in validas]
            else:
                recibidas = [(validas[0][0], validas[0][1]), (validas[1][0], max(1, validas[1][1] // 2))]
            recepcion = self.api.post("/recepciones", {
                "ordenCompraId": orden["id"], "fechaRecepcion": date.today().isoformat(),
                "observaciones": f"Recepción conforme de {natural(descripcion)[0].lower() + natural(descripcion)[1:]}",
                "lineas": [{"productoId": self.producto(s)["id"], "ubicacionId": self.ubicaciones["Almacén 1"],
                            "cantidadRecibida": c} for s, c in recibidas]})
            self.api.post(f"/recepciones/{recepcion['id']}/validacion")
            self.recepciones[ruc] = recepcion["id"]
            self.contar("recepciones validadas")
        self.orden_por_alertas()

    def orden_por_alertas(self):
        alertas = [a["sku"] for a in self.api.get("/inventario/alertas", pageSize=100)["items"]
                   if a["nivelAlerta"] == "critico"]
        mejor, coincidencias = None, []
        for proveedor in self.api.paginas("/proveedores"):
            if proveedor["ruc"] in PROVEEDORES:
                continue
            catalogo = {i["sku"]: i for i in self.api.get(f"/proveedores/{proveedor['id']}/catalogo")}
            comunes = [catalogo[s] for s in alertas if s in catalogo]
            if len(comunes) > len(coincidencias):
                mejor, coincidencias = proveedor, comunes
        if not mejor:
            self.aviso("No hay proveedor con productos en alerta crítica en su catálogo; no se crea la orden de reposición.")
            return
        lineas = [{"sku": i["sku"], "cantidadComprada": 200, "costoUnitario": max(float(i["precioCompra"]), 0.5)}
                  for i in coincidencias[:10]]
        self.api.post("/ordenes-compra", {"proveedorId": mejor["id"], "lineas": lineas})
        self.contar("órdenes de compra (reposición por alertas)")

    # --- Ventas pendientes de abastecimiento (para planificar compras) ---------------------------------------
    PENDIENTES = [
        # RUC del cliente, días hasta la entrega, [(SKU, unidades por encima del stock libre)]
        ("20600100099", 5, [("00762", 80), ("00743", 25), ("00160", 600)]),
        ("20600100088", 14, [("00762", 40), ("00064", 120), ("00015", 30)]),
        ("20600100055", 25, [("00160", 1500), ("00079", 40)]),
    ]

    def ventas_pendientes_de_abastecimiento(self):
        print("Ventas pendientes de abastecimiento")
        for ruc, dias, lineas in self.PENDIENTES:
            cliente = self.clientes[ruc]
            if not self.forzar and self.api.get("/cotizaciones", clienteId=cliente, pageSize=1)["total"] > 0:
                print("  ya existen ventas de este cliente; se omite (usar --forzar para repetir).")
                continue
            pedido = []
            for sku, exceso in lineas:
                producto = self.producto(sku)
                if not producto:
                    self.aviso(f"SKU {sku} no está en el catálogo; se omite.")
                    continue
                precio = float(producto.get("precioReferencial") or 1)
                pedido.append((sku, int(self.libre(sku)) + exceso, precio))
            if not pedido:
                continue
            cotizacion = self.api.post("/cotizaciones", {
                "clienteId": cliente, "lineas": self.lineas_api(pedido), "descuento": 0, "validezDias": 30,
                "condicionesComerciales": "Pago a 7 días, entrega en obra según avance."})
            self.contar("cotizaciones")
            self.api.post(f"/cotizaciones/{cotizacion['id']}/aceptacion")
            self.api.post(f"/cotizaciones/{cotizacion['id']}/validacion")
            entrega = (date.today() + timedelta(days=dias)).isoformat()
            self.api.post("/ventas", {"idCotizacion": cotizacion["id"], "fechaEntregaRequerida": entrega})
            self.contar("ventas pendientes de abastecimiento")

    # --- Kardex manual ---------------------------------------------------------------------------------------
    def movimientos(self):
        print("Movimientos manuales de Kardex")
        for tipo, sku, donde, cantidad, motivo in MOVIMIENTOS:
            producto = self.producto(sku)
            if not producto:
                self.aviso(f"SKU {sku} no está en el catálogo; se omite el movimiento.")
                continue
            try:
                if tipo == "traslado":
                    self.api.post("/inventario/traslados", {
                        "productoId": producto["id"], "ubicacionOrigenId": self.ubicaciones[donde[0]],
                        "ubicacionDestinoId": self.ubicaciones[donde[1]], "cantidad": cantidad, "motivo": motivo})
                else:
                    self.api.post("/inventario/movimientos", {
                        "tipoOperacion": tipo, "productoId": producto["id"], "ubicacionId": self.ubicaciones[donde],
                        "cantidad": cantidad, "motivo": motivo})
                self.contar("movimientos manuales")
            except ErrorApi as error:
                self.aviso(f"Movimiento {tipo} de {sku} omitido: {error}")

    # --- documentos OCR --------------------------------------------------------------------------------------
    def documentos(self):
        print("Documentos OCR")
        existentes = {d["nombreArchivo"] for d in self.api.paginas("/documentos")}
        for nombre, tipo, destino in DOCUMENTOS_OCR:
            archivo = self.imagenes / nombre
            if not archivo.exists():
                self.aviso(f"{nombre}: no se encontró en {self.imagenes}; se omite.")
                continue
            if nombre in existentes and not self.forzar:
                print(f"  {nombre}: ya cargado.")
                continue
            try:
                documento = self.api.subir("/documentos", {"tipoDocumento": tipo}, archivo)
                documento = self.esperar(documento["id"])
                self.contar("documentos OCR")
                if documento.get("codigoError"):
                    self.aviso(f"{nombre}: el OCR devolvió {documento['codigoError']}; queda para reprocesar.")
                    continue
                if destino:
                    self.validar_y_asociar(documento, destino)
            except ErrorApi as error:
                self.aviso(f"{nombre}: {error}")

    def esperar(self, id_documento):
        for _ in range(60):
            documento = self.api.get(f"/documentos/{id_documento}")
            if documento.get("codigoError") or documento["estadoProcesamiento"] != "pendiente":
                return documento
            time.sleep(2)
        return documento

    def validar_y_asociar(self, documento, destino):
        id_documento = documento["id"]
        vigentes = [d for d in documento["detalles"] if not d["descartado"]]
        if not any(d["idProducto"] or d["coincidencias"] for d in vigentes):
            self.aviso(f"{documento['nombreArchivo']}: el OCR no encontró coincidencias; queda para revisión manual.")
            return
        for detalle in vigentes:
            if detalle["idProducto"]:
                continue
            if detalle["coincidencias"]:
                self.api.put(f"/documentos/{id_documento}/detalles/{detalle['id']}",
                             {"idProducto": detalle["coincidencias"][0]["idProducto"]})
            else:
                self.api.post(f"/documentos/{id_documento}/detalles/{detalle['id']}/descarte")
        self.api.post(f"/documentos/{id_documento}/validacion")
        clase, _, referencia = destino.partition(":")
        if clase == "cotizacion" and referencia in self.cotizaciones:
            cuerpo = {"idCotizacion": self.cotizaciones[referencia], "idRecepcion": None}
        elif clase == "recepcion" and referencia in self.recepciones:
            cuerpo = {"idCotizacion": None, "idRecepcion": self.recepciones[referencia]}
        else:
            self.aviso(f"{documento['nombreArchivo']}: validado, sin operación a la cual asociarlo.")
            return
        self.api.post(f"/documentos/{id_documento}/asociacion", cuerpo)
        self.contar("documentos validados y asociados")

    def resumen(self):
        print("\nResumen de lo sembrado")
        if not self.cuenta:
            print("  nada nuevo: los datos de demostración ya existían.")
        for clave, valor in self.cuenta.items():
            print(f"  {clave}: {valor}")


def mostrar_plan():
    print("Plan de siembra (modo --dry-run, no se contacta la API):")
    print(f"  Clientes: {len(CLIENTES)} empresas + 1 de mostrador")
    print(f"  Proveedores: {len(PROVEEDORES)} con su catálogo de compra")
    for _, nota, lineas, destino, descripcion in COTIZACIONES:
        print(f"  Cotización {descripcion}: {len(fundir(lineas))} líneas -> {destino}")
    print(f"  Ventas directas de mostrador: {len(VENTAS_DIRECTAS)}")
    for _, lineas, destino, descripcion in ORDENES:
        print(f"  Orden de compra {descripcion}: {len(lineas)} líneas -> {destino}")
    print("  Orden de reposición con productos en alerta crítica (proveedor del catálogo base)")
    print(f"  Movimientos manuales de Kardex: {len(MOVIMIENTOS)}")
    print(f"  Documentos OCR con --con-ocr: {len(DOCUMENTOS_OCR)} imágenes")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", default="http://localhost:8080", help="Base del backend (sin /api/v1).")
    parser.add_argument("--dry-run", action="store_true", help="Muestra el plan sin llamar a la API.")
    parser.add_argument("--forzar", action="store_true", help="Repite las fases aunque ya existan datos sembrados.")
    parser.add_argument("--solo-pendientes", action="store_true",
                        help="Siembra solo las ventas pendientes de abastecimiento (con maestros ya cargados).")
    parser.add_argument("--con-ocr", action="store_true", help="Sube imágenes al OCR (gasta cuota de Gemini).")
    parser.add_argument("--imagenes", default=str(RAIZ / "Documentos-Escanear"), help="Carpeta de imágenes escaneadas.")
    argumentos = parser.parse_args()
    if argumentos.dry_run:
        mostrar_plan()
        return
    usuario, clave = leer_credenciales()
    api = Api(argumentos.url)
    try:
        sesion = api.post("/sesiones", {"username": usuario, "clave": clave})
    except ErrorApi as error:
        sys.exit(f"No se pudo iniciar sesión ({error.estado}). Revisar MIGAL_DEMO_USERNAME y MIGAL_DEMO_CLAVE.")
    api.token = sesion["token"]
    sembrador = Sembrador(api, argumentos.forzar, Path(argumentos.imagenes))
    try:
        sembrador.maestros()
        if argumentos.solo_pendientes:
            sembrador.ventas_pendientes_de_abastecimiento()
            sembrador.resumen()
            return
        if sembrador.cotizaciones_y_ventas():
            sembrador.movimientos()
        sembrador.compras_y_recepciones()
        sembrador.ventas_pendientes_de_abastecimiento()
        if argumentos.con_ocr:
            sembrador.documentos()
    except ErrorApi as error:
        sembrador.resumen()
        sys.exit(f"Error de la API: {error}")
    sembrador.resumen()


if __name__ == "__main__":
    main()
