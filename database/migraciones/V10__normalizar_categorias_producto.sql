-- Migración V10: categorías jerárquicas de producto.
-- Reemplaza el atributo libre producto.categoria_producto por la entidad categoria, organizada en dos niveles.
-- Las categorías de primer nivel tienen id_categoria_padre nulo; las subcategorías referencian a su padre con una clave foránea RESTRICT.
-- La regla de viabilidad de abastecimiento se apoya en categoria.aprobacion_automatica. Las categorías del catálogo se cargan como datos de referencia.

CREATE TABLE categoria (
    id_categoria          bigint        GENERATED ALWAYS AS IDENTITY,
    nombre                varchar(100)  NOT NULL,
    id_categoria_padre    bigint,
    aprobacion_automatica boolean       NOT NULL DEFAULT false,
    estado_activo         boolean       NOT NULL DEFAULT true,

    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT fk_categoria_categoria_padre FOREIGN KEY (id_categoria_padre)
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Una categoría no puede ser su propia categoría padre.
    CONSTRAINT ck_categoria_padre_distinto CHECK (id_categoria_padre IS NULL OR id_categoria_padre <> id_categoria)
);

-- Unicidad del nombre dentro de un mismo padre. Una restricción UNIQUE sobre (id_categoria_padre, nombre)
-- no compararía los padres nulos, por lo que las categorías de primer nivel se verifican con un índice parcial propio.
CREATE UNIQUE INDEX uq_categoria_nombre_primer_nivel ON categoria (nombre) WHERE id_categoria_padre IS NULL;
CREATE UNIQUE INDEX uq_categoria_nombre_padre ON categoria (id_categoria_padre, nombre) WHERE id_categoria_padre IS NOT NULL;
CREATE INDEX ix_categoria_id_categoria_padre ON categoria (id_categoria_padre);

-- Datos de referencia. Primero se insertan las categorías de primer nivel y luego las subcategorías,
-- que se vinculan a su padre por nombre.
INSERT INTO categoria (nombre) VALUES
    ('Materiales de construcción'),
    ('Acabados y pintura'),
    ('Instalaciones eléctricas'),
    ('Instalaciones sanitarias y gasfitería'),
    ('Herramientas'),
    ('Limpieza y cuidado del hogar'),
    ('Cerrajería y seguridad'),
    ('Bazar y varios');

INSERT INTO categoria (nombre, id_categoria_padre, aprobacion_automatica)
SELECT h.nombre, p.id_categoria, h.aprobacion_automatica
  FROM (VALUES
      ('Cemento y aglomerantes',          'Materiales de construcción',            true),
      ('Agregados',                       'Materiales de construcción',            true),
      ('Ladrillos y bloques',             'Materiales de construcción',            true),
      ('Maderas y paneles',               'Materiales de construcción',            false),
      ('Fierro y acero de construcción',  'Materiales de construcción',            true),
      ('Pinturas',                        'Acabados y pintura',                    false),
      ('Aerosoles y disolventes',         'Acabados y pintura',                    false),
      ('Herramientas de pintado',         'Acabados y pintura',                    false),
      ('Adhesivos y cintas',              'Acabados y pintura',                    false),
      ('Conducción eléctrica',            'Instalaciones eléctricas',              false),
      ('Tomas y controles',               'Instalaciones eléctricas',              false),
      ('Iluminación',                     'Instalaciones eléctricas',              false),
      ('Tuberías y conexiones',           'Instalaciones sanitarias y gasfitería', false),
      ('Aparatos sanitarios',             'Instalaciones sanitarias y gasfitería', false),
      ('Sellado e impermeabilización',    'Instalaciones sanitarias y gasfitería', false),
      ('Herramientas manuales',           'Herramientas',                          false),
      ('Sujetadores y fijaciones',        'Herramientas',                          false),
      ('Herramientas de corte y desgaste','Herramientas',                          false),
      ('Equipos de protección personal',  'Herramientas',                          false),
      ('Herramientas eléctricas y accesorios', 'Herramientas',                     false),
      ('Instrumentos de medición y trazo','Herramientas',                          false),
      ('Productos de limpieza',           'Limpieza y cuidado del hogar',          false),
      ('Utensilios de limpieza',          'Limpieza y cuidado del hogar',          false),
      ('Control de plagas',               'Limpieza y cuidado del hogar',          false)
  ) AS h (nombre, padre, aprobacion_automatica)
  JOIN categoria p ON p.nombre = h.padre AND p.id_categoria_padre IS NULL;

-- Producto no contiene datos en ningún entorno, por lo que id_categoria se agrega directamente como NOT NULL.
-- Si la tabla contuviera filas, esta sentencia fallaría y requeriría un valor inicial explícito antes de aplicar la restricción.
ALTER TABLE producto ADD COLUMN id_categoria bigint NOT NULL;
ALTER TABLE producto ADD CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria)
    REFERENCES categoria (id_categoria) ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE INDEX ix_producto_id_categoria ON producto (id_categoria);

-- La columna libre se elimina una vez que id_categoria la sustituye.
ALTER TABLE producto DROP COLUMN categoria_producto;

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE categoria ENABLE ROW LEVEL SECURITY;
