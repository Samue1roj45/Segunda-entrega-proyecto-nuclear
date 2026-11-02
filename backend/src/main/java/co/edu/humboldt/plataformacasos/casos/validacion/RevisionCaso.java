package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Solicitud que recorre la cadena de reglas: el caso revisado y lo que cada eslabón va encontrando. */
final class RevisionCaso {

    private final CasoEstudio caso;
    private final List<Incumplimiento> incumplimientos = new ArrayList<>();

    RevisionCaso(CasoEstudio caso) {
        this.caso = caso;
    }

    CasoEstudio caso() {
        return caso;
    }

    void registrar(Incumplimiento incumplimiento) {
        incumplimientos.add(incumplimiento);
    }

    List<Incumplimiento> incumplimientos() {
        return Collections.unmodifiableList(incumplimientos);
    }
}
