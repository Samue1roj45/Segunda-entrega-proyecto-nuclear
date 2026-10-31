package com.unihumboldt.nuclear.evaluacion.reporte;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * PDF con OpenPDF 2.0.3 (MPL-2.0 / LGPL-2.1+). El equipo no exigió licencia Apache exclusiva.
 */
public final class ReportePdfOpenPdf {

    private static final String[] COLUMNAS = {
            "Practica", "Estudiante", "Intento", "Estado", "Clasificacion",
            "Nota intento", "Oficial", "Nota oficial",
            "Peso obt.", "Peso tot.", "Escala", "Correctas", "Incorrectas", "Sin resp."
    };

    private ReportePdfOpenPdf() {
    }

    public static byte[] generar(List<FilaReporteEvaluacion> filas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCompressionLevel(0);
            document.open();
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            document.add(new Paragraph("Reporte de evaluacion — participaciones y resultados", titulo));
            document.add(new Paragraph(
                    "Cada fila es un intento. Nota intento no se combina. Oficial = SI marca la nota elegida por el docente.",
                    normal));
            document.add(new Paragraph(" "));
            if (filas.isEmpty()) {
                document.add(new Paragraph("Sin registros visibles para el actor autenticado.", normal));
            } else {
                PdfPTable tabla = new PdfPTable(COLUMNAS.length);
                tabla.setWidthPercentage(100);
                for (String col : COLUMNAS) {
                    tabla.addCell(celda(col, true));
                }
                for (FilaReporteEvaluacion fila : filas) {
                    tabla.addCell(celda(texto(fila.practicaNombre(), fila.practicaId()), false));
                    tabla.addCell(celda(texto(fila.estudianteCorreo(), fila.estudianteId()), false));
                    tabla.addCell(celda(String.valueOf(fila.numeroIntento()), false));
                    tabla.addCell(celda(fila.estado().name(), false));
                    tabla.addCell(celda(fila.clasificacion(), false));
                    tabla.addCell(celda(fila.notaFinal() == null ? "-" : fila.notaFinal().toPlainString(), false));
                    tabla.addCell(celda(fila.oficial() ? "SI" : "NO", false));
                    tabla.addCell(celda(fila.notaOficial() == null ? "-" : fila.notaOficial().toPlainString(), false));
                    tabla.addCell(celda(entero(fila.pesoObtenido()), false));
                    tabla.addCell(celda(entero(fila.pesoTotal()), false));
                    tabla.addCell(celda(entero(fila.escalaMaxima()), false));
                    tabla.addCell(celda(entero(fila.correctas()), false));
                    tabla.addCell(celda(entero(fila.incorrectas()), false));
                    tabla.addCell(celda(entero(fila.noRespondidas()), false));
                }
                document.add(tabla);
            }
            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF.", ex);
        }
        return out.toByteArray();
    }

    private static PdfPCell celda(String valor, boolean encabezado) {
        Font font = FontFactory.getFont(encabezado ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 8);
        return new PdfPCell(new Phrase(valor, font));
    }

    private static String texto(String preferido, Long id) {
        if (preferido != null && !preferido.isBlank()) {
            return preferido;
        }
        return id == null ? "-" : String.valueOf(id);
    }

    private static String entero(Integer valor) {
        return valor == null ? "-" : String.valueOf(valor);
    }
}
