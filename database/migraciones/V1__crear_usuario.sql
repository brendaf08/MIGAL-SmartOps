-- Migración V1: tabla usuario.
-- Define las cuentas de acceso al sistema según el modelo y diccionario de datos.
-- Las credenciales se almacenan únicamente como representación hash (password_hash).
-- La desactivación es lógica mediante estado_activo; la aplicación no elimina filas de esta tabla.

CREATE TABLE usuario (
    id_usuario          bigint       GENERATED ALWAYS AS IDENTITY,
    nombres             varchar(100) NOT NULL,
    apellidos           varchar(100) NOT NULL,
    username            varchar(50)  NOT NULL,
    password_hash       varchar(255) NOT NULL,
    rol                 varchar(20)  NOT NULL,
    estado_activo       boolean      NOT NULL DEFAULT true,
    fecha_registro      timestamptz  NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz,

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    -- El nombre de usuario identifica la cuenta en el inicio de sesión.
    CONSTRAINT uq_usuario_username UNIQUE (username),
    -- Niveles de acceso definidos en el diccionario de datos.
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('administrador', 'vendedor', 'almacenero', 'reponedor'))
);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;
