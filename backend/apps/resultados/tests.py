from decimal import Decimal
from unittest.mock import patch

from django.core import mail
from django.test import TestCase, override_settings
from rest_framework import status

from apps.participaciones.models import Participacion
from apps.resultados.models import NotificacionEnvio, Resultado
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
class EvaluacionRF13RF14Tests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.estudiante = crear_estudiante(self.docente)
        self.caso = crear_caso_con_rubrica(self.docente, escala=100)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth = autorizar(self.practica, self.estudiante)
        self.client = cliente(token_estudiante(self.estudiante, self.auth))

    def _iniciar(self):
        return self.client.post('/api/participaciones/iniciar/').data['id']

    def test_rf13_calcula_nota_con_pesos_y_escala(self):
        pid = self._iniciar()
        self.client.post(
            f'/api/participaciones/{pid}/responder/',
            {'pregunta_id': self.caso['p1'].id, 'respuesta_id': self.caso['r1_ok'].id},
            format='json',
        )
        self.client.post(
            f'/api/participaciones/{pid}/responder/',
            {'pregunta_id': self.caso['p2'].id, 'respuesta_id': self.caso['r2_bad'].id},
            format='json',
        )
        fin = self.client.post(f'/api/participaciones/{pid}/finalizar/')
        resultado = Resultado.objects.get(pk=fin.data['resultado_id'])
        self.assertEqual(resultado.correctas, 1)
        self.assertEqual(resultado.incorrectas, 1)
        self.assertEqual(resultado.peso_obtenido, 2)
        self.assertEqual(resultado.peso_total, 4)
        self.assertEqual(resultado.nota_final, Decimal('50.00'))

    def test_rf13_no_acepta_nota_enviada_por_cliente(self):
        pid = self._iniciar()
        self.client.post(
            f'/api/participaciones/{pid}/responder/',
            {'pregunta_id': self.caso['p1'].id, 'respuesta_id': self.caso['r1_ok'].id},
            format='json',
        )
        self.client.post(f'/api/participaciones/{pid}/finalizar/')
        resultado = Resultado.objects.get(participacion_id=pid)
        cli_doc = cliente(token_usuario(self.docente))
        resp = cli_doc.patch(f'/api/resultados/{resultado.id}/', {'nota_final': '100.00'}, format='json')
        self.assertIn(resp.status_code, (status.HTTP_405_METHOD_NOT_ALLOWED, status.HTTP_403_FORBIDDEN))
        resultado.refresh_from_db()
        self.assertNotEqual(resultado.nota_final, Decimal('100.00'))

    def test_rf14_estudiante_ve_su_resultado_final(self):
        pid = self._iniciar()
        self.client.post(f'/api/participaciones/{pid}/finalizar/')
        resultado = Resultado.objects.get(participacion_id=pid)
        resp = self.client.get(f'/api/resultados/{resultado.id}/')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn('nota_final', resp.data)
        self.assertTrue(resp.data['detalle_preguntas'])

    def test_rf14_otro_estudiante_no_ve_resultado(self):
        pid = self._iniciar()
        self.client.post(f'/api/participaciones/{pid}/finalizar/')
        resultado = Resultado.objects.get(participacion_id=pid)
        otro = crear_estudiante(self.docente, 'otro@uni.edu', 'Otro', 'Est', 'estOtro')
        auth2 = autorizar(self.practica, otro)
        c2 = cliente(token_estudiante(otro, auth2))
        resp = c2.get(f'/api/resultados/{resultado.id}/')
        self.assertEqual(resp.status_code, status.HTTP_404_NOT_FOUND)


@override_settings(EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
class IncompletosAusenciasRF24Tests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.est_incompleto = crear_estudiante(self.docente, 'inc@uni.edu', 'In', 'Comp', 'estInc')
        self.est_ausente = crear_estudiante(self.docente, 'aus@uni.edu', 'Au', 'Sente', 'estAus')
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth_inc = autorizar(self.practica, self.est_incompleto)
        self.auth_aus = autorizar(self.practica, self.est_ausente)
        self.cli_doc = cliente(token_usuario(self.docente))

    def test_rf24_incompleto_parcial_y_ausencia_cero(self):
        cli = cliente(token_estudiante(self.est_incompleto, self.auth_inc))
        pid = cli.post('/api/participaciones/iniciar/').data['id']
        cli.post(
            f'/api/participaciones/{pid}/responder/',
            {'pregunta_id': self.caso['p1'].id, 'respuesta_id': self.caso['r1_ok'].id},
            format='json',
        )
        resp = self.cli_doc.post(f'/api/practicas/{self.practica.id}/evaluar-pendientes/')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['incompletas'], 1)
        self.assertEqual(resp.data['ausencias'], 1)

        part_inc = Participacion.objects.get(pk=pid)
        self.assertEqual(part_inc.estado, Participacion.Estado.INCOMPLETA)
        self.assertEqual(part_inc.resultado.nota_final, Decimal('50.00'))

        part_aus = Participacion.objects.get(
            autorizacion=self.auth_aus, estado=Participacion.Estado.NO_INICIADA,
        )
        self.assertEqual(part_aus.resultado.nota_final, Decimal('0.00'))

        segunda = self.cli_doc.post(f'/api/practicas/{self.practica.id}/evaluar-pendientes/')
        self.assertEqual(segunda.data['incompletas'], 0)
        self.assertEqual(segunda.data['ausencias'], 0)

    def test_rf24_no_confunde_incompleto_con_ausencia(self):
        cli = cliente(token_estudiante(self.est_incompleto, self.auth_inc))
        cli.post('/api/participaciones/iniciar/')
        self.cli_doc.post(f'/api/practicas/{self.practica.id}/evaluar-pendientes/')
        self.assertFalse(
            Participacion.objects.filter(
                estudiante=self.est_incompleto, estado=Participacion.Estado.NO_INICIADA,
            ).exists()
        )


@override_settings(EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
class RetroalimentacionYNotificacionesTests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.otro_docente = crear_docente('docenteX', 'dx@uni.edu')
        self.estudiante = crear_estudiante(self.docente)
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth = autorizar(self.practica, self.estudiante)
        self.cli_est = cliente(token_estudiante(self.estudiante, self.auth))
        self.cli_doc = cliente(token_usuario(self.docente))

    def test_rf21_convocatoria_envia_correo_y_no_duplica(self):
        self.assertGreaterEqual(len(mail.outbox), 1)
        self.assertEqual(mail.outbox[0].to, [self.estudiante.correo])
        from apps.resultados.events import CONVOCATORIA_CREADA, Evento, publicador
        publicador.notificar(Evento(CONVOCATORIA_CREADA, {'autorizacion': self.auth}))
        self.assertEqual(
            NotificacionEnvio.objects.filter(tipo=NotificacionEnvio.Tipo.CONVOCATORIA).count(),
            1,
        )

    def test_rf26_docente_registra_feedback_ligado_al_intento(self):
        pid = self.cli_est.post('/api/participaciones/iniciar/').data['id']
        rid = self.cli_est.post(f'/api/participaciones/{pid}/finalizar/').data['resultado_id']
        resp = self.cli_doc.post(
            f'/api/resultados/{rid}/feedback-docente/',
            {'feedback_docente': 'Buen análisis del conflicto.'},
            format='json',
        )
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        resultado = Resultado.objects.get(pk=rid)
        self.assertEqual(resultado.feedback_docente, 'Buen análisis del conflicto.')
        self.assertEqual(resultado.docente_feedback_id, self.docente.id)
        self.assertIsNotNone(resultado.fecha_feedback)

    def test_rf26_docente_ajeno_no_registra_feedback(self):
        pid = self.cli_est.post('/api/participaciones/iniciar/').data['id']
        rid = self.cli_est.post(f'/api/participaciones/{pid}/finalizar/').data['resultado_id']
        cli_ajeno = cliente(token_usuario(self.otro_docente))
        resp = cli_ajeno.post(
            f'/api/resultados/{rid}/feedback-docente/',
            {'feedback_docente': 'No debería guardarse'},
            format='json',
        )
        self.assertIn(resp.status_code, (status.HTTP_403_FORBIDDEN, status.HTTP_404_NOT_FOUND))
        self.assertEqual(Resultado.objects.get(pk=rid).feedback_docente, '')

    def test_rf27_notifica_calificacion_al_destinatario(self):
        mail.outbox.clear()
        pid = self.cli_est.post('/api/participaciones/iniciar/').data['id']
        self.cli_est.post(f'/api/participaciones/{pid}/finalizar/')
        destinatarios = [m.to[0] for m in mail.outbox]
        self.assertIn(self.estudiante.correo, destinatarios)
        self.assertNotIn(self.otro_docente.email, destinatarios)

    def test_rf27_fallo_de_correo_no_pierde_calificacion(self):
        pid = self.cli_est.post('/api/participaciones/iniciar/').data['id']
        with patch('apps.resultados.notificaciones.send_mail', side_effect=OSError('smtp down')):
            fin = self.cli_est.post(f'/api/participaciones/{pid}/finalizar/')
        self.assertEqual(fin.status_code, status.HTTP_200_OK)
        self.assertTrue(Resultado.objects.filter(pk=fin.data['resultado_id']).exists())
        self.assertTrue(
            NotificacionEnvio.objects.filter(
                resultado_id=fin.data['resultado_id'],
                estado=NotificacionEnvio.Estado.ERROR,
            ).exists()
        )


class PermisoEstudianteFeedbackTests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.estudiante = crear_estudiante(self.docente)
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth = autorizar(self.practica, self.estudiante)
        self.cli_est = cliente(token_estudiante(self.estudiante, self.auth))

    def test_estudiante_no_modifica_feedback(self):
        pid = self.cli_est.post('/api/participaciones/iniciar/').data['id']
        rid = self.cli_est.post(f'/api/participaciones/{pid}/finalizar/').data['resultado_id']
        resp = self.cli_est.post(
            f'/api/resultados/{rid}/feedback-docente/',
            {'feedback_docente': 'intento de fraude'},
            format='json',
        )
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)
