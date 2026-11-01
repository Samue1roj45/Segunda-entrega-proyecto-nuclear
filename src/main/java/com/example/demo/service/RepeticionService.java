package com.example.demo.service;

import com.example.demo.dto.IntentosDto;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.DatosRepeticion;
import com.example.demo.model.SolicitudRepeticion;
import com.example.demo.repository.RepeticionRepository;
import com.example.demo.service.repeticion.CadenaValidacionRepeticion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-25
 Para qué sirve: el docente creador del caso concede un intento adicional a un estudiante
 y consulta cuántos intentos lleva y cuántos tiene permitidos.
 */
@Service
public class RepeticionService {

    private final RepeticionRepository repo;
    private final CadenaValidacionRepeticion cadena;

    public RepeticionService(RepeticionRepository repo, CadenaValidacionRepeticion cadena) {
        this.repo = repo;
        this.cadena = cadena;
    }

    @Transactional
    public IntentosDto autorizar(Long idAutorizacion, Long idDocente, String motivo) {
        DatosRepeticion datos = cargar(idAutorizacion);
        cadena.validar(new SolicitudRepeticion(idAutorizacion, idDocente, motivo, datos));
        repo.autorizar(idAutorizacion, idDocente, motivo);
        DatosRepeticion actualizados = cargar(idAutorizacion);
        return new IntentosDto(idAutorizacion, actualizados.intentosUsados(), actualizados.intentosPermitidos());
    }

    @Transactional(readOnly = true)
    public IntentosDto consultar(Long idAutorizacion, Long idDocente) {
        DatosRepeticion d = cargar(idAutorizacion);
        if (!idDocente.equals(d.idDocenteCreador()) && !idDocente.equals(d.idDocenteSesion())) {
            throw new AccesoDenegadoException("Solo el docente de la sesión o creador del caso puede consultar esto");
        }
        return new IntentosDto(idAutorizacion, d.intentosUsados(), d.intentosPermitidos());
    }

    private DatosRepeticion cargar(Long idAutorizacion) {
        return repo.cargar(idAutorizacion)
                .orElseThrow(() -> new ReglaNegocioException("La autorización no existe"));
    }
}
