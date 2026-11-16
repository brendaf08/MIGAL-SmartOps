"""Normalización del texto ferretero (ADR-011, capa determinista).

Convierte una descripción leída (``"Cem. Sol 42,5 kgs x 2 bls"``, ``"F° 1/2\""``) a una forma comparable: minúsculas,
sin tildes, con la jerga expandida y con las medidas críticas en forma canónica. Las medidas se devuelven aparte porque
la regla de consistencia técnica del matcher las compara por tipo: ``1/2in`` frente a ``3/8in`` no es la misma pieza.
"""

import re
import unicodedata
from dataclasses import dataclass

# Jerga y abreviaturas frecuentes en notas de mostrador y guías. Se amplía con el conjunto de evaluación.
JERGA: dict[str, str] = {
    "kk": "king kong",
    "f°": "fierro",
    "fo": "fierro",
    "fe": "fierro",
    "fierr": "fierro",
    "bls": "bolsa",
    "bols": "bolsa",
    "bl": "bolsa",
    "bol": "bolsa",
    "cem": "cemento",
    "cmto": "cemento",
    "cto": "cemento",
    "lad": "ladrillo",
    "ladr": "ladrillo",
    "tub": "tubo",
    "und": "unidad",
    "unid": "unidad",
    "uds": "unidad",
    "pza": "pieza",
    "pzas": "pieza",
    "galv": "galvanizado",
    "corrug": "corrugado",
    "cor": "corrugado",
    # En las notas de venta el diámetro (ϕ, Ø) antes de una medida designa la varilla de fierro: "Ø 5/8".
    "φ": "fierro",
    "ø": "fierro",
    "ag": "agua",
    "gdon": "galon",
    "glon": "galon",
    "gln": "galon",
    "octagonal": "octogonal",
}

# Palabras que no distinguen un producto de otro: conectores, unidades de venta y medidas de longitud o volumen que
# las notas escriben junto a la cantidad ("20 varillas", "mtrs arena gruesa", "Kg clavo"). Se descartan en ambos lados
# de la comparación, igual que los tokens de una sola letra ("tipo I", "N° 16").
PALABRAS_VACIAS: frozenset[str] = frozenset(
    {
        "de", "del", "la", "el", "los", "las", "para", "con", "y", "x", "tipo", "inc",
        "varilla", "bolsa", "unidad", "pieza", "millar", "ciento", "docena", "juego",
        "kg", "kilo", "m", "mt", "mts", "mtr", "mtrs", "metro", "m3", "cm", "galon", "litro",
    }
)  # fmt: skip

_FRACCION_PALABRAS = {"media": "1/2", "medio": "1/2", "un cuarto": "1/4", "tres octavos": "3/8", "un octavo": "1/8"}

_NUMERO = r"\d+(?:[.,]\d+)?"
_PULGADA = re.compile(rf'(?<![\d/])(\d+\s*/\s*\d+|{_NUMERO})\s*(?:"|\'\'|”|pulgadas?|pulg\.?|plg\.?)')
_FRACCION_SUELTA = re.compile(r"(?<![\d/])(\d+)\s*/\s*(\d+)(?![\d/a-z])")
_KILOS = re.compile(rf"(?<![\d.,])({_NUMERO})\s*(?:kgs?|kilos?|kilogramos?)\b")
_HUECOS = re.compile(r"(?<!\d)(\d+)\s*huecos?\b")
_AWG = re.compile(r"(?<!\d)(?:n[°o]\s*)?(\d+)\s*awg\b|\bawg\s*(\d+)\b")
_MILIMETROS = re.compile(rf"(?<![\d.,])({_NUMERO})\s*mm\b")
_LARGO = re.compile(rf"\bx\s*({_NUMERO})\s*m\b")
_FORMATO = re.compile(r"(?<![\d/])(\d+)\s*x\s*(\d+)(?:\s*cm\b)?")
# Cantidad al inicio de la línea ("20 varillas F° 1/2\"", "10 bls cem sol"), salvo que sea la medida del producto.
_CANTIDAD_INICIAL = re.compile(rf"^\s*{_NUMERO}\s+(?!(?:kgs?|kilos?|kilogramos?|mm|huecos?|awg|pulg\w*|plg)\b|[\"'”/])")
_SEPARADORES = re.compile(r"[^\w/.\s]")
_ESPACIOS = re.compile(r"\s+")


@dataclass(frozen=True)
class TextoNormalizado:
    """Texto comparable y medidas críticas detectadas, por ejemplo ``{"1/2in", "42.5kg"}``."""

    texto: str
    medidas: frozenset[str]


def _sin_tildes(texto: str) -> str:
    descompuesto = unicodedata.normalize("NFKD", texto)
    return "".join(c for c in descompuesto if not unicodedata.combining(c))


def _numero(valor: str) -> str:
    """Número con punto decimal y sin ceros sobrantes: ``42,50`` → ``42.5``."""
    numero = valor.replace(",", ".")
    if "." in numero:
        numero = numero.rstrip("0").rstrip(".")
    return numero


def _fraccion(valor: str) -> str:
    return re.sub(r"\s+", "", valor)


def singular(palabra: str) -> str:
    """Singular aproximado del español para comparar ``codos`` con ``codo`` y ``uniones`` con ``union``."""
    if len(palabra) <= 3 or not palabra.isalpha():
        return palabra
    if palabra.endswith("es") and palabra[-3] in "rnlzd":
        return palabra[:-2]
    if palabra.endswith("s") and palabra[-2] in "aeiou":
        return palabra[:-1]
    return palabra


def normalizar(texto: str) -> TextoNormalizado:
    """Normaliza una descripción y separa sus medidas críticas."""
    actual = _sin_tildes(texto.lower()).replace("º", "°")
    actual = _CANTIDAD_INICIAL.sub("", actual, count=1)
    for palabras, fraccion in _FRACCION_PALABRAS.items():
        actual = re.sub(rf"\b{palabras}\s+pulgadas?\b", f'{fraccion}"', actual)

    medidas: set[str] = set()

    def registrar(medida: str) -> str:
        medidas.add(medida)
        return f" {medida} "

    def pulgadas(valor: str) -> str:
        return registrar(f"{_fraccion(valor) if '/' in valor else _numero(valor)}in")

    def largo(valor: str) -> str:
        # El largo de la pieza ("x 9 m") se conserva como medida, pero no se compara como texto: las notas no lo
        # escriben y restaría similitud a todas las líneas.
        medidas.add(f"{_numero(valor)}m")
        return " "

    actual = _LARGO.sub(lambda m: largo(m.group(1)), actual)
    actual = _FORMATO.sub(lambda m: f" {m.group(1)}x{m.group(2)} ", actual)
    actual = _PULGADA.sub(lambda m: pulgadas(m.group(1)), actual)
    actual = _KILOS.sub(lambda m: registrar(f"{_numero(m.group(1))}kg"), actual)
    actual = _HUECOS.sub(lambda m: registrar(f"{m.group(1)}huecos"), actual)
    actual = _AWG.sub(lambda m: registrar(f"{m.group(1) or m.group(2)}awg"), actual)
    actual = _MILIMETROS.sub(lambda m: registrar(f"{_numero(m.group(1))}mm"), actual)
    # Una fracción sin unidad en ferretería es una medida en pulgadas: "fierro 1/2".
    actual = _FRACCION_SUELTA.sub(lambda m: registrar(f"{m.group(1)}/{m.group(2)}in"), actual)

    actual = actual.replace("f°", " fierro ")
    palabras = []
    for palabra in _SEPARADORES.sub(" ", actual).split():
        palabra = palabra.strip(".")
        if not palabra:
            continue
        if palabra in medidas:
            palabras.append(palabra)
            continue
        palabra = JERGA.get(palabra) or JERGA.get(singular(palabra)) or singular(palabra)
        if not (len(palabra) == 1 and palabra.isalpha()) and palabra not in PALABRAS_VACIAS:
            palabras.append(palabra)
    return TextoNormalizado(_ESPACIOS.sub(" ", " ".join(palabras)).strip(), frozenset(medidas))


def tipo_de_medida(medida: str) -> str:
    """Tipo de una medida canónica: ``in``, ``kg``, ``huecos``, ``awg``, ``mm`` o ``m``."""
    return re.sub(r"^[\d./]+", "", medida)
