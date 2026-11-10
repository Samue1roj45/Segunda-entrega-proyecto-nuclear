package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.puerto.VerificadorCodigoAutorizacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.Validador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma las cadenas de RF-01 en un solo lugar. Cambiar el orden o agregar una regla es
 * tocar este método, no el servicio (Chain of Responsibility).
 */
@Configuration
public class CadenasAutenticacionConfig {

    /** Errores de formato: se responden con 400 y un mensaje específico. */
    @Bean
    Validador<SolicitudAutenticacion> validacionFormatoAutenticacion(
            @Value("${app.autenticacion.dominio-institucional}") String dominioInstitucional) {
        Validador<SolicitudAutenticacion> cadena = new ValidadorCamposObligatorios();
        cadena.establecerSiguiente(new ValidadorDominioInstitucional(dominioInstitucional));
        return cadena;
    }

    /** Errores de credenciales: se responden con 401 y un mensaje genérico. */
    @Bean
    Validador<IntentoAutenticacion> validacionCredencialesEstudiante(VerificadorCodigoAutorizacion verificador) {
        Validador<IntentoAutenticacion> cadena = new ValidadorEstudianteRegistrado();
        cadena.establecerSiguiente(new ValidadorCodigoAutorizacion(verificador));
        return cadena;
    }
}
