# Imagen del servicio de OCR (RNF-03, RNF-04). La extracción visual ocurre en la API de Gemini, por lo que la imagen
# no necesita GPU ni modelos locales.
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1
WORKDIR /app
COPY pyproject.toml ./
# Las dependencias, con versiones fijas en pyproject.toml, forman su propia capa: un cambio en app/ no las reinstala.
RUN python -c "import tomllib; print('\n'.join(tomllib.load(open('pyproject.toml', 'rb'))['project']['dependencies']))" \
        > /tmp/requisitos.txt \
    && pip install -r /tmp/requisitos.txt
COPY app app
RUN pip install --no-deps . && useradd --system --no-create-home migal
USER migal
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
