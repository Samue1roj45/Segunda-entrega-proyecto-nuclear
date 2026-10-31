"""Facade de reportes de prácticas (RF-16)."""

from __future__ import annotations

import io

from openpyxl import Workbook
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from apps.participaciones.models import Participacion
from apps.practicas.models import AutorizacionEstudiante, Practica
from apps.resultados.proxy import AccesoEvaluacionProxy


class ReporteFacade:
    def __init__(self) -> None:
        self.proxy = AccesoEvaluacionProxy()

    def filas(self, user, practica_id: int, filtros: dict | None = None) -> tuple[Practica, list[dict]]:
        practica = self.proxy.asegurar_practica_docente(user, practica_id)
        filtros = filtros or {}
        autorizaciones = AutorizacionEstudiante.objects.filter(
            practica=practica,
        ).select_related('estudiante')
        participaciones = list(
            Participacion.objects.filter(practica=practica).select_related(
                'estudiante', 'resultado', 'autorizacion',
            )
        )
        if filtros.get('estado'):
            participaciones = [p for p in participaciones if p.estado == filtros['estado']]
        if filtros.get('estudiante'):
            est_id = int(filtros['estudiante'])
            participaciones = [p for p in participaciones if p.estudiante_id == est_id]
            autorizaciones = autorizaciones.filter(estudiante_id=est_id)

        filas = []
        cubiertos = set()
        for part in sorted(participaciones, key=lambda p: (p.estudiante.correo, p.numero_intento)):
            cubiertos.add(part.autorizacion_id)
            resultado = getattr(part, 'resultado', None)
            filas.append(self._fila_participacion(practica, part, resultado))

        if not filtros.get('estado') or filtros.get('estado') == Participacion.Estado.NO_INICIADA:
            for auth in autorizaciones:
                if auth.id in cubiertos:
                    continue
                filas.append({
                    'estudiante': auth.estudiante.nombre_completo,
                    'correo': auth.estudiante.correo,
                    'practica': practica.nombre,
                    'estado': 'AUSENCIA',
                    'numero_intento': None,
                    'inicio': None,
                    'fin': None,
                    'duracion_seg': None,
                    'nota': None,
                    'feedback': '',
                })
        return practica, filas

    def _fila_participacion(self, practica, part, resultado) -> dict:
        etiqueta = part.estado
        if part.estado == Participacion.Estado.NO_INICIADA:
            etiqueta = 'AUSENCIA' if resultado is not None else 'NO_INICIADA'
        return {
            'estudiante': part.estudiante.nombre_completo,
            'correo': part.estudiante.correo,
            'practica': practica.nombre,
            'estado': etiqueta,
            'numero_intento': part.numero_intento,
            'inicio': part.inicio.isoformat() if part.inicio else None,
            'fin': part.fin.isoformat() if part.fin else None,
            'duracion_seg': part.tiempo_usado_seg,
            'nota': str(resultado.nota_final) if resultado else None,
            'feedback': resultado.feedback_docente if resultado else '',
        }

    def json(self, user, practica_id: int, filtros: dict | None = None) -> dict:
        practica, filas = self.filas(user, practica_id, filtros)
        return {
            'practica_id': practica.id,
            'practica': practica.nombre,
            'caso': practica.caso.nombre,
            'filas': filas,
        }

    def pdf_bytes(self, user, practica_id: int, filtros: dict | None = None) -> bytes:
        practica, filas = self.filas(user, practica_id, filtros)
        buf = io.BytesIO()
        doc = SimpleDocTemplate(buf, pagesize=letter, title=f'Reporte {practica.nombre}')
        styles = getSampleStyleSheet()
        elements = [
            Paragraph(f'<b>Reporte de práctica</b>: {practica.nombre}', styles['Title']),
            Paragraph(
                f'Caso: {practica.caso.nombre} — Docente: {practica.docente.get_full_name() or practica.docente.username}',
                styles['Normal'],
            ),
            Paragraph(f'Registros: {len(filas)}', styles['Normal']),
            Spacer(1, 12),
        ]
        data = [['Estudiante', 'Correo', 'Estado', 'Intento', 'Nota', 'Inicio', 'Fin']]
        for fila in filas:
            data.append([
                fila['estudiante'],
                fila['correo'],
                fila['estado'],
                fila['numero_intento'] if fila['numero_intento'] is not None else '-',
                fila['nota'] if fila['nota'] is not None else '-',
                (fila['inicio'] or '-')[:19],
                (fila['fin'] or '-')[:19],
            ])
        tbl = Table(data, repeatRows=1)
        tbl.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#283593')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, -1), 8),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f0f0f0')]),
        ]))
        elements.append(tbl)
        doc.build(elements)
        return buf.getvalue()

    def excel_bytes(self, user, practica_id: int, filtros: dict | None = None) -> bytes:
        practica, filas = self.filas(user, practica_id, filtros)
        wb = Workbook()
        ws = wb.active
        ws.title = 'Resultados'
        ws.append([
            'Estudiante', 'Correo', 'Práctica', 'Estado', 'Intento',
            'Inicio', 'Fin', 'Duración (s)', 'Nota', 'Retroalimentación',
        ])
        for fila in filas:
            ws.append([
                fila['estudiante'],
                fila['correo'],
                fila['practica'],
                fila['estado'],
                fila['numero_intento'],
                fila['inicio'],
                fila['fin'],
                fila['duracion_seg'],
                float(fila['nota']) if fila['nota'] is not None else None,
                fila['feedback'],
            ])
        buf = io.BytesIO()
        wb.save(buf)
        return buf.getvalue()
