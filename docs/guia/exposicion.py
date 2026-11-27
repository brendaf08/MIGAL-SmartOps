"""Guía de exposición del proyecto (arquitectura, tecnologías, código, demostración y preguntas del profesor).
Uso: python docs/guia/exposicion.py   →   docs/guia/guia-de-exposicion.html
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generador import BASE, consejo, construir, flujo, lista, ojo, p, pasos, seccion, tabla  # noqa: E402

S = []


def sec(id_, titulo, *c):
    S.append(seccion(id_, titulo, *c))


def sub(id_, titulo, *c):
    return seccion(id_, titulo, *c, nivel=3)


def qa(pregunta, respuesta):
    return f'<details><summary>{pregunta}</summary>{respuesta}</details>'


def caja(rotulo, titulo, x, y, w, h, color):
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{color}" stroke="#0b3a9a" stroke-width="2"/>'
            f'<text x="{x + w / 2}" y="{y + 24}" text-anchor="middle" font-weight="700" font-size="15" fill="#10162a">{titulo}</text>'
            + ''.join(f'<text x="{x + w / 2}" y="{y + 46 + i * 18}" text-anchor="middle" font-size="12.5" fill="#26324f">{t}</text>'
                      for i, t in enumerate(rotulo)))


def flecha(x1, y1, x2, y2, texto='', dx=0, dy=-6, doble=False):
    s = (f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#e8590c" stroke-width="2.6" marker-end="url(#fa)"'
         + (' marker-start="url(#fb)"' if doble else '') + '/>')
    if texto:
        s += (f'<text x="{(x1 + x2) / 2 + dx}" y="{(y1 + y2) / 2 + dy}" text-anchor="middle" font-size="12" font-weight="600" '
              f'fill="#c2410c" paint-order="stroke" stroke="#fff" stroke-width="4">{texto}</text>')
    return s


def svg_arquitectura():
    return ('<div class="fig" style="padding:10px"><svg viewBox="0 0 980 470" xmlns="http://www.w3.org/2000/svg" role="img" '
            'aria-label="Arquitectura de MIGAL SmartOps" style="width:100%;height:auto;background:#fff;border-radius:10px">'
            '<defs><marker id="fa" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#e8590c"/></marker>'
            '<marker id="fb" markerWidth="10" markerHeight="10" refX="2" refY="5" orient="auto"><path d="M10,0 L0,5 L10,10 z" fill="#e8590c"/></marker></defs>'
            '<rect x="10" y="10" width="960" height="450" rx="16" fill="#f4f8ff" stroke="#9db6e8" stroke-dasharray="6 5"/>'
            '<text x="26" y="32" font-size="13" font-weight="700" fill="#0b3a9a">Docker Compose (4 contenedores en la red interna)</text>'
            + caja(['SPA React 19 + TypeScript', 'Mantine · TanStack Query', 'React Router · Zod'], 'Navegador (usuario)', 30, 60, 230, 100, '#fff7e6')
            + caja(['Sirve la SPA (archivos estáticos)', 'y reenvía /api al backend'], 'Frontend · Nginx', 30, 215, 230, 90, '#e8f0ff')
            + caja(['Monolito modular · API REST /api/v1', 'Spring Security + JWT · JPA/Hibernate', 'Transacciones y bloqueo pesimista'],
                   'Backend · Java 21 + Spring Boot 4', 340, 195, 330, 130, '#e8f0ff')
            + caja(['28 tablas · Flyway (V1–V21)', 'CHECK · triggers · Kardex inmutable'], 'PostgreSQL 17', 340, 360, 330, 80, '#e6f7ee')
            + caja(['FastAPI · Python 3.12', 'normalizador + RapidFuzz', 'extractor (Gemini visión)'], 'Servicio OCR', 730, 195, 220, 110, '#e8f0ff')
            + caja(['Gemini Flash (visión y', 'tool calling del agente)'], 'API de Google Gemini', 730, 60, 220, 90, '#fdeaea')
            + flecha(145, 160, 145, 215, 'HTTPS', 22, 0)
            + flecha(260, 260, 340, 260, '/api (JSON)', 0, -8)
            + flecha(505, 325, 505, 360, 'JDBC', 28, 0, doble=True)
            + flecha(670, 245, 730, 245, 'multipart + JWT', 0, -22)
            + flecha(730, 280, 670, 280, 'PUT resultados', 0, 16)
            + flecha(840, 195, 840, 150, 'visión', 32, 0)
            + flecha(560, 195, 740, 130, 'agente (tool calling)', -20, -16)
            + '<text x="30" y="445" font-size="12" fill="#5a6785">Base de datos: la accede SOLO el backend. El agente de IA y el OCR nunca tocan la base.</text>'
            + '</svg></div>')


# ====================================================================================================================
sec('veredicto', '0. ¿Es suficiente para presentar? Mi evaluación honesta',
    p('<strong>Sí, el sistema es sólido para una exposición universitaria.</strong> No es una maqueta: tiene backend real con reglas transaccionales, '
      'base de datos con integridad a nivel de motor, 4 roles, OCR + IA reales, contratos de API, pruebas automáticas, integración continua y '
      'despliegue con Docker. Lo que más impresiona no es la cantidad de pantallas, sino que <strong>las reglas de negocio se cumplen aunque alguien '
      'intente saltárselas</strong> (por ejemplo, no se puede reservar más stock del que existe, y el Kardex no se puede borrar).'),
    tabla(['Fortalezas para destacar', 'Riesgos que debes cubrir antes de exponer'], [
        ['Arquitectura clara y justificada con 13 ADR (decisiones por escrito, con alternativas descartadas).', '<strong>Internet y Gemini</strong>: el OCR real y el agente con Gemini dependen de la red y de la cuota. Ten plan B (proveedor simulado o capturas).'],
        ['Integridad de datos: restricciones, triggers diferibles, bloqueo pesimista, Kardex solo-anexión.', '<strong>Datos de demostración</strong>: ensaya con los datos sembrados; no improvises creando datos nuevos delante del profesor.'],
        ['Contrato OpenAPI por módulo validado en pruebas (frontend y backend hablan el mismo idioma).', '<strong>Cobertura real vs. alcance</strong>: tu documentación describe más de lo que está implementado en algunos puntos (p. ej. ADR-003 es del inicio y el producto evolucionó). Si el profesor compara, explica “la doc registra la decisión original; esto fue lo que se ajustó”.'],
        ['Pruebas: 691 en backend, 590 en frontend, 79 en OCR, accesibilidad y E2E; CI en GitHub Actions.', '<strong>Límites declarados</strong>: sin facturación SUNAT, sin contabilidad, sin WhatsApp, sin GPS (está en “alcance y límites”). Dilo tú primero.'],
        ['Demostración con flujo completo: cotizar → vender → reservar → despachar → Kardex; y reponer: compra → recepción.', '<strong>Tiempo</strong>: no muestres todo. Elige el flujo estrella (sección 10) y deja el resto como “también existe”.'],
    ]),
    consejo('Mi recomendación: 40 % del tiempo en <em>arquitectura y decisiones</em>, 40 % en <em>demostración</em> y 20 % en <em>preguntas</em>. Un profesor técnico '
            'valora más que sepas <strong>por qué</strong> elegiste algo y qué descartaste, que listar tecnologías.'),
    )

sec('plan', '1. Cómo proceder: el orden de tu exposición',
    p('Tu idea de ir <em>de lo estructural a la demostración</em> es la correcta. Te propongo este orden (≈ 25–30 min):'),
    tabla(['#', 'Bloque', 'Qué dices', 'Tiempo'], [
        ['1', 'Problema y solución', 'Ferretería con 3 ubicaciones (tienda + 2 almacenes), stock descuadrado, cotizaciones en papel, compras sin trazabilidad. MIGAL SmartOps centraliza todo.', '2 min'],
        ['2', 'Qué hace (módulos y roles)', 'Los 9 módulos funcionales y los 4 roles. Una diapositiva.', '2 min'],
        ['3', 'Arquitectura', 'Monolito modular + 1 servicio especializado (OCR) + PostgreSQL + Docker. Por qué NO microservicios.', '5 min'],
        ['4', 'Tecnologías por capa', 'Java/Spring, React/TS, Python/FastAPI, PostgreSQL, Docker; y para qué sirve cada librería.', '4 min'],
        ['5', 'Cómo está organizado el código', 'Mapa del repositorio, capas controller/service/repository/model, contrato OpenAPI, migraciones.', '4 min'],
        ['6', 'Decisiones difíciles', 'Reserva vs Kardex, concurrencia, IA que no toca la base, OCR en dos capas.', '4 min'],
        ['7', 'Calidad', 'Pruebas, CI, accesibilidad, contrato.', '2 min'],
        ['8', 'Demostración', 'Flujo estrella con datos sembrados (sección 10).', '8–10 min'],
        ['9', 'Límites y trabajo futuro + preguntas', 'Lo que no hace y cómo crecería.', '3 min + preguntas'],
    ]),
    ojo('Practica la demostración 3 veces seguidas. Lo que falla en vivo casi siempre es: sesión vencida, internet, o un dato que no recordabas cómo estaba.'),
    )

# ====================================================================================================================
sec('que-es', '2. Qué es MIGAL SmartOps (la explicación en 30 segundos)',
    p('<em>“MIGAL SmartOps es una plataforma web para la Ferretería Progresol Migal. Controla el inventario de la tienda y dos almacenes, permite cotizar y vender '
      'reservando el stock, genera órdenes de despacho, gestiona compras a proveedores y la recepción de mercadería, y deja un historial inmutable (Kardex) de cada '
      'entrada y salida. Además lee documentos escaneados con OCR para precargar datos y tiene un asistente de IA que responde consultas sobre el inventario.”</em>'),
    tabla(['Módulo', 'Para qué sirve', 'Quién lo usa'], [
        ['seguridad', 'Login, roles, usuarios, JWT', 'Todos / Administrador'],
        ['catalogo', 'Productos, categorías, SKU, precios de referencia', 'Almacenero, Administrador'],
        ['inventario', 'Existencias, reservas, alertas, Kardex, traslados', 'Almacenero, Vendedor (consulta)'],
        ['comercial', 'Clientes, cotizaciones, ventas', 'Vendedor, Administrador'],
        ['despacho', 'Órdenes (guías) de despacho, salida, entrega', 'Almacenero'],
        ['abastecimiento', 'Proveedores y órdenes de compra, faltantes', 'Reponedor'],
        ['recepciones', 'Recepción de mercadería contra una orden de compra', 'Almacenero'],
        ['documentosocr', 'Recibe documentos, los manda al servicio OCR, guarda resultados', 'Vendedor, Almacenero'],
        ['agenteia', 'Chat que consulta el inventario con herramientas autorizadas', 'Todos'],
    ]),
    )

# ====================================================================================================================
sec('arquitectura', '3. La arquitectura (lo más importante de explicar bien)',
    p('Respuesta corta a tu pregunta <em>“¿módulos, capas o microservicios?”</em>: <strong>las tres cosas, en distinto nivel</strong>.'),
    lista('<strong>Arquitectura en capas</strong> a nivel de todo el sistema: presentación (React) → lógica de negocio (Spring Boot) → servicios especializados (Python OCR) → persistencia (PostgreSQL).',
          '<strong>Monolito modular</strong> a nivel del backend: una sola aplicación desplegable, dividida en módulos funcionales (paquetes) con dependencias controladas.',
          '<strong>Un único microservicio</strong>: el OCR/Fuzzy Matching, separado porque usa otro lenguaje (Python), otro ritmo de cambio y es pesado/lento. No es “arquitectura de microservicios” general.'),
    svg_arquitectura(),
    sub('flujo-peticion', 'Cómo viaja una petición (ejemplo: registrar una venta)',
        flujo('Usuario hace clic', 'React valida con Zod', 'TanStack Query llama a /api/v1/ventas', 'Nginx reenvía al backend', 'Spring Security valida el JWT y el rol',
              'Controller → Service (transacción)', 'Repository/JPA → PostgreSQL'),
        pasos('<strong>React</strong> valida el formulario (campos obligatorios, fecha de entrega, cantidades) y envía <code>POST /api/v1/ventas</code> con la cabecera <code>Authorization: Bearer &lt;JWT&gt;</code> y una <code>Idempotency-Key</code>.',
              '<strong>Nginx</strong> (contenedor frontend) reenvía todo lo que empieza con <code>/api</code> al contenedor del backend.',
              '<strong>Spring Security</strong> verifica la firma del JWT, saca el usuario y el rol, y comprueba que el rol tenga permiso para ese endpoint (<code>@PreAuthorize</code>).',
              'El <strong>Controller</strong> convierte el JSON a un objeto y llama al <strong>Service</strong>; el controller no tiene reglas de negocio.',
              'El <strong>Service</strong> abre una <strong>transacción</strong>, bloquea las filas de inventario que va a tocar (bloqueo pesimista), calcula cuánto se puede reservar, crea la venta, el detalle y las reservas.',
              'Si algo falla (stock negativo, regla violada), se hace <em>rollback</em> de todo. Si va bien, <em>commit</em> y se responde 201 con el JSON.'),
        ),
    sub('por-que-monolito', '¿Por qué monolito modular y no microservicios? (pregunta típica)',
        p('Porque las operaciones de inventario, reservas y Kardex deben ser <strong>atómicas</strong> (RNF-08 y RNF-15): o se hace todo o no se hace nada. En un monolito eso es una sola '
          'transacción de base de datos. Con microservicios necesitarías <em>transacciones distribuidas</em> o sagas, más complejidad y más puntos de falla, sin beneficio para el tamaño de una ferretería. '
          'Los <strong>módulos</strong> dan el orden y permitirían extraer un servicio en el futuro. Está escrito en el ADR-004 con la tabla de alternativas descartadas.')),
    sub('modulos-capas', 'Módulos y capas dentro del backend',
        p('Cada módulo es un paquete Java con la misma estructura de 4 capas:'),
        tabla(['Capa', 'Qué hace', 'Qué NO hace'], [
            ['<code>controller</code>', 'Recibe HTTP, valida entrada, llama al servicio, devuelve DTO/JSON.', 'No contiene reglas de negocio ni accede a repositorios.'],
            ['<code>service</code>', 'Reglas de negocio y <strong>límites de transacción</strong> (<code>@Transactional</code>). Coordina con otros módulos.', 'No conoce HTTP.'],
            ['<code>repository</code>', 'Acceso a datos (Spring Data JPA, consultas JPQL/SQL).', 'Solo lo usa su propio módulo.'],
            ['<code>model</code>', 'Entidades JPA del dominio (Venta, Cotizacion, Inventario…).', 'No depende de otras capas.'],
        ]),
        lista('Un módulo accede a los datos de otro <strong>solo a través del <code>service</code> del otro</strong>, nunca a su repositorio.',
              '<strong>Solo el módulo <code>inventario</code></strong> modifica existencias, reservas y Kardex; los demás le piden la operación.',
              'Un paquete <code>comun</code> aporta lo transversal: errores de negocio, manejo global de errores (formato RFC 7807), paginación, idempotencia y tareas programadas.',
              'Estas reglas de dependencia se mantienen por convención arquitectónica y revisión de código en cada pull request.')),
    )

# ====================================================================================================================
sec('stack', '4. Tecnologías: qué usé, para qué, y por qué',
    seccion('stack-tabla', 'Mapa completo de tecnologías', tabla(['Capa', 'Tecnología', 'Para qué se usa aquí', 'Por qué esta y no otra'], [
        ['Backend', '<strong>Java 21</strong> (LTS)', 'Lenguaje del núcleo transaccional.', 'Tipado fuerte, ecosistema maduro, exigido por RNF-02. LTS = soporte prolongado.'],
        ['', '<strong>Spring Boot 4.1</strong>', 'Framework: API REST, inyección de dependencias, configuración.', 'Estándar de la industria para APIs empresariales; integra seguridad, datos y pruebas.'],
        ['', 'Spring Web MVC', 'Controladores REST.', 'Modelo síncrono simple, suficiente para la carga.'],
        ['', 'Spring Data JPA + Hibernate', 'Mapea entidades Java a tablas; repositorios.', 'Las relaciones (Venta→Detalle→Reserva) se expresan naturalmente. Modo <code>validate</code>: no altera el esquema.'],
        ['', 'Spring Security + OAuth2 Resource Server', 'Autenticación JWT (HS256) y autorización por rol/endpoint/método.', 'Sin sesión en servidor: encaja con una SPA. El usuario responsable sale del token, no del cuerpo.'],
        ['', 'Bean Validation', 'Validaciones declarativas (<code>@NotNull</code>, <code>@Size</code>).', 'Primera defensa; la BD es la segunda.'],
        ['', 'Flyway', 'Migraciones SQL versionadas (V1…V21).', 'Esquema trazable y reproducible; Hibernate nunca lo modifica.'],
        ['', 'Maven (+ wrapper)', 'Compilar, probar, empaquetar.', 'Convención del ecosistema; versión uniforme en todos los equipos.'],
        ['', 'JUnit 5, Mockito, <strong>Testcontainers</strong>', 'Pruebas; Testcontainers levanta un PostgreSQL real en cada corrida.', 'Probar contra PostgreSQL real (restricciones, triggers y transacciones se comportan igual que en producción).'],
        ['', 'swagger-request-validator', 'Valida cada respuesta/petición contra el contrato OpenAPI en los tests.', 'Garantiza que el código no se desvíe del contrato.'],
        ['Frontend', '<strong>React 19 + TypeScript</strong>', 'Interfaz SPA.', 'Componentes reutilizables; TypeScript evita errores de tipos antes de ejecutar.'],
        ['', 'Vite', 'Servidor de desarrollo y empaquetado.', 'Arranque y recarga muy rápidos.'],
        ['', '<strong>Mantine 9</strong>', 'Biblioteca de componentes (formularios, modales, tablas, notificaciones, dropzone).', 'Accesible y completa; se personalizó con un sistema de diseño (tokens).'],
        ['', '<strong>TanStack Query</strong>', 'Estado del servidor: caché, reintentos, invalidación tras cada mutación.', 'Evita “estado global a mano” y pantallas desactualizadas.'],
        ['', 'React Router', 'Navegación y rutas protegidas por rol.', 'Estándar de facto.'],
        ['', 'Zod + mantine-form', 'Validación de formularios con esquemas.', 'Una sola definición de reglas, con mensajes por campo.'],
        ['', 'Vitest, Testing Library, vitest-axe, Playwright, Storybook', 'Pruebas unitarias/pantallas, accesibilidad (axe), E2E y catálogo de componentes.', 'Calidad visual y de accesibilidad verificable.'],
        ['Servicio OCR', '<strong>Python 3.12 + FastAPI</strong>', 'API del microservicio, procesamiento asíncrono.', 'Python tiene el mejor ecosistema de IA/texto; FastAPI es rápido y valida con Pydantic.'],
        ['', '<strong>google-genai (Gemini)</strong>', 'Lee la imagen/PDF y devuelve las líneas en JSON.', 'Un modelo de visión descifra manuscritos y tablas sin entrenar un modelo propio.'],
        ['', '<strong>RapidFuzz</strong>', 'Fuzzy Matching: encuentra el SKU más parecido al texto leído.', 'Determinista y auditable; evita que la IA “invente” SKUs.'],
        ['Datos', '<strong>PostgreSQL 17</strong>', 'Base relacional transaccional.', 'ACID, CHECK, triggers diferibles, bloqueos de fila; exigido por RNF-01.'],
        ['Infra', '<strong>Docker + Compose</strong>', '4 contenedores: postgres, backend, servicio-ocr, frontend (Nginx).', 'Mismo entorno en cualquier PC; credenciales por variables de entorno.'],
        ['', 'GitHub Actions', '3 flujos de verificación (backend, frontend, servicio OCR).', 'Cada pull request se prueba antes de integrarse.'],
        ['IA', 'Gemini + <em>tool calling</em>', 'El agente decide qué función de consulta invocar.', 'El modelo no accede a la BD: solo llama funciones que el backend autoriza.'],
    ])),
    )

# ====================================================================================================================
sec('repo', '5. Mapa del repositorio: qué hace cada carpeta y archivo',
    p('Úsalo como “índice” cuando el profesor te pida “muéstrame dónde está eso”.'),
    seccion('repo-raiz', 'Nivel raíz', tabla(['Ruta', 'Contenido'], [
        ['<code>docs/</code>', 'Toda la documentación de diseño: requerimientos, casos de uso, diagramas (secuencia, actividades, paquetes, flujo de datos), modelo de datos y de clases, matriz de trazabilidad, ADR.'],
        ['<code>docs/adr/</code>', '<strong>13 decisiones de arquitectura</strong> (ADR-001 a 013): cada una con contexto, decisión, justificación, alternativas y consecuencias.'],
        ['<code>backend/</code>', 'API Java/Spring Boot. <code>pom.xml</code>, <code>Dockerfile</code>, código en <code>src/main/java/com/migal/smartops/</code>, pruebas en <code>src/test</code>, contratos en <code>api/</code>.'],
        ['<code>backend/api/modulos/*.yaml</code>', '<strong>Contrato OpenAPI</strong> de cada módulo (autenticación, catálogo, inventario, comercial, ventas, despacho, abastecimiento, recepciones, documentos-ocr, agente-ia). <code>componentes/</code> tiene errores, parámetros y esquemas comunes.'],
        ['<code>frontend/</code>', 'SPA React. <code>Dockerfile</code> + <code>nginx.conf</code> para servirla y reenviar <code>/api</code>.'],
        ['<code>servicio-ocr/</code>', 'Microservicio Python (FastAPI): <code>app/</code>, pruebas, <code>evaluar.py</code> (mide la calidad del OCR), contrato <code>api/procesar.yaml</code>.'],
        ['<code>database/migraciones/</code>', '<strong>V1…V21</strong>: scripts SQL de Flyway. Es la <em>única</em> fuente del esquema.'],
        ['<code>compose.yaml</code>', 'Orquesta los 4 contenedores con healthchecks y variables de entorno.'],
        ['<code>scripts/</code>', '<code>levantar-demo.ps1</code> (arranca todo y crea el .env), <code>sembrar-demo.py</code> (datos de demostración vía la API), <code>respaldar-bd.ps1</code>, <code>verificar-e2e-local.ps1</code>, <code>generar-secuencias.py</code>.'],
        ['<code>.github/workflows/</code>', 'CI: <code>verificar-backend</code>, <code>verificar-frontend</code>, <code>verificar-servicio-ocr</code>. <code>CODEOWNERS</code> y plantilla de PR.'],
        ['<code>CONTRIBUTING.md</code>', 'Reglas del equipo: ramas, PR pequeños, migraciones, sin credenciales en el repo.'],
        ['<code>docs/guia/</code>', 'La guía de uso con capturas y esta guía de exposición.'],
    ])),
    seccion('repo-backend', 'Backend por módulo (<code>backend/src/main/java/com/migal/smartops/</code>)', tabla(['Paquete', 'Archivos clave', 'Responsabilidad'], [
        ['<code>seguridad</code>', 'Usuario, RolUsuario, servicios de autenticación/usuarios, configuración de Spring Security', 'Login, emisión/validación de JWT, roles, rol de demostración, creación del admin inicial.'],
        ['<code>catalogo</code>', 'Producto, Categoria, ProductoService', 'Maestro de productos; SKU; categoría efectiva y reglas de aprobación.'],
        ['<code>inventario</code>', 'Inventario, MovimientoKardex, ReservaStock, <code>MovimientoInventarioService</code>, <code>ReservaInventarioService</code>, <code>ConsultaInventarioService</code>', 'El corazón: único módulo que cambia stock. Reservas, movimientos, traslados, alertas y consultas de disponibilidad (incluye “por recibir” y faltantes).'],
        ['<code>comercial</code>', 'Cliente, Cotizacion (+ historial), Venta, <code>CotizacionService</code>, <code>VentaService</code>, <code>VencimientoCotizaciones</code>', 'Ciclo de cotización (emitida→aceptada→validada→venta; rechazada, cancelada, vencida), ventas con reserva parcial, cancelaciones; tarea programada que vence cotizaciones.'],
        ['<code>despacho</code>', 'GuiaDespacho, ReimpresionGuia, DespachoService', 'Genera la orden, confirma la salida (genera el Kardex), registra la entrega, reimpresiones con motivo.'],
        ['<code>abastecimiento</code>', 'Proveedor, OrdenCompra, servicios', 'Órdenes de compra con fecha estimada de llegada, catálogo del proveedor.'],
        ['<code>recepciones</code>', 'OrdenRecepcion, DetalleRecepcion, servicio', 'Recepción contra una OC, por ubicación, con tope por línea; al validar genera entradas de Kardex.'],
        ['<code>documentosocr</code>', 'DocumentoDigital, DetalleExtraido, <code>ProveedorOcrHttp</code>, <code>ProcesadorOcrSimulado</code>', 'Recibe el archivo, lo encola <em>después del commit</em> hacia el servicio Python, recibe el resultado por callback (JWT de servicio), vigila pendientes.'],
        ['<code>agenteia</code>', '<code>ServicioAgente</code>, <code>HerramientasInventario/Ventas/Despacho…</code>, <code>ProveedorLlmGemini</code>, <code>…Simulado</code>, <code>…Hibrido</code>', 'Agente: define herramientas de <em>solo lectura</em>, valida permisos, llama a Gemini (o reglas locales), guarda la conversación.'],
        ['<code>comun</code>', 'CodigoError, ErrorDeNegocio, ManejadorDeErrores, ServicioIdempotencia', 'Errores homogéneos, paginación, idempotencia de operaciones (cabecera <code>Idempotency-Key</code>), tareas programadas.'],
    ])),
    seccion('repo-frontend', 'Frontend (<code>frontend/src/</code>)', tabla(['Ruta', 'Responsabilidad'], [
        ['<code>main.tsx</code>, <code>App.tsx</code>', 'Punto de entrada y enrutador.'],
        ['<code>app/</code>', '<code>AppProviders</code> (tema Mantine, notificaciones, modales, TanStack Query, sesión) y la configuración de la caché de consultas.'],
        ['<code>routes/</code>', '<code>AppRoutes</code>, <code>navigation.ts</code> (menú por rol) y <code>modulos/*.tsx</code> (rutas de cada módulo con los roles permitidos).'],
        ['<code>api/</code>', '<code>cliente.ts</code>: cliente HTTP único (token, errores, idempotencia). <code>mock.ts</code>: datos simulados cuando no hay backend real.'],
        ['<code>features/&lt;módulo&gt;/</code>', 'Una carpeta por módulo: <code>api.ts</code> (llamadas), <code>types.ts</code>, páginas (<code>*Page.tsx</code>), lógica pura (<code>calculos.ts</code>) y pruebas.'],
        ['<code>components/ui</code>', 'Piezas genéricas reutilizables: DataTable, FormPage, LineasEditor, Dropzone, Stepper…'],
        ['<code>components/domain</code>', 'Piezas del negocio: StockBar, StatusBadge, Constancia, DocumentoImprimible, MatchConfidence…'],
        ['<code>theme/</code>', 'Tokens de diseño (colores, espaciados, tipografías) y estilos globales.'],
        ['<code>features/seguridad</code>', 'Login, sesión, roles (<code>roles.ts</code>), permisos por acción (<code>permisos.ts</code>) y guardas de ruta.'],
    ])),
    seccion('repo-ocr', 'Servicio OCR (<code>servicio-ocr/app/</code>)', tabla(['Archivo', 'Función'], [
        ['<code>main.py</code>', 'App FastAPI: endpoint que recibe el documento (responde 202) y lo procesa en segundo plano.'],
        ['<code>extractor.py</code>', 'Llama a Gemini (visión) con un prompt que exige JSON estricto: texto crudo y cantidad por línea. Cadena de modelos de respaldo.'],
        ['<code>normalizador.py</code>', 'Traduce jerga ferretera: <em>kk→king kong, f°→fierro, bls→bolsa, pulg/"→pulgada</em>; extrae medidas.'],
        ['<code>matcher.py</code>', 'Fuzzy Matching con RapidFuzz sobre el catálogo; penaliza si la medida no coincide (fierro 1/2 ≠ 3/8).'],
        ['<code>resultados.py</code>', 'Arma el resultado y lo devuelve al backend (<code>PUT /api/v1/documentos/{id}/resultados</code>) con un JWT de servicio.'],
        ['<code>config.py</code>, <code>esquemas.py</code>', 'Configuración por variables de entorno y modelos Pydantic.'],
    ])),
    )

# ====================================================================================================================
sec('decisiones', '6. Las decisiones técnicas que debes saber defender',
    seccion('d-reserva', 'Reserva ≠ salida física (stock físico, reservado, libre)', p(
        'Al vender, el stock <strong>no sale</strong>: se <em>reserva</em>. La salida real al Kardex ocurre solo cuando el almacén <strong>confirma la salida</strong> del despacho. '
        '<code>libre = físico − reservado</code>. Esto permite prometer sin vender dos veces lo mismo y mantener un Kardex honesto (solo registra lo que físicamente se movió).'),
        lista('Si falta stock, la venta reserva lo que hay y el resto queda <strong>pendiente de abastecimiento</strong>.', 'Cancelar una venta libera reservas sin crear movimientos “ficticios” de Kardex.')),
    seccion('d-concurrencia', 'Concurrencia: ¿qué pasa si dos vendedores reservan lo mismo a la vez?', p(
        'Tres defensas en capas: (1) la operación corre en una <strong>transacción</strong>; (2) se usa <strong>bloqueo pesimista</strong> (<code>@Lock(PESSIMISTIC_WRITE)</code> = <code>SELECT … FOR UPDATE</code>) sobre '
        'las filas de inventario, así el segundo espera al primero; (3) la base tiene <strong>triggers de restricción diferibles</strong> que, al confirmar, verifican que la suma de reservas nunca supere el stock físico. '
        'Aunque hubiera un bug en Java, la base de datos rechaza el estado inválido.')),
    seccion('d-kardex', 'Kardex inmutable', p(
        'La tabla <code>movimiento_kardex</code> tiene un trigger que <strong>solo permite insertar</strong> (no UPDATE, DELETE ni TRUNCATE). Un error se corrige con un movimiento contrario con motivo. '
        'Así hay trazabilidad completa: quién, cuándo, por qué y a partir de qué operación (recepción, despacho, ajuste, traslado).')),
    seccion('d-idem', 'Idempotencia', p(
        'Las operaciones sensibles aceptan una cabecera <code>Idempotency-Key</code>. Si el cliente reintenta (por red lenta o doble clic) con la misma clave, el backend devuelve la <strong>respuesta original</strong> '
        'sin repetir la operación. Evita ventas o recepciones duplicadas. Se guarda en la tabla <code>idempotencia</code> y se limpia a diario.')),
    seccion('d-contrato', 'Contrato primero (OpenAPI)', p(
        'Cada módulo tiene su YAML OpenAPI. Las pruebas validan que cada respuesta real cumple el contrato, y el frontend adapta sus tipos a él. Hay incluso una prueba que verifica que los parámetros de las '
        '<em>herramientas del agente</em> coinciden con los de las operaciones del contrato.')),
    seccion('d-ia', 'IA con límites', p(
        '<strong>El modelo nunca toca la base de datos ni modifica nada.</strong> El agente solo puede invocar funciones de consulta que el backend define y autoriza según el rol (<em>tool calling</em>). '
        'Las preguntas simples (alertas, disponibilidad de un producto) se resuelven por reglas sin llamar a Gemini (más rápido y barato). Hay tres proveedores intercambiables: simulado, Gemini e híbrido.')),
    seccion('d-ocr', 'OCR en dos capas', p(
        '<strong>Capa 1 (IA):</strong> Gemini lee la imagen y devuelve texto + cantidad. <strong>Capa 2 (determinista):</strong> normalización + RapidFuzz asigna el SKU y calcula la confianza. '
        'Se separó para que <em>la IA no invente códigos de producto</em> (alucinaciones). Además el resultado <strong>siempre lo revisa un humano</strong> antes de usarse, y el OCR <em>no genera ventas ni movimientos por sí solo</em>.'),
        p('<strong>Comunicación asíncrona:</strong> el backend encola el documento <em>después</em> de confirmar su transacción (<code>AFTER_COMMIT</code>); el servicio responde 202 y, al terminar, llama de vuelta con un <strong>JWT de servicio</strong> '
          'de alcance limitado (<code>ocr:resultados:escribir</code>). Una tarea programada marca como “servicio no disponible” los documentos que no responden a tiempo.')),
    seccion('d-seg', 'Seguridad', lista(
        'Contraseñas guardadas como <strong>hash</strong> (nunca en claro).', 'JWT firmado (HS256) con secreto en variable de entorno; sin token de refresco (se vuelve a iniciar sesión al expirar).',
        'Autorización por <strong>endpoint</strong> y por <strong>método</strong>; el usuario responsable sale del token, no del cuerpo de la petición.',
        'Las credenciales viven fuera del repositorio (<code>.env</code>, no versionado); los puertos de la base y del backend se publican solo en <code>127.0.0.1</code>.',
        '<strong>Rol de demostración</strong> (ADR-013): reúne los permisos de los 4 roles; está <em>apagado por defecto</em> y se activa con una variable de entorno.')),
    seccion('d-bd', 'Base de datos', lista(
        '<strong>28 tablas</strong> (incluye la de Flyway), claves <code>bigint</code> generadas, nombres de restricciones explícitos (<code>pk_</code>, <code>fk_</code>, <code>uq_</code>, <code>ck_</code>).',
        '<strong>Sin eliminación en cascada</strong> y sin borrar entidades de negocio: se <em>desactivan</em> (<code>estado_activo</code>).',
        'Valores derivados (libre, totales, pendientes) <strong>no se guardan</strong>: se calculan, para no tener datos contradictorios. La excepción justificada es <code>cantidad_fisica</code>.',
        'Cantidades <code>numeric(14,3)</code> (admite kg y metros) e importes <code>numeric(14,2)</code>; estados como <code>varchar</code> con <code>CHECK</code>.',
        'Esquema versionado con <strong>Flyway</strong> (V1–V21), migraciones inmutables: un cambio se hace con una migración nueva.')),
    )

# ====================================================================================================================
sec('calidad', '7. Calidad: cómo sabes que funciona',
    tabla(['Qué', 'Cifras / herramientas'], [
        ['Pruebas del backend', '84 archivos de prueba, <strong>691 casos</strong>, con PostgreSQL real (Testcontainers) y validación contra el contrato OpenAPI.'],
        ['Pruebas del frontend', '63 archivos, <strong>590 casos</strong> (Vitest + Testing Library), incluyendo flujos de integración y accesibilidad con <em>axe</em>.'],
        ['Pruebas del OCR', '<strong>79 casos</strong> con pytest y respx (simula HTTP) más un conjunto de evaluación; sin llamar a Gemini en CI.'],
        ['E2E', 'Playwright con un PostgreSQL temporal (<code>docs/e2e-local.md</code>).'],
        ['Integración continua', '3 flujos de GitHub Actions por componente; cada pull request se verifica antes de integrarse.'],
        ['Trazabilidad', 'Matriz requerimiento ↔ caso de uso ↔ diagrama ↔ código (<code>docs/matriz-trazabilidad.md</code>).'],
        ['Proceso del equipo', 'Pull requests, CODEOWNERS, plantilla de PR, tareas en Notion, ADR para decisiones.'],
    ]),
    )

# ====================================================================================================================
sec('negocio', '8. Reglas de negocio clave (para explicar el “por qué” del flujo)',
    lista('<strong>Cotización</strong>: emitida → aceptada por el cliente → validada por MIGAL (Administrador) → se convierte en <strong>una sola</strong> venta. También rechazada, cancelada o vencida. No reserva stock.',
          '<strong>Venta</strong>: reserva lo disponible; lo demás queda pendiente de abastecimiento. La <strong>fecha de entrega requerida es obligatoria</strong> (sirve para planificar compras).',
          '<strong>Despacho</strong>: único y completo por venta; solo se confirma si todo está reservado; al confirmar la salida se genera el Kardex.',
          '<strong>Cancelación de venta</strong>: posible hasta que el almacén confirme la salida; libera reservas; con motivo obligatorio.',
          '<strong>Compras y recepción</strong>: crear una orden <em>no</em> cambia el stock; solo la <em>recepción confirmada</em> lo aumenta. Hay tope por línea (no se recibe más de lo pendiente).',
          '<strong>Alertas</strong>: reglas deterministas sobre stock libre vs. mínimo (no machine learning).',
          '<strong>Roles</strong>: Administrador, Vendedor, Almacenero, Reponedor; el de Demostración los reúne.'),
    ojo('La documentación (ADR-003, requerimientos) es del diseño inicial y el producto evolucionó (p. ej. cancelar/modificar cotizaciones aceptadas, fecha de entrega en la venta, previsto neto). '
        'Si el profesor nota una diferencia, la respuesta es: <em>“la decisión original quedó registrada en el ADR; las mejoras posteriores salieron de pruebas con usuarios y están en los pull requests #133–#136”</em>.'),
    )

# ====================================================================================================================
sec('limites', '9. Límites del sistema y trabajo futuro (dilo tú antes de que pregunten)',
    lista('<strong>No incluye</strong>: facturación electrónica SUNAT, contabilidad, WhatsApp Business, gestión de flota/GPS.',
          '<strong>Sin devoluciones</strong> tras la salida confirmada ni notificaciones automáticas al cliente (decisión de alcance).',
          '<strong>OCR/IA dependen de un servicio externo</strong> (Gemini): requieren internet, clave y cuota; por eso existe el modo simulado y la revisión humana.',
          '<strong>Un solo JWT de acceso</strong> sin refresco: la sesión se renueva iniciando sesión de nuevo.',
          '<strong>Despliegue</strong>: pensado para un servidor/PC con Docker; la base alojada podría migrar a un servicio gestionado.',
          '<strong>Mejoras futuras</strong>: devoluciones, reportes y tableros, notificaciones, refresco de token, métricas y trazas (observabilidad), extracción de módulos si la carga lo exigiera.'),
    )

# ====================================================================================================================
sec('demo', '10. Guion de la demostración (≈ 8–10 min)',
    p('<strong>Preparación (antes de entrar al aula):</strong> contenedores arriba (<code>docker compose --profile completo up -d --wait</code>), sesión iniciada con el usuario <em>Demostración</em>, '
      'tema claro, zoom del navegador al 110 %, una pestaña por pantalla, y la guía de uso abierta como respaldo.'),
    flujo('Disponibilidad', 'Cotización (stock para decidir)', 'Aceptar y validar', 'Venta con fecha', 'Despacho y salida', 'Kardex', 'Compra y recepción'),
    pasos('<strong>Disponibilidad y alertas</strong> (1 min): muestra existencias por ubicación, físico/reservado/previsto y la pestaña “En alerta”.',
          '<strong>Cotización</strong> (2 min): abre una cotización y señala <em>Stock para decidir</em> (libre, por recibir, previsto neto, “Falta comprar”). Menciona que no reserva.',
          '<strong>Aceptar → Validar → Convertir en venta</strong> (2 min): cambia de rol mentalmente (Vendedor/Administrador). En la venta, resalta la <em>fecha de entrega requerida</em> y las cantidades pendientes.',
          '<strong>Despacho</strong> (2 min): genera la orden, imprímela (hoja A4), <em>Confirmar salida</em> y luego <em>Ver movimientos en el Kardex</em> (solo los de esa operación).',
          '<strong>Reposición</strong> (2 min): pestaña <em>Faltantes de ventas</em> → nueva orden de compra con fecha estimada → recepción en 3 pasos repartida por ubicación; muestra el stock subiendo.',
          '<strong>Remate (1 min)</strong>: Agente de IA con “Productos en stock crítico” y OCR (si hay internet), recalcando que el <em>modelo no toca la base</em>.'),
    consejo('Si algo falla en vivo, no te disculpes: di “esto es justo la regla de negocio funcionando” si es una validación, o pasa a la guía de uso con capturas. Un fallo manejado con calma impresiona más que una demo perfecta.'),
    )

# ====================================================================================================================
sec('preguntas', '11. Preguntas probables del profesor (de básicas a avanzadas)',
    seccion('q-basicas', 'Básicas', qa('¿Qué problema resuelve?', p('Centraliza inventario, ventas y compras de una ferretería con 3 ubicaciones, evita vender stock inexistente y deja trazabilidad de cada movimiento.')),
            qa('¿Qué lenguajes usaste?', p('Java 21 (backend), TypeScript/JavaScript (frontend con React), Python 3.12 (servicio OCR) y SQL (PostgreSQL/Flyway).')),
            qa('¿Qué es una API REST?', p('Una interfaz HTTP donde cada recurso (ventas, productos) tiene una URL y se opera con verbos (GET consulta, POST crea, PUT modifica). Aquí intercambia JSON bajo <code>/api/v1</code>.')),
            qa('¿Qué es una SPA?', p('Single Page Application: la página se carga una vez y React cambia las vistas sin recargar; pide datos al backend por la API.')),
            qa('¿Para qué sirve Docker aquí?', p('Empaqueta cada componente con sus dependencias para que corra igual en cualquier computador. <code>compose.yaml</code> levanta los 4 contenedores con un comando.')),
            qa('¿Qué es el Kardex?', p('El registro histórico de todas las entradas y salidas de cada producto. Aquí es inmutable: solo se agregan movimientos.')),
            qa('¿Qué roles hay?', p('Administrador, Vendedor, Almacenero y Reponedor (más un rol de demostración que los reúne). El rol decide qué módulos y acciones ve el usuario, y el backend lo vuelve a verificar.'))),
    seccion('q-medias', 'Intermedias', qa('¿Por qué Spring Boot y no Node/Django?', p('Lo exigía el requerimiento RNF-02 y encaja con transacciones de inventario: Spring ofrece transacciones declarativas, seguridad y pruebas integradas, y Java tiene tipado fuerte para un dominio con mucha regla de negocio.')),
            qa('¿Cómo se comunican frontend y backend?', p('Por HTTP/JSON. Nginx sirve la SPA y reenvía <code>/api</code> al backend. Cada petición lleva un JWT en <code>Authorization: Bearer</code>.')),
            qa('¿Cómo funciona el login?', p('El backend verifica usuario y hash de contraseña y emite un JWT firmado (HS256) con el rol. El frontend lo guarda en la sesión y lo envía en cada petición; el backend valida firma, expiración y rol.')),
            qa('¿Qué hace Flyway y por qué no <code>ddl-auto=update</code>?', p('Aplica migraciones SQL versionadas. <code>update</code> cambia el esquema sin dejar registro; con Flyway el esquema es reproducible, revisable en pull requests e inmutable una vez integrado.')),
            qa('¿Cómo evitas vender más de lo que hay?', p('Transacción + bloqueo pesimista de las filas de inventario + trigger diferible en la base que impide que las reservas superen el stock físico. Tres defensas.')),
            qa('¿Qué es TanStack Query y por qué lo usaste?', p('Administra el <em>estado del servidor</em> (datos que viven en el backend): caché, estados de carga/error, reintentos e invalidación. Evita copiar datos a un estado global y mantiene las pantallas actualizadas tras cada operación.')),
            qa('¿Cómo validas los formularios?', p('En el frontend con Zod (mensajes por campo) para la experiencia de usuario, y <em>de nuevo</em> en el backend con Bean Validation y la base con CHECK. Nunca se confía solo en el cliente.')),
            qa('¿Cómo pruebas el sistema?', p('Backend con JUnit + Testcontainers (PostgreSQL real) y validación contra OpenAPI; frontend con Vitest/Testing Library y axe para accesibilidad; Playwright para E2E; todo corre en GitHub Actions.'))),
    seccion('q-avanzadas', 'Avanzadas', qa('¿Por qué no microservicios?', p('Las operaciones de inventario, reservas y Kardex deben ser atómicas. En microservicios requerirían transacciones distribuidas (sagas, consistencia eventual) que añaden complejidad sin beneficio a esta escala. Elegí monolito modular y extraje <em>solo</em> el OCR, que sí justifica separarse (lenguaje, carga y ritmo distintos). Está en el ADR-004.')),
            qa('¿Cómo garantizas que los módulos no se acoplen?', p('Reglas de dependencia (un módulo solo usa el <code>service</code> de otro, solo <code>inventario</code> muta stock) aplicadas en la estructura por paquetes y verificadas en la revisión de cada pull request.')),
            qa('¿Qué pasa si el cliente reintenta una petición por mala conexión?', p('La cabecera <code>Idempotency-Key</code> hace que el reintento devuelva la respuesta original sin ejecutar de nuevo la operación; se registra en la tabla <code>idempotencia</code>.')),
            qa('¿Cómo evitas que la IA invente SKUs?', p('La IA solo extrae texto y cantidades. La asignación de SKU la hace un pipeline determinista: normalización de jerga, extracción de medidas y similitud con RapidFuzz contra el catálogo, con penalización fuerte si la medida no coincide. Y un humano revisa antes de usarlo.')),
            qa('¿El agente puede modificar el inventario?', p('No. Solo invoca funciones de consulta que el backend expone y autoriza por rol (tool calling). No tiene acceso a la base de datos. Hay una prueba que verifica que las herramientas coinciden con el contrato.')),
            qa('¿Cómo manejas la caída de Gemini o de la cuota?', p('Una lista de modelos con cambio automático al agotarse la cuota, reintento ante saturación, plazos configurables, y el backend marca <code>SERVICIO_NO_DISPONIBLE</code> si el OCR no responde a tiempo. Para el agente, las consultas simples se resuelven por reglas sin Gemini.')),
            qa('¿Por qué bloqueo pesimista y no optimista?', p('El conflicto de reservar el mismo stock es <em>frecuente y costoso</em> si falla; bloquear la fila y que el segundo espere es más simple y predecible que reintentar. El bloqueo optimista conviene cuando los conflictos son raros.')),
            qa('¿Cómo aseguras la integridad si hay un bug en el código Java?', p('La base es la última línea de defensa: CHECK de cantidades, claves foráneas con RESTRICT, triggers diferibles para sumas de reservas, Kardex solo-anexión y unicidades. Un estado inválido se rechaza al confirmar.')),
            qa('¿Cómo escalaría el sistema?', p('Verticalmente primero (el cuello de botella real son las transacciones de inventario). Después réplicas del backend detrás de un balanceador (es sin estado gracias a JWT), réplicas de lectura de PostgreSQL y, si hiciera falta, extraer módulos (por ejemplo reportes) como servicios.')),
            qa('¿Qué es el contrato OpenAPI y qué ganas?', p('Una especificación de cada endpoint (rutas, datos, errores, roles). Genera una “fuente de verdad” común: las pruebas validan el backend contra ella y el frontend adapta sus tipos, evitando desalineaciones.')),
            qa('¿Qué es RLS en tus migraciones?', p('Row Level Security de PostgreSQL activada en las tablas como defensa adicional (el backend es el único cliente de la base y Supabase solo se usa como PostgreSQL).')),
            qa('¿Qué mejorarías?', p('Devoluciones, reportes/tableros, notificaciones, refresco de token, observabilidad (métricas/trazas) y pruebas de carga.'))),
    )

# ====================================================================================================================
sec('estudio', '12. Plan de estudio para dominarlo (paso a paso)',
    tabla(['Día', 'Qué hacer', 'Meta'], [
        ['1', 'Leer <code>docs/arquitectura-tecnologica.md</code> y mirar la figura de arquitectura. Recorrer <code>compose.yaml</code>.', 'Poder dibujar los 4 contenedores y explicar cómo hablan.'],
        ['2', 'Leer ADR-004 (backend) y ADR-001 (frontend); abrir el backend y mirar un módulo completo (<code>comercial</code>: controller → service → repository → model).', 'Explicar las 4 capas con un ejemplo real.'],
        ['3', 'Leer ADR-005 (BD) y abrir <code>V7__crear_kardex.sql</code> y <code>V8__crear_reservas.sql</code> (triggers).', 'Explicar por qué el Kardex es inmutable y cómo se evita sobrerreservar.'],
        ['4', 'Leer ADR-003, ADR-011 y ADR-012; leer <code>servicio-ocr/app/matcher.py</code> y <code>normalizador.py</code>.', 'Explicar el OCR en dos capas y el agente con tool calling.'],
        ['5', 'Recorrer el frontend: <code>routes/</code>, <code>api/cliente.ts</code>, un <code>features/comercial/CotizacionPage.tsx</code>.', 'Explicar cómo se protege una ruta por rol y cómo se llama a la API.'],
        ['6', 'Ensayar la demostración 3 veces con cronómetro, y responder en voz alta las preguntas de la sección 11.', 'Exposición fluida de 25–30 minutos.'],
    ]),
    consejo('Cuando quieras, practicamos: puedes pedirme que haga de profesor y te pregunte por módulos, y vamos corrigiendo tus respuestas. También puedo preparar las diapositivas.'),
    )


def main():
    idx = [(f'h{n}', i, t) for i, n, t in re.findall(r'<section id="([^"]+)"><h([23])>(.*?)</h\2>', ''.join(S))]
    salida = construir(S, idx, 'Guía de exposición de MIGAL SmartOps',
                       'Arquitectura, tecnologías, mapa del código, demostración y preguntas del profesor, de lo básico a lo avanzado.')
    ruta = os.path.join(BASE, 'guia-de-exposicion.html')
    open(ruta, 'w', encoding='utf-8').write(salida)
    print(ruta, round(os.path.getsize(ruta) / 1e6, 2), 'MB')


if __name__ == '__main__':
    main()
