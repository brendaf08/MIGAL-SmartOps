-- Migración V14: conversaciones y mensajes del agente de IA (CU-14).
-- Crea conversacion_agente y mensaje_agente, que conservan el historial de consultas de cada usuario al agente.
-- El agente es de solo lectura: estas tablas no referencian inventario, ventas ni movimientos de Kardex, y su
-- contenido no genera operaciones de negocio.
-- Excepción a la regla de eliminación restringida (ADR-005): mensaje_agente se elimina en cascada con su conversación.
-- La eliminación de una conversación por el usuario borra sus mensajes de forma física. Los mensajes no son entidades
-- de negocio; su conservación no tiene valor para la trazabilidad operativa. Las demás claves foráneas usan RESTRICT.
-- Las columnas herramientas y referencias almacenan arreglos JSON: las herramientas invocadas y los registros
-- consultados. Su estructura interna se valida en la capa de negocio.
-- La actualización de fecha_ultimo_mensaje al agregar un mensaje se aplica en la capa de negocio.

-- Conversación del usuario con el agente.
CREATE TABLE conversacion_agente (
    id_conversacion       bigint       GENERATED ALWAYS AS IDENTITY,
    id_usuario            bigint       NOT NULL,
    titulo                varchar(200) NOT NULL,
    fecha_creacion        timestamptz  NOT NULL DEFAULT now(),
    fecha_ultimo_mensaje  timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_conversacion_agente PRIMARY KEY (id_conversacion),
    CONSTRAINT fk_conversacion_agente_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- El título se genera a partir del primer mensaje y no puede estar formado solo por espacios.
    CONSTRAINT ck_conversacion_agente_titulo CHECK (btrim(titulo) <> ''),
    -- El último mensaje no puede anteceder a la creación de la conversación.
    CONSTRAINT ck_conversacion_agente_fechas CHECK (fecha_ultimo_mensaje >= fecha_creacion)
);

-- Listado de las conversaciones de un usuario, de la más reciente a la más antigua.
CREATE INDEX ix_conversacion_agente_usuario_fecha ON conversacion_agente (id_usuario, fecha_ultimo_mensaje DESC);

-- Mensaje de una conversación, del usuario o del agente.
CREATE TABLE mensaje_agente (
    id_mensaje       bigint       GENERATED ALWAYS AS IDENTITY,
    id_conversacion  bigint       NOT NULL,
    rol              varchar(10)  NOT NULL,
    texto            text         NOT NULL,
    incierta         boolean      NOT NULL DEFAULT false,
    herramientas     jsonb        NOT NULL DEFAULT '[]'::jsonb,
    referencias      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    fecha            timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_mensaje_agente PRIMARY KEY (id_mensaje),
    -- Eliminación en cascada: la eliminación de la conversación borra sus mensajes (excepción justificada en la cabecera).
    CONSTRAINT fk_mensaje_agente_conversacion FOREIGN KEY (id_conversacion)
        REFERENCES conversacion_agente (id_conversacion) ON DELETE CASCADE ON UPDATE RESTRICT,
    -- Rol del autor: usuario o agente.
    CONSTRAINT ck_mensaje_agente_rol CHECK (rol IN ('usuario', 'agente')),
    -- El texto es obligatorio y no puede estar formado solo por espacios.
    CONSTRAINT ck_mensaje_agente_texto CHECK (btrim(texto) <> ''),
    -- La indicación de respuesta incierta solo aplica a los mensajes del agente.
    CONSTRAINT ck_mensaje_agente_incierta CHECK (rol = 'agente' OR incierta = false),
    -- Los mensajes del usuario no registran herramientas ni referencias.
    CONSTRAINT ck_mensaje_agente_consultas_del_agente CHECK (
        rol = 'agente' OR (herramientas = '[]'::jsonb AND referencias = '[]'::jsonb)),
    -- Las herramientas y las referencias son arreglos JSON.
    CONSTRAINT ck_mensaje_agente_herramientas CHECK (jsonb_typeof(herramientas) = 'array'),
    CONSTRAINT ck_mensaje_agente_referencias CHECK (jsonb_typeof(referencias) = 'array')
);

-- Recuperación de los mensajes de una conversación en orden cronológico. La columna id_conversacion, líder del índice,
-- cubre la clave foránea.
CREATE INDEX ix_mensaje_agente_conversacion_fecha ON mensaje_agente (id_conversacion, fecha);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE conversacion_agente ENABLE ROW LEVEL SECURITY;
ALTER TABLE mensaje_agente ENABLE ROW LEVEL SECURITY;
