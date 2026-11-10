package com.migal.smartops.seguridad.config;

import com.migal.smartops.seguridad.model.RolUsuario;

/**
 * Usuario autenticado en la solicitud en curso. Sus datos se leen de la base de datos en cada solicitud, por
 * lo que reflejan de inmediato un cambio de rol o la desactivación de la cuenta (ADR-009).
 */
public record UsuarioAutenticado(long id, String username, RolUsuario rol) {
}
