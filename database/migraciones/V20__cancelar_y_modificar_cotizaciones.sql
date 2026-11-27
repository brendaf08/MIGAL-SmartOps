-- Migración V20: cancelación de cotizaciones aceptadas o validadas, modificación posterior a la aceptación e historial.
-- Una cotización cancelada es la que ya fue aceptada o validada y se dejó sin efecto (por el cliente, por MIGAL o porque
-- su venta se canceló antes de la salida del almacén). Se distingue de la rechazada, que nunca llegó a aceptarse.
-- La cancelación reutiliza los datos del rechazo (origen, usuario, fecha y motivo), obligatorios en ambos estados.
-- Cuando una cotización aceptada o validada se modifica, vuelve a emitida; el cambio y su motivo quedan en
-- cotizacion_historial, que registra también las cancelaciones.

ALTER TABLE cotizacion DROP CONSTRAINT ck_cotizacion_estado;
ALTER TABLE cotizacion ADD CONSTRAINT ck_cotizacion_estado CHECK (
    estado IN ('emitida', 'aceptada', 'validada', 'rechazada', 'cancelada', 'vencida'));

ALTER TABLE cotizacion DROP CONSTRAINT ck_cotizacion_rechazo;
ALTER TABLE cotizacion ADD CONSTRAINT ck_cotizacion_rechazo CHECK (
    (estado IN ('rechazada', 'cancelada')
        AND origen_rechazo IS NOT NULL
        AND id_usuario_rechazo IS NOT NULL
        AND fecha_rechazo IS NOT NULL
        AND motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> '')
    OR
    (estado NOT IN ('rechazada', 'cancelada')
        AND origen_rechazo IS NULL
        AND id_usuario_rechazo IS NULL
        AND fecha_rechazo IS NULL
        AND motivo_rechazo IS NULL));

CREATE TABLE cotizacion_historial (
    id_historial    bigint       GENERATED ALWAYS AS IDENTITY,
    id_cotizacion   bigint       NOT NULL,
    fecha           timestamptz  NOT NULL DEFAULT now(),
    id_usuario      bigint       NOT NULL,
    estado_anterior varchar(20)  NOT NULL,
    estado_nuevo    varchar(20)  NOT NULL,
    motivo          text         NOT NULL,

    CONSTRAINT pk_cotizacion_historial PRIMARY KEY (id_historial),
    CONSTRAINT fk_cotizacion_historial_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion (id_cotizacion) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_cotizacion_historial_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_cotizacion_historial_estados CHECK (
        estado_anterior IN ('emitida', 'aceptada', 'validada', 'rechazada', 'cancelada', 'vencida')
        AND estado_nuevo IN ('emitida', 'aceptada', 'validada', 'rechazada', 'cancelada', 'vencida')),
    CONSTRAINT ck_cotizacion_historial_motivo CHECK (btrim(motivo) <> '')
);

CREATE INDEX ix_cotizacion_historial_id_cotizacion ON cotizacion_historial (id_cotizacion);
CREATE INDEX ix_cotizacion_historial_id_usuario ON cotizacion_historial (id_usuario);

-- Se habilita RLS sin políticas, igual que el resto de las tablas: el backend se conecta directamente a PostgreSQL.
ALTER TABLE cotizacion_historial ENABLE ROW LEVEL SECURITY;
