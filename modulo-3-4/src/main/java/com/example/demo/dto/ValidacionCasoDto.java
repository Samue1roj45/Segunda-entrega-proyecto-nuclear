package com.example.demo.dto;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: un paso de la validación de un caso (sp_validar_caso): qué se revisó y si cumple.
 */
public record ValidacionCasoDto(int paso, String verificacion, boolean cumple) {
}
