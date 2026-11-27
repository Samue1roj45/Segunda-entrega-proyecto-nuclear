package co.edu.humboldt.plataformacasos.invitaciones;

import org.springframework.web.util.HtmlUtils;

/** Correo HTML con la imagen de SOMA (tabla + estilos en línea: es lo que soportan los clientes de correo). */
final class PlantillaCorreo {

    private PlantillaCorreo() { }

    /** @param titulo encabezado grande; @param parrafos texto plano (se escapa); @param textoBoton y @param enlace el botón principal. */
    static String html(String titulo, java.util.List<String> parrafos, String textoBoton, String enlace, String nota) {
        StringBuilder cuerpo = new StringBuilder();
        for (String p : parrafos) {
            cuerpo.append("<p style=\"margin:0 0 14px;font-size:16px;line-height:1.6;color:#3d3358\">").append(HtmlUtils.htmlEscape(p)).append("</p>");
        }
        String url = HtmlUtils.htmlEscape(enlace);
        return """
                <!DOCTYPE html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#ece6f7;font-family:'Segoe UI',Helvetica,Arial,sans-serif">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#ece6f7"><tr><td align="center" style="padding:32px 14px">
                  <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%">
                    <tr><td style="background:linear-gradient(135deg,#4b3a7a,#7a62b8);background-color:#5b4a8c;border-radius:20px 20px 0 0;padding:30px 36px;text-align:left">
                      <div style="font-size:30px;font-weight:700;letter-spacing:.14em;color:#ffffff">SOMA</div>
                      <div style="font-size:12px;letter-spacing:.12em;text-transform:uppercase;color:#d9cff2;margin-top:4px">Simulaciones para el aprendizaje en psicología social</div>
                    </td></tr>
                    <tr><td style="background:#ffffff;padding:34px 36px 30px;border-radius:0 0 20px 20px;box-shadow:0 12px 30px rgba(75,58,122,.15)">
                      <h1 style="margin:0 0 18px;font-family:Georgia,'Times New Roman',serif;font-size:28px;font-weight:600;color:#2f2552">%s</h1>
                      %s
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:26px 0 22px"><tr><td style="border-radius:999px;background:#5b4a8c">
                        <a href="%s" style="display:inline-block;padding:14px 34px;font-size:16px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:999px">%s</a>
                      </td></tr></table>
                      <p style="margin:0 0 6px;font-size:13px;color:#7b7092">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                      <p style="margin:0 0 22px;font-size:13px;word-break:break-all"><a href="%s" style="color:#5b4a8c">%s</a></p>
                      <hr style="border:none;border-top:1px solid #e6def5;margin:0 0 16px">
                      <p style="margin:0;font-size:13px;line-height:1.5;color:#7b7092">%s</p>
                    </td></tr>
                    <tr><td style="padding:18px 10px;text-align:center;font-size:12px;color:#8d83a6">Plataforma SOMA · Corporación Universitaria Alexander von Humboldt</td></tr>
                  </table>
                </td></tr></table></body></html>
                """.formatted(HtmlUtils.htmlEscape(titulo), cuerpo, url, HtmlUtils.htmlEscape(textoBoton), url, url, HtmlUtils.htmlEscape(nota));
    }
}
