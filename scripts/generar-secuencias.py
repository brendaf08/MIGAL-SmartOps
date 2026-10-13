#!/usr/bin/env python3
"""Genera PNG desde fuentes SequenceDiagram.org, conservando su nombre."""
from pathlib import Path
import base64
import os
import re
import sys
import tempfile

from selenium import webdriver
from selenium.webdriver.support.ui import WebDriverWait

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "docs/fuentes/secuencia"
DESTINATION = ROOT / "docs/imagenes"
PATTERN = re.compile(r"MIGAL-SEQ-CU-\d{2}-[a-z0-9]+(?:-[a-z0-9]+)*\.txt")


def main():
    files = sorted(SOURCES.glob("*.txt"))
    if not files:
        raise ValueError(f"No hay archivos .txt en {SOURCES}")
    for path in files:
        if not PATTERN.fullmatch(path.name):
            raise ValueError(f"Nombre fuera del estándar: {path.name}")
        if not path.read_text(encoding="utf-8-sig").strip():
            raise ValueError(f"El archivo está vacío: {path.name}")

    options = webdriver.ChromeOptions()
    options.add_argument("--headless=new")
    options.add_argument("--window-size=1920,1080")
    # Perfil temporal independiente del Chrome personal del usuario.
    with tempfile.TemporaryDirectory(prefix="migal-chrome-") as profile:
        options.add_argument(f"--user-data-dir={profile}")
        with webdriver.Chrome(options=options) as browser:
            browser.set_page_load_timeout(90)
            browser.set_script_timeout(60)
            browser.get("https://sequencediagram.org/")
            WebDriverWait(browser, 60).until(lambda driver: driver.execute_script(
                "return typeof SEQ !== 'undefined' && !!SEQ.api && "
                "typeof SEQ.api.generatePngDataUrl === 'function';"
            ))
            images = []
            for path in files:
                source = path.read_text(encoding="utf-8-sig")
                result = browser.execute_async_script(
                    "const source = arguments[0];"
                    "const done = arguments[arguments.length - 1];"
                    "try { SEQ.api.generatePngDataUrl(source, "
                    "url => done({url: url})); } "
                    "catch (error) { done({error: String(error)}); }",
                    source,
                )
                if not isinstance(result, dict) or result.get("error"):
                    raise ValueError(f"No se pudo generar {path.name}: {result}")
                url = result.get("url", "")
                if not url.startswith("data:image/png;base64,"):
                    raise ValueError(f"Respuesta PNG inválida para {path.name}")
                data = base64.b64decode(url.split(",", 1)[1], validate=True)
                if not data.startswith(b"\x89PNG\r\n\x1a\n"):
                    raise ValueError(f"Archivo PNG inválido para {path.name}")
                images.append((DESTINATION / (path.stem + ".png"), data))

    # No sustituir imágenes hasta que todos los diagramas hayan sido generados.
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for destination, data in images:
        descriptor, temp_name = tempfile.mkstemp(dir=DESTINATION, suffix=".tmp")
        try:
            with os.fdopen(descriptor, "wb") as stream:
                stream.write(data)
            os.replace(temp_name, destination)
        finally:
            if os.path.exists(temp_name):
                os.unlink(temp_name)
        print(f"Generado: {destination.relative_to(ROOT)}")
    print(f"Listo: {len(images)} diagramas. Revisa visualmente los PNG antes del pull request.")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"No se completó la generación: {error}", file=sys.stderr)
        print("Comprueba la conexión, Google Chrome y la sintaxis de las fuentes.", file=sys.stderr)
        sys.exit(1)
