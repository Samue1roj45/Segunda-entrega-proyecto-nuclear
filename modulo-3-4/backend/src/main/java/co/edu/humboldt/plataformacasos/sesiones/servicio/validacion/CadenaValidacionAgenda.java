package co.edu.humboldt.plataformacasos.sesiones.servicio.validacion;

import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import org.springframework.stereotype.Component;

/*
 #REQUISITO RF-04 || RF-18
 Patrón: CHAIN OF RESPONSIBILITY (armado de la cadena)
 Para qué sirve: único lugar donde se define el ORDEN de las validaciones. Al agendar se revisan
 condiciones y estudiantes; al cambiar las condiciones de una sesión ya agendada, solo las condiciones.
 */
@Component
public class CadenaValidacionAgenda {

    private final ValidadorAgenda agenda;
    private final ValidadorAgenda condiciones;

    public CadenaValidacionAgenda() {
        ValidadorAgenda espacio = new ValidadorEspacioAcademico();
        espacio.siguiente(new ValidadorFechas())
               .siguiente(new ValidadorTiempoMaximo())
               .siguiente(new ValidadorEstudiantes());
        this.agenda = espacio;

        ValidadorAgenda soloCondiciones = new ValidadorEspacioAcademico();
        soloCondiciones.siguiente(new ValidadorFechas())
                       .siguiente(new ValidadorTiempoMaximo());
        this.condiciones = soloCondiciones;
    }

    public void validar(SesionProgramada sesion) {
        agenda.validar(sesion);
    }

    public void validarCondiciones(SesionProgramada sesion) {
        condiciones.validar(sesion);
    }
}
