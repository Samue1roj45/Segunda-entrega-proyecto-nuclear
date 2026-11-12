package co.edu.humboldt.plataformacasos.evaluacion.contrato;

import java.util.Optional;

/** Puerto: quién hace la petición. Lo implementa la aplicación con la sesión del módulo 1. */
public interface ActorActualResolver {

    Optional<ActorActual> actual();
}
