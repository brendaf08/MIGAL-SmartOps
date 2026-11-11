import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, problema, simularServidor, type PeticionSimulada } from '@/test/apiReal';

const respuestaAgente = (conversacionId: string) => ({
  conversacionId,
  mensajeId: '31',
  texto: 'Hay 80 bolsas libres de Cemento Sol tipo I, bolsa 42.5 kg.',
  incierta: false,
  herramientas: [
    {
      nombre: 'consultarDisponibilidad',
      parametros: { q: 'cemento sol' },
      resumen: 'Cemento Sol tipo I, bolsa 42.5 kg, con 80 bolsas libres.',
      codigo: null,
    },
  ],
  referencias: [{ tipo: 'producto', id: '42', codigo: 'CEM-001' }],
});

beforeEach(() => {
  guardarToken('token-de-prueba');
  activarApiReal();
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  window.sessionStorage.clear();
});

describe('agente IA con la API real', () => {
  it('envía la pregunta como mensaje y devuelve el texto, la incertidumbre y las consultas realizadas', async () => {
    const { consultarAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return { estado: 201, cuerpo: respuestaAgente('7') };
    });

    const respuesta = await consultarAgente('Disponibilidad de cemento Sol 42.5 kg');

    const envio = llamadas[0] as PeticionSimulada;
    expect(envio.metodo).toBe('POST');
    expect(envio.url.pathname).toBe('/api/v1/agente/mensajes');
    expect(envio.cabeceras.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(envio.cuerpo).toEqual({ mensaje: 'Disponibilidad de cemento Sol 42.5 kg' });
    expect(respuesta).toEqual({
      consultas: ['Cemento Sol tipo I, bolsa 42.5 kg, con 80 bolsas libres.'],
      texto: 'Hay 80 bolsas libres de Cemento Sol tipo I, bolsa 42.5 kg.',
      incierta: false,
    });
  });

  it('agrega las consultas siguientes a la misma conversación', async () => {
    const { consultarAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return { estado: 201, cuerpo: respuestaAgente('7') };
    });

    await consultarAgente('primera pregunta');
    await consultarAgente('segunda pregunta');

    expect((llamadas[1] as PeticionSimulada).cuerpo).toEqual({
      mensaje: 'segunda pregunta',
      conversacionId: '7',
    });
  });

  it('abre una conversación nueva cuando la anterior ya no pertenece al usuario', async () => {
    const { consultarAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    let primera = true;
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (primera) {
        primera = false;
        return { estado: 201, cuerpo: respuestaAgente('7') };
      }
      const conversacion = (peticion.cuerpo as { conversacionId?: string }).conversacionId;
      if (conversacion) return problema(404, 'RECURSO_NO_ENCONTRADO');
      return { estado: 201, cuerpo: respuestaAgente('9') };
    });

    await consultarAgente('primera');
    const respuesta = await consultarAgente('segunda');

    expect(respuesta.texto).toContain('80 bolsas');
    const reintento = llamadas[llamadas.length - 1] as PeticionSimulada;
    expect(reintento.cuerpo).toEqual({ mensaje: 'segunda' });
  });

  it('propaga SERVICIO_NO_DISPONIBLE cuando el modelo de lenguaje no responde', async () => {
    const { consultarAgente } = await import('./api');
    simularServidor(() => problema(503, 'SERVICIO_NO_DISPONIBLE'));

    await expect(consultarAgente('hola')).rejects.toMatchObject({
      estado: 503,
      codigo: 'SERVICIO_NO_DISPONIBLE',
    });
  });

  it('lista las conversaciones propias con paginación', async () => {
    const { listarConversacionesAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return {
        cuerpo: {
          items: [
            {
              id: '7',
              titulo: 'Disponibilidad de cemento',
              fechaCreacion: '2026-11-08T10:15:00-05:00',
              fechaUltimoMensaje: '2026-11-08T10:16:00-05:00',
              cantidadMensajes: 2,
            },
          ],
          total: 1,
          page: 1,
          pageSize: 100,
        },
      };
    });

    const conversaciones = await listarConversacionesAgente();

    const listado = llamadas[0] as PeticionSimulada;
    expect(listado.url.pathname).toBe('/api/v1/agente/conversaciones');
    expect(listado.url.searchParams.get('pageSize')).toBe('100');
    expect(conversaciones).toEqual([expect.objectContaining({ id: '7', cantidadMensajes: 2 })]);
  });

  it('consulta una conversación con sus mensajes y la elimina con DELETE', async () => {
    const { consultarConversacionAgente, eliminarConversacionAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.metodo === 'DELETE') return { estado: 204 };
      return {
        cuerpo: {
          id: '7',
          titulo: 'Disponibilidad de cemento',
          fechaCreacion: '2026-11-08T10:15:00-05:00',
          fechaUltimoMensaje: '2026-11-08T10:16:00-05:00',
          mensajes: [
            {
              id: '30',
              autor: 'usuario',
              texto: 'Disponibilidad de cemento Sol 42.5 kg',
              fechaHora: '2026-11-08T10:15:00-05:00',
              incierta: false,
              herramientas: [],
              referencias: [],
            },
          ],
        },
      };
    });

    const conversacion = await consultarConversacionAgente('7');
    await eliminarConversacionAgente('7');

    expect(conversacion.mensajes[0]).toMatchObject({ autor: 'usuario' });
    expect((llamadas[0] as PeticionSimulada).url.pathname).toBe('/api/v1/agente/conversaciones/7');
    expect(llamadas[1]).toMatchObject({ metodo: 'DELETE' });
    expect((llamadas[1] as PeticionSimulada).url.pathname).toBe('/api/v1/agente/conversaciones/7');
  });

  it('obtiene el catálogo de herramientas del rol', async () => {
    const { listarHerramientasAgente } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return {
        cuerpo: [
          {
            nombre: 'consultarDisponibilidad',
            descripcion: 'Consultar disponibilidad de productos',
            operacion: 'GET /inventario/disponibilidad',
          },
        ],
      };
    });

    const herramientas = await listarHerramientasAgente();

    expect((llamadas[0] as PeticionSimulada).url.pathname).toBe('/api/v1/agente/herramientas');
    expect(herramientas).toEqual([expect.objectContaining({ nombre: 'consultarDisponibilidad' })]);
  });
});
