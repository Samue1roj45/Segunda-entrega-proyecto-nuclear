package co.edu.humboldt.plataformacasos.evaluacion.contrato;

import java.util.Optional;

// Patrón Puerto (Ports & Adapters): M5 pide el actor autenticado; el adaptador ActorDesdeSesion lo resuelve desde el JWT de M1.
/** Puerto: quién hace la petición. Lo implementa la aplicación con la sesión del módulo 1. */
public interface ActorActualResolver {

    Optional<ActorActual> actual();
}
