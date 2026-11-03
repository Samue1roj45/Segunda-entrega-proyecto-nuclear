package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a Usuario (módulo ajeno). No implementa autenticación ni CRUD.
 */
public record UsuarioRef(Long id, String username, String email, String nombreCompleto, Rol rol) {
}
