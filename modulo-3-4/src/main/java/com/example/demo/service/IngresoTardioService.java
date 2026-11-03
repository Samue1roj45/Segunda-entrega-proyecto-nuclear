package com.example.demo.service;

import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.DatosTiempoSesion;
import com.example.demo.politica.PoliticaIngresoTardio;
import com.example.demo.repository.ParticipacionRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-22
 Para qué sirve: decide si un estudiante puede iniciar su participación ahora, usando la
 estrategia de ingreso tardío. La BD (trigger trg_participaciones_bi) sigue siendo la
 última barrera; esto da el mensaje claro y el límite de ingreso para el frontend.
 */
@Service
public class IngresoTardioService {

    private final ParticipacionRepository repo;
    private final PoliticaIngresoTardio politica;

    public IngresoTardioService(ParticipacionRepository repo, PoliticaIngresoTardio politica) {
        this.repo = repo;
        this.politica = politica;
    }

    @Transactional(readOnly = true)
    public EstadoIngresoDto consultar(Long idAutorizacion) {
        DatosTiempoSesion d = repo.datosTiempo(idAutorizacion)
                .orElseThrow(() -> new ReglaNegocioException("La autorización no existe"));
        LocalDateTime limite = politica.limiteIngreso(d);

        String motivo = null;
        if (!"ACTIVA".equals(d.estadoAutorizacion())) {
            motivo = "La autorización del estudiante fue retirada";
        } else if ("FINALIZADO".equals(d.estadoSesion())) {
            motivo = "La sesión ya finalizó";
        } else if ("SIN_INICIAR".equals(d.estadoSesion())) {
            motivo = "La sesión aún no ha sido iniciada por el docente";
        } else if (d.ahora().isAfter(limite)) {
            motivo = "Ingreso tardío: ya pasó el límite para iniciar la participación";
        }
        return new EstadoIngresoDto(motivo == null, motivo, limite, d.ahora(), d.estadoSesion());
    }

    public void exigirPermitido(Long idAutorizacion) {
        EstadoIngresoDto estado = consultar(idAutorizacion);
        if (!estado.permitido()) {
            throw new ReglaNegocioException(estado.motivo());
        }
    }
}
