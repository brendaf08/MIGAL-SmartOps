-- Migración V15: tabla idempotencia.
-- Guarda el resultado de las operaciones que exigen la cabecera Idempotency-Key (ADR-009 y ADR-010) para que
-- una solicitud repetida devuelva la respuesta original en lugar de ejecutarse de nuevo.
-- Una clave repetida con la misma huella de solicitud devuelve la respuesta guardada; con otra huella
-- responde IDEMPOTENCIA_CONFLICTO. Los registros se conservan hasta fecha_expiracion (24 horas) y se
-- eliminan de forma periódica. No es una entidad de negocio, por lo que no sigue la política de solo anexión.

CREATE TABLE idempotencia (
    id_idempotencia  bigint        GENERATED ALWAYS AS IDENTITY,
    clave            varchar(100)  NOT NULL,
    id_usuario       bigint        NOT NULL,
    operacion        varchar(100)  NOT NULL,
    huella           char(64)      NOT NULL,
    estado_http      smallint,
    respuesta        jsonb,
    fecha_creacion   timestamptz   NOT NULL DEFAULT now(),
    fecha_expiracion timestamptz   NOT NULL,

    CONSTRAINT pk_idempotencia PRIMARY KEY (id_idempotencia),
    CONSTRAINT fk_idempotencia_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- La clave identifica la solicitud dentro de un usuario y una operación.
    CONSTRAINT uq_idempotencia_usuario_operacion_clave UNIQUE (id_usuario, operacion, clave),
    CONSTRAINT ck_idempotencia_clave CHECK (btrim(clave) <> ''),
    CONSTRAINT ck_idempotencia_operacion CHECK (btrim(operacion) <> ''),
    -- La huella es el resumen SHA-256 de la solicitud, en hexadecimal.
    CONSTRAINT ck_idempotencia_huella CHECK (huella ~ '^[0-9a-f]{64}$'),
    -- Mientras la operación está en curso no hay respuesta; al terminar se guardan ambas.
    CONSTRAINT ck_idempotencia_respuesta CHECK (
        (estado_http IS NULL AND respuesta IS NULL)
        OR (estado_http IS NOT NULL AND respuesta IS NOT NULL)),
    CONSTRAINT ck_idempotencia_estado_http CHECK (estado_http IS NULL OR estado_http BETWEEN 100 AND 599),
    CONSTRAINT ck_idempotencia_vigencia CHECK (fecha_expiracion > fecha_creacion)
);

CREATE INDEX ix_idempotencia_id_usuario ON idempotencia (id_usuario);
-- Acelera la limpieza periódica de registros vencidos.
CREATE INDEX ix_idempotencia_fecha_expiracion ON idempotencia (fecha_expiracion);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE idempotencia ENABLE ROW LEVEL SECURITY;
