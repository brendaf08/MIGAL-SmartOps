-- Migración V7: tabla movimiento_kardex y disparadores asociados.
-- Registra las variaciones físicas del inventario según el modelo y diccionario de datos y el ADR-005.
-- El registro es de solo anexión: tg_movimiento_kardex_solo_anexion rechaza toda actualización o eliminación,
-- y tg_movimiento_kardex_no_truncar rechaza el vaciado completo de la tabla.
-- Una corrección se registra como un movimiento compensatorio de origen ajuste que referencia el movimiento corregido.
-- Un traslado genera una salida y una entrada vinculadas por referencia_traslado. La restricción diferible
-- tg_movimiento_kardex_traslado verifica que cada referencia agrupe exactamente ambos movimientos.
-- El saldo de cantidad_fisica se actualiza en la capa de negocio, en la misma transacción que el movimiento.

CREATE TABLE movimiento_kardex (
    id_movimiento           bigint        GENERATED ALWAYS AS IDENTITY,
    id_inventario           bigint        NOT NULL,
    fecha_hora              timestamptz   NOT NULL DEFAULT now(),
    tipo_operacion          varchar(10)   NOT NULL,
    cantidad                numeric(14,3) NOT NULL,
    motivo                  text,
    id_usuario_responsable  bigint        NOT NULL,
    origen_movimiento       varchar(20)   NOT NULL,
    id_guia                 bigint,
    id_recepcion            bigint,
    referencia_traslado     uuid,
    id_movimiento_corregido bigint,

    CONSTRAINT pk_movimiento_kardex PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_movimiento_kardex_inventario FOREIGN KEY (id_inventario)
        REFERENCES inventario (id_inventario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_kardex_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_kardex_guia_despacho FOREIGN KEY (id_guia)
        REFERENCES guia_despacho (id_guia) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_kardex_orden_recepcion FOREIGN KEY (id_recepcion)
        REFERENCES orden_recepcion (id_recepcion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_kardex_movimiento_corregido FOREIGN KEY (id_movimiento_corregido)
        REFERENCES movimiento_kardex (id_movimiento) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_movimiento_kardex_tipo_operacion CHECK (tipo_operacion IN ('entrada', 'salida')),
    CONSTRAINT ck_movimiento_kardex_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_kardex_origen CHECK (origen_movimiento IN ('despacho', 'recepcion', 'ajuste', 'traslado')),
    -- Un despacho es una salida vinculada a su guía y no a una recepción.
    CONSTRAINT ck_movimiento_kardex_origen_despacho CHECK (origen_movimiento <> 'despacho'
        OR (tipo_operacion = 'salida' AND id_guia IS NOT NULL AND id_recepcion IS NULL)),
    -- Una recepción es una entrada vinculada a su recepción y no a una guía.
    CONSTRAINT ck_movimiento_kardex_origen_recepcion CHECK (origen_movimiento <> 'recepcion'
        OR (tipo_operacion = 'entrada' AND id_recepcion IS NOT NULL AND id_guia IS NULL)),
    -- Un ajuste exige motivo y no admite referencias de operación.
    CONSTRAINT ck_movimiento_kardex_origen_ajuste CHECK (origen_movimiento <> 'ajuste'
        OR (motivo IS NOT NULL AND btrim(motivo) <> ''
            AND id_guia IS NULL AND id_recepcion IS NULL AND referencia_traslado IS NULL)),
    -- Un traslado exige motivo y su referencia compartida, y no admite referencias de operación.
    CONSTRAINT ck_movimiento_kardex_origen_traslado CHECK (origen_movimiento <> 'traslado'
        OR (motivo IS NOT NULL AND btrim(motivo) <> ''
            AND referencia_traslado IS NOT NULL AND id_guia IS NULL AND id_recepcion IS NULL)),
    -- La referencia de traslado solo corresponde a movimientos de origen traslado.
    CONSTRAINT ck_movimiento_kardex_referencia_traslado CHECK (referencia_traslado IS NULL
        OR origen_movimiento = 'traslado'),
    -- La corrección solo corresponde a movimientos compensatorios de origen ajuste.
    CONSTRAINT ck_movimiento_kardex_correccion CHECK (id_movimiento_corregido IS NULL
        OR origen_movimiento = 'ajuste')
);

CREATE INDEX ix_movimiento_kardex_id_inventario ON movimiento_kardex (id_inventario);
CREATE INDEX ix_movimiento_kardex_id_usuario_responsable ON movimiento_kardex (id_usuario_responsable);
CREATE INDEX ix_movimiento_kardex_id_guia ON movimiento_kardex (id_guia);
CREATE INDEX ix_movimiento_kardex_id_recepcion ON movimiento_kardex (id_recepcion);
CREATE INDEX ix_movimiento_kardex_id_movimiento_corregido ON movimiento_kardex (id_movimiento_corregido);
-- Índice parcial: solo los movimientos de traslado poseen referencia.
CREATE INDEX ix_movimiento_kardex_referencia_traslado ON movimiento_kardex (referencia_traslado)
    WHERE referencia_traslado IS NOT NULL;

-- Disparador de solo anexión. Se rechaza cualquier actualización o eliminación de una fila.
CREATE FUNCTION fn_movimiento_kardex_solo_anexion() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'movimiento_kardex es de solo anexion: no se permite la operacion %', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER tg_movimiento_kardex_solo_anexion
    BEFORE UPDATE OR DELETE ON movimiento_kardex
    FOR EACH ROW
    EXECUTE FUNCTION fn_movimiento_kardex_solo_anexion();

CREATE TRIGGER tg_movimiento_kardex_no_truncar
    BEFORE TRUNCATE ON movimiento_kardex
    FOR EACH STATEMENT
    EXECUTE FUNCTION fn_movimiento_kardex_solo_anexion();

-- Restricción diferible: al confirmar la transacción, cada referencia de traslado debe agrupar exactamente
-- una salida y una entrada de igual cantidad y de igual producto, en inventarios distintos.
CREATE FUNCTION fn_movimiento_kardex_verificar_traslado() RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_movimientos  integer;
    v_salidas      integer;
    v_entradas     integer;
    v_cantidades   integer;
    v_productos    integer;
    v_inventarios  integer;
BEGIN
    SELECT count(*),
           count(*) FILTER (WHERE m.tipo_operacion = 'salida'),
           count(*) FILTER (WHERE m.tipo_operacion = 'entrada'),
           count(DISTINCT m.cantidad),
           count(DISTINCT i.id_producto),
           count(DISTINCT m.id_inventario)
      INTO v_movimientos, v_salidas, v_entradas, v_cantidades, v_productos, v_inventarios
      FROM movimiento_kardex m
      JOIN inventario i ON i.id_inventario = m.id_inventario
     WHERE m.referencia_traslado = NEW.referencia_traslado;

    IF v_movimientos <> 2 OR v_salidas <> 1 OR v_entradas <> 1 OR v_cantidades <> 1
       OR v_productos <> 1 OR v_inventarios <> 2 THEN
        RAISE EXCEPTION 'el traslado % debe agrupar una salida y una entrada de igual cantidad, producto e inventarios distintos',
            NEW.referencia_traslado
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER tg_movimiento_kardex_traslado
    AFTER INSERT ON movimiento_kardex
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    WHEN (NEW.referencia_traslado IS NOT NULL)
    EXECUTE FUNCTION fn_movimiento_kardex_verificar_traslado();

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE movimiento_kardex ENABLE ROW LEVEL SECURITY;
