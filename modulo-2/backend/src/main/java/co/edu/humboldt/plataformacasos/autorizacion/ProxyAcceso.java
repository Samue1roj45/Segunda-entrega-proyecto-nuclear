package co.edu.humboldt.plataformacasos.autorizacion;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Patrón Proxy (RF-02): se interpone entre quien llama y el servicio real. Antes de delegar
 * verifica con {@link ServicioAutorizacion} el permiso declarado con {@link RequierePermiso}.
 *
 * <p>Seguro por defecto: si algún método de la interfaz no declara su permiso, el proxy no se crea.
 * Cualquier módulo puede proteger su servicio con {@code ProxyAcceso.proteger(...)}.</p>
 */
public final class ProxyAcceso implements InvocationHandler {

    private final Object servicioReal;
    private final ServicioAutorizacion autorizacion;

    private ProxyAcceso(Object servicioReal, ServicioAutorizacion autorizacion) {
        this.servicioReal = servicioReal;
        this.autorizacion = autorizacion;
    }

    public static <T> T proteger(Class<T> interfazServicio, T servicioReal, ServicioAutorizacion autorizacion) {
        Objects.requireNonNull(servicioReal, "servicioReal");
        Objects.requireNonNull(autorizacion, "autorizacion");
        if (!interfazServicio.isInterface()) {
            throw new IllegalArgumentException(interfazServicio.getName() + " debe ser una interfaz");
        }
        for (Method metodo : interfazServicio.getMethods()) {
            if (!metodo.isDefault() && metodo.getAnnotation(RequierePermiso.class) == null) {
                throw new IllegalStateException("El método " + interfazServicio.getSimpleName() + "."
                        + metodo.getName() + " no declara @RequierePermiso");
            }
        }
        Object proxy = Proxy.newProxyInstance(interfazServicio.getClassLoader(),
                new Class<?>[]{interfazServicio}, new ProxyAcceso(servicioReal, autorizacion));
        return interfazServicio.cast(proxy);
    }

    @Override
    public Object invoke(Object proxy, Method metodo, Object[] argumentos) throws Throwable {
        if (metodo.getDeclaringClass() == Object.class) {
            return metodo.invoke(servicioReal, argumentos);
        }
        autorizacion.exigirPermiso(metodo.getAnnotation(RequierePermiso.class).value());
        try {
            return metodo.invoke(servicioReal, argumentos);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
