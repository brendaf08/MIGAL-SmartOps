"""Asignación de SKU por similitud difusa (ADR-011 y ADR-012, punto 6).

El modelo de visión no elige SKU: este módulo compara cada línea normalizada con el catálogo activo recibido del
backend, de modo que solo se sugieren productos que existen. El puntaje base combina ``token_sort_ratio`` y
``token_set_ratio``, porque ``token_set_ratio`` aislado da el máximo cuando un texto está contenido en el otro
(``cemento`` frente a ``cemento sol 42.5kg``). Sobre el puntaje base se aplican dos reglas calibradas con notas de
venta reales (ADR-012, punto 6):

- Una medida o un material incompatibles (``1/2in`` frente a ``3/8in``, ``cpvc`` frente a ``pvc``) multiplican el
  puntaje por ``FACTOR_MEDIDA_INCOMPATIBLE``.
- Si el tipo de producto de la línea, su primera palabra, no aparece en el candidato, el puntaje se multiplica por
  ``FACTOR_TIPO_DISTINTO``.

Con el catálogo de demostración y 50 líneas de cuatro notas de venta manuscritas, 49 asignan el SKU correcto con
confianza de al menos 0.60, frente a 36 sin estas reglas ni la normalización de cantidades, unidades y plurales. Con
el catálogo real de 818 productos, 47 de esas líneas reciben sugerencia sobre 0.60.
"""

from collections import Counter
from dataclasses import dataclass

from rapidfuzz import fuzz, process
from rapidfuzz.distance import DamerauLevenshtein

from app.esquemas import Coincidencia, ProductoCatalogo
from app.normalizador import PALABRAS_VACIAS, TextoNormalizado, normalizar, tipo_de_medida

PESO_ORDEN = 0.6
PESO_CONJUNTO = 0.4
FACTOR_MEDIDA_INCOMPATIBLE = 0.3
FACTOR_TIPO_DISTINTO = 0.7
SIMILITUD_TIPO = 80
MATERIALES = frozenset({"pvc", "cpvc", "bronce", "galvanizado", "cobre", "acero"})
MAXIMO_COINCIDENCIAS = 3
# Errores de escritura: una palabra de 4 a 7 letras que no existe en el catálogo se corrige si hay otra a una letra de
# distancia (sustitución, falta, sobra o intercambio: "curbas" por "curva"); de 8 o más letras se admiten dos. Una
# línea corregida pierde un poco de confianza frente a una escrita bien, para que la persona la revise.
LONGITUD_MINIMA_CORRECCION = 4
LONGITUD_DOS_ERRORES = 8
FACTOR_PALABRA_CORREGIDA = 0.95
# Con catálogos grandes, solo los candidatos más parecidos por conjunto de palabras (calculado en C++ por RapidFuzz)
# pasan al puntaje completo, que se calcula en Python.
CANDIDATOS_PREFILTRO = 50


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


def vocabulario(candidatos: list[Candidato]) -> Counter[str]:
    """Palabras del catálogo normalizado, con cuántos productos las usan. Las medidas no son palabras."""
    palabras: Counter[str] = Counter()
    for candidato in candidatos:
        for palabra in set(candidato.normalizado.texto.split()):
            if (
                palabra not in candidato.normalizado.medidas
                and palabra.isalpha()
                and len(palabra) >= LONGITUD_MINIMA_CORRECCION
            ):
                palabras[palabra] += 1
    return palabras


def corregir_errores(linea: TextoNormalizado, palabras: Counter[str]) -> tuple[TextoNormalizado, bool]:
    """Corrige las palabras de la línea que no existen en el catálogo y están a una o dos letras de una que sí existe.

    Devuelve la línea corregida y si cambió algo. Se ignoran las medidas, los números y las palabras cortas, que serían
    corregidas por azar. Entre varias palabras igual de cercanas gana la más usada en el catálogo.
    """
    corregidas: list[str] = []
    cambio = False
    for palabra in linea.texto.split():
        corregida = palabra
        largo = len(palabra)
        if (
            largo >= LONGITUD_MINIMA_CORRECCION
            and palabra.isalpha()
            and palabra not in palabras
            and palabra not in PALABRAS_VACIAS
            and palabra not in linea.medidas
        ):
            maxima = 2 if largo >= LONGITUD_DOS_ERRORES else 1
            mejor: tuple[int, int, str] | None = None
            for existente, usos in palabras.items():
                if abs(len(existente) - largo) > maxima:
                    continue
                distancia = DamerauLevenshtein.distance(palabra, existente, score_cutoff=maxima)
                if distancia > maxima:
                    continue
                clave = (distancia, -usos, existente)
                if mejor is None or clave < mejor:
                    mejor = clave
            if mejor is not None:
                corregida = mejor[2]
                cambio = True
        corregidas.append(corregida)
    if not cambio:
        return linea, False
    return TextoNormalizado(" ".join(corregidas), linea.medidas), True


def medidas_incompatibles(linea: frozenset[str], candidato: frozenset[str]) -> bool:
    """Hay incompatibilidad si ambos indican una medida del mismo tipo y ninguna coincide."""
    for tipo in {tipo_de_medida(m) for m in linea}:
        de_linea = {m for m in linea if tipo_de_medida(m) == tipo}
        del_candidato = {m for m in candidato if tipo_de_medida(m) == tipo}
        if del_candidato and not de_linea & del_candidato:
            return True
    return False


def materiales_incompatibles(linea: TextoNormalizado, candidato: TextoNormalizado) -> bool:
    """La línea nombra un material y el candidato nombra otro distinto: ``cpvc`` (agua caliente) no es ``pvc``,
    aunque por caracteres se parezcan."""
    de_linea = MATERIALES & set(linea.texto.split())
    del_candidato = MATERIALES & set(candidato.texto.split())
    return bool(de_linea and del_candidato and not de_linea & del_candidato)


def comparte_tipo_de_producto(linea: TextoNormalizado, candidato: TextoNormalizado) -> bool:
    """La primera palabra de la línea, que en ferretería nombra el tipo de producto (``tubo``, ``codo``, ``fierro``),
    aparece en el candidato, con tolerancia a errores de escritura (``mezcladura`` frente a ``mezcladora``).

    Sin esta regla, ``tubo cpvc 1/2`` se parece más a ``union cpvc 1/2`` que a ``tubo cpvc agua caliente 1/2``,
    porque la similitud por caracteres pesa igual el tipo que los atributos.
    """
    primera = next((p for p in linea.texto.split() if p not in linea.medidas), None)
    if primera is None:
        return True
    return any(fuzz.ratio(primera, palabra) >= SIMILITUD_TIPO for palabra in candidato.texto.split())


def puntaje(linea: TextoNormalizado, candidato: TextoNormalizado) -> float:
    """Confianza de 0 a 1 de que la línea corresponda al candidato."""
    if not linea.texto or not candidato.texto:
        return 0.0
    base = (
        PESO_ORDEN * fuzz.token_sort_ratio(linea.texto, candidato.texto)
        + PESO_CONJUNTO * fuzz.token_set_ratio(linea.texto, candidato.texto)
    ) / 100
    if medidas_incompatibles(linea.medidas, candidato.medidas) or materiales_incompatibles(linea, candidato):
        base *= FACTOR_MEDIDA_INCOMPATIBLE
    if not comparte_tipo_de_producto(linea, candidato):
        base *= FACTOR_TIPO_DISTINTO
    return round(min(max(base, 0.0), 1.0), 3)


def coincidencias(texto: str, candidatos: list[Candidato], umbral_baja: float) -> list[Coincidencia]:
    """Hasta tres coincidencias con confianza igual o mayor al umbral, de mayor a menor."""
    linea, corregida = corregir_errores(normalizar(texto), vocabulario(candidatos))
    if len(candidatos) > CANDIDATOS_PREFILTRO and linea.texto:
        elegidos = process.extract(
            linea.texto,
            [c.normalizado.texto for c in candidatos],
            scorer=fuzz.token_set_ratio,
            limit=CANDIDATOS_PREFILTRO,
        )
        candidatos = [candidatos[indice] for _, _, indice in elegidos]
    factor = FACTOR_PALABRA_CORREGIDA if corregida else 1.0
    puntuados = [(round(puntaje(linea, c.normalizado) * factor, 3), c.sku) for c in candidatos]
    seleccionados = sorted((p for p in puntuados if p[0] >= umbral_baja), key=lambda p: (-p[0], p[1]))
    return [Coincidencia(sku=sku, confianza=valor) for valor, sku in seleccionados[:MAXIMO_COINCIDENCIAS]]
