import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  activarApiReal,
  cargarModulo,
  json,
  problema,
  restablecerApiReal,
  simularServidor,
} from '@/test/servidorApi';

const USUARIO_API = (id: string, rol: string, activo = true) => ({
  id,
  nombres: 'Rosa',
  apellidos: 'Quispe Huamán',
  username: 'rquispe',
  rol,
  estadoActivo: activo,
  fechaRegistro: '2026-03-02T09:15:00-05:00',
  fechaActualizacion: null,
});

beforeEach(activarApiReal);
afterEach(restablecerApiReal);

describe('usuarios, listado (rama de la API real)', () => {
  it('recorre todas las páginas y mapea los usuarios al directorio de la interfaz', async () => {
    const primera = Array.from({ length: 100 }, (_, i) => USUARIO_API(String(i), 'vendedor'));
    const llamadas = simularServidor({
      'GET /api/v1/usuarios': (llamada) =>
        llamada.consulta.get('page') === '1'
          ? json({ items: primera, total: 101, page: 1, pageSize: 100 })
          : json({
              items: [USUARIO_API('101', 'almacenero', false)],
              total: 101,
              page: 2,
              pageSize: 100,
            }),
    });
    const { listarUsuarios } = await cargarModulo(() => import('./api'));

    const usuarios = await listarUsuarios();

    expect(llamadas[0]!.cabeceras.get('Authorization')).toBe('Bearer TOKEN-PRUEBA');
    expect(llamadas[0]!.cabeceras.has('Idempotency-Key')).toBe(false);
    expect(usuarios).toHaveLength(101);
    expect(usuarios[0]).toEqual({
      id: '0',
      nombre: 'Rosa Quispe Huamán',
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      usuario: 'rquispe',
      rol: 'Vendedor',
      estado: 'Activo',
    });
    expect(usuarios.at(-1)).toEqual({
      id: '101',
      nombre: 'Rosa Quispe Huamán',
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      usuario: 'rquispe',
      rol: 'Almacenero',
      estado: 'Inactivo',
    });
  });

  it('muestra el rol de la API con inicial mayúscula en el resumen de sesión', async () => {
    simularServidor({
      'GET /api/v1/usuarios': () =>
        json({ items: [USUARIO_API('7', 'reponedor')], total: 1, page: 1, pageSize: 100 }),
    });
    const { listarUsuarios } = await cargarModulo(() => import('@/features/seguridad/api'));

    const resumen = await listarUsuarios();

    expect(resumen).toEqual([
      { usuario: 'rquispe', name: 'Rosa Quispe Huamán', role: 'Reponedor' },
    ]);
  });

  it('propaga el error de permiso del listado', async () => {
    simularServidor({
      'GET /api/v1/usuarios': () => problema(403, 'PERMISO_INSUFICIENTE', 'Solo el administrador.'),
    });
    const { listarUsuarios } = await cargarModulo(() => import('./api'));

    await expect(listarUsuarios()).rejects.toMatchObject({
      codigo: 'PERMISO_INSUFICIENTE',
      estado: 403,
    });
  });
});

describe('usuarios, revocación de acceso (rama de la API real)', () => {
  it('desactiva el acceso con PUT /usuarios/{id}/estado y devuelve el usuario actualizado', async () => {
    const llamadas = simularServidor({
      'PUT /api/v1/usuarios/7/estado': () => json(USUARIO_API('7', 'almacenero', false)),
    });
    const { revocarAcceso } = await cargarModulo(() => import('./api'));

    const usuario = await revocarAcceso('7');

    expect(llamadas[0]!.cuerpo).toEqual({ estadoActivo: false });
    expect(usuario).toEqual({
      id: '7',
      nombre: 'Rosa Quispe Huamán',
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      usuario: 'rquispe',
      rol: 'Almacenero',
      estado: 'Inactivo',
    });
  });

  it('informa RECURSO_NO_ENCONTRADO cuando el identificador no existe', async () => {
    simularServidor({
      'PUT /api/v1/usuarios/999/estado': () => problema(404, 'RECURSO_NO_ENCONTRADO', 'No existe.'),
    });
    const { revocarAcceso } = await cargarModulo(() => import('./api'));

    await expect(revocarAcceso('999')).rejects.toMatchObject({
      codigo: 'RECURSO_NO_ENCONTRADO',
      estado: 404,
    });
  });
});

describe('usuarios, registro y edición (rama de la API real)', () => {
  it('registra con nombres, apellidos, clave temporal y el rol en minúsculas', async () => {
    const llamadas = simularServidor({
      'POST /api/v1/usuarios': () => json(USUARIO_API('9', 'vendedor'), 201),
    });
    const { registrarUsuario } = await cargarModulo(() => import('./api'));

    const usuario = await registrarUsuario({
      nombres: ' Rosa ',
      apellidos: 'Quispe Huamán',
      usuario: 'rquispe',
      clave: 'Temporal2026',
      rol: 'Vendedor',
    });

    expect(llamadas[0]!.cuerpo).toEqual({
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      username: 'rquispe',
      claveTemporal: 'Temporal2026',
      rol: 'vendedor',
    });
    expect(usuario.rol).toBe('Vendedor');
  });

  it('traduce USUARIO_DUPLICADO para mostrarlo en el campo del formulario', async () => {
    simularServidor({
      'POST /api/v1/usuarios': () => problema(409, 'USUARIO_DUPLICADO', 'Ya existe.'),
    });
    const { registrarUsuario } = await cargarModulo(() => import('./api'));

    await expect(
      registrarUsuario({
        nombres: 'A',
        apellidos: 'B',
        usuario: 'admin',
        clave: 'clave1234',
        rol: 'Vendedor',
      }),
    ).rejects.toThrow('USUARIO_DUPLICADO');
  });

  it('consulta y modifica con GET y PUT /usuarios/{id}', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/usuarios/7': () => json(USUARIO_API('7', 'almacenero')),
      'PUT /api/v1/usuarios/7': () => json(USUARIO_API('7', 'reponedor')),
    });
    const { actualizarUsuario, obtenerUsuario } = await cargarModulo(() => import('./api'));

    expect((await obtenerUsuario('7')).apellidos).toBe('Quispe Huamán');
    const usuario = await actualizarUsuario('7', {
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      rol: 'Reponedor',
    });

    expect(llamadas[1]!.cuerpo).toEqual({
      nombres: 'Rosa',
      apellidos: 'Quispe Huamán',
      rol: 'reponedor',
    });
    expect(usuario.rol).toBe('Reponedor');
  });

  it('reactiva el acceso con estadoActivo verdadero', async () => {
    const llamadas = simularServidor({
      'PUT /api/v1/usuarios/7/estado': () => json(USUARIO_API('7', 'almacenero', true)),
    });
    const { reactivarAcceso } = await cargarModulo(() => import('./api'));

    expect((await reactivarAcceso('7')).estado).toBe('Activo');
    expect(llamadas[0]!.cuerpo).toEqual({ estadoActivo: true });
  });
});
