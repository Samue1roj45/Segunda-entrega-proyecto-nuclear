package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.*;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Contrato ecos-soma/1. Lo que usa la persona (crear sesión, ver intentos) va con el JWT web; lo que usa el juego
 * (canje, eventos, resultado) va con el código de sesión o con el token del canje, nunca con el JWT web.
 */
@RestController
@RequestMapping("/api/ecos/v1")
public class EcosControlador {

    private final ServicioEcos ecos;
    private final ServicioAutorizacion autorizacion;

    public EcosControlador(ServicioEcos ecos, ServicioAutorizacion autorizacion) {
        this.ecos = ecos;
        this.autorizacion = autorizacion;
    }

    // ---- Estudiante (JWT web) ----

    @PostMapping("/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionCreada crearSesion() {
        return ecos.crearSesion(autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS).id());
    }

    @GetMapping("/mis-intentos")
    public EstadoEstudiante misIntentos() {
        return ecos.estado(autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS).id());
    }

    // ---- Docente (JWT web): sus estudiantes, su tablero en vivo y sus resultados ----

    private Long docente() {
        return autorizacion.exigirPermiso(Permiso.INVITAR_ESTUDIANTES).id();
    }

    @GetMapping("/mis-estudiantes")
    public java.util.List<EstudianteDocente> misEstudiantes() {
        return ecos.estudiantesDelDocente(docente());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/mis-estudiantes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable long id) {
        ecos.retirarEstudiante(docente(), id);
    }

    @PostMapping("/mis-estudiantes/{id}/repeticion")
    public EstudianteDocente repeticion(@PathVariable long id) {
        return ecos.concederRepeticion(docente(), id);
    }

    @GetMapping("/en-vivo")
    public java.util.List<EnVivo> enVivo() {
        return ecos.enVivo(docente());
    }

    @GetMapping("/resultados")
    public java.util.List<ResultadoDocente> resultadosDelDocente() {
        return ecos.resultadosDelDocente(docente());
    }

    // ---- Administrador (JWT web) ----

    @PostMapping("/catalogo")
    public ResponseEntity<Map<String, Object>> cargarCatalogo(@RequestBody CatalogoImportado catalogo) {
        autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        boolean creado = ecos.importarCatalogo(catalogo);
        return ResponseEntity.status(creado ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of("casoVersion", catalogo.casoVersion().toLowerCase(), "creado", creado));
    }

    // ---- Juego (sin JWT web) ----

    @PostMapping("/sesiones/{codigo}/canje")
    public RespuestaCanje canje(@PathVariable String codigo) {
        return ecos.canjear(codigo);
    }

    @PostMapping("/sesiones/{sesionId}/eventos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void evento(@PathVariable String sesionId, @RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestBody EventoJuego evento) {
        ecos.registrarEvento(sesionId, auth, evento);
    }

    @PostMapping("/sesiones/{sesionId}/resultado")
    public ResponseEntity<ResultadoRecibido> resultado(@PathVariable String sesionId,
                                                       @RequestHeader(value = "Authorization", required = false) String auth,
                                                       @RequestBody EnvioResultado envio) {
        ServicioEcos.Recepcion r = ecos.recibirResultado(sesionId, auth, envio);
        return ResponseEntity.status(r.creado() ? HttpStatus.CREATED : HttpStatus.OK).body(r.resultado());
    }
}
