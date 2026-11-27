package co.edu.humboldt.plataformacasos.configuracion;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Viste con la imagen de SOMA los correos que la aplicación arma solo en texto plano (avisos de la campana,
 * convocatorias con código de acceso y notificaciones). Tablas y estilos en línea: es lo que soportan Gmail y Outlook.
 * Si el texto trae un código de acceso, se muestra destacado en una tarjeta para copiarlo fácil.
 */
final class PlantillaCorreoTexto {

    /** "... el código de acceso: ABCD1234" (final del correo de convocatoria). */
    private static final Pattern CODIGO = Pattern.compile("(?i)c[oó]digo de acceso:\\s*([A-Za-z0-9_-]{4,64})\\s*\\.?\\s*$");
    private static final String FIRMA = "— Plataforma SOMA";

    private PlantillaCorreoTexto() { }

    static String html(String asunto, String texto, String urlPlataforma) {
        String titulo = asunto == null ? "SOMA" : asunto.replaceFirst("^SOMA\\s*·\\s*", "").trim();
        if (titulo.isEmpty()) titulo = "SOMA";

        String limpio = texto == null ? "" : texto.replace("\r\n", "\n").trim();
        if (limpio.endsWith(FIRMA)) limpio = limpio.substring(0, limpio.length() - FIRMA.length()).trim();

        String codigo = null;
        Matcher m = CODIGO.matcher(limpio);
        if (m.find()) {
            codigo = m.group(1);
            String antes = limpio.substring(0, m.start());
            limpio = antes.isBlank() ? "Tu código de acceso aparece abajo."
                    : (antes + m.group().substring(0, m.group().indexOf(':')) + " que aparece abajo.").trim();
        }

        StringBuilder parrafos = new StringBuilder();
        for (String p : partir(limpio)) {
            parrafos.append("<p style=\"margin:0 0 14px;font-size:16px;line-height:1.65;color:#3d3358\">")
                    .append(esc(p).replace("\n", "<br>")).append("</p>");
        }

        String bloqueCodigo = codigo == null ? "" : """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:22px 0 8px"><tr>
                  <td align="center" style="background:#f4f0fb;border:2px dashed #b9a8e3;border-radius:16px;padding:22px 16px">
                    <div style="font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:#7b6aa8;margin-bottom:8px">Tu código de acceso</div>
                    <div style="font-family:'Courier New',Consolas,monospace;font-size:32px;font-weight:700;letter-spacing:.22em;color:#2f2552">%s</div>
                    <div style="font-size:12px;color:#8d83a6;margin-top:10px">Es personal: no lo compartas con nadie.</div>
                  </td></tr></table>
                """.formatted(esc(codigo));

        String boton = (urlPlataforma == null || urlPlataforma.isBlank()) ? "" : """
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:24px 0 6px"><tr><td style="border-radius:999px;background:#5b4a8c">
                  <a href="%s" style="display:inline-block;padding:14px 34px;font-size:16px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:999px">Ir a SOMA</a>
                </td></tr></table>
                """.formatted(esc(urlPlataforma.trim()));

        return """
                <!DOCTYPE html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#ece6f7;font-family:'Segoe UI',Helvetica,Arial,sans-serif">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0">%s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#ece6f7"><tr><td align="center" style="padding:32px 14px">
                  <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%">
                    <tr><td style="background:linear-gradient(135deg,#4b3a7a,#7a62b8);background-color:#5b4a8c;border-radius:20px 20px 0 0;padding:28px 36px;text-align:left">
                      <div style="font-size:30px;font-weight:700;letter-spacing:.14em;color:#ffffff">SOMA</div>
                      <div style="font-size:12px;letter-spacing:.12em;text-transform:uppercase;color:#d9cff2;margin-top:4px">Simulaciones para el aprendizaje en psicología social</div>
                    </td></tr>
                    <tr><td style="background:#ffffff;padding:34px 36px 30px;border-radius:0 0 20px 20px">
                      <h1 style="margin:0 0 18px;font-family:Georgia,'Times New Roman',serif;font-size:26px;font-weight:600;line-height:1.3;color:#2f2552">%s</h1>
                      %s%s%s
                      <hr style="border:none;border-top:1px solid #e6def5;margin:22px 0 14px">
                      <p style="margin:0;font-size:13px;line-height:1.5;color:#7b7092">Recibes este correo porque tienes una cuenta en SOMA. Si no esperabas este mensaje, puedes ignorarlo.</p>
                    </td></tr>
                    <tr><td style="padding:18px 10px;text-align:center;font-size:12px;color:#8d83a6">Plataforma SOMA · Corporación Universitaria Alexander von Humboldt</td></tr>
                  </table>
                </td></tr></table></body></html>
                """.formatted(esc(titulo), esc(resumen(limpio)), esc(titulo),
                parrafos, bloqueCodigo, boton);
    }

    /** Escapa solo lo necesario (&lt; &gt; &amp; comillas) y deja tildes y eñes tal cual. */
    private static String esc(String valor) {
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    /** Párrafos separados por líneas en blanco (o por línea si no hay ninguna en blanco). */
    private static List<String> partir(String texto) {
        List<String> salida = new ArrayList<>();
        if (texto.isBlank()) return salida;
        String[] bloques = texto.contains("\n\n") ? texto.split("\n\\s*\n") : texto.split("\n");
        for (String b : bloques) {
            if (!b.isBlank()) salida.add(b.trim());
        }
        return salida;
    }

    /** Texto de vista previa que muestran las bandejas de entrada junto al asunto. */
    private static String resumen(String texto) {
        String una = texto.replaceAll("\\s+", " ").trim();
        return una.length() > 110 ? una.substring(0, 107) + "..." : una;
    }
}
