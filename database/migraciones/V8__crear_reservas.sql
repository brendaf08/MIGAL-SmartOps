-- Migración V8: tabla reserva_stock, disparadores de restricción y vistas de disponibilidad.
-- Registra las cantidades de stock comprometidas por cada línea de venta desde una ubicación determinada (ADR-003 y ADR-005).
-- Las claves foráneas compuestas que incluyen id_producto garantizan que cada reserva corresponde al producto de su línea.
-- Los disparadores diferibles impiden que la suma de reservas activas supere la cantidad física del inventario
-- o la cantidad vendida de la línea. Las cantidades libres y pendientes se obtienen mediante vistas y no se almacenan.
-- La transición de estado de cada reserva se verifica en la capa de negocio.

CREATE TABLE reserva_stock (
    id_reserva             bigint        GENERATED ALWAYS AS IDENTITY,
    id_detalle_venta       bigint        NOT NULL,
    id_inventario          bigint        NOT NULL,
    id_producto            bigint        NOT NULL,
    cantidad_reservada     numeric(14,3) NOT NULL,
    estado                 varchar(20)   NOT NULL,
    fecha_reserva          timestamptz   NOT NULL DEFAULT now(),
    fecha_cierre           timestamptz,
    id_usuario_responsable bigint        NOT NULL,

    CONSTRAINT pk_reserva_stock PRIMARY KEY (id_reserva),
    -- La reserva corresponde al producto de la línea de venta que la origina.
    CONSTRAINT fk_reserva_stock_detalle_venta_producto FOREIGN KEY (id_detalle_venta, id_producto)
        REFERENCES detalle_venta (id_detalle_venta, id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- La reserva se origina en un inventario del mismo producto.
    CONSTRAINT fk_reserva_stock_inventario_producto FOREIGN KEY (id_inventario, id_producto)
        REFERENCES inventario (id_inventario, id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reserva_stock_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_reserva_stock_cantidad CHECK (cantidad_reservada > 0),
    -- Estados de la reserva: activa, liberada o consumida.
    CONSTRAINT ck_reserva_stock_estado CHECK (estado IN ('activa', 'liberada', 'consumida')),
    -- Condicional: fecha_cierre es obligatoria en los estados finales y nula mientras la reserva está activa.
    CONSTRAINT ck_reserva_stock_fecha_cierre CHECK (
        (estado = 'activa' AND fecha_cierre IS NULL)
        OR
        (estado <> 'activa' AND fecha_cierre IS NOT NULL)
    )
);

-- Índices de las claves foráneas compuestas. La columna líder de cada una es id_detalle_venta e id_inventario.
CREATE INDEX ix_reserva_stock_id_detalle_venta_producto ON reserva_stock (id_detalle_venta, id_producto);
CREATE INDEX ix_reserva_stock_id_inventario_producto ON reserva_stock (id_inventario, id_producto);
CREATE INDEX ix_reserva_stock_id_usuario_responsable ON reserva_stock (id_usuario_responsable);
-- Índices parciales sobre las reservas activas, utilizados por las sumas de disponibilidad y por los disparadores.
CREATE INDEX ix_reserva_stock_activa_inventario ON reserva_stock (id_inventario) WHERE estado = 'activa';
CREATE INDEX ix_reserva_stock_activa_detalle_venta ON reserva_stock (id_detalle_venta) WHERE estado = 'activa';

-- Verifica que la suma de reservas activas de un inventario no supere su cantidad física.
-- Se invoca desde reserva_stock (al insertar o actualizar) y desde inventario (al modificar la cantidad física).
CREATE FUNCTION fn_verificar_reservas_inventario() RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_cantidad_fisica numeric(14,3);
    v_cantidad_activa numeric(14,3);
BEGIN
    -- Bloquea la fila del inventario antes de sumar: serializa las verificaciones de transacciones concurrentes sin bloquear las inserciones que citan la fila,
    -- que de otro modo no verían las reservas aún no confirmadas de la otra transacción.
    PERFORM 1 FROM inventario WHERE id_inventario = NEW.id_inventario FOR NO KEY UPDATE;

    SELECT i.cantidad_fisica, COALESCE(SUM(r.cantidad_reservada), 0)
      INTO v_cantidad_fisica, v_cantidad_activa
      FROM inventario i
      LEFT JOIN reserva_stock r ON r.id_inventario = i.id_inventario AND r.estado = 'activa'
     WHERE i.id_inventario = NEW.id_inventario
     GROUP BY i.cantidad_fisica;

    IF v_cantidad_activa > v_cantidad_fisica THEN
        RAISE EXCEPTION 'las reservas activas del inventario % (%) superan la cantidad fisica (%)',
            NEW.id_inventario, v_cantidad_activa, v_cantidad_fisica
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

-- Verifica que la suma de reservas activas de una línea de venta no supere su cantidad vendida.
-- Se invoca desde reserva_stock (al insertar o actualizar) y desde detalle_venta (al modificar la cantidad vendida).
CREATE FUNCTION fn_verificar_reservas_detalle_venta() RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_cantidad_vendida numeric(14,3);
    v_cantidad_activa  numeric(14,3);
BEGIN
    -- Bloquea la fila de la línea antes de sumar, por la misma razón que en la verificación del inventario.
    PERFORM 1 FROM detalle_venta WHERE id_detalle_venta = NEW.id_detalle_venta FOR NO KEY UPDATE;

    SELECT d.cantidad_vendida, COALESCE(SUM(r.cantidad_reservada), 0)
      INTO v_cantidad_vendida, v_cantidad_activa
      FROM detalle_venta d
      LEFT JOIN reserva_stock r ON r.id_detalle_venta = d.id_detalle_venta AND r.estado = 'activa'
     WHERE d.id_detalle_venta = NEW.id_detalle_venta
     GROUP BY d.cantidad_vendida;

    IF v_cantidad_activa > v_cantidad_vendida THEN
        RAISE EXCEPTION 'las reservas activas de la linea de venta % (%) superan la cantidad vendida (%)',
            NEW.id_detalle_venta, v_cantidad_activa, v_cantidad_vendida
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

-- Restricciones diferibles: se verifican al confirmar la transacción, cuando todas las reservas de la operación son visibles.
CREATE CONSTRAINT TRIGGER tg_reserva_stock_verificar_inventario
    AFTER INSERT OR UPDATE ON reserva_stock
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_verificar_reservas_inventario();

CREATE CONSTRAINT TRIGGER tg_inventario_verificar_reservas
    AFTER UPDATE OF cantidad_fisica ON inventario
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_verificar_reservas_inventario();

CREATE CONSTRAINT TRIGGER tg_reserva_stock_verificar_detalle_venta
    AFTER INSERT OR UPDATE ON reserva_stock
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_verificar_reservas_detalle_venta();

CREATE CONSTRAINT TRIGGER tg_detalle_venta_verificar_reservas
    AFTER UPDATE OF cantidad_vendida ON detalle_venta
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_verificar_reservas_detalle_venta();

-- Disponibilidad por inventario. Las cantidades derivadas no se almacenan; se calculan a partir de las reservas activas.
CREATE VIEW v_disponibilidad_inventario WITH (security_invoker = true) AS
SELECT i.id_inventario,
       i.cantidad_fisica,
       COALESCE(r.cantidad_reservada, 0)                        AS cantidad_reservada,
       i.cantidad_fisica - COALESCE(r.cantidad_reservada, 0)    AS cantidad_libre
  FROM inventario i
  LEFT JOIN (
      SELECT id_inventario, SUM(cantidad_reservada) AS cantidad_reservada
        FROM reserva_stock
       WHERE estado = 'activa'
       GROUP BY id_inventario
  ) r ON r.id_inventario = i.id_inventario;

-- Cobertura por línea de venta. En ventas despachadas o canceladas no existe cantidad pendiente de abastecimiento,
-- porque sus reservas se consumieron o liberaron al cerrarse la operación.
CREATE VIEW v_cobertura_detalle_venta WITH (security_invoker = true) AS
SELECT d.id_detalle_venta,
       d.cantidad_vendida,
       COALESCE(r.cantidad_reservada, 0) AS cantidad_reservada_actual,
       CASE
           WHEN v.estado IN ('despachada', 'cancelada') THEN 0
           ELSE d.cantidad_vendida - COALESCE(r.cantidad_reservada, 0)
       END AS cantidad_pendiente_abastecimiento
  FROM detalle_venta d
  JOIN venta v ON v.id_venta = d.id_venta
  LEFT JOIN (
      SELECT id_detalle_venta, SUM(cantidad_reservada) AS cantidad_reservada
        FROM reserva_stock
       WHERE estado = 'activa'
       GROUP BY id_detalle_venta
  ) r ON r.id_detalle_venta = d.id_detalle_venta;

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE reserva_stock ENABLE ROW LEVEL SECURITY;
