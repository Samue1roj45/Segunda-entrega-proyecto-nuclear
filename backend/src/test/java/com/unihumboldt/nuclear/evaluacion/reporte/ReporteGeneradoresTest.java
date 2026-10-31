package com.unihumboldt.nuclear.evaluacion.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ReporteGeneradoresTest {

    @Test
    void pdfVacioSigueSiendoPdf() {
        byte[] pdf = ReportePdfOpenPdf.generar(List.of());
        assertTrue(pdf.length > 4);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
        assertTrue(new String(pdf).contains("Sin registros"));
    }

    @Test
    void excelVacioSoloEncabezado() throws Exception {
        byte[] xlsx = ReporteExcelPoi.generar(List.of());
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheet("evaluacion");
            assertEquals(0, hoja.getLastRowNum());
            assertEquals("clasificacion", hoja.getRow(0).getCell(9).getStringCellValue());
        }
    }
}
