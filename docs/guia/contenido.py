"""Contenido de la guía de uso de MIGAL SmartOps."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generador import (BASE, ANCHO, consejo, construir, figura, flujo, lista, ojo, p, pasos, seccion, tabla)  # noqa: E402

SI = '<span class="si">✔</span>'
NO = '<span class="no">—</span>'

S = []   # secciones
IDX = []  # (clase, id, título)


def sec(id_, titulo, *contenido, nivel=2):
    S.append(seccion(id_, titulo, *contenido, nivel=nivel))


# ===================================================================================================================
sec('inicio', '1. Antes de empezar',
    p('<strong>MIGAL SmartOps</strong> es el sistema de la Ferretería Progresol Migal para llevar el inventario, vender, despachar, '
      'comprar a proveedores y recibir mercadería, todo en un solo lugar y con trazabilidad (cada movimiento queda registrado).'),
    p('Esta guía recorre <strong>todas las pantallas</strong> con capturas reales del sistema. En cada imagen verás '
      '<span class="insig">1</span> <span class="insig">2</span> <span class="insig">3</span> flechas numeradas que señalan el botón o '
      'el campo del que se habla; el significado de cada número está justo debajo de la imagen.'),
    consejo('Si es la primera vez, lee las secciones 1 a 3. Después puedes ir directo al módulo que necesites desde el índice de la izquierda.'),
    seccion('inicio-sesion', 'Iniciar sesión', figura('01-login', [
        ('usuario', 'Escribe tu <strong>usuario</strong> (por ejemplo <code>admin</code>).', 'l'),
        ('clave', 'Escribe tu <strong>contraseña</strong>.', 'l'),
        ('ojo', 'El ícono del ojo <strong>muestra u oculta</strong> la contraseña mientras la escribes.', 'r'),
        ('entrar', 'Pulsa <strong>Iniciar sesión</strong> (o la tecla Enter).', 'b'),
    ], 'Pantalla de acceso.'),
        p('El administrador te entrega tu usuario y una contraseña temporal. Por seguridad conviene cambiarla en el primer ingreso '
          '(ver <a href="#cuenta">Cambiar tu contraseña</a>).'),
        ojo('Si escribes mal tus datos el sistema lo avisa y no te deja entrar. Si olvidaste la contraseña, pide al Administrador que te '
            'registre una temporal desde <em>Usuarios y roles</em>.'),
        nivel=3),
    )

# ===================================================================================================================
sec('pantalla', '2. Conoce la pantalla principal',
    p('Todas las pantallas comparten la misma estructura: un <strong>menú a la izquierda</strong>, una <strong>barra superior</strong> con '
      'ajustes de tu cuenta y, en el centro, el contenido del módulo.'),
    figura('03-disponibilidad', [
        ('menu', '<strong>Menú de módulos</strong>, agrupado en Operación, Comercial, Abastecimiento, Asistente, Administración y Mi cuenta. '
                 'Solo ves los módulos que tu rol puede usar. El módulo activo aparece resaltado.', 'r'),
        ('cabecera', '<strong>Barra superior</strong>: modo claro/oscuro, tu nombre con tu rol y el botón para <strong>cerrar sesión</strong>.', 'bl'),
        ('buscar', 'Casi todas las listas tienen un <strong>buscador</strong> y <strong>pestañas</strong> con contadores para filtrar rápido.', 'b'),
        ('alerta', 'Las pestañas muestran cuántos registros hay en cada estado (por ejemplo “En alerta 165”).', 'b'),
    ], 'Pantalla de Disponibilidad con el menú y la barra superior.'),
    seccion('roles', 'Roles: qué puede hacer cada persona', p(
        'Cada usuario tiene un <strong>rol</strong>. El rol decide qué módulos ve y qué botones aparecen. Si un botón no aparece, es porque tu rol no '
        'tiene permiso para esa acción.'),
        tabla(['Módulo', 'Administrador', 'Vendedor', 'Almacenero', 'Reponedor'], [
            ['Disponibilidad', SI, SI, SI, SI], ['Catálogo', SI, NO, SI, NO], ['Alertas de stock', SI, NO, SI, SI],
            ['Movimientos y Kardex', SI, NO, SI, NO], ['Despachos', SI, NO, SI, NO], ['Recepciones', SI, NO, SI, NO],
            ['Documentos OCR', SI, SI, SI, NO], ['Cotizaciones', SI, SI, NO, NO], ['Ventas', SI, SI, SI, NO],
            ['Clientes', SI, SI, NO, NO], ['Proveedores', SI, NO, NO, SI], ['Órdenes de compra', SI, NO, NO, SI],
            ['Agente de IA', SI, SI, SI, SI], ['Usuarios y roles', SI, NO, NO, NO], ['Cambiar contraseña', SI, SI, SI, SI],
        ]),
        p('<strong>Acciones importantes por rol</strong> (el Administrador ve todo, pero en los flujos operativos suele ser de consulta):'),
        lista('<strong>Vendedor</strong>: crea y modifica cotizaciones, registra la aceptación del cliente, convierte en venta, registra y cancela ventas, '
              'duplica cotizaciones.',
              '<strong>Administrador</strong>: <em>valida</em> las cotizaciones aceptadas, cancela con origen “MIGAL” y gestiona usuarios.',
              '<strong>Almacenero</strong>: registra movimientos de Kardex, recepciones, genera órdenes de despacho, confirma salidas y entregas.',
              '<strong>Reponedor</strong>: crea órdenes de compra y gestiona proveedores.'),
        consejo('Existe también un usuario de <strong>Demostración</strong> que reúne los permisos de los cuatro roles, útil para presentar el sistema. '
                'Aparece como “Rol: Demostración” en el Agente de IA y en la barra superior.'),
        nivel=3),
    seccion('ajustes', 'Ajustes de la barra superior',
        p('El ícono de la <strong>luna/sol</strong> cambia entre modo claro y oscuro; el sistema recuerda tu elección. El ícono de la derecha, junto a tu '
          'nombre, <strong>cierra la sesión</strong>.'),
        nivel=3),
    seccion('cuenta', 'Cambiar tu contraseña', figura('34-cuenta', [
        ('actual', 'Escribe tu <strong>contraseña actual</strong> para confirmar que eres tú.', 'l'),
        ('nueva', 'Escribe la <strong>nueva contraseña</strong>: mínimo 8 caracteres, con al menos una letra y un número.', 'l'),
        ('confirmar', 'Repítela para evitar errores de tipeo.', 'l'),
        ('guardar', 'Pulsa <strong>Guardar contraseña</strong>.', 'tl'),
    ], 'Menú “Mi cuenta” → Cambiar contraseña.'), nivel=3),
    )

# ===================================================================================================================
sec('inventario', '3. Inventario',
    p('El inventario muestra qué hay en la tienda y en los almacenes. Hay tres cifras que debes distinguir:'),
    lista('<strong>Físico</strong>: lo que realmente está en los estantes.',
          '<strong>Reservado</strong>: lo que ya está comprometido para una venta (aún no salió).',
          '<strong>Libre</strong> = físico − reservado: lo que todavía puedes ofrecer o vender.',
          '<strong>Previsto</strong> (o “por recibir”): lo que viene en órdenes de compra pendientes. <em>No cuenta como stock hasta que se recibe.</em>'),
    seccion('disp', 'Disponibilidad',
            p('Es la pantalla de consulta rápida: cuánto hay de cada producto, por ubicación (Tienda, Almacén 1, Almacén 2).'),
            figura('03-disponibilidad', [
                ('buscar', 'Escribe el <strong>nombre o SKU</strong> del producto para filtrar la lista.', 'b'),
                ('todos', 'Pestaña <strong>Todos</strong>: todo el catálogo con stock.', 'bl'),
                ('alerta', 'Pestaña <strong>En alerta</strong>: solo los productos en el mínimo o críticos.', 'b'),
            ], 'Cada fila muestra Tienda, Almacén 1, Almacén 2, Reservado, Previsto y una barra de reparto.'),
            consejo('Si la tabla es más ancha que tu pantalla, desplázala hacia la derecha para ver todas las columnas.'),
            nivel=3),
    seccion('cat', 'Catálogo de productos',
            p('El catálogo es la lista maestra de productos. Cada producto tiene un <strong>SKU</strong> (código único), nombre, categoría, unidad de '
              'medida, stock mínimo y precio referencial.'),
            figura('04-catalogo', [
                ('nuevo', '<strong>Registrar producto</strong>: abre el formulario de alta.', 'bl'),
                ('buscar', 'Busca por <strong>nombre o SKU</strong>.', 'b'),
                ('categoria', 'Filtra por <strong>categoría</strong>.', 'b'),
                ('estado', 'Filtra por <strong>estado</strong> (activos o inactivos).', 'b'),
                ('fila', 'Haz clic en una fila para abrir la <strong>ficha</strong> del producto.', 'r'),
            ], 'Listado del catálogo.'),
            figura('05-ficha-producto', [
                ('modificar', '<strong>Modificar</strong>: edita nombre, categoría, unidad, mínimo, precio o estado (el SKU no cambia).', 'bl'),
                ('datos', 'Datos del producto: categoría, unidad de medida, stock mínimo y descripción.', 'r'),
                ('existencias', '<strong>Existencias</strong>: libre, físico y reservado, y una barra frente al mínimo.', 'r'),
                ('ubicaciones', 'Cuánto hay en <strong>cada ubicación</strong>.', 'tl'),
            ], 'Ficha de un producto.'),
            h := '<h4>Registrar un producto nuevo</h4>',
            figura('06-producto-nuevo', [
                ('sku', '<strong>SKU</strong>: formato <code>AAA-000</code> (por ejemplo <code>CEM-049</code>) o un código de <strong>5 dígitos</strong> '
                        '(por ejemplo <code>00123</code>).', 'r'),
                ('nombre', '<strong>Nombre oficial</strong> del producto.', 'r'),
                ('descripcion', '<strong>Descripción</strong> (opcional, hasta 300 caracteres).', 'r'),
                ('guardar', '<strong>Registrar producto</strong> cuando termines. Más abajo completas categoría, unidad, precio referencial y stock mínimo.', 'tl'),
                ('cancelar', '<strong>Cancelar</strong> descarta el formulario.', 'tl'),
            ], 'Formulario de alta de producto.'),
            ojo('Al <em>modificar</em> un producto el SKU queda bloqueado: no se puede cambiar. El stock mínimo es el valor con el que se calculan las alertas.'),
            nivel=3),
    seccion('alert', 'Alertas de stock',
            p('Muestra los productos cuyo stock <strong>libre</strong> llegó al mínimo o por debajo, del más grave al menos grave.'),
            figura('07-alertas', [
                ('tarjetas', 'Resumen: cuántos productos están <strong>críticos</strong>, <strong>en el mínimo</strong> y el <strong>total en alerta</strong>.', 'bl'),
                ('tabs', 'Filtra por <strong>Todas / Críticas / En el mínimo</strong>.', 'b'),
                ('buscar', 'Busca un producto por nombre o SKU.', 'b'),
                ('fila', 'Cada fila indica el stock libre, el mínimo, <strong>cuánto falta</strong> y una barra frente al mínimo.', 'l'),
                ('nivel', 'La última columna marca el <strong>nivel</strong> (crítico o mínimo); a la derecha hay un enlace “Ver disponibilidad”.', 'l'),
            ], 'Alertas de stock.'),
            lista('<strong>Crítico</strong>: no hay stock libre (o está muy por debajo del mínimo).',
                  '<strong>En el mínimo</strong>: llegó justo al límite que definiste.'),
            nivel=3),
    seccion('kardex', 'Movimientos y Kardex',
            p('El <strong>Kardex</strong> es el historial de todas las entradas y salidas de mercadería. Cada recepción, despacho o ajuste deja un movimiento que '
              '<strong>no se puede borrar</strong>: así siempre se sabe por qué cambió el stock.'),
            figura('08-kardex', [
                ('registrar', '<strong>Registrar movimiento</strong>: ajustes manuales y traslados entre almacenes.', 'bl'),
                ('filtros', '<strong>Filtros</strong>: SKU o producto, tipo (entrada/salida), origen (recepción, despacho, ajuste, traslado), ubicación y rango de fechas.', 'b'),
                ('tabla', 'Cada fila: fecha, SKU, producto, tipo, cantidad, origen y ubicación.', 'l'),
            ], 'Listado de movimientos del Kardex.'),
            consejo('Cuando registras una recepción, un despacho o un movimiento, el botón <em>“Ver movimientos en el Kardex”</em> te lleva al listado ya filtrado '
                    'con <strong>solo los movimientos de esa operación</strong>.'),
            h := '<h4>Registrar un movimiento manual</h4>',
            figura('09-movimiento', [
                ('tipo', '<strong>Tipo</strong>: <em>Entrada por ajuste</em> (suma stock), <em>Salida por merma o ajuste</em> (resta) o <em>Traslado interno</em> (mueve entre almacenes).', 'bl'),
                ('producto', '<strong>Producto</strong>: busca y elige. Al elegirlo verás sus existencias por ubicación.', 'r'),
                ('almacen', '<strong>Almacén</strong> donde ocurre el movimiento (en un traslado eliges origen y destino).', 'bl'),
                ('cantidad', '<strong>Cantidad</strong> (número entero). Una salida no puede superar lo que hay.', 'br'),
                ('motivo', '<strong>Motivo</strong>: obligatorio, mínimo 10 caracteres. Queda guardado en el Kardex.', 'r'),
                ('revisar', '<strong>Revisar movimiento</strong>: te muestra un resumen y pide confirmación antes de registrar.', 'tl'),
            ], 'Registrar un movimiento.'),
            ojo('Un traslado genera <strong>dos movimientos vinculados</strong> (una salida en el origen y una entrada en el destino). '
                'Después de confirmar no se puede deshacer: si te equivocas, registra el movimiento contrario con su motivo.'),
            nivel=3),
    )

# ===================================================================================================================
sec('recepcion', '4. Recepción de mercadería y documentos OCR',
    p('Cuando llega mercadería de un proveedor se registra una <strong>recepción</strong> contra una <strong>orden de compra</strong>. '
      'Al validarla, las entradas pasan al Kardex y el stock sube.'),
    seccion('recepciones', 'Lista de recepciones', figura('12-recepciones', [
        ('nueva', '<strong>Nueva recepción</strong>.', 'bl'),
        ('tabla', 'Cada recepción indica su <strong>orden de compra</strong>, el proveedor, el almacén donde se recibió y la fecha.', 'b'),
        ('estado', 'Estado de la recepción y <strong>estado de la orden</strong> (completada o recepción parcial).', 'l'),
    ], 'Recepciones registradas.'), nivel=3),
    seccion('recepcion-nueva', 'Registrar una recepción (3 pasos)',
            p('El asistente tiene tres pasos: <strong>1. Orden de compra</strong> → <strong>2. Documento</strong> → <strong>3. Conteo</strong>.'),
            h := '<h4>Paso 1: elegir la orden de compra</h4>',
            figura('13-recepcion-paso1', [
                ('pasos', 'Los <strong>tres pasos</strong> del asistente; el actual aparece resaltado.', 'b'),
                ('lista', 'Lista de órdenes <strong>pendientes o con recepción parcial</strong>: número, proveedor, cuántos productos y cuántas unidades faltan por recibir.', 'l'),
                ('elegir', 'Pulsa <strong>Elegir</strong> en la orden que estás recibiendo.', 'l'),
                ('continuar', 'Luego <strong>Continuar</strong> (se habilita al elegir una orden).', 'tl'),
            ], 'Paso 1.'),
            h2 := '<h4>Paso 2: documento (opcional)</h4>',
            p('Aquí puedes subir la <strong>guía del proveedor</strong> para que el sistema lea las cantidades con OCR, o continuar sin documento.'),
            figura('14-recepcion-paso2', [
                ('ubicacion', '<strong>Ubicación que recibe</strong>: Tienda, Almacén 1 o Almacén 2. Cámbiala para repartir la mercadería entre ubicaciones.', 'r'),
                ('aviso', 'Recuerda: <strong>la carga no modifica el inventario</strong>; el stock solo cambia al confirmar el conteo.', 'r'),
                ('foto', '<strong>Tomar foto</strong> de la guía con la cámara, o arrastra/selecciona un archivo (JPG, PNG o PDF de hasta 10 MB).', 'tr'),
                ('sinDoc', '<strong>Registrar sin documento</strong>: pasa directo al conteo.', 'tl'),
                ('usar', '<strong>Usar datos de la guía</strong>: precarga el conteo con lo que leyó el OCR (lo revisas siempre).', 'tl'),
            ], 'Paso 2.'),
            h3 := '<h4>Paso 3: conteo</h4>',
            figura('15-recepcion-paso3', [
                ('ubicacion', 'La <strong>ubicación que recibe</strong>. Si recibes en otra, cámbiala: la columna “Recibido en …” cambia con ella.', 'r'),
                ('todo', '<strong>Todo lo pedido llegó a [ubicación]</strong>: rellena de una vez todas las líneas con lo pendiente.', 'bl'),
                ('cantidad', '<strong>Recibido en [ubicación]</strong>: escribe lo que realmente llegó. Vacío cuenta como 0.', 'tr'),
                ('diferencia', 'La <strong>diferencia</strong> te avisa: “Faltan N” si llegó menos, “Igual al pedido” si coincide. '
                               'No se puede recibir <strong>más</strong> de lo pendiente.', 'l'),
                ('confirmar', '<strong>Confirmar recepción</strong>: se habilita con al menos una cantidad mayor que 0. Genera las entradas del Kardex.', 'tl'),
            ], 'Paso 3.'),
            ojo('Si recibes parcialmente, la orden queda en <em>Recepción parcial</em> y podrás registrar otra recepción más adelante con lo que falte. '
                'Cuando se completa todo, pasa a <em>Completada</em>.'),
            nivel=3),
    seccion('ocr', 'Documentos OCR',
            p('El <strong>OCR</strong> lee una foto o PDF y convierte las líneas en datos, para <strong>precargar</strong> una recepción, una cotización o una venta. '
              'Siempre lo revisas tú antes de usarlo: <em>no modifica inventario, ventas ni Kardex por sí solo</em>.'),
            figura('16-documentos', [
                ('procesar', '<strong>Procesar documento</strong>: sube un documento nuevo.', 'bl'),
                ('estados', 'Los documentos se agrupan por estado: <strong>Pendiente</strong>, <strong>Procesado por OCR</strong> y <strong>Validado</strong>.', 'b'),
                ('tabla', 'Aquí aparecen cuando existan; desde cada fila puedes abrir la <strong>revisión</strong>.', 'b'),
            ], 'Bandeja de documentos.'),
            figura('17-ocr-procesar', [
                ('tipos', 'Elige el <strong>tipo de documento</strong>: Guía de proveedor, Cotización de cliente u Orden de compra.', 'br'),
                ('foto', '<strong>Tomar foto</strong> con la cámara del equipo.', 'r'),
                ('soltar', 'O <strong>arrastra</strong> el archivo aquí, o haz clic para seleccionarlo (JPG, PNG o PDF de hasta 10 MB).', 'r'),
                ('aviso', 'Aviso: el procesamiento <strong>no modifica el inventario</strong>.', 'bl'),
            ], 'Procesar un documento.'),
            h4 := '<h4>Revisar lo leído</h4>',
            p('Tras procesar, el sistema te lleva a una <strong>pantalla de revisión</strong> con las líneas detectadas (producto, cantidad, precio). '
              'Corrige lo que haga falta —el OCR puede equivocarse en letras y números parecidos— y confirma. Si el producto leído coincide con uno del catálogo, '
              'se sugiere automáticamente; si hay varios parecidos, eliges.'),
            lista('Si el mismo producto aparece <strong>repetido</strong>, el sistema suma sus cantidades en una sola línea.',
                  'El <strong>precio debe ser mayor que 0</strong> para poder usar la línea en una cotización o venta.'),
            nivel=3),
    )

# ===================================================================================================================
sec('despacho', '5. Despachos',
    p('Un <strong>despacho</strong> es la salida física de mercadería de una venta. Se gestiona con una <strong>orden de despacho</strong> (la guía que acompaña la salida).'),
    flujo('Venta lista', 'Generar orden de despacho', 'Orden emitida', 'Confirmar salida', 'Salida confirmada', 'Registrar entrega', 'Entregada'),
    figura('10-despachos', [
        ('estados', 'Pestañas por estado: <strong>Pendientes de despacho</strong>, <strong>En preparación</strong>, <strong>Salida confirmada</strong> y <strong>Entregadas</strong>.', 'bl'),
        ('buscar', 'Busca por número de orden, venta o cliente.', 'b'),
        ('listas', '<strong>Ventas listas para generar orden</strong>: ventas con todo su stock reservado y aún sin orden.', 'tl'),
        ('generar', '<strong>Generar orden de despacho</strong> de esa venta (pide confirmación).', 'tl'),
    ], 'Pantalla de Despachos.'),
    figura('11-despacho-detalle', [
        ('estado', 'El <strong>estado</strong> de la orden: aquí, “Salida confirmada”.', 'r'),
        ('imprimir', '<strong>Imprimir orden</strong>: abre la hoja imprimible de la orden (con firmas de quien despacha, chofer y quien recibe).', 'b'),
        ('entrega', '<strong>Registrar entrega</strong>: cuando el cliente ya recibió, la orden pasa a “Entregada”.', 'bl'),
        ('productos', 'Productos a despachar: cantidad a despachar y cantidad <strong>reservada</strong>.', 'r'),
    ], 'Detalle de una orden de despacho.'),
    p('<strong>Confirmar salida</strong> (cuando la orden está recién emitida): el sistema pide confirmación, registra las <strong>salidas en el Kardex</strong> y consume la reserva. '
      'Solo se puede confirmar si todas las cantidades están reservadas.'),
    ojo('Confirmar la salida <strong>no se puede deshacer</strong>. Hasta ese momento, la venta todavía se puede cancelar.'),
    consejo('Si necesitas volver a imprimir una orden ya generada, el sistema te pide un <strong>motivo de reimpresión</strong> para dejar constancia.'),
    )

# ===================================================================================================================
sec('comercial', '6. Comercial: clientes, cotizaciones y ventas',
    flujo('Cotización emitida', 'Aceptada por el cliente', 'Validada por MIGAL', 'Convertir en venta', 'Despacho'),
    seccion('clientes', 'Clientes', figura('25-clientes', [
        ('registrar', '<strong>Registrar cliente</strong> (nombre o razón social, documento, teléfono...).', 'bl'),
        ('tabs', 'Pestañas <strong>Activos</strong> / <strong>Inactivos</strong>.', 'b'),
        ('buscar', 'Busca por nombre o documento.', 'b'),
        ('tabla', 'Lista de clientes con su documento, teléfono y estado.', 'l'),
    ], 'Listado de clientes.'),
        p('El “Cliente de mostrador” se usa para ventas rápidas sin cliente identificado.'), nivel=3),
    seccion('cotizaciones', 'Cotizaciones',
            p('Una cotización es la <strong>propuesta de precios</strong> que se entrega al cliente. <strong>No reserva ni descuenta stock</strong>: el stock recién se reserva '
              'cuando la cotización se convierte en venta.'),
            figura('18-cotizaciones', [
                ('nueva', '<strong>Nueva cotización</strong>.', 'bl'),
                ('tabs', 'Filtra por estado: <strong>Emitidas, Aceptadas, Rechazadas, Canceladas, Vencidas</strong>. El número es la cantidad en cada una.', 'b'),
                ('buscar', 'Busca por <strong>número o cliente</strong>.', 'b'),
                ('fila', 'Haz clic en una fila para ver el <strong>detalle</strong>.', 'r'),
                ('estado', 'La columna <strong>Estado</strong> usa colores: azul (emitida), verde (validada), rojo (rechazada), gris (vencida).', 'l'),
            ], 'Listado de cotizaciones.'),
            h := '<h4>Crear una cotización</h4>',
            figura('21-cotizacion-nueva', [
                ('cliente', '<strong>Cliente</strong>: busca por nombre o documento. Los clientes inactivos no se pueden elegir.', 'tr'),
                ('cargar', '<strong>Cargar desde un documento</strong>: lee una nota o cotización escaneada con OCR y precarga los productos.', 'bl'),
                ('lineas', 'Tabla de <strong>productos</strong>: elige el producto, la cantidad y el precio (se precarga el precio referencial). Agrega más líneas con “Agregar producto”.', 'tl'),
                ('descuento', '<strong>Descuento</strong> global en soles (no puede superar el subtotal).', 'tl'),
                ('emitir', '<strong>Emitir cotización</strong>: se habilita cuando hay al menos un producto.', 'tl'),
            ], 'Formulario de nueva cotización.'),
            pasos('Elige el <strong>cliente</strong> (o continúa sin cliente).',
                  'Agrega los <strong>productos</strong> con su cantidad y precio (mayor que 0).',
                  'Mientras armas la cotización verás el cuadro <strong>“Stock para decidir”</strong> (se explica más abajo).',
                  'Completa <strong>validez</strong> (1 a 60 días; por defecto 7), <strong>forma de pago</strong> y observaciones.',
                  'Pulsa <strong>Emitir cotización</strong> y confirma.'),
            h2 := '<h4>Detalle y ciclo de vida de la cotización</h4>',
            figura('19-cotizacion-detalle', [
                ('imprimir', '<strong>Imprimir</strong>: abre la cotización lista para entregar al cliente o guardar en PDF.', 'bl'),
                ('modificar', '<strong>Modificar</strong>: se puede mientras la cotización no tenga venta (ver reglas abajo).', 'b'),
                ('cancelar', '<strong>Cancelar cotización</strong>: para las ya aceptadas o validadas.', 'b'),
                ('validar', '<strong>Validar viabilidad</strong> (Administrador): MIGAL confirma que puede cumplir la propuesta.', 'br'),
                ('pasos', 'El <strong>ciclo</strong>: Emitida → Aceptada → Validada → Venta. El paso actual está marcado.', 'b'),
                ('productos', 'Productos cotizados con cantidad, precio e importe, y los totales.', 'r'),
                ('cliente', 'Cliente y condiciones de la cotización.', 'l'),
            ], 'Detalle de una cotización aceptada por el cliente.'),
            tabla(['Estado', 'Qué significa', 'Qué puedes hacer'], [
                ['<strong>Emitida</strong>', 'Entregada al cliente, esperando respuesta.', 'Modificar · Registrar aceptación · Registrar rechazo'],
                ['<strong>Aceptada por el cliente</strong>', 'El cliente dijo que sí.', 'Validar (Administrador) · Modificar · Cancelar'],
                ['<strong>Validada por MIGAL</strong>', 'MIGAL confirmó que puede cumplir.', 'Convertir en venta · Modificar · Cancelar'],
                ['<strong>Rechazada</strong>', 'El cliente dijo que no (se registra el motivo).', 'Duplicar'],
                ['<strong>Cancelada</strong>', 'Se dejó sin efecto después de aceptada o validada (con motivo).', 'Duplicar'],
                ['<strong>Vencida</strong>', 'Pasó su fecha de vigencia sin aceptarse (se cierra sola).', 'Duplicar'],
            ]),
            h3 := '<h4>Stock para decidir</h4>',
            p('Para que el vendedor sepa <strong>qué puede prometer</strong>, el detalle (y el formulario, mientras armas la cotización) muestra por cada producto:'),
            figura('20-stock-para-decidir', [
                ('titulo', 'Tarjeta <strong>“Stock para decidir”</strong>.', 'l'),
                ('libre', '<strong>Stock libre</strong>: lo que hay disponible hoy (físico − reservado).', 't'),
                ('reservado', '<strong>Reservado</strong>: comprometido para otras ventas.', 't'),
                ('porRecibir', '<strong>Por recibir</strong>: lo pedido a proveedores, con la <strong>próxima fecha de llegada</strong> estimada.', 't'),
                ('previsto', '<strong>Previsto neto</strong>: lo por recibir <em>menos</em> lo que otras ventas pendientes ya reclaman. '
                             'Si se descuenta algo, aparece el aviso “Otras ventas compiten por este stock”.', 't'),
                ('cobertura', '<strong>Cobertura</strong>: <span style="color:#157347;font-weight:700">Cubierto con stock</span>, '
                              '<span style="color:#b45309;font-weight:700">Cubierto con lo por recibir</span> o '
                              '<span style="color:#c92a2a;font-weight:700">Falta comprar</span>.', 'l'),
            ], 'Tarjeta “Stock para decidir”.'),
            ojo('Es solo <strong>informativo</strong>: la cotización no reserva nada. Dos cotizaciones pueden mostrar el mismo stock libre; quien convierta primero en venta se lo lleva.'),
            h4 := '<h4>Modificar una cotización aceptada o validada</h4>',
            p('Una cotización <strong>emitida</strong> se edita libremente. Una <strong>aceptada o validada sin venta</strong> también se puede modificar, con estas reglas:'),
            lista('Al guardar, <strong>vuelve a “Emitida”</strong>: el cliente debe aceptarla de nuevo y, si estaba validada, el Administrador debe validarla otra vez.',
                  'Es <strong>obligatorio un motivo del cambio</strong> (mínimo 10 caracteres). Queda en el <strong>historial</strong> con quién lo hizo y cuándo.',
                  'La <strong>vigencia</strong> se mantiene, salvo que cambies los días de validez en el mismo formulario.',
                  'Si ya tiene <strong>venta</strong>, no se modifica: hay que cancelar la venta y emitir una cotización nueva.'),
            figura('37-cotizacion-editar-aviso', [
                ('aviso', 'Aviso amarillo: <strong>“Al guardar, la cotización vuelve a Emitida”</strong>.', 'b'),
                ('cliente', 'Puedes cambiar el cliente.', 'r'),
                ('lineas', 'Y los productos, cantidades y precios.', 'tr'),
                ('guardar', '<strong>Guardar cambios</strong>.', 'tl'),
            ], 'Editar una cotización aceptada: parte superior.'),
            figura('36-cotizacion-editar-motivo', [
                ('stock', 'El cuadro <strong>Stock para decidir</strong> se actualiza según lo que cambies.', 'bl'),
                ('motivo', '<strong>Motivo del cambio</strong> (obligatorio, mínimo 10 caracteres).', 'r'),
                ('guardar', '<strong>Guardar cambios</strong> aplica el cambio y deja constancia.', 'tl'),
            ], 'Editar una cotización aceptada: motivo del cambio.'),
            h5 := '<h4>Cancelar una cotización</h4>',
            figura('38-cotizacion-cancelar', [
                ('origen', '<strong>¿Quién desiste?</strong> “El cliente” (lo registra Vendedor o Administrador) o “MIGAL” (solo Administrador).', 'l'),
                ('motivo', '<strong>Motivo</strong> obligatorio.', 'l'),
                ('confirmar', '<strong>Cancelar cotización</strong>: queda en estado Cancelada. No se puede reactivar, pero se puede <em>duplicar</em>.', 'tl'),
            ], 'Diálogo para cancelar.'),
            lista('<strong>Emitida</strong> → se <em>rechaza</em> con “Registrar rechazo” (el cliente dijo que no).',
                  '<strong>Aceptada/validada sin venta</strong> → se <em>cancela</em>.',
                  '<strong>Con venta</strong> → se cancela la <em>venta</em> (posible hasta que el almacén confirme la salida); al hacerlo, la cotización de origen también queda cancelada.'),
            consejo('<strong>Duplicar cotización</strong> (botón que aparece en las rechazadas, canceladas y vencidas) crea una <em>nueva cotización emitida</em> con el mismo cliente, '
                    'productos y condiciones, por si el cliente vuelve.'),
            h6 := '<h4>Imprimir o guardar en PDF</h4>',
            figura('35-impresion', [
                ('imprimir', '<strong>Imprimir</strong>: abre el diálogo del navegador. Allí elige tu impresora o <strong>“Guardar como PDF”</strong>.', 'bl'),
                ('volver', '<strong>Volver</strong> a la pantalla anterior.', 'b'),
                ('hoja', 'La <strong>hoja A4</strong> con el logo, los datos, la tabla de productos y los totales. Sin menú ni botones.', 'l'),
                ('datos', 'Datos del documento: emisión, vigencia, cliente, estado y condiciones.', 'r'),
            ], 'Cotización lista para imprimir.'),
            p('El mismo botón <strong>Imprimir</strong> existe en el detalle de la <strong>venta</strong>, de la <strong>orden de compra</strong> y de la <strong>orden de despacho</strong>.'),
            nivel=3),
    seccion('ventas', 'Ventas',
            p('Una venta compromete el stock: al registrarla, el sistema <strong>reserva</strong> lo que hay libre para esa venta.'),
            figura('22-ventas', [
                ('registrar', '<strong>Registrar venta</strong>.', 'bl'),
                ('tabs', 'Estados: <strong>Pendiente de abastecimiento</strong>, <strong>Pendiente de despacho</strong>, <strong>Lista para despacho</strong>, <strong>Despachada</strong> y <strong>Cancelada</strong>.', 'b'),
                ('buscar', 'Busca por número o cliente.', 'b'),
                ('entrega', 'Columna <strong>Entrega</strong>: “Completa” o “Parcial” (con cuántas unidades quedan <strong>pendientes</strong>).', 'tl'),
                ('fila', 'Haz clic en una venta para abrir su detalle.', 'r'),
            ], 'Listado de ventas.'),
            tabla(['Estado de la venta', 'Qué significa'], [
                ['<strong>Pendiente de abastecimiento</strong>', 'Faltan productos: parte de lo vendido aún no tiene stock reservado.'],
                ['<strong>Pendiente de despacho</strong>', 'Todo el stock está reservado; falta generar la orden de despacho.'],
                ['<strong>Lista para despacho</strong>', 'Ya tiene orden de despacho emitida.'],
                ['<strong>Despachada</strong>', 'La mercadería salió del almacén (estado final).'],
                ['<strong>Cancelada</strong>', 'La venta se anuló y su reserva se liberó (estado final).'],
            ]),
            h := '<h4>Registrar una venta</h4>',
            figura('24-venta-nueva', [
                ('cliente', '<strong>Cliente</strong>: búscalo por nombre o documento.', 'tr'),
                ('nuevoCliente', '<strong>Registrar cliente nuevo</strong> sin salir de esta pantalla.', 'bl'),
                ('origen', '<strong>Origen de los productos</strong>: <em>Manual</em>, <em>Desde una cotización aceptada</em> (carga cliente y productos, y ya no se pueden cambiar) o '
                           '<em>Desde un documento</em> (OCR).', 'r'),
                ('fecha', '<strong>Fecha de entrega requerida</strong>: <strong>obligatoria</strong>. Es cuándo el cliente necesita los productos; sirve para planificar el abastecimiento.', 'tr'),
                ('confirmar', '<strong>Confirmar venta</strong>: se habilita con al menos un producto y la fecha de entrega.', 'tl'),
            ], 'Formulario de nueva venta.'),
            consejo('Para convertir una cotización en venta, entra al detalle de la cotización <strong>validada</strong> y pulsa <strong>Convertir en venta</strong>: '
                    'abre este formulario ya precargado.'),
            h2 := '<h4>Detalle de una venta</h4>',
            figura('23-venta-detalle', [
                ('estado', 'Estado de la venta.', 'bl'),
                ('imprimir', '<strong>Imprimir</strong> la venta.', 'bl'),
                ('productos', 'Para cada producto: <strong>solicitada</strong>, <strong>atendida</strong> (con stock) y <strong>pendiente</strong>, más los totales.', 'r'),
                ('pendiente', 'Aviso de <strong>cantidades pendientes</strong>: cuánto se puede atender hoy y cuánto falta abastecer.', 'tl'),
                ('cancelar', '<strong>Cancelar venta</strong>: libera lo reservado (pide un motivo). Se puede hasta que el almacén confirme la salida.', 'tl'),
                ('completar', '<strong>Completar venta</strong>: intenta reservar ahora el stock que ya llegó para lo que estaba pendiente.', 'tl'),
            ], 'Detalle de una venta con cantidades pendientes.'),
            ojo('Cuando una venta queda <em>Pendiente de abastecimiento</em>, lo que falta aparece en la pestaña <strong>“Faltantes de ventas”</strong> de Órdenes de compra, '
                'para que el equipo de compras sepa qué pedir.'),
            nivel=3),
    )

# ===================================================================================================================
sec('abastecimiento', '7. Abastecimiento: proveedores y órdenes de compra',
    seccion('proveedores', 'Proveedores', figura('26-proveedores', [
        ('registrar', '<strong>Registrar proveedor</strong> (razón social, RUC, contacto y los productos que suministra).', 'bl'),
        ('tabs', 'Pestañas <strong>Todos / Activos / Inactivos</strong>.', 'b'),
        ('buscar', 'Busca por razón social o RUC.', 'b'),
        ('productos', 'La columna <strong>Productos</strong> indica cuántos productos del catálogo suministra cada proveedor.', 'tl'),
    ], 'Listado de proveedores.'),
        p('Un proveedor <strong>inactivo</strong> no acepta órdenes de compra nuevas.'), nivel=3),
    seccion('compras', 'Órdenes de compra',
            figura('27-compras', [
                ('nueva', '<strong>Nueva orden de compra</strong>.', 'bl'),
                ('tabs', 'Estados: <strong>Pendientes</strong>, <strong>Recepción parcial</strong>, <strong>Completadas</strong>.', 'b'),
                ('faltantes', 'Pestaña <strong>Faltantes de ventas</strong>: lo que hace falta comprar para atender ventas pendientes.', 'b'),
                ('buscar', 'Busca por número de orden o proveedor.', 'b'),
                ('avance', '<strong>Avance de recepción</strong>: cuántas unidades han llegado de las pedidas.', 'tl'),
            ], 'Listado de órdenes de compra.'),
            h := '<h4>Faltantes de ventas</h4>',
            p('Esta pestaña ayuda a decidir <strong>qué comprar</strong>. Puedes verla <em>por venta</em> o <em>por producto</em>, y filtrar por fecha de entrega.'),
            figura('28-faltantes', [
                ('pestana', 'Pestaña <strong>Faltantes de ventas</strong> con el número de ventas afectadas.', 'b'),
                ('vistas', 'Cambia entre <strong>Por venta</strong> y <strong>Por producto</strong> (suma lo que falta de cada producto entre todas las ventas).', 'b'),
                ('fecha', 'Filtra por <strong>fecha de entrega</strong> requerida por el cliente.', 'bl'),
                ('venta', 'Cada bloque es una venta: cliente, cotización de origen, y cuándo se necesita (“en 5 d”).', 'tl'),
                ('tabla', 'Los productos y las <strong>unidades que faltan</strong>.', 'r'),
            ], 'Faltantes de ventas pendientes de abastecimiento.'),
            h2 := '<h4>Crear una orden de compra</h4>',
            figura('29-compra-nueva', [
                ('proveedor', '<strong>Proveedor</strong>: los que suministran los productos aparecen primero al agregar líneas. Los inactivos no se pueden elegir.', 'tr'),
                ('fecha', '<strong>Fecha de la orden</strong> (no puede ser anterior a hoy).', 'r'),
                ('estimada', '<strong>Fecha estimada de llegada</strong> (opcional): es la que ve el vendedor como “próxima llegada” en <em>Stock para decidir</em>.', 'br'),
                ('faltantes', 'Panel de <strong>faltantes</strong> a la vista mientras compras, para no olvidar nada.', 'tl'),
                ('guardar', '<strong>Guardar orden de compra</strong>: se habilita con proveedor y al menos un producto.', 'tl'),
            ], 'Formulario de nueva orden de compra.'),
            pasos('Elige el <strong>proveedor</strong>.', 'Indica la <strong>fecha estimada de llegada</strong> (recomendado).',
                  'Mira los <strong>faltantes</strong> y agrega los productos con cantidad y precio de compra.',
                  'Guarda: la orden queda <strong>Pendiente</strong> hasta recibirla en <a href="#recepcion">Recepciones</a>.'),
            nivel=3),
    )

# ===================================================================================================================
sec('asistente', '8. Agente de IA',
    p('El agente responde <strong>preguntas en lenguaje natural</strong> sobre inventario, productos y ubicación de mercadería. <strong>Solo consulta</strong>: no modifica datos, '
      'y respeta los permisos de tu rol.'),
    figura('30-agente', [
        ('rol', 'Insignia con tu <strong>rol</strong>: el agente responde según tus permisos.', 'r'),
        ('respuesta', 'La <strong>respuesta</strong> del agente, indicando qué consultó (por ejemplo “Consultó las alertas de stock”).', 'l'),
        ('sugerencias', '<strong>Preguntas sugeridas</strong>: haz clic para enviarlas.', 'tr'),
        ('caja', 'Escribe tu <strong>pregunta</strong> aquí.', 'l'),
        ('enviar', '<strong>Enviar consulta</strong> (o Enter).', 'r'),
    ], 'Conversación con el agente.'),
    p('Ejemplos de preguntas útiles:'),
    lista('“¿Qué productos están en stock crítico?”', '“¿Cuánto hay de ladrillo King Kong?”', '“¿Dónde está el cemento?” (ubicación por almacén)'),
    consejo('Las preguntas simples (alertas, disponibilidad de un producto) se responden al instante. Las más complejas pueden tardar unos segundos. '
            'Si el servicio de IA está saturado, el sistema reintenta y te avisa con claridad.'),
    )

# ===================================================================================================================
sec('admin', '9. Administración de usuarios',
    p('Solo el <strong>Administrador</strong> gestiona usuarios y roles.'),
    figura('31-usuarios', [
        ('registrar', '<strong>Registrar usuario</strong>.', 'bl'),
        ('tabs', 'Pestañas <strong>Todos / Activos / Inactivos</strong>.', 'b'),
        ('rol', 'El <strong>rol</strong> de cada persona.', 'b'),
        ('acciones', '<strong>Modificar rol</strong> y <strong>Revocar acceso</strong>. No puedes revocar tu propia cuenta.', 'tl'),
    ], 'Usuarios y roles.'),
    figura('32-usuario-nuevo', [
        ('nombres', '<strong>Nombres y apellidos</strong> de la persona.', 'r'),
        ('usuario', '<strong>Usuario</strong>: minúsculas, sin espacios, de 4 a 20 caracteres.', 'r'),
        ('clave', '<strong>Contraseña temporal</strong>: mínimo 8 caracteres, con una letra y un número. Se recomienda que la persona la cambie en su primer ingreso.', 'r'),
        ('registrar', '<strong>Registrar usuario</strong>. Antes eliges también el <strong>rol</strong> (Administrador, Vendedor, Almacenero o Reponedor).', 'tl'),
    ], 'Alta de usuario.'),
    ojo('<strong>Revocar acceso</strong> deshabilita al usuario sin borrar su historial: sus operaciones pasadas siguen en el sistema.'),
    )

# ===================================================================================================================
sec('flujos', '10. Flujos completos, paso a paso',
    p('Así se encadenan los módulos en situaciones reales.'),
    seccion('flujo-venta', 'De la cotización a la entrega', flujo('Cotizar', 'Cliente acepta', 'Admin valida', 'Convertir en venta', 'Generar orden', 'Confirmar salida', 'Entregar'),
            pasos('<strong>Vendedor</strong>: crea la <a href="#cotizaciones">cotización</a>, revisa “Stock para decidir” y la emite. Puede imprimirla para el cliente.',
                  '<strong>Vendedor</strong>: cuando el cliente acepta, abre la cotización y pulsa <strong>Registrar aceptación</strong>.',
                  '<strong>Administrador</strong>: abre la cotización aceptada y pulsa <strong>Validar viabilidad</strong>.',
                  '<strong>Vendedor</strong>: pulsa <strong>Convertir en venta</strong>, indica la <strong>fecha de entrega requerida</strong> y confirma. El stock libre queda reservado.',
                  'Si algo faltaba, la venta queda <em>Pendiente de abastecimiento</em> hasta que llegue mercadería (ver el flujo de reposición).',
                  '<strong>Almacenero</strong>: en <em>Despachos</em> genera la <strong>orden de despacho</strong>, la imprime, prepara la mercadería y <strong>confirma la salida</strong>.',
                  '<strong>Almacenero</strong>: cuando el cliente recibe, pulsa <strong>Registrar entrega</strong>. La venta queda <em>Despachada</em>.'), nivel=3),
    seccion('flujo-repo', 'Reponer lo que falta', flujo('Alerta o faltante', 'Orden de compra', 'Llega mercadería', 'Recepción', 'Venta se completa'),
            pasos('<strong>Reponedor</strong>: revisa <em>Alertas de stock</em> y la pestaña <em>Faltantes de ventas</em>.',
                  'Crea una <strong>orden de compra</strong> con la fecha estimada de llegada.',
                  'Cuando llega la mercadería, el <strong>Almacenero</strong> registra la <strong>recepción</strong> contra esa orden, contando lo recibido por ubicación.',
                  'Al confirmar, el stock sube y el Kardex registra las entradas.',
                  'En la venta pendiente, pulsa <strong>Completar venta</strong> para reservar lo que ya llegó.'), nivel=3),
    seccion('flujo-cambio', 'El cliente cambia de opinión',
            tabla(['Situación', 'Qué hacer'], [
                ['La cotización aún está <em>Emitida</em> y el cliente dice que no', '<strong>Registrar rechazo</strong> con el motivo.'],
                ['Ya la había aceptado (o estaba validada) y desiste', '<strong>Cancelar cotización</strong> eligiendo quién desiste y el motivo.'],
                ['Ya es una venta, pero la mercadería no salió', '<strong>Cancelar venta</strong> con el motivo: se libera la reserva y la cotización queda cancelada.'],
                ['Quiere cambiar cantidades o precios', '<strong>Modificar</strong> la cotización (vuelve a Emitida y pide motivo) o, si ya es venta, cancelar y emitir una nueva.'],
                ['El cliente vuelve tras un rechazo, cancelación o vencimiento', '<strong>Duplicar cotización</strong>.'],
                ['La mercadería ya salió', 'No se cancela desde el sistema: las devoluciones se gestionan aparte.'],
            ]), nivel=3),
    )

# ===================================================================================================================
sec('ayuda', '11. Ayuda rápida',
    seccion('glosario', 'Glosario', tabla(['Término', 'Significado'], [
        ['SKU', 'Código único de un producto (por ejemplo <code>CEM-049</code> o <code>00123</code>).'],
        ['Stock físico / reservado / libre', 'Lo que hay en estantes / lo comprometido a ventas / lo que se puede vender (físico − reservado).'],
        ['Previsto o por recibir', 'Lo pedido a proveedores que aún no llega. No es stock.'],
        ['Previsto neto', 'Por recibir menos lo que otras ventas pendientes ya reclaman.'],
        ['Kardex', 'Historial inmutable de entradas y salidas de mercadería.'],
        ['OCR', 'Lectura automática de un documento escaneado; siempre se revisa antes de usarlo.'],
        ['Faltante', 'Cantidad vendida que todavía no tiene stock reservado.'],
        ['Validar viabilidad', 'Confirmación de MIGAL de que puede cumplir una cotización aceptada.'],
    ]), nivel=3),
    seccion('faq', 'Preguntas frecuentes',
            '<details><summary>No veo un botón que otra persona sí ve.</summary><p>Los botones dependen del rol. Revisa la tabla de roles en la sección 2 o pide al Administrador que ajuste tu rol.</p></details>'
            '<details><summary>El botón para guardar o confirmar está gris.</summary><p>Falta algún dato obligatorio. Mira el mensaje a la izquierda del botón (por ejemplo “Agregar al menos un producto”).</p></details>'
            '<details><summary>No puedo recibir más de lo pedido.</summary><p>Es una regla del sistema: la recepción tiene un tope por línea igual a lo pendiente. Si llegó de más, registra solo lo pedido y gestiona el sobrante con el proveedor.</p></details>'
            '<details><summary>Cometí un error en un movimiento de Kardex.</summary><p>Los movimientos no se editan ni se borran. Registra un movimiento contrario con un motivo claro.</p></details>'
            '<details><summary>No puedo modificar una cotización.</summary><p>Si ya tiene una venta, no se modifica: cancela la venta y emite una cotización nueva. Si está rechazada, cancelada o vencida, usa <em>Duplicar cotización</em>.</p></details>'
            '<details><summary>La pantalla se ve cortada o muy pequeña.</summary><p>Actualiza la página con Ctrl+F5 o ajusta el zoom del navegador.</p></details>'
            '<details><summary>El Agente de IA tarda o dice que está saturado.</summary><p>Reintenta en unos segundos; las preguntas simples (alertas y disponibilidad) se responden sin depender del servicio externo.</p></details>',
            nivel=3),
    )


def main():
    import re
    IDX.extend((f'h{n}', i, t) for i, n, t in re.findall(r'<section id="([^"]+)"><h([23])>(.*?)</h\2>', ''.join(S)))
    salida = construir(S, IDX, 'Guía de uso de MIGAL SmartOps',
                       'Ferretería Progresol Migal · Inventario, comercial, despachos y abastecimiento. Paso a paso, con capturas reales y flechas.')
    ruta = os.path.join(BASE, 'guia-de-uso.html')
    open(ruta, 'w', encoding='utf-8').write(salida)
    print(ruta, round(os.path.getsize(ruta) / 1e6, 2), 'MB')


if __name__ == '__main__':
    main()
