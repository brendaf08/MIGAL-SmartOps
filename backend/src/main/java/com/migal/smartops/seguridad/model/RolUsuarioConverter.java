package com.migal.smartops.seguridad.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte {@link RolUsuario} al texto en minúsculas que almacena la columna {@code usuario.rol}. */
@Converter(autoApply = true)
public class RolUsuarioConverter implements AttributeConverter<RolUsuario, String> {

	@Override
	public String convertToDatabaseColumn(RolUsuario rol) {
		return rol == null ? null : rol.valor();
	}

	@Override
	public RolUsuario convertToEntityAttribute(String valor) {
		return valor == null ? null : RolUsuario.desdeValor(valor);
	}
}
