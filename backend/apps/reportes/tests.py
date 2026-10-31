from django.core import mail
from django.test import TestCase, override_settings
from rest_framework import status

from apps.resultados.test_fixtures import (
    autorizar,
    cliente,
    crear_caso_con_rubrica,
    crear_docente,
    crear_estudiante,
    crear_practica,
    token_estudiante,
    token_usuario,
)


@override_settings(EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
class ReportesRF16Tests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.otro = crear_docente('otroDoc', 'otrodoc@uni.edu')
        self.estudiante = crear_estudiante(self.docente)
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth = autorizar(self.practica, self.estudiante)
        cli = cliente(token_estudiante(self.estudiante, self.auth))
        pid = cli.post('/api/participaciones/iniciar/').data['id']
        cli.post(f'/api/participaciones/{pid}/finalizar/')
        self.cli_doc = cliente(token_usuario(self.docente))

    def test_rf16_genera_pdf_excel_y_json(self):
        pdf = self.cli_doc.get(f'/api/reportes/practica/{self.practica.id}/pdf/')
        xls = self.cli_doc.get(f'/api/reportes/practica/{self.practica.id}/excel/')
        js = self.cli_doc.get(f'/api/reportes/practica/{self.practica.id}/')
        self.assertEqual(pdf.status_code, status.HTTP_200_OK)
        self.assertTrue(pdf.content.startswith(b'%PDF'))
        self.assertEqual(xls.status_code, status.HTTP_200_OK)
        self.assertGreater(len(xls.content), 0)
        self.assertEqual(js.status_code, status.HTTP_200_OK)
        self.assertTrue(js.data['filas'])
        estados = {f['estado'] for f in js.data['filas']}
        self.assertIn('FINALIZADA', estados)

    def test_rf16_docente_ajeno_no_descarga(self):
        cli_ajeno = cliente(token_usuario(self.otro))
        pdf = cli_ajeno.get(f'/api/reportes/practica/{self.practica.id}/pdf/')
        self.assertEqual(pdf.status_code, status.HTTP_403_FORBIDDEN)
