package co.edu.humboldt.plataformacasos.compartido.excepcion;

/** La operación choca con datos existentes (por ejemplo, un correo ya registrado): se responde 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
