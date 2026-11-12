package co.edu.humboldt.plataformacasos.evaluacion.soporte;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActualResolver;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DOBLE DE PRUEBA del puerto ActorActualResolver. En la aplicación el actor sale del usuario del token
 * del módulo 1 leído de la base (integracion.ActorDesdeSesion); aquí sale de los claims del JWT de prueba
 * (ActorTokens), así las pruebas del módulo no necesitan usuarios reales.
 * Claims: rol (ESTUDIANTE, DOCENTE o ADMINISTRADOR), usuarioId, estudianteId, docenteId y, opcionalmente,
 * permisos. Sin el claim "permisos", el docente tiene todos los permisos asignables (RF-03).
 */
public class ActorDeClaimsJwt implements ActorActualResolver {

    @Override
    public Optional<ActorActual> actual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        try {
            Object rolClaim = jwt.getClaim("rol");
            Rol rol = Rol.valueOf(String.valueOf(rolClaim));
            Long usuarioId = numero(jwt.getClaim("usuarioId"));
            if (usuarioId == null) {
                usuarioId = numero(jwt.getSubject());
            }
            if (usuarioId == null) {
                return Optional.empty();
            }
            return Optional.of(new ActorActual(usuarioId, numero(jwt.getClaim("estudianteId")),
                    numero(jwt.getClaim("docenteId")), rol, permisos(jwt, rol)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Set<Permiso> permisos(Jwt jwt, Rol rol) {
        Object claim = jwt.getClaim("permisos");
        if (claim instanceof Collection<?> lista) {
            return lista.stream().map(Object::toString).map(Permiso::valueOf).collect(Collectors.toSet());
        }
        if (rol == Rol.DOCENTE) {
            return Arrays.stream(Permiso.values()).filter(Permiso::asignableADocente)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permiso.class)));
        }
        return rol.permisosBase();
    }

    private static Long numero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
