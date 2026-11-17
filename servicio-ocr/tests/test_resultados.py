import httpx
import respx

from app.esquemas import ResultadoDocumento
from app.resultados import registrar

URL = "http://backend.local/api/v1/documentos/812/resultados"


def enviar() -> bool:
    return registrar("http://backend.local/", "812", "token-812", ResultadoDocumento.ilegible(), 1.0, espera_base=0)


@respx.mock
def test_reintenta_ante_errores_del_servidor() -> None:
    ruta = respx.put(URL).mock(side_effect=[httpx.Response(503), httpx.ConnectError("caido"), httpx.Response(200)])

    assert enviar()
    assert ruta.call_count == 3


@respx.mock
def test_un_documento_que_ya_no_esta_pendiente_no_se_reintenta() -> None:
    ruta = respx.put(URL).mock(return_value=httpx.Response(409))

    assert not enviar()
    assert ruta.call_count == 1


@respx.mock
def test_un_rechazo_del_backend_no_se_reintenta() -> None:
    ruta = respx.put(URL).mock(return_value=httpx.Response(403))

    assert not enviar()
    assert ruta.call_count == 1


@respx.mock
def test_se_rinde_despues_de_los_reintentos() -> None:
    ruta = respx.put(URL).mock(return_value=httpx.Response(502))

    assert not enviar()
    assert ruta.call_count == 3
