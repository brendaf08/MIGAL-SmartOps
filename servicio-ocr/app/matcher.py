"""Asignación de SKU por similitud difusa (ADR-011 y ADR-012, punto 6).

El modelo de visión no elige SKU: este módulo compara cada línea normalizada con el catálogo activo recibido del
backend, de modo que solo se sugieren productos que existen. El puntaje base combina ``token_sort_ratio`` y
``token_set_ratio``, porque ``token_set_ratio`` aislado da el máximo cuando un texto está contenido en el otro
(``cemento`` frente a ``cemento sol 42.5kg``). Una medida incompatible multiplica el puntaje por
``FACTOR_MEDIDA_INCOMPATIBLE``.
"""

from dataclasses import dataclass

from rapidfuzz import fuzz

from app.esquemas import Coincidencia, ProductoCatalogo
from app.normalizador import TextoNormalizado, normalizar, tipo_de_medida

PESO_ORDEN = 0.6
PESO_CONJUNTO = 0.4
FACTOR_MEDIDA_INCOMPATIBLE = 0.3
MAXIMO_COINCIDENCIAS = 3


@dataclass(frozen=True)
class Candidato:
    """Producto del catálogo con su texto normalizado, calculado una vez por solicitud."""

    sku: str
    normalizado: TextoNormalizado


def preparar_catalogo(catalogo: list[ProductoCatalogo]) -> list[Candidato]:
    """Normaliza el nombre, la marca y la presentación de cada producto."""
    candidatos = []
    for producto in catalogo:
        partes = [producto.nombreOficial, producto.marca or "", producto.presentacion or ""]
        candidatos.append(Candidato(producto.sku, normalizar(" ".join(p for p in partes if p))))
    return candidatos


def medidas_incompatibles(linea: frozenset[str], candidato: frozenset[str]) -> bool:
    """Hay incompatibilidad si ambos indican una medida del mismo tipo y ninguna coincide."""
    for tipo in {tipo_de_medida(m) for m in linea}:
        de_linea = {m for m in linea if tipo_de_medida(m) == tipo}
        del_candidato = {m for m in candidato if tipo_de_medida(m) == tipo}
        if del_candidato and not de_linea & del_candidato:
            return True
    return False


def puntaje(linea: TextoNormalizado, candidato: TextoNormalizado) -> float:
    """Confianza de 0 a 1 de que la línea corresponda al candidato."""
    if not linea.texto or not candidato.texto:
        return 0.0
    base = (
        PESO_ORDEN * fuzz.token_sort_ratio(linea.texto, candidato.texto)
        + PESO_CONJUNTO * fuzz.token_set_ratio(linea.texto, candidato.texto)
    ) / 100
    if medidas_incompatibles(linea.medidas, candidato.medidas):
        base *= FACTOR_MEDIDA_INCOMPATIBLE
    return round(min(max(base, 0.0), 1.0), 3)


def coincidencias(texto: str, candidatos: list[Candidato], umbral_baja: float) -> list[Coincidencia]:
    """Hasta tres coincidencias con confianza igual o mayor al umbral, de mayor a menor."""
    linea = normalizar(texto)
    puntuados = [(puntaje(linea, c.normalizado), c.sku) for c in candidatos]
    seleccionados = sorted((p for p in puntuados if p[0] >= umbral_baja), key=lambda p: (-p[0], p[1]))
    return [Coincidencia(sku=sku, confianza=valor) for valor, sku in seleccionados[:MAXIMO_COINCIDENCIAS]]
