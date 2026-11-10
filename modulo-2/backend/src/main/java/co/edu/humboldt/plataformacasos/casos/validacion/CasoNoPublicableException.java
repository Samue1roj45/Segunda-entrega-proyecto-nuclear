package co.edu.humboldt.plataformacasos.casos.validacion;

/** El caso incumple reglas del DSL-CEPS: no se publica y permanece en BORRADOR (RF-09). */
public class CasoNoPublicableException extends RuntimeException {

    private final transient InformeValidacion informe;

    public CasoNoPublicableException(InformeValidacion informe) {
        super("El caso no se puede publicar: incumple " + informe.incumplimientos().size() + " regla(s).");
        this.informe = informe;
    }

    public InformeValidacion informe() {
        return informe;
    }
}
