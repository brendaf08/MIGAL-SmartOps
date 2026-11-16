"""Evalúa el pipeline sobre el conjunto de evaluación (ADR-012, punto 8; plan, paso 2.7).

Cada caso de ``tests/datos/evaluacion/`` es un archivo de imagen o PDF acompañado de un JSON con el mismo nombre y las
líneas esperadas. El catálogo se lee de ``catalogo.json`` en la misma carpeta. El script llama a la API de Gemini, por
lo que exige ``GEMINI_API_KEY``; no forma parte de las pruebas automatizadas.

Uso: ``python evaluar.py``
"""

import json
import sys
import time
from pathlib import Path

from app.config import configuracion
from app.esquemas import ProductoCatalogo
from app.extractor import ModeloGemini, cantidad_valida
from app.matcher import coincidencias, preparar_catalogo

CARPETA = Path(__file__).parent / "tests" / "datos" / "evaluacion"
TIPOS = {".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".pdf": "application/pdf"}
UMBRAL_BAJA = 0.60


def main() -> int:
    casos = sorted(p for p in CARPETA.glob("*") if p.suffix.lower() in TIPOS and p.with_suffix(".json").exists())
    if not casos or not (CARPETA / "catalogo.json").exists():
        print(f"No hay casos de evaluación en {CARPETA}. Ver tests/datos/evaluacion/README.md.")
        return 1
    ajustes = configuracion()
    if not ajustes.gemini_api_key:
        print("Falta la variable de entorno GEMINI_API_KEY.")
        return 1
    catalogo = [ProductoCatalogo(**p) for p in json.loads((CARPETA / "catalogo.json").read_text(encoding="utf-8"))]
    candidatos = preparar_catalogo(catalogo)
    modelo = ModeloGemini(ajustes.gemini_api_key, ajustes.migal_ocr_gemini_modelo, ajustes.tiempo_modelo_segundos)

    lineas = cantidades = top1 = top3 = 0
    tiempos: list[float] = []
    for caso in casos:
        esperado = json.loads(caso.with_suffix(".json").read_text(encoding="utf-8"))["lineas"]
        inicio = time.perf_counter()
        extraccion = modelo.extraer(caso.read_bytes(), TIPOS[caso.suffix.lower()])
        leidas = [(linea, coincidencias(linea.textoCrudo, candidatos, UMBRAL_BAJA)) for linea in extraccion.lineas]
        tiempos.append(time.perf_counter() - inicio)
        for indice, linea_esperada in enumerate(esperado):
            lineas += 1
            if indice >= len(leidas):
                continue
            leida, sugeridas = leidas[indice]
            if cantidad_valida(leida.cantidadLeida) == cantidad_valida(linea_esperada.get("cantidad")):
                cantidades += 1
            skus = [c.sku for c in sugeridas]
            top1 += bool(skus) and skus[0] == linea_esperada["sku"]
            top3 += linea_esperada["sku"] in skus

    tiempos.sort()
    p95 = tiempos[min(len(tiempos) - 1, int(len(tiempos) * 0.95))]
    print(f"Documentos: {len(casos)}  Líneas esperadas: {lineas}")
    print(f"SKU correcto entre las tres primeras: {top3 / lineas:.1%}  (meta >= 85 %)")
    print(f"SKU correcto en la primera posición: {top1 / lineas:.1%}  (meta >= 70 %)")
    print(f"Exactitud de cantidades: {cantidades / lineas:.1%}  (meta >= 90 %)")
    print(f"Tiempo por documento, percentil 95: {p95:.1f} s  (meta <= 10 s, RNF-07)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
