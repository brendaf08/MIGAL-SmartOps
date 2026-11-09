-- Migración V12: rechazo de cotización, guía de despacho anulada, fecha de entrega y reimpresiones de guía.
-- Registra en cotizacion el origen del rechazo (cliente o migal), el usuario que lo registra, la fecha y el motivo.
-- Los cuatro datos son obligatorios cuando la cotización está rechazada y nulos en cualquier otro estado.
-- Se amplía el dominio de guia_despacho.estado con el valor anulada, que corresponde a la cancelación de una venta
-- en lista_para_despacho. La anulación no genera movimientos de Kardex, pues la guía no había confirmado la salida.
-- Se agrega fecha_entrega, obligatoria cuando la guía está entregada y nula en los demás estados.
-- La salida confirmada exige fecha_salida_real y el usuario responsable; en anulada ambos datos pueden no existir,
-- pero se informan juntos o se omiten juntos.
-- reimpresion_guia es de solo anexión: tg_reimpresion_guia_solo_anexion rechaza toda actualización o eliminación,
-- y tg_reimpresion_guia_no_truncar rechaza el vaciado completo de la tabla. La reimpresión no modifica el estado
-- de la guía ni el inventario.
-- Las transiciones de estado se aplican en la capa de negocio, como en las migraciones anteriores.

-- Rechazo de cotización.
ALTER TABLE cotizacion
    ADD COLUMN origen_rechazo varchar(10),
    ADD COLUMN id_usuario_rechazo bigint,
    ADD COLUMN fecha_rechazo timestamptz,
    ADD COLUMN motivo_rechazo text;

ALTER TABLE cotizacion ADD CONSTRAINT ck_cotizacion_origen_rechazo CHECK (
    origen_rechazo IS NULL OR origen_rechazo IN ('cliente', 'migal'));
ALTER TABLE cotizacion ADD CONSTRAINT fk_cotizacion_usuario_rechazo FOREIGN KEY (id_usuario_rechazo)
    REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE INDEX ix_cotizacion_id_usuario_rechazo ON cotizacion (id_usuario_rechazo);

-- Condicional: el origen, el usuario, la fecha y el motivo (no vacío) son obligatorios cuando la cotización está
-- rechazada y nulos en otro caso. Las comparaciones con el motivo se escriben con IS NOT NULL para que un valor nulo
-- no produzca un resultado nulo.
ALTER TABLE cotizacion ADD CONSTRAINT ck_cotizacion_rechazo CHECK (
    (estado = 'rechazada'
        AND origen_rechazo IS NOT NULL
        AND id_usuario_rechazo IS NOT NULL
        AND fecha_rechazo IS NOT NULL
        AND motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> '')
    OR
    (estado <> 'rechazada'
        AND origen_rechazo IS NULL
        AND id_usuario_rechazo IS NULL
        AND fecha_rechazo IS NULL
        AND motivo_rechazo IS NULL));

-- Guía de despacho: estado anulada y fecha de entrega.
ALTER TABLE guia_despacho DROP CONSTRAINT ck_guia_despacho_estado;
ALTER TABLE guia_despacho ADD CONSTRAINT ck_guia_despacho_estado CHECK (
    estado IN ('preparacion', 'emitida', 'salida_confirmada', 'entregada', 'anulada'));

ALTER TABLE guia_despacho ADD COLUMN fecha_entrega timestamptz;
-- La fecha de entrega es obligatoria solo cuando la guía está entregada.
ALTER TABLE guia_despacho ADD CONSTRAINT ck_guia_despacho_fecha_entrega CHECK (
    (estado = 'entregada') = (fecha_entrega IS NOT NULL));

-- Coherencia de la salida. Se reemplaza la restricción de V6 para admitir la anulación. Las comparaciones entre
-- datos de salida se escriben como igualdad de indicadores booleanos, de modo que un valor nulo no produzca un resultado nulo.
ALTER TABLE guia_despacho DROP CONSTRAINT ck_guia_despacho_salida_confirmada;
ALTER TABLE guia_despacho ADD CONSTRAINT ck_guia_despacho_salida_confirmada CHECK (
    (estado IN ('salida_confirmada', 'entregada')
        AND fecha_salida_real IS NOT NULL
        AND id_usuario_responsable IS NOT NULL)
    OR
    (estado IN ('preparacion', 'emitida')
        AND fecha_salida_real IS NULL
        AND id_usuario_responsable IS NULL)
    OR
    (estado = 'anulada'
        AND (fecha_salida_real IS NULL) = (id_usuario_responsable IS NULL)));

-- Reimpresiones de la guía. Cada reimpresión registra el usuario, la fecha y el motivo obligatorio.
CREATE TABLE reimpresion_guia (
    id_reimpresion     bigint       GENERATED ALWAYS AS IDENTITY,
    id_guia            bigint       NOT NULL,
    id_usuario         bigint       NOT NULL,
    fecha_reimpresion  timestamptz  NOT NULL DEFAULT now(),
    motivo             text         NOT NULL,

    CONSTRAINT pk_reimpresion_guia PRIMARY KEY (id_reimpresion),
    CONSTRAINT fk_reimpresion_guia_guia_despacho FOREIGN KEY (id_guia)
        REFERENCES guia_despacho (id_guia) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reimpresion_guia_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- El motivo es obligatorio y no puede estar formado solo por espacios.
    CONSTRAINT ck_reimpresion_guia_motivo CHECK (btrim(motivo) <> '')
);

CREATE INDEX ix_reimpresion_guia_id_guia ON reimpresion_guia (id_guia);
CREATE INDEX ix_reimpresion_guia_id_usuario ON reimpresion_guia (id_usuario);

-- Disparador de solo anexión. Se rechaza cualquier actualización o eliminación de una fila.
CREATE FUNCTION fn_reimpresion_guia_solo_anexion() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'reimpresion_guia es de solo anexion: no se permite la operacion %', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER tg_reimpresion_guia_solo_anexion
    BEFORE UPDATE OR DELETE ON reimpresion_guia
    FOR EACH ROW
    EXECUTE FUNCTION fn_reimpresion_guia_solo_anexion();

CREATE TRIGGER tg_reimpresion_guia_no_truncar
    BEFORE TRUNCATE ON reimpresion_guia
    FOR EACH STATEMENT
    EXECUTE FUNCTION fn_reimpresion_guia_solo_anexion();

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE reimpresion_guia ENABLE ROW LEVEL SECURITY;
