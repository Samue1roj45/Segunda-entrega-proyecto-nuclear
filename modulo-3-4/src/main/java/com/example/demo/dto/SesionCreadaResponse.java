package com.example.demo.dto;

import java.util.List;
/*
 #REQUISITO RF-04 || RF-18
 Para qué sirve: respuesta al agendar: id de la sesión y los estudiantes convocados.
 */
public record SesionCreadaResponse(Long idPractica, List<ConvocadoDto> convocados) {
}
