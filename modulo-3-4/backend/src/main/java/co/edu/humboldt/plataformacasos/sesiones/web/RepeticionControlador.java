package co.edu.humboldt.plataformacasos.sesiones.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dto.AutorizarRepeticionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.IntentosDto;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioRepeticion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-25
 Para qué sirve: API REST del DOCENTE para conceder un intento adicional a un estudiante
 (repetición excepcional) y ver cuántos intentos lleva. Exige el permiso AUTORIZAR_REPETICION y,
 para conceder, ser el docente creador del caso.
 */
@RestController
@RequestMapping("/api/autorizaciones")
public class RepeticionControlador {

    private final ServicioRepeticion service;
    private final ServicioAutorizacion autorizacion;

    public RepeticionControlador(ServicioRepeticion service, ServicioAutorizacion autorizacion) {
        this.service = service;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/{idAutorizacion}/repeticiones")
    public ResponseEntity<IntentosDto> autorizar(@PathVariable Long idAutorizacion,
                                                 @Valid @RequestBody(required = false) AutorizarRepeticionRequest req) {
        String motivo = (req == null) ? null : req.motivo();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.autorizar(idAutorizacion, docente(), motivo));
    }

    @GetMapping("/{idAutorizacion}/intentos")
    public IntentosDto intentos(@PathVariable Long idAutorizacion) {
        return service.consultar(idAutorizacion, docente());
    }

    private Long docente() {
        return autorizacion.exigirPermiso(Permiso.AUTORIZAR_REPETICION).id();
    }
}
