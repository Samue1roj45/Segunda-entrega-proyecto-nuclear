package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.CatalogoImportado;
import org.slf4j.Logger;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Al arrancar deja cargado el banco de preguntas real de «Ecos de una decisión» (idempotente por casoVersion). */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.ecos.catalogo-inicial", havingValue = "true", matchIfMissing = true)
class CargaCatalogoInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaCatalogoInicial.class);

    private final ServicioEcos ecos;
    private final ObjectMapper json;

    CargaCatalogoInicial(ServicioEcos ecos, ObjectMapper json) {
        this.ecos = ecos;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (InputStream in = new ClassPathResource("ecos/catalogo-caso1.json").getInputStream()) {
            boolean creado = ecos.importarCatalogo(json.readValue(in, CatalogoImportado.class));
            if (creado) log.info("Catálogo de Ecos de una decisión cargado.");
        } catch (Exception e) {
            log.warn("No se pudo cargar el catálogo inicial de Ecos: {}", e.getMessage());
        }
    }
}
