package co.edu.humboldt.plataformacasos.autorizacion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Permiso que exige {@link ProxyAcceso} antes de ejecutar el método de un servicio protegido. */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequierePermiso {

    Permiso value();
}
