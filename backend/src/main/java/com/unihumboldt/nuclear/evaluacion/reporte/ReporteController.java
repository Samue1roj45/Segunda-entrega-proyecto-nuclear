package com.unihumboldt.nuclear.evaluacion.reporte;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final DescargaReporte descarga;

    public ReporteController(DescargaReporte descarga) {
        this.descarga = descarga;
    }

    @GetMapping("/evaluacion.pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante) {
        return archivo(descarga.pdf(practica, estudiante));
    }

    @GetMapping("/evaluacion.xlsx")
    public ResponseEntity<byte[]> excel(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante) {
        return archivo(descarga.excel(practica, estudiante));
    }

    private ResponseEntity<byte[]> archivo(ArchivoReporte reporte) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(reporte.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(reporte.nombreArchivo()).build());
        headers.setContentLength(reporte.contenido().length);
        return ResponseEntity.ok().headers(headers).body(reporte.contenido());
    }
}
