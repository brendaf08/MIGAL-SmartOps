-- ADR-013. Rol de demostración: reúne los permisos de los cuatro roles del negocio para presentar el sistema con una
-- sola cuenta. Solo se crea al arrancar (MIGAL_ROL_DEMOSTRACION, MIGAL_DEMO_USERNAME, MIGAL_DEMO_CLAVE) y no puede
-- asignarse por la API de usuarios.
ALTER TABLE usuario DROP CONSTRAINT ck_usuario_rol;
ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_rol CHECK (rol IN ('administrador', 'vendedor', 'almacenero', 'reponedor', 'demostracion'));
