package com.unihumboldt.nuclear.evaluacion.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ReporteExcelPoi {

    private static final String[] COLUMNAS = {
            "participacionId", "resultadoId", "practicaId", "practicaNombre",
            "estudianteId", "estudianteCorreo", "estudianteNombre",
            "numeroIntento", "estado", "clasificacion",
            "notaFinal", "pesoObtenido", "pesoTotal", "escalaMaxima",
            "correctas", "incorrectas", "noRespondidas", "inicio", "fin",
            "oficial", "notaOficial"
    };

    private ReporteExcelPoi() {
    }

    public static byte[] generar(List<FilaReporteEvaluacion> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("evaluacion");
            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < COLUMNAS.length; i++) {
                encabezado.createCell(i).setCellValue(COLUMNAS[i]);
            }
            int filaIdx = 1;
            for (FilaReporteEvaluacion fila : filas) {
                Row row = hoja.createRow(filaIdx++);
                int c = 0;
                setNum(row, c++, fila.participacionId());
                setNum(row, c++, fila.resultadoId());
                setNum(row, c++, fila.practicaId());
                setTxt(row, c++, fila.practicaNombre());
                setNum(row, c++, fila.estudianteId());
                setTxt(row, c++, fila.estudianteCorreo());
                setTxt(row, c++, fila.estudianteNombre());
                row.createCell(c++).setCellValue(fila.numeroIntento());
                setTxt(row, c++, fila.estado().name());
                setTxt(row, c++, fila.clasificacion());
                if (fila.notaFinal() != null) {
                    row.createCell(c++).setCellValue(fila.notaFinal().doubleValue());
                } else {
                    row.createCell(c++).setBlank();
                }
                setNum(row, c++, fila.pesoObtenido() == null ? null : fila.pesoObtenido().longValue());
                setNum(row, c++, fila.pesoTotal() == null ? null : fila.pesoTotal().longValue());
                setNum(row, c++, fila.escalaMaxima() == null ? null : fila.escalaMaxima().longValue());
                setNum(row, c++, fila.correctas() == null ? null : fila.correctas().longValue());
                setNum(row, c++, fila.incorrectas() == null ? null : fila.incorrectas().longValue());
                setNum(row, c++, fila.noRespondidas() == null ? null : fila.noRespondidas().longValue());
                setTxt(row, c++, fila.inicio() == null ? null : fila.inicio().toString());
                setTxt(row, c++, fila.fin() == null ? null : fila.fin().toString());
                setTxt(row, c++, fila.oficial() ? "SI" : "NO");
                if (fila.notaOficial() != null) {
                    row.createCell(c++).setCellValue(fila.notaOficial().doubleValue());
                } else {
                    row.createCell(c++).setBlank();
                }
            }
            libro.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel.", ex);
        }
    }

    private static void setTxt(Row row, int col, String valor) {
        if (valor == null) {
            row.createCell(col).setBlank();
        } else {
            row.createCell(col).setCellValue(valor);
        }
    }

    private static void setNum(Row row, int col, Long valor) {
        if (valor == null) {
            row.createCell(col).setBlank();
        } else {
            row.createCell(col).setCellValue(valor);
        }
    }
}
